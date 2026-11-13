package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EagleBits : VarBitReferences() {
    val quest = find("eaglepeak_quest")
    val desert = find("eaglepeak_unblocked_desert")
    val jungle = find("eaglepeak_unblocked_jungle")
    val vine = find("eaglepeak_jungle_vine")
}

internal object EagleVars : VarpReferences() {
    val planted = find("eagle_vine_planted_minute")
}

internal object EagleSave : VarpBuilder() {
    init {
        build("eagle_vine_planted_minute") { transmitNever = true }
    }
}

internal object EagleTimers : TimerReferences() {
    val growth = find("eagle_vine_growth")
}

internal object EagleRopes : MapObjSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<EagleTravel>("eagle-ropes.toml")
    }
}

internal object EagleItems : ObjReferences() {
    val supports =
        listOf(
                "fenk_cane",
                "hunting_teasing_stick",
                "thatching_spar_light",
                "thatching_spar_med",
                "thatching_spar_dense",
            )
            .map { find(it) }
}

internal object EagleNpcs : NpcReferences() {
    val desert = find("eaglepeak_eagle_todesert")
    val jungle = find("eaglepeak_eagle_tojungle")
    val polar = find("eaglepeak_eagle_topolar")
}

internal object EagleLocs : LocReferences() {
    val peak =
        listOf(find("eaglepeak_cave_entrance_open"), find("eaglepeak_new_cave_entrance_open"))
    val peakExit = find("eaglepeak_human_exitmid")
    val doors =
        listOf(
                "eaglepeak_gate_0_feather",
                "eaglepeak_gate_1_feather",
                "eaglepeak_gate_0_feather_mirror",
                "eaglepeak_gate_1_feather_mirror",
                "eaglepeak_gate_2_feather_mirror",
            )
            .map { find(it) }
    val jungleIn = find("eaglepeak_cavein_jungle")
    val jungleOut = find("eaglepeak_jungle_cave_entrance")
    val polarIn = find("eaglepeak_cavein_ice")
    val polarOut = find("eaglepeak_ice_cave_entrance")
    val desertIn = find("eaglepeak_cavein_desert")
    val desertOut = find("eaglepeak_desert_cave_exit")
    val handholds =
        listOf(find("hunting_polar_climbing_rocks"), find("hunting_polar_climbing_rocks_inactive"))
    val vines =
        listOf(
                "eagles_peak_vine_small",
                "eagles_peak_vine_small_w_cane",
                "eagles_peak_vine_medium",
                "eagles_peak_vine_tall",
                "eaglepeak_farming_vines_fullygrown",
            )
            .map { find(it) }
}

private enum class EagleJourney(val hub: CoordGrid, val eyrie: CoordGrid, val diary: DiaryTask) {
    Desert(CoordGrid(2019, 4958, 3), CoordGrid(3421, 9571), DiaryTask.FlyEagleToDesert),
    Jungle(CoordGrid(2022, 4961, 3), CoordGrid(2515, 9322), DiaryTask.FlyPeakToFeldip),
    Polar(CoordGrid(2025, 4963, 3), CoordGrid(2726, 10217), DiaryTask.FlyEagleToSnow),
}

