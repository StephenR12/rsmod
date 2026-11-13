package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class EagleTravelTest {
    @Test
    fun GameTestState
        .`regional eagles require training and a rope and only the outward trip credits the diary`() =
        runGameTest(EagleTravel::class) {
            val landings =
                listOf(CoordGrid(3421, 9571), CoordGrid(2515, 9322), CoordGrid(2726, 10217))
            val nests =
                listOf(CoordGrid(2020, 4958, 3), CoordGrid(2023, 4961, 3), CoordGrid(2026, 4963, 3))
            val tasks =
                listOf(
                    DiaryTask.FlyEagleToDesert,
                    DiaryTask.FlyPeakToFeldip,
                    DiaryTask.FlyEagleToSnow,
                )
            player.members = true
            for (i in nests.indices) {
                val bird = spawnNpc(nests[i], npcTypes.getValue(1487 + i))
                val start = nests[i].translateX(-1)
                val returnBird = spawnNpc(landings[i].translateX(1), npcTypes.getValue(1487 + i))
                player.teleport(start)
                player.inv[0] = null
                player.setVarBit(cacheTypes.varbits.getValue(2780), 39)
                player.opNpc1(bird)
                advance(2)
                assertEquals(start, player.coords)
                player.setVarBit(cacheTypes.varbits.getValue(2780), 40)
                player.opNpc1(bird)
                advance(2)
                assertFalse(player.hasCompletedDiaryTask(tasks[i]))
                player.inv[0] = InvObj(objs.rope)
                player.opNpc1(bird)
                advance(2)
                assertEquals(landings[i], player.coords)
                assertTrue(player.hasCompletedDiaryTask(tasks[i]))
                player.opNpc1(returnBird)
                advance(2)
                assertEquals(start, player.coords)
            }
        }
}
