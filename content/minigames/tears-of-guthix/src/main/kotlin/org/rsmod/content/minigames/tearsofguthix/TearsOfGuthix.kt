package org.rsmod.content.minigames.tearsofguthix

import jakarta.inject.Inject
import java.time.Instant
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object TearLocs : LocReferences() {
    val juna = find("tog_juna_2ops")
    val before = find("tog_juna_1op")
    val down = find("tog_cave_down")
    val up = find("tog_cave_up")
    val wall = find("tog_weepingwall")
    val streams =
        listOf("r", "l").map { side ->
            listOf("off", "good", "bad").map { find("tog_weeping_wall_${it}_$side") }
        }
}

internal object TearBits : VarBitReferences() {
    val quest = find("tog_juna_bowl")
    val collecting = find("tog_minigame_collecting")
    val tears = find("tog_tears_collected")
    val best = find("tog_max_tears_collected")
    val qp = find("tog_qp_before_return")
    val billion = find("tog_xp_billions")
    val countdown = find("tog_countdown")
    val house = find("poh_house_location")
}

internal object TearVars : VarpReferences() {
    val last = find("tears_last_visit")
    val xp = find("tears_previous_xp")
    val druid = find("druidquest")
    val rune = find("runemysteries")
}

internal object TearVarConfig : VarpBuilder() {
    init {
        for (n in listOf("tears_last_visit", "tears_previous_xp")) build(n) {
            permanent = true
            transmitNever = true
        }
    }
}

internal object TearItems : ObjReferences() {
    val bowl = find("tog_bowl")
}

internal object TearTimer : TimerReferences() {
    val tick = find("tears_collection_tick")
}

internal object TearUI : InterfaceReferences() {
    val overlay = find("tog_sidepanel")
}

private val entrance = CoordGrid(3251, 9517, 2)
private val centre = CoordGrid(3257, 9517, 2)
private val order =
    listOf(
        stats.cooking,
        stats.crafting,
        stats.firemaking,
        stats.fishing,
        stats.magic,
        stats.mining,
        stats.prayer,
        stats.ranged,
        stats.runecrafting,
        stats.smithing,
        stats.woodcutting,
        stats.agility,
        stats.herblore,
        stats.fletching,
        stats.thieving,
        stats.slayer,
        stats.attack,
        stats.defence,
        stats.strength,
        stats.hitpoints,
        stats.farming,
        stats.construction,
        stats.hunter,
    )

private data class Wall(val tile: CoordGrid, val side: Int, val angle: LocAngle)

private val walls =
    listOf(
        Wall(CoordGrid(3257, 9514, 2), 0, LocAngle.North),
        Wall(CoordGrid(3258, 9514, 2), 1, LocAngle.North),
        Wall(CoordGrid(3259, 9514, 2), 0, LocAngle.North),
        Wall(CoordGrid(3257, 9520, 2), 1, LocAngle.South),
        Wall(CoordGrid(3258, 9520, 2), 0, LocAngle.South),
        Wall(CoordGrid(3259, 9520, 2), 1, LocAngle.South),
        Wall(CoordGrid(3261, 9516, 2), 0, LocAngle.West),
        Wall(CoordGrid(3261, 9517, 2), 1, LocAngle.West),
        Wall(CoordGrid(3261, 9518, 2), 0, LocAngle.West),
    )

private class Visit(
    val ends: Int,
    var start: Int,
    var selected: Int = -1,
    var position: CoordGrid? = null,
)