public class EagleTravel : PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, route) in
            mapOf(
                EagleNpcs.desert to EagleJourney.Desert,
                EagleNpcs.jungle to EagleJourney.Jungle,
                EagleNpcs.polar to EagleJourney.Polar,
            )) {
            onOpNpc1(npc) { fly(route, it.npc.coords) }
            onApNpc1(npc) { if (isWithinApRange(it.npc, 2)) fly(route, it.npc.coords) }
            onOpNpcU(npc, objs.rope) { fly(route, it.npc.coords) }
        }
        for (cave in EagleLocs.peak) {
            onOpLoc1(cave) { enterPeak() }
            onApLoc1(cave) { if (isWithinApRange(it.loc, 2)) enterPeak() }
        }
        onOpLoc1(EagleLocs.peakExit) { telejump(CoordGrid(2327, 3496)) }
        onApLoc1(EagleLocs.peakExit) {
            if (isWithinApRange(it.loc, 1)) telejump(CoordGrid(2327, 3496))
        }
        for (door in EagleLocs.doors) onOpLoc1(door) {
            if (player.members && vars[EagleBits.quest] >= 40)
                telejump(CoordGrid(if (coords.x < 2003) 2003 else 2002, it.loc.z, 3))
            else mes("The stone door is sealed.")
        }
        onOpLoc1(EagleLocs.jungleIn) { climbVine() }
        onApLoc1(EagleLocs.jungleIn) { if (isWithinApRange(it.loc, 2)) climbVine() }
        for (vine in EagleLocs.vines) {
            onOpLoc1(vine) { climbVine() }
            onApLoc1(vine) { if (isWithinApRange(it.loc, 2)) climbVine() }
        }
        for (support in EagleItems.supports) onOpLocU(EagleLocs.vines.first(), support) {
            if (!player.members || vars[EagleBits.quest] < 40 || vars[EagleBits.vine] != 0)
                return@onOpLocU
            if (!invDel(inv, support, slot = it.invSlot).success) return@onOpLocU
            vars[EagleVars.planted] = now()
            vars[EagleBits.vine] = 1
            timer(EagleTimers.growth, 100)
            mes("You train the young vine around its support.")
        }
        onOpLoc1(EagleLocs.jungleOut) { telejump(CoordGrid(2514, 2927)) }
        onApLoc1(EagleLocs.jungleOut) {
            if (isWithinApRange(it.loc, 2)) telejump(CoordGrid(2514, 2927))
        }
        onPlayerLogin {
            if (player.vars[EagleVars.planted] > 0) {
                updateVine(player)
                if (player.vars[EagleBits.vine] < 3) player.timer(EagleTimers.growth, 100)
            }
        }
        onPlayerTimer(EagleTimers.growth) { updateVine(player) }
        for (rocks in EagleLocs.handholds) {
            onOpLoc1(rocks) { climbPolar() }
            onApLoc1(rocks) { if (isWithinApRange(it.loc, 2)) climbPolar() }
        }
        onOpLoc1(EagleLocs.polarIn) { if (player.members) telejump(CoordGrid(2707, 10204)) }
        onApLoc1(EagleLocs.polarIn) {
            if (player.members && isWithinApRange(it.loc, 1)) telejump(CoordGrid(2707, 10204))
        }
        onOpLoc1(EagleLocs.polarOut) { telejump(CoordGrid(2744, 3830)) }
        onApLoc1(EagleLocs.polarOut) {
            if (isWithinApRange(it.loc, 2)) telejump(CoordGrid(2744, 3830))
        }
        onOpLoc1(EagleLocs.desertIn) { enterDesert() }
        onApLoc1(EagleLocs.desertIn) { if (isWithinApRange(it.loc, 1)) enterDesert() }
        onOpLoc1(EagleLocs.desertOut) { exitDesert() }
        onApLoc1(EagleLocs.desertOut) { if (isWithinApRange(it.loc, 2)) exitDesert() }
    }

    private fun ProtectedAccess.fly(route: EagleJourney, bird: CoordGrid) {
        if (!player.members || vars[EagleBits.quest] < 40) {
            mes("You have not learned to travel with eagles.")
            return
        }
        if (objs.rope !in inv) {
            mes("You need a rope to lasso the eagle.")
            return
        }
        val fromPeak = bird.level == 3 && bird.mx == 31 && bird.mz == 77
        if (!fromPeak && (bird.level != 0 || bird.chebyshevDistance(route.eyrie) > 20)) return
        telejump(if (fromPeak) route.eyrie else route.hub)
        if (fromPeak) player.completeDiaryTask(route.diary)
    }

    private fun ProtectedAccess.enterPeak() {
        if (player.members && vars[EagleBits.quest] >= 10) telejump(CoordGrid(1994, 4983, 3))
    }

    private fun ProtectedAccess.climbVine() {
        updateVine(player)
        if (!player.members || vars[EagleBits.vine] < 3) {
            mes("The vine is not strong enough to climb.")
            return
        }
        telejump(CoordGrid(2526, 9324))
    }

    private fun ProtectedAccess.climbPolar() {
        if (!player.members || stat(stats.agility) < 35) {
            mes("You need 35 Agility to climb the handholds.")
            return
        }
        val up = coords.x <= 2741
        telejump(if (up) CoordGrid(2744, 3830) else CoordGrid(2740, 3830, 1))
    }

    private fun ProtectedAccess.enterDesert() {
        if (!player.members) return
        if (vars[EagleBits.desert] == 0 && stat(stats.strength) < 45) {
            mes("You need 45 Strength to move the boulder blocking the cave.")
            return
        }
        vars[EagleBits.desert] = 1
        telejump(CoordGrid(3420, 9562))
    }

    private fun ProtectedAccess.exitDesert() {
        if (vars[EagleBits.desert] == 0 && stat(stats.strength) < 45) {
            mes("A boulder blocks the exit.")
            return
        }
        vars[EagleBits.desert] = 1
        telejump(CoordGrid(3405, 3159))
    }

    private fun updateVine(player: Player) {
        val planted = player.vars[EagleVars.planted]
        if (planted == 0) {
            player.clearTimer(EagleTimers.growth)
            return
        }
        val minutes = now() - planted
        if (minutes >= 40) {
            VarPlayerIntMapSetter.set(player, EagleBits.vine, 3)
            VarPlayerIntMapSetter.set(player, EagleBits.jungle, 1)
            VarPlayerIntMapSetter.set(player, EagleVars.planted, 0)
            player.clearTimer(EagleTimers.growth)
            player.mes("Those vines should be worth checking on now.")
        } else if (minutes >= 5 && player.vars[EagleBits.vine] == 1)
            VarPlayerIntMapSetter.set(player, EagleBits.vine, 2)
    }

    private fun now(): Int = (System.currentTimeMillis() / 60_000).toInt()
}
