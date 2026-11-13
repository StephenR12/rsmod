package org.rsmod.content.minigames.tearsofguthix

import java.time.Instant
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class TearsOfGuthixTest {
    @Test
    fun GameTestState
        .`blue streams in one visit pay the lowest skill and enforce both weekly gates`() =
        runGameTest(TearsOfGuthix::class) {
            val entrance = CoordGrid(3251, 9517, 2)
            player.placeAt(entrance)
            player.members = true
            val juna = placeMapLoc(CoordGrid(3252, 9516, 2), locTypes.getValue(28929))
            val quest = cacheTypes.varbits.getValue(451)
            val collected = cacheTypes.varbits.getValue(455)
            val last = cacheTypes.varps.getValue(65616)
            player.setVarp(varps.quest_points, 145)
            fun story() {
                player.opLoc1(juna)
                advance()
            }
            story()
            assertNull(player.righthand)
            player.setVarBit(quest, 2)
            player.righthand = InvObj(objTypes.getValue(1205))
            story()
            assertEquals(objTypes.getValue(1205).id, player.righthand?.id)
            player.righthand = null
            story()
            assertEquals(4704, player.righthand?.id)
            val tiles =
                (3257..3259).flatMap { x -> listOf(CoordGrid(x, 9514, 2), CoordGrid(x, 9520, 2)) } +
                    (9516..9518).map { CoordGrid(3261, it, 2) }
            val walls = tiles.associateWith { placeMapLoc(it, locTypes.getValue(6660)) }
            fun select(ids: Set<Int>) {
                val active =
                    tiles.first { tile -> findLocs(tile).any { it.coords == tile && it.id in ids } }
                val wall = walls.getValue(active)
                player.withProtectedAccess {
                    eventBus.publish(this, LocEvents.Op1(wall, wall, locTypes.getValue(6660)))
                }
            }
            repeat(12) {
                select(setOf(6661, 6665))
                advance()
            }
            val good = player.vars[collected]
            assertTrue(good > 0)
            repeat(3) {
                select(setOf(6662, 6666))
                advance()
            }
            assertTrue(
                player.vars[collected] < good,
                "green streams: before=$good after=${player.vars[collected]}, cycle=${mapClock.cycle} locs=${tiles.map{findLocs(it).map{l->l.id}.toList()}}",
            )
            repeat(130) {
                if (player.righthand != null) select(setOf(6661, 6665))
                advance()
            }
            assertEquals(entrance, player.coords)
            assertNull(player.righthand)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CollectHundredGuthixTears))
            assertTrue(player.statMap.getXP(stats.cooking) >= 1000)
            assertTrue(player.vars[cacheTypes.varbits.getValue(10730)] >= 100)
            story()
            assertNull(player.righthand)
            player.setVarp(last, Instant.now().epochSecond.toInt() - 8 * 86400)
            story()
            assertNull(player.righthand)
            player.setVarp(varps.quest_points, 146)
            story()
            assertEquals(4704, player.righthand?.id)
        }
}
