package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.firemaking.LightSourceRecipes
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DorgeshCourseLocs : LocReferences() {
    val cable = find("dorgesh_cablebalance_start")
    val swing = find("dorgesh_caves_rope_swing")
    val bars = find("dorgesh_caves_monkeybars_end")
    val wall = find("dorgesh_cave_juttingwall")
    val pipe = find("dorgesh_caves_pipe_agility2")
    val pylon = find("dorgesh_pylon_grapple_base")
    val climb = find("dorgesh_cavewall_slope_steps")
    val ladder = find("dorgesh_caves_ladder_down")
    val currentUp = find("dorgesh_generator_steps2")
    val currentDown = find("dorgesh_generator_steps_blank_top")
    val oldUp = find("dorgesh_old_generator_steps2")
    val oldDown = find("dorgesh_old_generator_steps_blank_top")
    val boiler = find("dorgesh_old_generator_boiler")
    val console = find("dorgesh_old_generator_console")
}

internal object DorgeshCourseNpc : NpcReferences() {
    val turgall = find("dorgesh_male_engineer")
}

internal object DorgeshCourseBits : VarBitReferences() {
    val quest = find("dttd_main")
    val heavy = find("dorgesh_agility_part_heavy")
    val delicate = find("dorgesh_agility_part_delicate")
    val returning = find("dorgesh_agility_course_direction")
}

internal object DorgeshCourseVars : VarpReferences() {
    val progress = find("dorgesh_agility_progress")
}

internal object DorgeshCourseVarBuilder : VarpBuilder() {
    init {
        build("dorgesh_agility_progress") { transmitNever = true }
    }
}

internal object DorgeshCourseItems : ObjReferences() {
    val spanner = find("dorgesh_spanner")
    val parts =
        listOf("powerbox", "lever", "cog", "battery", "fuse", "meter").map {
            find("dorgesh_powerstation_$it")
        }
    val broken =
        listOf("powerbox", "lever", "cog", "battery", "fuse", "meter").map {
            find("dorgesh_powerstation_${it}_broken")
        }
    val permanent =
        listOf(
                "wint_torch",
                "wint_torch_offhand",
                "seers_headband_easy",
                "seers_headband_medium",
                "seers_headband_hard",
                "seers_headband_elite",
                "skillcape_firemaking",
                "skillcape_firemaking_trimmed",
            )
            .map { find(it) }
}

private data class DorgeshObstacle(
    val type: LocType,
    val a: CoordGrid,
    val b: CoordGrid,
    val anchors: List<CoordGrid>,
    val xp: Double,
    val ticks: Int,
    val move: SeqType,
)

private fun tile(x: Int, z: Int) = CoordGrid(x, z, 3)

private val route: List<DorgeshObstacle> =
    with(DorgeshCourseLocs) {
        listOf(
            DorgeshObstacle(
                cable,
                tile(2721, 5241),
                tile(2721, 5230),
                listOf(tile(2721, 5240), tile(2721, 5231)),
                25.0,
                7,
                GnomeSequences.balance,
            ),
            DorgeshObstacle(
                swing,
                tile(2727, 5228),
                tile(2734, 5228),
                listOf(tile(2729, 5228), tile(2729, 5229)),
                22.0,
                5,
                GnomeSequences.swing,
            ),
            DorgeshObstacle(
                bars,
                tile(2740, 5225),
                tile(2740, 5214),
                listOf(tile(2740, 5224), tile(2740, 5215)),
                25.0,
                8,
                ArenaMoves.bars,
            ),
            DorgeshObstacle(
                swing,
                tile(2734, 5215),
                tile(2727, 5215),
                listOf(tile(2728, 5215), tile(2728, 5216)),
                22.0,
                5,
                GnomeSequences.swing,
            ),
            DorgeshObstacle(
                wall,
                tile(2721, 5216),
                tile(2721, 5212),
                listOf(tile(2721, 5214)),
                7.5,
                4,
                GnomeSequences.climb,
            ),
            DorgeshObstacle(
                pipe,
                tile(2721, 5209),
                tile(2721, 5205),
                listOf(tile(2721, 5206)),
                7.5,
                4,
                GnomeSequences.crawl,
            ),
            DorgeshObstacle(
                cable,
                tile(2720, 5205),
                tile(2720, 5194),
                listOf(tile(2720, 5204), tile(2720, 5195)),
                25.0,
                7,
                GnomeSequences.balance,
            ),
            DorgeshObstacle(
                bars,
                tile(2718, 5194),
                tile(2707, 5194),
                listOf(tile(2717, 5192), tile(2708, 5192)),
                25.0,
                8,
                ArenaMoves.bars,
            ),
            DorgeshObstacle(
                pylon,
                tile(2719, 5242),
                tile(2709, 5242),
                listOf(tile(2710, 5242)),
                18.0,
                7,
                GrappleSequences.grapple,
            ),
            DorgeshObstacle(
                pylon,
                tile(2704, 5233),
                tile(2704, 5223),
                listOf(tile(2704, 5224)),
                18.0,
                7,
                GrappleSequences.grapple,
            ),
            DorgeshObstacle(
                pylon,
                tile(2703, 5221),
                tile(2703, 5211),
                listOf(tile(2703, 5212)),
                18.0,
                7,
                GrappleSequences.grapple,
            ),
        )
    }

