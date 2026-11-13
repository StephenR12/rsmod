package org.rsmod.content.skills.woodcutting.forestry

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.content.interfaces.bank.deliverToBank
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.axeWoodcuttingAnim
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeDespawnTime
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting.Companion.treeLogs
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.*
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

public data class TreeChopRoll(
    val player: Player,
    val tree: BoundLocInfo,
    val type: UnpackedLocType,
) : UnboundEvent

internal object ForestController : ControllerBuilder() {
    val event = build("forestry_event")
}

internal object ForestQueues : QueueReferences() {
    val root = find("forestry_root_chop")
}

public enum class ForestEvent(val duration: Int) {
    Roots(155),
    Sapling(400),
    Entlings(200),
    Flowers(300),
    Leprechaun(100),
    Hives(250),
    Ritual(200),
    Poachers(200),
    Pheasants(160),
}

private data class Visit(val tile: CoordGrid, val tick: Int)

private data class Contribution(
    var count: Int = 0,
    var points: Int = 0,
    var phase: Int = -1,
    var last: Int = -1,
    var next: Int = 0,
    val ingredients: MutableList<Int> = mutableListOf(),
    val rounds: MutableSet<Int> = mutableSetOf(),
)

private class Session(
    val kind: ForestEvent,
    val tree: CoordGrid,
    val tiles: List<CoordGrid>,
    val started: Int,
    val eligible: MutableSet<Player>,
) {
    val group = eligible.size.coerceIn(1, 10)
    val people = mutableMapOf<Player, Contribution>()
    val locs = mutableMapOf<Int, LocInfo>()
    val npcs = mutableMapOf<Int, Npc>()
    val values = IntArray(8)
    val choices = IntArray(8)
    var completed = 0
    var progress = 0
    var special = -1
    var phase = -1
}

private data class RootChop(val origin: CoordGrid, val pos: CoordGrid)

