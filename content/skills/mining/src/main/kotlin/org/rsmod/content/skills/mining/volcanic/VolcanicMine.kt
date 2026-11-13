package org.rsmod.content.skills.mining.volcanic

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.mining.CelestialMiningRing
import org.rsmod.content.skills.mining.CrystalPickaxeLifecycle
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeLevel
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private class Miner(var water: Int, var last: CoordGrid) {
    var points = 0
    var capped = 0
    val checked = mutableSetOf<Int>()
    var direction = 2
}

private class Boulder(val channel: Int, val group: Int) {
    var stage = 0
    var hp = 40 * group
    var loc: LocInfo? = null
    var arrives = 0

    fun position(): CoordGrid {
        val centre = listOf(3807, 3791, 3823)[channel]
        val z = listOf(10127, 10136, 10149, 10172, listOf(10200, 10196, 10198)[channel])[stage]
        val offset = listOf(3, 3, 2, 1, 0)[stage]
        return CoordGrid(centre - offset, z - offset, 1)
    }
}

private data class Swing(val channel: Int, val stage: Int, val tile: CoordGrid)

private class Volcano(val timer: Controller, val begins: Int) {
    val people = mutableMapOf<Player, Miner>()
    val boulders = mutableListOf<Boulder>()
    val blocked = BooleanArray(3) { true }
    val pressure = IntArray(3)
    val revealed = BooleanArray(3)
    val hardened = mutableMapOf<CoordGrid, Int>()
    val scenery = mutableListOf<LocInfo>()
    val beasts = mutableListOf<Npc>()
    var stability = 100
    var started = false
    var group = 0
}

