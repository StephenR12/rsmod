package org.rsmod.content.skills.crafting.tanning

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.interfaces.IfSetObject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentTypeList
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class TanningTest {
    @Test
    fun GameTestState.`cache 233 exposes the native tannery interface objects and trade NPCs`() =
        runTanningTest { deps ->
            assertEquals(324, interfaces.tanner.id)
            assertEquals(
                listOf(1739, 1739, 6287, 7801, 1753, 1751, 1749, 1747),
                deps.recipes.recipes.map { it.input.id },
            )
            assertEquals(
                listOf(1741, 1743, 6289, 6289, 1745, 2505, 2507, 2509),
                deps.recipes.recipes.map { it.output.id },
            )
            assertEquals(
                listOf(3231, 5809, 6058, 6526, 8711, 14662),
                listOf(
                        npcs.ellis_tanner,
                        npcs.tanner,
                        npcs.ranging_guild_leatherworker,
                        npcs.werewolf_tanner,
                        npcs.eodan,
                        npcs.auburn_tanner,
                    )
                    .map { it.id },
            )
            assertEquals(
                listOf(
                    listOf("Talk-to", null, "Trade", null, null),
                    listOf("Talk-to", null, "Trade", null, null),
                    listOf("Talk-to", null, null, null, null),
                    listOf("Talk-to", null, "Trade", null, null),
                    listOf("Talk-to", null, "Tan-hides", null, null),
                    listOf("Talk-to", null, "Trade", null, null),
                ),
                listOf(
                        npcs.ellis_tanner,
                        npcs.tanner,
                        npcs.ranging_guild_leatherworker,
                        npcs.werewolf_tanner,
                        npcs.eodan,
                        npcs.auburn_tanner,
                    )
                    .map { npcTypes[it].op.toList() },
            )
            val recipes = deps.recipes.recipes
            assertEquals((100..107).toList(), recipes.map { it.modelComponent.component })
            assertEquals((108..115).toList(), recipes.map { it.textComponent.component })
            assertEquals((116..123).toList(), recipes.map { it.priceComponent.component })
            assertTrue(recipes.all { deps.componentTypes[it.modelComponent].type == 6 })
            assertTrue(recipes.all { deps.componentTypes[it.textComponent].text == "Hide" })
            assertTrue(recipes.all { deps.componentTypes[it.priceComponent].text == "Cost" })
            assertEquals(
                List(8) { "Tan <col=ff7000>All" } +
                    List(8) { "Tan <col=ff7000>X" } +
                    List(8) { "Tan <col=ff7000>5" } +
                    List(8) { "Tan <col=ff7000>1" },
                listOf(
                        recipes.map { it.buttons.all },
                        recipes.map { it.buttons.x },
                        recipes.map { it.buttons.five },
                        recipes.map { it.buttons.one },
                    )
                    .flatten()
                    .map { deps.componentTypes[it].buttonText },
            )
        }

    @Test
    fun GameTestState.`recipe table pins all cache slots membership and three price schedules`() =
        runTanningTest { deps ->
            assertEquals(
                listOf(
                    "Soft leather:1739:1741:false:1:2:10:100:108:116:124:132:140:148",
                    "Hard leather:1739:1743:false:3:5:30:101:109:117:125:133:141:149",
                    "Snakeskin:6287:6289:true:15:25:150:102:110:118:126:134:142:150",
                    "Swamp snakeskin:7801:6289:true:20:45:200:103:111:119:127:135:143:151",
                    "Green dragon leather:1753:1745:true:20:45:200:104:112:120:128:136:144:152",
                    "Blue dragon leather:1751:2505:true:20:45:200:105:113:121:129:137:145:153",
                    "Red dragon leather:1749:2507:true:20:45:200:106:114:122:130:138:146:154",
                    "Black dragon leather:1747:2509:true:20:45:200:107:115:123:131:139:147:155",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.input.id}:${it.output.id}:${it.members}:${it.standardPrice}:${it.premiumPrice}:${it.eodanPrice}:${it.modelComponent.component}:${it.textComponent.component}:${it.priceComponent.component}:${it.buttons.all.component}:${it.buttons.x.component}:${it.buttons.five.component}:${it.buttons.one.component}"
                },
            )
        }

    @Test
    fun GameTestState
        .`opening a standard tanner configures the native interface prices and buttons`() =
        runTanningTest { deps ->
            preparePlayer()
            add(objs.cow_hide)
            add(objs.coins)
            player.withProtectedAccess { deps.tanning.open(this, TannerPricing.Standard) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 324 })
            assertEquals(
                listOf(1741, 1743, 6289, 6289, 1745, 2505, 2507, 2509),
                client.filterIsInstance<IfSetObject>().map { it.obj },
            )
            assertTrue(client.filterIsInstance<IfSetObject>().all { it.count == 1 })
            assertEquals(
                listOf(
                    "Soft leather",
                    "1 Coin",
                    "Hard leather",
                    "3 Coins",
                    "Snakeskin",
                    "15 Coins",
                    "Swamp snakeskin",
                    "20 Coins",
                    "Green dragon leather",
                    "20 Coins",
                    "Blue dragon leather",
                    "20 Coins",
                    "Red dragon leather",
                    "20 Coins",
                    "Black dragon leather",
                    "20 Coins",
                ),
                client.filterIsInstance<IfSetText>().map { it.text },
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 324 && it.componentId in 124..155
                }
            assertEquals(32, events.size)
            assertTrue(events.all { it.start == 0 && it.end == 0 })

            player.ifButton(deps.recipes.recipes.first().buttons.one, comsub = 0)
            advance()
            assertEquals(1, player.count(objs.leather))
        }

    @Test
    fun GameTestState.`Ranging Guild Leatherworker exposes tanning through dialogue only`() =
        runTanningTest { deps ->
            preparePlayer()
            add(objs.cow_hide)
            add(objs.coins)
            val npc =
                spawnNpc(player.coords.translate(1, 0), npcTypes[npcs.ranging_guild_leatherworker])

            player.opNpc1(npc)
            advance()
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chat_right_pbutton, subcomponent = -1)
            )
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chat_left_pbutton, subcomponent = -1)
            )
            player.resumeActiveCoroutine(
                ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = 1)
            )

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 324 })
            assertTrue(player.ui.containsModal(interfaces.tanner))
            advance()
            player.ifButton(deps.recipes.recipes.first().buttons.one, comsub = 0)
            advance()
            assertEquals(0, player.count(objs.cow_hide))
            assertEquals(1, player.count(objs.leather))
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`standard and premium prices match every wiki recipe`() =
        runTanningTest { deps ->
            assertEquals(
                listOf(1, 3, 15, 20, 20, 20, 20, 20),
                deps.recipes.recipes.map { deps.tanning.price(it, TannerPricing.Standard) },
            )
            assertEquals(
                listOf(2, 5, 25, 45, 45, 45, 45, 45),
                deps.recipes.recipes.map { deps.tanning.price(it, TannerPricing.Premium) },
            )
        }

    @Test
    fun GameTestState.`Eodan applies the exact highest completed Kourend diary discount`() =
        runTanningTest { deps ->
            val expected =
                listOf(
                    listOf(10, 30, 150, 200, 200, 200, 200, 200),
                    listOf(8, 24, 120, 160, 160, 160, 160, 160),
                    listOf(6, 18, 90, 120, 120, 120, 120, 120),
                    listOf(4, 12, 60, 80, 80, 80, 80, 80),
                    listOf(2, 6, 30, 40, 40, 40, 40, 40),
                )
            val tiers =
                listOf(
                    null,
                    varbits.kourend_diary_easy_complete,
                    varbits.kourend_diary_medium_complete,
                    varbits.kourend_diary_hard_complete,
                    varbits.kourend_diary_elite_complete,
                )
            for ((index, tier) in tiers.withIndex()) {
                player.setVarBit(varbits.kourend_diary_easy_complete, 0)
                player.setVarBit(varbits.kourend_diary_medium_complete, 0)
                player.setVarBit(varbits.kourend_diary_hard_complete, 0)
                player.setVarBit(varbits.kourend_diary_elite_complete, 0)
                if (tier != null) {
                    player.setVarBit(tier, 1)
                }
                assertEquals(
                    expected[index],
                    deps.recipes.recipes.map { deps.tanning.price(it, TannerPricing.Eodan, player) },
                )
            }
            player.setVarBit(varbits.kourend_diary_easy_complete, 1)
            player.setVarBit(varbits.kourend_diary_elite_complete, 1)
            assertEquals(
                2,
                deps.tanning.price(deps.recipes.recipes.first(), TannerPricing.Eodan, player),
            )
        }

    @Test
    fun GameTestState
        .`all eight recipes convert a full inventory immediately and charge exactly`() =
        runTanningTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                repeat(27) { add(recipe.input) }
                add(objs.coins, recipe.standardPrice * 27)
                player.withProtectedAccess {
                    deps.tanning.open(this, TannerPricing.Standard)
                    deps.tanning.tan(this, recipe, Int.MAX_VALUE)
                }

                assertEquals(0, player.count(recipe.input), recipe.name)
                assertEquals(27, player.count(recipe.output), recipe.name)
                assertEquals(0, player.count(objs.coins), recipe.name)
                assertMessageSent("The tanner tans your hides.")
                player.ifClose()
            }
        }

    @Test
    fun GameTestState.`requested quantity is capped by hides and affordability`() =
        runTanningTest { deps ->
            preparePlayer()
            val hardLeather = deps.recipes.recipes[1]
            add(objs.cow_hide, 10)
            add(objs.coins, 9)
            player.withProtectedAccess {
                deps.tanning.open(this, TannerPricing.Standard)
                deps.tanning.tan(this, hardLeather, 5)
            }

            assertEquals(7, player.count(objs.cow_hide))
            assertEquals(3, player.count(objs.hard_leather))
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState
        .`membership missing hides and missing coins never partially mutate inventory`() =
        runTanningTest { deps ->
            val green = deps.recipes.recipes[4]
            preparePlayer()
            player.members = false
            add(objs.green_dragonhide)
            add(objs.coins, 20)
            player.withProtectedAccess {
                deps.tanning.open(this, TannerPricing.Standard)
                deps.tanning.tan(this, green, 1)
            }
            assertMessageSent("You need to be on a members' world to tan that hide.")
            assertEquals(1, player.count(objs.green_dragonhide))
            assertEquals(20, player.count(objs.coins))
            player.ifClose()

            preparePlayer()
            player.withProtectedAccess {
                deps.tanning.open(this, TannerPricing.Standard)
                deps.tanning.tan(this, deps.recipes.recipes.first(), 1)
            }
            assertMessageSent("You don't have any suitable hides to tan.")
            player.ifClose()

            preparePlayer()
            add(objs.cow_hide)
            player.withProtectedAccess {
                deps.tanning.open(this, TannerPricing.Standard)
                deps.tanning.tan(this, deps.recipes.recipes.first(), 1)
            }
            assertMessageSent("You don't have enough coins to pay for tanning.")
            assertEquals(1, player.count(objs.cow_hide))
        }

    @Test
    fun GameTestState.`closing the tannery invalidates stale interface actions`() =
        runTanningTest { deps ->
            preparePlayer()
            add(objs.cow_hide)
            add(objs.coins)
            player.withProtectedAccess { deps.tanning.open(this, TannerPricing.Standard) }
            deps.tanning.close(player)

            player.withProtectedAccess { deps.tanning.tan(this, deps.recipes.recipes.first(), 1) }

            assertEquals(1, player.count(objs.cow_hide))
            assertEquals(0, player.count(objs.leather))
            assertEquals(1, player.count(objs.coins))
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

    private fun GameTestState.runTanningTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, TanningScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(TanningModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val tanning: Tanning,
        val recipes: TanningRecipeRegistry,
        val componentTypes: ComponentTypeList,
    )
}
