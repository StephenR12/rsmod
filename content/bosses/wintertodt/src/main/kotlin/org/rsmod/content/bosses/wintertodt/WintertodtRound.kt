package org.rsmod.content.bosses.wintertodt

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.queueDeath
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.skills.firemaking.FiremakingXp
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

public var Player.winterPoints: Int by intVarp(WinterVars.points)
public var Player.winterRound: Int by intVarp(WinterVars.round)
public var Player.winterSession: Int by intVarp(WinterVars.session)
public var Player.winterLastCold: Int by intVarp(WinterVars.lastCold)
public var Player.winterWarmth: Int by intVarBit(WinterBits.warmth)
public var Player.winterRewards: Int by intVarBit(WinterBits.rewards)
private var Player.winterKills: Int by intVarp(WinterVars.kills)

public enum class WinterBrazier {
    Unlit,
    Lit,
    Broken,
}

public data class WinterStation(
    val coords: CoordGrid,
    val pyroCoords: CoordGrid,
    var state: WinterBrazier = WinterBrazier.Unlit,
    var health: Int = 14,
    var npc: Npc? = null,
    var nextDrain: Int = 0,
)

private data class WinterParticipant(var interruptions: Int = 0, var chopping: Boolean = false)

private data class WinterSnow(val due: Int, val kind: Int, val tile: CoordGrid, val station: Int)

/**
 * Exact published energy, score and warmth formulas; random attack cadence is a documented model.
 */
