package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ApeLocs : LocReferences() {
    val stages =
        listOf(
                listOf("stepping_stone"),
                listOf("climbable_tree"),
                listOf("monkeybars_start", "monkeybars_end"),
                listOf("cliff_climb_1"),
                listOf("rope_swing"),
                listOf("agility_tree_base", "sliding_vine"),
            )
            .map { names -> names.map { find("100_ilm_$it") } }
}

internal object ApeGreegrees : ObjReferences() {
    val items =
        listOf(
            find("mm_monkey_greegree_for_small_ninja_monkey"),
            find("mm_monkey_greegree_for_medium_ninja_monkey"),
            find("mm2_kruk_greegree"),
        ) +
            listOf(
                    "normal_monkey",
                    "normal_gorilla",
                    "bearded_gorilla",
                    "ancient_monkey_skull",
                    "small_zombie_monkey",
                    "large_zombie_monkey",
                )
                .map { find("mm_monkey_greegree_for_$it") }
}

internal object ApeForms : NpcReferences() {
    val forms =
        listOf(
            find("mm_transmogrification_small_ninja_monkey"),
            find("mm_transmogrification_medium_ninja_monkey"),
            find("mm2_kruk_combat"),
        ) +
            listOf(
                    "normal_monkey",
                    "normal_gorilla",
                    "bearded_gorilla",
                    "ancient_monkey",
                    "small_zombie_monkey",
                    "large_zombie_monkey",
                )
                .map { find("mm_transmogrification_$it") }
}

internal object ApeCourseVars : VarpReferences() {
    val stage = find("ape_agility_stage")
    val laps = find("ape_agility_laps")
}

internal object ApeCourseVarBuilder : VarpBuilder() {
    init {
        build("ape_agility_stage") { transmitNever = true }
        build("ape_agility_laps") { transmitNever = true }
    }
}

internal object ApeCourseBits : VarBitReferences() {
    val mm2 = find("mm2_progress")
}

internal object ApeCourseTimers : TimerReferences() {
    val form = find("ape_agility_monkey_form")
}

public class ApeAtollCourse @Inject constructor(private val marks: CourseMarks) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((i, types) in ApeLocs.stages.withIndex()) for (type in types) {
            onOpLoc1(type) { traverse(i, it.loc) }
            onApLoc1(type) {
                if (isWithinApRange(it.loc, if (i == 2 || i == 4) 2 else 1)) traverse(i, it.loc)
            }
        }
        for (item in ApeGreegrees.items) onOpHeld2(item) {
            if (isInCombat()) {
                mes("You cannot use a greegree in combat.")
                return@onOpHeld2
            }
            if (!player.members || !canTransform(coords)) {
                mes("The greegree has no effect here.")
                return@onOpHeld2
            }
            invEquip(it.slot)
            refreshForm()
        }
        onPlayerLogin {
            if (canTransform(player.coords) && ApeGreegrees.items.any { it in player.worn })
                player.timer(ApeCourseTimers.form, 1)
        }
        onPlayerTimer(ApeCourseTimers.form) { refreshForm() }
    }

    private fun onIsland(tile: CoordGrid) = tile.x in 2688..2815 && tile.z in 2688..2815

    private fun canTransform(tile: CoordGrid) =
        onIsland(tile) || (tile.x in 2496..2559 && tile.z in 9152..9215) ||
            (tile.x in 2880..2943 && tile.z in 9088..9151) || (tile.x in 2591..2623 && tile.z in 3257..3290 && tile.level == 0)

    private fun ProtectedAccess.refreshForm() {
        val i = ApeGreegrees.items.indexOfFirst { it in worn }
        if (i >= 0 && canTransform(coords)) {
            val ridingGorilla = ApeGreegrees.items[i].id == 19525 && player.transmog?.id == 7099 &&
                coords.x in 2880..2943 && coords.z in 9088..9151
            if (!ridingGorilla && player.transmog?.id != ApeForms.forms[i].id) transmog(ApeForms.forms[i])
            timer(ApeCourseTimers.form, 1)
        } else {
            if (ApeForms.forms.any { it.id == player.transmog?.id }) player.transmog = null
            player.clearTimer(ApeCourseTimers.form)
        }
    }

    private suspend fun ProtectedAccess.traverse(i: Int, loc: BoundLocInfo) {
        if (!player.members || !onIsland(coords)) return
        if (statBase(stats.agility) < 48) {
            mes("You need an Agility level of 48 to attempt the course.")
            return
        }
        val equipped = ApeGreegrees.items.take(3).any { it in worn }
        val failed =
            !equipped || (i in 1..4 && random.of(100) < (75 - stat(stats.agility)).coerceAtLeast(0))
        if (failed) {
            if (!equipped) mes("Only the stealthiest and most agile monkey can use this!")
            crossObstacle(START, ArenaMoves.step, 3)
            takeInstantHit(HitType.Typeless, 1 + random.of(4))
            return
        }
        refreshForm()
        faceLoc(loc)
        val finish =
            listOf(
                CoordGrid(2753, 2742),
                CoordGrid(2753, 2742, 2),
                CoordGrid(2747, 2741),
                CoordGrid(2743, 2741),
                CoordGrid(2756, 2732),
                CoordGrid(2770, 2747),
            )[i]
        val action =
            listOf(
                ArenaMoves.step,
                GnomeSequences.climb,
                ArenaMoves.bars,
                GnomeSequences.climb,
                GnomeSequences.swing,
                GnomeSequences.descend,
            )[i]
        crossObstacle(finish, action, listOf(3, 5, 7, 5, 5, 8)[i])
        val xp = listOf(40.0, 40.0, 40.0, 60.0, 100.0, 0.0)[i]
        if (xp > 0) statAdvance(stats.agility, xp)
        if (i == 0) vars[ApeCourseVars.stage] = 1
        else if (vars[ApeCourseVars.stage] == i) {
            vars[ApeCourseVars.stage] = (i + 1) % 6
            if (i == 5) {
                statAdvance(stats.agility, 300.0)
                player.completeDiaryTask(DiaryTask.CompleteApeAtollLap)
                if (vars[ApeCourseBits.mm2] >= 195) vars[ApeCourseVars.laps]++
                recordLap(AgilityLap.Ape)
                marks.roll(this, 48, finish)
            }
        }
    }

    private companion object {
        val START = CoordGrid(2755, 2742)
    }
}