public class DorgeshKaanCourse
@Inject
constructor(private val objects: ObjTypeList, private val marks: CourseMarks) : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in route.map { it.type }.distinct()) {
            onOpLoc1(type) { cross(it.loc) }
            onApLoc1(type) {
                if (isWithinApRange(it.loc, if (type == DorgeshCourseLocs.pylon) 10 else 2))
                    cross(it.loc)
            }
        }
        onOpLoc1(DorgeshCourseLocs.climb) {
            if (vars[DorgeshCourseBits.quest] >= 13 && player.members) {
                val loc = it.loc
                crossObstacle(CoordGrid(loc.x + 1, loc.z, loc.level + 1), GnomeSequences.climb, 3)
            }
        }
        onOpLoc1(DorgeshCourseLocs.ladder) {
            crossObstacle(CoordGrid(2716, 5240), GnomeSequences.descend, 3)
        }
        onOpLoc1(DorgeshCourseLocs.currentUp) {
            crossObstacle(CoordGrid(it.loc.x, 5237, 3), GnomeSequences.climb, 3)
        }
        onOpLoc1(DorgeshCourseLocs.currentDown) {
            crossObstacle(CoordGrid(it.loc.x, 5239, 2), GnomeSequences.descend, 3)
        }
        onOpLoc1(DorgeshCourseLocs.oldUp) {
            crossObstacle(CoordGrid(it.loc.x, 5197, 3), GnomeSequences.climb, 3)
        }
        onOpLoc1(DorgeshCourseLocs.oldDown) {
            crossObstacle(CoordGrid(it.loc.x, 5193, 2), GnomeSequences.descend, 3)
        }
        onOpNpc1(DorgeshCourseNpc.turgall) { talk() }
        for (part in DorgeshCourseItems.parts) onOpNpcU(DorgeshCourseNpc.turgall, part) {
            deliver()
        }
        onOpLocU(DorgeshCourseLocs.boiler, DorgeshCourseItems.spanner) { removePart(false) }
        onOpLocU(DorgeshCourseLocs.console, DorgeshCourseItems.spanner) { removePart(true) }
    }

    private fun ProtectedAccess.eligible(): Boolean {
        if (!player.members || vars[DorgeshCourseBits.quest] < 13 || stat(stats.agility) < 70) {
            mes("You need 70 Agility and completion of Death to the Dorgeshuun to use the course.")
            return false
        }
        if (
            (LightSourceRecipes.all.map { it.lit } + DorgeshCourseItems.permanent).none {
                it in worn || it in inv
            }
        ) {
            mes("You need a light source to use this course.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.cross(loc: BoundLocInfo) {
        if (!eligible()) return
        val index = route.indexOfFirst { loc.coords in it.anchors }
        if (index < 0) return
        val obstacle = route[index]
        val grapple = index >= 8
        val fromA = coords.chebyshevDistance(obstacle.a) <= coords.chebyshevDistance(obstacle.b)
        val origin = if (fromA) obstacle.a else obstacle.b
        if (coords.chebyshevDistance(origin) > 3) return
        if (
            grapple &&
                (stat(stats.ranged) < 70 ||
                    stat(stats.strength) < 70 ||
                    !player.hasGrapple(objects))
        ) {
            mes("Equip a crossbow and mith grapple. You need 70 Ranged and 70 Strength.")
            return
        }
        val part = DorgeshCourseItems.parts.indexOfFirst { it in inv }
        if (part >= 0 && (part >= 3) != grapple) {
            mes(
                if (grapple) "That part is too heavy to grapple with."
                else "That part is too delicate for this route."
            )
            return
        }
        faceLoc(loc)
        if (
            !grapple &&
                (obstacle.type == DorgeshCourseLocs.cable ||
                    obstacle.type == DorgeshCourseLocs.bars) &&
                random.of(100) < (101 - stat(stats.agility)).coerceAtLeast(1) / 2
        ) {
            if (part >= 0)
                invReplace(inv, DorgeshCourseItems.parts[part], 1, DorgeshCourseItems.broken[part])
            crossObstacle(CoordGrid(loc.x, loc.z), GnomeSequences.descend, 4)
            takeInstantHit(HitType.Typeless, 10)
            return
        }
        crossObstacle(if (fromA) obstacle.b else obstacle.a, obstacle.move, obstacle.ticks)
        statAdvance(stats.agility, obstacle.xp)
        if (grapple) {
            statAdvance(stats.strength, 18.0)
            statAdvance(stats.ranged, 18.0)
            player.completeDiaryTask(DiaryTask.GrappleDorgeshPylon)
            if (random.of(25) == 0 && invDel(worn, objs.mith_grapple, 1).success)
                mes("Your grapple breaks as you untie it.")
        }
        val returning = vars[DorgeshCourseBits.returning] != 0
        if (vars[DorgeshCourseBits.heavy] > 0 && fromA != returning)
            vars[DorgeshCourseVars.progress] =
                vars[DorgeshCourseVars.progress] or (1 shl (index + if (returning) 11 else 0))
    }

    private suspend fun ProtectedAccess.talk() {
        if (!eligible()) return
        if (vars[DorgeshCourseBits.heavy] > 0 && DorgeshCourseItems.parts.any { it in inv }) {
            deliver()
            return
        }
        if (vars[DorgeshCourseBits.heavy] == 0) {
            if (!choice2("Offer to bring a replacement generator part.", true, "Goodbye.", false))
                return
            vars[DorgeshCourseBits.heavy] = 1 + random.of(3)
            vars[DorgeshCourseBits.delicate] = 1 + random.of(3)
            vars[DorgeshCourseBits.returning] = 0
            vars[DorgeshCourseVars.progress] = 0
        }
        if (DorgeshCourseItems.spanner !in inv && invAdd(inv, DorgeshCourseItems.spanner).failure) {
            mes("Make room for Turgall's spanner.")
            return
        }
        val heavy = vars[DorgeshCourseBits.heavy] - 1
        val delicate = vars[DorgeshCourseBits.delicate] + 2
        mes(
            "Use the spanner on the old boiler for a ${objects[DorgeshCourseItems.parts[heavy]].name.lowercase()}, or the console for a ${objects[DorgeshCourseItems.parts[delicate]].name.lowercase()}."
        )
    }

    private fun ProtectedAccess.removePart(delicate: Boolean) {
        if (!eligible() || DorgeshCourseItems.spanner !in inv) return
        val order = vars[if (delicate) DorgeshCourseBits.delicate else DorgeshCourseBits.heavy]
        if (order !in 1..3) {
            mes("Ask Turgall which parts he needs first.")
            return
        }
        val mask = vars[DorgeshCourseVars.progress]
        if (mask and 255 != 255 && mask and 1792 != 1792) {
            mes("Complete a route to the old generator first.")
            return
        }
        if (DorgeshCourseItems.parts.any { it in inv }) {
            mes("You are already carrying a spare part.")
            return
        }
        val item = DorgeshCourseItems.parts[order - 1 + if (delicate) 3 else 0]
        if (invAdd(inv, item).failure) {
            mes("Make room for the part.")
            return
        }
        vars[DorgeshCourseBits.returning] = 1
        vars[DorgeshCourseVars.progress] = mask and 2047
        anim(GnomeSequences.climb)
        mes("You remove the ${objects[item].name.lowercase()} with your spanner.")
    }

    private fun ProtectedAccess.deliver() {
        if (!eligible()) return
        val part = DorgeshCourseItems.parts.indexOfFirst { it in inv }
        if (part < 0 || vars[DorgeshCourseBits.returning] == 0) return
        val delicate = part >= 3
        val requested = vars[if (delicate) DorgeshCourseBits.delicate else DorgeshCourseBits.heavy]
        if (requested != part % 3 + 1) return
        val mask = vars[DorgeshCourseVars.progress]
        val back = mask ushr 11
        val wanted = if (delicate) 1792 else 255
        if (back and wanted != wanted) {
            mes("Complete the return route before handing in the part.")
            return
        }
        if (invDel(inv, DorgeshCourseItems.parts[part]).failure) return
        val agilityLegs = (if (mask and 255 == 255) 1 else 0) + (if (delicate) 0 else 1)
        if (agilityLegs > 0) statAdvance(stats.agility, 1216.0 * agilityLegs)
        if (agilityLegs < 2) statAdvance(stats.ranged, 571.0 * (2 - agilityLegs))
        vars[DorgeshCourseBits.heavy] = 0
        vars[DorgeshCourseBits.delicate] = 0
        vars[DorgeshCourseBits.returning] = 0
        vars[DorgeshCourseVars.progress] = 0
        recordLap(AgilityLap.Dorgesh)
        marks.roll(this, 70, CoordGrid(2740, 5214, 3))
        mes("Turgall fits the replacement part into the generator.")
    }
}
