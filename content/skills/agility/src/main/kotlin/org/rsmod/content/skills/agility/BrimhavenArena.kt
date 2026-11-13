package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.abs
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ArenaBits : VarBitReferences() {
    val entry = find("agilityarena_canenter")
    val tagged = find("agilityarena_taggedlastpillar")
    val waiting = find("agilityarena_taggednoticket")
    val available = find("agilityarena_ticketavailable")
}

internal object ArenaInterfaces : InterfaceReferences() {
    val hud = find("agilityarena_overlay")
}

internal object ArenaTimers : TimerReferences() {
    val course = find("brimhaven_arena")
}

internal object ArenaNpcs : NpcReferences() {
    val clerk = find("agilityarena_clerk")
    val traders =
        listOf(
                "agilityarena_tickettrader",
                "agilityarena_tickettrader_2ops",
                "agilityarena_tickettrader_3ops",
            )
            .map { find(it) }
}

internal object ArenaObjs : ObjReferences() {
    val gloves = listOf("med", "hard", "elite").map { find("atjun_gloves_$it") }
    val ticket = find("agilityarena_ticket_new")
    val legacyTicket = find("agilityarena_ticket")
    val voucher = find("agilityarena_voucher")
    val graceful =
        listOf("hood", "cape", "top", "legs", "gloves", "boots").associate {
            find("graceful_$it") to find("graceful_${it}_skillcapecolour")
        }
    val products =
        listOf("cert_toadflax", "cert_snapdragon", "piratehook", "pack_amylase").map { find(it) }
}

internal object ArenaMoves : SeqReferences() {
    val bars = find("human_monkeybars_walk")
    val step = find("human_steppingstonejump")
    val jump = find("human_jump_hurdle")
    val dodge = find("human_dodge_rf_a")
}

internal enum class ArenaObstacle(
    val level: Int,
    val xp: Double,
    val ticks: Int,
    val move: SeqType,
    vararg val names: String,
) {
    Ledge(1, 16.0, 9, GnomeSequences.balance, "ledgebalance", "ledgebalance2"),
    Pillars(1, 18.0, 9, ArenaMoves.step, "pillar_top"),
    Bars(1, 14.0, 13, ArenaMoves.bars, "monkeybars_end"),
    Handholds(20, 22.0, 10, GnomeSequences.climb, "handholds"),
    Rope(1, 10.0, 9, GnomeSequences.balance, "ropebalance"),
    Log(1, 12.0, 9, GnomeSequences.balance, "logbalance1", "logbalance2", "logbalance3"),
    Planks(1, 6.0, 9, GnomeSequences.balance, "plank", "plank2", "plank3"),
    Wall(1, 8.0, 5, GnomeSequences.climb, "lowwall"),
    Swing(1, 20.0, 4, GnomeSequences.swing, "ropeswing"),
    Spikes(20, 24.0, 4, ArenaMoves.jump),
    Blade(1, 0.0, 5, ArenaMoves.jump),
    Saw(40, 28.0, 5, ArenaMoves.jump),
    Pad(20, 26.0, 4, ArenaMoves.jump),
    Darts(40, 30.0, 10, ArenaMoves.dodge),
}

internal object ArenaLocs : LocReferences() {
    val planks = listOf(find("agilityarena_plank_middle"), find("agilityarena_plank2_middle"))
    val damaged = find("agilityarena_plank_broke1")
    val enter = find("agilityarena_ladderdown")
    val exit = find("agilityarena_ladderup")
    val rescue = find("agilityarena_climbingrope")
    val dispensers = listOf(find("agility_ticketpillar"), find("agilityarena_poisondarts"))
    val obstacles =
        ArenaObstacle.entries.associateWith { kind -> kind.names.map { find("agilityarena_$it") } }
}

private data class ArenaBridge(val a: CoordGrid, val b: CoordGrid, val obstacle: ArenaObstacle) {
    private val east = a.x != b.x
    val mid = CoordGrid((a.x + b.x) / 2, (a.z + b.z) / 2, 3)

