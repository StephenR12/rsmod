package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsAlignment
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsGod
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SepulchreLocs : LocReferences() {
    val entrance = find("sepulchre_ow_entrance")
    val surface = find("sepulchre_lobby_exit_3x3")
    val start = find("hallowed_minigame_start")
    val retreat = find("hallowed_stairs_return")
    val obelisk = find("hallowed_exit")
    val stairs = (1..4).map { find("hallowed_stairs_floor$it") }
    val drops = SepulchreFloor.entries.flatMap { it.routes }.associateWith { find(it.drop) }
    val lastDrop = find("hallowed_floor_5_drop_2")
    val stones = find("hallowed_floor_2_steppingstone")
    val finish =
        listOf(
                "path_end_jumpover_01_north",
                "path_end_jumpover_01_east",
                "path_end_jumpover_01_south",
                "path_end_jumpover_01_west",
                "path_end_jumpover_02",
                "path_end_gate",
                "floor_5_steppingstone",
            )
            .map { find("hallowed_$it") }
}

internal object SepulchreBits : VarBitReferences() {
    val usePrivate = find("hallowed_private_instances")
    val ownPrivate = find("hallowed_private_instances_purchased")
    val quest = find("myq5")
    val remaining = find("hallowed_time_remaining")
    val elapsed = find("hallowed_time_spent")
    val floor = find("hallowed_current_floor")
    val floorTime = find("hallowed_current_floor_time_spent")
    val best = (1..5).map { find("hallowed_time_pb_f$it") }
    val overall = find("hallowed_time_pb_all")
    val intro = find("hallowed_tutorial")
    val freeShards = find("hallowed_teleports_received")
}

internal object SepulchreTimer : TimerReferences() {
    val run = find("hallowed_course_tick")
}

internal object SepulchreUI : InterfaceReferences() {
    val overlay = find("hallowed_overlay")
}

internal object SepulchreItems : ObjReferences() {
    val shard = find("hallowed_teleport")
    val token = find("hallowed_token")
    val marks = find("hallowed_mark")
    val ring = find("hallowed_ring")
}

internal object SepulchreMoves : SeqReferences() {
    val crystal = find("hallowed_teleport_activate")
}

