package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class TitheRewardsTest {
    data class Deps
    @Inject
    constructor(
        val patches: FarmingPatchRepository,
        val bits: VarBitTypeList,
        val vars: VarpTypeList,
    )

    @Test
    fun GameTestState
        .`Tithe points buy one can that fills at a barrel and waters an actual seedling`() =
        runInjectedGameTest(
            Deps::class,
            null,
            TitheRewards::class,
            FarmingShops::class,
            TreeSeedlings::class,
        ) { deps ->
            val tile = CoordGrid(1800, 3502)
            val farmer =
                spawnNpc(
                    tile,
                    npcTypes.values.single { it.internalName == "hosidius_tithe_farmer" },
                )
            player.members = true
            player.teleport(tile.translateX(-1))
            fun reward(index: Int) {
                player.opNpc3(farmer)
                advance(3)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, index))
            }
            val points = deps.bits[4893]!!
            val doses = deps.vars[65632]!!
            player.setVarBit(points, 199)
            reward(12)
            assertEquals(0, player.count(objs.gricollers_can))
            player.setVarBit(points, 400)
            reward(12)
            reward(12)
            assertEquals(200, player.vars[points])
            assertEquals(1, player.count(objs.gricollers_can))
            assertEquals(0, player.vars[doses])
            advance(2)
            val source = locTypes.values.single { it.internalName == "farm_water_barrel1" }
            val barrel = placeMapLoc(tile.translateZ(1), source)
            player.clearPendingAction()
            val slot = player.inv.mapSlots { _, obj -> obj?.id == objs.gricollers_can.id }.single()
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        LocUEvents.Op(barrel, barrel, source, objTypes[objs.gricollers_can], slot),
                    )
                )
            }
            assertEquals(1000, player.vars[doses])
            val dry = objTypes.values.single { it.internalName == "plantpot_acorn" }
            val wet = objTypes.values.single { it.internalName == "plantpot_acorn_watered" }
            player.inv[5] = InvObj(dry)
            player.withProtectedAccess {
                check(
                    eventBus.publish(
                        this,
                        HeldUEvents.Type(objTypes[objs.gricollers_can], slot, dry, 5),
                    )
                )
            }
            assertEquals(1, player.count(wet))
            assertEquals(999, player.vars[doses])
            reward(13)
            assertEquals(1000, player.vars[doses])
            assertEquals(200, player.vars[points])
            val patch =
                deps.patches.getOrCreate(player, "test:allotment", FarmingPatchType.Allotment)
            patch.weeds = 1
            reward(14)
            reward(14)
            assertEquals(150, player.vars[points])
            deps.patches.advance(player, patch, 600000)
            deps.patches.advance(player, patch, 1800000)
            assertEquals(1, patch.weeds)
            reward(15)
            deps.patches.advance(player, patch, 1800000)
            deps.patches.advance(player, patch, 3000000)
            assertEquals(3, patch.weeds)
        }

    @Test
    fun GameTestState
        .`Bologa requires Zamorak equipment and Prayer before selling grape blessings`() =
        runInjectedGameTest(Deps::class, null, TitheRewards::class) { deps ->
            player.members = true
            val at = CoordGrid(1800, 3502)
            val farmer =
                spawnNpc(at, npcTypes.values.single { it.internalName == "hosidius_tithe_farmer" })
            val bologa =
                spawnNpc(
                    at.translateX(2),
                    npcTypes.values.single { it.internalName == "hosidius_evilgrapegirl" },
                )
            val blessings = objTypes.values.single { it.internalName == "grape_blessing" }
            player.teleport(at.translateX(1))
            player.setVarBit(deps.bits[4893]!!, 1)
            player.inv[0] = InvObj(objs.coins, 75000)
            fun shop() {
                player.opNpc3(farmer)
                advance(3)
                player.resumeActiveCoroutine(ResumePauseButtonInput(components.menu_list, 7))
            }
            shop()
            assertEquals(0, player.count(blessings))
            player.setBaseLevel(stats.prayer, 50)
            player.opNpc1(bologa)
            advance(3)
            assertEquals(75000, player.count(objs.coins))
            player.worn[1] = InvObj(objTypes.values.single { it.internalName == "zamorak_cape" })
            player.opNpc1(bologa)
            advance(3)
            player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, 1))
            assertEquals(0, player.count(objs.coins))
            shop()
            assertEquals(20, player.count(blessings))
            assertEquals(0, player.vars[deps.bits[4893]!!])
        }
}
