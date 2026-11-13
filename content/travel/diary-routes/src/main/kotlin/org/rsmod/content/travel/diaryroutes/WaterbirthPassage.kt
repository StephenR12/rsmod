package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerCombatEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WaterbirthLocs : LocReferences() {
    val shoreCave = find("dagannoth_caveentrance_rock")
    val shoreExit = find("dagannoth_rocky_steps")
    val summitCave = find("dagannoth_caveentrance_ice")
    val rocks = find("dagannoth_waterbirth_rock_climb_agility_shortcut_bottom")
    val rocksTop = find("dagannoth_waterbirth_rock_climb_agility_shortcut_top")
    val central = find("dagannoth_ladder_base_ext2")
    val returnLadder = find("dagexp_entrance_ladder")
    val exit = find("dagexp_exit_ladder")
    val ladders = (1..34).map { find("dagexp_ladder$it") }
    val doors =
        listOf(
            find("dagannoth_pressure_door"),
            find("dagannoth_pressure_door2"),
            find("dagannoth_pressure_door3"),
        )
    val open = find("dagannoth_pressure_open")
    val blocker = find("dagcave_invisibleblocking")
    val lowerLight = find("horror_ladder_base2")
    val upperLight = find("horror_ladder_top2")
}

internal object WaterNpcs : NpcReferences() {
    val rellekka = find("viking_dagganoth_cave_ferryman_rellekka")
    val island = find("viking_dagganoth_cave_waterbirth_island")
    val supports = listOf("south", "west", "north").map { find("dagannoth_weak_door_$it") }
}

internal object WaterSupportStats : NpcEditor() {
    init {
        for (type in WaterNpcs.supports) edit(type) { hitpoints = 10 }
    }
}

internal object WaterItems : ObjReferences() {
    val rock = find("vt_useless_rock")
    val egg = find("scrambled_egg")
}

internal object WaterVars : VarpReferences() {
    val frem = find("viking")
}

internal object WaterbirthBits : VarBitReferences() {
    val horror = find("horrorquest")
}

