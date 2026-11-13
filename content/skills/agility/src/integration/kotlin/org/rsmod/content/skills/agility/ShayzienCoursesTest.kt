package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.quiver
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class ShayzienCoursesTest {
    @Test
    fun GameTestState
        .`both branches start at the same mapped ladder and award only ordered laps`() =
        runGameTest(ShayzienCourses::class) {
            player.observerUUID = 1L
            player.members = true
            fun stage(
                name: String,
                x: Int,
                z: Int,
                plane: Int,
                shape: Int,
                angle: Int,
                endX: Int,
                endZ: Int,
                endPlane: Int,
            ) =
                placeMapLoc(
                    CoordGrid(x, z, plane),
                    locTypes.values.single { it.internalName == "shayzien_agility_$name" },
                    LocShape[shape],
                    LocAngle[angle],
                ) to CoordGrid(endX, endZ, endPlane)
            val shared =
                listOf(
                    stage("both_start_ladder", 1554, 3631, 0, 10, 0, 1554, 3632, 3),
                    stage("both_rope_climb", 1552, 3633, 3, 10, 1, 1538, 3633, 2),
                    stage("both_rope_walk", 1536, 3633, 2, 22, 2, 1527, 3633, 2),
                )
            val basic =
                listOf(
                    stage("low_bar_climb", 1523, 3640, 2, 10, 2, 1523, 3643, 3),
                    stage("low_rope_walk_1", 1525, 3644, 3, 22, 0, 1539, 3644, 2),
                    stage("low_rope_walk_2", 1541, 3644, 2, 22, 0, 1553, 3644, 2),
                    stage("low_end_jump", 1554, 3641, 2, 10, 0, 1554, 3640, 0),
                )
            val advanced =
                listOf(
                    stage("up_swing_jump_1", 1512, 3637, 2, 10, 1, 1510, 3637, 2),
                    stage("up_jump_platform_1", 1510, 3634, 2, 10, 0, 1510, 3630, 2),
                    stage("up_jump_platform_2", 1510, 3627, 2, 10, 0, 1510, 3620, 2),
                    stage("up_swing_jump_2", 1512, 3619, 2, 10, 3, 1522, 3619, 2),
                    stage("up_end_jump", 1522, 3621, 2, 10, 2, 1522, 3625, 0),
                )
            val task = DiaryTask.FinishShayzienAgilityLap
            fun complete() = task.region.tasks(task.difficulty)[task.number - 1].isComplete(player)
            player.teleport(CoordGrid(1554, 3642, 2))
            player.opLoc1(basic.last().first)
            advance(20)
            assertFalse(complete())
            assertEquals(0, player.statMap.getFineXP(stats.agility))
            for ((upper, path) in listOf(false to basic, true to advanced)) {
                player.setCurrentLevel(stats.agility, if (upper) 99 else 1)
                player.teleport(CoordGrid(1554, 3630))
                for ((i, obstacle) in (shared + path).withIndex()) {
                    val (loc, dest) = obstacle
                    if (upper && i == shared.size) {
                        player.opLoc1(loc)
                        advance(35)
                        assertNotEquals(dest, player.coords)
                        player.righthand =
                            InvObj(
                                objTypes.values.single {
                                    it.internalName == "xbows_crossbow_bronze"
                                }
                            )
                        player.quiver = InvObj(objs.mith_grapple)
                    }
                    player.opLoc1(loc)
                    advance(35)
                    assertEquals(
                        dest,
                        player.coords,
                        "${if (upper) "Advanced" else "Basic"} stage ${i+1}",
                    )
                }
                assertTrue(complete())
                assertEquals(if (upper) 6610 else 1535, player.statMap.getFineXP(stats.agility))
            }
        }
}
