package org.rsmod.content.skills.fletching.valetotems

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object EntSeqs : SeqReferences() {
    val admire = find("npc_ent_worship_01")
}

private data class ValeStop(
    val tile: CoordGrid,
    val site: Int? = null,
    val scenery: List<CoordGrid> = emptyList(),
)

private class EntWalk(var stop: Int) {
    var wait = 0
    var phase = 0
    var admired: CoordGrid? = null
    var lastStep: CoordGrid? = null
    var trail: Pair<CoordGrid, CoordGrid>? = null
    var trailExpires = 0
    val steps = WeakHashMap<Player, Int>()
    val admiredBy = WeakHashMap<Player, Boolean>()
}

public class ValeEnts
@Inject
constructor(
    private val totems: ValeTotems,
    private val players: PlayerList,
    private val locs: LocRepository,
    private val routes: RouteFactory,
) : PluginScript() {
    private val walks = WeakHashMap<Npc, EntWalk>()

    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Create> { start(npc) }
        onEvent<NpcStateEvents.Respawn> { start(npc) }
        onNpcTimer(ValeTimer.walk) { pulse() }
        for (type in ValeLocs.admire) onOpLoc1(type) { event ->
            val ent = walks.values.firstOrNull { it.admired == event.loc.coords }
            if (ent != null && !ent.admiredBy.containsKey(player)) {
                ent.admiredBy[player] = true
                player.runEnergy = (player.runEnergy + 2000).coerceAtMost(10000)
                mes("You admire the scenery alongside the ent and feel refreshed.")
            } else mes("You take a moment to admire the scenery.")
        }
    }

    private fun start(npc: Npc) {
        val origin = ValeNpcs.ents.indexOfFirst { it.id == npc.id }
        if (origin < 0) return
        npc.mode = org.rsmod.game.entity.npc.NpcMode.None
        walks[npc] = EntWalk(stops.indexOfFirst { it.site == origin })
        npc.timer(ValeTimer.walk, 1)
    }

    private fun StandardNpcAccess.pulse() {
        val state = walks[npc] ?: return
        val stop = stops[state.stop]
        val nextSite =
            (state.stop until state.stop + stops.size).firstNotNullOf {
                stops[it % stops.size].site
            }
        if (mapClock >= state.trailExpires) state.trail = null
        val trail = state.trail
        if (trail != null)
            for (p in players) {
                if (!p.members || p.vars[ValeBits.intro] < 100 || p.coords.level != 0) continue
                val a = p.coords == trail.first || p.previousCoords == trail.first
                val b = p.coords == trail.second || p.previousCoords == trail.second
                if (!a && !b) continue
                val bits = (state.steps[p] ?: 0) or (if (a) 1 else 0) or (if (b) 2 else 0)
                if (bits == 3 && state.steps[p] != 3) {
                    set(p, ValeBits.sites[nextSite].trail, 1)
                    set(p, ValeBits.buff, 4)
                    p.mes(
                        "You feel closer to the ent. It will leave you more offerings at the next totem."
                    )
                }
                state.steps[p] = bits
            }
        if (state.wait > 0) {
            state.wait--
            if (state.wait > 0) return
        }
        if (coords.chebyshevDistance(stop.tile) > 1) {
            if (npc.routeDestination.isEmpty() && mapClock % 4 == 0) routeTo(stop.tile)
            if (state.lastStep != coords) {
                val last = state.lastStep
                if (
                    last != null && coords.chebyshevDistance(last) == 1 && random.randomBoolean(18)
                ) {
                    state.trail = last to coords
                    state.steps.clear()
                    state.trailExpires = mapClock + 24
                    locs.add(last, ValeLocs.footprints[0], 24, LocAngle.West, LocShape.GroundDecor)
                    locs.add(
                        coords,
                        ValeLocs.footprints[1],
                        24,
                        LocAngle.West,
                        LocShape.GroundDecor,
                    )
                }
                state.lastStep = coords
            }
            return
        }
        if (stop.site != null) {
            if (state.phase == 0) {
                anim(EntSeqs.admire)
                state.wait = 14
                state.phase = 1
                return
            }
            if (state.phase == 1) {
                for (player in players) totems.visit(player, stop.site)
                state.wait = 8
                state.phase = 2
                state.trail = null
                return
            }
        } else if (stop.scenery.isNotEmpty() && state.phase < 2) {
            state.admired = random.pick(stop.scenery)
            state.admiredBy.clear()
            anim(EntSeqs.admire)
            state.wait = if (state.phase == 0) 2 else 8
            state.phase++
            return
        }
        state.stop = (state.stop + 1) % stops.size
        state.phase = 0
        state.admired = null
        state.lastStep = coords
        routeTo(stops[state.stop].tile)
    }

    private fun StandardNpcAccess.routeTo(tile: CoordGrid) {
        val collision = npc.collisionStrategy ?: return
        val route = routes.create(npc.avatar, tile, collision)
        npc.abortRoute()
        npc.moveSpeed = npc.defaultMoveSpeed
        for (step in route) npc.routeDestination.add(CoordGrid(step.x, step.z, step.level))
    }

    private companion object {
        fun pos(x: Int, z: Int) = CoordGrid(x, z)

        fun scenery(x: Int, z: Int, vararg points: Pair<Int, Int>) =
            ValeStop(pos(x, z), scenery = points.map { pos(it.first, it.second) })

        val stops =
            listOf(
                ValeStop(pos(1368, 3375), 0),
                ValeStop(pos(1367, 3354)),
                scenery(1349, 3336, 1345 to 3337, 1346 to 3333, 1351 to 3333),
                ValeStop(pos(1346, 3317), 1),
                scenery(1359, 3292, 1360 to 3290, 1356 to 3290, 1355 to 3294),
                ValeStop(pos(1386, 3274), 2),
                scenery(1405, 3295, 1408 to 3294, 1403 to 3298, 1404 to 3293),
                ValeStop(pos(1412, 3287), 3),
                scenery(1424, 3307, 1425 to 3309, 1426 to 3305, 1421 to 3303),
                ValeStop(pos(1439, 3305), 4),
                ValeStop(pos(1455, 3313)),
                scenery(1467, 3312, 1465 to 3315, 1467 to 3308, 1470 to 3310),
                ValeStop(pos(1477, 3330), 5),
                ValeStop(pos(1465, 3320)),
                ValeStop(pos(1453, 3337)),
                ValeStop(pos(1453, 3342), 6),
                scenery(1434, 3332, 1433 to 3334, 1432 to 3330, 1438 to 3330),
                scenery(1413, 3335, 1415 to 3335, 1414 to 3333, 1410 to 3335),
                ValeStop(pos(1396, 3329), 7),
                scenery(1384, 3348, 1382 to 3348, 1384 to 3351, 1386 to 3347),
                ValeStop(pos(1383, 3363)),
            )
    }
}
