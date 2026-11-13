package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.ShopsModule
import org.rsmod.api.shops.config.ShopComponents
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.spell.utility.standard.StaminaEffectRepository
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid

class GraceTest {
    @Test
    fun GameTestState
        .`Grace trades ordinary graceful for marks and her amylase packs open into 100 crystals`() =
        runInjectedGameTest(
            ShopOperationMap::class,
            ShopsModule(),
            Grace::class,
            ShopScript::class,
        ) { _ ->
            val t = CoordGrid(3050, 4961, 1)
            player.teleport(t.translateX(1))
            player.members = true
            val grace = spawnNpc(t, npcTypes.getValue(5919))
            player.inv[0] = InvObj(objTypes.getValue(11849), 34)
            player.opNpc3(grace)
            advance(3)
            val shop = player.openedShop!!.inv
            val hood = objs.graceful_hood_inv
            fun buy(slot: Int) {
                player.ifButton(
                    ShopComponents.shop_inv,
                    comsub = slot + 1,
                    obj = shop[slot]!!.id,
                    op = IfButtonOp.Op2,
                )
                advance(1)
            }
            buy(0)
            assertEquals(0, player.count(hood))
            player.inv[0] = InvObj(objTypes.getValue(11849), 45)
            buy(0)
            assertEquals(1, player.count(hood))
            buy(6)
            assertEquals(1, player.count(objTypes.getValue(12641)))
            val hoodSlot = player.inv.indexOfFirst { it?.id == hood.id }
            player.ifButton(
                ShopComponents.shop_side_inv,
                comsub = hoodSlot,
                obj = hood.id,
                op = IfButtonOp.Op2,
            )
            advance()
            assertEquals(28, player.count(objTypes.getValue(11849)))
            assertEquals(0, player.count(hood))
            val pack = player.inv.indexOfFirst { it?.id == 12641 }
            player.ifClose()
            advance(2)
            player.withProtectedAccess { opHeld1(pack) }
            assertEquals(100, player.count(objTypes.getValue(12640)))
            player.opNpc5(grace)
            advance(2)
            assertEquals(1, player.vars[cacheTypes.varps.getValue(65750)])
            player.opNpc4(grace)
            advance(2)
            assertTrue(player.isAccessProtected)
        }

    data class CapeDeps
    @Inject
    constructor(val worn: WornInteractions, val stamina: StaminaEffectRepository)

    @Test
    fun GameTestState
        .`worn Agility cape transforms and shares a permanent daily stamina boost with the carried cape`() =
        runInjectedGameTest(CapeDeps::class, null, AgilityCape::class) { deps ->
            player.members = true
            player.setBaseLevel(stats.agility, 99)
            player.setCurrentLevel(stats.agility, 99)
            player.inv[0] = InvObj(objs.agility_skillcape)
            player.inv[1] = InvObj(objs.agility_skillcape_t)
            player.withProtectedAccess { invEquip(0) }
            assertEquals(13340, player.worn[Wearpos.Back.slot]?.id)
            player.runEnergy = 50
            player.withProtectedAccess {
                deps.worn.interact(this, player.worn, Wearpos.Back.slot, IfButtonOp.Op3)
            }
            assertEquals(10000, player.runEnergy)
            assertEquals(100, deps.stamina.remainingCycles(player))
            assertEquals(1, player.vars[varbits.stamina_active])
            player.runEnergy = 400
            player.withProtectedAccess { opHeld3(1) }
            assertEquals(400, player.runEnergy)
            player.setVarp(cacheTypes.varps.getValue(65751), 1)
            player.withProtectedAccess { opHeld3(1) }
            assertEquals(10000, player.runEnergy)
        }
}
