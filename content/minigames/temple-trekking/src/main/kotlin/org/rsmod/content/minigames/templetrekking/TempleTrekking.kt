package org.rsmod.content.minigames.templetrekking

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private val hubs = listOf(CoordGrid(3480, 3237), CoordGrid(3436, 3486))
private val food =
    mapOf(
        3369 to 6,
        3371 to 7,
        3373 to 8,
        329 to 9,
        2003 to 11,
        3381 to 8,
        2142 to 3,
        361 to 10,
        7530 to 11,
        379 to 12,
        365 to 13,
        2325 to 10,
        2323 to 14,
        7178 to 12,
        7188 to 12,
        1891 to 12,
        1897 to 15,
        5988 to 10,
        6703 to 14,
    )
private val timber =
    listOf(960, 1511, 1521, 1519, 1517, 1515, 1513, 6333, 6332, 19669, 2862, 3239, 10810)
private var Player.trekOrigin by intVarp(TrekVars.origin)

private enum class Encounter {
    Beasts,
    Snakes,
    Snails,
    Bridge,
    Crank,
}

private class Trek(
    val player: Player,
    val escort: Int,
    val route: Int,
    val events: List<Encounter>,
    val region: Region,
) {
    val direction = escort / 6
    val number = 5 - escort % 6 / 2
    var index = 0
    val encounter
        get() = events[index]

    var turns = 0
    var points = 0
    var repaired = 0
    var spared = true
    var outfitDropped = false
    var helper: Npc? = null
    lateinit var follower: Npc
    val monsters = linkedSetOf<Npc>()
    val engaged = mutableSetOf<Npc>()
    val remains = mutableSetOf<Npc>()

    fun tile(x: Int, z: Int, level: Int = 0) = region.normal[level, x / 64, z / 64, x % 64, z % 64]
}

