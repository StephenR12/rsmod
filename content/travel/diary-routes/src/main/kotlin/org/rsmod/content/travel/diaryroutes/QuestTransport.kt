package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object QuestRouteLocs : LocReferences() {
    val hollows = find("route_rowboat_hollows")
    val mortton = find("route_rowboat_mortton")
    val volcano = find("volcano_entrance")
    val volcanoRope = find("climbing_rope2")
    val crandor = find("dragon_slayer_qip_ruin_entrance")
    val crandorRope = find("dragon_slayer_qip_climbing_rope")
    val secretWall = find("dragonsecretdoor")
    val manhole = find("vc_manhole_open")
    val sewerLadder = find("vc_ladder")
}

internal object QuestRouteItems : ObjReferences() {
    val tokens = find("ectotoken")
    val charos = find("ring_of_charos_unlocked")
    val ghostspeak =
        listOf(
            find("amulet_of_ghostspeak"),
            find("amulet_of_ghostspeak_enchanted"),
            objs.diary_morytania_legs_2,
            objs.diary_morytania_legs_3,
            objs.diary_morytania_legs_4,
        )
}

internal object QuestRouteNpcs : NpcReferences() {
    val captain = find("ahoy_ghost_captain_1")
    val islandCaptain = find("ahoy_ghost_captain_2")
    val cyreg = find("route_cyreg_paddlehorn")
}

internal object QuestRouteBits : VarBitReferences() {
    val ghostQuest = find("ahoy_questvar")
    val ratQuest = find("ratcatch_var")
    val secretDiscovered = find("dragonslayer_crandor_found_secret_door")
}

internal object QuestRouteVars : VarpReferences() {
    val lifetimeFare = find("dragontooth_lifetime_fare")
    val passedWall = find("crandor_volcano_passage")
    val dragonQuest = find("dragonquest")
    val myreque = find("routequest")
    val natureSpirit = find("druidspirit")
}

internal object QuestRouteSaves : VarpBuilder() {
    init {
        build("dragontooth_lifetime_fare") { transmitNever = true }
        build("crandor_volcano_passage") { transmitNever = true }
    }
}

internal object QuestTransportPlacements : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<QuestTransport>("quest-transport-npcs.toml")
    }
}

