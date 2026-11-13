package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.StepValidator
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal object SepulchreHazardMoves : SeqReferences() {
    val fireReady = find("hallowed_statue_ready_fire_blast")
    val fire = find("hallowed_statue_start_fire_blast")
    val fireEnd = find("hallowed_statue_end_fire_blast")
    val bolt = find("hallowed_statue_projectile_attack")
    val sword = find("hallowed_statue_sword_start_attack")
    val spin = find("hallowed_statue_sword_spin")
    val lightning = find("hallowed_statue_lightning_start_attack")
}

internal object SepulchreHazardSpots : SpotanimReferences() {
    val warning = find("hallowed_statue_lightning_start_strike")
    val strike = find("hallowed_statue_lightning_strike")
    val forward = find("hallowed_telepad_telegraph_forward")
    val back = find("hallowed_telepad_telegraph_backward")
    val ring = find("hallowed_ring_teleport")
}

internal object SepulchreMissiles : NpcReferences() {
    val bolts =
        listOf(
            find("hallowed_projectile_npc"),
            find("hallowed_projectile_npc_t2"),
            find("hallowed_projectile_npc_t3"),
        )
    val swords =
        listOf(
            find("hallowed_sword_npc"),
            find("hallowed_sword_npc_t2"),
            find("hallowed_sword_npc_t3"),
        )
}

internal data class SepulchreFooting(
    var safe: CoordGrid,
    var previous: CoordGrid = safe,
    var immuneUntil: Int = 0,
)