@Singleton
public class WintertodtRound
@Inject
constructor(
    private val clock: MapClock,
    private val locs: LocRepository,
    private val collision: CollisionFlagMap,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val controllers: ControllerRepository,
    private val random: GameRandom,
    private val events: EventBus,
    private val xp: FiremakingXp,
) {
    public val stations =
        listOf(
            WinterStation(CoordGrid(1620, 3997), CoordGrid(1619, 3996)),
            WinterStation(CoordGrid(1638, 3997), CoordGrid(1641, 3996)),
            WinterStation(CoordGrid(1620, 4015), CoordGrid(1619, 4018)),
            WinterStation(CoordGrid(1638, 4015), CoordGrid(1641, 4018)),
        )
    private val players = WeakHashMap<Player, WinterParticipant>()
    private val snow = mutableListOf<WinterSnow>()
    public var active: Boolean = false
        private set

    public var energy: Int = 100
        private set

    public var roundId: Int = 1
        private set

    private var nextRound = clock + 100
    private var regenEnergy = 0
    private var started = false

    public fun startup() {
        if (started) return
        started = true
        for (station in stations) {
            station.npc =
                Npc(npcTypes[WinterNpcs.pyro], station.pyroCoords).also {
                    npcs.add(it, Int.MAX_VALUE)
                }
            change(station, WinterBrazier.Unlit)
        }
        val controller = Controller(WinterControllers.round, CoordGrid(1630, 4006))
        controller.aiTimer(1)
        controllers.add(controller, Int.MAX_VALUE)
    }

    public fun join(player: Player) {
        if (!inside(player.coords) || !player.members || player.statBase(stats.firemaking) < 50)
            return
        if (player in players) return
        players[player] = WinterParticipant()
        player.winterSession = 1
        player.winterRound = roundId
        player.winterPoints = 0
        player.winterWarmth = 1000
        player.winterLastCold = clock.cycle - 10
        player.ifOpenOverlay(WinterInterfaces.hud, events)
        player.runClientScript(1433)
        player.softTimer(WinterTimers.warmth, 1)
        display(player)
    }

    public fun leave(player: Player) {
        players.remove(player)
        player.winterSession = 0
        player.winterPoints = 0
        for (slot in player.inv.indices) if (player.inv[slot]?.id in 20695..20702)
            player.inv[slot] = null
        player.ifCloseOverlay(WinterInterfaces.hud, events)
    }

    public fun eligible(player: Player): Boolean =
        inside(player.coords) &&
            player.members &&
            player.statBase(stats.firemaking) >= 50 &&
            player.winterSession == 1

    public fun participating(player: Player): Boolean =
        active && eligible(player) && player.winterRound == roundId

    public fun beginRound() {
        active = true
        energy = 100
        roundId++
        regenEnergy = clock + 35
        snow.clear()
        for (station in stations) {
            station.health = 14
            station.npc?.resetTransmog()
            station.nextDrain = clock + 14
            change(station, WinterBrazier.Unlit)
        }
        for (player in players.keys.toList()) if (eligible(player)) {
            player.winterRound = roundId
            player.winterPoints = 0
            display(player)
        }
    }

    public fun tick() {
        for (attack in snow.filter { it.due <= clock.cycle }.toList()) {
            snow.remove(attack)
            impact(attack)
        }
        if (!active) {
            if (clock.cycle >= nextRound) beginRound()
            return
        }
        for (station in stations) if (
            station.state == WinterBrazier.Lit &&
                station.health > 0 &&
                clock.cycle >= station.nextDrain
        ) {
            energy = (energy - 1).coerceAtLeast(0)
            station.nextDrain = clock + 14
        }
        if (
            stations.none { it.state == WinterBrazier.Lit && it.health > 0 } &&
                clock.cycle >= regenEnergy
        ) {
            energy = (energy + 1).coerceAtMost(100)
            regenEnergy = clock + 35
        }
        if (energy == 0) {
            finishRound()
            return
        }
        // Exact cadence is not published. Check every20 ticks with more structure attacks at low
        // energy.
        if (
            clock % 20 == 0 && random.of(100) < if (energy < 40) 60 else if (energy < 80) 25 else 10
        )
            attack()
        if (clock % 10 == 0)
            for (player in players.keys.toList()) if (eligible(player)) display(player)
    }

    private fun finishRound() {
        active = false
        nextRound = clock + 100
        snow.clear()
        for (player in players.keys.toList()) {
            if (!participatingAtFinish(player)) continue
            val points = player.winterPoints
            if (points >= 500) {
                val additional =
                    1 +
                        points / 500 +
                        if (points % 500 > 0 && random.of(500) < points % 500) 1 else 0
                player.winterRewards = (player.winterRewards + additional).coerceAtMost(8000)
                player.statAdvance(stats.firemaking, player.statBase(stats.firemaking) * 100.0)
                player.winterKills++
                player.completeDiaryTask(DiaryTask.SubdueWintertodt)
                player.mes(
                    "You have subdued the Wintertodt. You have ${player.winterRewards} rewards to collect from the cart."
                )
            } else
                player.mes(
                    "You did not earn enough points to be worthy of a gift from the citizens of Kourend this time."
                )
            player.winterPoints = 0
            display(player)
        }
        for (station in stations) change(station, WinterBrazier.Unlit)
    }

    private fun participatingAtFinish(player: Player): Boolean =
        eligible(player) && player.winterRound == roundId

    public fun score(player: Player, amount: Int) {
        if (!participating(player) || amount <= 0) return
        player.winterPoints =
            (player.winterPoints.toLong() + amount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        display(player)
    }

    public fun markAction(player: Player, chopping: Boolean): Int {
        val state = players.getOrPut(player) { WinterParticipant() }
        state.chopping = chopping
        return state.interruptions
    }

    public fun uninterrupted(player: Player, mark: Int): Boolean =
        players[player]?.interruptions == mark

    public fun station(coords: CoordGrid): WinterStation? =
        stations.firstOrNull { it.coords == coords }

    public fun pyro(npc: Npc): WinterStation? = stations.firstOrNull { it.pyroCoords == npc.coords }

    public fun light(station: WinterStation, player: Player): Boolean {
        if (!participating(player) || station.health <= 0 || station.state != WinterBrazier.Unlit)
            return false
        change(station, WinterBrazier.Lit)
        station.nextDrain = clock + random.of(2, 8)
        score(player, 25)
        player.statAdvance(
            stats.firemaking,
            player.statBase(stats.firemaking) * 6.0 * xp.multiplier(player),
        )
        return true
    }

    public fun repair(station: WinterStation, player: Player): Boolean {
        if (!participating(player) || station.state != WinterBrazier.Broken) return false
        change(station, WinterBrazier.Unlit)
        score(player, 25)
        player.statAdvance(stats.construction, player.statBase(stats.construction) * 4.0)
        return true
    }

    public fun heal(station: WinterStation, player: Player) {
        station.health = 14
        station.npc?.resetTransmog()
        station.nextDrain = clock + random.of(2, 8)
        if (participating(player)) score(player, 75)
    }

    public fun warmthTick(player: Player) {
        if (!eligible(player)) {
            if (player.winterSession != 0) leave(player)
            return
        }
        player.softTimer(WinterTimers.warmth, 1)
        if (clock % 100 == 0) restore(player, 80 + 10 * warmItems(player))
        if (
            active &&
                !safe(player.coords) &&
                clock % 10 == 0 &&
                clock.cycle - player.winterLastCold >= 10 &&
                random.of(100) < if (energy > 60) 20 else if (energy > 20) 5 else 1
        ) {
            player.winterLastCold = clock.cycle
            hurt(player, 0)
        }
        if (clock % 10 == 0) display(player)
    }

    public fun restore(player: Player, amount: Int) {
        if (eligible(player)) {
            player.winterWarmth = (player.winterWarmth + amount).coerceAtMost(1000)
            display(player)
        }
    }

    public fun warmItems(player: Player): Int =
        player.worn
            .count { obj -> obj != null && WinterWarmClothing.all.any { it.id == obj.id } }
            .coerceAtMost(4)

    public fun damagePercent(player: Player, kind: Int): Int {
        val warm = warmItems(player)
        val fm = player.statBase(stats.firemaking).coerceAtLeast(1)
        val lit = stations.count { it.state == WinterBrazier.Lit }
        return when (kind) {
            0 -> (16 - warm - minOf(2 * lit, 6)) * 100 / fm
            1 -> 2 * ((10 - warm) * 100 / fm)
            else -> 3 * ((10 - warm) * 100 / fm)
        }
    }

    private fun hurt(player: Player, kind: Int) {
        if (!eligible(player)) return
        player.winterWarmth =
            (player.winterWarmth - damagePercent(player, kind) * 10).coerceAtLeast(0)
        players[player]?.let { if (kind != 0 || !it.chopping) it.interruptions++ }
        player.mes("The cold of the Wintertodt seeps into your bones.")
        if (player.winterWarmth == 0) {
            player.statSub(stats.hitpoints, player.hitpoints, 0)
            leave(player)
            player.queueDeath()
        }
    }

    private fun attack() {
        val kind = random.of(4)
        val index = random.of(stations.size)
        val station = stations[index]
        if (kind == 3) {
            station.health = (station.health - 7).coerceAtLeast(0)
            if (station.health == 0) station.npc?.transmog(npcTypes[WinterNpcs.down], Int.MAX_VALUE)
            return
        }
        val candidates =
            players.keys.filter { eligible(it) && !safe(it.coords) && canSnowTarget(it.coords) }
        if (kind == 0 && candidates.isEmpty()) return
        val tile =
            if (kind == 0) candidates[random.of(candidates.size)].coords
            else station.coords.translate(1, 1)
        locs.add(tile, WinterLocs.warning, 3, LocAngle.West, LocShape.CentrepieceStraight)
        snow += WinterSnow(clock + 3, kind, tile, index)
    }

    private fun impact(attack: WinterSnow) {
        if (!active) return
        val station = stations[attack.station]
        when (attack.kind) {
            1 -> change(station, WinterBrazier.Broken)
            2 -> change(station, WinterBrazier.Unlit)
        }
        if (attack.kind == 2) return
        locs.add(attack.tile, WinterLocs.snow, 3, LocAngle.West, LocShape.CentrepieceStraight)
        val radius = if (attack.kind == 1) 2 else 1
        for (player in players.keys.toList()) if (
            player.coords.level == attack.tile.level &&
                kotlin.math.abs(player.coords.x - attack.tile.x) <= radius &&
                kotlin.math.abs(player.coords.z - attack.tile.z) <= radius
        )
            hurt(player, if (attack.kind == 1) 1 else 2)
    }

    private fun change(station: WinterStation, state: WinterBrazier) {
        station.state = state
        val type =
            when (state) {
                WinterBrazier.Unlit -> WinterLocs.unlit
                WinterBrazier.Lit -> WinterLocs.lit
                WinterBrazier.Broken -> WinterLocs.broken
            }
        locs.add(station.coords, type, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    public fun canSnowTarget(tile: CoordGrid): Boolean =
        (-1..1).all { dx -> (-1..1).all { dz -> !collision.isWalkBlocked(tile.translate(dx, dz)) } }

    public fun display(player: Player) {
        // Cache233 clientscripts1421 takes points, energy0..3500, four down flags and four brazier
        // states.
        val args =
            listOf<Any>(player.winterPoints, energy * 35) +
                stations.map { if (it.health == 0) 1 else 0 } +
                stations.map { it.state.ordinal }
        player.runClientScript(1421, args)
        player.ifSetText(
            WinterComponents.status,
            if (active) "Subdue the Wintertodt!"
            else "The Wintertodt returns in ${((nextRound-clock.cycle).coerceAtLeast(0)*3+4)/5}s.",
        )
    }

    public companion object {
        public fun inside(tile: CoordGrid): Boolean =
            tile.level == 0 && tile.x in 1600..1663 && tile.z in 3968..4031

        public fun safe(tile: CoordGrid): Boolean = tile.z <= 3984
    }
}