public class VolcanicMine
@Inject
constructor(
    private val commerce: PetrifiedPete,
    private val clock: MapClock,
    private val locs: LocRepository,
    private val cons: ControllerRepository,
    private val npcs: NpcRepository,
    private val locTypes: LocTypeList,
    private val npcTypes: NpcTypeList,
    private val items: ObjTypeList,
    private val ground: ObjRepository,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
    private val rng: GameRandom,
    private val celestial: CelestialMiningRing,
    private val crystal: CrystalPickaxeLifecycle,
    private val xp: XpModifiers,
    private val pets: SkillPetRates,
) : PluginScript() {
    private var round: Volcano? = null
    private val chambers =
        listOf(CoordGrid(3785, 10156, 1), CoordGrid(3813, 10172, 1), CoordGrid(3828, 10155, 1))
    private val directions = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)
    private val supplies = (21532..21540).toSet()

    override fun ScriptContext.startup() {
        val entry = locTypes.getValue(31031)
        onOpLoc1(entry) { enter() }
        onOpLoc2(entry) { commerce.pay(this, 30) }
        onOpLoc1(locTypes.getValue(31032)) { leave(player, true, true) }
        onAiConTimer(VolcanicCourse.round) {
            val current = round
            if (current != null && current.timer === controller) tick(current)
        }
        for (id in 31029..31030) onOpLoc1(locTypes.getValue(id)) {
            val v = round ?: return@onOpLoc1
            if (player !in v.people) return@onOpLoc1
            if (!v.started && id == 31029) {
                mes("Wait for the volcano to rumble before descending.")
                return@onOpLoc1
            }
            telejump(
                it.loc.coords.translate(1, if (id == 31029) -1 else 2, if (id == 31029) -2 else 2)
            )
        }
        for (id in 31034..31038) onOpLoc1(locTypes.getValue(id)) {
            val b =
                round?.boulders?.find { b -> b.loc?.coords == it.loc.coords && b.loc?.id == id }
                    ?: return@onOpLoc1
            clearWeakQueue(VolcanicQueue.mine)
            swing(Swing(b.channel, b.stage, it.loc.coords), false)
        }
        onPlayerQueueWithArgs<Swing>(VolcanicQueue.mine) { swing(it.args, true) }
        onOpLoc1(locTypes.getValue(31040)) { inspect(it.loc.coords) }
        onOpLoc1(locTypes.getValue(31043)) { cap(it.loc, true) }
        onOpLocU(locTypes.getValue(31043), VolcanicItems.rock) { cap(it.loc, true) }
        onOpLoc1(locTypes.getValue(31044)) { cap(it.loc, false) }
        onOpLoc1(locTypes.getValue(31045)) {
            if (playing(player) != null && invAdd(inv, VolcanicItems.rock).success)
                locs.change(it.loc, locTypes.getValue(31046), 6)
        }
        val jumps =
            listOf(
                3795 to 10175,
                3788 to 10175,
                3812 to 10170,
                3803 to 10170,
                3827 to 10169,
                3817 to 10171,
            )
        for ((i, dest) in jumps.withIndex()) onOpLoc1(locTypes.getValue(31047 + i)) {
            if (playing(player) != null) {
                telejump(CoordGrid(dest.first, dest.second, 1))
                player.queueHit(1, HitType.Typeless, rng.of(1..3))
            }
        }
        onOpHeld1(VolcanicItems.vessel) { pour(round?.people?.get(player)?.direction ?: 2) }
        onOpHeld3(VolcanicItems.vessel) {
            pour(choice4("North", 0, "East", 1, "South", 2, "West", 3))
        }
        onOpHeld4(VolcanicItems.vessel) { checkWater() }
        onOpWorn2(VolcanicItems.vessel) { pour(round?.people?.get(player)?.direction ?: 2) }
        onOpWorn3(VolcanicItems.vessel) { pour(0) }
        onOpWorn4(VolcanicItems.vessel) { pour(1) }
        onOpWorn5(VolcanicItems.vessel) { pour(2) }
        onOpWorn6(VolcanicItems.vessel) { pour(3) }
        onOpWorn7(VolcanicItems.vessel) { checkWater() }
        onPlayerLogout {
            if (round?.people?.containsKey(player) == true) leave(player, false, true)
        }
        onPlayerLogin {
            if (inMine(player.coords) && round?.people?.containsKey(player) != true) {
                strip(player)
                move(player, entrance)
            }
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (round?.people?.containsKey(player) == true) leave(player, false, false)
        }
    }

    private suspend fun ProtectedAccess.enter() {
        val current = round
        if (current != null && (current.started || current.people.size >= 50)) {
            mes("A game is already in progress.")
            return
        }
        if (!commerce.eligible(player) || commerce.holding(player)) {
            mes("You need level 50 Mining, access to Fossil Island and no belongings held by Pete.")
            return
        }
        if (
            inv.isFull() &&
                !(vars[VolcanicVars.admission] == 0 && invTotal(inv, VolcanicItems.numulite) == 30)
        ) {
            mes("Make room for a heat-proof vessel.")
            return
        }
        if (!commerce.pay(this, 30)) return
        if (round?.let { it.started || it.people.size >= 50 } == true) {
            mes("This game has started. Your admission is valid for the next one.")
            return
        }
        if (!invAdd(inv, VolcanicItems.vessel).success) return
        val v =
            round
                ?: Volcano(Controller(VolcanicCourse.round, lobby), clock + 50).also {
                    round = it
                    cons.add(it.timer, Int.MAX_VALUE)
                    it.timer.aiTimer(1)
                }
        if (vars[VolcanicVars.admission] == 1) vars[VolcanicVars.admission] = 0
        v.people[player] = Miner(if (vars[VolcanicBits.large] != 0) 70 else 35, lobby)
        telejump(lobby)
        if (VolcanicUi.hud !in player.ui) player.ifOpenOverlay(VolcanicUi.hud, events)
        hud(v, player)
    }

    private fun playing(p: Player) = round?.takeIf { it.started && p in it.people && p.level == 1 }

    private fun start(v: Volcano) {
        v.started = true
        v.group = v.people.size
        val channels = 1 + (if (v.group >= 10) 1 else 0) + (if (v.group >= 20) 1 else 0)
        for (i in 0 until channels) {
            val b = Boulder(i, (v.group + channels - 1) / channels)
            v.boulders += b
            place(b)
        }
        resetPressure(v)
        for ((x, z) in listOf(3797 to 10158, 3818 to 10170)) {
            val beast = Npc(npcTypes.getValue(7817), CoordGrid(x, z, 1))
            npcs.add(beast, Int.MAX_VALUE)
            beast.mode = NpcMode.None
            beast.respawns = false
            v.beasts += beast
        }
        for (p in v.people.keys) p.mes(
            "The volcano rumbles. You have ten minutes to mine the boulder and escape."
        )
    }

    private fun resetPressure(v: Volcano) {
        for (i in 0..2) {
            v.pressure[i] = if (v.group <= 1) rng.of(40..60) else rng.of(25..75)
            v.revealed[i] = false
        }
        for (miner in v.people.values) miner.checked.clear()
    }

    private fun tick(v: Volcano) {
        for ((p, m) in v.people.toMap()) {
            if (!p.isSlotAssigned || !inMine(p.coords)) {
                leave(p, false, false)
                continue
            }
            val dx = (p.x - m.last.x).coerceIn(-1, 1)
            val dz = (p.z - m.last.z).coerceIn(-1, 1)
            if (dx != 0 || dz != 0) m.direction = directions.indexOf(dx to dz)
            m.last = p.coords
        }
        if (round !== v) return
        val elapsed = clock.cycle - v.begins
        if (!v.started && elapsed >= 0) start(v)
        if (v.started) {
            if (elapsed == 500) resetPressure(v)
            if (elapsed % 10 == 0) {
                for (i in 0..2) v.pressure[i] =
                    (v.pressure[i] + if (v.blocked[i]) 1 else -1).coerceIn(0, 100)
                val dangerous = v.pressure.count { it !in 25..75 }
                if (dangerous >= 2) v.stability = (v.stability - dangerous + 1).coerceAtLeast(0)
            }
            if (elapsed >= 1000 || v.stability == 0) {
                for (p in v.people.keys.toList()) {
                    leave(p, false, false)
                    p.mes("The volcano erupts!")
                    p.queueHit(0, HitType.Typeless, p.hitpoints)
                }
                return
            }
            for (b in v.boulders) if (b.loc == null && b.stage < 5 && b.arrives <= clock.cycle)
                place(b)
            coolPlatforms(v)
            if (elapsed % 12 == 0)
                for (p in v.people.keys) if (p.level == 1) {
                    val beast =
                        v.beasts.firstOrNull {
                            it.isSlotAssigned && p.coords.chebyshevDistance(it.coords) <= 12
                        }
                    if (beast != null && rng.of(3) == 0)
                        p.queueHit(
                            beast,
                            1,
                            HitType.Ranged,
                            rng.of(0..if (p.vars[varbits.protect_from_missiles] != 0) 1 else 8),
                        )
                    if (v.stability < 65 && rng.of(12) == 0)
                        p.queueHit(2, HitType.Typeless, rng.of(1..8))
                }
        }
        if (elapsed % 5 == 0) for (p in v.people.keys) hud(v, p)
        v.timer.aiTimer(1)
    }

    private fun place(b: Boulder) {
        b.loc =
            locs.add(
                b.position(),
                locTypes.getValue(31034 + b.stage),
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
    }

    private fun hud(v: Volcano, p: Player) {
        val miner = v.people[p] ?: return
        fun set(bit: org.rsmod.game.type.varbit.VarBitType, value: Int) =
            VarPlayerIntMapSetter.set(p, bit, value)
        set(VolcanicBits.players, v.people.size.coerceAtMost(63))
        set(VolcanicBits.stability, v.stability)
        set(VolcanicBits.current, miner.points.coerceAtMost(65535))
        set(VolcanicBits.status, if (v.started) 2 else 1)
        set(
            VolcanicBits.seconds,
            (if (v.started) v.begins + 1000 - clock.cycle else v.begins - clock.cycle).coerceIn(
                0,
                1023,
            ),
        )
        set(VolcanicBits.chamber, (0..2).sumOf { i -> if (v.blocked[i]) 1 shl i else 0 })
        for (i in 0..2) set(VolcanicBits.vent[i], if (v.revealed[i]) v.pressure[i] else 127)
    }

    private fun ProtectedAccess.inspect(pos: CoordGrid) {
        val v = playing(player) ?: return
        val i =
            when {
                pos.x < 3796 -> 0
                pos.x > 3819 -> 2
                else -> 1
            }
        v.revealed[i] = true
        val m = v.people.getValue(player)
        if (m.checked.add(i)) {
            m.points += 50
            statAdvance(stats.mining, 50.0 * xp.get(player, stats.mining))
        }
        for (p in v.people.keys) hud(v, p)
    }

    private fun ProtectedAccess.cap(loc: BoundLocInfo, block: Boolean) {
        val v = playing(player) ?: return
        val i = chambers.indexOf(loc.coords)
        if (i < 0 || v.blocked[i] == block) return
        if (block && !invDel(inv, VolcanicItems.rock).success) {
            mes("You need a large rock to block the chamber.")
            return
        }
        if (!block && Mining.findPickaxe(player, items) == null) {
            mes("You need a pickaxe to clear the chamber.")
            return
        }
        v.blocked[i] = block
        val changed =
            locs.add(
                loc.coords,
                locTypes.getValue(if (block) 31044 else 31043),
                Int.MAX_VALUE,
                loc.angle,
                loc.shape,
            )
        v.scenery += changed
        val m = v.people.getValue(player)
        if (m.capped < 300) {
            m.capped += 50
            m.points += 50
        }
        statAdvance(stats.mining, 50.0 * xp.get(player, stats.mining))
        for (p in v.people.keys) hud(v, p)
    }

    private fun ProtectedAccess.checkWater() {
        round?.people?.get(player)?.let { mes("Your vessel holds ${it.water} measures of water.") }
    }

    private fun ProtectedAccess.pour(direction: Int) {
        val v = playing(player) ?: return
        val m = v.people.getValue(player)
        if (direction !in directions.indices) {
            mes("Face north, east, south or west to throw water.")
            return
        }
        val (dx, dz) = directions[direction]
        val tile = coords.translate(dx, dz)
        if (m.water <= 0) {
            mes("Your vessel is empty.")
            return
        }
        val lava = locs.findExact(tile, locTypes.getValue(30997)) ?: return
        locs.change(lava, locTypes.getValue(30998), 150)
        v.hardened[tile] = clock + 150
        m.water--
        m.points += 2
        m.direction = direction
        hud(v, player)
    }

    private fun coolPlatforms(v: Volcano) {
        val it = v.hardened.iterator()
        while (it.hasNext()) {
            val (tile, end) = it.next()
            val left = end - clock.cycle
            if (left == 25 || left == 5)
                locs.add(
                    tile,
                    locTypes.getValue(if (left == 25) 30999 else 31000),
                    left,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
            if (left <= 0) {
                for (p in v.people.keys) if (p.coords == tile) {
                    p.queueHit(1, HitType.Typeless, rng.of(53..60))
                    move(p, CoordGrid(3813, 10158, 1))
                }
                it.remove()
            }
        }
    }

    private fun ProtectedAccess.swing(req: Swing, roll: Boolean) {
        val v = playing(player) ?: return
        val b = v.boulders.getOrNull(req.channel) ?: return
        val loc = b.loc ?: return
        val size = locTypes[loc].width
        if (
            b.stage != req.stage ||
                loc.coords != req.tile ||
                player.x !in req.tile.x - 1..req.tile.x + size ||
                player.z !in req.tile.z - 1..req.tile.z + size
        )
            return
        val tool =
            Mining.findPickaxe(player, items)
                ?: run {
                    mes("You need a pickaxe.")
                    return
                }
        val pick = items[tool]
        if (
            roll &&
                statRandom(
                    stats.mining,
                    90 + pick.pickaxeLevel,
                    180 + pick.pickaxeLevel,
                    celestial.invisibleBoost(player),
                )
        ) {
            val m = v.people.getValue(player)
            var points = 1
            var miningXp = 5
            if (random.of(listOf(20, 12, 8, 5, 3)[b.stage]) == 0) {
                val required = listOf(50, 50, 50, 50, 55, 70, 85)
                val low = listOf(0, 0, -20, -40, -75, -90, -10)
                val high = listOf(510, 185, 170, 160, 150, 90, 80)
                val i =
                    (6 downTo 1).firstOrNull {
                        player.miningLvl >= required[it] &&
                            statRandom(stats.mining, low[it], high[it], 0)
                    } ?: 0
                val fragment = VolcanicItems.fragments[i]
                if (invAdd(inv, fragment).success) {
                    points = listOf(15, 20, 25, 34, 44, 55, 70)[i]
                    miningXp = listOf(10, 20, 30, 40, 60, 100, 150)[i]
                    secondary()
                    pets.roll(player, stats.mining, 13321, 60000)
                }
            }
            if (points == 1) spam("You chip off a part of the boulder.")
            m.points += points
            statAdvance(stats.mining, miningXp * xp.get(player, stats.mining))
            crystal.consumeCharge(this, tool)
            if (--b.hp <= 0) {
                locs.del(loc, Int.MAX_VALUE)
                b.loc = null
                b.stage++
                if (b.stage >= 5) {
                    for (miner in v.people.values) miner.points += 100
                } else {
                    b.arrives = clock + 8
                    b.hp = (if (b.stage == 4) 60 else 40) * b.group
                }
                for (p in v.people.keys) hud(v, p)
                return
            }
            hud(v, player)
        }
        anim(pick.pickaxeAnim)
        weakQueue(VolcanicQueue.mine, pick.pickaxeInterval, req)
    }

    private fun ProtectedAccess.secondary() {
        if (random.of(10) == 0) {
            invAddOrDrop(
                ground,
                items.getValue(if (random.of(2) == 0) 21543 else 21545),
                random.of(1..4),
            )
            return
        }
        val roll = random.of(100)
        if (roll < 4) invAddOrDrop(ground, VolcanicItems.numulite, random.of(1..10))
        else if (roll < 9) {
            val f = random.of(100)
            val i =
                when {
                    f < 50 -> 0
                    f < 75 -> 1
                    f < 90 -> 2
                    else -> 3
                }
            if (player.vars[if (i == 3) VolcanicBits.rareOff else VolcanicBits.fossilsOff] == 0)
                invAddOrDrop(ground, VolcanicItems.fossil[i], 1)
        }
    }

    private fun move(p: Player, to: CoordGrid) = PathingEntityCommon.telejump(p, collision, to)

    private fun strip(p: Player) {
        for (slot in p.inv.indices) if (p.inv[slot]?.id in supplies) p.inv[slot] = null
        if (p.righthand?.id == VolcanicItems.vessel.id) p.righthand = null
    }

    private fun leave(p: Player, claim: Boolean, outside: Boolean) {
        val v = round ?: return
        val miner = v.people.remove(p) ?: return
        strip(p)
        if (claim && v.started && miner.points > 0) {
            val earned = minOf(miner.points, 2097151 - p.vars[VolcanicVars.points])
            VarPlayerIntMapSetter.set(p, VolcanicVars.points, p.vars[VolcanicVars.points] + earned)
            VarPlayerIntMapSetter.set(p, VolcanicBits.done, 1)
            p.statAdvance(stats.mining, miner.points * 3.8 * xp.get(p, stats.mining))
            p.mes("Pete credits you with $earned Volcanic Mine points.")
        }
        VarPlayerIntMapSetter.set(p, VolcanicBits.current, 0)
        if (VolcanicUi.hud in p.ui) p.ifCloseOverlay(VolcanicUi.hud, events)
        if (outside) move(p, entrance)
        if (v.people.isEmpty()) {
            for (b in v.boulders) b.loc?.let { locs.del(it, Int.MAX_VALUE) }
            for (tile in v.hardened.keys) locs
                .findExact(tile, LocShape.CentrepieceStraight)
                ?.takeIf { it.id in 30998..31000 }
                ?.let { locs.del(it, Int.MAX_VALUE) }
            for (loc in v.scenery.distinctBy { it.coords }) locs
                .findExact(loc.coords, loc.shape)
                ?.takeIf { it.id == 31043 }
                ?.let { locs.del(it, Int.MAX_VALUE) }
            for (n in v.beasts) if (n.isSlotAssigned) npcs.del(n, Int.MAX_VALUE)
            cons.del(v.timer)
            round = null
        }
    }
}
