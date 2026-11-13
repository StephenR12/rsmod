package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FairyRingTravelTest {
    @Test
    fun GameTestState.`Zanaris dials lead to the Wizards Tower and record DIS after arrival`() =
        runGameTest(FairyRingTravel::class) {
            val source = CoordGrid(2412, 4434)
            val destination = CoordGrid(3108, 3149)
            player.members = true
            player.teleport(source.translate(1, 0))
            allocZoneCollision(destination)
            val ring = placeMapLoc(source, locTypes.getValue(29560))
            player.withProtectedAccess {
                vars[cacheTypes.varbits.getValue(2329)] = 1
                vars[DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).reward] = 1
            }
            player.opLoc2(ring)
            advance()
            fun click(component: Int) {
                player.ifButton(cacheTypes.components.getValue((398 shl 16) or component))
                advance()
            }
            click(19)
            click(23)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.RingWizardsTower))
            click(26)
            advance(5)
            assertEquals(destination, player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RingWizardsTower))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(3999)])
            assertEquals(17, player.vars[cacheTypes.varbits.getValue(5374)])
        }

    @Test
    fun GameTestState.`ordinary rings require both permission and a staff or the elite reward`() =
        runGameTest(FairyRingTravel::class) {
            val source = CoordGrid(3108, 3149)
            val start = source.translate(1, 0)
            player.members = true
            player.teleport(start)
            val ring = placeMapLoc(source, locTypes.getValue(29495))
            player.opLoc1(ring)
            advance(5)
            assertEquals(start, player.coords)
            player.withProtectedAccess { vars[cacheTypes.varbits.getValue(2329)] = 1 }
            player.opLoc1(ring)
            advance(5)
            assertEquals(start, player.coords)
            player.withProtectedAccess {
                vars[DiaryRegion.Lumbridge.tier(DiaryDifficulty.Elite).reward] = 1
            }
            allocZoneCollision(CoordGrid(2412, 4434))
            player.opLoc1(ring)
            advance(5)
            assertEquals(CoordGrid(2412, 4434), player.coords)
        }
}
