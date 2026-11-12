package org.rsmod.content.skills.crafting.customfur

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import net.rsprot.protocol.game.outgoing.inv.UpdateInvFull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class CustomFurClothingTest {
    @Test
    fun GameTestState.`cache 233 exposes Asyff and the native custom fur interface inventory`() =
        runCustomFurTest { deps ->
            assertEquals(2887, npcs.asyff.id)
            assertEquals(
                listOf("Talk-to", null, "Trade", "Fur Clothing", null),
                npcTypes[npcs.asyff].op.toList(),
            )
            assertEquals(477, interfaces.hunting_customfurs.id)
            assertEquals(6, components.hunting_customfurs_choices_items.component)
            assertEquals(482, invs.hunting_customfurshop.id)

            preparePlayer()
            player.withProtectedAccess { deps.service.open(this) }
            val display = requireNotNull(deps.service.display(player))
            assertEquals(20, display.size)
            assertEquals(
                listOf(
                    10035,
                    10037,
                    10039,
                    10041,
                    10043,
                    10045,
                    10047,
                    10049,
                    10051,
                    10053,
                    10055,
                    10057,
                    10059,
                    10061,
                    10063,
                    10065,
                    10067,
                    10069,
                    10071,
                    10075,
                ),
                display.map { it?.id },
            )
        }

    @Test
    fun GameTestState.`recipe table pins all twenty wiki fees and fur alternatives`() =
        runCustomFurTest { deps ->
            assertEquals(
                listOf(
                    "Kyatt legs:10035:10101,10103x1:200",
                    "Kyatt top:10037:10101,10103x1:200",
                    "Kyatt hat:10039:10103x1:1000",
                    "Larupia legs:10041:10093,10095x1:100",
                    "Larupia top:10043:10093,10095x1:100",
                    "Larupia hat:10045:10095x1:500",
                    "Graahk legs:10047:10097,10099x1:150",
                    "Graahk top:10049:10097,10099x1:150",
                    "Graahk headdress:10051:10099x1:750",
                    "Wood camo top:10053:10121x2:20",
                    "Wood camo legs:10055:10121x2:20",
                    "Jungle camo top:10057:10119x2:20",
                    "Jungle camo legs:10059:10119x2:20",
                    "Desert camo top:10061:10123x2:20",
                    "Desert camo legs:10063:10123x2:20",
                    "Polar camo top:10065:10117x2:20",
                    "Polar camo legs:10067:10117x2:20",
                    "Spotted cape:10069:10125x2:400",
                    "Spottier cape:10071:10127x2:800",
                    "Gloves of silence:10075:10115x2:600",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.output.id}:${it.furs.joinToString(",") { fur -> fur.id.toString() }}" +
                        "x${it.furCount}:${it.fee}"
                },
            )
        }

    @Test
    fun GameTestState.`opening Asyff's service transmits and enables the cache-native panel`() =
        runCustomFurTest { deps ->
            preparePlayer()
            player.withProtectedAccess { deps.service.open(this) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 477 })
            val event =
                client.filterIsInstance<IfSetEventsV2>().single {
                    it.interfaceId == 477 && it.componentId == 6
                }
            assertEquals(0, event.start)
            assertEquals(19, event.end)
            assertTrue(player.ui.containsModal(interfaces.hunting_customfurs))
            advance()
            assertTrue(client.filterIsInstance<UpdateInvFull>().any { it.inventoryId == 482 })
        }

    @Test
    fun GameTestState.`all twenty displayed products consume the exact fee and fur`() =
        runCustomFurTest { deps ->
            for ((slot, recipe) in deps.recipes.recipes.withIndex()) {
                preparePlayer()
                val fur = recipe.furs.first()
                add(fur, recipe.furCount)
                add(objs.coins, recipe.fee)
                player.withProtectedAccess { deps.service.open(this) }
                player.ifButton(
                    components.hunting_customfurs_choices_items,
                    comsub = slot,
                    obj = recipe.output.id,
                )
                advance()

                assertEquals(0, player.count(fur), recipe.name)
                assertEquals(0, player.count(objs.coins), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                player.ifClose()
                advance()
            }
        }

    @Test
    fun GameTestState
        .`tatty fur is prioritised for bodies and legs while hats require perfect fur`() =
        runCustomFurTest { deps ->
            preparePlayer()
            val body = deps.recipes.forOutput(objs.larupia_top)!!
            add(objs.tatty_larupia_fur)
            add(objs.larupia_fur)
            add(objs.coins, body.fee)
            player.withProtectedAccess { deps.service.open(this) }
            player.ifButton(
                components.hunting_customfurs_choices_items,
                comsub = deps.recipes.recipes.indexOf(body),
                obj = body.output.id,
            )
            advance()
            assertEquals(0, player.count(objs.tatty_larupia_fur))
            assertEquals(1, player.count(objs.larupia_fur))
            assertEquals(1, player.count(objs.larupia_top))
            player.ifClose()
            advance()

            preparePlayer()
            val hat = deps.recipes.forOutput(objs.larupia_hat)!!
            add(objs.tatty_larupia_fur)
            add(objs.coins, hat.fee)
            player.withProtectedAccess { deps.service.open(this) }
            player.ifButton(
                components.hunting_customfurs_choices_items,
                comsub = deps.recipes.recipes.indexOf(hat),
                obj = hat.output.id,
            )
            advance()
            assertEquals(1, player.count(objs.tatty_larupia_fur))
            assertEquals(0, player.count(objs.larupia_hat))
            assertMessageSent("You don't have the required fur to make that item.")
        }

    @Test
    fun GameTestState.`full inventories convert atomically and missing resources do not mutate`() =
        runCustomFurTest { deps ->
            preparePlayer()
            val recipe = deps.recipes.forOutput(objs.polar_camo_top)!!
            add(recipe.furs.single(), recipe.furCount)
            add(objs.coins, recipe.fee)
            repeat(25) { add(objs.pot_empty) }
            assertTrue(player.inv.isFull())
            player.withProtectedAccess { deps.service.open(this) }
            player.ifButton(
                components.hunting_customfurs_choices_items,
                comsub = deps.recipes.recipes.indexOf(recipe),
                obj = recipe.output.id,
            )
            advance()
            assertEquals(1, player.count(recipe.output))
            assertEquals(0, player.count(recipe.furs.single()))
            assertEquals(0, player.count(objs.coins))
            player.ifClose()
            advance()

            preparePlayer()
            add(recipe.furs.single(), recipe.furCount)
            add(objs.coins, recipe.fee - 1)
            player.withProtectedAccess { deps.service.open(this) }
            player.ifButton(
                components.hunting_customfurs_choices_items,
                comsub = deps.recipes.recipes.indexOf(recipe),
                obj = recipe.output.id,
            )
            advance()
            assertEquals(recipe.furCount, player.count(recipe.furs.single()))
            assertEquals(recipe.fee - 1, player.count(objs.coins))
            assertEquals(0, player.count(recipe.output))
            assertMessageSent("You don't have enough coins to pay for that item.")
        }

    @Test
    fun GameTestState
        .`members checks spoof protection and closing prevent unauthorised conversion`() =
        runCustomFurTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            player.members = false
            add(recipe.furs.first(), recipe.furCount)
            add(objs.coins, recipe.fee)
            player.withProtectedAccess { deps.service.open(this) }
            assertFalse(player.ui.containsModal(interfaces.hunting_customfurs))
            assertEquals(null, deps.service.display(player))

            preparePlayer()
            add(recipe.furs.first(), recipe.furCount)
            add(objs.coins, recipe.fee)
            player.withProtectedAccess { deps.service.open(this) }
            player.ifButton(
                components.hunting_customfurs_choices_items,
                comsub = 1,
                obj = recipe.output.id,
            )
            advance()
            assertEquals(0, player.count(recipe.output))

            player.ifClose()
            advance()
            player.withProtectedAccess { deps.service.make(this, 0, recipe.output) }
            assertEquals(0, player.count(recipe.output))
            assertEquals(recipe.furCount, player.count(recipe.furs.first()))
            assertEquals(recipe.fee, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`Asyff right-click actions open the native shop and custom-fur panel`() =
        runCustomFurTest { _ ->
            preparePlayer()
            val asyff = spawnNpc(player.coords.translate(1, 0), npcTypes[npcs.asyff])

            player.opNpc3(asyff)
            advance()
            assertEquals(63, player.openedShop?.inv?.type?.id)
            player.ifClose()
            advance()

            player.opNpc4(asyff)
            advance()
            assertTrue(player.ui.containsModal(interfaces.hunting_customfurs))
        }

    @Test
    fun GameTestState.`Asyff is closed on non-members worlds`() = runCustomFurTest { _ ->
        preparePlayer()
        player.members = false
        val asyff = spawnNpc(player.coords.translate(1, 0), npcTypes[npcs.asyff])

        player.opNpc4(asyff)
        advance()

        assertTrue(
            client.filterIsInstance<IfSetText>().any {
                it.text == "Sorry, we're closed at the moment."
            }
        )
        assertFalse(player.ui.containsModal(interfaces.hunting_customfurs))
    }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runCustomFurTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            CustomFurClothingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(CustomFurClothingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val service: CustomFurClothing, val recipes: CustomFurRecipeRegistry)
}
