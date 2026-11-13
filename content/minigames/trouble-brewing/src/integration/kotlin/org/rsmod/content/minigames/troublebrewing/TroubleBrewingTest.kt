package org.rsmod.content.minigames.troublebrewing

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.hat
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class TroubleBrewingTest {
    @Test
    fun GameTestState
        .`a full two-crew match makes and packs rum then pays pieces and awards the diary`() =
        runGameTest(TroubleBrewing::class) {
            val docks = CoordGrid(3813, 3020)
            val red = registerPlayer(docks)
            val quitter = registerPlayer(docks)
            val captains =
                listOf(1814, 1815).mapIndexed { i, id ->
                    spawnNpc(docks.translate(i, 1), npcTypes.getValue(id))
                }
            for (p in listOf(player, red, quitter)) {
                p.members = true
                p.placeAt(docks)
                p.stats[stats.cooking] = 40
            }
            val quest = cacheTypes.varps.getValue(655)
            fun join(p: Player, crew: Int) {
                p.placeAt(captains[crew].coords.translate(0, -1))
                p.withProtectedAccess { opNpc3(captains[crew]) }
                advance()
            }
            join(player, 0)
            assertNull(player.hat)
            player.setVarp(quest, 140)
            player.inv[0] = InvObj(objs.coins)
            join(player, 0)
            assertNull(player.hat)
            player.inv[0] = null
            join(player, 0)
            assertEquals(8949, player.hat?.id)
            for (p in listOf(red, quitter)) {
                p.setVarp(quest, 140)
                join(p, 1)
            }
            advance(300)
            assertEquals(CoordGrid(3817, 2996), player.coords)
            assertEquals(CoordGrid(3815, 2955), red.coords)
            fun interact(p: Player, id: Int, c: CoordGrid) {
                val type = locTypes.getValue(id)
                val loc = placeMapLoc(c, type)
                p.withProtectedAccess { eventBus.publish(this, LocEvents.Op1(loc, loc, type)) }
            }
            fun use(id: Int, c: CoordGrid, item: Int) {
                val type = locTypes.getValue(id)
                val loc = placeMapLoc(c, type)
                val obj = objTypes.getValue(item)
                val slot = player.inv.indices.first { player.inv[it]?.id == item }
                player.withProtectedAccess {
                    eventBus.publish(this, LocUEvents.Op(loc, loc, type, obj, slot))
                }
            }
            interact(quitter, 15996, quitter.coords)
            assertNull(quitter.hat)
            player.inv[0] = InvObj(objs.bucket_empty)
            repeat(5) {
                use(15936, CoordGrid(3816, 3000), objs.bucket_empty.id)
                if (it == 0) {
                    use(15847, CoordGrid(3811, 3003, 2), objs.bucket_water.id)
                    assertEquals(1, player.inv.count(objs.bucket_water))
                }
                use(15847, CoordGrid(3811, 3000, 2), objs.bucket_water.id)
            }
            for ((item, x) in listOf(8974 to 3811, 8981 to 3815, 8977 to 3813, 8976 to 3817)) {
                repeat(if (item == 8974) 3 else 1) {
                    player.inv[15] = InvObj(objTypes.getValue(item))
                    use(15847, CoordGrid(x, 3003, 2), item)
                }
            }
            player.inv[27] = InvObj(objs.tinderbox)
            for (x in 3819..3821) {
                val c = CoordGrid(x, 2998)
                repeat(3) {
                    player.inv[14] = InvObj(objs.logs)
                    use(15903, c, objs.logs.id)
                }
                use(15904, c, objs.tinderbox.id)
            }
            advance(69)
            assertTrue(findLocs(CoordGrid(3821, 3003)).any { it.id == 15926 })
            interact(player, 15926, CoordGrid(3821, 3003))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CompleteTroubleBrewing))
            advance(1932)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CompleteTroubleBrewing))
            assertTrue(red.hasCompletedDiaryTask(DiaryTask.CompleteTroubleBrewing))
            assertFalse(quitter.hasCompletedDiaryTask(DiaryTask.CompleteTroubleBrewing))
            assertNull(player.hat)
            assertEquals(0, player.inv.count(objs.bucket_empty))
            val pieces = cacheTypes.varps.getValue(4218)
            assertEquals(30, player.vars[pieces])
            assertEquals(0, red.vars[pieces])
            val jimmy = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(1816))
            player.withProtectedAccess { opNpc3(jimmy) }
            advance()
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 0))
            advance()
            assertEquals(10, player.vars[pieces])
            assertEquals(1, player.inv.count(objTypes.getValue(8941)))
        }
}
