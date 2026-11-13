package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.feet
import org.rsmod.api.player.hands
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.torso
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.script.*
import org.rsmod.content.skills.smithing.smelting.AnvilRecipes
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GiantsFoundry
@Inject
constructor(
    private val recipes: AnvilRecipes,
    private val types: ObjTypeList,
    private val inventories: InvTypeList,
    private val shop: FoundryShop,
) : PluginScript() {
    private data class Operation(val tool: Int, val heat: Int = 0, val fast: Boolean = false)

    private data class Work(val tool: Int, val expires: Int, val caught: Boolean)

    private val active = mutableMapOf<Player, Work>()

    private data class Metal(val tier: Int, val bars: Int)

    private val materials by lazy {
        buildMap {
            for ((tier, bar) in recipes.bars.withIndex()) put(bar.id, Metal(tier, 1))
            for (r in recipes.all) if (r.bars >= 2 && r.count == 1)
                put(r.output.id, Metal(r.metal - 1, r.bars - 1))
        }
    }

    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<Operation>(FoundryQueues.refine) { next ->
            val op = next.args
            if (!holding(this) || vars[FoundryBits.progress] >= 1000) return@onPlayerQueueWithArgs
            if (op.heat != 0) {
                val temperature = adjustHeat(this, op.heat)
                if (temperature in 1..999) {
                    val speed =
                        (kotlin.math.abs(op.heat) + (if (op.fast) 5 else 2)).coerceAtMost(
                            if (op.fast) 80 else 30
                        )
                    // The queue processor visits newly queued steps once during this same tick.
                    weakQueue(
                        FoundryQueues.refine,
                        2,
                        op.copy(heat = if (op.heat > 0) speed else -speed),
                    )
                }
            } else if (refine(this, op.tool) && vars[FoundryBits.progress] < 1000) {
                anim(FoundrySeqs.loop)
                weakQueue(FoundryQueues.refine, if (op.tool == 0) 6 else 3, op)
            }
        }
        onOpWorn1(FoundryObjs.preform) { mes("Put your preform into the storage rack first.") }
        onPlayerSoftTimer(FoundryTimers.cooling) {
            if (!player.righthand.isType(FoundryObjs.preform)) {
                player.clearSoftTimer(FoundryTimers.cooling)
                return@onPlayerSoftTimer
            }
            if (player.vars[FoundryBits.progress] < 1000 && player.currentMapClock % 5 == 0) {
                set(player, FoundryBits.heat, (player.vars[FoundryBits.heat] - 1).coerceAtLeast(0))
            }
            val window = active[player]
            if (window != null && player.currentMapClock > window.expires) {
                active.remove(player)
                player.ifSetHide(FoundryButtons.sweet, true)
            }
        }
        onPlayerLogout {
            active.remove(player)
            if (player.righthand.isType(FoundryObjs.preform)) {
                player.righthand = null
                set(player, FoundryBits.stored, 1)
                player.activityBlocksTeleport = false
            }
        }
        onOpLoc1(FoundryLocs.bank) {
            ifOpenMainSidePair(interfaces.bank_main, interfaces.bank_side)
        }
        onOpLoc1(FoundryLocs.entrance) { if (eligible(this)) telejump(CoordGrid(3363, 11482)) }
        onOpLoc1(FoundryLocs.exit) {
            if (player.righthand.isType(FoundryObjs.preform)) {
                mes("Store your preform before leaving the foundry.")
                return@onOpLoc1
            }
            ifCloseSub(FoundryInterfaces.hud)
            telejump(CoordGrid(3359, 3149))
        }
        for (n in listOf(FoundryNpcs.kovac, FoundryNpcs.multi)) {
            onOpNpc1(n) {
                when (
                    choice3(
                        "Ask for a commission.",
                        1,
                        "Hand in a sword.",
                        2,
                        "Open the reward shop.",
                        3,
                    )
                ) {
                    1 -> commission(this)
                    2 -> handIn(this)
                    3 -> shop.open(this)
                }
            }
            onOpNpc2(n) { handIn(this) }
            onOpNpc3(n) { commission(this) }
            onOpNpc4(n) { shop.open(this) }
        }
        for (loc in FoundryLocs.crucibles) {
            onOpLoc1(loc) { if (player.vars[FoundryBits.crucible] == 2) pour(this) else fill(this) }
            onOpLoc2(loc) {
                mes(
                    FoundryBits.metals
                        .mapIndexed { i, b -> "${types[recipes.bars[i]].name}: ${vars[b]}" }
                        .joinToString(", ")
                )
            }
            onOpLoc3(loc) {
                if (choice2("Destroy the molten metal.", true, "Cancel.", false))
                    clearCrucible(player)
            }
            for (item in materials.keys) onOpLocU(loc, types.getValue(item)) {
                fill(this, it.invSlot)
            }
        }
        for (loc in FoundryLocs.jigs) onOpLoc1(loc) {
            when (vars[FoundryBits.mould]) {
                0,
                1 -> openMould(this)
                else -> takePreform(this)
            }
        }
        onIfModalButton(FoundryButtons.mouldTabs) {
            if (it.comsub >= 0) vars[FoundryBits.tab] = (it.comsub / 9).coerceIn(0, 2)
        }
        onIfModalButton(FoundryButtons.mouldList) {
            if (it.comsub >= 0) selectMould(this, vars[FoundryBits.tab], it.comsub / 17 + 1)
        }
        onIfModalButton(FoundryButtons.reset) { for (b in FoundryBits.parts) vars[b] = 0 }
        onIfModalButton(FoundryButtons.set) { if (setMould(this)) ifClose() }
        for ((i, tool) in FoundryLocs.tools.withIndex()) onOpLoc1(tool) { work(this, i) }
        for ((loc, sign) in listOf(FoundryLocs.heat to 1, FoundryLocs.cool to -1)) {
            onOpLoc1(loc) { temper(this, sign, false) }
            onOpLoc2(loc) { temper(this, sign, true) }
        }
        onOpLocU(FoundryLocs.cool, objs.bucket_empty) {
            if (inv[it.invSlot].isType(objs.bucket_empty))
                inv[it.invSlot] = InvObj(objs.bucket_water)
        }
        for (loc in FoundryLocs.storage) onOpLoc1(loc) { store(this) }
        for (loc in FoundryLocs.boxes) onOpLoc1(loc) {
            if (
                eligible(this) &&
                    !inv.contains(objs.bucket_empty) &&
                    !inv.contains(objs.bucket_water)
            )
                invAdd(inv, objs.bucket_empty)
        }
    }

    fun commission(access: ProtectedAccess): Boolean =
        with(access) {
            if (!eligible(this)) return false
            if (vars[FoundryBits.words[0]] == 0) {
                val axis = random.of(3)
                val other = (axis + 1 + random.of(2)) % 3
                vars[FoundryBits.words[0]] = 1 + axis + 3 * random.of(2)
                vars[FoundryBits.words[1]] = 1 + other + 3 * random.of(2)
            }
            mes(
                "Kovac wants a ${FoundryMoulds.words[vars[FoundryBits.words[0]]].lowercase()}, ${FoundryMoulds.words[vars[FoundryBits.words[1]]].lowercase()} sword."
            )
            true
        }

    private fun openMould(access: ProtectedAccess) =
        with(access) {
            if (!eligible(this) || vars[FoundryBits.words[0]] == 0) {
                mes("Ask Kovac for a commission first.")
                return
            }
            ifOpenMainModal(FoundryInterfaces.mould)
            ifSetEvents(FoundryButtons.mouldTabs, 0..26, IfEvent.Op1)
            ifSetEvents(FoundryButtons.mouldList, 0..190, IfEvent.Op1)
            ifSetEvents(FoundryButtons.set, -1..-1, IfEvent.Op1)
            ifSetEvents(FoundryButtons.reset, -1..-1, IfEvent.Op1)
        }

    fun selectMould(access: ProtectedAccess, part: Int, index: Int): Boolean =
        with(access) {
            val mould = FoundryMoulds.find(part, index) ?: return false
            if (!eligible(this) || vars[FoundryBits.mould] >= 2 || !mould.usable(player))
                return false
            vars[FoundryBits.parts[part]] = index
            true
        }

    fun setMould(access: ProtectedAccess): Boolean =
        with(access) {
            if (!eligible(this) || vars[FoundryBits.words[0]] == 0 || vars[FoundryBits.mould] >= 2)
                return false
            if (
                FoundryBits.parts.withIndex().any { (part, b) ->
                    FoundryMoulds.find(part, vars[b])?.usable(player) != true
                }
            ) {
                mes("Select an unlocked forte, blade and tip that you can use.")
                return false
            }
            vars[FoundryBits.mould] = 1
            mes("You set the mould in the jig.")
            true
        }

    fun fill(access: ProtectedAccess, slot: Int? = null): Int =
        with(access) {
            if (!eligible(this)) return 0
            var space = 28 - FoundryBits.metals.sumOf { vars[it] }
            var added = 0
            for (i in if (slot == null) 0 until inv.size else slot..slot) {
                val obj = inv[i] ?: continue
                val metal = materials[obj.id] ?: continue
                if (player.stat(stats.smithing) < listOf(1, 15, 30, 50, 70, 85)[metal.tier])
                    continue
                val count = minOf(obj.count, space / metal.bars)
                if (count == 0) continue
                if (invDel(inv, types[obj], count, i).failure) continue
                val weight = count * metal.bars
                vars[FoundryBits.metals[metal.tier]] += weight
                space -= weight
                added += weight
            }
            val total = 28 - space
            vars[FoundryBits.crucible] =
                when (total) {
                    0 -> 0
                    28 -> 2
                    else -> 1
                }
            vars[FoundryBits.alloy] = FoundryMoulds.metalScore(FoundryBits.metals.map { vars[it] })
            mes("The crucible holds $total of 28 bars of metal.")
            added
        }

    private fun clearCrucible(p: Player) {
        for (b in FoundryBits.metals) set(p, b, 0)
        set(p, FoundryBits.crucible, 0)
        set(p, FoundryBits.alloy, 0)
    }

    fun pour(access: ProtectedAccess): Boolean =
        with(access) {
            if (
                !eligible(this) ||
                    vars[FoundryBits.words[0]] == 0 ||
                    vars[FoundryBits.mould] != 1 ||
                    vars[FoundryBits.crucible] != 2 ||
                    vars[FoundryBits.initial] != 0
            ) {
                mes("You need a commission, a set mould and 28 bars of metal.")
                return false
            }
            val parts =
                FoundryBits.parts.mapIndexed { i, b ->
                    FoundryMoulds.find(i, vars[b]) ?: return false
                }
            if (parts.any { !it.usable(player) }) return false
            val score = FoundryMoulds.metalScore(FoundryBits.metals.map { vars[it] })
            val quality = score + parts.sumOf { m -> FoundryBits.words.sumOf { m.score(vars[it]) } }
            vars[FoundryBits.difficulty] = score
            vars[FoundryBits.initial] = quality
            vars[FoundryBits.quality] = quality
            vars[FoundryBits.heat] = 1000
            vars[FoundryBits.progress] = 0
            var last = 0
            for ((i, seed) in FoundryBits.sequences.withIndex()) {
                last =
                    if (i == 0) 0
                    else if (i == 1) 1 else if (i == 2) 2 else (last + 1 + random.of(2)) % 3
                vars[seed] = last
            }
            clearCrucible(player)
            vars[FoundryBits.mould] = 2
            mes("You pour the molten alloy into the mould.")
            true
        }

    fun takePreform(access: ProtectedAccess): Boolean =
        with(access) {
            if (!eligible(this) || vars[FoundryBits.mould] < 2 || vars[FoundryBits.initial] == 0)
                return false
            if (player.righthand != null || player.lefthand != null) {
                mes("You'll need both hands free to lift the preform.")
                return false
            }
            if (!player.hands.isAnyType(objs.ice_gloves, FoundryObjs.glovesIce)) {
                val slot = inv.indexOfFirst { it.isType(objs.bucket_water) }
                if (slot < 0) {
                    mes("Use ice gloves or a bucket of water to pick up the hot preform.")
                    return false
                }
                inv[slot] = InvObj(objs.bucket_empty)
            }
            player.activityBlocksTeleport = true
            player.righthand = InvObj(FoundryObjs.preform)
            vars[FoundryBits.mould] = 0
            softTimer(FoundryTimers.cooling, 1)
            ifOpenOverlay(FoundryInterfaces.hud)
            true
        }

    private fun store(access: ProtectedAccess) =
        with(access) {
            if (!eligible(this)) return
            if (player.righthand.isType(FoundryObjs.preform)) {
                active.remove(player)
                player.righthand = null
                clearSoftTimer(FoundryTimers.cooling)
                player.activityBlocksTeleport = false
                vars[FoundryBits.stored] = 1
                ifCloseSub(FoundryInterfaces.hud)
            } else if (
                vars[FoundryBits.stored] == 1 && player.righthand == null && player.lefthand == null
            ) {
                player.activityBlocksTeleport = true
                player.righthand = InvObj(FoundryObjs.preform)
                vars[FoundryBits.stored] = 0
                softTimer(FoundryTimers.cooling, 1)
                ifOpenOverlay(FoundryInterfaces.hud)
            }
        }

    private fun holding(access: ProtectedAccess): Boolean =
        with(access) {
            eligible(this) &&
                player.righthand.isType(FoundryObjs.preform) &&
                vars[FoundryBits.initial] > 0 &&
                vars[FoundryBits.stored] == 0
        }

    private fun temper(access: ProtectedAccess, sign: Int, fast: Boolean): Unit =
        with(access) {
            if (!holding(this) || vars[FoundryBits.progress] >= 1000) return
            clearWeakQueue(FoundryQueues.refine)
            weakQueue(FoundryQueues.refine, 1, Operation(-1, sign * (if (fast) 40 else 10), fast))
        }

    fun adjustHeat(access: ProtectedAccess, delta: Int): Int =
        with(access) {
            if (holding(this) && vars[FoundryBits.progress] < 1000)
                vars[FoundryBits.heat] =
                    (vars[FoundryBits.heat] + delta.coerceIn(-80, 80)).coerceIn(0, 1000)
            vars[FoundryBits.heat]
        }

    private fun work(access: ProtectedAccess, tool: Int): Unit =
        with(access) {
            if (
                !holding(this) ||
                    vars[FoundryBits.progress] >= 1000 ||
                    vars[FoundryBits.quality] == 0
            )
                return
            val offered = active[player]
            if (offered != null && offered.tool == tool && mapClock <= offered.expires)
                active[player] = offered.copy(caught = true)
            clearWeakQueue(FoundryQueues.refine)
            anim(FoundrySeqs.work)
            weakQueue(FoundryQueues.refine, if (tool == 0) 5 else 2, Operation(tool))
        }

    fun refine(access: ProtectedAccess, tool: Int): Boolean =
        with(access) {
            if (
                !holding(this) ||
                    tool !in 0..2 ||
                    vars[FoundryBits.progress] >= 1000 ||
                    vars[FoundryBits.quality] == 0
            )
                return false
            val previous = vars[FoundryBits.progress]
            val sections = FoundryMoulds.sections(vars[FoundryBits.difficulty])
            val section = (previous * sections / 1000).coerceAtMost(6)
            val window = active[player]
            val sweet =
                window != null && window.tool == tool && window.caught && mapClock <= window.expires
            if (sweet) {
                active.remove(player)
                ifSetHide(FoundryButtons.sweet, true)
            }
            val needed = vars[FoundryBits.sequences[section]]
            val width = 333 - vars[FoundryBits.difficulty] * 167 / 130
            val lower = 166 + (2 - tool) * 333 - width / 2
            if (tool != needed || vars[FoundryBits.heat] !in lower until lower + width) {
                vars[FoundryBits.quality] = (vars[FoundryBits.quality] - 10).coerceAtLeast(0)
                mes("The preform is damaged. Check the tool and temperature on the HUD.")
                return false
            }
            vars[FoundryBits.heat] =
                (vars[FoundryBits.heat] + listOf(-25, 15, -17)[tool]).coerceIn(0, 1000)
            val uniform =
                listOf(
                        player.torso.isType(FoundryObjs.rewards[4]),
                        player.legs.isType(FoundryObjs.rewards[5]),
                        player.feet.isType(FoundryObjs.rewards[6]),
                        player.hands.isAnyType(FoundryObjs.rewards[7], FoundryObjs.glovesIce),
                    )
                    .count { it }
            val extra = if (uniform == 4 || (uniform > 0 && random.of(5) < uniform)) 1 else 0
            val step = if (tool == 0) 20 else 10
            vars[FoundryBits.progress] =
                (previous + step + extra + if (sweet) 50 else 0).coerceAtMost(1000)
            if (sweet)
                vars[FoundryBits.quality] =
                    minOf(vars[FoundryBits.initial], vars[FoundryBits.quality] + 10)
            statAdvance(stats.smithing, listOf(4.5, 3.2, 3.3)[tool] * (if (sweet) 2 else 1))
            val marks = if (sections >= 6) listOf(150, 450, 750) else listOf(200, 650)
            if (marks.any { it > previous && it <= vars[FoundryBits.progress] }) {
                active[player] = Work(tool, mapClock + 10, false)
                ifSetHide(FoundryButtons.sweet, false)
            }
            if (vars[FoundryBits.progress] == 1000)
                mes("Your commission is complete. Take the sword to Kovac.")
            true
        }

    fun handIn(access: ProtectedAccess): Boolean =
        with(access) {
            if (!holding(this)) return false
            val q = vars[FoundryBits.quality]
            if (q > 0 && vars[FoundryBits.progress] != 1000) {
                mes("You haven't finished the sword yet.")
                return false
            }
            if (q > 0) {
                val xp = FoundryMoulds.handInXp(q)
                val bank = player.invMap.getOrPut(inventories[invs.bank])
                val paid = player.invTransaction(bank) { add(select(bank), objs.coins.id, xp * 2) }
                if (!paid.success && invAdd(inv, objs.coins, xp * 2).failure) {
                    mes("Make room for the coins from your commission.")
                    return false
                }
                statAdvance(stats.smithing, xp.toDouble())
                vars[FoundryVars.points] += q
                player.runEnergy = (player.runEnergy + 5000).coerceAtMost(10000)
                mes("Kovac awards you $q reputation, $xp Smithing experience and ${xp*2} coins.")
            } else mes("Kovac discards the ruined preform.")
            active.remove(player)
            clearSoftTimer(FoundryTimers.cooling)
            player.righthand = null
            player.activityBlocksTeleport = false
            for (b in
                FoundryBits.words +
                    FoundryBits.parts +
                    listOf(
                        FoundryBits.initial,
                        FoundryBits.quality,
                        FoundryBits.progress,
                        FoundryBits.heat,
                        FoundryBits.stored,
                        FoundryBits.mould,
                    )) vars[b] = 0
            ifCloseSub(FoundryInterfaces.hud)
            true
        }

    private fun eligible(access: ProtectedAccess): Boolean =
        with(access) {
            if (player.members && vars[FoundryBits.quest] >= 30) return true
            mes("Complete Sleeping Giants to use the foundry.")
            false
        }
}
