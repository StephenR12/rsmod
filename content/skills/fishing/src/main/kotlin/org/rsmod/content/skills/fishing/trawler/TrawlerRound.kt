package org.rsmod.content.skills.fishing.trawler

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

public var Player.trawlerSession: Int by intVarp(TrawlerVars.session)
public var Player.trawlerRewardXp: Int by intVarp(TrawlerVars.xp)
public var Player.trawlerRewardManta: Int by intVarp(TrawlerVars.manta)
public var Player.trawlerInspected: Int by intVarp(TrawlerVars.inspected)

/**
 * Published durations and contribution are exact. Hazard cadence and gross catch are a bounded
 * model.
 */
@Singleton
public class TrawlerRound
@Inject
constructor(
    private val controllers: ControllerRepository,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val loot: TrawlerRewards,
    private val random: GameRandom,
    private val collision: org.rsmod.routefinder.collision.CollisionFlagMap,
    private val events: org.rsmod.events.EventBus,
) {
    private val players = WeakHashMap<Player, Int>()
    private var started = false
    public var active: Boolean = false
        private set

    public var countdown: Int = 0
        private set

    public var water: Int = 0
        private set

    public var torn: Boolean = false
        private set

    public var railBroken: Boolean = false
        private set

    public var tentacle: Npc? = null
        private set

    public var chops: Int = 0
        private set

    public val leaks: BooleanArray = BooleanArray(16)
    private var serial = 0
    private var elapsed = 0
    private var healthyTicks = 0
    private var participants = 0

    public fun startup() {
        if (started) return
        started = true
        val c = Controller(TrawlerControllers.round, CoordGrid(2016, 4825))
        c.aiTimer(1)
        controllers.add(c, Int.MAX_VALUE)
    }

    public fun board(player: Player) {
        players[player] = 0
        player.trawlerSession = -1
        VarPlayerIntMapSetter.set(player, TrawlerBits.points, 0)
        if (!active && countdown == 0) countdown = 100
        player.mes("The trawler will depart after the current voyage and boarding period.")
    }

    public fun login(player: Player) {
        if (atSea(player.coords) || wreck(player.coords)) {
            move(player, CoordGrid(2653, 3174))
            leave(player)
        } else if (lobby(player.coords) && player.members) board(player)
    }

    public fun leave(player: Player) {
        players.remove(player)
        player.trawlerSession = 0
        player.ifCloseOverlay(TrawlerInterfaces.overlay, events)
        VarPlayerIntMapSetter.set(player, TrawlerBits.status, 0)
    }

    public fun tick() {
        for (p in players.keys.toList()) if (
            (players[p] == 0 && !lobby(p.coords)) || (players[p] != 0 && !atSea(p.coords))
        ) {
            leave(p)
        }
        if (!active) {
            if (countdown > 0 && --countdown == 0) depart()
            return
        }
        elapsed++
        if (elapsed % 10 == 0) {
            val index = random.of(16)
            leaks[index] = true
        }
        if (elapsed % 50 == 0 && random.of(4) == 0) torn = true
        if (elapsed % 100 == 0 && tentacle == null && !railBroken && random.of(3) == 0) {
            chops = 0
            tentacle =
                Npc(types[TrawlerNpcs.tentacle], CoordGrid(2011, 4827, 1)).also {
                    npcs.add(it, Int.MAX_VALUE)
                }
        }
        if (elapsed % 5 == 0) water = (water + leaks.count { it }).coerceAtMost(100)
        if (!torn) healthyTicks += if (tentacle == null) 2 else 1
        update()
        if (water >= 100) finish(false) else if (elapsed >= 500) finish(true)
    }

    private fun depart() {
        val boarding = players.filterValues { it == 0 }.keys.toList()
        if (boarding.isEmpty()) return
        active = true
        elapsed = 0
        healthyTicks = 0
        water = 0
        torn = false
        railBroken = false
        leaks.fill(false)
        participants = boarding.size
        serial++
        for (p in boarding) {
            players[p] = serial
            p.trawlerSession = serial
            move(p, CoordGrid(2016, 4825))
            VarPlayerIntMapSetter.set(p, TrawlerBits.points, 0)
            p.completeDiaryTask(DiaryTask.BoardFishingTrawler)
            p.ifOpenOverlay(TrawlerInterfaces.overlay, events)
            p.mes("The trawler sets sail!")
        }
        update()
    }

    public fun points(player: Player): Int = player.vars[TrawlerBits.points]

    public fun playing(player: Player): Boolean =
        active && players[player] == serial && atSea(player.coords)

    public fun addPoints(player: Player, amount: Int) {
        if (playing(player))
            VarPlayerIntMapSetter.set(
                player,
                TrawlerBits.points,
                (points(player) + amount).coerceAtMost(255),
            )
    }

    public fun patch(player: Player, index: Int): Boolean {
        if (!playing(player) || index !in leaks.indices || !leaks[index]) return false
        leaks[index] = false
        addPoints(player, 5)
        update()
        return true
    }

    public fun bail(player: Player): Boolean {
        if (!playing(player) || water == 0) return false
        water = (water - 5).coerceAtLeast(0)
        addPoints(player, 2)
        update()
        return true
    }

    public fun mend(player: Player): Boolean {
        if (!playing(player) || !torn) return false
        torn = false
        addPoints(player, 7)
        update()
        return true
    }

    public fun repair(player: Player): Boolean {
        if (!playing(player) || !railBroken) return false
        railBroken = false
        addPoints(player, 10)
        update()
        return true
    }

    public fun chop(player: Player): Boolean {
        if (!playing(player) || tentacle == null) return false
        addPoints(player, 10)
        chops++
        if (chops >= 3) {
            npcs.del(tentacle!!, Int.MAX_VALUE)
            tentacle = null
            railBroken = true
        }
        update()
        return true
    }

    private fun update() {
        for (p in players.filterValues { it == serial }.keys) {
            fun set(bit: org.rsmod.game.type.varbit.VarBitType, n: Int) =
                VarPlayerIntMapSetter.set(p, bit, n)
            set(TrawlerBits.status, 1)
            set(TrawlerBits.water, water)
            set(TrawlerBits.net, if (torn) 1 else 0)
            set(TrawlerBits.time, ((500 - elapsed + 49) / 50).coerceIn(0, 15))
            set(TrawlerBits.catch, healthyTicks / 50)
            for (i in leaks.indices) set(TrawlerBits.hulls[i], if (leaks[i]) 2 else 0)
            set(TrawlerBits.rail, if (railBroken) 2 else 0)
        }
    }

    public fun finish(success: Boolean) {
        if (!active) return
        for (p in players.filterValues { it == serial }.keys.toList()) {
            val contribution = points(p)
            if (success) {
                if (contribution >= 50 && p.statBase(stats.fishing) >= 15)
                    loot.generate(p, contribution, participants, healthyTicks)
                move(p, CoordGrid(2676, 3170))
                p.mes(
                    if (contribution >= 50)
                        "The trawler returns to Port Khazard. Inspect the net for your catch."
                    else "You did not contribute enough to receive a catch."
                )
            } else {
                move(p, CoordGrid(1953, 4826))
                p.mes("The trawler has sunk! Climb onto a barrel to return to shore.")
            }
            leave(p)
        }
        tentacle?.let { npcs.del(it, Int.MAX_VALUE) }
        tentacle = null
        active = false
        countdown = if (players.isEmpty()) 0 else 100
    }

    private fun move(player: Player, dest: CoordGrid) {
        player.cancelActiveCoroutine()
        org.rsmod.game.entity.util.PathingEntityCommon.telejump(player, collision, dest)
    }

    public companion object {
        public fun atSea(c: CoordGrid): Boolean =
            c.x in 2007..2028 && c.z in 4818..4830 && c.level in 0..1

        public fun lobby(c: CoordGrid): Boolean =
            c.x in 2669..2673 && c.z in 3166..3182 && c.level == 0

        public fun wreck(c: CoordGrid): Boolean =
            c.x in 1943..1964 && c.z in 4818..4833 && c.level == 0
    }
}
