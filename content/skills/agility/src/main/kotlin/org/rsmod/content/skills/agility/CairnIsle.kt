package org.rsmod.content.skills.agility

import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object CairnIsleLocs : LocReferences() {
    val rocks = find("zqclimbingrocks")
}

internal object CairnIsleTimers : TimerReferences() {
    val bridge = find("cairn_isle_bridge")
}

public class CairnIsle : PluginScript() {
    private val previousX = WeakHashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpLoc1(CairnIsleLocs.rocks) { climb(it.loc) }
        onApLoc1(CairnIsleLocs.rocks) { if (isWithinApRange(it.loc, 1)) climb(it.loc) }
        onPlayerLogin {
            if (nearIsle(player.coords)) {
                previousX[player] = player.coords.x
                player.timer(CairnIsleTimers.bridge, 1)
            }
        }
        onPlayerLogout { previousX.remove(player) }
        onPlayerTimer(CairnIsleTimers.bridge) { checkBridge() }
    }

    private suspend fun ProtectedAccess.climb(rocks: BoundLocInfo) {
        if (!player.members || coords.level != 0 || rocks.z !in 2978..2980) return
        if (stat(stats.agility) < 15) {
            mes("You need an Agility level of 15 to climb these rocks.")
            return
        }
        val up = rocks.x == 2794 && coords.x >= 2795
        val down = rocks.x == 2792 && coords.x <= 2791
        if (!up && !down) return
        val fail = random.of(100) < (55 - stat(stats.agility)).coerceAtLeast(0)
        faceLoc(rocks)
        crossObstacle(CoordGrid(if (up && !fail) 2791 else 2795, 2979), GnomeSequences.climb, 4)
        if (fail) {
            mes("You fall and hurt yourself.")
            val damage = statBase(stats.hitpoints) / 20 + 1
            repeat(2) { takeInstantHit(HitType.Typeless, damage) }
        } else statAdvance(stats.agility, 1.0)
        if (up && !fail) {
            previousX[player] = coords.x
            timer(CairnIsleTimers.bridge, 1)
        }
    }

    private fun ProtectedAccess.checkBridge() {
        val previous = previousX.put(player, coords.x)
        if (!nearIsle(coords)) {
            previousX.remove(player)
            player.clearTimer(CairnIsleTimers.bridge)
            return
        }
        if (coords.z == 2979 && previous != null) {
            val crossesMiddle =
                (previous > 2779 && coords.x <= 2779) || (previous < 2779 && coords.x >= 2779)
            if (
                crossesMiddle &&
                    random.of(100) < 2 + (99 - stat(stats.agility)).coerceAtLeast(0) / 3
            ) {
                val damage = statBase(stats.hitpoints) / 11
                mes("You lose your footing and are washed back to shore.")
                telejump(CoordGrid(2796, 2977))
                if (damage > 0) repeat(3) { takeInstantHit(HitType.Typeless, damage) }
                previousX.remove(player)
                player.clearTimer(CairnIsleTimers.bridge)
                return
            }
            if (previous > 2771 && coords.x <= 2771)
                player.completeDiaryTask(DiaryTask.ExploreCairnIsle)
        }
        timer(CairnIsleTimers.bridge, 1)
    }

    private fun nearIsle(tile: CoordGrid): Boolean =
        tile.level == 0 && tile.x in 2753..2794 && tile.z in 2960..2995
}
