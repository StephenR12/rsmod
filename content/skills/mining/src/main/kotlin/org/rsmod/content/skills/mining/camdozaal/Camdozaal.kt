package org.rsmod.content.skills.mining.camdozaal

import jakarta.inject.Inject
import java.time.Instant
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CamBits : VarBitReferences() {
    val quest = find("bim")
    val intro = find("camdozaal_ramarno_intro")
    val buffs =
        listOf("fish_catch", "fish_prep", "defence", "mining", "rewards", "timed_multi").map {
            find("camdozaal_${it}_buff")
        }
    val time = find("camdozaal_timed_multi_buff_remaining")
    val kudos = find("vm_kudos")
    val relics = (1..5).map { find("camdozaal_museum_relic_$it") }
    val barriers = ('a'..'g').map { find("camdozaal_vault_barrier_$it") }
    val boxes =
        listOf("small" to 6, "medium" to 4, "large" to 3).flatMap { (name, n) ->
            (1..n).map { find("camdozaal_vault_chest_${name}_$it") }
        }
    val remaining = find("camdozaal_vault_time_remaining")
}

internal object CamVars : VarpReferences() {
    val stored = find("camdozaal_stored_barronite")
    val questState = find("bim_main")
    val general = find("camdozaal_main")
    val end = find("camdozaal_boost_until")
}

internal object CamSave : VarpBuilder() {
    init {
        build("camdozaal_boost_until") { permanent = true }
    }
}

internal object CamNativeSaves : VarpEditor() {
    init {
        for (ref in listOf(CamVars.questState, CamVars.stored, CamVars.general)) edit(ref) {
            permanent = true
        }
    }
}

internal object CamTime : TimerReferences() {
    val boost = find("camdozaal_boost")
    val vault = find("camdozaal_vault_watch")
}

internal object CamQueues : QueueReferences() {
    val fish = find("camdozaal_fishing")
}

internal object CamUi : InterfaceReferences() {
    val shop = find("camdozaal_ramarno_shop")
    val vault = find("camdozaal_vault")
}

internal object CamButtons : ComponentReferences() {
    val buffs = (1..6).map { find("camdozaal_ramarno_shop:buff_button_$it") }
}

internal object CamPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<CamPeople>("people.toml")
    }
}

internal object CamTools : MapObjSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<CamTools>("tools.toml")
    }
}

internal object Camdozaal {
    fun allowed(p: Player) = p.vars[CamBits.quest] >= 120

    fun boost(p: Player, i: Int): Int =
        100 +
            (if (p.vars[CamBits.buffs[i]] > 0) if (i == 4) 5 else 10 else 0) +
            (if (p.vars[CamVars.end] > Instant.now().epochSecond) 5 else 0)
}

