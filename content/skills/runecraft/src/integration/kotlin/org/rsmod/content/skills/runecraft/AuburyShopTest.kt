package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.ShopsModule
import org.rsmod.api.shops.config.ShopComponents
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class AuburyShopTest {
    @Test
    fun GameTestState
        .`Aubury sells rune packs and selects a separate cape stock by permanent mastery`() =
        runInjectedGameTest(
            ShopOperationMap::class,
            ShopsModule(),
            ShopScript::class,
            AuburyShop::class,
        ) { _ ->
            player.members = true
            player.teleport(CoordGrid(3253, 3402))
            val npc = spawnNpc(CoordGrid(3253, 3403), npcTypes.getValue(2886))
            fun open() {
                player.opNpc3(npc)
                advance(2)
            }
            fun buy(id: Int) {
                val stock = player.openedShop!!.inv
                val slot = stock.indexOfFirst { it?.id == id }
                assertTrue(slot >= 0)
                player.ifButton(
                    ShopComponents.shop_inv,
                    comsub = slot + 1,
                    obj = id,
                    op = IfButtonOp.Op2,
                )
                advance()
            }
            open()
            assertEquals(5, player.openedShop!!.inv.type.id)
            val pack = objTypes.getValue(12728)
            player.inv[0] = InvObj(objs.coins, 2000)
            buy(pack.id)
            assertEquals(1, player.count(pack))
            player.ifClose()
            advance(2)
            val slot = player.inv.indexOfFirst { it?.id == pack.id }
            player.withProtectedAccess { opHeld1(slot) }
            advance()
            assertEquals(100, player.count(objs.air_rune))
            player.setBaseLevel(stats.runecrafting, 99)
            open()
            assertEquals(459, player.openedShop!!.inv.type.id)
            buy(objs.runecrafting_skillcape.id)
            assertEquals(0, player.count(objs.runecrafting_hood))
            player.inv[0] = InvObj(objs.coins, 99_000)
            buy(objs.runecrafting_skillcape.id)
            assertEquals(0, player.count(objs.coins))
            assertEquals(1, player.count(objs.runecrafting_skillcape))
            assertEquals(1, player.count(objs.runecrafting_hood))
            player.ifClose()
            advance()
            player.setBaseLevel(stats.mining, 99)
            open()
            assertEquals(460, player.openedShop!!.inv.type.id)
            assertTrue(player.openedShop!!.inv.any { it?.id == objs.runecrafting_skillcape_t.id })
        }
}
