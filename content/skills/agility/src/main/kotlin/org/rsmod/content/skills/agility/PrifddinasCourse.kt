package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal enum class PrifObstacle(
    val key: String,
    val landing: CoordGrid,
    val xp: Double,
    val movement: SeqType,
    val ticks: Int,
) {
    Start("start_ladder", CoordGrid(3255, 6109, 2), 11.5, GnomeSequences.climb, 3),
    BankRope("tightrope_start1", CoordGrid(3272, 6105, 2), 30.7, GnomeSequences.balance, 8),
    Chimney("chimney_jump", CoordGrid(3269, 6112, 2), 28.1, ArenaMoves.jump, 3),
    RoofEdge("roof_jump", CoordGrid(3270, 6116), 23.0, ArenaMoves.jump, 3),
    FirstHole("dark_hole_active", CoordGrid(3293, 6141), 11.5, GnomeSequences.crawl, 5),
    TreeLadder("tree_ladder_long", CoordGrid(3293, 6144, 2), 0.0, GnomeSequences.climb, 3),
    FirstBridge("rope_bridge1", CoordGrid(3281, 6142, 2), 25.6, GnomeSequences.balance, 5),
    FirstTreeRope("tightrope1", CoordGrid(3270, 6147, 2), 30.7, GnomeSequences.balance, 6),
    SecondBridge("rope_bridge2", CoordGrid(3270, 6158, 2), 25.6, GnomeSequences.balance, 5),
    SecondTreeRope("tightrope2", CoordGrid(3274, 6168, 2), 30.7, GnomeSequences.balance, 6),
    ThirdTreeRope("tightrope3", CoordGrid(3284, 6176), 30.7, GnomeSequences.balance, 7),
    Finish("dark_hole_end", CoordGrid(3240, 6109), 1037.1, GnomeSequences.crawl, 5),
}

internal object PrifLocs : LocReferences() {
    val obstacles = PrifObstacle.entries.associateWith { find("prif_agility_${it.key}") }
    val recovery = find("prif_agility_tree_ladder_short")
    val portals = (1..6).map { find("prif_agility_shortcut_portal_$it") }
}

internal object PrifNpcs : NpcReferences() {
    val guide = find("prif_agility")
}

internal object PrifItems : ObjReferences() {
    val shard = find("prif_crystal_shard")
}

internal object PrifBits : VarBitReferences() {
    val portal = find("prif_agility_shortcut_transmit")
}

public class PrifddinasCourse @Inject constructor(private val ground: ObjRepository) :
    PluginScript() {
    private data class Lap(
        val start: Int,
        var next: Int,
        var portal: Int,
        var fell: Boolean = false,
    )

    private val laps = WeakHashMap<Player, Lap>()
    private val skips =
        listOf(
            PrifObstacle.BankRope,
            PrifObstacle.FirstHole,
            PrifObstacle.FirstTreeRope,
            PrifObstacle.SecondBridge,
            PrifObstacle.SecondTreeRope,
            PrifObstacle.ThirdTreeRope,
        )

    override fun ScriptContext.startup() {
        for ((stage, type) in PrifLocs.obstacles) {
            onOpLoc1(type) { traverse(stage) }
            onApLoc1(type) { if (isWithinApRange(it.loc, 2)) traverse(stage) }
        }
        for ((index, type) in PrifLocs.portals.withIndex()) {
            onOpLoc1(type) { portal(index) }
            onApLoc1(type) { if (isWithinApRange(it.loc, 2)) portal(index) }
        }
        onOpLoc1(PrifLocs.recovery) {
            val lap = laps[player] ?: return@onOpLoc1
            if (!lap.fell) return@onOpLoc1
            crossObstacle(PrifObstacle.FirstTreeRope.landing, GnomeSequences.climb, 3)
            lap.fell = false
        }
        onOpNpc1(PrifNpcs.guide) { records() }
        onOpNpc2(PrifNpcs.guide) { records() }
        onPlayerLogout {
            laps.remove(player)
            VarPlayerIntMapSetter.set(player, PrifBits.portal, 0)
        }
    }

    private suspend fun ProtectedAccess.traverse(stage: PrifObstacle) {
        if (!player.members || vars[varbits.song_of_the_elves] < 200) {
            mes("You must complete Song of the Elves to use this course.")
            return
        }
        val lap =
            if (stage == PrifObstacle.Start) {
                if (stat(stats.agility) < 75) {
                    mes("You need an Agility level of 75 to climb this ladder.")
                    return
                }
                Lap(player.currentMapClock, 0, random.of(6) + 1).also {
                    laps[player] = it
                    vars[PrifBits.portal] = it.portal
                }
            } else laps[player] ?: return
        if (lap.fell || lap.next != stage.ordinal) return
        val level = stat(stats.agility)
        if (
            stage in
                listOf(PrifObstacle.BankRope, PrifObstacle.Chimney, PrifObstacle.FirstTreeRope) &&
                level < 91 &&
                random.of(100) < 91 - level
        ) {
            if (stage == PrifObstacle.FirstTreeRope) {
                telejump(CoordGrid(3268, 6145))
                lap.fell = true
                lap.next = PrifObstacle.SecondBridge.ordinal
            } else {
                telejump(CoordGrid(3253, 6109))
                vars[PrifBits.portal] = 0
                laps.remove(player)
            }
            takeInstantHit(HitType.Typeless, random.of(2..5))
            return
        }
        crossObstacle(stage.landing, stage.movement, stage.ticks)
        if (stage.xp > 0) statAdvance(stats.agility, stage.xp)
        lap.next++
        if (stage == PrifObstacle.Finish) {
            val time = player.currentMapClock - lap.start
            if (time > 0 && (vars[LapVars.prifBest] == 0 || time < vars[LapVars.prifBest]))
                vars[LapVars.prifBest] = time
            vars[PrifBits.portal] = 0
            laps.remove(player)
            recordLap(AgilityLap.Prifddinas)
        }
    }

    private suspend fun ProtectedAccess.portal(index: Int) {
        val lap = laps[player] ?: return
        val skip = skips[index]
        if (lap.fell || lap.portal != index + 1 || lap.next != skip.ordinal) return
        crossObstacle(skip.landing, ArenaMoves.step, 2)
        statAdvance(stats.agility, 82.0)
        lap.next++
        lap.portal = 0
        vars[PrifBits.portal] = 0
        if (invAdd(inv, PrifItems.shard).failure)
            ground.add(PrifItems.shard, coords, duration = 100, receiver = player)
    }

    private fun ProtectedAccess.records() {
        val count = vars[LapVars.counts.getValue(AgilityLap.Prifddinas)]
        val best = vars[LapVars.prifBest]
        mes("You have completed $count laps of the Prifddinas course.")
        if (best > 0) mes("Your fastest lap took ${best*6/10}.${best*6%10} seconds.")
    }
}
