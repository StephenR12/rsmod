package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.clue.ClueVessels
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.mining.camdozaal.Camdozaal
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MineralAccess : VarBitReferences() {
    val fossil = find("fossilquest_progress")
    val camTorum = find("pmoon_quest")
}

internal object MineralClock : ControllerBuilder() {
    val rocks = build("calcified_rocks")
}

/**
 * The timers belong to the rocks in this world. Every miner sees the same exhaustion and seepage.
 */
@Singleton
public class SpecialistMinerals
@Inject
constructor(
    private val clock: MapClock,
    private val locs: LocRepository,
    private val types: LocTypeList,
    private val controllers: ControllerRepository,
    private val players: PlayerRepository,
    private val ground: ObjRepository,
    private val rng: GameRandom,
    private val xp: XpModifiers,
    private val vessels: ClueVessels,
    private val pets: SkillPetRates,
) {
    private val exhaustedAt = mutableMapOf<CoordGrid, Int>()
    private val wet = mutableMapOf<CoordGrid, LocInfo>()
    private val hopping = WeakHashMap<Player, Boolean>()
    private val centre = CoordGrid(1508, 9541, 1)
    private var active = false
    private var cycle = 0
    private var dryAt = 0

    private fun inMine(p: Player) = p.level == 1 && p.x in 1493..1525 && p.z in 9532..9550

    private fun barronite(id: Int) = id in 41547..41548

    private fun calcified(id: Int) = id in 51485..51491 && id % 2 == 1

    public fun supports(id: Int) = id == MiningLocs.fossilAsh.id || calcified(id) || barronite(id)

    public fun canMine(p: Player, id: Int): Boolean {
        if (!supports(id)) return true
        if (barronite(id)) return Camdozaal.allowed(p)
        return p.members &&
            (if (calcified(id)) p.vars[MineralAccess.camTorum] >= 5
            else p.vars[MineralAccess.fossil] >= 50)
    }

    public fun adjustBounds(p: Player, rock: BoundLocInfo, ordinary: IntRange): IntRange =
        when {
            barronite(rock.id) ->
                Camdozaal.boost(p, 3).let { ordinary.first * it / 100..ordinary.last * it / 100 }
            rock.coords in wet && p !in hopping ->
                ordinary.first * 115 / 100..ordinary.last * 115 / 100
            else -> ordinary
        }

    internal fun login(p: Player) {
        if (wet.isNotEmpty() && inMine(p)) {
            hopping[p] = true
            p.mes(
                "Your pickaxe keeps slipping on the rocks as it's still wet from the last deposit."
            )
        }
    }

    internal fun tick(con: Controller) {
        hopping.keys.removeIf { !inMine(it) }
        if (wet.isNotEmpty() && clock.cycle >= dryAt) {
            for ((pos, effect) in wet) {
                locs.del(effect, Int.MAX_VALUE)
                deplete(pos)
            }
            wet.clear()
        }
        for (pos in exhaustedAt.filterValues { it <= clock.cycle }.keys.toList()) deplete(pos)
        if (clock.cycle >= cycle) {
            hopping.clear()
            val available = buildList {
                for (x in 1497..1521) for (z in 9537..9546) {
                    val rock = locs.findExact(CoordGrid(x, z, 1), LocShape.CentrepieceStraight)
                    if (rock != null && calcified(rock.id)) add(rock)
                }
            }
            if (available.isNotEmpty()) {
                val focus = available[rng.of(available.size)]
                dryAt = clock + rng.of(33..50)
                for (rock in available) if (rock.coords.chebyshevDistance(focus.coords) <= 2) {
                    wet[rock.coords] =
                        locs.add(
                            rock.coords,
                            types.getValue(51493),
                            Int.MAX_VALUE,
                            rock.angle,
                            LocShape.WallDecorStraightNoOffset,
                        )
                    exhaustedAt[rock.coords] = dryAt
                }
                players.findAll(ZoneKey.from(centre), 3).filter(::inMine).forEach {
                    it.mes("Some cracks around the cave begin to ooze water.")
                }
            }
            cycle = clock + rng.of(100..200)
        }
        con.aiTimer(1)
    }

    private fun deplete(pos: CoordGrid) {
        exhaustedAt.remove(pos)
        val current = locs.findExact(pos, LocShape.CentrepieceStraight) ?: return
        if (calcified(current.id)) locs.change(current, types.getValue(current.id + 1), 50)
        else if (barronite(current.id)) locs.change(current, types.getValue(current.id + 2), 100)
    }

    public fun mine(a: ProtectedAccess, rock: BoundLocInfo): Boolean =
        with(a) {
            val ash = rock.id == MiningLocs.fossilAsh.id
            val barronite = barronite(rock.id)
            val product =
                if (barronite) {
                    if (random.of(100) < 24) MiningObjs.barroniteDeposit
                    else MiningObjs.barroniteShards
                } else if (ash) {
                    if (random.of(20) == 0) objs.soda_ash else MiningObjs.volcanicAsh
                } else if (random.of(75) == 0) MiningObjs.deposit else MiningObjs.shards
            val amount =
                when (product) {
                    MiningObjs.volcanicAsh -> (1 + (player.miningLvl - 22) / 15).coerceIn(1, 6)
                    MiningObjs.barroniteShards -> random.of(4..6)
                    MiningObjs.shards -> random.of(1..3)
                    else -> 1
                }
            val before = inv.indexOfFirst { it == null }
            if (!vessels.geodes(player, player.miningLvl, 741600)) invAdd(inv, product, amount)
            val baseXp =
                when (product) {
                    MiningObjs.barroniteShards -> 16.0
                    MiningObjs.barroniteDeposit -> 32.0
                    MiningObjs.deposit -> 36.0
                    MiningObjs.shards -> 33.0
                    else -> 10.0
                }
            statAdvance(stats.mining, baseXp * xp.get(player, stats.mining))
            pets.roll(
                player,
                stats.mining,
                13321,
                741600,
                before.takeIf { it >= 0 && inv[it]?.id == product.id } ?: -1,
            )
            if (ash) {
                val empty = random.of(4) == 0
                if (empty) locs.change(rock, MiningLocs.fossilEmpty, 50)
                return empty
            }
            if (!barronite && random.of(5000) == 0) invAddOrDrop(ground, MiningObjs.moonLoop)
            exhaustedAt.putIfAbsent(rock.coords, clock + (if (barronite) 33 else 117))
            if (!active) {
                active = true
                cycle = clock + rng.of(100..200)
                val con = Controller(MineralClock.rocks, centre)
                controllers.add(con, Int.MAX_VALUE)
                con.aiTimer(1)
            }
            return false
        }
}