    fun start(tile: CoordGrid): CoordGrid =
        if (tile.chebyshevDistance(a) <= tile.chebyshevDistance(b)) a else b

    fun landing(from: CoordGrid): CoordGrid {
        val far = if (from == a) b else a
        return far.translate((from.x - far.x).compareTo(0) * 2, (from.z - far.z).compareTo(0) * 2)
    }

    fun crossed(from: CoordGrid, to: CoordGrid): Boolean {
        if (
            from.level != 3 ||
                to.level != 3 ||
                from.chebyshevDistance(to) !in 1..2 ||
                to.chebyshevDistance(mid) > 3
        )
            return false
        val perpendicular = if (east) abs(to.z - a.z) else abs(to.x - a.x)
        val start = if (east) from.x else from.z
        val end = if (east) to.x else to.z
        val centre = if (east) mid.x else mid.z
        return perpendicular <= 1 &&
            ((start < centre && end >= centre) || (start > centre + 1 && end <= centre + 1))
    }
}

private fun pillar(x: Int, z: Int): CoordGrid = CoordGrid(2761 + x * 11, 9546 + z * 11, 3)

private val bridges: List<ArenaBridge> = buildList {
    val east =
        listOf(
            listOf(
                ArenaObstacle.Ledge,
                ArenaObstacle.Bars,
                ArenaObstacle.Handholds,
                ArenaObstacle.Ledge,
            ),
            listOf(ArenaObstacle.Planks, ArenaObstacle.Saw, ArenaObstacle.Darts, ArenaObstacle.Pad),
            listOf(
                ArenaObstacle.Swing,
                ArenaObstacle.Darts,
                ArenaObstacle.Pillars,
                ArenaObstacle.Spikes,
            ),
            listOf(ArenaObstacle.Log, ArenaObstacle.Saw, ArenaObstacle.Blade, ArenaObstacle.Pad),
            listOf(
                ArenaObstacle.Ledge,
                ArenaObstacle.Wall,
                ArenaObstacle.Handholds,
                ArenaObstacle.Planks,
            ),
        )
    val north =
        listOf(
            listOf(
                ArenaObstacle.Pillars,
                ArenaObstacle.Spikes,
                ArenaObstacle.Blade,
                ArenaObstacle.Rope,
                ArenaObstacle.Log,
            ),
            listOf(
                ArenaObstacle.Handholds,
                ArenaObstacle.Rope,
                ArenaObstacle.Wall,
                ArenaObstacle.Bars,
                ArenaObstacle.Wall,
            ),
            listOf(
                ArenaObstacle.Spikes,
                ArenaObstacle.Bars,
                ArenaObstacle.Saw,
                ArenaObstacle.Darts,
                ArenaObstacle.Pillars,
            ),
            listOf(
                ArenaObstacle.Blade,
                ArenaObstacle.Pad,
                ArenaObstacle.Rope,
                ArenaObstacle.Log,
                ArenaObstacle.Swing,
            ),
        )
    for ((z, row) in east.withIndex()) for ((x, kind) in row.withIndex()) add(
        ArenaBridge(pillar(x, z), pillar(x + 1, z), kind)
    )
    for ((z, row) in north.withIndex()) for ((x, kind) in row.withIndex()) add(
        ArenaBridge(pillar(x, z), pillar(x, z + 1), kind)
    )
}

