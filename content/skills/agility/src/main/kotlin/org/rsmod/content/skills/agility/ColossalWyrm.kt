package org.rsmod.content.skills.agility

import org.rsmod.api.shops.onOmniShopButton
import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WyrmBits : VarBitReferences() {
    val met = find("varlamore_wyrm_agility_met_guide")
    val basic = find("varlamore_wyrm_agility_basic_progress")
    val advanced = find("varlamore_wyrm_agility_advanced_progress")
    val termite = find("varlamore_wyrm_agility_termite_obstacle")
    val visited = find("varlamore_visited")
}

internal object WyrmVars : VarpReferences() {
    val best = listOf(find("wyrm_basic_best_cycles"), find("wyrm_advanced_best_cycles"))
    val selected = find("omnishop_selected_id")
    val last = find("omnishop_lastshop")
    val redraw = find("omnishop_redraw")
}

internal object WyrmVarBuilder : VarpBuilder() {
    init {
        for (name in listOf("wyrm_basic_best_cycles", "wyrm_advanced_best_cycles")) build(name) {
            permanent = true
            transmitNever = true
        }
    }
}

internal object WyrmSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<WyrmSpawns>("wyrm.toml")
    }
}

internal object WyrmNpcs : NpcReferences() {
    val guide = find("varlamore_wyrm_agility_guide")
    val intro = find("varlamore_wyrm_agility_guide_1op")
    val normal = find("varlamore_wyrm_agility_guide_3op")
}

internal object WyrmItems : ObjReferences() {
    val termite = find("varlamore_wyrm_agility_termite")
    val bone = find("blessed_bone_shard")
    val scroll = find("teleportscroll_colossal_wyrm")
    val acorn = find("calcified_acorn")
    val pieces =
        listOf("hood", "top", "legs", "gloves", "boots", "cape").map {
            find("graceful_$it") to find("graceful_${it}_wyrm")
        }
}

internal object WyrmMoves : SeqReferences() {
    val balance = find("human_walk_logbalance_loop")
    val jump = find("wyrm_agility_longjump")
    val small = find("wyrm_agility_shortjump")
    val bars = find("human_monkeybars_walk")
    val slide = find("wyrm_agility_zipline_walk")
}

internal object WyrmLocs : LocReferences() {
    val names =
        listOf(
            "start_ladder",
            "balance_1",
            "multijump",
            "basic_balance_1",
            "basic_monkeybars_1",
            "basic_jump_1",
            "basic_ladder_1",
            "advanced_ladder_1",
            "advanced_jump_1",
            "advanced_balance_1",
            "advanced_monkeybars_1",
            "end_zipline",
        )
    val course = names.map { find("varlamore_wyrm_agility_${it}_trigger") }
}

internal object WyrmShop : InterfaceReferences() {
    val shop = find("omnishop_main")
}

internal object WyrmShopButtons : ComponentReferences() {
    val list = find("omnishop_main:list")
    val buy = (1..4).map { find("omnishop_main:button_$it") }
}

public class ColossalWyrm @Inject constructor() : PluginScript() {
    private val starts = WeakHashMap<Player, Int>()
    private val termSpots = listOf(1, 2, 3, 4, 5, 8, 9, 10)

