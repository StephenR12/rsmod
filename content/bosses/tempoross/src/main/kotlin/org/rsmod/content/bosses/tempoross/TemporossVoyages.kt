package org.rsmod.content.bosses.tempoross

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

internal class Crew(val region: Region, team: List<Player>) {
    val points = team.associateWith { 0 }.toMutableMap()
    val maxEnergy = team.size * (260 - (team.size - 1) * 80 / 39)
    val maxEssence = team.size * (250 - (team.size - 1) * 80 / 39)
    var energy = maxEnergy
    var essence = maxEssence
    var elapsed = 0
    var storm = 0
    var submerged = 0
    var phases = 0
    var returnIn = -1
    var waveIn = 0
    val residents = mutableListOf<Npc>()
    val pools = mutableListOf<Npc>()
    val fish = mutableListOf<Npc>()
    val captains = mutableListOf<Npc>()
    val fires = mutableMapOf<CoordGrid, Npc>()
    val firesSeen = mutableSetOf<CoordGrid>()
    val tethered = mutableMapOf<Player, CoordGrid>()
    val broken = mutableSetOf<Int>()
    val cannons = List(4) { ArrayDeque<Int>() }
    val disabled = IntArray(4)
    val cannonFill = IntArray(4)
    val loaders = mutableMapOf<Player, Int>()
    lateinit var spirit: Npc

    fun tile(x: Int, z: Int) = region.normal[0, 47, 46, x - 3008, z - 2944]

    fun add(player: Player, score: Int) {
        val old = points[player] ?: return
        points[player] = old + score
        set(player, SeaBits.points, old + score)
    }
}