public class WaterbirthPassage
@Inject
constructor(
    private val locs: LocRepository,
    private val npcs: NpcRepository,
    private val ground: ObjRepository,
    private val players: PlayerRepository,
    private val npcTypes: NpcTypeList,
    private val playerList: PlayerList,
) : PluginScript() {
    // Each native ladder is paired with a ladder on the adjacent plane. Its
    // landing tile is on the walkable side of the partner's collision footprint.
    private val landings =
        listOf(
            CoordGrid(1810, 4405, 2),
            CoordGrid(1807, 4405, 3),
            CoordGrid(1822, 4404, 2),
            CoordGrid(1825, 4404, 3),
            CoordGrid(1834, 4387, 2),
            CoordGrid(1834, 4390, 3),
            CoordGrid(1809, 4394, 1),
            CoordGrid(1812, 4394, 2),
            CoordGrid(1799, 4386, 2),
            CoordGrid(1799, 4389, 1),
            CoordGrid(1799, 4382, 1),
            CoordGrid(1796, 4382, 2),
            CoordGrid(1800, 4369, 2),
            CoordGrid(1803, 4369, 1),
            CoordGrid(1828, 4362, 1),
            CoordGrid(1825, 4362, 2),
            CoordGrid(1863, 4373, 2),
            CoordGrid(1863, 4370, 1),
            CoordGrid(1864, 4390, 1),
            CoordGrid(1864, 4387, 2),
            CoordGrid(1890, 4409, 0),
            CoordGrid(1890, 4406, 1),
            CoordGrid(1957, 4373, 1),
            CoordGrid(1957, 4370, 0),
            CoordGrid(1824, 4379, 3),
            CoordGrid(1824, 4382, 2),
            CoordGrid(1838, 4374, 2),
            CoordGrid(1838, 4377, 3),
            CoordGrid(1850, 4384, 1),
            CoordGrid(1850, 4387, 2),
            CoordGrid(1932, 4377, 1),
            CoordGrid(1932, 4380, 2),
            CoordGrid(1961, 4392, 2),
            CoordGrid(1961, 4393, 3),
        )
    private val supports =
        listOf(CoordGrid(2545, 10141), CoordGrid(2543, 10143), CoordGrid(2545, 10145))

    override fun ScriptContext.startup() {
        for ((npc, fromPort) in listOf(WaterNpcs.rellekka to true, WaterNpcs.island to false)) {
            onOpNpc1(npc) { sail(fromPort, true) }
            onOpNpc3(npc) { sail(fromPort, false) }
        }
        onOpLoc1(WaterbirthLocs.shoreCave) { telejump(CoordGrid(2442, 10147)) }
        onOpLoc1(WaterbirthLocs.shoreExit) { telejump(CoordGrid(2523, 3740)) }
        onOpLoc1(WaterbirthLocs.summitCave) {
            if (it.loc.coords == CoordGrid(2542, 3740)) telejump(CoordGrid(2545, 10143))
        }
        onOpLoc1(WaterbirthLocs.rocks) { climbRocks(true) }
        onOpLoc1(WaterbirthLocs.rocksTop) { climbRocks(false) }
        for (door in WaterbirthLocs.doors) onOpLoc1(door) { openDoor(it.loc) }
        for ((index, support) in WaterNpcs.supports.withIndex()) {
            // Damage uses the ordinary weapon/spell roll, including rune and ammo costs.
            // A successful strike topples the prop rather than killing a combat NPC.
            onEvent<NpcHitEvents.ResolveDamage>(support.id) {
                val source = if (hit.isFromPlayer) hit.resolvePlayerSource(playerList) else null
                if (source != null && damage > 0) {
                    if (canHit(source.coords, index)) topple(npc)
                    else source.mes("This door does not seem to be openable from this side...")
                }
                damage = 0
            }
        }
        onEvent<PlayerCombatEvents.Chainhit> {
            val index = supports.indexOf(target.coords)
            if (
                index < 0 ||
                    target.visType.id != WaterNpcs.supports[index].id ||
                    !canHit(player.coords, index)
            )
                return@onEvent
            for ((i, tile) in supports.withIndex()) {
                npcs
                    .findAll(tile)
                    .firstOrNull { it.visType.id == WaterNpcs.supports[i].id }
                    ?.let(::topple)
            }
        }
        onOpLoc1(WaterbirthLocs.central) {
            if (choice2("Climb up to the island.", true, "Climb down into the dungeon.", false))
                telejump(CoordGrid(2544, 3740))
            else telejump(CoordGrid(1798, 4407, 3))
        }
        onOpLoc2(WaterbirthLocs.central) { telejump(CoordGrid(2544, 3740)) }
        onOpLoc3(WaterbirthLocs.central) { telejump(CoordGrid(1798, 4407, 3)) }
        onOpLoc1(WaterbirthLocs.returnLadder) { telejump(CoordGrid(2545, 10143)) }
        for ((index, ladder) in WaterbirthLocs.ladders.withIndex()) onOpLoc1(ladder) {
            telejump(landings[index])
        }
        onOpLoc1(WaterbirthLocs.exit) {
            if (!player.members || vars[WaterbirthBits.horror] < 10) {
                mes("You must complete Horror from the Deep to use this exit.")
                return@onOpLoc1
            }
            telejump(CoordGrid(2515, 10008))
            player.completeDiaryTask(DiaryTask.ReachLighthouseFromWaterbirth)
        }
        onOpLoc1(WaterbirthLocs.lowerLight) { telejump(CoordGrid(2515, 10006, 1)) }
        onOpLoc1(WaterbirthLocs.upperLight) {
            if (vars[WaterbirthBits.horror] >= 10) telejump(CoordGrid(2515, 10008))
        }
    }

    private fun canHit(from: CoordGrid, target: Int): Boolean {
        if (from.level != 0) return false
        // The straight line between the northern and southern corridors crosses
        // their own barricades. Only the western approach has a shot at both.
        if (from.x < 2543) return target != 1
        if (from.z < 10141 || from.z > 10145) return target == 1
        return true
    }

    private fun topple(npc: Npc) {
        val loc = locs.findExact(npc.coords, WaterbirthLocs.blocker) ?: return
        if (locs.del(loc, 100)) npc.transmog(npcTypes.getValue(npc.id + 2), 100)
    }

    private fun ProtectedAccess.openDoor(door: BoundLocInfo) {
        if (coords.x <= door.x) {
            val north = CoordGrid(2490, door.z + 2)
            val south = CoordGrid(2490, door.z)
            if (!weighted(north) || !weighted(south)) {
                mes("The two pressure pads must be weighed down.")
                return
            }
        }
        locs.change(door, WaterbirthLocs.open, 10)
    }

    private fun ProtectedAccess.weighted(tile: CoordGrid): Boolean =
        coords == tile ||
            players.findAll(tile).any() ||
            ground.findAll(tile).any {
                it.type == WaterItems.rock.id || it.type == WaterItems.egg.id
            }

    private fun ProtectedAccess.climbRocks(up: Boolean) {
        if (stat(stats.agility) < 85) mes("You need an Agility level of 85 to climb these rocks.")
        else telejump(if (up) CoordGrid(2545, 3745) else CoordGrid(2548, 3748))
    }

    private suspend fun ProtectedAccess.sail(fromPort: Boolean, talk: Boolean) {
        if (!player.members) return
        val fare = if (vars[WaterVars.frem] >= 10) 0 else 1000
        if (
            talk &&
                !choice2(
                    if (fare == 0) "Sail with Jarvald." else "Pay 1,000 coins for passage.",
                    true,
                    "Stay here.",
                    false,
                )
        )
            return
        if (fare > 0 && !invDel(inv, objs.coins, count = fare).success) {
            mes("Jarvald asks for 1,000 coins.")
            return
        }
        telejump(if (fromPort) CoordGrid(2549, 3758) else CoordGrid(2621, 3684))
    }
}