    override fun ScriptContext.startup() {
        for (guide in listOf(WyrmNpcs.intro, WyrmNpcs.normal)) onOpNpc1(guide) {
            if (!player.members) return@onOpNpc1
            if (vars[WyrmBits.met] == 0) {
                if (
                    choice2(
                        "Help collect termites from the scaffolding.",
                        true,
                        "Maybe later.",
                        false,
                    )
                ) {
                    vars[WyrmBits.met] = 1
                    mes(
                        "Start at the ladder beside the wyrm's head. You'll gather termites as you cross the scaffolding."
                    )
                }
            } else if (choice2("Browse your wares.", true, "Check my course records.", false))
                shop()
            else records()
        }
        onOpNpc3(WyrmNpcs.normal) { shop() }
        onOpNpc4(WyrmNpcs.normal) { records() }
        for (i in listOf(0, 1, 3, 4, 6, 7, 8, 9, 11)) {
            val type = WyrmLocs.course[i]
            onOpLoc1(type) { cross(i) }
            onApLoc1(type) { if (isWithinApRange(it.loc, 1)) cross(i) }
        }
        onPlayerLogout { starts.remove(player) }
        onOmniShopButton(SHOP,WyrmShopButtons.list) {
            if (vars[WyrmVars.last] != SHOP) return@onOmniShopButton
            val index = it.comsub / 12
            if (it.comsub < 0 || index !in 0..3) return@onOmniShopButton
            vars[WyrmVars.selected] = index
            when (it.op.slot) {
                1 -> runClientScript(7146, SHOP, -1, index, 0, 0, 0, -1, "", 1)
                2 -> purchase(index, 1)
                3 -> purchase(index, 5)
                4 -> purchase(index, 10)
                5 -> purchase(index, 50)
            }
        }
        for ((n, button) in WyrmShopButtons.buy.withIndex()) onOmniShopButton(SHOP,button) {
            if (vars[WyrmVars.last] == SHOP && it.op.slot == 1)
                purchase(vars[WyrmVars.selected], listOf(1, 5, 10, 50)[n])
        }
        onOpHeld1(WyrmItems.scroll) {
            if (!player.members || vars[WyrmBits.visited] == 0) {
                mes("You need to have visited Varlamore first.")
                return@onOpHeld1
            }
            val wildBase = if (coords.z >= 9920) 9920 else 3520
            if (
                player.isTeleportBlocked() ||
                    vars[varbits.inside_wilderness] != 0 && (coords.z - wildBase) / 8 + 1 > 20
            ) {
                mes("A magical force stops you from teleporting.")
                return@onOpHeld1
            }
            if (invDel(inv, WyrmItems.scroll).failure) return@onOpHeld1
            anim(seqs.teleport_casting)
            delay(3)
            teleport(CoordGrid(1641, 2921))
        }
    }

    private suspend fun ProtectedAccess.cross(i: Int) {
        if (!player.members || vars[WyrmBits.met] == 0) {
            mes("Speak to Worm Tongue before climbing onto the scaffolding.")
            return
        }
        val adv = i in 7..10
        val level = if (adv) 62 else 50
        if (stat(stats.agility) < level) {
            mes("You need level $level Agility for this course.")
            return
        }
        val basic = vars[WyrmBits.basic]
        val upper = vars[WyrmBits.advanced]
        when (i) {
            0 -> {
                starts[player] = player.currentMapClock
                vars[WyrmBits.advanced] = 0
                // The published observed yield corresponds to about one swarm per three laps across
                // both paths.
                vars[WyrmBits.termite] =
                    if (random.of(100) < 48) termSpots[random.of(termSpots.size)] else 0
                stage(1653, 2931, 1, WolfMoves.climb, 4, 37.2)
                vars[WyrmBits.basic] = 1
            }
            1 ->
                if (basic == 1) {
                    stage(1655, 2918, 1, WyrmMoves.balance, 10, 37.2)
                    scoop(1)
                    for ((x, z) in
                        listOf(
                            1655 to 2915,
                            1654 to 2913,
                            1653 to 2912,
                            1652 to 2911,
                            1651 to 2910,
                            1649 to 2910,
                        )) {
                        stage(x, z, 1, WyrmMoves.small, 2, 6.2)
                    }
                    scoop(2)
                    vars[WyrmBits.basic] = 3
                }
            3 ->
                if (basic == 3 && upper == 0) {
                    stage(1635, 2910, 1, WyrmMoves.balance, 13, 37.2)
                    scoop(3)
                    vars[WyrmBits.basic] = 4
                }
            4 ->
                if (basic == 4) {
                    stage(1627, 2914, 1, WyrmMoves.bars, 10, 37.2)
                    scoop(4)
                    stage(1627, 2931, 1, WyrmMoves.jump, 13, 37.2)
                    scoop(5)
                    vars[WyrmBits.basic] = 6
                }
            6 ->
                if (basic == 6) {
                    stage(1625, 2932, 2, WolfMoves.climb, 4, 37.2)
                    vars[WyrmBits.basic] = 7
                }
            7 ->
                if (basic == 3 && upper == 0) {
                    stage(1648, 2908, 2, WolfMoves.climb, 4, 70.0)
                    vars[WyrmBits.basic] = 0
                    vars[WyrmBits.advanced] = 1
                }
            8 ->
                if (upper == 1) {
                    stage(1635, 2907, 2, WyrmMoves.jump, 14, 70.0)
                    scoop(8)
                    vars[WyrmBits.advanced] = 2
                }
            9 ->
                if (upper == 2) {
                    stage(1625, 2916, 2, WyrmMoves.balance, 14, 70.0)
                    scoop(9)
                    stage(1624, 2931, 2, WyrmMoves.bars, 17, 70.0)
                    scoop(10)
                    vars[WyrmBits.advanced] = 4
                }
            11 ->
                if (basic == 7 || upper == 4) {
                    val advanced = upper == 4
                    stage(1645, 2933, 0, WyrmMoves.slide, 12, if (advanced) 358.0 else 243.7)
                    recordLap(if (advanced) AgilityLap.WyrmAdvanced else AgilityLap.WyrmBasic)
                    val best = WyrmVars.best[if (advanced) 1 else 0]
                    starts.remove(player)?.let {
                        val time = player.currentMapClock - it
                        if (vars[best] == 0 || time < vars[best]) vars[best] = time
                    }
                    vars[WyrmBits.basic] = 0
                    vars[WyrmBits.advanced] = 0
                    vars[WyrmBits.termite] = 0
                }
        }
    }