public class ForestryEvents
@Inject
constructor(
    private val clock: MapClock,
    private val random: GameRandom,
    private val gear: ForestryGear,
    private val harvest: ForestryHarvest,
    private val areas: AreaChecker,
    private val collision: CollisionFlagMap,
    private val locs: LocRepository,
    private val npcs: NpcRepository,
    private val cons: ControllerRepository,
    private val ground: ObjRepository,
    private val locTypes: LocTypeList,
    private val npcTypes: NpcTypeList,
    private val items: ObjTypeList,
    private val xp: XpModifiers,
) : PluginScript() {
    private val sessions = mutableMapOf<CoordGrid, Session>()
    private val visits = mutableMapOf<Player, ArrayDeque<Visit>>()
    private val pending = mutableMapOf<CoordGrid, Pair<Int, ForestEvent>>()
    private val cooldown = mutableMapOf<CoordGrid, Int>()
    private val eligibleLogs = setOf(1521, 1519, 6333, 1517, 6332, 10810, 1515, 1513)
    private val flowers = intArrayOf(12120, 12134, 12141, 12143, 12145, 12147, 12149, 12151)
    private val piles = intArrayOf(47493, 47494, 47495, 47496, 47499)
    private val leaves = intArrayOf(6020, 6022, 6024, 6026, 6028, 6030)

    override fun ScriptContext.startup() {
        onEvent<TreeChopRoll> { record(this) }
        onPlayerLogout {
            visits.remove(player)
            for (s in sessions.values) {
                s.eligible.remove(player)
                if (s.people.remove(player) != null) clearEventItems(player, s.kind)
            }
        }
        onEvent<Woodcutting.CutLogs> {
            val luck = player.vars[ForestryBits.luck]
            if (luck > 0 && random.of(4) == 0) {
                VarPlayerIntMapSetter.set(player, ForestryBits.luck, luck - 1)
                reward(player, 28134, if (random.of(5) < 3) 22 else 23)
                woodXp(player, player.woodcuttingLvl * 1.5)
            }
        }
        onAiConTimer(ForestController.event) {
            val origin = controller.coords
            val session = sessions[origin]
            if (session == null) {
                val scheduled = pending.remove(origin)
                if (scheduled == null || !start(origin, scheduled.second)) {
                    cons.del(controller)
                    return@onAiConTimer
                }
            } else if (
                clock.cycle - session.started >= session.kind.duration || finished(session)
            ) {
                end(session)
                cons.del(controller)
                return@onAiConTimer
            } else update(session)
            controller.aiTimer(1)
        }
        for (id in 47482..47483) onOpLoc1(locTypes.getValue(id)) {
            val s = at(it.loc.coords) ?: return@onOpLoc1
            val req = RootChop(s.tree, it.loc.coords)
            clearWeakQueue(ForestQueues.root)
            chop(req, false)
        }
        onPlayerQueueWithArgs<RootChop>(ForestQueues.root) { chop(it.args, true) }
        for ((i, id) in piles.withIndex()) onOpLoc1(locTypes.getValue(id)) {
            collect(it.loc.coords, i)
        }
        onOpLoc1(locTypes.getValue(47484)) { feed(it.loc.coords) }
        onOpLoc2(locTypes.getValue(47484)) {
            val s = at(it.loc.coords) ?: return@onOpLoc2
            mes("The sapling has ${s.progress*100/(s.group*900)}% of the nutrition it needs.")
        }
        for ((i, id) in flowers.withIndex()) onOpNpc1(npcTypes.getValue(id)) { tend(it.npc, i) }
        for (id in listOf(12543, 12544)) {
            onOpNpc1(npcTypes.getValue(id)) {
                val n = it.npc
                val pick = choice3("Prune the top.", 0, "Prune the back.", 1, "Prune the sides.", 2)
                prune(n, pick)
            }
            onOpNpc3(npcTypes.getValue(id)) { prune(it.npc, 0) }
            onOpNpc4(npcTypes.getValue(id)) { prune(it.npc, 1) }
            onOpNpc5(npcTypes.getValue(id)) { prune(it.npc, 2) }
        }
        for (id in 12515..12518) onOpNpc1(npcTypes.getValue(id)) { buildHive(it.npc) }
        for (id in 49936..49937) onOpLoc1(locTypes.getValue(id)) { stealEgg(it.loc.coords) }
        onOpNpc1(npcTypes.getValue(12536)) { handEgg(it.npc) }
        onOpNpcU(npcTypes.getValue(12536), items.getValue(28661)) { handEgg(it.npc) }
        onOpNpc1(npcTypes.getValue(12541)) { disarm(it.npc) }
        onOpNpc3(npcTypes.getValue(12163)) { deposit(it.npc) }
        onOpNpc1(npcTypes.getValue(12163)) {
            val n = it.npc
            if (choice2("Deposit items.", true, "Leave.", false)) deposit(n)
        }
        onOpNpc1(npcTypes.getValue(12519)) {
            mes("Find the ritual circle whose shape and colour each appear only once.")
        }
        onOpNpc1(npcTypes.getValue(12514)) {
            mes("Bring logs to the unfinished beehive to help the bees move in.")
        }
    }

    private fun nearby(a: CoordGrid, b: CoordGrid) =
        a.level == b.level && a.chebyshevDistance(b) <= 30

    private fun record(e: TreeChopRoll) {
        val p = e.player
        val pos = e.tree.coords
        val history = visits.getOrPut(p) { ArrayDeque() }
        val previous = history.lastOrNull()
        if (previous != null && previous.tile == pos && clock.cycle - previous.tick <= 4)
            history.removeLast()
        else {
            for (s in sessions.values) if (p !in s.people) s.eligible.remove(p)
            if (history.size == 2) history.removeFirst()
        }
        history.addLast(Visit(pos, clock.cycle))
        if (
            !gear.hasKit(p) ||
                e.type.treeLogs.id !in eligibleLogs ||
                pos.level != 0 ||
                pos.z >= 6400 ||
                areas.inArea(areasWoodGuild, pos) ||
                pending.containsKey(pos) ||
                cooldown.getOrDefault(pos, 0) > clock.cycle ||
                sessions.keys.any { nearby(it, pos) }
        )
            return
        val kind = ForestEvent.entries[random.of(if (p.members) 9 else 3)]
        val delay = random.of(1..e.type.treeDespawnTime.coerceAtLeast(2))
        pending[pos] = (clock.cycle + delay) to kind
        val c = Controller(ForestController.event, pos)
        cons.add(c, Int.MAX_VALUE)
        c.aiTimer(delay)
    }

    private val areasWoodGuild
        get() = org.rsmod.api.config.refs.areas.woodcutting_guild

    private fun start(origin: CoordGrid, kind: ForestEvent): Boolean {
        cooldown.entries.removeIf { it.value <= clock.cycle }
        cooldown[origin] = clock + random.of(67..200)
        if (sessions.keys.any { nearby(origin, it) }) return false
        val eligible =
            visits
                .filter { (p, v) ->
                    nearby(p.coords, origin) &&
                        v.any { nearby(it.tile, origin) && clock.cycle - it.tick <= 300 }
                }
                .keys
                .toMutableSet()
        if (eligible.isEmpty() || eligible.none(gear::hasKit)) return false
        val current =
            visits
                .count { (_, v) ->
                    v.lastOrNull()?.let { it.tile == origin && clock.cycle - it.tick <= 5 } == true
                }
                .coerceIn(1, 10)
        // Published odds depend on the tree and group. Ten cutters double the solo chance.
        if (random.of(120) >= 9 + current) return false
        val tiles = mutableListOf<CoordGrid>()
        for (radius in 2..7) for (dx in -radius..radius) for (dz in -radius..radius) {
            if (
                maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) != radius ||
                    dx % 2 != 0 ||
                    dz % 2 != 0
            )
                continue
            val p = origin.translate(dx, dz)
            if (
                collision[p.x, p.z, p.level] and
                    (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR) !=
                    0 || locs.findAll(p).any { it.shape == LocShape.CentrepieceStraight }
            )
                continue
            tiles += p
        }
        if (tiles.size < 9) return false
        val s = Session(kind, origin, tiles, clock.cycle, eligible)
        sessions[origin] = s
        when (kind) {
            ForestEvent.Roots -> for (i in 0..5) place(s, i, 47482)
            ForestEvent.Sapling -> {
                place(s, 0, 47484)
                for (i in piles.indices) place(s, i + 1, piles[i])
                for (i in 0..2) s.choices[i] = random.of(5)
            }
            ForestEvent.Entlings ->
                for (i in 0..4) {
                    spawn(s, i, 12543)
                    s.choices[i] = random.of(3)
                    request(s, i)
                }
            ForestEvent.Flowers -> for (i in flowers.indices) spawn(s, i, flowers[i])
            ForestEvent.Leprechaun -> {
                spawn(s, 8, 12163)
                place(s, 0, 49934)
            }
            ForestEvent.Hives -> {
                spawn(s, 8, 12514)
                spawn(s, 0, 12515)
            }
            ForestEvent.Ritual -> spawn(s, 8, 12519)
            ForestEvent.Poachers -> {
                for (i in 0..2) spawn(s, 5 + i, 12553 + i)
                spawn(s, 8, 12559)
            }
            ForestEvent.Pheasants -> {
                spawn(s, 8, 12536)
                for (i in 0..3) place(s, i, 49936)
            }
        }
        update(s)
        return true
    }

    private fun place(s: Session, i: Int, id: Int) {
        s.locs[i] =
            locs.add(
                s.tiles[i],
                locTypes.getValue(id),
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
    }

    private fun spawn(s: Session, i: Int, id: Int) {
        val n = s.npcs[i]
        if (n != null) {
            n.transmog(npcTypes.getValue(id), Int.MAX_VALUE)
            return
        }
        val created = Npc(npcTypes.getValue(id), s.tiles[i])
        npcs.add(created, Int.MAX_VALUE)
        created.mode = org.rsmod.game.entity.npc.NpcMode.None
        created.respawns = false
        s.npcs[i] = created
    }

    private fun at(pos: CoordGrid) = sessions.values.firstOrNull { s -> pos in s.tiles.take(9) }

    private fun npcSession(n: Npc) = sessions.values.firstOrNull { n in it.npcs.values }

    private fun finished(s: Session) =
        when (s.kind) {
            ForestEvent.Sapling -> s.progress >= s.group * 900
            ForestEvent.Entlings -> s.completed >= 5
            ForestEvent.Hives -> s.completed >= 6
            ForestEvent.Poachers -> s.progress >= 3
            else -> false
        }

    private fun update(s: Session) {
        val t = clock.cycle - s.started
        when (s.kind) {
            ForestEvent.Roots ->
                if (t >= 30 && (t - 30) % 20 == 0) {
                    if (s.special >= 0) place(s, s.special, 47482)
                    s.special = random.of(6)
                    place(s, s.special, 47483)
                }
            ForestEvent.Pheasants ->
                if (t % 20 == 0) {
                    if (s.special >= 0) place(s, s.special, 49936)
                    s.special = random.of(4)
                    place(s, s.special, 49937)
                    s.phase++
                }
            ForestEvent.Flowers ->
                if (t % 72 == 0) {
                    s.special = random.of(4)
                    s.phase++
                }
            ForestEvent.Entlings ->
                if (t > 0 && t % 10 == 0)
                    for (i in 0..4) if (s.values[i] < s.group * 8) request(s, i)
            ForestEvent.Poachers ->
                if (t % 20 == 0) {
                    if (t > 0 && s.npcs.containsKey(0)) s.progress++
                    s.npcs.remove(0)?.let { npcs.del(it, Int.MAX_VALUE) }
                    val n = Npc(npcTypes.getValue(12541), s.tiles[random.of(5)])
                    npcs.add(n, Int.MAX_VALUE)
                    n.respawns = false
                    s.npcs[0] = n
                }
            ForestEvent.Ritual -> {
                if (t % 25 == 0) {
                    val shape = random.of(4)
                    val colour = random.of(4)
                    val pairs =
                        listOf(
                            (shape + 1) % 4 to (colour + 1) % 4,
                            (shape + 1) % 4 to (colour + 2) % 4,
                            (shape + 2) % 4 to (colour + 1) % 4,
                            (shape + 2) % 4 to (colour + 2) % 4,
                        )
                    s.special = random.of(5)
                    s.phase++
                    var other = 0
                    for (i in 0..4) {
                        val (a, b) = if (i == s.special) shape to colour else pairs[other++]
                        spawn(s, i, 12520 + a * 4 + b)
                    }
                }
                for (p in s.eligible) if (p.coords == s.tiles[s.special]) {
                    val c = s.people.getOrPut(p) { Contribution() }
                    if (c.phase != s.phase) {
                        c.phase = s.phase
                        c.count++
                        reward(p, 28134, if (random.of(5) == 0) 20 else 21)
                        woodXp(p, 2.0 * p.woodcuttingLvl + 60)
                    }
                }
            }
            ForestEvent.Leprechaun -> {
                if (t > 0 && t % 20 == 0) {
                    s.locs.remove(s.special.coerceAtLeast(0))?.let { locs.del(it, Int.MAX_VALUE) }
                    s.special = random.of(8)
                    place(s, s.special, 49934)
                    s.phase++
                }
                for (p in s.eligible) if (p.coords == s.tiles[s.special.coerceAtLeast(0)]) {
                    val c = s.people.getOrPut(p) { Contribution() }
                    if (c.rounds.add(t / 20))
                        VarPlayerIntMapSetter.set(
                            p,
                            ForestryBits.luck,
                            (p.vars[ForestryBits.luck] + random.of(2..3)).coerceAtMost(255),
                        )
                }
            }
            else -> Unit
        }
    }

    private fun request(s: Session, i: Int) {
        s.npcs[i]?.say(
            listOf("A little off the top!", "Breezy at the back!", "Short on the sides!")[
                s.choices[i]]
        )
    }

    private fun reward(p: Player, id: Int, n: Int) {
        if (n > 0 && !gear.reward(p, id, n)) p.invAddOrDrop(ground, items.getValue(id), n)
    }

    private fun woodXp(p: Player, value: Double) {
        p.statAdvance(stats.woodcutting, value * xp.get(p, stats.woodcutting))
    }

    private fun participated(p: Player, s: Session): Contribution? {
        val c = s.people.getOrPut(p) { Contribution() }
        if (c.next > clock.cycle) return null
        c.next = clock + 2
        return c
    }

    private fun ProtectedAccess.chop(req: RootChop, resolve: Boolean) {
        val s = sessions[req.origin] ?: return
        val loc = s.locs.values.firstOrNull { it.coords == req.pos } ?: return
        if (
            s.kind != ForestEvent.Roots ||
                player.coords.level != loc.coords.level ||
                player.coords.chebyshevDistance(req.pos) > 2
        )
            return
        val axe = Woodcutting.findAxe(player, items) ?: return
        if (resolve) {
            val c = s.people.getOrPut(player) { Contribution() }
            c.count++
            val eligible = player in s.eligible
            val special = loc.id == 47483
            val level = player.woodcuttingLvl
            val bark =
                when {
                    !eligible -> 1
                    special -> minOf(12, 8 + (level - 1) / 20) + random.of(2)
                    else -> (if (level > 40) 3 else 2) + random.of(2)
                }
            val result = harvest.chop(player, axe)
            if (!result.skipLog) reward(player, 28134, bark)
            woodXp(
                player,
                result.xp * (if (eligible) level * (if (special) 1.2 else 0.55) else 1.0),
            )
        }
        anim(items[axe].axeWoodcuttingAnim)
        weakQueue(ForestQueues.root, 4, req)
    }

    private fun ProtectedAccess.collect(pos: CoordGrid, i: Int) {
        val s = at(pos) ?: return
        if (s.kind != ForestEvent.Sapling || s.locs[i + 1]?.coords != pos) return
        val c = participated(player, s) ?: return
        val stage = c.ingredients.size
        if (stage == 3) return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                if (stage > 0) delete(bag, 28647 + stage, 1)
                add(bag, 28648 + stage, 1)
            }
        if (tx.success) c.ingredients += i
    }

    private fun ProtectedAccess.feed(pos: CoordGrid) {
        val s = at(pos) ?: return
        val c = s.people[player] ?: return
        if (
            s.kind != ForestEvent.Sapling ||
                c.ingredients.size != 3 ||
                !invDel(inv, items.getValue(28650)).success
        )
            return
        val correct = c.ingredients.indices.filter { c.ingredients[it] == s.choices[it] }
        val nutrition = 10 + 30 * correct.size
        for (i in correct) mes(
            "The sapling likes ${locTypes.getValue(piles[c.ingredients[i]]).name.lowercase()} as ingredient ${i+1}."
        )
        c.ingredients.clear()
        s.progress += nutrition
        c.count++
        c.points += nutrition
        if (player in s.eligible) {
            reward(player, 28134, (nutrition * 12 / 100).coerceAtLeast(1))
            woodXp(player, player.woodcuttingLvl * nutrition / 100.0)
            if (player.members) statAdvance(stats.farming, stat(stats.farming) * nutrition / 200.0)
        } else woodXp(player, 1.0)
    }

    private fun ProtectedAccess.prune(n: Npc, style: Int) {
        val s = npcSession(n) ?: return
        val i = s.npcs.entries.first { it.value == n }.key
        val c = participated(player, s) ?: return
        if (s.values[i] >= s.group * 8 || s.choices[i] != style) {
            c.next = clock + 5
            mes("The entling recoils from the wrong cut.")
            return
        }
        s.values[i]++
        c.count++
        if (player in s.eligible) {
            reward(player, 28134, random.of(4..5))
            woodXp(player, player.woodcuttingLvl * 0.5)
            if (player.members) statAdvance(stats.fletching, stat(stats.fletching) * 0.2)
        } else woodXp(player, 1.0)
        if (s.values[i] == s.group * 8) {
            spawn(s, i, 12544)
            s.completed++
        } else {
            s.choices[i] = random.of(3)
            request(s, i)
        }
    }

    private fun ProtectedAccess.tend(n: Npc, i: Int) {
        val s = npcSession(n) ?: return
        val c = participated(player, s) ?: return
        if (i / 2 != s.special) {
            mes("This bush has no pollen to exchange.")
            return
        }
        if (c.phase != s.phase) {
            c.last = -1
            c.phase = s.phase
        }
        if (c.last == i) {
            mes("Take the pollen to the matching bush.")
            return
        }
        val pollen = items.getValue(28190)
        if (c.last < 0 && pollen !in inv && !invAdd(inv, pollen).success) return
        if (c.last >= 0) c.rounds += s.phase
        c.last = i
        c.count++
        if (player in s.eligible) {
            val bark = random.of(1..2)
            c.points += bark
            reward(player, 28134, bark)
            woodXp(player, player.woodcuttingLvl / 5.0 + 2.8)
        } else woodXp(player, 1.0)
    }

    private fun ProtectedAccess.buildHive(n: Npc) {
        val s = npcSession(n) ?: return
        if (s.npcs[0] !== n || participated(player, s) == null) return
        val storage = gear.logStore(player)
        val source =
            if (inv.any { it?.id in eligibleLogs || it?.id in setOf(1511, 2862, 19669) }) inv
            else if (
                player.inv.any { it?.id in setOf(28140, 28142, 28143, 28145) } ||
                    player.worn.any { it?.id in setOf(28140, 28142, 28143, 28145) }
            )
                storage
            else return
        val slot =
            source.indexOfFirst { it?.id in eligibleLogs || it?.id in setOf(1511, 2862, 19669) }
        if (slot < 0) return
        val log = source[slot] ?: return
        if (!invDel(source, items[log], 1, slot).success) return
        val group =
            when (log.id) {
                1511,
                2862 -> 0
                10810,
                1515,
                1513,
                19669 -> 2
                else -> 1
            }
        val c = s.people.getValue(player)
        c.count++
        s.progress++
        if (player in s.eligible) {
            reward(player, 28134, random.of(2 + group..3 + group))
            woodXp(player, player.woodcuttingLvl * (0.4 + 0.18 * group))
            if (vars[ForestryBits.house] > 0)
                statAdvance(stats.construction, stat(stats.construction) * (0.2 + group * 0.1))
        } else woodXp(player, 1.0)
        if (s.progress >= s.group * 5) {
            s.progress = 0
            s.completed++
            if (s.completed < 6) spawn(s, 0, 12515)
        } else spawn(s, 0, 12515 + minOf(3, s.progress * 4 / (s.group * 5)))
    }

    private fun ProtectedAccess.stealEgg(pos: CoordGrid) {
        val s = at(pos) ?: return
        if (s.kind != ForestEvent.Pheasants || s.tiles[s.special] != pos) return
        val c = participated(player, s) ?: return
        val egg = items.getValue(28661)
        if (egg in inv || !invAdd(inv, egg).success) return
        c.count++
        if (player in s.eligible) {
            reward(player, 28134, if (random.of(10) == 0) 8 else 9)
            woodXp(player, player.woodcuttingLvl * 1.2)
            statAdvance(stats.thieving, stat(stats.thieving) * 0.5)
            if (random.of(2) == 0) reward(player, 28624, 1)
            if (random.of(900 - (player.woodcuttingLvl.coerceAtMost(99) - 1) * 450 / 98) == 0)
                reward(player, 28663, 1)
        }
    }

    private fun ProtectedAccess.handEgg(n: Npc) {
        if (
            npcSession(n)?.kind == ForestEvent.Pheasants &&
                invDel(inv, items.getValue(28661)).success
        )
            mes("You hand the pheasant egg to the forester.")
    }

    private fun ProtectedAccess.disarm(n: Npc) {
        val s = npcSession(n) ?: return
        if (s.npcs[0] !== n) return
        val c = participated(player, s) ?: return
        s.npcs.remove(0)
        npcs.del(n, Int.MAX_VALUE)
        c.count++
        s.completed++
        if (player in s.eligible) {
            reward(player, 28134, random.of(8..12))
            statAdvance(stats.hunter, stat(stats.hunter) * 0.5)
            woodXp(player, player.woodcuttingLvl * 0.5)
        }
    }

    private suspend fun ProtectedAccess.deposit(n: Npc) {
        val s = npcSession(n) ?: return
        if (!choice2("Deposit my items.", true, "Leave.", false) || npcSession(n) != s) return
        val bank = this.bank
        for (slot in inv.indices) {
            val item = inv[slot] ?: continue
            if (player !in s.eligible && item.id !in gear.logs) continue
            if (items[item].param(org.rsmod.api.config.refs.params.no_bank) != 0) continue
            if (player.deliverToBank(bank, listOf(items.uncert(item)))) inv[slot] = null
        }
        if (gear.hasBasket(player)) gear.bankLogs(player)
    }

    private fun clearEventItems(p: Player, kind: ForestEvent) {
        for (id in
            when (kind) {
                ForestEvent.Sapling -> listOf(28648, 28649, 28650)
                ForestEvent.Flowers -> listOf(28190)
                ForestEvent.Pheasants -> listOf(28661)
                else -> emptyList()
            }) p.invDel(p.inv, items.getValue(id), p.inv.count(items.getValue(id)).coerceAtLeast(1))
    }

    private fun end(s: Session) {
        sessions.remove(s.tree)
        for ((p, c) in s.people) {
            clearEventItems(p, s.kind)
            if (c.count <= 0 || !nearby(p.coords, s.tree)) continue
            val level = p.woodcuttingLvl
            if (s.kind == ForestEvent.Entlings && random.of(5) < s.completed)
                ground.add(items.getValue(5070 + random.of(3)), p.coords, 100, p)
            if (p !in s.eligible) continue
            when (s.kind) {
                ForestEvent.Sapling ->
                    if (finished(s)) {
                        val avg = c.points / c.count
                        woodXp(p, level * avg * 0.4)
                        reward(p, 28134, 60 + avg * 140 / 100)
                        reward(p, leaves[random.of(6)], 20)
                    }
                ForestEvent.Entlings -> {
                    woodXp(p, level * s.completed * 3.0)
                    reward(p, 28134, s.completed * 25)
                    if (p.members) reward(p, leaves[random.of(6)], s.completed * 4)
                }
                ForestEvent.Flowers ->
                    if (c.rounds.size >= 4) {
                        reward(p, 28134, c.points * 3 / 4)
                        reward(p, 464, random.of(1..8))
                        reward(
                            p,
                            if (maxOf(level, p.stat(stats.farming)) >= 65) 5295 else 5318,
                            random.of(1..3),
                        )
                        woodXp(p, level / 2.0 + 3)
                    }
                ForestEvent.Ritual -> {
                    woodXp(p, 2.0 * c.count * level + 55)
                    reward(p, 28134, c.count * 18)
                    if (random.of(30) == 0) reward(p, 28655, 1)
                }
                ForestEvent.Hives -> {
                    woodXp(p, level * s.completed * 3.0)
                    reward(p, 28134, s.completed * 18)
                    if (random.of(3) < 2 && s.completed > 0) reward(p, 28674, 1)
                }
                ForestEvent.Poachers ->
                    if (s.progress < 3) {
                        reward(p, 28134, c.count * 8)
                        woodXp(p, level * c.count.toDouble())
                        if (random.of(30) == 0) reward(p, 28626, 1)
                    }
                else -> Unit
            }
        }
        for (loc in s.locs.values) locs.del(loc, Int.MAX_VALUE)
        for (n in s.npcs.values) npcs.del(n, Int.MAX_VALUE)
    }
}
