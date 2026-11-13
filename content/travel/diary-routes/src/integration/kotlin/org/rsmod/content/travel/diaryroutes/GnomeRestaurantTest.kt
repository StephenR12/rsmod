package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.PlayerGnomeCookingSuccess
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class GnomeRestaurantTest {
    @Test
    fun GameTestState
        .`both four course recommendations require a completed handmade dish and one hand in`() =
        runGameTest(GnomeRestaurant::class) {
            player.members = true
            val save = cacheTypes.varps.getValue(65683)
            for ((track, id) in listOf(2656, 6531).withIndex()) {
                val tile = if (track == 0) CoordGrid(2449, 3503, 1) else CoordGrid(2483, 3489, 1)
                val teacher = spawnNpc(tile, npcTypes.getValue(id))
                player.teleport(tile.translateZ(-1))
                fun ask() {
                    player.opNpc1(teacher)
                    advance(2)
                }
                ask()
                ask()
                assertEquals(2, (player.vars[save] ushr (4 * track)) and 15)
                for ((i, name) in
                    listOf(
                            listOf(
                                "toad_crunchies",
                                "chocchip_crunchies",
                                "toad_batta",
                                "cheese+tom_batta",
                            ),
                            listOf("fruit_blast", "pineapple_punch", "wizard_blizzard", "sgg"),
                        )[track]
                        .withIndex()) {
                    val lesson = objTypes.values.first { it.internalName == name }
                    player.clearInv()
                    player.inv[0] = InvObj(lesson)
                    ask()
                    assertEquals(2 + 2 * i, (player.vars[save] ushr (4 * track)) and 15)
                    eventBus.publish(PlayerGnomeCookingSuccess(player, lesson))
                    ask()
                    assertEquals(4 + 2 * i, (player.vars[save] ushr (4 * track)) and 15)
                }
                player.clearInv()
            }
            assertEquals(170, player.vars[save])
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(2493)])
        }

    @Test
    fun GameTestState
        .`ordinary NPC conversation requires the active box and unexpired matching handmade order`() =
        runGameTest(GnomeRestaurant::class) {
            player.members = true
            val chef = spawnNpc(CoordGrid(2437, 3500, 1), npcTypes.getValue(2547))
            player.teleport(CoordGrid(2437, 3499, 1))
            val box = objTypes.getValue(9477)
            val active = cacheTypes.varbits.getValue(2478)
            player.opNpc3(chef)
            advance(2)
            assertEquals(0, player.count(box))
            player.setVarp(cacheTypes.varps.getValue(65683), 170)
            random.next = 0
            random.then = 0
            player.opNpc3(chef)
            advance(2)
            assertEquals(1, player.count(box))
            assertEquals(1, player.vars[active])
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(2484)])
            val customer = spawnNpc(CoordGrid(2421, 3471), npcTypes.getValue(2575))
            player.teleport(CoordGrid(2421, 3470))
            val premade = objTypes.values.first { it.internalName == "premade_toad_crunchies" }
            player.inv[1] = InvObj(premade)
            fun deliver() {
                player.opNpc1(customer)
                advance(2)
            }
            deliver()
            assertEquals(1, player.count(box))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverGnomeFood))
            player.inv[1] = InvObj(objTypes.values.first { it.internalName == "toad_crunchies" })
            player.inv[0] = null
            deliver()
            assertEquals(1, player.vars[active])
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.DeliverGnomeFood))
            player.inv[0] = InvObj(box)
            player.setVarBit(cacheTypes.varbits.getValue(2480), 11)
            deliver()
            assertEquals(0, player.vars[active])
            assertEquals(0, player.count(box))
            assertEquals(1, player.count(objTypes.getValue(9474)))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.DeliverGnomeFood))
            player.teleport(CoordGrid(2437, 3499, 1))
            random.next = 0
            random.then = 0
            player.opNpc3(chef)
            advance(2)
            player.setVarp(
                cacheTypes.varps.getValue(836),
                (System.currentTimeMillis() / 1000).toInt() - 361,
            )
            player.inv[5] = InvObj(objTypes.values.first { it.internalName == "toad_crunchies" })
            player.teleport(CoordGrid(2421, 3470))
            deliver()
            assertEquals(0, player.vars[active])
            assertEquals(
                1,
                player.count(objTypes.values.first { it.internalName == "toad_crunchies" }),
            )
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(11960)])
        }
}
