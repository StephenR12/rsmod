package org.rsmod.content.skills.agility

import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SlayerChainLocs : LocReferences() {
    val bottom = find("slayertower_sc_chainbottom")
    val top = find("slayertower_sc_chaintop")
}

public class SlayerTowerChains : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(SlayerChainLocs.bottom) { climb(it.loc, up = true) }
        onOpLoc1(SlayerChainLocs.top) { climb(it.loc, up = false) }
    }

    private suspend fun ProtectedAccess.climb(chain: BoundLocInfo, up: Boolean) {
        if (!player.members) return
        val advanced = chain.x == 3447 && chain.z == 3576
        if (!advanced && (chain.x != 3422 || chain.z != 3550)) return
        val required = if (advanced) 71 else 61
        if (stat(stats.agility) < required) {
            mes("You need an Agility level of $required to climb this chain.")
            return
        }
        faceLoc(chain)
        val dest = CoordGrid(chain.x, chain.z - 1, chain.level + if (up) 1 else -1)
        crossObstacle(dest, if (up) GnomeSequences.climb else GnomeSequences.descend, 4)
        val injured = random.of(100) < (100 - stat(stats.agility)) / 2
        statAdvance(stats.agility, if (injured) 6.0 else 3.0)
        if (injured) {
            takeInstantHit(HitType.Typeless, random.of(2, 5))
            mes("You rip your hands on the spikes as you climb.")
        } else if (advanced && up) player.completeDiaryTask(DiaryTask.ClimbSlayerTowerAdvancedChain)
    }
}
