package org.rsmod.content.skills.crafting.jewellery

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.smithing.smelting.FurnaceKind
import org.rsmod.content.skills.smithing.smelting.SmeltingModule
import org.rsmod.content.skills.smithing.smelting.furnace_locs
import org.rsmod.content.skills.smithing.smelting.furnace_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class GoldJewelleryCraftingTest {
    @Test
    fun GameTestState.`cache 233 gold interface exposes the exact ordinary recipe contract`() =
        runJewelleryTest { deps ->
            assertEquals(446, interfaces.crafting_gold.id)
            assertEquals(32, deps.recipes.recipes.size)
            assertEquals(
                setOf(
                    8,
                    9,
                    10,
                    11,
                    12,
                    13,
                    14,
                    15,
                    23,
                    24,
                    25,
                    26,
                    27,
                    28,
                    29,
                    30,
                    37,
                    38,
                    39,
                    40,
                    41,
                    42,
                    43,
                    44,
                    50,
                    52,
                    53,
                    54,
                    55,
                    56,
                    57,
                    58,
                ),
                deps.recipes.recipes.mapTo(mutableSetOf()) { it.component.component },
            )
            assertFalse(
                deps.recipes.recipes.any {
                    it.component.isType(components.crafting_gold_slayer_ring)
                }
            )
            assertEquals(
                listOf(
                    "Gold ring:5:15.0:false",
                    "Sapphire ring:20:40.0:false",
                    "Emerald ring:27:55.0:false",
                    "Ruby ring:34:70.0:false",
                    "Diamond ring:43:85.0:false",
                    "Dragonstone ring:55:100.0:true",
                    "Onyx ring:67:115.0:true",
                    "Zenyte ring:89:150.0:true",
                    "Gold necklace:6:20.0:false",
                    "Sapphire necklace:22:55.0:false",
                    "Emerald necklace:29:60.0:false",
                    "Ruby necklace:40:75.0:false",
                    "Diamond necklace:56:90.0:false",
                    "Dragonstone necklace:72:105.0:true",
                    "Onyx necklace:82:120.0:true",
                    "Zenyte necklace:92:165.0:true",
                    "Gold amulet (u):8:30.0:false",
                    "Sapphire amulet (u):24:65.0:false",
                    "Emerald amulet (u):31:70.0:false",
                    "Ruby amulet (u):50:85.0:false",
                    "Diamond amulet (u):70:100.0:false",
                    "Dragonstone amulet (u):80:150.0:true",
                    "Onyx amulet (u):90:165.0:true",
                    "Zenyte amulet (u):98:200.0:true",
                    "Gold bracelet:7:25.0:true",
                    "Sapphire bracelet:23:60.0:true",
                    "Emerald bracelet:30:65.0:true",
                    "Ruby bracelet:42:80.0:true",
                    "Diamond bracelet:58:95.0:true",
                    "Dragonstone bracelet:74:110.0:true",
                    "Onyx bracelet:84:125.0:true",
                    "Zenyte bracelet:95:180.0:true",
                ),
                deps.recipes.recipes.map { "${it.name}:${it.level}:${it.xp}:${it.members}" },
            )

            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.gold_bar)
            player.withProtectedAccess { deps.crafting.select(this, furnace, FurnaceKind.Standard) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 446 }
            assertEquals(components.mainmodal.id, open.destinationCombinedId)
            val events = client.filterIsInstance<IfSetEventsV2>().filter { it.interfaceId == 446 }
            assertEquals(32, events.size)
            assertEquals(
                deps.recipes.recipes.mapTo(mutableSetOf()) { it.component.component },
                events.mapTo(mutableSetOf()) { it.componentId },
            )
            assertTrue(events.all { it.start == 1 && it.end == 28 && it.events1 == 1 })
        }

    @Test
    fun GameTestState.`all thirty-two ordinary recipes consume their exact inputs and award xp`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.mould)
                add(objs.gold_bar)
                recipe.gem?.let { add(it) }
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.crafting.process(
                        this,
                        GoldJewelleryRequest(
                            furnace,
                            FurnaceKind.Standard,
                            recipe.output,
                            remaining = 1,
                        ),
                    )
                }

                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(0, player.count(objs.gold_bar), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
                recipe.gem?.let { assertEquals(0, player.count(it), recipe.name) }
                assertEquals(1, player.count(recipe.mould), recipe.name)
            }
        }

    @Test
    fun GameTestState.`selected sapphire ring batches every three ticks`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val recipe = requireNotNull(deps.recipes.forOutput(objs.sapphire_ring))
            preparePlayer()
            add(recipe.mould)
            add(objs.gold_bar, 2)
            add(objs.sapphire, 2)
            val startXp = player.statMap.getFineXP(stats.crafting)

            select(deps, furnace, recipe, quantity = 2)
            advance(2)
            assertEquals(0, player.count(objs.sapphire_ring))
            advance()
            assertEquals(1, player.count(objs.sapphire_ring))
            assertEquals(400, player.statMap.getFineXP(stats.crafting) - startXp)
            advance(2)
            assertEquals(1, player.count(objs.sapphire_ring))
            advance()
            assertEquals(2, player.count(objs.sapphire_ring))
            assertEquals(800, player.statMap.getFineXP(stats.crafting) - startXp)
            assertEquals(0, player.weakQueueList.count(furnace_queues.goldJewellery))
        }

    @Test
    fun GameTestState.`membership level mould gem and gold-bar failures are atomic`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val bracelet = requireNotNull(deps.recipes.forOutput(objs.gold_bracelet))
            preparePlayer()
            player.members = false
            add(bracelet.mould)
            add(objs.gold_bar)
            select(deps, furnace, bracelet)
            assertMessageSent("You need to be on a members' world to craft this jewellery.")
            assertEquals(1, player.count(objs.gold_bar))

            val diamond = requireNotNull(deps.recipes.forOutput(objs.diamond_ring))
            preparePlayer()
            player.stats[stats.crafting] = diamond.level - 1
            add(diamond.mould)
            add(objs.gold_bar)
            add(objs.diamond)
            select(deps, furnace, diamond)
            assertMessageSent("You need a Crafting level of 43 to make this item.")
            assertEquals(1, player.count(objs.gold_bar))
            assertEquals(1, player.count(objs.diamond))

            preparePlayer()
            add(objs.gold_bar)
            add(objs.diamond)
            select(deps, furnace, diamond)
            assertMessageSent("You need a ring mould to make this item.")

            preparePlayer()
            add(diamond.mould)
            add(objs.gold_bar)
            select(deps, furnace, diamond)
            assertMessageSent("You need a cut diamond to make this item.")

            preparePlayer()
            add(diamond.mould)
            add(objs.diamond)
            player.withProtectedAccess { deps.crafting.select(this, furnace, FurnaceKind.Standard) }
            assertMessageSent("You need a gold bar to craft gold jewellery.")
            assertEquals(1, player.count(objs.diamond))
        }

    @Test
    fun GameTestState.`free-to-play recipes work while member-only tiers remain gated`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val rubyRing = requireNotNull(deps.recipes.forOutput(objs.ruby_ring))
            preparePlayer()
            player.members = false
            add(rubyRing.mould)
            add(objs.gold_bar)
            add(objs.ruby)
            select(deps, furnace, rubyRing)
            advance(3)
            assertEquals(1, player.count(objs.ruby_ring))

            val dragonstoneRing = requireNotNull(deps.recipes.forOutput(objs.dragonstone_ring))
            preparePlayer()
            player.members = false
            add(dragonstoneRing.mould)
            add(objs.gold_bar)
            add(objs.dragonstone)
            select(deps, furnace, dragonstoneRing)
            assertEquals(0, player.count(objs.dragonstone_ring))
            assertEquals(1, player.count(objs.gold_bar))
        }

    @Test
    fun GameTestState.`Shilo charges per item elite diary waives it and Lovakite rejects`() =
        runJewelleryTest { deps ->
            val recipe = requireNotNull(deps.recipes.forOutput(objs.gold_ring))
            val shilo = prepareFurnace(furnace_locs.shilo_village)
            preparePlayer()
            add(recipe.mould)
            add(objs.gold_bar, 2)
            add(objs.coins, 40)
            select(deps, shilo, recipe, quantity = 2, FurnaceKind.ShiloVillage)
            advance(6)
            assertEquals(2, player.count(objs.gold_ring))
            assertEquals(0, player.count(objs.coins))

            preparePlayer()
            player.setVarBit(varbits.karamja_diary_elite_complete, 1)
            add(recipe.mould)
            add(objs.gold_bar)
            select(deps, shilo, recipe, kind = FurnaceKind.ShiloVillage)
            advance(3)
            assertEquals(1, player.count(objs.gold_ring))

            preparePlayer()
            add(recipe.mould)
            add(objs.gold_bar)
            player.withProtectedAccess {
                deps.crafting.select(this, shilo, FurnaceKind.ShiloVillage)
            }
            player.resumeActiveCoroutine(ResumePauseButtonInput(recipe.component, 1))
            assertMessageSent("You need 20 coins to use this furnace.")
            assertEquals(1, player.count(objs.gold_bar))

            val lovakite = prepareFurnace(furnace_locs.lovakite, offset = 4)
            preparePlayer()
            add(recipe.mould)
            add(objs.gold_bar)
            player.withProtectedAccess {
                deps.crafting.select(this, lovakite, FurnaceKind.Lovakite)
            }
            assertMessageSent(
                "This furnace is not at the right temperature to craft gold jewellery."
            )
            assertEquals(1, player.count(objs.gold_bar))
        }

    @Test
    fun GameTestState.`movement and material changes stop a batch without duplication`() =
        runJewelleryTest { deps ->
            val recipe = requireNotNull(deps.recipes.forOutput(objs.gold_ring))
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(recipe.mould)
            add(objs.gold_bar, 2)
            select(deps, furnace, recipe, quantity = 2)
            advance(3)
            assertEquals(1, player.count(objs.gold_ring))

            player.placeAt(furnace.coords.translateX(4))
            advance(3)
            assertEquals(1, player.count(objs.gold_ring))
            assertEquals(1, player.count(objs.gold_bar))
            assertEquals(0, player.weakQueueList.count(furnace_queues.goldJewellery))

            player.placeAt(furnace.coords.translateX(-1))
            select(deps, furnace, recipe)
            player.inv[player.inv.indexOfFirst { it?.id == objs.gold_bar.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.gold_ring))
        }

    private fun GameTestScope.select(
        deps: Dependencies,
        furnace: BoundLocInfo,
        recipe: GoldJewelleryRecipe,
        quantity: Int = 1,
        kind: FurnaceKind = FurnaceKind.Standard,
    ) {
        player.withProtectedAccess { deps.crafting.select(this, furnace, kind) }
        player.resumeActiveCoroutine(ResumePauseButtonInput(recipe.component, quantity))
    }

    private fun GameTestScope.prepareFurnace(
        type: org.rsmod.game.type.loc.LocType,
        offset: Int = 0,
    ): BoundLocInfo {
        val furnace = placeMapLoc(CoordGrid(0, 50, 50, 34 + offset, 31), type)
        player.placeAt(furnace.coords.translateX(-1))
        return furnace
    }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
        player.setVarBit(varbits.karamja_diary_elite_complete, 0)
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

    private fun GameTestState.runJewelleryTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            GoldJewelleryScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SmeltingModule())
            install(JewelleryModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val crafting: GoldJewelleryCrafting, val recipes: GoldJewelleryRecipeRegistry)
}
