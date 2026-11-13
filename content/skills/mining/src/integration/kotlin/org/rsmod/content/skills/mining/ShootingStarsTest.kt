package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.ShopsModule
import org.rsmod.api.shops.config.ShopComponents
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.testing.GameTestState
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class ShootingStarsTest {
    @Test
    fun GameTestState.discoveryLevelGatesAndTimeBasedDepletionUseTheLandedStar() =
        runInjectedGameTest(EventBus::class, null, ShootingStars::class) { bus ->
            player.members = false
            // First crash at the Dwarven Mine, size nine; the next impact is several thousand ticks
            // away.
            random.next = 2000
            random.then = 0
            random.then = 0
            bus.publish(GameLifecycle.Startup)
            advance(2002)
            val pos = CoordGrid(3018, 3444)
            val type = locTypes.getValue(41020)
            val loc = BoundLocInfo(requireNotNull(findLoc(pos, type)), type)
            player.telejump(pos.translate(-1, 0))
            advance(2)
            player.inv[0] = InvObj(objTypes.getValue(1275))
            player.stats[stats.mining] = 89
            player.opLoc1(loc)
            advance(3)
            val dust = objTypes.getValue(25527)
            assertEquals(300, player.vars[cacheTypes.varbits.getValue(15351)])
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65816)])
            assertEquals(0, player.count(dust))
            player.stats[stats.mining] = 99
            val initial = player.statMap.getFineXP(stats.mining)
            player.opLoc1(loc)
            advance(705)
            assertNotNull(findLoc(pos, locTypes.getValue(41021)))
            assertNull(findLoc(pos, type))
            assertTrue(player.count(dust) > 0)
            assertEquals(player.count(dust) * 80, player.statMap.getFineXP(stats.mining) - initial)
            assertEquals(
                300 - player.count(dust) / 2,
                player.vars[cacheTypes.varbits.getValue(15351)],
            )
            val board = placeMapLoc(CoordGrid(2441, 3155), locTypes.getValue(41230))
            player.telejump(board.coords.translate(0, -1))
            player.opLoc1(board)
            advance(2)
            assertModalOpen(cacheTypes.interfaces.getValue(47))
        }

    @Test
    fun GameTestState.dusuriSellsAndRepurchasesWithStardust() =
        runInjectedGameTest(
            ShopOperationMap::class,
            ShopsModule(),
            Dusuri::class,
            ShopScript::class,
        ) { _ ->
            player.members = true
            val npc = spawnNpc(CoordGrid(3022, 3342), npcTypes.getValue(10633))
            player.telejump(npc.coords.translate(-1, 0))
            player.opNpc1(npc)
            advance(2)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            advance()
            val shop = requireNotNull(player.openedShop).inv
            val slot = shop.indexOfFirst { it?.id == 25539 }
            assertTrue(slot >= 0)
            val dust = objTypes.getValue(25527)
            val ring = objTypes.getValue(25539)
            player.inv[0] = InvObj(dust, 2000)
            player.ifButton(
                ShopComponents.shop_inv,
                comsub = slot + 1,
                obj = ring.id,
                op = IfButtonOp.Op2,
            )
            advance()
            assertEquals(1, player.count(ring))
            assertEquals(0, player.count(dust))
            val bagSlot = player.inv.indexOfFirst { it?.id == ring.id }
            player.ifButton(
                ShopComponents.shop_side_inv,
                comsub = bagSlot,
                obj = ring.id,
                op = IfButtonOp.Op2,
            )
            advance()
            assertEquals(0, player.count(ring))
            assertEquals(1600, player.count(dust))
        }
}