@Singleton
internal class TemporossVoyages
@Inject
constructor(
    private val regions: RegionRepository,
    private val controllers: ControllerRepository,
    private val npcs: NpcRepository,
    private val types: NpcTypeList,
    private val locs: LocRepository,
    private val locTypes: LocTypeList,
    private val random: GameRandom,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
) {
    private val waiting = linkedSetOf<Player>()
    private val voyages = mutableMapOf<Player, Crew>()
    var subdued = 0
        private set

    var fastest = 0
        private set

    private var timer = false
    private var countdown = 0

    fun trip(p: Player) = voyages[p]

    fun board(p: Player, solo: Boolean) {
        if (!p.members || p.statBase(stats.fishing) < 35) {
            p.mes("You need level 35 Fishing to join the Spirit Anglers.")
            return
        }
        if (p in voyages) return
        if (!timer) {
            val con = Controller(SeaSpirit.lobby, pier)
            controllers.add(con, Int.MAX_VALUE)
            con.aiTimer(1)
            timer = true
        }
        if (solo) {
            launch(listOf(p))
        } else if (p in waiting) {
            leave(p, true)
        } else {
            if (waiting.isEmpty()) countdown = 100
            waiting += p
            set(p, SeaVars.trip, 1)
            move(p, lobbyDeck)
            p.mes("You board the Spirit Anglers' ship.")
            if (waiting.size >= 40) launch(waiting.toList())
        }
    }

    private fun launch(team: List<Player>) {
        val region =
            regions.add(
                RegionTemplate.create {
                    for (level in 0..2) copy(376, 368, level) {
                        zoneWidth = 8
                        zoneLength = 8
                        regionZoneX = 4
                        regionZoneZ = 4
                    }
                }
            )
        if (region == null) {
            team.forEach { it.mes("The ship is waiting for a cove to become available.") }
            countdown = 10
            return
        }
        region.reclaimFromCycle = clock.cycle + 6
        val game = Crew(region, team)
        for ((i, p) in team.withIndex()) {
            waiting.remove(p)
            voyages[p] = game
            set(p, SeaVars.trip, 2)
            set(p, SeaBits.tethered, 0)
            set(p, SeaBits.points, 0)
            move(p, game.tile(if (i % 2 == 0) 3035 else 3059, 2977))
            p.ifOpenOverlay(SeaUi.hud, events)
        }
        fun spawn(id: Int, x: Int, z: Int) = resident(game, id, game.tile(x, z))
        game.spirit = spawn(10573, 3043, 2974)
        for ((x, z) in FISH_SPOTS) game.fish += spawn(if (z > 2977) 10565 else 10568, x, z)
        for (z in listOf(2982, 2972)) game.pools += spawn(10570, 3047, z)
        for ((i, c) in CANNONS.withIndex()) spawn(10576 + i, c.first, c.second)
        game.captains += spawn(10587, 3034, 2976)
        game.captains += spawn(10585, 3060, 2978)
        updateHud(game)
    }

    fun leave(p: Player, returnToPier: Boolean) {
        val wasWaiting = waiting.remove(p)
        val game = voyages.remove(p)
        if (game == null && !wasWaiting && p.vars[SeaVars.trip] == 0) return
        game?.points?.remove(p)
        game?.tethered?.remove(p)
        game?.loaders?.remove(p)
        if (game != null && game.points.isEmpty()) destroy(game)
        for (slot in p.inv.indices) if (p.inv[slot]?.id in harpoonfish) p.inv[slot] = null
        set(p, SeaVars.trip, 0)
        set(p, SeaBits.points, 0)
        set(p, SeaBits.tethered, 0)
        p.ifCloseOverlay(SeaUi.hud, events)
        if (returnToPier) move(p, pier)
    }

    private fun resident(game: Crew, id: Int, pos: CoordGrid): Npc {
        val npc = Npc(types.getValue(id), pos)
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        game.residents += npc
        return npc
    }

    private fun destroy(game: Crew) {
        for (n in game.residents) if (n.isSlotAssigned) npcs.del(n, Int.MAX_VALUE)
        game.residents.clear()
    }

    private fun move(p: Player, to: CoordGrid) = PathingEntityCommon.telejump(p, collision, to)

    fun tick(controller: Controller) {
        for (p in waiting.toList()) {
            if (p.coords.x !in 3129..3135 || p.coords.z !in 2830..2847) leave(p, false)
        }
        if (waiting.isNotEmpty() && --countdown <= 0) launch(waiting.toList())
        for (game in voyages.values.toSet()) tick(game)
        if (waiting.isEmpty() && voyages.isEmpty()) {
            controllers.del(controller)
            timer = false
        } else {
            controller.aiTimer(1)
        }
    }

    private fun tick(g: Crew) {
        g.elapsed++
        if (g.returnIn >= 0) {
            if (--g.returnIn <= 0) g.points.keys.toList().forEach { leave(it, true) }
            return
        }
        for ((p, where) in g.tethered.toMap()) if (p.coords != where) untether(g, p)
        if (g.submerged > 0) {
            if (--g.submerged == 0) {
                g.energy = g.maxEnergy
                g.spirit.transmog(
                    types.getValue(if (g.phases >= 3) 10575 else 10573),
                    Int.MAX_VALUE,
                )
                g.pools.forEach { it.resetTransmog() }
                if (g.phases >= 3)
                    g.points.keys.forEach {
                        it.mes(
                            "Tempoross has gathered too much energy. The storm is out of control!"
                        )
                    }
            }
        } else {
            // At 350 the native storm bar is full. The enraged phase allows queued shots to land.
            g.storm += if (g.phases >= 3) 20 else 1
            if (g.storm >= 350) {
                finish(g, false)
                return
            }
            if (g.elapsed % 3 == 0)
                for (i in g.cannons.indices) {
                    if (g.disabled[i] > g.elapsed) continue
                    val shot = g.cannons[i].removeFirstOrNull() ?: continue
                    crateVisual(g, i)
                    g.energy = (g.energy - shot).coerceAtLeast(0)
                }
            if (g.energy == 0) {
                g.submerged = 74
                g.phases++
                g.storm = 0
                g.waveIn = 0
                g.spirit.transmog(types.getValue(10574), Int.MAX_VALUE)
                g.pools.forEach { it.transmog(types.getValue(10571), Int.MAX_VALUE) }
                g.points.keys.forEach {
                    it.mes("Tempoross is vulnerable! Harpoon the spirit pool!")
                }
            } else if (g.energy * 10 > g.maxEnergy && g.elapsed % 45 == 0) {
                when ((g.elapsed / 45) % 3) {
                    1 -> {
                        g.waveIn = 9
                        g.points.keys.forEach { it.mes("A colossal wave closes in...") }
                    }
                    2 -> {
                        val p = random.pick(g.points.keys)
                        val c = p.coords
                        light(g, c)
                        light(g, c.translate(1, 0))
                    }
                    else -> {
                        val i = random.of(4)
                        g.disabled[i] = g.elapsed + 12
                        g.loaders
                            .filterValues { it == i }
                            .keys
                            .toList()
                            .forEach { p ->
                                p.cancelActiveCoroutine()
                                g.loaders.remove(p)
                                p.mes("A torrent stuns you at the cannon.")
                            }
                    }
                }
            }
            if (g.waveIn > 0 && --g.waveIn == 0) wave(g)
        }
        if (g.elapsed % 28 == 0) {
            val start = random.of(3)
            for ((i, n) in g.fish.withIndex()) {
                if (i % 3 == start) n.transmog(types.getValue(10569), Int.MAX_VALUE)
                else n.resetTransmog()
            }
        }
        if (g.elapsed % 2 == 0) for (p in g.points.keys) if (p.coords in g.fires) loseSupplies(p)
        if (g.elapsed % 20 == 0 && g.fires.isNotEmpty() && g.fires.size < 24) {
            val tile = random.pick(g.fires.keys)
            val next = tile.translate(if (random.of(2) == 0) 1 else -1, 0)
            if (next !in g.firesSeen && (collision[next.x, next.z, next.level] and 0x240100) == 0)
                light(g, next)
        }
        if (g.elapsed % 2 == 0) updateHud(g)
    }

    fun scatter(g: Crew, p: Player, damage: Int) {
        if (g.returnIn >= 0 || g.submerged <= 0 || p !in g.points) return
        g.add(p, 55)
        p.runEnergy = (p.runEnergy + 300).coerceAtMost(10000)
        g.essence = (g.essence - damage).coerceAtLeast(0)
        if (g.essence == 0) finish(g, true)
    }

    private fun finish(g: Crew, success: Boolean) {
        if (g.returnIn >= 0) return
        g.returnIn = 33
        if (success) {
            subdued++
            if (fastest == 0 || g.elapsed < fastest) fastest = g.elapsed
        }
        for ((p, points) in g.points) {
            untether(g, p)
            if (success) {
                set(p, SeaVars.kills, (p.vars[SeaVars.kills] + 1).coerceAtLeast(1))
                set(p, SeaVars.best, maxOf(points, p.vars[SeaVars.best]))
                val bestTime = p.vars[SeaVars.fastest]
                if (bestTime == 0 || g.elapsed < bestTime) set(p, SeaVars.fastest, g.elapsed)
                val excess = points - 2000
                val permits =
                    if (excess < 0) 0
                    else 1 + excess / 700 + if (random.of(700) < excess % 700) 1 else 0
                set(p, SeaBits.permits, (p.vars[SeaBits.permits] + permits).coerceAtMost(8000))
                p.mes("Tempoross has been subdued. You earned $permits reward permits.")
            } else p.mes("The storm overwhelms the ship!")
        }
        for (n in g.captains) n.transmog(types.getValue(n.id + 1), Int.MAX_VALUE)
        updateHud(g)
    }

    fun crateVisual(g: Crew, i: Int) {
        val fill =
            when (g.cannons[i].size) {
                in 0..7 -> 0
                in 8..14 -> 1
                else -> 2
            }
        if (fill == g.cannonFill[i]) return
        val (x, z) = CANNONS[i]
        val pos = g.tile(x, z)
        val loc = locs.findExact(pos, locTypes.getValue(40968 + 4 * g.cannonFill[i] + i))
        if (loc != null) locs.change(loc, locTypes.getValue(40968 + 4 * fill + i), Int.MAX_VALUE)
        g.cannonFill[i] = fill
    }

    private fun light(g: Crew, pos: CoordGrid) {
        if (pos in g.fires) return
        g.firesSeen += pos
        val npc = resident(g, 8643, pos)
        locs.add(
            pos,
            locTypes.getValue(37582),
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        g.fires[pos] = npc
    }

    fun douse(g: Crew, pos: CoordGrid) {
        val npc = g.fires.remove(pos) ?: return
        g.residents.remove(npc)
        if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        locs.findExact(pos, locTypes.getValue(37582))?.let { locs.del(it, Int.MAX_VALUE) }
    }

    private fun wave(g: Crew) {
        for (p in g.points.keys) {
            if (p in g.tethered) {
                g.add(p, 10)
                untether(g, p)
            } else {
                p.cancelActiveCoroutine()
                loseSupplies(p)
                val next = p.coords.translate(1, 0)
                if ((collision[next.x, next.z, next.level] and 0x240100) == 0) move(p, next)
                p.mes("The wave sweeps over you!")
            }
        }
        for (i in ANCHORS.indices) if (random.of(100) < 15 + 10 * g.storm / 350 && i !in g.broken) {
            val loc = locs.findExact(anchor(g, i), locTypes.getValue(ANCHORS[i].first))
            if (loc != null) {
                locs.change(loc, locTypes.getValue(ANCHORS[i].second), Int.MAX_VALUE)
                g.broken += i
            }
        }
        g.fires.keys.toList().forEach { douse(g, it) }
        g.firesSeen.clear()
    }

    fun anchor(g: Crew, i: Int) = ANCHOR_TILES[i].let { (x, z) -> g.tile(x, z) }

    fun repair(g: Crew, i: Int): Boolean {
        if (i !in g.broken) return false
        val loc = locs.findExact(anchor(g, i), locTypes.getValue(ANCHORS[i].second)) ?: return false
        locs.change(loc, locTypes.getValue(ANCHORS[i].first), Int.MAX_VALUE)
        return g.broken.remove(i)
    }

    fun untether(g: Crew, p: Player) {
        g.tethered.remove(p)
        set(p, SeaBits.tethered, 0)
    }

    private fun loseSupplies(p: Player) {
        val atRisk =
            listOf(RAW, COOKED) +
                listOf(objs.hammer.id, objs.rope.id, objs.bucket_empty.id, objs.bucket_water.id)
        val slots = p.inv.indices.filter { p.inv[it]?.id in atRisk }.toMutableList()
        repeat(minOf(slots.size, random.of(1..3))) {
            p.inv[slots.removeAt(random.of(slots.size))] = null
        }
    }

    private fun updateHud(g: Crew) {
        val energy = if (g.submerged > 0) g.maxEnergy * (74 - g.submerged) / 74 else g.energy
        for ((p, points) in g.points) p.runClientScript(
            4074,
            if (g.submerged > 0) 1 else 0,
            points,
            energy,
            g.essence,
            g.maxEnergy,
            g.maxEssence,
            g.storm,
            *Array<Any>(4) { if (it in g.broken) 1 else 0 },
            *Array<Any>(4) { 0 },
        )
    }

    companion object {
        val ANCHORS = listOf(41352 to 40996, 41353 to 40997, 41354 to 41010, 41355 to 41011)
        val ANCHOR_TILES = listOf(3036 to 2977, 3058 to 2977, 3044 to 2995, 3043 to 2961)
        val CANNONS = listOf(3038 to 2978, 3038 to 2976, 3056 to 2978, 3056 to 2976)
        val FISH_SPOTS =
            listOf(
                3048 to 3002,
                3051 to 2995,
                3037 to 2996,
                3036 to 2963,
                3048 to 2957,
                3046 to 2954,
            )
    }
}