public class CamdozaalForge
@Inject
constructor(
    private val locs: LocTypeList,
    private val npcs: NpcTypeList,
    private val items: ObjTypeList,
) : PluginScript() {
    private val shards = MiningObjs.barroniteShards

    private fun sync(p: Player) {
        val remain = (p.vars[CamVars.end] - Instant.now().epochSecond).coerceAtLeast(0)
        VarPlayerIntMapSetter.set(p, CamBits.buffs[5], if (remain > 0) 1 else 0)
        VarPlayerIntMapSetter.set(p, CamBits.time, ((remain + 59) / 60).toInt().coerceAtMost(60))
        if (remain > 0) p.softTimer(CamTime.boost, 100) else p.clearSoftTimer(CamTime.boost)
    }

    override fun ScriptContext.startup() {
        onPlayerLogin { sync(player) }
        onPlayerSoftTimer(CamTime.boost) { sync(player) }
        onOpLoc1(locs.getValue(41357)) {
            if (Camdozaal.allowed(player)) telejump(CoordGrid(2952, 5764))
            else mes("You need to complete Below Ice Mountain to enter the ruins.")
        }
        onOpLoc1(locs.getValue(41446)) { telejump(CoordGrid(2998, 3493)) }
        onOpLoc1(locs.getValue(41593)) {
            if (objs.bronze_pickaxe !in inv) invAdd(inv, objs.bronze_pickaxe)
        }
        for (id in listOf(41411, 41412)) {
            val forge = locs.getValue(id)
            onOpLoc1(forge) { fuel() }
            onOpLocU(forge, shards) { fuel() }
            onOpLoc2(forge) { mes("The forge holds ${vars[CamVars.stored]} barronite shards.") }
            onOpLoc3(forge) {
                if (vars[CamBits.intro] >= 2 && vars[CamVars.stored] > 0) {
                    val delivered =
                        invAdd(inv, shards, vars[CamVars.stored], strict = false).completed()
                    vars[CamVars.stored] -= delivered
                }
            }
        }
        for (id in listOf(10702, 10703, 10684, 10685)) {
            val npc = npcs.getValue(id)
            onOpNpc1(npc) {
                if (!Camdozaal.allowed(player)) return@onOpNpc1
                if (vars[CamBits.intro] < 2) {
                    vars[CamBits.intro] = vars[CamBits.intro] + 1
                    mes("Ramarno explains how the sacred forge powers the city.")
                } else if (listOf(25635, 25637, 25639).all { items.getValue(it) in inv }) assemble()
                else shop()
            }
            onOpNpc3(npc) { shop() }
            onOpNpcU(npc, shards) { fuel() }
            for (part in
                listOf(
                    25633,
                    25635,
                    25637,
                    25639,
                    25641,
                    25644,
                    objs.imcando_hammer_offhand.id,
                )) onOpNpcU(npc, items.getValue(part)) {
                if (!Camdozaal.allowed(player)) return@onOpNpcU
                if (
                    part == 25633 &&
                        choice2(
                            "Repair the broken hammer for 1,250 shards.",
                            true,
                            "Trade it for shards.",
                            false,
                        )
                ) {
                    exchange(25633, it.invSlot, 1250, 25644)
                    return@onOpNpcU
                }
                if (
                    part in listOf(25635, 25637, 25639) &&
                        listOf(25635, 25637, 25639).all { items.getValue(it) in inv } &&
                        choice2(
                            "Assemble the barronite mace.",
                            true,
                            "Trade this piece for shards.",
                            false,
                        )
                ) {
                    assemble()
                    return@onOpNpcU
                }
                val amount =
                    when (part) {
                        25633 -> 350
                        25635,
                        25637,
                        25639 -> 100
                        else -> 500
                    }
                if (choice2("Exchange this item for $amount shards.", true, "Keep my item.", false))
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, part, 1, it.invSlot)
                        add(bag, shards.id, amount)
                    }
            }
        }
        for ((i, button) in CamButtons.buffs.withIndex()) onIfModalButton(button) {
            if (it.op.slot != 1 || vars[CamBits.intro] < 2 || !Camdozaal.allowed(player))
                return@onIfModalButton
            val cost = listOf(2500, 2500, 2500, 2500, 3000, 1000)[i]
            if (vars[CamBits.buffs[i]] > 0) {
                mes("That charm is already active.")
                return@onIfModalButton
            }
            val stored = minOf(vars[CamVars.stored], cost)
            if (cost > stored && !invDel(inv, shards, cost - stored).success) {
                mes("You need $cost barronite shards.")
                return@onIfModalButton
            }
            vars[CamVars.stored] -= stored
            if (i == 5) {
                vars[CamVars.end] = Instant.now().epochSecond.toInt() + 3600
                sync(player)
            } else vars[CamBits.buffs[i]] = 1
        }
        val crusher = locs.getValue(41551)
        onOpLoc1(crusher) { crush(it.loc) }
        onOpLocU(crusher, MiningObjs.barroniteDeposit) { crush(it.loc) }
        for (i in 0..4) {
            val relic = items.getValue(25686 + i * 2)
            onOpNpcU(npcs.getValue(5214), relic) {
                val first = vars[CamBits.relics[i]] == 0
                val tx =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, relic.id, 1, it.invSlot)
                        add(bag, objs.coins.id, if (first) 200 else 100)
                    }
                if (tx.success && first) {
                    vars[CamBits.relics[i]] = 1
                    vars[CamBits.kudos] += 1
                }
            }
        }
    }

    private fun ProtectedAccess.fuel() {
        if (vars[CamBits.intro] < 2 || !Camdozaal.allowed(player)) return
        val amount = minOf(invTotal(inv, shards), Int.MAX_VALUE - vars[CamVars.stored])
        if (amount > 0 && invDel(inv, shards, amount).success) vars[CamVars.stored] += amount
    }

    private fun ProtectedAccess.shop() {
        if (Camdozaal.allowed(player) && vars[CamBits.intro] >= 2) {
            sync(player)
            ifOpenMainModal(CamUi.shop)
        }
    }

    private fun ProtectedAccess.exchange(id: Int, slot: Int, price: Int, result: Int) {
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, id, 1, slot)
                delete(bag, shards.id, price)
                add(bag, result, 1)
            }
        if (!tx.success) mes("You need $price barronite shards in your inventory.")
    }

    private suspend fun ProtectedAccess.assemble() {
        if (
            !choice2(
                "Give Ramarno 1,500 shards and the three mace pieces.",
                true,
                "Keep the pieces.",
                false,
            )
        )
            return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                for (part in listOf(25635, 25637, 25639)) delete(bag, part, 1)
                delete(bag, shards.id, 1500)
                add(bag, 25641, 1)
            }
        if (!tx.success) mes("Bring all three pieces and 1,500 barronite shards.")
    }

    private suspend fun ProtectedAccess.crush(anvil: BoundLocInfo) {
        while (
            Camdozaal.allowed(player) &&
                player.smithingLvl >= 14 &&
                isWithinDistance(anvil, 1) &&
                MiningObjs.barroniteDeposit in inv
        ) {
            val hammers =
                setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
            if (!inv.any { it?.id in hammers } && !player.worn.any { it?.id in hammers }) {
                mes("You need a hammer to crush the deposit.")
                return
            }
            // Relative rare weights use the published head, hammer and five relic rates.
            val luck = Camdozaal.boost(player, 4)
            val roll = random.of(210000)
            val id =
                when {
                    roll < 1050 * luck / 100 -> 25635
                    roll < 1750 * luck / 100 && player.members -> 25633
                    roll in (1750 * luck / 100) until (4750 * luck / 100) ->
                        25686 + 2 * random.of(5)
                    else -> shards.id
                }
            val count =
                if (id != shards.id) 1
                else if (random.of(20) == 0) random.of(15..35) else random.of(6..8)
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, MiningObjs.barroniteDeposit.id, 1)
                    add(bag, id, count)
                }
            if (!tx.success) return
            anim(seqs.human_hammer_hit)
            statAdvance(stats.smithing, 30.0)
            delay(3)
        }
    }
}
