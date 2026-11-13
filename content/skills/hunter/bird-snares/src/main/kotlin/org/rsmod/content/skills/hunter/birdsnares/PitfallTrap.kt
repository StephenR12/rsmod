package org.rsmod.content.skills.hunter.birdsnares

import org.rsmod.api.player.events.HunterCapture
import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.content.skills.crafting.hunterpouches.HunterPouchStorage
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid

/**
 * Pitfall mechanics with explicit approximate tease/catch/fur odds where dated sources omit them.
 */
public class PitfallTrap
@Inject
constructor(
    private val pouches: HunterPouchStorage,
    private val locs: LocRepository,
    private val locTypes: LocTypeList,
    private val npcs: NpcRepository,
    private val cons: ControllerRepository,
    private val players: PlayerList,
    private val limits: PortableHunterTraps,
    private val random: GameRandom,
) {
    private data class Trap(
        val con: Controller,
        val source: BoundLocInfo,
        val species: PitfallSpecies,
        val parts: List<LocInfo>,
        val current: MutableMap<LocInfo, LocInfo?>,
        var phase: Int = 1,
        var loot: List<InvObj> = emptyList(),
    )

    private data class Lure(
        val npc: Npc,
        val species: PitfallSpecies,
        val owner: Player,
        var lastPit: Int? = null,
        var nextAttack: Int = 0,
    )

    private val traps = mutableMapOf<Int, Trap>()
    private val lures = mutableMapOf<Npc, Lure>()

    init {
        limits.register(PitfallController.trap, PitfallVars.owner)
    }

    public suspend fun setup(
        access: ProtectedAccess,
        pit: BoundLocInfo,
        species: PitfallSpecies? = null,
    ) =
        with(access) {
            if (!player.members) return
            val previous = traps[pit.coords.packed]
            if (previous != null && (previous.con.owner != player.slotId || previous.phase != 3)) {
                mes("This pit is already in use.")
                return
            }
            val nativeIndex = PitfallLocs.sources.indexOfFirst { it.id == pit.id } + 1
            val nativeSpecies =
                when (nativeIndex) {
                    in 1..6 -> 2
                    in 7..11 -> 0
                    in 12..16 -> 1
                    in 17..21 -> 3
                    in 22..25 -> 4
                    else -> -1
                }
            val creature =
                previous?.species
                    ?: species
                    ?: PitfallSpeciesList.all.getOrNull(nativeSpecies)
                    ?: PitfallSpeciesList.all.firstOrNull { it.inArea(pit.coords) }
                    ?: return
            if (player.stat(stats.hunter) < creature.level) {
                mes("You need a Hunter level of ${creature.level} to set this trap.")
                return
            }
            if (objs.knife !in player.inv && objs.fletching_knife !in player.inv) {
                mes("You need a knife to build this trap.")
                return
            }
            val log =
                PitfallSpeciesList.logs.firstOrNull { it in player.inv }
                    ?: run {
                        mes("You need suitable logs to build this trap.")
                        return
                    }
            if (previous == null && limits.count(player) >= maximumTraps(player)) {
                mes("You cannot lay any more traps at your current Hunter level.")
                return
            }
            val origin = player.coords
            anim(BoxTrapSeqs.lay)
            delay(BUILD_TICKS)
            if (
                player.coords != origin ||
                    traps[pit.coords.packed] !== previous ||
                    (previous == null && limits.count(player) >= maximumTraps(player))
            )
                return
            val paid = player.invTransaction(player.inv) { delete(select(player.inv), log.id, 1) }
            if (!paid.success) return
            if (previous != null) {
                previous.loot = emptyList()
                previous.phase = 1
                render(previous, 1)
                previous.con.aiTimer(EXPIRY_TICKS)
                return
            }
            val con = Controller(PitfallController.trap, pit.coords)
            con.owner = player.slotId
            con.aiTimer(EXPIRY_TICKS)
            cons.add(con, Int.MAX_VALUE)
            val bit = locTypes[pit].multiVarBit
            val parts =
                if (bit < 0) listOf(LocInfo(pit.layer, pit.coords, pit.entity))
                else
                    buildList {
                            for (dx in -2..3) for (dz in -2..3) for (loc in
                                locs.findAll(CoordGrid(pit.x + dx, pit.z + dz, pit.level))) if (
                                locTypes.getValue(loc.id).multiVarBit == bit
                            )
                                add(loc)
                        }
                        .ifEmpty { listOf(LocInfo(pit.layer, pit.coords, pit.entity)) }
            val trap =
                Trap(
                    con,
                    pit,
                    creature,
                    parts,
                    parts.associateWith<LocInfo, LocInfo?> { it }.toMutableMap(),
                )
            traps[pit.coords.packed] = trap
            render(trap, 1)
        }

    public fun tease(access: ProtectedAccess, npc: Npc, species: PitfallSpecies) =
        with(access) {
            if (
                !player.members || player.stat(stats.hunter) < species.level || !npc.isValidTarget()
            )
                return
            val spear = player.righthand?.id == PitfallObjs.spear.id
            if (PitfallObjs.stick !in player.inv && !spear) {
                mes("You need a teasing stick or an equipped hunter's spear.")
                return
            }
            val existing = lures[npc]
            if (existing != null && existing.owner !== player) {
                mes("Someone else is luring that creature.")
                return
            }
            anim(PitfallSeqs.tease)
            faceEntitySquare(npc)
            // Exact tease odds are unpublished. Spear's sourced +5 points is preserved in this
            // model.
            if (random.of(100) >= if (spear) 100 else 95) {
                mes("The creature ignores your attempt to tease it.")
                return
            }
            lures[npc] = Lure(npc, species, player, existing?.lastPit, player.currentMapClock)
            npc.playerFollow(player)
            player.softTimer(PitfallTimers.lure, 1)
        }

    public suspend fun jump(access: ProtectedAccess, pit: BoundLocInfo) =
        with(access) {
            val trap = traps[pit.coords.packed] ?: return
            if (!player.members || trap.phase != 1 || trap.con.owner != player.slotId) return
            val source = trap.source
            val width = source.adjustedWidth.coerceAtLeast(2)
            val length = source.adjustedLength.coerceAtLeast(2)
            val x = player.coords.x
            val z = player.coords.z
            val destination =
                when {
                    x < source.x ->
                        CoordGrid(
                            source.x + width,
                            z.coerceIn(source.z, source.z + length - 1),
                            source.level,
                        )
                    x >= source.x + width ->
                        CoordGrid(
                            source.x - 1,
                            z.coerceIn(source.z, source.z + length - 1),
                            source.level,
                        )
                    z < source.z ->
                        CoordGrid(
                            x.coerceIn(source.x, source.x + width - 1),
                            source.z + length,
                            source.level,
                        )
                    else ->
                        CoordGrid(
                            x.coerceIn(source.x, source.x + width - 1),
                            source.z - 1,
                            source.level,
                        )
                }
            val lure =
                lures.values.firstOrNull {
                    it.owner === player &&
                        it.species == trap.species &&
                        it.npc.isValidTarget() &&
                        distance(it.npc.coords, source.coords) <= 6
                }
            anim(BoxTrapSeqs.check)
            delay(JUMP_TICKS)
            telejump(destination)
            if (
                lure == null ||
                    traps[pit.coords.packed] !== trap ||
                    trap.phase != 1 ||
                    lure.lastPit == pit.coords.packed ||
                    !lure.npc.isValidTarget()
            )
                return
            lure.lastPit = pit.coords.packed
            if (
                random.randomDouble() >=
                    approximateCatchChance(player.stat(stats.hunter), trap.species)
            ) {
                lure.npc.playerFollow(player)
                mes("The creature jumps over the pit.")
                return
            }
            trap.phase = 2
            render(trap, 2)
            lures.remove(lure.npc)
            npcs.despawn(lure.npc, lure.npc.type.respawnRate)
            trap.loot = rollLoot(player.stat(stats.hunter), trap.species)
            delay(COLLAPSE_TICKS)
            if (traps[pit.coords.packed] === trap) {
                trap.phase = 3
                render(trap, 3)
                trap.con.aiTimer(EXPIRY_TICKS)
            }
        }

    public fun dismantle(access: ProtectedAccess, pit: BoundLocInfo) =
        with(access) {
            val trap = traps[pit.coords.packed] ?: return
            if (trap.con.owner != player.slotId || trap.phase == 2) return
            if (trap.phase == 3) {
                val bones = trap.loot.first { it.id == objs.big_bones.id }
                val remains = RemainsDrop(player, bones)
                publish(remains)
                val delivered =
                    pouches.deliverCaught(
                        player,
                        trap.loot.mapNotNull { item ->
                            val count = if (item.id == bones.id) remains.remaining else item.count
                            if (count > 0) item.copy(count = count) else null
                        },
                    )
                if (!delivered) {
                    mes("You don't have enough inventory space to collect the trap.")
                    return
                }
                remains.commit()
                statAdvance(stats.hunter, trap.species.xp)
                publish(HunterCapture(player, trap.species.npc.internalNameValue))
                val task =
                    when (trap.species.index) {
                        0 -> DiaryTask.CatchSpinedLarupia
                        1 -> DiaryTask.CatchHornedGraahk
                        2 -> DiaryTask.CatchSabreKyatt
                        else -> null
                    }
                task?.let(player::completeDiaryTask)
            }
            remove(trap)
        }

    public fun tick(player: Player) {
        val active = lures.values.filter { it.owner === player }.toList()
        for (lure in active) {
            val npc = lure.npc
            if (
                !player.members ||
                    !npc.isValidTarget() ||
                    player.coords.level != npc.coords.level ||
                    distance(player.coords, npc.coords) > 12
            ) {
                npc.resetMode()
                lures.remove(npc)
                continue
            }
            if (
                distance(player.coords, npc.coords) <= 2 &&
                    player.currentMapClock >= lure.nextAttack
            ) {
                player.queueHit(npc, 1, HitType.Melee, random.of(0..lure.species.maxHit))
                lure.nextAttack = player.currentMapClock + 4
            }
        }
        if (lures.values.any { it.owner === player }) player.softTimer(PitfallTimers.lure, 1)
    }

    public fun expire(con: Controller) {
        traps[con.coords.packed]?.let { remove(it, deferred = true) }
    }

    public fun clear(player: Player) {
        traps.values.filter { it.con.owner == player.slotId }.toList().forEach { remove(it) }
        lures.values
            .filter { it.owner === player }
            .toList()
            .forEach {
                it.npc.resetMode()
                lures.remove(it.npc)
            }
        player.clearSoftTimer(PitfallTimers.lure)
    }

    public fun maximumTraps(player: Player): Int = minOf(5, player.stat(stats.hunter) / 20 + 1)

    public fun approximateCatchChance(level: Int, species: PitfallSpecies): Double =
        if (species.index >= 3 || (species.index == 1 && level >= 85)) 1.0
        else (0.5 + (level.coerceIn(species.level, 99) - species.level) * 0.005).coerceAtMost(0.95)

    private fun rollLoot(level: Int, species: PitfallSpecies): List<InvObj> = buildList {
        add(InvObj(objs.big_bones))
        add(InvObj(species.meat))
        val fur =
            if (
                species.index >= 3 ||
                    random.randomDouble() <
                        (0.5 + (level.coerceIn(species.level, 99) - species.level) * 0.005)
                            .coerceAtMost(0.95)
            )
                species.fur
            else PitfallObjs.tatty[species.index]
        add(InvObj(fur))
        if (species.index >= 3) add(InvObj(PitfallObjs.antlers[species.index - 3]))
        if (species.index == 3) add(InvObj(PitfallObjs.splinters, random.of(2..6)))
    }

    private fun render(trap: Trap, stage: Int) {
        for (original in trap.parts) {
            val type = locTypes.getValue(original.id)
            val target =
                if (stage == 0) original.id
                else if (type.multiLoc.isNotEmpty())
                    (type.multiLoc.getOrNull(stage)?.toInt()?.and(0xffff) ?: type.multiLocDefault)
                        .let { if (it == 65535) -1 else it }
                else
                    when (stage) {
                        1 -> PitfallLocs.set.id
                        2 -> PitfallLocs.catching.id
                        else -> PitfallLocs.full[trap.species.index].id
                    }
            val current = trap.current[original]
            if (target < 0) {
                if (current != null) locs.del(current, Int.MAX_VALUE)
                trap.current[original] = null
            } else {
                val next =
                    original.copy(entity = LocEntity(target, original.shapeId, original.angleId))
                locs.add(next, Int.MAX_VALUE)
                trap.current[original] = next
            }
        }
    }

    private fun remove(trap: Trap, deferred: Boolean = false) {
        render(trap, 0)
        traps.remove(trap.source.coords.packed)
        if (deferred) trap.con.duration(0) else cons.del(trap.con)
    }

    private fun distance(a: CoordGrid, b: CoordGrid): Int = maxOf(abs(a.x - b.x), abs(a.z - b.z))

    public companion object {
        public const val BUILD_TICKS: Int = 4
        public const val JUMP_TICKS: Int = 2
        public const val COLLAPSE_TICKS: Int = 2
        public const val EXPIRY_TICKS: Int = 200
        private var Controller.owner: Int by intVarCon(PitfallVars.owner)
    }
}
