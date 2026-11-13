package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GnomeObstacles : LocReferences() {
    val log = find("gnome_log_balance1")
    val firstNet = find("obstical_net2")
    val branch = find("climbing_branch")
    val rope = find("balancing_rope")
    val tree = find("climbing_tree")
    val treeSides = find("climbing_tree2")
    val lastNet = find("obstical_net3")
    val westPipe = find("obstical_pipe3_1")
    val eastPipe = find("obstical_pipe3_2")
}

internal object GnomeSequences : SeqReferences() {
    val swing = find("human_ropeswing")
    val balance = find("human_walk_logbalance_loop")
    val climb = find("human_climbing")
    val descend = find("human_climbing_down")
    val crawl = find("human_doublepipesqueeze")
}

public class GnomeStrongholdCourse @Inject constructor(private val marks: CourseMarks) :
    PluginScript() {
    private val nextObstacle = WeakHashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpLoc1(GnomeObstacles.log) { cross(it.loc, Stage.Log) }
        onOpLoc1(GnomeObstacles.firstNet) { cross(it.loc, Stage.FirstNet) }
        onOpLoc1(GnomeObstacles.branch) { cross(it.loc, Stage.Branch) }
        onOpLoc1(GnomeObstacles.rope) { cross(it.loc, Stage.Rope) }
        onOpLoc1(GnomeObstacles.tree) { cross(it.loc, Stage.Tree) }
        onOpLoc1(GnomeObstacles.treeSides) { cross(it.loc, Stage.Tree) }
        onOpLoc1(GnomeObstacles.lastNet) { cross(it.loc, Stage.LastNet) }
        onOpLoc1(GnomeObstacles.westPipe) { cross(it.loc, Stage.Pipe) }
        onOpLoc1(GnomeObstacles.eastPipe) { cross(it.loc, Stage.Pipe) }
    }

    private suspend fun ProtectedAccess.cross(loc: BoundLocInfo, stage: Stage) {
        if (!player.members || loc.x !in 2471..2487 || loc.z !in 3418..3435) return
        arriveDelay()
        val landing =
            when (stage) {
                Stage.Log -> CoordGrid(2474, 3429)
                Stage.FirstNet -> CoordGrid(coords.x.coerceIn(loc.x, loc.x + 1), 3424, 1)
                Stage.Branch -> CoordGrid(2473, 3420, 2)
                Stage.Rope -> CoordGrid(2483, 3420, 2)
                Stage.Tree -> CoordGrid(2485, 3419)
                Stage.LastNet -> CoordGrid(coords.x.coerceIn(loc.x, loc.x + 1), 3427)
                Stage.Pipe -> {
                    // The northern mouths share the model and operation with the entrances.
                    if (loc.z != 3431 || coords.z > loc.z) {
                        mes("The entrance to the pipe is at the other end.")
                        return
                    }
                    CoordGrid(loc.x, 3437)
                }
            }
        faceLoc(loc)
        anim(stage.animation)
        if (landing.level == coords.level) {
            exactMove(coords, landing, 0, stage.ticks * 30, stage.direction)
            delay(stage.ticks)
        } else {
            delay(stage.ticks)
            telejump(landing)
        }
        resetAnim()
        if (coords != landing) return
        var xp = stage.xp
        if (stage == Stage.Log || nextObstacle[player] == stage.ordinal) {
            if (stage == Stage.Pipe) {
                nextObstacle.remove(player)
                xp += 50.0
                player.completeDiaryTask(DiaryTask.GnomeAgilityLap)
                marks.roll(this, 1, CoordGrid(2485, 3420, 2))
                recordLap(AgilityLap.Gnome)
            } else {
                nextObstacle[player] = stage.ordinal + 1
            }
        } else {
            nextObstacle.remove(player)
        }
        statAdvance(stats.agility, xp)
    }

    private enum class Stage(
        val xp: Double,
        val ticks: Int,
        val animation: org.rsmod.game.type.seq.SeqType,
        val direction: Int,
    ) {
        Log(10.0, 6, GnomeSequences.balance, Constants.em_face_south),
        FirstNet(10.0, 3, GnomeSequences.climb, Constants.em_face_south),
        Branch(6.5, 3, GnomeSequences.climb, Constants.em_face_south),
        Rope(10.0, 6, GnomeSequences.balance, Constants.em_face_east),
        Tree(6.5, 3, GnomeSequences.descend, Constants.em_face_east),
        LastNet(10.0, 3, GnomeSequences.climb, Constants.em_face_north),
        Pipe(7.5, 7, GnomeSequences.crawl, Constants.em_face_north),
    }
}
