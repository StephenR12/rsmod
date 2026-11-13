package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class GnomeStrongholdCourseTest {
    @Test
    fun GameTestState.`alternate net and pipe lanes complete one ordered lap`() =
        runGameTest(GnomeStrongholdCourse::class) {
            player.observerUUID = 1L
            player.members = true
            val start = player.statMap.getFineXP(stats.agility)
            fun cross(
                name: String,
                target: CoordGrid,
                from: CoordGrid,
                to: CoordGrid,
                ground: Boolean = false,
            ) {
                val type = locTypes.values.single { it.internalName == name }
                allocZoneCollision(from)
                allocZoneCollision(to)
                val shape = if (ground) LocShape.GroundDecor else LocShape.CentrepieceStraight
                val loc = placeMapLoc(target, type, shape)
                player.teleport(from)
                player.opLoc1(loc)
                advance(12)
                assertEquals(to, player.coords, type.toString())
            }
            cross(
                "gnome_log_balance1",
                CoordGrid(2474, 3435),
                CoordGrid(2474, 3436),
                CoordGrid(2474, 3429),
                true,
            )
            cross(
                "obstical_net2",
                CoordGrid(2475, 3425),
                CoordGrid(2475, 3426),
                CoordGrid(2475, 3424, 1),
            )
            cross(
                "climbing_branch",
                CoordGrid(2473, 3422, 1),
                CoordGrid(2473, 3423, 1),
                CoordGrid(2473, 3420, 2),
            )
            cross(
                "balancing_rope",
                CoordGrid(2478, 3420, 2),
                CoordGrid(2477, 3420, 2),
                CoordGrid(2483, 3420, 2),
                true,
            )
            cross(
                "climbing_tree2",
                CoordGrid(2487, 3420, 2),
                CoordGrid(2486, 3420, 2),
                CoordGrid(2485, 3419),
            )
            cross(
                "obstical_net3",
                CoordGrid(2487, 3426),
                CoordGrid(2487, 3425),
                CoordGrid(2487, 3427),
            )
            cross(
                "obstical_pipe3_2",
                CoordGrid(2487, 3431),
                CoordGrid(2487, 3430),
                CoordGrid(2487, 3437),
            )
            assertEquals(1105, player.statMap.getFineXP(stats.agility) - start)
            cross(
                "obstical_pipe3_1",
                CoordGrid(2484, 3431),
                CoordGrid(2484, 3430),
                CoordGrid(2484, 3437),
            )
            assertEquals(1180, player.statMap.getFineXP(stats.agility) - start)
        }
}
