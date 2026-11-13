package org.rsmod.content.skills.hunter.moths

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object HunterGuildLocs : LocReferences() {
    val down = find("hunterguild_stairs_down01_combined")
    val up = find("hunterguild_stairs_up01")
    val ropeUp = find("hunterguild_rope_up01")
    val ropeDown = find("hunterguild_rope_down01")
    val roots = find("hg_cavekit_roots01_bottom01")
}

/** Burrow oldid14784691; Roots oldid14977258, both before the cache-233.4 cutoff. */
public class HunterGuildAccess : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(HunterGuildLocs.ropeUp) {
            if (player.members && player.statBase(stats.hunter) >= 46)
                telejump(CoordGrid(1556, 3046, 2))
            else mes("You need an unboosted Hunter level of 46 to use the guild's amenities.")
        }
        onOpLoc1(HunterGuildLocs.ropeDown) { telejump(CoordGrid(1559, 3046)) }
        onOpLoc1(HunterGuildLocs.down) {
            if (player.members && player.statBase(stats.hunter) >= 46)
                teleport(CoordGrid(1557, 9446))
            else mes("You need a Hunter level of 46 to enter the Burrow.")
        }
        onOpLoc1(HunterGuildLocs.up) { teleport(CoordGrid(1557, 3050)) }
        onOpLocU(HunterGuildLocs.roots, objs.knife) { takeLog() }
    }

    public suspend fun ProtectedAccess.takeLog() {
        if (!player.members || objs.knife !in player.inv) return
        if (player.inv.indices.count { player.inv[it]?.id == objs.logs.id } >= 3) {
            mes("You already have some logs. Taking more could risk the stability of the cavern.")
            return
        }
        if (!player.invAdd(player.inv, objs.logs).success) {
            mes("You don't have enough inventory space.")
            return
        }
        mes("You manage to ease some logs off the roots.")
        delay(1)
    }
}

internal object VarlamoreMothSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<HunterGuildAccess>("varlamore-moths.toml")
    }
}
