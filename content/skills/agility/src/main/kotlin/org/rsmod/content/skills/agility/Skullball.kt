package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object SkullballTimers : TimerReferences() {
    val watch = find("werewolf_skullball_round")
}

public class Skullball
@Inject
constructor(
    private val npcTypes: NpcTypeList,
    private val npcs: NpcRepository,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private data class Goal(val x: Int, val z: Int, val dx: Int, val dz: Int)

    private data class Game(
        val ball: Npc,
        var goal: Int = 0,
        var started: Int = 0,
        var dx: Int = 0,
        var dz: Int = 0,
        var remaining: Int = 0,
    )

    private val games = WeakHashMap<Player, Game>()
    private val validator = StepValidator(collision)
    private val goals =
        listOf(
            Goal(3555, 9870, 0, 1),
            Goal(3556, 9883, 0, 1),
            Goal(3558, 9891, 0, 1),
            Goal(3557, 9900, 0, 1),
            Goal(3558, 9906, 0, 1),
            Goal(3563, 9911, 1, 0),
            Goal(3575, 9905, 0, -1),
            Goal(3574, 9888, 0, -1),
            Goal(3575, 9878, 0, -1),
            Goal(3568, 9864, -1, 0),
            Goal(3563, 9866, 0, 1),
        )

    override fun ScriptContext.startup() {
        onOpNpc1(WolfActors.skullBoss) {
            if (!wolfEntry(25)) return@onOpNpc1
            if (
                games.containsKey(player) &&
                    !choice2("Restart my game.", true, "Continue my game.", false)
            )
                return@onOpNpc1
            clear(player)
            val start =
                listOf(
                    CoordGrid(3552, 9861),
                    CoordGrid(3554, 9862),
                    CoordGrid(3555, 9862),
                    CoordGrid(3557, 9861),
                )
            val ball = Npc(npcTypes[WolfActors.ball], start[random.of(start.size)])
            npcs.add(ball, 2500)
            games[player] = Game(ball)
            hint(player, goals.first())
            timer(SkullballTimers.watch, 1)
            mes("Kick the skull through the marked goals, then into the hole at the finish.")
        }
        onOpNpc1(WolfActors.coach) {
            val game = games[player]
            if (game == null) mes("Talk to the Skullball Boss to start a game.")
            else {
                hint(player, goals[game.goal])
                mes("You have passed ${game.goal} of the ten goals.")
            }
        }
        onOpNpc1(WolfActors.ball) { kick(it.npc, 1) }
        onOpNpc3(WolfActors.ball) { kick(it.npc, 4) }
        onOpNpc4(WolfActors.ball) { kick(it.npc, 9) }
        onOpNpc5(WolfActors.ball) {
            val game = games[player] ?: return@onOpNpc5
            if (game.ball === it.npc) hint(player, goals[game.goal])
        }
        onPlayerLogout { clear(player) }
        onPlayerTimer(SkullballTimers.watch) {
            val game = games[player]
            if (
                game == null ||
                    player.coords.x !in 3547..3582 ||
                    player.coords.z !in 9858..9918 ||
                    !game.ball.isSlotAssigned
            )
                clear(player)
            else if (game.remaining > 0) roll(game)
        }
    }

    private fun ProtectedAccess.kick(ball: Npc, distance: Int) {
        val game = games[player] ?: return
        if (ball !== game.ball || coords.level != ball.coords.level) return
        val current = ball.coords
        val dx = (current.x - coords.x).coerceIn(-1, 1)
        val dz = (current.z - coords.z).coerceIn(-1, 1)
        if (dx == 0 && dz == 0) return
        anim(WolfMoves.kick)
        game.dx = dx
        game.dz = dz
        game.remaining = distance
    }

    private fun ProtectedAccess.roll(game: Game) {
        val current = game.ball.coords
        var dx = game.dx
        var dz = game.dz
        fun goalAhead(): Boolean {
            val goal = goals[game.goal]
            return current.x + dx == goal.x &&
                current.z + dz == goal.z &&
                dx == goal.dx &&
                dz == goal.dz
        }
        fun passable(x: Int, z: Int) =
            validator.canTravel(current.level, current.x, current.z, x, z)
        if (!goalAhead() && !passable(dx, dz)) {
            val horizontal = dx != 0 && !passable(dx, 0)
            val vertical = dz != 0 && !passable(0, dz)
            if (horizontal || !vertical) dx = -dx
            if (vertical || !horizontal) dz = -dz
            game.dx = dx
            game.dz = dz
            if (!goalAhead() && !passable(dx, dz)) {
                game.remaining = 0
                return
            }
        }
        val next = current.translate(dx, dz)
        game.remaining--
        val target = goals[game.goal]
        val crosses = next.x == target.x && next.z == target.z && dx == target.dx && dz == target.dz
        if (crosses) {
            if (game.goal == 0) game.started = player.currentMapClock
            game.goal++
            if (game.goal == goals.size) {
                val overtime = (player.currentMapClock - game.started - 400).coerceAtLeast(0)
                val xp = (750 - 8 * (overtime / 5)).coerceAtLeast(0)
                clear(player)
                recordLap(AgilityLap.Skullball)
                statAdvance(stats.agility, xp.toDouble())
                mes("You finish the Skullball course and earn $xp Agility experience.")
                return
            }
            hint(player, goals[game.goal])
            mes("Goal! You have passed ${game.goal} of the ten goals.")
        }
        game.ball.telejump(collision, next)
    }

    private fun hint(player: Player, goal: Goal) {
        player.client.write(
            HintArrow(
                HintArrow.TileHintArrow(
                    goal.x,
                    goal.z,
                    80,
                    HintArrow.TileHintArrow.HintArrowTilePosition.CENTER,
                )
            )
        )
    }

    private fun clear(player: Player) {
        val game = games.remove(player) ?: return
        if (game.ball.isSlotAssigned) npcs.del(game.ball, Int.MAX_VALUE)
        player.clearTimer(SkullballTimers.watch)
        player.client.write(HintArrow(HintArrow.ResetHintArrow))
    }
}
