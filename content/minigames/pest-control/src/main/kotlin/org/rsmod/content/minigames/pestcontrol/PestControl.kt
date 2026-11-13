package org.rsmod.content.minigames.pestcontrol

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerCombatPermission
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statRestoreAll
import org.rsmod.api.player.ui.*
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.*
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal var Player.commendations by intVarp(PestVars.points)
private var Player.returnBoat by intVarp(PestVars.returnBoat)
private var Player.activity by intVarBit(PestBits.activity)
private var Player.specialEnergy by intVarp(varps.sa_energy)

private data class Participation(var activity: Int = 30, var damage: Int = 0)

private class Game(
    val lander: Lander,
    val region: Region,
    val started: Int,
    val order: List<Int>,
    val knight: Npc,
) {
    val team = linkedMapOf<Player, Participation>()
    val portals = mutableMapOf<Int, Npc>()
    val pests = mutableMapOf<Npc, Pair<Int, Int>>()
    var shields = 0
    var nextPest = 0

    fun tile(x: Int, z: Int, h: Int = 0): CoordGrid = region.normal[h, 41, 40, x - 2624, z - 2560]
}

public class PestControl
@Inject
constructor(
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val interactions: AiPlayerInteractions,
    private val ground: ObjRepository,
    private val random: GameRandom,
    private val players: PlayerList,
    private val stats: StatTypeList,
) : PluginScript() {
    private val waiting = Lander.entries.associateWith { linkedSetOf<Player>() }
    private val departure = mutableMapOf<Lander, Int>()
    private val playing = mutableMapOf<Player, Game>()
    private val owners = mutableMapOf<Npc, Game>()
    private var lastTick = -1

    override fun ScriptContext.startup() {
        for (boat in Lander.entries) {
            onOpLoc1(PestLocs.gangplanks[boat.ordinal]) { board(player, boat) }
            onOpLoc1(PestLocs.ladders[boat.ordinal]) { leave(player, true) }
        }
        onOpNpc1(PestNpcs.port) { telejump(CoordGrid(2658, 2676)) }
        onOpNpc3(PestNpcs.port) { telejump(CoordGrid(2658, 2676)) }
        onOpNpc1(PestNpcs.outpost) { telejump(CoordGrid(3041, 3202)) }
        onOpNpc3(PestNpcs.outpost) { telejump(CoordGrid(3041, 3202)) }
        for (loc in PestLocs.gates) onOpLoc1(loc) {
            val game = playing[player] ?: return@onOpLoc1
            val gate = it.loc.coords
            val middle = game.tile(2656, 2592)
            val destination =
                when {
                    gate.x < middle.x - 6 || gate.x > middle.x + 6 ->
                        CoordGrid(
                            if (player.coords.x < gate.x) gate.x + 1 else gate.x - 1,
                            gate.z,
                            gate.level,
                        )
                    else ->
                        CoordGrid(
                            gate.x,
                            if (player.coords.z < gate.z) gate.z + 1 else gate.z - 1,
                            gate.level,
                        )
                }
            telejump(destination)
        }
        onOpLoc1(PestLocs.tower) {
            if (player in playing) telejump(it.loc.coords.translate(0, 0, levelOffset = 1))
        }
        onOpLoc1(PestLocs.towerTop) {
            if (player in playing) telejump(it.loc.coords.translate(0, 0, levelOffset = -1))
        }
        val allTargets =
            (PestNpcs.portal.values.flatten() +
                    PestNpcs.pests.flatten() +
                    PestNpcs.shield.values.flatten() +
                    PestNpcs.defenders)
                .distinct()
        for (type in allTargets) {
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(2)
                if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
            }
            onEvent<NpcHitEvents.ResolveDamage>(type.id) {
                val game = owners[npc] ?: return@onEvent
                val source = if (hit.isFromPlayer) hit.resolvePlayerSource(players) else null
                val slot = game.portals.entries.firstOrNull { it.value === npc }?.key
                if (
                    (source != null && source !in game.team) ||
                        npc === game.knight ||
                        (slot != null && slot !in game.order.take(game.shields))
                )
                    damage = 0
            }
            onNpcHit(type) {
                if (!hit.isFromPlayer || hit.damage <= 0) return@onNpcHit
                val player = hit.resolvePlayerSource(players) ?: return@onNpcHit
                val game = owners[npc] ?: return@onNpcHit
                val score = game.team[player] ?: return@onNpcHit
                score.damage += hit.damage
                score.activity = (score.activity + hit.damage).coerceAtMost(50)
                player.activity = score.activity
            }
        }
        onEvent<NpcKillEvents.Credited> {
            val game = owners[npc] ?: return@onEvent
            val slot = game.portals.entries.firstOrNull { it.value === npc }?.key
            if (slot != null && player in game.team && slot in game.order.take(game.shields)) {
                game.portals.remove(slot)
                game.knight.hitpoints = (game.knight.hitpoints + 50).coerceAtMost(200)
                if (game.portals.isEmpty()) finish(game, true)
            } else game.pests.remove(npc)
            owners.remove(npc)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            playing[player]?.let { safeDestination = it.tile(2657, 2610) }
        }
        onEvent<PlayerCombatPermission> {
            if (attacker in playing || target in playing) allowed = false
        }
        onEvent<PlayerTeleportEvent.Start> { if (player.returnBoat != 0) leave(player, false) }
        onPlayerLogout { if (player.returnBoat != 0) leave(player, true) }
        onPlayerLogin {
            val boat = Lander.entries.getOrNull(player.returnBoat - 1)
            if (boat != null) {
                move(player, boat.dock)
                player.returnBoat = 0
                player.activity = 0
            }
        }
        onPlayerSoftTimer(PestTimers.game) {
            if (lastTick == clock.cycle) return@onPlayerSoftTimer
            lastTick = clock.cycle
            for (boat in Lander.entries) if (departure[boat]?.let { it <= clock.cycle } == true)
                start(boat)
            playing.values.toSet().forEach(::tick)
        }
    }

    private fun board(player: Player, boat: Lander) {
        if (player.returnBoat != 0) {
            if (player in waiting.getValue(boat)) leave(player, true)
            return
        }
        if (!player.members || player.combatLevel < boat.requirement) {
            player.mes("You need a combat level of ${boat.requirement} to board this lander.")
            return
        }
        if (player.commendations >= 4000) {
            player.mes("Spend some commendation points before boarding.")
            return
        }
        waiting.getValue(boat).add(player)
        player.returnBoat = boat.ordinal + 1
        move(player, boat.deck)
        player.softTimer(PestTimers.game, 1)
        player.ifOpenOverlay(PestHud.boat, events)
        schedule(boat)
    }

    private fun schedule(boat: Lander) {
        val queue = waiting.getValue(boat)
        when {
            queue.size >= 25 -> departure[boat] = clock + 1
            queue.size >= 5 -> departure.putIfAbsent(boat, clock + 200)
            else -> departure.remove(boat)
        }
        for (player in queue) {
            player.ifSetText(PestText.waiting, "Players ready: ${queue.size} (min. 5)")
            player.ifSetText(PestText.level, "Combat level: ${boat.requirement}+")
            player.ifSetText(PestText.points, "Points: ${player.commendations}")
            player.ifSetText(
                PestText.boatTime,
                if (boat in departure) "Next departure: within 2 minutes" else "Waiting for players",
            )
        }
    }

    private fun start(boat: Lander) {
        val template =
            RegionTemplate.create {
                for (h in 0..1) copy(328, 320, h) {
                    zoneWidth = 8
                    zoneLength = 8
                    regionZoneX = 4
                    regionZoneZ = 4
                }
            }
        val region = regions.add(template)
        if (region == null) {
            departure[boat] = clock + 10
            return
        }
        val knight = spawn(PestNpcs.defenders[random.of(4)], region.normal[0, 41, 40, 32, 32], 200)
        val game = Game(boat, region, clock.cycle, ORDERS[random.of(ORDERS.size)], knight)
        owners[knight] = game
        for ((i, c) in PORTALS.withIndex()) {
            val portal =
                spawn(
                    PestNpcs.shield.getValue(boat)[i],
                    game.tile(c.x, c.z),
                    if (boat == Lander.Novice) 200 else 250,
                )
            game.portals[i] = portal
            owners[portal] = game
        }
        val queue = waiting.getValue(boat)
        for (player in queue.take(25)) {
            queue.remove(player)
            game.team[player] = Participation()
            playing[player] = game
            player.ifCloseOverlay(PestHud.boat, events)
            player.ifOpenOverlay(PestHud.game, events)
            player.activity = 30
            move(player, game.tile(2657, 2610))
            player.mes("Protect the Void Knight and destroy the four portals!")
        }
        departure.remove(boat)
        schedule(boat)
        hud(game)
    }

    private fun spawn(type: NpcType, coords: CoordGrid, hp: Int): Npc {
        val npc = Npc(types[type], coords)
        npcs.add(npc, 2200)
        npc.hitpoints = hp
        return npc
    }

    private fun tick(game: Game) {
        val elapsed = clock.cycle - game.started
        if (elapsed >= 2000) {
            finish(game, true)
            return
        }
        if (game.shields < 4 && elapsed >= 25 + game.shields * 50) {
            val slot = game.order[game.shields++]
            val portal = game.portals.getValue(slot)
            portal.transmog(types[PestNpcs.portal.getValue(game.lander)[slot]], Int.MAX_VALUE)
            game.team.keys.forEach { it.mes("The ${COLOURS[slot]} portal shield has dropped!") }
        }
        if (elapsed % 12 == 0 && game.pests.size < 40) {
            val slot = game.portals.keys.toList()[random.of(game.portals.size)]
            val portal = game.portals.getValue(slot)
            val kind = game.nextPest++ % 7
            val tier = (game.lander.ordinal + random.of(3)).coerceAtMost(4)
            val type = PestNpcs.pests[tier][kind]
            val npc =
                spawn(type, portal.coords.translate(2, 1), types[type].hitpoints.coerceAtLeast(10))
            game.pests[npc] = slot to kind
            owners[npc] = game
            when (kind) {
                0,
                1,
                2 -> npc.walk(game.knight.coords)
                3 -> Unit
                else ->
                    npc.opPlayer2(game.team.keys.elementAt(random.of(game.team.size)), interactions)
            }
        }
        if (elapsed % 5 == 0) {
            for ((npc, born) in game.pests) {
                if (npc.hitpoints <= 0 || npc.slotId == INVALID_SLOT) continue
                val (slot, kind) = born
                when (kind) {
                    0,
                    1,
                    2 -> {
                        if (npc.isWithinDistance(game.knight, if (kind == 0) 1 else 10)) {
                            game.knight.hitpoints =
                                (game.knight.hitpoints - random.of(3 + game.lander.ordinal))
                                    .coerceAtLeast(0)
                        } else if (kind == 0 && elapsed % 55 == 0) {
                            PathingEntityCommon.telejump(
                                npc,
                                collision,
                                game.knight.coords.translate(1, 0),
                            )
                        } else npc.walk(game.knight.coords)
                    }
                    3 ->
                        game.portals[slot]?.let { portal ->
                            if (npc.isWithinDistance(portal, 5))
                                portal.hitpoints =
                                    (portal.hitpoints + random.of(1..5)).coerceAtMost(
                                        if (game.lander == Lander.Novice) 200 else 250
                                    )
                        }
                }
            }
            if (game.knight.hitpoints == 0) {
                finish(game, false)
                return
            }
            if (elapsed > 0)
                game.team.forEach { (player, score) ->
                    score.activity = (score.activity - 1).coerceAtLeast(0)
                    player.activity = score.activity
                }
            hud(game)
        }
    }

    private fun hud(game: Game) {
        val seconds = (2000 - clock.cycle + game.started) * 3 / 5
        for ((player, score) in game.team) {
            player.ifSetText(
                PestText.time,
                "Time remaining: ${seconds/60}:${(seconds%60).toString().padStart(2,'0')}",
            )
            player.ifSetText(PestText.health, game.knight.hitpoints.toString())
            player.ifSetText(PestText.damage, score.damage.toString())
            for (i in 0..3) {
                player.ifSetText(PestText.portal[i], (game.portals[i]?.hitpoints ?: 0).toString())
                player.ifSetHide(PestText.shield[i], i in game.order.take(game.shields))
            }
        }
    }

    private fun finish(game: Game, victory: Boolean) {
        for ((player, score) in game.team.toMap()) {
            if (victory && score.activity > 0 && score.damage > 0) {
                val bonus = PestBits.ca.indexOfLast { player.vars[it] >= 2 } + 1
                player.commendations =
                    (player.commendations + game.lander.score + bonus).coerceAtMost(4000)
                player.invAddOrDrop(
                    ground,
                    objs.coins,
                    player.combatLevel * 10,
                    coords = game.lander.dock,
                )
                player.completeDiaryTask(game.lander.diary)
                player.mes("You have successfully defended the island!")
            } else
                player.mes(
                    if (victory) "You did not participate enough to receive a commendation."
                    else "Alas, the Void Knight has died."
                )
            player.statRestoreAll(stats.values)
            player.specialEnergy = 1000
            player.runEnergy = 10000
            leave(player, true)
        }
    }

    private fun leave(player: Player, returnToDock: Boolean) {
        val boat = Lander.entries.getOrNull(player.returnBoat - 1) ?: return
        val game = playing.remove(player)
        if (game != null) {
            game.team.remove(player)
            player.ifCloseOverlay(PestHud.game, events)
            if (game.team.isEmpty()) {
                val alive = owners.filterValues { it === game }.keys.toList()
                for (npc in alive) {
                    owners.remove(npc)
                    if (npc.slotId != INVALID_SLOT) npcs.del(npc, Int.MAX_VALUE)
                }
            }
        } else {
            waiting.getValue(boat).remove(player)
            player.ifCloseOverlay(PestHud.boat, events)
            schedule(boat)
        }
        player.returnBoat = 0
        player.activity = 0
        player.clearSoftTimer(PestTimers.game)
        if (returnToDock) move(player, boat.dock)
    }

    private fun move(player: Player, tile: CoordGrid) =
        PathingEntityCommon.telejump(player, collision, tile)

    private companion object {
        val PORTALS =
            listOf(
                CoordGrid(2628, 2591),
                CoordGrid(2680, 2588),
                CoordGrid(2669, 2570),
                CoordGrid(2645, 2569),
            )
        val COLOURS = listOf("purple", "blue", "yellow", "red")
        val ORDERS =
            listOf(
                listOf(1, 3, 2, 0),
                listOf(1, 0, 3, 2),
                listOf(0, 1, 2, 3),
                listOf(0, 2, 1, 3),
                listOf(2, 3, 0, 1),
                listOf(2, 0, 3, 1),
            )
    }
}