public class QuestTransport : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(QuestRouteNpcs.captain) { sailToDragontooth(true) }
        onOpNpc3(QuestRouteNpcs.captain) { sailToDragontooth(false) }
        onOpNpc1(QuestRouteNpcs.islandCaptain) {
            // On the return journey the captain understands the player's gestures.
            if (player.members && choice2("Return to Port Phasmatys.", true, "Stay here.", false)) {
                telejump(CoordGrid(3701, 3487))
            }
        }
        onOpLoc1(QuestRouteLocs.hollows) {
            if (!player.members || vars[QuestRouteVars.natureSpirit] < 110) {
                mes("You should get permission before using this boat.")
                return@onOpLoc1
            }
            telejump(CoordGrid(3522, 3285))
            player.completeDiaryTask(DiaryTask.BoardHollowsBoat)
        }
        onOpLoc1(QuestRouteLocs.mortton) { sailFromMortton(true) }
        onOpLoc2(QuestRouteLocs.mortton) { sailFromMortton(false) }
        onOpNpc1(QuestRouteNpcs.cyreg) { sailFromMortton(true) }

        onOpLoc1(QuestRouteLocs.volcano) {
            vars[QuestRouteVars.passedWall] = 0
            telejump(CoordGrid(2856, 9568))
        }
        onOpLoc1(QuestRouteLocs.volcanoRope) {
            vars[QuestRouteVars.passedWall] = 0
            telejump(CoordGrid(2856, 3170))
        }
        onOpLoc1(QuestRouteLocs.crandor) {
            vars[QuestRouteVars.passedWall] = 0
            telejump(CoordGrid(2833, 9656))
        }
        onOpLoc1(QuestRouteLocs.secretWall) { openWall() }
        onOpLoc1(QuestRouteLocs.crandorRope) {
            telejump(CoordGrid(2833, 3254))
            if (vars[QuestRouteVars.passedWall] != 0) {
                player.completeDiaryTask(DiaryTask.VisitCrandorByDungeon)
                vars[QuestRouteVars.passedWall] = 0
            }
        }
        onOpLoc1(QuestRouteLocs.manhole) {
            val sarim = it.loc.coords == CoordGrid(3018, 3232)
            val varrock = it.loc.coords == CoordGrid(3267, 3400)
            if (!sarim && !varrock) return@onOpLoc1
            if (!player.members || vars[QuestRouteBits.ratQuest] < (if (sarim) 105 else 90)) {
                mes("You have not been invited into these Rat Pits.")
                return@onOpLoc1
            }
            telejump(if (sarim) CoordGrid(2962, 9650) else CoordGrid(2896, 5097))
            if (sarim) player.completeDiaryTask(DiaryTask.EnterSarimRatPits)
        }
        onOpLoc1(QuestRouteLocs.sewerLadder) { exitPits(it.loc.coords) }
        onApLoc1(QuestRouteLocs.sewerLadder) {
            if (isWithinApRange(it.loc, 1)) exitPits(it.loc.coords)
        }
    }

    private fun ProtectedAccess.exitPits(ladder: CoordGrid) {
        when (ladder) {
            CoordGrid(2962, 9651) -> telejump(CoordGrid(3018, 3231))
            CoordGrid(2895, 5097) -> telejump(CoordGrid(3267, 3399))
            CoordGrid(2661, 9641) -> telejump(CoordGrid(2561, 3319))
        }
    }

    private suspend fun ProtectedAccess.sailToDragontooth(talk: Boolean) {
        if (!player.members || QuestRouteItems.ghostspeak.none { it in worn }) {
            mes("The ghost captain wails incomprehensibly.")
            return
        }
        val unlimited = vars[QuestRouteVars.lifetimeFare] != 0
        val fare =
            when {
                unlimited -> 0
                QuestRouteItems.charos in worn && vars[QuestRouteBits.ghostQuest] >= 8 -> 10
                else -> 25
            }
        var charge = fare
        if (talk) {
            if (unlimited) {
                if (!choice2("Travel to Dragontooth Island.", true, "Stay here.", false)) return
            } else {
                when (
                    menu(
                        "Dragontooth Island",
                        true,
                        listOf(
                            "Pay $fare ecto-tokens for a return trip.",
                            "Pay 500 ecto-tokens for unlimited trips.",
                            "Stay here.",
                        ),
                    )
                ) {
                    0 -> Unit
                    1 -> charge = 500
                    else -> return
                }
            }
        }
        if (charge > 0 && !invDel(inv, QuestRouteItems.tokens, count = charge).success) {
            mes("You need $charge ecto-tokens for this fare.")
            return
        }
        if (charge == 500) vars[QuestRouteVars.lifetimeFare] = 1
        telejump(CoordGrid(3791, 3560))
        player.completeDiaryTask(DiaryTask.VisitDragontooth)
    }

    private suspend fun ProtectedAccess.sailFromMortton(talk: Boolean) {
        if (!player.members || vars[QuestRouteVars.myreque] < 20) {
            mes("Cyreg has not given you permission to use his boat.")
            return
        }
        if (invTotal(inv, objs.coins) < 10) {
            mes("The fare is ten coins.")
            return
        }
        val free = talk && QuestRouteItems.charos in worn
        if (
            talk &&
                !choice2(
                    if (free) "Charm Cyreg into a free passage." else "Pay ten coins for passage.",
                    true,
                    "Stay here.",
                    false,
                )
        )
            return
        if (!free && !invDel(inv, objs.coins, count = 10).success) return
        telejump(CoordGrid(3496, 3379))
    }

    private fun ProtectedAccess.openWall() {
        val fromCrandor = coords.z >= 9600
        val stage = vars[QuestRouteVars.dragonQuest]
        if (
            vars[QuestRouteBits.secretDiscovered] == 0 && stage < 10 && !(fromCrandor && stage >= 8)
        ) {
            mes("The wall will not budge.")
            return
        }
        vars[QuestRouteBits.secretDiscovered] = 1
        vars[QuestRouteVars.passedWall] = if (fromCrandor) 0 else 1
        telejump(CoordGrid(2836, if (fromCrandor) 9599 else 9600))
        player.completeDiaryTask(DiaryTask.DiscoverCrandorWall)
    }
}