public class TearsOfGuthix
@Inject
constructor(
    private val locations: LocRepository,
    private val clock: MapClock,
    private val events: EventBus,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
) : PluginScript() {
    private val visits = mutableMapOf<Player, Visit>()
    private val stream = intArrayOf(1, 1, 1, 2, 2, 2, 0, 0, 0)
    private val active = intArrayOf(3, 4, 5, 0, 1, 2)
    private var changed = -1

    override fun ScriptContext.startup() {
        onOpLoc1(TearLocs.down) { telejump(CoordGrid(3219, 9532, 2)) }
        onOpLoc1(TearLocs.up) { telejump(CoordGrid(3225, 9538)) }
        onOpLoc1(TearLocs.before) {
            mes("Complete the Tears of Guthix quest to collect tears from the walls.")
        }
        onOpLoc1(TearLocs.juna) { enter() }
        onOpLoc2(TearLocs.juna) { enter() }
        onOpLoc1(TearLocs.wall) {
            val visit = visits[player] ?: return@onOpLoc1
            val i = walls.indexOfFirst { w -> w.tile == it.loc.coords }
            if (i >= 0) {
                visit.selected = i
                visit.position = player.coords
                faceLoc(it.loc)
            }
        }
        onPlayerSoftTimer(TearTimer.tick) {
            val visit = visits[player] ?: return@onPlayerSoftTimer
            if (clock.cycle >= visit.ends) {
                finish(player, true)
                return@onPlayerSoftTimer
            }
            updateStreams()
            VarPlayerIntMapSetter.set(
                player,
                TearBits.countdown,
                (visit.ends - clock.cycle).coerceAtMost(1023),
            )
            if (
                clock.cycle >= visit.start && visit.selected >= 0 && player.coords == visit.position
            ) {
                val change =
                    when (stream[visit.selected]) {
                        1 -> 1
                        2 -> -1
                        else -> 0
                    }
                VarPlayerIntMapSetter.set(
                    player,
                    TearBits.tears,
                    (player.vars[TearBits.tears] + change).coerceIn(0, 511),
                )
            } else if (visit.position != player.coords) visit.selected = -1
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> { if (player in visits) finish(player, false) }
        onEvent<PlayerTeleportEvent.Start> { if (player in visits) finish(player, false) }
        onPlayerLogout {
            if (player in visits) {
                finish(player, false)
                move(player, entrance)
            }
        }
        onPlayerLogin {
            if (player.vars[TearBits.collecting] != 0) {
                cleanup(player)
                move(player, entrance)
            }
        }
    }

    private fun ProtectedAccess.enter() {
        if (!player.members || vars[TearBits.quest] < 2 || player in visits) return
        if (player.righthand != null || player.lefthand != null) {
            mes("You need both hands free to hold the stone bowl.")
            return
        }
        val qp = vars[varps.quest_points]
        val last = vars[TearVars.last]
        val now = Instant.now().epochSecond
        if (last > 0 && now - last < 7 * 86400L) {
            mes("Seven full days must pass between visits.")
            return
        }
        val old = vars[TearBits.billion] * 1_000_000_000L + vars[TearVars.xp]
        if (last > 0 && qp <= vars[TearBits.qp] && totalXp(player) - old < 100000) {
            mes("Earn one quest point or 100,000 experience before telling another story.")
            return
        }
        if (qp <= 0) return
        if (visits.isEmpty()) {
            for (i in walls.indices) show(i)
            changed = clock.cycle
        }
        visits[player] = Visit(clock.cycle + qp, clock.cycle + 4)
        vars[TearBits.collecting] = 1
        vars[TearBits.tears] = 0
        vars[TearBits.countdown] = qp.coerceAtMost(1023)
        player.righthand = InvObj(TearItems.bowl)
        telejump(centre)
        player.ifOpenOverlay(TearUI.overlay, events)
        player.softTimer(TearTimer.tick, 1)
    }

    private fun updateStreams() {
        if (changed == clock.cycle) return
        changed = clock.cycle
        val slot = clock.cycle % 16
        if (slot !in active.indices) return
        val old = active[slot]
        val empty = walls.indices.filter { stream[it] == 0 }
        val next = empty[random.of(empty.size)]
        stream[next] = stream[old]
        stream[old] = 0
        active[slot] = next
        show(old)
        show(next)
    }

    private fun show(i: Int) {
        val w = walls[i]
        locations.add(
            w.tile,
            TearLocs.streams[w.side][stream[i]],
            Int.MAX_VALUE,
            w.angle,
            LocShape.WallDecorStraightNoOffset,
        )
    }

    private fun totalXp(p: Player) = order.sumOf { p.statMap.getXP(it).toLong() }

    private fun finish(p: Player, reward: Boolean) {
        if (visits.remove(p) == null) return
        val tears = p.vars[TearBits.tears]
        cleanup(p)
        if (!reward) return
        move(p, entrance)
        val skill =
            order
                .filter {
                    when (it) {
                        stats.construction -> p.vars[TearBits.house] > 0
                        stats.herblore -> p.vars[TearVars.druid] >= 4
                        stats.runecrafting -> p.vars[TearVars.rune] >= 6
                        else -> true
                    }
                }
                .minBy { p.statMap.getXP(it) }
        val level = p.statBase(skill)
        val xp = (level * level / 15.0).coerceIn(10.0, 60.0) * tears
        val bonus =
            if (p.vars[DiaryRegion.Lumbridge.tier(DiaryDifficulty.Hard).complete] != 0) 1.1 else 1.0
        p.statAdvance(skill, xp * bonus)
        val total = totalXp(p)
        VarPlayerIntMapSetter.set(p, TearVars.last, Instant.now().epochSecond.toInt())
        VarPlayerIntMapSetter.set(p, TearVars.xp, (total % 1_000_000_000L).toInt())
        VarPlayerIntMapSetter.set(p, TearBits.billion, (total / 1_000_000_000L).toInt())
        VarPlayerIntMapSetter.set(p, TearBits.qp, p.vars[varps.quest_points].coerceAtMost(1023))
        VarPlayerIntMapSetter.set(p, TearBits.best, maxOf(p.vars[TearBits.best], tears))
        if (tears >= 100) p.completeDiaryTask(DiaryTask.CollectHundredGuthixTears)
        p.mes("You drink $tears tears and gain experience in ${skill.internalName}.")
    }

    private fun cleanup(p: Player) {
        if (p.righthand?.id == TearItems.bowl.id) p.righthand = null
        p.clearSoftTimer(TearTimer.tick)
        p.ifCloseOverlay(TearUI.overlay, events)
        for (bit in
            listOf(TearBits.collecting, TearBits.tears, TearBits.countdown)) VarPlayerIntMapSetter
            .set(p, bit, 0)
    }

    private fun move(p: Player, c: CoordGrid) = PathingEntityCommon.telejump(p, collision, c)
}
