package org.rsmod.content.bosses.world

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class GiantEggSacTest {
    @Test
    fun GameTestState
        .`a knife and a bank are required to cut the sac and all one hundred eggs are recoverable`() =
        runGameTest(GiantEggSac::class) {
            val full = objTypes.getValue(23517)
            player.placeAt(CoordGrid(3200, 3200))
            player.inv[0] = InvObj(full)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(InvObj(full), player.inv[0])
            player.inv[1] = InvObj(objs.knife)
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(InvObj(full), player.inv[0])
            val bank = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(10355))
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(26, player.count(objs.red_spiders_eggs))
            assertEquals(74, player.inv[0]?.vars)
            player.placeAt(bank.coords.translate(10, 0))
            repeat(2) {
                for (i in 2..27) player.inv[i] = null
                player.withProtectedAccess { opHeld1(0) }
            }
            assertEquals(22, player.inv[0]?.vars)
            for (i in 2..27) player.inv[i] = null
            player.withProtectedAccess { opHeld1(0) }
            assertEquals(22, player.count(objs.red_spiders_eggs))
            assertEquals(0, player.count(objTypes.getValue(23520)))
        }

    @Test
    fun GameTestState
        .`bankside empty deposits the selected sac without consuming another open sac`() =
        runGameTest(GiantEggSac::class) {
            player.members = true
            val open = objTypes.getValue(23520)
            player.inv[2] = InvObj(open, vars = 21)
            player.inv[4] = InvObj(open, vars = 64)
            eventBus.publish(HeldBanksideEvents.Type(player, 4, open))
            assertEquals(InvObj(open, vars = 21), player.inv[2])
            assertNull(player.inv[4])
            val bank = player.invMap.getOrPut(cacheTypes.invs[invs.bank])
            assertEquals(InvObj(objs.red_spiders_eggs, 64), bank[0])
        }
}
