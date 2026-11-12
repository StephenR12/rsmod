package org.rsmod.content.skills.hunter.birdsnares

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/** Shared-footprint placement guard for portable Hunter traps. */
public class BirdSnarePlacement
@Inject
constructor(private val locRepo: LocRepository, private val locTypes: LocTypeList) {
    private val maximumSceneryDimension: Int =
        locTypes.values.maxOfOrNull { max(it.width, it.length) } ?: 1

    public fun canPlace(coords: CoordGrid): Boolean {
        val minimumX = (coords.x - maximumSceneryDimension + 1).coerceAtLeast(0)
        val minimumZ = (coords.z - maximumSceneryDimension + 1).coerceAtLeast(0)
        val southWest = ZoneKey.fromAbsolute(minimumX, minimumZ, coords.level)
        val northEast = ZoneKey.from(coords)
        for (zoneX in southWest.x..northEast.x) {
            for (zoneZ in southWest.z..northEast.z) {
                val zone = ZoneKey(zoneX, zoneZ, coords.level)
                if (locRepo.findAll(zone).any { it.coversTrapTile(coords) }) {
                    return false
                }
            }
        }
        return true
    }

    private fun LocInfo.coversTrapTile(coords: CoordGrid): Boolean {
        if (shape !in FLOOR_SCENERY_SHAPES) {
            return false
        }
        val bound = BoundLocInfo(this, locTypes[this])
        return coords.x in bound.x until (bound.x + bound.adjustedWidth) &&
            coords.z in bound.z until (bound.z + bound.adjustedLength)
    }

    private companion object {
        val FLOOR_SCENERY_SHAPES =
            setOf(
                LocShape.WallDiagonal,
                LocShape.CentrepieceStraight,
                LocShape.CentrepieceDiagonal,
                LocShape.GroundDecor,
            )
    }
}
