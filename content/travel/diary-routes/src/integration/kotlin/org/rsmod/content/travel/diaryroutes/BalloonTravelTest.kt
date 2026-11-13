package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class BalloonTravelTest {
    @Test
    fun GameTestState.`first flights navigate three screens while repeat flights use one log`() =
        runGameTest(BalloonTravel::class) {
            data class Course(
                val button: Int,
                val unlock: Int,
                val fuel: Int,
                val fare: Int,
                val landing: CoordGrid,
                val path: String,
            )
            val routes =
                listOf(
                    Course(
                        13,
                        2869,
                        1515,
                        10,
                        CoordGrid(2459, 3107),
                        "SSLRRRRRELRRERRLLRRRLRREDRRRRRRRSRRELDRLRRDRRRLERRRRSERRD",
                    ),
                    Course(
                        14,
                        2870,
                        1513,
                        3,
                        CoordGrid(2479, 3458),
                        "RRRRRRRDRRRRRRRRRRRRLDLDLRRLDRRSRRERDRRLLLRRDLRRRRREREDRR",
                    ),
                    Course(
                        15,
                        2871,
                        1521,
                        10,
                        CoordGrid(2923, 3301),
                        "RRRLLRRERRRRLERRDRLRRDRLRRRRDRRRLRRRRRRRRSRRRRRRERRRDRRRD",
                    ),
                    Course(
                        18,
                        2872,
                        1519,
                        10,
                        CoordGrid(3298, 3481),
                        "LLLLRRDDDRRDLRRDLLRRRRRRRRLLREERRRSRRRRRRRSRDDSRDDSRERRED",
                    ),
                )
            player.members = true
            player.setBaseLevel(stats.firemaking, 60)
            player.setCurrentLevel(stats.firemaking, 60)
            val start = CoordGrid(2810, 3355)
            val pilot = spawnNpc(start.translateX(-1), npcTypes.getValue(4728))
            player.teleport(start)
            fun click(group: Int, index: Int) {
                player.ifButton(cacheTypes.components.getValue(group shl 16 or index))
                advance(1)
            }
            fun plan(button: Int) {
                player.opNpc4(pilot)
                advance(2)
                click(469, button)
            }
            player.setVarBit(cacheTypes.varbits.getValue(2868), 1)
            plan(13)
            assertEquals(0, player.vars[cacheTypes.varbits.getValue(2884)])
            player.setVarBit(cacheTypes.varbits.getValue(2866), 200)
            for (c in routes) {
                player.teleport(start)
                val fuel = objTypes.getValue(c.fuel)
                repeat(c.fare) { player.inv[it] = InvObj(fuel) }
                plan(c.button)
                assertEquals(0, player.count(fuel))
                assertTrue(player.vars[cacheTypes.varbits.getValue(2884)] > 0)
                for ((index, step) in c.path.withIndex()) {
                    click(
                        471,
                        when (step) {
                            'L' -> 24
                            'S' -> 17
                            'D' -> 6
                            'E' -> 9
                            else -> 4
                        },
                    )
                    if (index + 1 < c.path.length)
                        assertEquals(
                            start,
                            player.coords,
                            "course=${c.button} step=$index $step x=${player.vars[cacheTypes.varbits.getValue(2882)]} y=${player.vars[cacheTypes.varbits.getValue(2883)]}",
                        )
                }
                assertEquals(
                    c.landing,
                    player.coords,
                    "course=${c.button} x=${player.vars[cacheTypes.varbits.getValue(2882)]} y=${player.vars[cacheTypes.varbits.getValue(2883)]} map=${player.vars[cacheTypes.varbits.getValue(2884)]} bags=${player.vars[cacheTypes.varbits.getValue(2880)]} logs=${player.vars[cacheTypes.varbits.getValue(2881)]}",
                )
                assertEquals(1, player.vars[cacheTypes.varbits.getValue(c.unlock)])
            }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RideCastleWarsBalloon))
            val assistant = spawnNpc(CoordGrid(3299, 3480), npcTypes.getValue(4732))
            player.inv[0] = InvObj(objs.logs)
            player.opNpc4(assistant)
            advance(2)
            click(469, 17)
            assertEquals(CoordGrid(2937, 3421), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.FlyFromVarrock))
            assertEquals(0, player.count(objs.logs))
        }
}