public class TempleTrekking
@Inject
constructor(
    private val regions: RegionRepository,
    private val npcs: NpcRepository,
    private val locs: LocRepository,
    private val ground: ObjRepository,
    private val types: NpcTypeList,
    private val items: ObjTypeList,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val interactions: AiPlayerInteractions,
    private val modifier: NpcHitModifier,
) : PluginScript() {
    private val selected = mutableMapOf<Player, Pair<Int, Npc>>()
    private val trips = mutableMapOf<Player, Trek>()
    private val attackers = mutableMapOf<Npc, Trek>()
    private val provisions = mutableMapOf<Player, ArrayDeque<Int>>()

    override fun ScriptContext.startup() {
        for ((index, npc) in TrekNpcs.escorts.withIndex()) {
            onOpNpc1(npc) { choose(index, it.npc) }
            onOpNpc2(npc) { choose(index, it.npc) }
            onOpNpc3(npc) { choose(index, it.npc) }
            onOpNpc4(npc) { supply(index, it.npc) }
            for (id in food.keys) onOpNpcU(npc, items.getValue(id)) { feed(index, it.npc, id) }
            onEvent<NpcHitEvents.ResolveDamage>(npc.id) { if (hit.isFromPlayer) damage = 0 }
        }
        for ((route, button) in TrekButtons.routes.withIndex()) onIfModalButton(button) {
            val (escort, npc) = selected.remove(player) ?: return@onIfModalButton
            ifClose()
            if (canEscort(escort) && isWithinDistance(npc, 3)) start(escort, route)
        }
        for (path in TrekScenery.escape) onOpLoc1(path) {
            val trip = trips[player] ?: return@onOpLoc1
            end(trip, false, true)
        }
        for (path in TrekScenery.evade) onOpLoc1(path) {
            val trip = trips[player] ?: return@onOpLoc1
            if (
                trip.encounter in listOf(Encounter.Bridge, Encounter.Crank) ||
                    trip.route == 2 ||
                    (trip.route == 1 && 2 * trip.monsters.size > trip.number)
            ) {
                mes("You cannot evade this encounter yet.")
                return@onOpLoc1
            }
            next(trip)
        }
        for (path in TrekScenery.success) onOpLoc1(path) {
            val trip = trips[player] ?: return@onOpLoc1
            if (
                trip.encounter == Encounter.Bridge && trip.repaired < 3 ||
                    trip.encounter != Encounter.Bridge && trip.monsters.isNotEmpty()
            ) {
                mes("You must deal with this encounter before continuing.")
                return@onOpLoc1
            }
            next(trip)
        }
        for (bridge in TrekScenery.bridge) {
            onOpLoc1(bridge) { repair(it.loc) }
            for (id in timber) onOpLocU(bridge, items.getValue(id)) { repair(it.loc, id) }
        }
        for (corpse in TrekNpcs.carcasses) onOpNpcU(corpse, objs.knife) {
            val trip = trips[player] ?: return@onOpNpcU
            if (it.npc !in trip.remains) return@onOpNpcU
            val n = random.of(2, 5)
            if (invAdd(inv, TrekItems.hide, n).success) {
                trip.remains.remove(it.npc)
                despawn(it.npc)
            }
        }
        onOpNpc1(TrekNpcs.crank) {
            val trip = trips[player] ?: return@onOpNpc1
            if (trip.helper != it.npc || !trip.spared) return@onOpNpc1
            trip.spared = false
            statBoost(stats.hitpoints, 15, 0)
            statBoost(stats.defence, 3, 10)
            trip.follower.hitpoints = trip.follower.baseHitpointsLvl
            val rotten = items.getValue(2959)
            repeat(invTotal(inv, rotten)) { invReplace(inv, rotten, 1, items.getValue(2003)) }
            mes("Abidor Crank heals you and your companion.")
        }
        for (type in TrekNpcs.beasts + TrekNpcs.snakes + TrekNpcs.snails + TrekNpcs.lumber) onEvent<
            NpcHitEvents.Impact
        >(
            type.id
        ) {
            val trip = attackers[npc] ?: return@onEvent
            if (hit.isFromPlayer && npc.hitpoints > 0) {
                trip.engaged.add(npc)
                npc.opPlayer2(trip.player, interactions)
            }
        }
        onEvent<NpcKillEvents.Credited> {
            val trip = attackers[npc] ?: return@onEvent
            if (player == trip.player) defeated(trip, npc)
        }
        onPlayerSoftTimer(TrekTimers.tick) { trips[player]?.let(::tick) }
        onEvent<PlayerTeleportEvent.Start> { trips[player]?.let { end(it, false, false) } }
        onEvent<PlayerDeathEvent.ResolveRespawn> { trips[player]?.let { end(it, false, false) } }
        onPlayerLogout {
            trips[player]?.let { end(it, false, true) }
            selected.remove(player)
            provisions.remove(player)
        }
        onPlayerLogin {
            if (player.trekOrigin in 1..2) {
                move(player, hubs[player.trekOrigin - 1])
                player.trekOrigin = 0
            }
        }
    }

    private fun ProtectedAccess.canEscort(index: Int): Boolean {
        if (
            !player.members ||
                vars[TrekQuest.aid] < 430 ||
                index >= 6 && vars[TrekQuest.darkness] < 320
        ) {
            mes(
                if (index < 6) "You must complete In Aid of the Myreque first."
                else "You must complete Darkness of Hallowvale first."
            )
            return false
        }
        if (vars[TrekQuest.pet] != 0) {
            mes("You cannot take your pet on a trek.")
            return false
        }
        return player !in trips
    }

    private fun ProtectedAccess.choose(index: Int, npc: Npc) {
        if (!canEscort(index)) return
        selected[player] = index to npc
        ifOpenMainModal(TrekUi.routes)
    }

    private suspend fun ProtectedAccess.supply(index: Int, npc: Npc) {
        val options = inv.filterNotNull().map { it.id }.distinct().filter { it in food }
        if (options.isEmpty()) {
            mes("You have no food suitable for your companion.")
            return
        }
        val i = menu("Give your companion some food", true, options.map { items.getValue(it).name })
        options.getOrNull(i)?.let { feed(index, npc, it) }
    }

    private fun ProtectedAccess.feed(index: Int, npc: Npc, id: Int) {
        val trip = trips[player]
        if (trip == null && !canEscort(index) || trip != null && trip.follower != npc) return
        val pack = provisions.getOrPut(player) { ArrayDeque() }
        if (trip != null && npc.hitpoints < npc.baseHitpointsLvl) {
            if (invDel(inv, items.getValue(id)).success)
                npc.hitpoints = (npc.hitpoints + 10).coerceAtMost(npc.baseHitpointsLvl)
        } else if (trip == null && pack.size < 15 && invDel(inv, items.getValue(id)).success)
            pack.addLast(food.getValue(id))
    }

    private fun ProtectedAccess.start(index: Int, route: Int) {
        val region =
            regions.add(
                RegionTemplate.create {
                    copy(248, 628, 0) {
                        zoneWidth = 4
                        zoneLength = 4
                        regionZoneX = 2
                        regionZoneZ = 4
                    }
                    for (level in 0..1) copy(260, 629, level) {
                        zoneWidth = 4
                        zoneLength = 3
                        regionZoneX = 8
                        regionZoneZ = 4
                    }
                }
            ) ?: return
        val max = listOf(9, 4, 5)[route]
        val count = if (random.of(4) == 0) random.of(0, max) else 3
        val encounters =
            List(count) {
                when (random.of(12)) {
                    0 -> Encounter.Crank
                    in 1..4 -> Encounter.Bridge
                    in 5..7 -> Encounter.Beasts
                    in 8..9 -> Encounter.Snakes
                    else -> Encounter.Snails
                }
            }
        val trip = Trek(player, index, route, encounters, region)
        trips[player] = trip
        player.trekOrigin = trip.direction + 1
        trip.follower = Npc(types[TrekNpcs.escorts[index]], trip.tile(1999, 5030))
        npcs.add(trip.follower, 36000)
        player.softTimer(TrekTimers.tick, 1)
        if (count == 0) end(trip, true, true) else stage(trip)
    }

    private fun stage(trip: Trek) {
        trip.turns = 0
        trip.spared = true
        trip.repaired = 0
        trip.outfitDropped = false
        val bridge = trip.encounter == Encounter.Bridge
        if (bridge) {
            for (level in 0..1) {
                val bridgeTile = trip.tile(2090, 5039, level)
                val fixed =
                    locs.findAll(bridgeTile).firstOrNull { loc ->
                        TrekScenery.bridge.drop(1).any { it.id == loc.id }
                    }
                if (fixed != null) locs.change(fixed, TrekScenery.bridge.first(), Int.MAX_VALUE)
            }
        }
        val start = if (bridge) trip.tile(2082, 5041) else trip.tile(1999, 5030)
        move(trip.player, start)
        trip.follower.teleport(collision, start.translate(1, 0))
        if (trip.encounter == Encounter.Crank) {
            trip.helper =
                Npc(types[TrekNpcs.crank], start.translate(2, 2)).also { npcs.add(it, 36000) }
            return
        }
        val group =
            when (trip.encounter) {
                Encounter.Beasts -> TrekNpcs.beasts
                Encounter.Snakes -> TrekNpcs.snakes
                Encounter.Snails -> TrekNpcs.snails
                else -> TrekNpcs.lumber
            }
        if (bridge) {
            spawnLumberjack(trip, 0)
            return
        }
        repeat(trip.number) { i ->
            val tile = start.translate((i % 3 - 1) * 4, 4 + (i / 3) * 4)
            val foe = Npc(types[group[trip.route]], tile)
            npcs.add(foe, 36000)
            attackers[foe] = trip
            trip.monsters.add(foe)
        }
    }

    private fun spawnLumberjack(trip: Trek, index: Int) {
        val foe = Npc(types[TrekNpcs.lumber[trip.route]], trip.tile(2084 + index, 5042))
        npcs.add(foe, 36000)
        attackers[foe] = trip
        trip.monsters.add(foe)
    }

    private fun tick(trip: Trek) {
        trip.turns++
        if (trip.encounter == Encounter.Bridge && trip.repaired < 3) {
            val phase = trip.turns % 54
            if (phase < 5 && trip.monsters.size < 5) spawnLumberjack(trip, phase)
        }
        val companion = trip.follower
        if (companion.hitpoints <= 0) {
            trip.player.mes("Your companion did not survive the trek.")
            end(trip, false, true)
            return
        }
        val stock = provisions[trip.player]
        if (companion.hitpoints <= companion.baseHitpointsLvl / 2 && !stock.isNullOrEmpty())
            companion.hitpoints =
                (companion.hitpoints + stock.removeFirst()).coerceAtMost(companion.baseHitpointsLvl)
        if (trip.turns % 4 != 0) return
        if (trip.monsters.isEmpty() || trip.turns < 8) {
            if (distance(companion.coords, trip.player.coords) > 2)
                companion.walk(trip.player.coords)
            return
        }
        for ((i, foe) in trip.monsters.toList().withIndex()) {
            if (foe.hitpoints <= 0) {
                defeated(trip, foe)
                continue
            }
            if (i == 0 || foe in trip.engaged) {
                foe.opPlayer2(trip.player, interactions)
                continue
            }
            val reach = if (trip.encounter == Encounter.Snails) 8 else foe.size + 1
            if (distance(foe.coords, companion.coords) > reach) {
                foe.walk(companion.coords)
                continue
            }
            val max =
                when (trip.encounter) {
                    Encounter.Snakes -> 6 * (trip.route + 1)
                    Encounter.Snails -> listOf(8, 13, 17)[trip.route]
                    Encounter.Beasts -> 2 + trip.route
                    else -> 2 + trip.route
                }
            companion.queueHit(
                foe,
                1,
                if (trip.encounter == Encounter.Snails) HitType.Ranged else HitType.Melee,
                random.of(0, max),
                modifier,
            )
            if (i == 1)
                foe.queueHit(
                    companion,
                    1,
                    HitType.Melee,
                    random.of(0, 1 + trip.escort % 6 / 2 * 3),
                    modifier,
                )
        }
    }

    private fun defeated(trip: Trek, npc: Npc) {
        if (!trip.monsters.remove(npc)) return
        attackers.remove(npc)
        trip.engaged.remove(npc)
        if (trip.encounter != Encounter.Bridge) trip.points += listOf(325, 625, 1125)[trip.route]
        fun drop(id: Int, count: Int = 1) {
            ground.add(
                items.getValue(id),
                npc.coords,
                constants.lootdrop_duration,
                trip.player,
                count,
            )
        }
        when (trip.encounter) {
            Encounter.Beasts -> {
                drop(532)
                drop(TrekItems.nails.id, random.of(2, 3))
            }
            Encounter.Snakes -> {
                val dead = Npc(types[TrekNpcs.carcasses[trip.route]], npc.coords)
                npcs.add(dead, 36000)
                trip.remains.add(dead)
            }
            Encounter.Snails ->
                drop(if (random.of(50) == 0) TrekItems.perfect.id else TrekItems.shell.id)
            Encounter.Bridge -> {
                drop(526)
                drop(960)
                if (!trip.outfitDropped && random.of(4) == 0) {
                    val reward =
                        listOf(10939, 10940, 10941, 10933).firstOrNull { id ->
                            trip.player.inv.none { it?.id == id } &&
                                trip.player.worn.none { it?.id == id } &&
                                trip.player.invMap[invs.bank]?.any { it?.id == id } != true
                        }
                    if (reward != null) {
                        drop(reward)
                        trip.outfitDropped = true
                    }
                }
            }
            Encounter.Crank -> Unit
        }
    }

    private fun ProtectedAccess.repair(loc: BoundLocInfo, provided: Int? = null) {
        val trip = trips[player] ?: return
        if (trip.encounter != Encounter.Bridge) return
        if (trip.repaired == 3) {
            move(player, trip.tile(2095, 5040))
            return
        }
        if (
            objs.hammer !in inv &&
                player.righthand?.let { items.getValue(it.id).name.endsWith(" axe", true) } != true
        ) {
            mes("You need a hammer or a wielded axe.")
            return
        }
        val id =
            provided
                ?: inv.filterNotNull().firstOrNull { it.id in timber }?.id
                ?: run {
                    mes("Use three logs or planks to repair the bridge.")
                    return
                }
        if (invDel(inv, items.getValue(id)).failure) return
        trip.repaired++
        locs.change(loc, TrekScenery.bridge[trip.repaired], Int.MAX_VALUE)
        if (trip.repaired == 3) {
            mes("You repair the bridge and cross with your companion.")
            move(player, trip.tile(2095, 5040))
            trip.follower.teleport(collision, player.coords.translate(1, 0))
        }
    }

    private fun next(trip: Trek) {
        cleanup(trip)
        trip.index++
        if (trip.index >= trip.events.size) end(trip, true, true) else stage(trip)
    }

    private fun cleanup(trip: Trek) {
        for (n in trip.monsters) {
            attackers.remove(n)
            despawn(n)
        }
        for (n in trip.remains) despawn(n)
        trip.helper?.let(::despawn)
        trip.helper = null
        trip.monsters.clear()
        trip.remains.clear()
        trip.engaged.clear()
    }

    private fun end(trip: Trek, reward: Boolean, returnToWorld: Boolean) {
        val p = trip.player
        if (trips.remove(p) == null) return
        cleanup(trip)
        despawn(trip.follower)
        p.trekOrigin = 0
        p.clearSoftTimer(TrekTimers.tick)
        if (returnToWorld) move(p, if (reward) hubs[1 - trip.direction] else CoordGrid(3488, 3269))
        if (reward) {
            val token = TrekItems.tokens[trip.direction][trip.route]
            val points = trip.points.coerceIn(325, 8000)
            val received = p.invAdd(trip.player.inv, token, 1, vars = points)
            if (received.failure)
                ground.add(
                    org.rsmod.game.obj.Obj.fromOwner(
                        p,
                        p.coords,
                        org.rsmod.game.inv.InvObj(token, 1, points),
                    ),
                    constants.lootdrop_duration,
                )
            if (trip.escort % 6 < 2) p.completeDiaryTask(DiaryTask.EscortHardTrekCompanion)
            p.mes("You and your companion arrive safely. You receive a reward token.")
        }
    }

    private fun despawn(n: Npc) {
        if (n.slotId != INVALID_SLOT) npcs.del(n, Int.MAX_VALUE)
    }

    private fun move(p: Player, c: CoordGrid) = PathingEntityCommon.telejump(p, collision, c)

    private fun distance(a: CoordGrid, b: CoordGrid) = maxOf(abs(a.x - b.x), abs(a.z - b.z))
}