public class CalcifiedDeposits
@Inject
constructor(private val minerals: SpecialistMinerals, private val locs: LocTypeList) :
    PluginScript() {
    private val exterior = CoordGrid(1435, 3129)
    private val interior = CoordGrid(1439, 9507, 1)

    override fun ScriptContext.startup() {
        onAiConTimer(MineralClock.rocks) { minerals.tick(controller) }
        onPlayerLogin { minerals.login(player) }
        onOpLoc1(locs.getValue(51375)) {
            when (it.loc.coords) {
                exterior ->
                    if (allowed()) telejump(interior.translate(0, 2))
                    else mes("You need Attala's permission to enter Cam Torum.")
                interior -> telejump(exterior.translate(1, -1))
            }
        }
        onOpHeld1(MiningObjs.moth) {
            val wildBase = if (coords.z >= 9920) 9920 else 3520
            if (
                !allowed() ||
                    player.isTeleportBlocked() ||
                    vars[varbits.inside_wilderness] != 0 && (coords.z - wildBase) / 8 + 1 > 20
            )
                return@onOpHeld1
            if (invDel(inv, MiningObjs.moth, 1, it.slot).success) {
                mes("You crush the moth, releasing the energy inside...")
                teleport(CoordGrid(1439, 9550, 1))
            }
        }
        for (id in anvils()) onOpLocU(locs.getValue(id), MiningObjs.deposit) { crush(it.loc) }
    }

    private fun ProtectedAccess.allowed() = player.members && vars[MineralAccess.camTorum] >= 5

    private fun anvils() =
        locs.values
            .filter { it.name.equals("Anvil", true) || it.name == "Rusted anvil" }
            .map { it.id }

    private suspend fun ProtectedAccess.crush(anvil: BoundLocInfo) {
        val hammers = setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
        while (player.members && MiningObjs.deposit in inv && isWithinDistance(anvil, 1)) {
            if (!inv.any { it?.id in hammers } && !player.worn.any { it?.id in hammers }) {
                mes("You need a hammer to crush the deposit.")
                return
            }
            val moth = random.of(4) == 0
            val product = if (moth) MiningObjs.moth else MiningObjs.shards
            val amount = if (moth) 1 else random.of(5..15)
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, MiningObjs.deposit.id, 1)
                    add(bag, product.id, amount)
                }
            if (!tx.success) return
            anim(seqs.human_hammer_hit)
            statAdvance(stats.smithing, 1.0)
            delay(3)
        }
    }
}
