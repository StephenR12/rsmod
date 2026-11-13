package org.rsmod.content.skills.herblore.mixology

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.round
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal object MixSeqs : SeqReferences() {
    val machines =
        listOf("retort", "agitator", "alembic").map {
            find("human_machinery_alchemy01_${it}01_interact01")
        }
}

@Singleton
public class MasteringMixology
@Inject
constructor(
    private val types: ObjTypeList,
    private val random: GameRandom,
    private val events: EventBus,
) : PluginScript() {
    private data class Processing(
        val source: CoordGrid,
        val machine: Int,
        val slot: Int,
        val input: InvObj,
        val recipe: MixRecipe,
        var progress: Int = 0,
        var ticks: Int = 0,
        var lastCycle: Int = Int.MIN_VALUE,
        var clickedAt: Int = Int.MIN_VALUE,
        var bonuses: Int = 0,
    )

    private val processing = WeakHashMap<Player, Processing>()
    private val herbExpiry = WeakHashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpLoc1(MixLocs.entry) {
            if (gate(player)) {
                stopAction()
                telejump(CoordGrid(1389, 9314))
                ensureOrders(player)
            } else mes("You need level 60 Herblore and completion of Children of the Sun.")
        }
        onOpLoc1(MixLocs.exit) {
            stopAction()
            cleanup(player)
            telejump(CoordGrid(1388, 2914))
        }
        onOpLoc1(MixLocs.refiner) { refine(this, it.loc) }
        for ((id, _) in MixRecipes.herbs) onOpLocU(MixLocs.refiner, types.getValue(id)) {
            refine(this, it.loc, id)
        }
        onOpLoc1(MixLocs.hopper) { deposit(this) }
        for (paste in MixObjs.paste) onOpLocU(MixLocs.hopper, paste) { deposit(this) }
        for ((i, lever) in MixLocs.levers.withIndex()) onOpLoc1(lever) { pull(this, i) }
        for (vessel in
            (listOf(MixLocs.vessel) + MixLocs.vesselForms).distinctBy { it.id }) onOpLoc1(vessel) {
            takeBase(this)
        }
        for (machine in 0..2) for (loc in
            (listOf(MixLocs.machines[machine]) + MixLocs.machineForms[machine]).distinctBy {
                it.id
            }) {
            onOpLoc1(loc) { process(this, it.loc, machine) }
            for (potion in MixObjs.unfinished) onOpLocU(loc, potion) {
                process(this, it.loc, machine, it.invSlot)
            }
            onOpLoc2(loc) { mes("Processing progress: ${vars[MixBits.progress[machine]]}/60.") }
        }
        onOpLoc1(MixLocs.conveyor) { submit(this) }
        for (potion in MixObjs.finished) onOpLocU(MixLocs.conveyor, potion) { submit(this) }
        for (potion in MixObjs.unfinished + MixObjs.finished) onOpHeldU(MixObjs.digweed, potion) {
            enrich(this, it.firstSlot, it.secondSlot)
        }
        for ((i, potion) in MixObjs.finished.withIndex()) onOpHeld3(potion) {
            val state = inv[it.slot]?.vars?.getBits(MixState.state.bits) ?: 0
            mes(
                "${types[potion].name}: ${listOf("unprocessed","homogenised","concentrated","crystalised")[state and 3]}${if(state and 4!=0)" with digweed"else""}."
            )
        }
        for ((i, plant) in MixLocs.herbs.withIndex()) onOpLoc1(plant) { pickDigweed(this, i) }
        onOpLoc1(MixLocs.herbReady) {
            val i = herbSite(it.loc.coords)
            if (i >= 0) pickDigweed(this, i)
        }
        onOpLoc1(MixLocs.shelf) { if (allowed(player)) invAdd(inv, MixObjs.notes) }
        onOpHeld1(MixObjs.notes) {
            mes(
                "Mox, Aga and Lye are mixed in threes. Match your orders, process at the requested workstation, then deliver to the conveyor."
            )
        }
        onPlayerLogin {
            resetMachines(player)
            if (MixRecipes.inBasement(player.coords)) {
                ensureOrders(player)
                player.softTimer(MixTimer.refresh, 2)
            }
        }
        onPlayerLogout { cleanup(player) }
        onPlayerSoftTimer(MixTimer.refresh) {
            if (!MixRecipes.inActivity(player.coords)) {
                if (MixObjs.digweed in player.inv)
                    player.invDel(
                        player.inv,
                        types[MixObjs.digweed],
                        player.inv.count(types[MixObjs.digweed]),
                    )
                player.ifCloseSub(MixUi.overlay, events)
            } else if (MixUi.overlay !in player.ui) player.ifOpenOverlay(MixUi.overlay, events)
            if ((herbExpiry[player] ?: Int.MAX_VALUE) <= player.currentMapClock) {
                for (bit in MixBits.herbs) VarPlayerIntMapSetter.set(player, bit, 0)
                herbExpiry.remove(player)
            }
            val run = processing[player]
            if (run != null && player.currentMapClock - run.lastCycle > 3) resetMachines(player)
            if (MixRecipes.inBasement(player.coords)) player.softTimer(MixTimer.refresh, 2)
            else cleanup(player)
        }
    }

    public fun gate(player: Player): Boolean =
        player.members && player.statBase(stats.herblore) >= 60 && player.vars[MixBits.quest] >= 24

    public fun allowed(player: Player): Boolean =
        gate(player) && MixRecipes.inBasement(player.coords)

    private fun touch(access: ProtectedAccess) {
        ensureOrders(access.player)
        access.softTimer(MixTimer.refresh, 2)
        if (MixRecipes.inActivity(access.coords) && MixUi.overlay !in access.player.ui)
            access.ifOpenOverlay(MixUi.overlay)
    }

    public fun ensureOrders(player: Player) {
        if (player.vars[MixBits.orderType[0]] == 0) rollOrders(player)
    }

    public fun rollOrders(player: Player) {
        val eligible = MixRecipes.all.filter { player.stat(stats.herblore) >= it.level }
        if (eligible.isEmpty()) return
        val total = eligible.sumOf { it.weight }
        for (i in 0..2) {
            var roll = random.of(total)
            val recipe =
                eligible.first {
                    roll -= it.weight
                    roll < 0
                }
            VarPlayerIntMapSetter.set(player, MixBits.orderType[i], recipe.nativeType)
            VarPlayerIntMapSetter.set(player, MixBits.orderModifier[i], random.of(1..3))
        }
    }

    public suspend fun refine(
        access: ProtectedAccess,
        source: BoundLocInfo,
        selected: Int? = null,
    ): Unit =
        with(access) {
            if (!allowed(player)) return
            touch(this)
            var first = true
            while (allowed(player) && isWithinDistance(source, 1)) {
                val slot =
                    inv.indexOfFirst {
                        it != null &&
                            (selected == null || it.id == selected) &&
                            it.id in MixRecipes.herbs
                    }
                if (slot < 0) break
                val obj = inv.getValue(slot)
                val (paste, count) = MixRecipes.herbs.getValue(obj.id)
                anim(seqs.human_herbing_grind)
                gatheringDelay(if (first) 1 else 2)
                first = false
                if (!allowed(player) || !isWithinDistance(source, 1) || inv[slot] != obj) break
                val tx =
                    player.invTransaction(inv) {
                        val i = select(inv)
                        delete(i, obj.id, 1, slot = slot)
                        add(i, MixObjs.paste[paste].id, count)
                    }
                if (!tx.success) {
                    mes("Make room for the paste.")
                    break
                }
                statAdvance(stats.herblore, 6.0)
            }
            resetAnim()
        }

    public fun deposit(access: ProtectedAccess): Boolean =
        with(access) {
            if (!allowed(player)) return false
            touch(this)
            var deposited = false
            for (i in 0..2) {
                val amount =
                    minOf(3000 - vars[MixBits.available[i]], inv.count(types[MixObjs.paste[i]]))
                if (amount <= 0) continue
                if (player.invDel(inv, types[MixObjs.paste[i]], amount).success) {
                    vars[MixBits.available[i]] += amount
                    deposited = true
                }
            }
            mes(
                if (deposited) "You add paste to the hopper."
                else "The hopper can hold up to 3,000 of each paste."
            )
            deposited
        }

    public fun pull(access: ProtectedAccess, paste: Int): Boolean =
        with(access) {
            if (!allowed(player) || paste !in 0..2) return false
            touch(this)
            val slots = MixBits.mixer.map { vars[it] }.filter { it != 0 }.toMutableList()
            slots.add(paste + 1)
            while (slots.size > 3) slots.removeAt(0)
            for (i in 0..2) vars[MixBits.mixer[i]] = slots.getOrElse(i) { 0 }
            vars[MixBits.vessel] = MixRecipes.recipe(slots)?.nativeType ?: 0
            true
        }

    public fun takeBase(access: ProtectedAccess): Boolean =
        with(access) {
            if (!allowed(player)) return false
            touch(this)
            val recipe = MixRecipes.recipe(MixBits.mixer.map { vars[it] }) ?: return false
            if (player.stat(stats.herblore) < recipe.level) {
                mes("You need level ${recipe.level} Herblore for this mixture.")
                return false
            }
            if ((0..2).any { vars[MixBits.available[it]] < recipe.mix[it] * 10 }) {
                mes("There is not enough paste in the hopper.")
                return false
            }
            if (!player.invAdd(inv, recipe.unfinished).success) return false
            for (i in 0..2) {
                vars[MixBits.available[i]] -= recipe.mix[i] * 10
                vars[MixBits.mixer[i]] = 0
            }
            vars[MixBits.vessel] = 0
            true
        }

    public suspend fun process(
        access: ProtectedAccess,
        source: BoundLocInfo,
        machine: Int,
        selectedSlot: Int? = null,
    ): Unit =
        with(access) {
            if (!allowed(player) || machine !in 0..2) return
            touch(this)
            var run = processing[player]
            if (
                run == null ||
                    run.source != source.coords ||
                    run.machine != machine ||
                    inv[run.slot] != run.input ||
                    mapClock - run.lastCycle > 3 ||
                    selectedSlot != null && selectedSlot != run.slot
            ) {
                resetMachines(player)
                val slot =
                    selectedSlot
                        ?: inv.indexOfFirst { item ->
                            item != null && MixObjs.unfinished.any { it.id == item.id }
                        }
                val input = inv[slot] ?: return
                val recipe = MixRecipes.all.firstOrNull { it.unfinished.id == input.id } ?: return
                run = Processing(source.coords, machine, slot, input, recipe, lastCycle = mapClock)
                processing[player] = run
            } else if (run.clickedAt != mapClock) {
                run.clickedAt = mapClock
                run.lastCycle = mapClock
                val bonus =
                    when (machine) {
                        0 -> 2
                        1 ->
                            if (run.progress in 25..40 && run.bonuses < 2) {
                                if (run.bonuses++ == 0) 14 else 2
                            } else 0
                        else ->
                            if (run.ticks >= 10 && run.bonuses == 0) {
                                run.bonuses++
                                14
                            } else 0
                    }
                if (bonus > 0) {
                    run.progress = (run.progress + 8).coerceAtMost(60)
                    statAdvance(stats.herblore, bonus.toDouble())
                }
            }
            val work = run
            vars[MixBits.machinePotion[machine]] = work.recipe.nativeType
            vars[MixBits.progress[machine]] = work.progress
            while (
                processing[player] === work &&
                    allowed(player) &&
                    isWithinDistance(source, 1) &&
                    inv[work.slot] == work.input
            ) {
                anim(MixSeqs.machines[machine])
                gatheringDelay(1)
                if (
                    processing[player] !== work ||
                        !allowed(player) ||
                        !isWithinDistance(source, 1) ||
                        inv[work.slot] != work.input
                )
                    break
                if (work.lastCycle < mapClock) {
                    work.lastCycle = mapClock
                    work.ticks++
                    work.progress =
                        (work.progress +
                                2 +
                                (player.stat(stats.herblore) - 60).coerceAtLeast(0) / 20)
                            .coerceAtMost(60)
                }
                vars[MixBits.progress[machine]] = work.progress
                if (machine == 1 && work.progress in 25..28 || machine == 2 && work.ticks == 10)
                    mes("The workstation is ready for a well-timed extra push!")
                if (work.progress >= 60) {
                    val state = work.input.vars.getBits(MixState.state.bits)
                    val packed =
                        work.input.vars.withBits(
                            MixState.state.bits,
                            (state and 4) or (listOf(2, 1, 3)[machine]),
                        )
                    val tx =
                        player.invTransaction(inv) {
                            val i = select(inv)
                            delete(i, work.input.id, 1, slot = work.slot)
                            add(i, work.recipe.finished.id, 1, slot = work.slot, vars = packed)
                        }
                    if (tx.success) mes("You finish processing the potion.")
                    resetMachines(player)
                    break
                }
            }
            resetAnim()
        }

    public fun submit(access: ProtectedAccess): Boolean =
        with(access) {
            if (!allowed(player)) return false
            touch(this)
            val selected = mutableListOf<Triple<Int, MixRecipe, Boolean>>()
            for (order in 0..2) {
                val recipe =
                    MixRecipes.all.firstOrNull { it.nativeType == vars[MixBits.orderType[order]] }
                        ?: continue
                val modifier = vars[MixBits.orderModifier[order]]
                val slot =
                    inv.indices.firstOrNull { slot ->
                        slot !in selected.map { it.first } &&
                            inv[slot]?.id == recipe.finished.id &&
                            requireNotNull(inv[slot]).vars.getBits(MixState.state.bits) and 3 ==
                                modifier
                    } ?: continue
                selected.add(Triple(slot, recipe, true))
            }
            if (selected.isEmpty()) {
                val slot =
                    inv.indices.firstOrNull { slot ->
                        val item = inv[slot]
                        item != null &&
                            MixObjs.finished.any { it.id == item.id } &&
                            item.vars.getBits(MixState.state.bits) and 3 in 1..3
                    } ?: return false
                selected.add(
                    Triple(
                        slot,
                        MixRecipes.all.first { it.finished.id == inv.getValue(slot).id },
                        false,
                    )
                )
            }
            val doses =
                selected.map { (slot, recipe, ordered) ->
                    Triple(
                        recipe,
                        ordered,
                        inv.getValue(slot).vars.getBits(MixState.state.bits) and 4 != 0,
                    )
                }
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    for ((slot, recipe, _) in selected) delete(
                        i,
                        recipe.finished.id,
                        1,
                        slot = slot,
                    )
                }
            if (!tx.success) return false
            val multiplier = 10 + 2 * (selected.size - 1)
            for ((recipe, ordered, digweed) in doses) {
                val double = if (digweed) 2 else 1
                statAdvance(
                    stats.herblore,
                    (if (ordered) recipe.xp else round(recipe.xp / 3)) * double,
                )
                for (i in 0..2) {
                    val base =
                        if (ordered) recipe.points[i]
                        else if (i == recipe.mix.indexOfLast { it > 0 }) 10 else 0
                    vars[MixVars.points[i]] =
                        (vars[MixVars.points[i]].toLong() +
                                base * double * (if (ordered) multiplier else 10) / 10)
                            .coerceAtMost(Int.MAX_VALUE.toLong())
                            .toInt()
                }
            }
            vars[MixVars.orders] =
                (vars[MixVars.orders].toLong() + selected.size)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            val count = vars[MixBits.sinceHerb] + selected.size
            // Active-order interval is an explicit approximation of the unpublished level-scaled
            // spawn process.
            val interval = (31 - (player.stat(stats.herblore) - 60) / 3).coerceIn(18, 31)
            if (
                count >= interval && MixObjs.digweed !in inv && MixBits.herbs.none { vars[it] != 0 }
            ) {
                vars[MixBits.herbs[random.of(4)]] = 1
                herbExpiry[player] = mapClock + 33
                vars[MixBits.sinceHerb] = 0
                mes("A digweed plant has bloomed in a corner of the lab!")
            } else vars[MixBits.sinceHerb] = count.coerceAtMost(31)
            rollOrders(player)
            mes("You fulfil ${selected.size} potion order${if(selected.size==1)""else"s"}.")
            true
        }

    public fun enrich(access: ProtectedAccess, herbSlot: Int, potionSlot: Int): Boolean =
        with(access) {
            if (!allowed(player) || herbSlot == potionSlot) return false
            val potion = inv[potionSlot] ?: return false
            if (
                inv[herbSlot]?.id != MixObjs.digweed.id ||
                    (MixObjs.unfinished + MixObjs.finished).none { it.id == potion.id }
            )
                return false
            val state = potion.vars.getBits(MixState.state.bits)
            if (state and 4 != 0) {
                mes("This potion has already been improved with digweed.")
                return false
            }
            val tx =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, MixObjs.digweed.id, 1, slot = herbSlot)
                    delete(i, potion.id, 1, slot = potionSlot)
                    add(
                        i,
                        potion.id,
                        1,
                        slot = potionSlot,
                        vars = potion.vars.withBits(MixState.state.bits, state or 4),
                    )
                }
            tx.success
        }

    private fun herbSite(c: CoordGrid): Int =
        when {
            c.x < 1394 && c.z > 9325 -> 0
            c.x > 1394 && c.z > 9325 -> 1
            c.x > 1394 -> 2
            else -> 3
        }

    public fun pickDigweed(access: ProtectedAccess, index: Int): Boolean =
        with(access) {
            if (
                !allowed(player) ||
                    index !in 0..3 ||
                    vars[MixBits.herbs[index]] == 0 ||
                    MixObjs.digweed in inv
            )
                return false
            if (!player.invAdd(inv, MixObjs.digweed).success) return false
            vars[MixBits.herbs[index]] = 0
            herbExpiry.remove(player)
            true
        }

    private fun resetMachines(player: Player) {
        processing.remove(player)
        for (bit in MixBits.progress + MixBits.machinePotion) VarPlayerIntMapSetter.set(
            player,
            bit,
            0,
        )
    }

    private fun cleanup(player: Player) {
        resetMachines(player)
        herbExpiry.remove(player)
        for (bit in MixBits.herbs) VarPlayerIntMapSetter.set(player, bit, 0)
        if (MixObjs.digweed in player.inv)
            player.invDel(
                player.inv,
                types[MixObjs.digweed],
                player.inv.count(types[MixObjs.digweed]),
            )
        player.ifCloseSub(MixUi.overlay, events)
    }
}
