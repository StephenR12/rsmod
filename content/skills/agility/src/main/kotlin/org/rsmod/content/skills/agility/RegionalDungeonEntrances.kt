package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EntranceLocs : LocReferences() {
    val fence = find("lumbridge_sc_fencejump")
    val crevice = find("dwarf_mines_sc_wall_crack")
    val jutting = find("fairy_sc_juttingwall")
    val lock = find("toollock5")
    val cellar = find("stairs_cellar")
    val stairs = find("stairs_from_cellar")
    val woodcuttingGates = listOf(find("wcguild_gatel"), find("wcguild_gater"))
}

internal object EntranceVars : VarpReferences() {
    val lostCity = find("zanaris")
}

internal object EntranceItems : ObjReferences() {
    val pick = find("lockpick")
}

public class RegionalDungeonEntrances @Inject constructor(private val locs: LocRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(EntranceLocs.fence) {
            val fence = it.loc
            if (!agility(13)) return@onOpLoc1
            val north = coords.z >= fence.z
            crossObstacle(
                CoordGrid(fence.x, if (north) fence.z - 1 else fence.z),
                seqs.human_longjump,
                3,
            )
            player.completeDiaryTask(DiaryTask.JumpVarrockSouthFence)
        }
        onOpLoc1(EntranceLocs.crevice) {
            if (!agility(42)) return@onOpLoc1
            val west = it.loc.x == 3029
            crossObstacle(CoordGrid(if (west) 3035 else 3028, 9806), GnomeSequences.crawl, 4)
            player.completeDiaryTask(DiaryTask.SqueezeDwarvenMineCrevice)
        }
        onOpLoc1(EntranceLocs.jutting) {
            val wall = it.loc
            val level = if (wall.x == 2400) 46 else 66
            if (!agility(level) || vars[EntranceVars.lostCity] < 6) return@onOpLoc1
            val southbound = coords.z > wall.z
            val z = if (southbound) wall.z - 1 else wall.z + 1
            val clipped = random.of(256) < (100 - stat(stats.agility)).coerceIn(1, 80)
            crossObstacle(CoordGrid(wall.x, z), GnomeSequences.balance, 5)
            if (clipped) takeInstantHit(HitType.Typeless, 1 + random.of(4))
            statAdvance(stats.agility, if (clipped) 6.0 else 10.0)
            if (southbound) player.completeDiaryTask(DiaryTask.SqueezeCosmicWall)
        }
        onOpLoc1(EntranceLocs.lock) {
            val loc = it.loc
            if (coords.z >= loc.z && player.members)
                crossObstacle(CoordGrid(loc.x, loc.z - 1), GnomeSequences.climb, 2)
            else mes("The door is locked.")
        }
        onOpLoc2(EntranceLocs.lock) { unlock(it.loc) }
        for (type in listOf(EntranceLocs.cellar, EntranceLocs.stairs)) {
            onOpLoc1(type) { cellar(it.loc, type == EntranceLocs.cellar) }
            onApLoc1(type) {
                if (isWithinApRange(it.loc, 1)) cellar(it.loc, type == EntranceLocs.cellar)
            }
        }
        for (gate in EntranceLocs.woodcuttingGates) onOpLoc1(gate) {
            val loc = it.loc
            val east = loc.x == 1657
            val westSide = coords.x <= loc.x
            val entering = east != westSide
            if (entering && (!player.members || stat(stats.woodcutting) < 60)) {
                mes("You need level 60 Woodcutting to enter the Woodcutting Guild.")
                return@onOpLoc1
            }
            crossObstacle(
                CoordGrid(if (westSide) loc.x + 1 else loc.x, loc.z),
                seqs.human_walk_f,
                2,
            )
            if (entering) player.completeDiaryTask(DiaryTask.EnterWoodcuttingGuild)
        }
    }

    private suspend fun ProtectedAccess.cellar(target: BoundLocInfo, descending: Boolean) {
        val dz = if (descending) 6400 else -6400
        val twin = if (descending) EntranceLocs.stairs else EntranceLocs.cellar
        if (locs.findExact(target.coords.translateZ(dz), twin) == null) return
        val landing =
            if (target.x == 2603 && target.z in setOf(3078, 9478))
                CoordGrid(target.x - 1, target.z + dz + 1)
            else coords.translateZ(dz)
        crossObstacle(landing, if (descending) GnomeSequences.descend else GnomeSequences.climb, 3)
    }

    private fun ProtectedAccess.agility(level: Int): Boolean {
        if (!player.members) return false
        if (stat(stats.agility) >= level) return true
        mes("You need level $level Agility to use this shortcut.")
        return false
    }

    private suspend fun ProtectedAccess.unlock(loc: BoundLocInfo) {
        if (!player.members || loc.coords != CoordGrid(2601, 9482)) return
        if (stat(stats.thieving) < 82 || EntranceItems.pick !in inv) {
            mes("You need a lockpick and level 82 Thieving to pick this lock.")
            return
        }
        delay(2)
        if (random.of(8) != 0) {
            mes("You fail to pick the lock.")
            return
        }
        val z = if (coords.z >= loc.z) loc.z - 1 else loc.z
        crossObstacle(CoordGrid(loc.x, z), GnomeSequences.climb, 2)
        statAdvance(stats.thieving, 50.0)
        player.completeDiaryTask(DiaryTask.PickYanilleDungeonDoor)
    }
}
