package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.isAssignedSlayerTarget
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CerberusLocs : LocReferences() {
    val inCaves = listOf("a_01", "b_02", "c_03").map { find("hellhound_cave_entrance_$it") }
    val outCaves = listOf("01", "02", "03").map { find("hellhound_cave_entrance_$it") }
    val winch = find("hellhound_winch")
    val gate = find("hellhound_iron_gate_active")
}

internal object CerberusItems : ObjReferences() {
    val scroll = find("teleportscroll_cerberus")
}

internal enum class CerberusRoom(
    val winch: CoordGrid,
    val portcullis: CoordGrid,
    val spawn: CoordGrid,
) {
    West(CoordGrid(1291, 1254), CoordGrid(1239, 1225), CoordGrid(1238, 1250)),
    North(CoordGrid(1307, 1269), CoordGrid(1303, 1289), CoordGrid(1302, 1314)),
    East(CoordGrid(1328, 1254), CoordGrid(1367, 1225), CoordGrid(1366, 1250));

    val inside
        get() = portcullis.translate(1, 3)

    val outside
        get() = if (this == North) winch.translate(2, 0) else winch.translate(0, -1)
}

public class CerberusLair
@Inject
constructor(private val types: NpcTypeList, private val players: PlayerRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (cave in CerberusLocs.inCaves) onOpLoc1(cave) {
            if (player.members) telejump(CoordGrid(1310, 1238))
        }
        for (cave in CerberusLocs.outCaves) onOpLoc1(cave) { telejump(CoordGrid(2873, 9847)) }
        onOpLoc1(CerberusLocs.winch) {
            val room =
                CerberusRoom.entries.firstOrNull { room -> room.winch == it.loc.coords }
                    ?: return@onOpLoc1
            if (!player.members) return@onOpLoc1
            if (
                stat(stats.slayer) < 91 ||
                    !types[SlayerBossNpcs.cerberus].isAssignedSlayerTarget(player, room.spawn)
            ) {
                mes(
                    "The Key Master requires level 91 Slayer and a hellhound or Cerberus assignment."
                )
                return@onOpLoc1
            }
            telejump(room.inside)
        }
        onOpLoc2(CerberusLocs.winch) {
            for (room in CerberusRoom.entries) {
                val occupied =
                    players.findAll(ZoneKey.from(room.spawn), 4).any { other ->
                        other.coords.level == 0 &&
                            other.coords.x in room.spawn.x - 17..room.spawn.x + 19 &&
                            other.coords.z in room.portcullis.z + 1..room.spawn.z + 15
                    }
                mes("${room.name} chamber: ${if (occupied) "occupied" else "empty"}.")
            }
        }
        onOpLoc1(CerberusLocs.gate) {
            CerberusRoom.entries
                .firstOrNull { room -> room.portcullis == it.loc.coords }
                ?.let { room -> telejump(room.outside) }
        }
        onOpHeld1(CerberusItems.scroll) {
            val wildBase = if (coords.z >= 9920) 9920 else 3520
            if (
                !player.members ||
                    player.isTeleportBlocked() ||
                    (vars[varbits.inside_wilderness] != 0 && (coords.z - wildBase) / 8 + 1 > 20)
            ) {
                mes("You cannot teleport from here.")
                return@onOpHeld1
            }
            if (invDel(inv, CerberusItems.scroll, 1, it.slot).success)
                telejump(CoordGrid(1310, 1250))
        }
    }
}