    private suspend fun ProtectedAccess.stage(
        x: Int,
        z: Int,
        plane: Int,
        move: org.rsmod.game.type.seq.SeqType,
        ticks: Int,
        xp: Double,
    ) {
        crossObstacle(CoordGrid(x, z, plane), move, ticks)
        statAdvance(stats.agility, xp)
    }

    private fun ProtectedAccess.scoop(spot: Int) {
        if (vars[WyrmBits.termite] != spot) return
        val n = if (spot >= 8) random.of(12..15) else random.of(8..10)
        val bonus = if (random.of(100) < 80) random.of(22..28) else 0
        val reward =
            player.invTransaction(inv) {
                val bag = select(inv)
                add(bag, WyrmItems.termite.id, n)
                if (bonus > 0) add(bag, WyrmItems.bone.id, bonus)
            }
        vars[WyrmBits.termite] = 0
        if (reward.success) mes("You scoop up $n termites.")
        else mes("Your inventory is too full to collect the termites.")
    }

    private fun ProtectedAccess.records() {
        for ((i, course) in listOf(AgilityLap.WyrmBasic, AgilityLap.WyrmAdvanced).withIndex()) {
            val ticks = vars[WyrmVars.best[i]]
            mes(
                "${course.label}: ${vars[LapVars.counts.getValue(course)]} laps. Best time: ${if(ticks==0)"none" else "${ticks*6/10}.${ticks*6%10}s"}."
            )
        }
    }

    private fun ProtectedAccess.shop() {
        if (!player.members || vars[WyrmBits.met] == 0) return
        vars[WyrmVars.selected] = -1
        vars[WyrmVars.last] = SHOP
        ifOpenMainModal(WyrmShop.shop)
        runClientScript(7140, SHOP, -1)
        ifSetEvents(
            WyrmShopButtons.list,
            0..47,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
        )
        for (button in WyrmShopButtons.buy) ifSetEvents(button, -1..0, IfEvent.Op1)
    }

    private fun ProtectedAccess.purchase(i: Int, requested: Int) {
        if (!player.members || vars[WyrmBits.met] == 0 || i !in 0..3) return
        val cost = listOf(100, 40, 650, 900)[i]
        val n =
            minOf(requested, invTotal(inv, WyrmItems.termite) / cost, if (i == 2) 1 else requested)
        if (n == 0) {
            mes("You need more termites.")
            return
        }
        val exchange =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, WyrmItems.termite.id, n * cost)
                if (i == 2)
                    for ((from, to) in WyrmItems.pieces) {
                        delete(bag, from.id, 1)
                        add(bag, to.id, 1)
                    }
                else
                    add(
                        bag,
                        when (i) {
                            0 -> GraceItems.pack
                            1 -> WyrmItems.scroll
                            else -> WyrmItems.acorn
                        }.id,
                        n,
                    )
            }
        if (exchange.failure) {
            mes(
                if (i == 2) "Bring a full uncoloured graceful outfit in your inventory."
                else "You need more inventory space."
            )
            return
        }
        vars[WyrmVars.redraw]++
    }

    private companion object {
        const val SHOP = 3764
    }
}