@Singleton
public class ArenaDispensers
@Inject
constructor(
    private val clock: MapClock,
    private val random: GameRandom,
    private val locations: LocRepository,
) {
    private val pillars = (0..4).flatMap { x -> (0..4).map { z -> pillar(x, z) } }.dropLast(1)
    private var round = -1
    private var index = -1
    private val safeLanes = mutableMapOf(9557 to 9557, 9590 to 9590)

    internal fun isSafePlank(centre: Int, lane: Int): Boolean {
        target()
        return safeLanes[centre] == lane
    }

    private fun rotatePlanks() {
        for ((centre, startX) in listOf(9557 to 2765, 9590 to 2798)) {
            val previous = safeLanes.getValue(centre)
            val next =
                listOf(centre - 1, centre, centre + 1).filter { it != previous }[random.of(2)]
            safeLanes[centre] = next
            for (x in startX..startX + 3) for (z in centre - 1..centre + 1) {
                val segment = locations.findExact(CoordGrid(x, z, 3), LocShape[22]) ?: continue
                val type = if (z == next) ArenaLocs.planks[(x - startX) % 2] else ArenaLocs.damaged
                if (segment.id != type.id) locations.change(segment, type, Int.MAX_VALUE)
            }
        }
    }

    internal fun target(): CoordGrid {
        val current = clock.cycle / 100
        if (round != current) {
            val next = random.of(if (index < 0) pillars.size else pillars.size - 1)
            index = if (index >= 0 && next >= index) next + 1 else next
            rotatePlanks()
            round = current
        }
        return pillars[index]
    }
}

