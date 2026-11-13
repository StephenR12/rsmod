package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import kotlin.math.abs
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.WeaponCategory
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RegionalShortcutVars : VarpReferences() {
    val karamjaRope = find("karamja_volcano_grapple_unlocked")
    val regicide = find("regicide_quest")
    val grandTree = find("grandtree")
    val village = find("treequest")
}

internal object RegionalShortcutVarBuilder : VarpBuilder() {
    init {
        build("karamja_volcano_grapple_unlocked") { transmitNever = true }
    }
}

internal object GrappleSequences : SeqReferences() {
    val grapple = find("xbows_human_fire_and_climb_grapple")
}

public class RegionalShortcuts
@Inject
constructor(private val locs: LocTypeList, private val objects: ObjTypeList) : PluginScript() {
    private enum class Gate {
        None,
        Regicide,
        Priest,
        KaramjaGrapple,
        WaterGrapple,
        RiverGrapple,
        GrandTree,
    }

    private data class Crossing(
        val loc: String,
        val tile: CoordGrid,
        val from: CoordGrid,
        val to: CoordGrid,
        val level: Int,
        val xp: Double,
        val animation: SeqType,
        val ticks: Int,
        val task: DiaryTask? = null,
        val approach: Int = 1,
        val gate: Gate = Gate.None,
    )

    private val wallExit = WeakHashMap<Player, CoordGrid>()

    override fun ScriptContext.startup() {
        fun tile(x: Int, z: Int) = CoordGrid(x, z)
        val rope = GnomeSequences.swing
        val jump = seqs.human_longjump
        val climb = GnomeSequences.climb
        val crossings =
            listOf(
                Crossing(
                    "tree_ropeswing1",
                    tile(2705, 3209),
                    tile(2710, 3209),
                    tile(2704, 3209),
                    10,
                    3.0,
                    rope,
                    5,
                    DiaryTask.SwingMossGiantIsland,
                ),
                Crossing(
                    "tree_ropeswing2",
                    tile(2703, 3205),
                    tile(2704, 3205),
                    tile(2709, 3205),
                    10,
                    3.0,
                    rope,
                    5,
                ),
                Crossing(
                    "karam_dungeon_stone1",
                    tile(2649, 9561),
                    tile(2649, 9562),
                    tile(2647, 9557),
                    12,
                    7.5,
                    jump,
                    4,
                    DiaryTask.JumpBrimhavenLava,
                ),
                Crossing(
                    "karam_dungeon_stone2",
                    tile(2647, 9558),
                    tile(2647, 9557),
                    tile(2649, 9562),
                    1,
                    7.5,
                    jump,
                    4,
                    DiaryTask.JumpBrimhavenLava,
                ),
                Crossing(
                    "mine_log_balance1",
                    tile(2599, 3477),
                    tile(2598, 3477),
                    tile(2603, 3477),
                    20,
                    8.5,
                    GnomeSequences.balance,
                    5,
                    DiaryTask.CrossCoalTrucksLog,
                ),
                Crossing(
                    "mine_log_balance1",
                    tile(2602, 3477),
                    tile(2603, 3477),
                    tile(2598, 3477),
                    20,
                    8.5,
                    GnomeSequences.balance,
                    5,
                    DiaryTask.CrossCoalTrucksLog,
                ),
                Crossing(
                    "fai_falador_castle_crumble_mid",
                    tile(2935, 3355),
                    tile(2934, 3355),
                    tile(2936, 3355),
                    5,
                    0.5,
                    climb,
                    3,
                    DiaryTask.ClimbFaladorWestWall,
                ),
                Crossing(
                    "fai_falador_castle_crumble_mid",
                    tile(2935, 3355),
                    tile(2936, 3355),
                    tile(2934, 3355),
                    5,
                    0.5,
                    climb,
                    3,
                    DiaryTask.ClimbFaladorWestWall,
                ),
                Crossing(
                    "archeuus_runestone_shortcut_boulder",
                    tile(1776, 3883),
                    tile(1776, 3884),
                    tile(1776, 3880),
                    49,
                    0.0,
                    jump,
                    4,
                    DiaryTask.LeapDenseEssenceBoulder,
                ),
                Crossing(
                    "elves_overpass_sc_rocks_top",
                    tile(2333, 3252),
                    tile(2332, 3252),
                    tile(2338, 3253),
                    85,
                    0.0,
                    climb,
                    5,
                    DiaryTask.ClimbAdvancedElvenOverpass,
                    gate = Gate.Regicide,
                ),
                Crossing(
                    "elves_overpass_sc_rocks_bottom",
                    tile(2337, 3253),
                    tile(2338, 3253),
                    tile(2332, 3252),
                    85,
                    0.0,
                    climb,
                    5,
                    DiaryTask.ClimbAdvancedElvenOverpass,
                    gate = Gate.Regicide,
                ),
                Crossing(
                    "morytania_railing_sc_fence_1",
                    tile(3424, 3476),
                    tile(3423, 3476),
                    tile(3425, 3484),
                    65,
                    0.0,
                    GnomeSequences.crawl,
                    6,
                    DiaryTask.CrossSalveRailing,
                    gate = Gate.Priest,
                ),
                Crossing(
                    "morytania_railing_sc_fence_2",
                    tile(3425, 3484),
                    tile(3425, 3484),
                    tile(3423, 3476),
                    65,
                    0.0,
                    GnomeSequences.crawl,
                    6,
                    gate = Gate.Priest,
                ),
                Crossing(
                    "xbows_raft_br",
                    tile(3252, 3179),
                    tile(3246, 3179),
                    tile(3259, 3179),
                    8,
                    0.0,
                    GrappleSequences.grapple,
                    9,
                    DiaryTask.CrossRiverLumRaft,
                    8,
                    Gate.RiverGrapple,
                ),
                Crossing(
                    "xbows_raft_br",
                    tile(3252, 3179),
                    tile(3259, 3179),
                    tile(3246, 3179),
                    8,
                    0.0,
                    GrappleSequences.grapple,
                    9,
                    DiaryTask.CrossRiverLumRaft,
                    8,
                    Gate.RiverGrapple,
                ),
                Crossing(
                    "gnome_stronghold_sc_rock_top",
                    tile(2487, 3515),
                    tile(2486, 3515),
                    tile(2489, 3521),
                    37,
                    0.0,
                    climb,
                    5,
                    DiaryTask.ClimbFromGrandTreeToOttos,
                    gate = Gate.GrandTree,
                ),
                Crossing(
                    "gnome_stronghold_sc_rock_bottom",
                    tile(2489, 3520),
                    tile(2489, 3521),
                    tile(2486, 3515),
                    37,
                    0.0,
                    climb,
                    5,
                    gate = Gate.GrandTree,
                ),
                Crossing(
                    "xbows_beach_to_island_tree_basic",
                    tile(2841, 3434),
                    tile(2842, 3427),
                    tile(2842, 3433),
                    36,
                    0.0,
                    GrappleSequences.grapple,
                    6,
                    DiaryTask.GrappleWaterObeliskIsland,
                    8,
                    Gate.WaterGrapple,
                ),
                Crossing(
                    "xbows_jungletree_karamja_basic",
                    tile(2873, 3134),
                    tile(2878, 3139),
                    tile(2874, 3127),
                    53,
                    0.0,
                    GrappleSequences.grapple,
                    6,
                    DiaryTask.GrappleKaramjaVolcano,
                    8,
                    Gate.KaramjaGrapple,
                ),
                Crossing(
                    "xbows_jungletree_karamja_basic",
                    tile(2873, 3134),
                    tile(2874, 3127),
                    tile(2878, 3139),
                    53,
                    0.0,
                    GrappleSequences.grapple,
                    6,
                    DiaryTask.GrappleKaramjaVolcano,
                    8,
                    Gate.KaramjaGrapple,
                ),
            )
        // The two western cliff sections can be traversed in either direction. The eastern descent
        // is one-way.
        // TODO(quest): The Troll Stronghold arena gate guards the approach from Death Plateau.
        val troll = "trollheim_wildy_climb_rocks"
        val trollRoutes =
            listOf(
                Crossing(
                    troll,
                    tile(2916, 3672),
                    tile(2915, 3672),
                    tile(2918, 3672),
                    64,
                    0.0,
                    climb,
                    4,
                ),
                Crossing(
                    troll,
                    tile(2917, 3672),
                    tile(2918, 3672),
                    tile(2915, 3672),
                    64,
                    0.0,
                    climb,
                    4,
                ),
                Crossing(
                    troll,
                    tile(2922, 3672),
                    tile(2921, 3672),
                    tile(2924, 3673),
                    64,
                    0.0,
                    climb,
                    4,
                ),
                Crossing(
                    troll,
                    tile(2923, 3673),
                    tile(2924, 3673),
                    tile(2921, 3672),
                    64,
                    0.0,
                    climb,
                    4,
                ),
                Crossing(
                    troll,
                    tile(2947, 3678),
                    tile(2946, 3678),
                    tile(2950, 3681),
                    64,
                    0.0,
                    climb,
                    5,
                    DiaryTask.ClimbTrollheimIntoWilderness,
                ),
            )
        val names = locs.values.associateBy { it.internalName }
        for ((name, paths) in (crossings + trollRoutes).groupBy { it.loc }) {
            val type = names.getValue(name)
            onOpLoc1(type) { cross(paths, it.loc) }
            onApLoc1(type) {
                if (isWithinApRange(it.loc, paths.maxOf { path -> path.approach }))
                    cross(paths, it.loc)
            }
        }
        for (north in listOf(true, false)) {
            val ground =
                if (north)
                    listOf(
                        "xbows_fai_falador_castle_walls_hillskew",
                        "xbows_fai_falador_castle_arches_hillskew",
                    )
                else listOf("xbows_yanille_castlewall")
            val top =
                if (north)
                    listOf(
                        "xbows_fai_falador_castle_walls_battlement",
                        "xbows_fai_falador_castle_walls_crenalation",
                    )
                else listOf("xbows_yanille_castlewall_battlement")
            for (name in ground) onOpLoc1(names.getValue(name)) { scaleWall(north, it.loc) }
            for (name in top) onOpLoc1(names.getValue(name)) { jumpOffWall(north, it.loc) }
        }
    }

    private suspend fun ProtectedAccess.cross(paths: List<Crossing>, target: BoundLocInfo) {
        if (!player.members) return
        val route =
            paths
                .filter { it.tile == target.coords && it.from.level == coords.level }
                .minByOrNull { coords.chebyshevDistance(it.from) } ?: return
        val distance = coords.chebyshevDistance(route.from)
        if (distance > (if (route.gate == Gate.KaramjaGrapple) 3 else 2)) return
        if (stat(stats.agility) < route.level) {
            mes("You need an Agility level of ${route.level} to use this shortcut.")
            return
        }
        when (route.gate) {
            Gate.None -> Unit
            Gate.Priest ->
                if (vars[varps.priest_in_peril] < 60) {
                    mes("You must complete Priest in Peril first.")
                    return
                }
            Gate.Regicide ->
                if (vars[RegionalShortcutVars.regicide] < 15) {
                    mes("You must complete Regicide first.")
                    return
                }
            Gate.RiverGrapple -> if (!grappleReady(19, 37)) return
            Gate.GrandTree ->
                if (vars[RegionalShortcutVars.grandTree] < 160) {
                    mes("The ground is too weak for you to climb at the moment.")
                    return
                }
            Gate.WaterGrapple -> if (!grappleReady(22, 39)) return
            Gate.KaramjaGrapple -> {
                if (!grappleReady(21, 42)) return
                if (route.from.z < 3134 && vars[RegionalShortcutVars.karamjaRope] == 0) {
                    mes("You first need to secure the rope from Musa Point.")
                    return
                }
            }
        }
        faceLoc(target)
        traverse(route.to, route.animation, route.ticks)
        if (coords != route.to) return
        if (route.xp > 0) statAdvance(stats.agility, route.xp)
        if (route.gate == Gate.KaramjaGrapple) {
            vars[RegionalShortcutVars.karamjaRope] = 1
        }
        if (route.gate in setOf(Gate.KaramjaGrapple, Gate.WaterGrapple, Gate.RiverGrapple))
            checkGrapple()
        if (route.gate == Gate.GrandTree && vars[RegionalShortcutVars.village] < 9) return
        route.task?.let(player::completeDiaryTask)
    }

    private fun ProtectedAccess.grappleReady(strength: Int, ranged: Int): Boolean {
        if (stat(stats.strength) < strength || stat(stats.ranged) < ranged) {
            mes("You need $strength Strength and $ranged Ranged to grapple here.")
            return false
        }
        if (!player.hasGrapple(objects)) {
            mes("You need to equip a crossbow and a mith grapple.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.checkGrapple() {
        if (random.of(25) == 0 && invDel(worn, objs.mith_grapple, 1).success)
            mes("Your grapple breaks as you untie the rope.")
    }

    private suspend fun ProtectedAccess.scaleWall(falador: Boolean, target: BoundLocInfo) {
        val height = if (falador) 11 else 39
        if (!player.members || coords.level != 0) return
        if ((falador && target.x !in 3032..3033) || (!falador && target.x != 2556)) return
        if (stat(stats.agility) < height) {
            mes("You need an Agility level of $height to climb here.")
            return
        }
        if (!grappleReady(if (falador) 37 else 38, if (falador) 19 else 21)) return
        val south = if (falador) CoordGrid(3032, 3388) else CoordGrid(2556, 3072)
        val north = if (falador) CoordGrid(3033, 3391) else CoordGrid(2556, 3075)
        val exit =
            if (coords.chebyshevDistance(south) < coords.chebyshevDistance(north)) north else south
        faceLoc(target)
        anim(GrappleSequences.grapple)
        delay(4)
        telejump(if (falador) CoordGrid(3032, 3389, 1) else CoordGrid(2556, 3073, 1))
        wallExit[player] = exit
        checkGrapple()
    }

    private suspend fun ProtectedAccess.jumpOffWall(falador: Boolean, target: BoundLocInfo) {
        if (
            !player.members ||
                coords.level != 1 ||
                (falador && target.x !in 3032..3033) ||
                (!falador && target.x != 2556)
        )
            return
        val crossed = wallExit.remove(player)?.takeIf { abs(it.x - target.x) < 3 }
        val exit = crossed ?: if (falador) CoordGrid(3033, 3391) else CoordGrid(2556, 3072)
        anim(seqs.human_longjump)
        delay(2)
        telejump(exit)
        if (crossed != null)
            player.completeDiaryTask(
                if (falador) DiaryTask.GrappleFaladorWall else DiaryTask.GrappleYanilleWall
            )
    }

    private suspend fun ProtectedAccess.traverse(to: CoordGrid, sequence: SeqType, ticks: Int) {
        val dx = to.x - coords.x
        val dz = to.z - coords.z
        val face =
            if (abs(dx) > abs(dz)) {
                if (dx > 0) Constants.em_face_east else Constants.em_face_west
            } else if (dz > 0) Constants.em_face_north else Constants.em_face_south
        anim(sequence)
        exactMove(coords, to, 0, ticks * 30, face)
        delay(ticks)
        resetAnim()
    }
}

internal fun Player.hasGrapple(objects: ObjTypeList): Boolean {
    val weapon = righthand?.let { objects[it.id] } ?: return false
    return weapon.weaponCategory == WeaponCategory.Crossbow.id && quiver?.id == objs.mith_grapple.id
}
