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
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SilverJewelleryCraftingTest {
    @Test
    fun GameTestState.`cache 233 silver interface exposes all twenty recipe contracts`() =
        runJewelleryTest { deps ->
            assertEquals(6, interfaces.silver_crafting.id)
            assertEquals(20, deps.recipes.recipes.size)
            assertEquals(
                setOf(7, 8, 9, 11, 12, 13, 15, 16, 17, 19, 20, 21, 23, 24, 25, 26, 27, 28, 29, 30),
                deps.recipes.recipes.mapTo(mutableSetOf()) { it.component.component },
            )
            assertEquals(
                listOf(
                    "Opal ring:1:10.0:1:true",
                    "Jade ring:13:32.0:1:true",
                    "Topaz ring:16:35.0:1:true",
                    "Opal necklace:16:35.0:1:true",
                    "Jade necklace:25:54.0:1:true",
                    "Topaz necklace:32:70.0:1:true",
                    "Opal amulet (u):27:55.0:1:true",
                    "Jade amulet (u):34:70.0:1:true",
                    "Topaz amulet (u):45:80.0:1:true",
                    "Opal bracelet:22:45.0:1:true",
                    "Jade bracelet:29:60.0:1:true",
                    "Topaz bracelet:38:75.0:1:true",
                    "Unstrung symbol:16:50.0:1:false",
                    "Unstrung emblem:17:50.0:1:true",
                    "Silver sickle:18:50.0:1:true",
                    "Conductor:20:50.0:1:true",
                    "Silver bolts (unf):21:50.0:10:true",
                    "Tiara:23:52.5:1:false",
                    "Silvthrill rod:25:55.0:1:true",
                    "Demonic sigil:30:50.0:1:true",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.xp}:${it.outputCount}:${it.members}"
                },
            )

            val init = deps.cacheTypes.clientscripts.getValue(1596)
            assertEquals(25, init.intArgumentCount)
            val operands = init.intOperands.toList()
            assertTrue(operands.windowed(3).contains(listOf(4201, 20, 4200)))
            assertTrue(operands.windowed(3).contains(listOf(7637, 25, 7649)))
            assertTrue(operands.windowed(3).contains(listOf(6748, 30, 6747)))
            assertFalse(operands.any { it == 13715 || it == 1372 || it == 1990 })

            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(objs.silver_bar)
            player.withProtectedAccess { deps.crafting.select(this, furnace, FurnaceKind.Standard) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 6 }
            assertEquals(components.mainmodal.id, open.destinationCombinedId)
            val events = client.filterIsInstance<IfSetEventsV2>().filter { it.interfaceId == 6 }
            assertEquals(20, events.size)
            assertEquals(
                deps.recipes.recipes.mapTo(mutableSetOf()) { it.component.component },
                events.mapTo(mutableSetOf()) { it.componentId },
            )
            assertTrue(events.all { it.start == 1 && it.end == 28 && it.events1 == 1 })
        }

    @Test
    fun GameTestState.`all twenty recipes consume exact inputs preserve moulds and award xp`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.mould)
                recipe.ingredients.forEach { add(it.obj, it.count) }
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.crafting.process(
                        this,
                        SilverCraftingRequest(
                            furnace,
                            FurnaceKind.Standard,
                            recipe.output,
                            remaining = 1,
                        ),
                    )
                }

                assertEquals(recipe.outputCount, player.count(recipe.output), recipe.name)
                recipe.ingredients.forEach { assertEquals(0, player.count(it.obj), recipe.name) }
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
                assertEquals(1, player.count(recipe.mould), recipe.name)
            }
        }

    @Test
    fun GameTestState
        .`special quest mould recipes have exact multi-material and repeat contracts`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val conductor = requireNotNull(deps.recipes.forOutput(objs.conductor))
            val rod = requireNotNull(deps.recipes.forOutput(objs.silvthrill_rod))
            val sigil = requireNotNull(deps.recipes.forOutput(objs.demonic_sigil))

            assertEquals(listOf(objs.silver_bar.id), conductor.ingredients.map { it.obj.id })
            assertEquals(
                listOf(objs.silver_bar.id, objs.mithril_bar.id, objs.sapphire.id),
                rod.ingredients.map { it.obj.id },
            )
            assertEquals(listOf(objs.silver_bar.id), sigil.ingredients.map { it.obj.id })

            preparePlayer()
            add(rod.mould)
            add(objs.silver_bar, 2)
            add(objs.mithril_bar, 2)
            add(objs.sapphire, 2)
            val startXp = player.statMap.getFineXP(stats.crafting)
            select(deps, furnace, rod, quantity = 2)
            advance(6)
            assertEquals(2, player.count(objs.silvthrill_rod))
            assertEquals(1, player.count(rod.mould))
            assertEquals(1100, player.statMap.getFineXP(stats.crafting) - startXp)

            preparePlayer()
            add(rod.mould)
            add(objs.silver_bar)
            add(objs.sapphire)
            select(deps, furnace, rod)
            assertMessageSent("You need a mithril bar to make this item.")
            assertEquals(1, player.count(objs.silver_bar))
            assertEquals(1, player.count(objs.sapphire))
        }

    @Test
    fun GameTestState.`selected silver bolts make ten per bar every three ticks`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val recipe = requireNotNull(deps.recipes.forOutput(objs.silver_bolts_unf))
            preparePlayer()
            add(recipe.mould)
            add(objs.silver_bar, 2)
            val startXp = player.statMap.getFineXP(stats.crafting)

            select(deps, furnace, recipe, quantity = 2)
            advance(2)
            assertEquals(0, player.count(objs.silver_bolts_unf))
            advance()
            assertEquals(10, player.count(objs.silver_bolts_unf))
            advance(2)
            assertEquals(10, player.count(objs.silver_bolts_unf))
            advance()
            assertEquals(20, player.count(objs.silver_bolts_unf))
            assertEquals(1000, player.statMap.getFineXP(stats.crafting) - startXp)
            assertEquals(0, player.weakQueueList.count(furnace_queues.silverJewellery))
        }

    @Test
    fun GameTestState.`membership level mould gem and silver-bar failures are atomic`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val opal = requireNotNull(deps.recipes.forOutput(objs.opal_ring))
            preparePlayer()
            player.members = false
            add(opal.mould)
            add(objs.silver_bar)
            add(objs.opal)
            select(deps, furnace, opal)
            assertMessageSent("You need to be on a members' world to craft this item.")
            assertEquals(1, player.count(objs.silver_bar))

            val topaz = requireNotNull(deps.recipes.forOutput(objs.topaz_ring))
            preparePlayer()
            player.stats[stats.crafting] = topaz.level - 1
            add(topaz.mould)
            add(objs.silver_bar)
            add(objs.red_topaz)
            select(deps, furnace, topaz)
            assertMessageSent("You need a Crafting level of 16 to make this item.")
            assertEquals(1, player.count(objs.red_topaz))

            preparePlayer()
            add(objs.silver_bar)
            add(objs.red_topaz)
            select(deps, furnace, topaz)
            assertMessageSent("You need a ring mould to make this item.")

            preparePlayer()
            add(topaz.mould)
            add(objs.silver_bar)
            select(deps, furnace, topaz)
            assertMessageSent("You need a cut red topaz to make this item.")

            preparePlayer()
            add(topaz.mould)
            add(objs.red_topaz)
            player.withProtectedAccess { deps.crafting.select(this, furnace, FurnaceKind.Standard) }
            assertMessageSent("You need a silver bar to craft silver jewellery.")
        }

    @Test
    fun GameTestState.`unstrung symbols and tiaras work on free worlds`() =
        runJewelleryTest { deps ->
            val furnace = prepareFurnace(furnace_locs.general)
            val symbol = requireNotNull(deps.recipes.forOutput(objs.unstrung_holy_symbol))
            preparePlayer()
            player.members = false
            add(symbol.mould)
            add(objs.silver_bar)
            select(deps, furnace, symbol)
            advance(3)
            assertEquals(1, player.count(objs.unstrung_holy_symbol))

            val tiara = requireNotNull(deps.recipes.forOutput(objs.tiara))
            preparePlayer()
            player.members = false
            add(tiara.mould)
            add(objs.silver_bar)
            select(deps, furnace, tiara)
            advance(3)
            assertEquals(1, player.count(objs.tiara))
        }

    @Test
    fun GameTestState.`Shilo charges per item elite diary waives it and Lovakite rejects`() =
        runJewelleryTest { deps ->
            val recipe = requireNotNull(deps.recipes.forOutput(objs.unstrung_holy_symbol))
            val shilo = prepareFurnace(furnace_locs.shilo_village)
            preparePlayer()
            add(recipe.mould)
            add(objs.silver_bar, 2)
            add(objs.coins, 40)
            select(deps, shilo, recipe, quantity = 2, kind = FurnaceKind.ShiloVillage)
            advance(6)
            assertEquals(2, player.count(objs.unstrung_holy_symbol))
            assertEquals(0, player.count(objs.coins))

            preparePlayer()
            player.setVarBit(varbits.karamja_diary_elite_complete, 1)
            add(recipe.mould)
            add(objs.silver_bar)
            select(deps, shilo, recipe, kind = FurnaceKind.ShiloVillage)
            advance(3)
            assertEquals(1, player.count(objs.unstrung_holy_symbol))

            val lovakite = prepareFurnace(furnace_locs.lovakite, offset = 4)
            preparePlayer()
            add(recipe.mould)
            add(objs.silver_bar)
            player.withProtectedAccess {
                deps.crafting.select(this, lovakite, FurnaceKind.Lovakite)
            }
            assertMessageSent(
                "This furnace is not at the right temperature to craft silver jewellery."
            )
            assertEquals(1, player.count(objs.silver_bar))
        }

    @Test
    fun GameTestState.`movement and material changes stop a batch without duplication`() =
        runJewelleryTest { deps ->
            val recipe = requireNotNull(deps.recipes.forOutput(objs.tiara))
            val furnace = prepareFurnace(furnace_locs.general)
            preparePlayer()
            add(recipe.mould)
            add(objs.silver_bar, 2)
            select(deps, furnace, recipe, quantity = 2)
            advance(3)
            assertEquals(1, player.count(objs.tiara))

            player.placeAt(furnace.coords.translateX(4))
            advance(3)
            assertEquals(1, player.count(objs.tiara))
            assertEquals(1, player.count(objs.silver_bar))
            assertEquals(0, player.weakQueueList.count(furnace_queues.silverJewellery))

            player.placeAt(furnace.coords.translateX(-1))
            select(deps, furnace, recipe)
            player.inv[player.inv.indexOfFirst { it?.id == objs.silver_bar.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.tiara))
        }

    private fun GameTestScope.select(
        deps: Dependencies,
        furnace: BoundLocInfo,
        recipe: SilverCraftingRecipe,
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
            SilverJewelleryScript::class,
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
    constructor(
        val crafting: SilverJewelleryCrafting,
        val recipes: SilverCraftingRecipeRegistry,
        val cacheTypes: TypeListMap,
    )
}