public class HallowedSepulchre
@Inject
constructor(
    private val regions: org.rsmod.api.repo.region.RegionRepository,
    private val treasure: SepulchreTreasure,
    private val traps: SepulchreTraps,
    private val registry: PlayerRegistry,
    private val alignment: PrayerGodWarsAlignment,
    private val types: ObjTypeList,
) : PluginScript() {
    private val lobby = CoordGrid(2400, 5970)

    private data class Run(
        val floor: SepulchreFloor,
        val route: SepulchreRoute,
        val space: SepulchreSpace,
        val started: Int,
        val carried: Int,
        var limit: Int,
        var stage: Int = 0,
        var finished: Int? = null,
        var usedToken: Boolean = false,
        val footing: SepulchreFooting = SepulchreFooting(space.world(route.start)),
        val treasure: TreasureProgress = TreasureProgress(),
    ) {
        fun elapsed(now: Int) = ((finished ?: now) - started).coerceAtLeast(0)

        fun remaining(now: Int) = (limit - elapsed(now)).coerceAtLeast(0)
    }

    private val runs = WeakHashMap<Player, Run>()

    override fun ScriptContext.startup() {
        for ((kind, types) in
            listOf(
                SepulchreChallenge.Bridge to listOf(TreasureLocs.bridge, TreasureLocs.poolBridge),
                SepulchreChallenge.Prayer to listOf(TreasureLocs.barrier),
                SepulchreChallenge.Portal to listOf(TreasureLocs.portal),
                SepulchreChallenge.Grapple to listOf(TreasureLocs.pillar),
                SepulchreChallenge.Grand to listOf(TreasureLocs.gate),
            )) for (type in types) {
            val range = if (kind == SepulchreChallenge.Grapple) 6 else 2
            onApLoc1(type) { if (isWithinApRange(it.loc, range)) challenge(it.loc, kind) }
            onOpLoc1(type) { challenge(it.loc, kind) }
        }
        for (type in TreasureLocs.braziers) onOpLoc1(type) {
            if (timed() != null) treasure.pray(this)
        }
        for ((index, type) in TreasureLocs.coffins.withIndex()) onOpLoc1(type) {
            val run = timed() ?: return@onOpLoc1
            treasure.loot(
                this,
                it.loc.copy(coords = run.space.normal(it.loc.coords)),
                index,
                run.floor,
                run.treasure,
            )
        }
        onOpHeld1(TreasureItems.sack) { treasure.sack(this) }
        onOpLoc1(SepulchreLocs.entrance) { if (eligible()) telejump(lobby) }
        onOpLoc1(SepulchreLocs.surface) { telejump(CoordGrid(3654, 3384)) }
        onOpLoc1(SepulchreLocs.start) {
            if (!eligible()) return@onOpLoc1
            if (statBase(stats.agility) < 52) {
                mes("You need an Agility level of 52 to enter the Hallowed Sepulchre.")
                return@onOpLoc1
            }
            if (vars[SepulchreBits.intro] == 0) {
                mes("Speak to the Mysterious Stranger before entering the Sepulchre.")
                return@onOpLoc1
            }
            begin(SepulchreFloor.One, 0, 200, false)
        }
        onOpLoc1(SepulchreLocs.retreat) {
            if (choice2("Return to the lobby.", true, "Keep going.", false)) leave()
        }
        onOpLoc2(SepulchreLocs.retreat) { leave() }
        for ((route, type) in SepulchreLocs.drops) onOpLoc1(type) {
            val run = runs[player] ?: return@onOpLoc1
            if (run.route != route || run.stage != 0) return@onOpLoc1
            crossObstacle(run.space.world(route.landing), GnomeSequences.descend, 2)
            run.footing.safe = coords
            run.footing.previous = coords
            run.stage = 1
            statAdvance(stats.agility, 25.0)
        }
        onOpLoc1(SepulchreLocs.lastDrop) {
            val run = runs[player] ?: return@onOpLoc1
            if (run.floor != SepulchreFloor.Five || run.stage != 1) return@onOpLoc1
            crossObstacle(run.space.world(CoordGrid(2298, 5831, 0)), GnomeSequences.descend, 2)
            run.footing.safe = coords
            run.footing.previous = coords
            run.stage = 2
            statAdvance(stats.agility, 25.0)
        }
        onApLoc1(SepulchreLocs.stones) { if (isWithinApRange(it.loc, 2)) stones(it.loc) }
        onOpLoc1(SepulchreLocs.stones) { stones(it.loc) }
        for (type in SepulchreLocs.finish) {
            onApLoc1(type) { if (isWithinApRange(it.loc, 2)) finish(it.loc) }
            onOpLoc1(type) { finish(it.loc) }
        }
        for ((i, type) in SepulchreLocs.stairs.withIndex()) onOpLoc1(type) {
            val run = runs[player] ?: return@onOpLoc1
            if (run.floor.ordinal != i || run.finished == null) return@onOpLoc1
            val next = SepulchreFloor.entries[i + 1]
            if (run.remaining(player.currentMapClock) <= 0) {
                mes("You're out of time. Use a magical obelisk to return to the lobby.")
                return@onOpLoc1
            }
            if (statBase(stats.agility) < next.requirement) {
                mes("You need an Agility level of ${next.requirement} to descend further.")
                return@onOpLoc1
            }
            begin(
                next,
                run.carried + run.elapsed(player.currentMapClock),
                run.remaining(player.currentMapClock) + 200,
                run.usedToken,
            )
        }
        onOpLoc1(SepulchreLocs.obelisk) { if (runs[player]?.finished != null) activate() }
        onOpLoc2(SepulchreLocs.obelisk) {
            if (choice2("Return to the lobby.", true, "Stay here.", false)) exitAtObelisk()
        }
        onOpLoc3(SepulchreLocs.obelisk) { exitAtObelisk() }
        onOpHeld1(SepulchreItems.token) {
            val run = runs[player]
            if (run == null || run.remaining(player.currentMapClock) == 0) {
                mes("You can only use that token during a timed Sepulchre run.")
                return@onOpHeld1
            }
            if (invDel(inv, SepulchreItems.token).success) {
                run.limit = (run.limit + 100).coerceAtMost(8191)
                run.usedToken = true
                updateTimer(run)
                runClientScript(
                    if (run.finished == null) 3442 else 3443,
                    run.remaining(player.currentMapClock),
                )
            }
        }
        onOpHeld1(SepulchreItems.shard) {
            val base = if (coords.z >= 9920) 9920 else 3520
            if (!eligible()) return@onOpHeld1
            if (
                player.isTeleportBlocked() ||
                    (vars[varbits.inside_wilderness] != 0 && (coords.z - base) / 8 + 1 > 20)
            ) {
                mes("A magical force stops you from teleporting.")
                return@onOpHeld1
            }
            if (invDel(inv, SepulchreItems.shard).failure) return@onOpHeld1
            anim(SepulchreMoves.crystal)
            delay(3)
            clear()
            teleport(lobby)
            publish(PlayerTeleportEvent.Start(player, lobby))
        }
        onPlayerTimer(SepulchreTimer.run) {
            val run = runs[player]
            if (run == null || !run.floor.contains(run.space.normal(coords))) {
                clear()
                return@onPlayerTimer
            }
            updateTimer(run)
            if (run.finished == null)
                traps.process(
                    this,
                    run.footing,
                    run.space,
                    if (run.space.region == null) 0 else run.started,
                )
        }
        onPlayerLogout {
            val run = runs.remove(player) ?: return@onPlayerLogout
            player.clearTimer(SepulchreTimer.run)
            for (bit in
                listOf(
                    SepulchreBits.remaining,
                    SepulchreBits.elapsed,
                    SepulchreBits.floorTime,
                    SepulchreBits.floor,
                )) org.rsmod.api.player.vars.VarPlayerIntMapSetter.set(player, bit, 0)
            run.space.region?.let(traps::remove)
            if (run.floor.contains(run.space.normal(player.coords)))
                registry.relocateOnLogout(player, lobby)
        }
    }

    private fun ProtectedAccess.timed(): Run? =
        runs[player]?.takeIf { it.remaining(player.currentMapClock) > 0 }

    private suspend fun ProtectedAccess.challenge(loc: BoundLocInfo, kind: SepulchreChallenge) {
        val run = runs[player] ?: return
        treasure.cross(
            this,
            loc.copy(coords = run.space.normal(loc.coords)),
            kind,
            run.floor,
            run.treasure,
            run.space,
            run.remaining(player.currentMapClock) > 0,
        )
    }

    private fun ProtectedAccess.eligible(): Boolean {
        if (player.members && vars[SepulchreBits.quest] >= 138) return true
        mes("You must complete Sins of the Father to enter the Hallowed Sepulchre.")
        return false
    }

    private fun ProtectedAccess.begin(
        floor: SepulchreFloor,
        carried: Int,
        limit: Int,
        token: Boolean,
    ) {
        val route = floor.routes[random.of(floor.routes.size)]
        val space =
            if (vars[SepulchreBits.usePrivate] != 0 && vars[SepulchreBits.ownPrivate] != 0) {
                SepulchreSpace.privateFloor(floor, regions, player.currentMapClock)
                    ?: run {
                        mes("There is no private chamber available just now.")
                        return
                    }
            } else SepulchreSpace()
        val old = runs[player]
        if (old != null) {
            if (floor != SepulchreFloor.One) award(old)
            old.space.region?.let(traps::remove)
        }
        val run =
            Run(floor, route, space, player.currentMapClock, carried, limit, usedToken = token)
        runs[player] = run
        treasure.reset(this)
        vars[SepulchreBits.floor] = floor.ordinal + 1
        telejump(space.world(route.start))
        ifOpenOverlay(SepulchreUI.overlay)
        runClientScript(3442, limit)
        updateTimer(run)
        timer(SepulchreTimer.run, 1)
    }

    private fun ProtectedAccess.updateTimer(run: Run) {
        val now = player.currentMapClock
        vars[SepulchreBits.remaining] = run.remaining(now).coerceAtMost(8191)
        vars[SepulchreBits.elapsed] = (run.carried + run.elapsed(now)).coerceAtMost(8191)
        vars[SepulchreBits.floorTime] = run.elapsed(now).coerceAtMost(8191)
    }

    private suspend fun ProtectedAccess.stones(worldLoc: BoundLocInfo) {
        val run = runs[player] ?: return
        val loc = worldLoc.copy(coords = run.space.normal(worldLoc.coords))
        val here = run.space.normal(coords)
        if (run.floor != SepulchreFloor.Two || here.x != 2518 || loc.x != 2518) return
        val endZ = if (here.z < loc.z) loc.z + 2 else loc.z - 2
        if (endZ !in 5999..6009) return
        crossObstacle(run.space.world(CoordGrid(2518, endZ, 2)), ArenaMoves.step, 2)
    }

    private suspend fun ProtectedAccess.finish(worldLoc: BoundLocInfo) {
        val run = runs[player] ?: return
        if (run.finished != null || run.stage != (if (run.floor == SepulchreFloor.Five) 2 else 1))
            return
        val loc = worldLoc.copy(coords = run.space.normal(worldLoc.coords))
        val center = run.floor.center
        val landing =
            when (run.floor) {
                SepulchreFloor.One,
                SepulchreFloor.Two ->
                    when {
                        loc.x < center.x -> loc.coords.translate(2, 0)
                        loc.x > center.x -> loc.coords.translate(-2, 0)
                        loc.z < center.z -> loc.coords.translate(0, 2)
                        else -> loc.coords.translate(0, -2)
                    }
                SepulchreFloor.Three -> loc.coords.translate(if (loc.x < center.x) 2 else -2, 0)
                SepulchreFloor.Four -> CoordGrid(2528, 5846, 1)
                SepulchreFloor.Five -> CoordGrid(2272, 5860, 0)
            }
        crossObstacle(run.space.world(landing), ArenaMoves.step, 2)
        run.finished = player.currentMapClock
        updateTimer(run)
        runClientScript(3443, run.remaining(player.currentMapClock))
    }

    private fun ProtectedAccess.award(run: Run) {
        statAdvance(stats.agility, run.floor.completionXp.toDouble())
        val elapsed = run.elapsed(player.currentMapClock)
        val best = SepulchreBits.best[run.floor.ordinal]
        if (!run.usedToken && elapsed in 1..1023 && (vars[best] == 0 || elapsed < vars[best]))
            vars[best] = elapsed
        if (run.floor == SepulchreFloor.Five && !run.usedToken) {
            val total = elapsed + run.carried
            if (
                total in 1..8191 &&
                    (vars[SepulchreBits.overall] == 0 || total < vars[SepulchreBits.overall])
            )
                vars[SepulchreBits.overall] = total
        }
    }

    private fun ProtectedAccess.activate() {
        fun aligned(god: PrayerGodWarsGod) = alignment.count(player, god) > 0
        when {
            aligned(PrayerGodWarsGod.Zamorak) -> {
                player.runEnergy = 0
                takeInstantHit(HitType.Typeless, 3)
            }
            aligned(PrayerGodWarsGod.Armadyl) ||
                aligned(PrayerGodWarsGod.Bandos) ||
                player.worn
                    .filterNotNull { types[it].paramMap?.getOrNull(params.guthixitem) == 1 }
                    .isNotEmpty() -> {
                player.runEnergy = (player.runEnergy - 2500).coerceAtLeast(0)
                takeInstantHit(HitType.Typeless, 1)
            }
            aligned(PrayerGodWarsGod.Saradomin) -> player.runEnergy = 10000
            else -> mes("The obelisk does not respond.")
        }
    }

    private fun ProtectedAccess.exitAtObelisk() {
        val run = runs[player] ?: return
        if (run.finished == null) return
        award(run)
        publish(SepulchreExit(player, run.floor))
        activate()
        leave()
    }

    private fun ProtectedAccess.leave() {
        clear()
        telejump(lobby)
    }

    private fun ProtectedAccess.clear() {
        runs.remove(player)?.space?.region?.let(traps::remove)
        player.clearTimer(SepulchreTimer.run)
        vars[SepulchreBits.remaining] = 0
        vars[SepulchreBits.elapsed] = 0
        vars[SepulchreBits.floorTime] = 0
        vars[SepulchreBits.floor] = 0
        ifCloseSub(SepulchreUI.overlay)
    }
}
