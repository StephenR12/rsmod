package org.rsmod.content.travel.diaryroutes

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.*
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

class CharterTravelTest {
    @Test
    fun GameTestState
        .`shipyard crew enforces both quest flags contraband funds and both fare discounts`() =
        runGameTest(CharterTravel::class) {
            player.members = true
            val from = CoordGrid(3000, 3033)
            val to = CoordGrid(2761, 3238)
            player.teleport(from)
            allocZoneCollision(to)
            val crew = spawnNpc(from.translateX(1), npcTypes.getValue(9312))
            player.setVarBit(cacheTypes.varbits.getValue(75), 1)
            player.setVarp(cacheTypes.varps.getValue(150), 160)
            player.inv[0] = InvObj(objs.coins, 400)
            fun sail() {
                player.opNpc4(crew)
                advance(2)
                player.ifButton(cacheTypes.components.getValue((72 shl 16) or 21))
                advance(2)
            }
            sail()
            assertEquals(from, player.coords)
            player.setVarp(cacheTypes.varps.getValue(365), 2)
            player.inv[1] = InvObj(objTypes.getValue(431))
            sail()
            assertEquals(400, player.count(objs.coins))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CharterShipyard))
            player.inv[1] = null
            player.inv[0] = InvObj(objs.coins, 99)
            player.worn[Wearpos.Ring.slot] = InvObj(objTypes.getValue(6465))
            player.setVarp(cacheTypes.varps.getValue(655), 140)
            sail()
            assertEquals(99, player.count(objs.coins))
            player.inv[0] = InvObj(objs.coins, 100)
            sail()
            assertEquals(to, player.coords)
            assertEquals(0, player.count(objs.coins))
            assertEquals(1, player.vars[cacheTypes.varbits.getValue(11209)])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CharterShipyard))
        }
}