public class BrimhavenArena
@Inject
constructor(private val dispensers: ArenaDispensers, private val ground: ObjRepository) :
    PluginScript() {
    private data class Runner(
        var previous: CoordGrid,
        var shown: Int = -1,
        var lastTag: Int = -2,
        var pads: Int = 0,
        var padExpiry: Int = 0,
    )

    private val runners = WeakHashMap<Player, Runner>()

    override fun ScriptContext.startup() {
        onOpNpc1(ArenaNpcs.clerk) {
            if (statBase(stats.agility)>=99 && choice2("Ask about the Agility cape.",true,"Ask about the arena.",false)) {
                org.rsmod.content.skills.skillcapes.SkillcapeSale.offer(this,stats.agility,objs.agility_hood)
                return@onOpNpc1
            }
            if (choice2("Pay 200 coins to enter the Agility Arena?", true, "No thanks.", false))
                pay()
        }
        onOpNpc3(ArenaNpcs.clerk) { pay() }
        onOpLoc1(ArenaLocs.enter) {
            if (!player.members) return@onOpLoc1
            if (vars[ArenaBits.entry] == 0) {
                mes("Pay Cap'n Izzy No-Beard 200 coins to enter the arena.")
                return@onOpLoc1
            }
            vars[ArenaBits.entry] = 0
            telejump(pillar(4, 4).translateZ(-1))
            startRun()
        }
        onOpLoc1(ArenaLocs.exit) {
            clearRun()
            telejump(CoordGrid(2809, 3193))
        }
        onOpLoc1(ArenaLocs.rescue) {
            if (!inside(coords) || coords.level != 0) return@onOpLoc1
            crossObstacle(CoordGrid(it.loc.x, it.loc.z, 3), GnomeSequences.climb, 4)
        }
        for (type in ArenaLocs.dispensers) onOpLoc1(type) { tag(it.loc) }
        for ((kind, types) in ArenaLocs.obstacles) for (type in types) {
            onOpLoc1(type) { cross(it.loc, kind) }
            onApLoc1(type) {
                if (isWithinApRange(it.loc, if (kind == ArenaObstacle.Swing) 4 else 1))
                    cross(it.loc, kind)
            }
        }
        onPlayerTimer(ArenaTimers.course) {
            if (!inside(coords)) {
                clearRun()
                return@onPlayerTimer
            }
            val runner = runners[player] ?: return@onPlayerTimer
            showTarget(runner)
            val bridge =
                bridges.firstOrNull {
                    it.obstacle.names.isEmpty() && it.crossed(runner.previous, coords)
                }
            if (bridge != null) traverse(bridge, bridge.start(runner.previous), null, runner)
            runner.previous = coords
            timer(ArenaTimers.course, 1)
        }
        onPlayerLogin {
            if (inside(player.coords)) {
                runners[player] = Runner(player.coords)
                player.timer(ArenaTimers.course, 1)
            }
        }
        onPlayerLogout { runners.remove(player) }
        for (trader in ArenaNpcs.traders) {
            onOpNpc3(trader) { store() }
            onOpNpcU(trader, ArenaObjs.ticket) {
                redeem(countDialog("How many tickets would you like to exchange?"))
            }
            onOpNpcU(trader, ArenaObjs.legacyTicket) {
                val count =
                    countDialog("Convert how many old tickets into vouchers?")
                        .coerceIn(0, invTotal(inv, ArenaObjs.legacyTicket))
                if (count > 0)
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, ArenaObjs.legacyTicket.id, count)
                        add(bag, ArenaObjs.voucher.id, count)
                    }
            }
        }
    }

    private fun ProtectedAccess.pay() {
        if (!player.members) return
        if (vars[ArenaBits.entry] != 0) {
            mes("You have already paid the entrance fee.")
            return
        }
        if (invDel(inv, objs.coins, 200).failure) {
            mes("You need 200 coins to enter the Agility Arena.")
            return
        }
        vars[ArenaBits.entry] = 1
        mes("You pay the entrance fee. Climb down the ladder to enter the arena.")
    }

    private fun inside(tile: CoordGrid): Boolean =
        tile.x in 2753..2812 && tile.z in 9540..9596 && tile.level in listOf(0, 3)

    private fun ProtectedAccess.startRun() {
        val run = Runner(coords)
        runners[player] = run
        showTarget(run)
        timer(ArenaTimers.course, 1)
    }

    private fun ProtectedAccess.clearRun() {
        runners.remove(player)
        player.clearTimer(ArenaTimers.course)
        vars[ArenaBits.tagged] = 0
        vars[ArenaBits.waiting] = 0
        vars[ArenaBits.available] = 0
        ifCloseSub(ArenaInterfaces.hud)
        player.client.write(HintArrow(HintArrow.ResetHintArrow))
    }

    private fun ProtectedAccess.showTarget(run: Runner) {
        val round = mapClock / 100
        if (run.shown == round) return
        if (run.shown < 0) ifOpenOverlay(ArenaInterfaces.hud)
        val target = dispensers.target()
        run.shown = round
        vars[ArenaBits.tagged] = if (run.lastTag == round - 1) 1 else 0
        vars[ArenaBits.available] = if (run.lastTag == round - 1) 1 else 0
        vars[ArenaBits.waiting] = 0
        player.client.write(
            HintArrow(
                HintArrow.TileHintArrow(
                    target.x,
                    target.z,
                    90,
                    HintArrow.TileHintArrow.HintArrowTilePosition.CENTER,
                )
            )
        )
    }

    private fun ProtectedAccess.tag(loc: BoundLocInfo) {
        val run = runners[player] ?: return
        if (loc.coords != dispensers.target() || coords.level != 3) {
            mes("This dispenser is not active.")
            return
        }
        val round = mapClock / 100
        if (run.lastTag == round) return
        val award = run.lastTag == round - 1
        run.lastTag = round
        val xp = (stat(stats.agility) / 10).coerceAtMost(10) * 30.0
        if (xp > 0) statAdvance(stats.agility, xp)
        vars[ArenaBits.tagged] = 1
        vars[ArenaBits.waiting] = if (award) 0 else 1
        if (!award) {
            mes("You tag the dispenser. Tag the next active one to claim a ticket.")
            return
        }
        val elite = DiaryRegion.Karamja.tasks(DiaryDifficulty.Elite).all { it.isComplete(player) }
        val amount = if (elite && random.of(10) == 0) 2 else 1
        for (item in listOf(ArenaObjs.ticket, ArenaObjs.voucher)) if (
            invAdd(inv, item, amount).failure
        )
            ground.add(InvObj(item, amount), coords, 300, player)
        publish(AgilityArenaTicket(player))
        player.completeDiaryTask(DiaryTask.ClaimBrimhavenArenaTicket)
        mes("The dispenser awards you a ticket and a Brimhaven voucher.")
    }

    private suspend fun ProtectedAccess.cross(loc: BoundLocInfo, kind: ArenaObstacle) {
        val run = runners[player] ?: return
        val bridge =
            bridges
                .filter { it.obstacle == kind }
                .minByOrNull { it.mid.chebyshevDistance(loc.coords) } ?: return
        if (bridge.mid.chebyshevDistance(loc.coords) > 5 || coords.level != 3) return
        val from = bridge.start(coords)
        traverse(bridge, from, loc, run)
        run.previous = coords
    }

    private suspend fun ProtectedAccess.traverse(
        bridge: ArenaBridge,
        from: CoordGrid,
        loc: BoundLocInfo?,
        run: Runner,
    ) {
        val kind = bridge.obstacle
        val level = stat(stats.agility)
        if (level < kind.level) {
            if (loc == null) telejump(bridge.landing(if (from == bridge.a) bridge.b else bridge.a))
            mes("You need an Agility level of ${kind.level} to cross this obstacle.")
            return
        }
        val badPlank =
            kind == ArenaObstacle.Planks && (loc == null || !dispensers.isSafePlank(from.z, loc.z))
        val failed =
            if (kind == ArenaObstacle.Planks) badPlank && level < 99
            else random.of(100) < (99 - level) * (kind.level + 25) / 160
        if (failed) {
            val falls = kind.names.isNotEmpty() && kind != ArenaObstacle.Wall
            val tile =
                if (falls) CoordGrid(bridge.mid.x, bridge.mid.z, 0)
                else bridge.landing(if (from == bridge.a) bridge.b else bridge.a)
            crossObstacle(tile, kind.move, kind.ticks / 2)
            takeInstantHit(HitType.Typeless, (stat(stats.hitpoints) / 10 + 1).coerceIn(2, 10))
            if (kind == ArenaObstacle.Darts) statSub(stats.agility, 2, 0)
            mes("You fail to negotiate the obstacle.")
            return
        }
        crossObstacle(bridge.landing(from), kind.move, kind.ticks)
        if (kind == ArenaObstacle.Pad) {
            if (mapClock >= run.padExpiry) run.pads = 0
            if (run.pads >= 2) return
            run.pads++
            run.padExpiry = mapClock + 8
        }
        if (kind.xp > 0) statAdvance(stats.agility, kind.xp * gloveBonus())
    }

    private fun ProtectedAccess.gloveBonus(): Double =
        if (ArenaObjs.gloves.any { it in worn }) 1.1 else 1.0

    private fun ProtectedAccess.redeem(request: Int) {
        val count = request.coerceIn(0, invTotal(inv, ArenaObjs.ticket))
        if (count == 0) {
            mes("You need Agility arena tickets to exchange for experience.")
            return
        }
        if (invDel(inv, ArenaObjs.ticket, count).success)
            statAdvance(stats.agility, count * 345.0 * gloveBonus())
    }

    private suspend fun ProtectedAccess.store() {
        if (!player.members) return
        val buy =
            menu(
                "Agility Arena Store",
                true,
                listOf(
                    "Agility experience (1 ticket)",
                    "Toadflax (3 vouchers)",
                    "Snapdragon (10 vouchers)",
                    "Pirate's hook (800 vouchers)",
                    "Amylase pack (60 vouchers)",
                    "Recolour a graceful outfit (250 vouchers)",
                ),
            )
        if (buy == 0) {
            redeem(countDialog("How many tickets would you like to exchange?"))
            return
        }
        if (buy == 5) {
            val recolour =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, ArenaObjs.voucher.id, 250)
                    for ((plain, coloured) in ArenaObjs.graceful) {
                        delete(bag, plain.id, 1)
                        add(bag, coloured.id, 1)
                    }
                }
            if (recolour.failure)
                mes("Bring 250 vouchers and a complete uncoloured graceful outfit.")
            return
        }
        val idx = buy - 1
        val product = ArenaObjs.products.getOrNull(idx) ?: return
        val cost = listOf(3, 10, 800, 60)[idx]
        val purchase =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, ArenaObjs.voucher.id, cost)
                add(bag, product.id, 1)
            }
        if (purchase.failure) mes("You need $cost vouchers and enough inventory space.")
    }
}