/** The trap footprints and directions come from the placed statue and floor-decoration shapes. */
public class SepulchreTraps
@Inject
constructor(
    private val locs: LocRepository,
    private val world: WorldRepository,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val collision: CollisionFlagMap,
) {
    private val byZone = mutableMapOf<ZoneKey, List<LocInfo>>()
    private val lastVisual = mutableMapOf<LocInfo, Int>()
    private val missiles = mutableMapOf<LocInfo, Npc>()
    private val reach = mutableMapOf<LocInfo, Int>()
    private val steps = StepValidator(collision)

    internal fun remove(region: org.rsmod.game.region.Region) {
        val zones = region.toZoneList().toSet()
        byZone.keys.removeAll(zones)
        val owned = lastVisual.keys.filter { ZoneKey.from(it.coords) in zones }
        for (loc in owned) {
            lastVisual.remove(loc)
            reach.remove(loc)
            val npc = missiles.remove(loc)
            if (npc?.isSlotAssigned == true) npcs.del(npc, Int.MAX_VALUE)
        }
    }

    internal suspend fun process(
        access: ProtectedAccess,
        footing: SepulchreFooting,
        space: SepulchreSpace,
        originTick: Int,
    ) =
        with(access) {
            val tick = player.currentMapClock
            val here = coords
            val rhythm = tick - originTick
            val currentZone = ZoneKey.from(here)
            val hazardLocs =
                byZone.getOrPut(currentZone) {
                    buildList {
                        for (x in -5..5) for (z in -5..5) for (loc in
                            locs.findAll(currentZone.translate(x, z))) {
                            if (loc.id == 38427 || loc.id in 38428..38435 || loc.id in 38444..38450)
                                add(loc)
                        }
                    }
                }
            var threatened = false
            var hit = false
            var shock = false
            var warp: LocInfo? = null
            for (loc in hazardLocs) {
                val last = lastVisual.put(loc, tick)
                val visual = last != tick
                val fx = loc.coords.x
                val fz = loc.coords.z
                val dx = here.x - fx
                val dz = here.z - fz
                val native = space.normal(loc.coords)
                when (loc.id) {
                    38427 -> {
                        // Every Fire model spans three squares perpendicular to the firing statue.
                        val across =
                            if (loc.angleId % 2 == 0) dx == 0 && dz in 0..2
                            else dz == 0 && dx in 0..2
                        val offset = if (loc.angleId % 2 == 0) native.x else native.z
                        val phase = Math.floorMod(rhythm + offset / 3, 8)
                        if (visual)
                            when (phase) {
                                0 -> world.locAnim(loc, SepulchreHazardMoves.fireReady)
                                2 -> world.locAnim(loc, SepulchreHazardMoves.fire)
                                4 -> world.locAnim(loc, SepulchreHazardMoves.fireEnd)
                            }
                        if (across) {
                            threatened = true
                            if (phase in 2..3) hit = true
                        }
                    }
                    in 38428..38435,
                    in 38444..38446 -> {
                        val sword = loc.id < 38444
                        val tier =
                            when (loc.id) {
                                in 38428..38431,
                                38444 -> 0
                                38432,
                                38433,
                                38445 -> 1
                                else -> 2
                            }
                        val speed = tier + 2
                        val direction = direction(loc.angleId)
                        val size = if (sword) 3 else 1
                        val source = loc.coords.translate(size / 2, size / 2)
                        val limit =
                            reach.getOrPut(loc) {
                                var length = 0
                                for (offset in (size / 2 + 1)..36) {
                                    val tile =
                                        source.translate(
                                            direction.first * offset,
                                            direction.second * offset,
                                        )
                                    if (!walkable(tile)) break
                                    length = offset
                                }
                                length
                            }
                        if (limit == 0) continue
                        val travel = (limit + speed - 1) / speed
                        val cycle = if (sword) travel * 2 + 4 else travel + 3
                        val along =
                            (here.x - source.x) * direction.first +
                                (here.z - source.z) * direction.second
                        val side =
                            abs(
                                (here.x - source.x) * direction.second -
                                    (here.z - source.z) * direction.first
                            )
                        val inLane = along in 1..limit && side <= size / 2
                        if (inLane) threatened = true
                        val phase = Math.floorMod(rhythm, cycle)
                        val lane = if (direction.first != 0) native.z else native.x
                        val fires =
                            sword ||
                                Math.floorMod(lane + rhythm / cycle, 3) < (if (tier == 0) 1 else 2)
                        val distance =
                            when {
                                phase < travel -> minOf(limit, (phase + 1) * speed)
                                sword && phase < 2 * travel ->
                                    (limit - (phase - travel + 1) * speed).coerceAtLeast(1)
                                else -> 0
                            }
                        if (!fires || distance == 0) continue
                        if (visual) {
                            if (phase == 0)
                                world.locAnim(
                                    loc,
                                    if (sword) SepulchreHazardMoves.sword
                                    else SepulchreHazardMoves.bolt,
                                )
                            val tile =
                                source.translate(
                                    direction.first * distance,
                                    direction.second * distance,
                                )
                            val missile = missiles[loc]
                            if (phase == 0 || missile == null || !missile.isSlotAssigned) {
                                if (missile?.isSlotAssigned == true)
                                    npcs.del(missile, Int.MAX_VALUE)
                                val type =
                                    if (sword) SepulchreMissiles.swords[tier]
                                    else SepulchreMissiles.bolts[tier]
                                val npc = Npc(npcTypes[type], tile)
                                npcs.add(npc, (if (sword) 2 * travel else travel) - phase)
                                if (sword) npc.anim(SepulchreHazardMoves.spin)
                                missiles[loc] = npc
                            } else missile.telejump(collision, tile)
                        }
                        if (inLane && abs(along - distance) < speed) hit = true
                    }
                    38447,
                    38448 -> {
                        val phase = Math.floorMod(rhythm + 2 * native.x + 3 * native.z, 9)
                        if (phase == 0 && visual)
                            world.spotanimMap(
                                if (loc.id == 38448) SepulchreHazardSpots.forward
                                else SepulchreHazardSpots.back,
                                loc.coords,
                            )
                        if (here == loc.coords && phase in 0..2) warp = loc
                    }
                    38449,
                    38450 -> {
                        val phase = Math.floorMod(rhythm + native.x + native.z, 7)
                        val dir = direction(loc.angleId)
                        val center = loc.coords.translate(1 + dir.first * 3, 1 + dir.second * 3)
                        if (visual && phase == 0) world.locAnim(loc, SepulchreHazardMoves.lightning)
                        for (x in -1..1) for (z in -1..1) {
                            if ((x + z) % 2 != 0) continue
                            val tile = center.translate(x, z)
                            if (visual && (phase == 0 || phase == 2))
                                world.spotanimMap(
                                    if (phase == 0) SepulchreHazardSpots.warning
                                    else SepulchreHazardSpots.strike,
                                    tile,
                                )
                            if (tile == here && phase == 2) shock = true
                        }
                    }
                }
            }
            if (tick >= footing.immuneUntil) {
                when {
                    warp != null -> {
                        val sign = if (warp.id == 38448) 1 else -1
                        val dx = (here.x - footing.previous.x).coerceIn(-1, 1) * sign
                        val dz = (here.z - footing.previous.z).coerceIn(-1, 1) * sign
                        var dest = here
                        if (dx != 0 || dz != 0)
                            repeat(3) {
                                if (steps.canTravel(dest.level, dest.x, dest.z, dx, dz))
                                    dest = dest.translate(dx, dz)
                            }
                        if (dest != here) {
                            telejump(dest)
                            footing.immuneUntil = tick + 5
                        }
                    }
                    hit -> {
                        if (player.worn.none { it?.id == SepulchreItems.ring.id })
                            takeInstantHit(HitType.Typeless, 1 + random.of(3))
                        else spotanim(SepulchreHazardSpots.ring)
                        stopAction()
                        telejump(footing.safe)
                        footing.immuneUntil = tick + 3
                    }
                    shock -> {
                        stopAction()
                        delay(2)
                    }
                }
            }
            if (!threatened && !hit && warp == null && !shock) footing.safe = here
            footing.previous = here
        }

    private fun walkable(tile: CoordGrid): Boolean =
        collision[tile.x, tile.z, tile.level] and
            (CollisionFlag.LOC or CollisionFlag.BLOCK_WALK or CollisionFlag.GROUND_DECOR) == 0

    private fun direction(angle: Int) =
        when (angle) {
            0 -> 0 to -1
            1 -> -1 to 0
            2 -> 0 to 1
            else -> 1 to 0
        }
}
