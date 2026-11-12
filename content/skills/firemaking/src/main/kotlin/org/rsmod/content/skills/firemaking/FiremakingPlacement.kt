package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.config.refs.content
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/** Cache-233 fire-placement rules shared by tinderbox and Barbarian-bow ignition. */
public class FiremakingPlacement
@Inject
constructor(private val locRepo: LocRepository, private val locTypes: LocTypeList) {
    private val maximumSceneryDimension: Int =
        locTypes.values.maxOfOrNull { max(it.width, it.length) } ?: 1

    /** Returns the player-visible rejection message, or `null` when [coords] can hold a fire. */
    public fun rejectionAt(coords: CoordGrid): String? {
        val areaMessage = FiremakingRestrictedAreas.messageAt(coords)
        if (areaMessage != null) {
            return areaMessage
        }
        if (hasFloorSceneryAt(coords) || isClosedDoorSwingTile(coords)) {
            return Firemaking.CANNOT_LIGHT_HERE
        }
        return null
    }

    /**
     * Locs are indexed only by their south-west origin. Search every possible origin zone so a
     * multi-tile centrepiece is rejected across its complete rotated footprint.
     */
    private fun hasFloorSceneryAt(coords: CoordGrid): Boolean {
        val minimumX = (coords.x - maximumSceneryDimension + 1).coerceAtLeast(0)
        val minimumZ = (coords.z - maximumSceneryDimension + 1).coerceAtLeast(0)
        val southWest = ZoneKey.fromAbsolute(minimumX, minimumZ, coords.level)
        val northEast = ZoneKey.from(coords)
        for (zoneX in southWest.x..northEast.x) {
            for (zoneZ in southWest.z..northEast.z) {
                val zone = ZoneKey(zoneX, zoneZ, coords.level)
                if (locRepo.findAll(zone).any { it.isFloorSceneryCovering(coords) }) {
                    return true
                }
            }
        }
        return false
    }

    private fun LocInfo.isFloorSceneryCovering(coords: CoordGrid): Boolean {
        if (shape !in FLOOR_SCENERY_SHAPES) {
            return false
        }
        val type = locTypes[this]
        if (shape == LocShape.WallDiagonal && type.contentGroup in DOOR_CONTENT_GROUPS) {
            return false
        }
        val bound = BoundLocInfo(this, type)
        return coords.x in bound.x until (bound.x + bound.adjustedWidth) &&
            coords.z in bound.z until (bound.z + bound.adjustedLength)
    }

    /** A closed door blocks the tile onto which its opened model would move. */
    private fun isClosedDoorSwingTile(coords: CoordGrid): Boolean =
        DOOR_ORIGIN_OFFSETS.any { (x, z) ->
            locRepo.findAll(coords.translate(x, z)).any { loc ->
                val group = locTypes[loc].contentGroup
                group in CLOSED_DOOR_CONTENT_GROUPS &&
                    DoorTranslations.translateOpen(loc.coords, loc.shape, loc.angle) == coords
            }
        }

    private companion object {
        val FLOOR_SCENERY_SHAPES: Set<LocShape> =
            setOf(
                LocShape.WallDiagonal,
                LocShape.CentrepieceStraight,
                LocShape.CentrepieceDiagonal,
                LocShape.GroundDecor,
            )

        val CLOSED_DOOR_CONTENT_GROUPS: Set<Int> =
            setOf(
                content.closed_single_door.id,
                content.closed_left_door.id,
                content.closed_right_door.id,
            )

        val DOOR_CONTENT_GROUPS: Set<Int> =
            CLOSED_DOOR_CONTENT_GROUPS +
                setOf(
                    content.opened_single_door.id,
                    content.opened_left_door.id,
                    content.opened_right_door.id,
                )

        val DOOR_ORIGIN_OFFSETS: List<Pair<Int, Int>> =
            listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}

/** Exact map polygons published by the cutoff Fire article. */
internal object FiremakingRestrictedAreas {
    private const val DWARVES_MESSAGE = "The dwarves won't be happy if you light a fire here."
    private const val GILBERT_MESSAGE = "A colourful magical force won't let you light a fire here."
    private const val JIGGIG_MESSAGE = "The ogres won't be happy if you light a fire here."

    private val restrictions =
        listOf(
            RestrictedArea(
                TilePolygon(
                    level = 0,
                    vertices =
                        listOf(
                            TilePoint(2944, 3417),
                            TilePoint(3072, 3417),
                            TilePoint(3072, 3520),
                            TilePoint(3008, 3520),
                            TilePoint(3008, 3456),
                            TilePoint(2880, 3456),
                            TilePoint(2880, 3520),
                            TilePoint(2944, 3520),
                        ),
                ),
                DWARVES_MESSAGE,
            ),
            RestrictedArea(
                TilePolygon.rectangle(
                    level = 0,
                    west = 3118,
                    south = 3390,
                    east = 3136,
                    north = 3408,
                ),
                GILBERT_MESSAGE,
            ),
            RestrictedArea(
                TilePolygon.rectangle(
                    level = 0,
                    west = 2432,
                    south = 3008,
                    east = 2496,
                    north = 3072,
                ),
                JIGGIG_MESSAGE,
            ),
        )

    fun messageAt(coords: CoordGrid): String? =
        restrictions.firstOrNull { coords in it.polygon }?.message

    private data class RestrictedArea(val polygon: TilePolygon, val message: String)
}

internal data class TilePoint(val x: Int, val z: Int)

internal class TilePolygon(private val level: Int, private val vertices: List<TilePoint>) {
    init {
        require(vertices.size >= 3) { "A tile polygon needs at least three vertices." }
    }

    operator fun contains(coords: CoordGrid): Boolean {
        if (coords.level != level) {
            return false
        }
        val point = TilePoint(coords.x, coords.z)
        var inside = false
        var previous = vertices.last()
        for (current in vertices) {
            if (point.onSegment(previous, current)) {
                return true
            }
            val crosses =
                (current.z > point.z) != (previous.z > point.z) &&
                    point.x <
                        (previous.x - current.x).toDouble() * (point.z - current.z).toDouble() /
                            (previous.z - current.z).toDouble() + current.x
            if (crosses) {
                inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun TilePoint.onSegment(first: TilePoint, second: TilePoint): Boolean {
        val cross =
            (z - first.z).toLong() * (second.x - first.x) -
                (x - first.x).toLong() * (second.z - first.z)
        return cross == 0L &&
            x in minOf(first.x, second.x)..maxOf(first.x, second.x) &&
            z in minOf(first.z, second.z)..maxOf(first.z, second.z)
    }

    companion object {
        fun rectangle(level: Int, west: Int, south: Int, east: Int, north: Int): TilePolygon =
            TilePolygon(
                level,
                listOf(
                    TilePoint(west, south),
                    TilePoint(east, south),
                    TilePoint(east, north),
                    TilePoint(west, north),
                ),
            )
    }
}
