package org.rsmod.content.skills.crafting.glassmaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.interfaces.IfSetEventsV2
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.content.skills.smithing.smelting.furnace_locs
import org.rsmod.content.skills.smithing.smelting.furnace_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class GlassblowingTest {
    @Test
    fun GameTestState.`cache 233 exposes extended glassmaking object identities`() =
        runGlassmakingTest {
            assertEquals(1775, objs.molten_glass.id)
            assertEquals(1781, objs.soda_ash.id)
            assertEquals(1783, objs.bucket_of_sand.id)
            assertEquals(1925, objs.bucket_empty.id)
            assertEquals(4525, objs.empty_oil_lamp.id)
            assertEquals(4540, objs.oil_lantern_frame.id)
            assertEquals(4535, objs.empty_oil_lantern.id)
            assertEquals(10980, objs.empty_light_orb.id)
            assertEquals(10981, objs.cave_goblin_wire.id)
            assertEquals(10973, objs.light_orb.id)
        }

    @Test
    fun GameTestState.`cache 233 exposes all eight glassblowing recipes and animation`() =
        runGlassmakingTest { deps ->
            assertEquals(
                listOf(
                    "Beer glass:1:1919:17.5",
                    "Empty candle lantern:4:4527:19.0",
                    "Empty oil lamp:12:4525:25.0",
                    "Vial:33:229:35.0",
                    "Empty fishbowl:42:6667:42.5",
                    "Unpowered orb:46:567:52.5",
                    "Lantern lens:49:4542:55.0",
                    "Empty light orb:87:10980:70.0",
                ),
                deps.recipes.recipes.map { "${it.name}:${it.level}:${it.output.id}:${it.xp}" },
            )
            assertEquals(884, seqs.human_glassblowing.id)
        }

    @Test
    fun GameTestState.`pipe on molten glass opens the cache quantity menu`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            add(objs.glassblowing_pipe)
            add(objs.molten_glass, 2)

            player.withProtectedAccess { deps.glassblowing.select(this) }

            val open = client.filterIsInstance<IfOpenSub>().single { it.interfaceId == 270 }
            assertEquals(components.chatbox_chatmodal.id, open.destinationCombinedId)
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    *deps.recipes.recipes.map { it.output.id }.toTypedArray(),
                    -1,
                    -1,
                    2,
                    "What would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 15..22
                }
            assertEquals(8, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 2 })
        }

    @Test
    fun GameTestState.`all recipes consume one glass preserve the pipe and award exact xp`() =
        runGlassmakingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.glassblowing_pipe)
                add(objs.molten_glass)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.glassblowing.process(this, GlassblowingRequest(recipe.output, 1))
                }

                assertEquals(0, player.count(objs.molten_glass), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(1, player.count(objs.glassblowing_pipe), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`glassblowing batches every three ticks and stops without its pipe`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            add(objs.glassblowing_pipe)
            add(objs.molten_glass, 2)
            val recipe = deps.recipes.recipes.first()
            player.withProtectedAccess { deps.glassblowing.start(this, recipe, quantity = 2) }

            advance(2)
            assertEquals(0, player.count(objs.beer_glass))
            advance()
            assertEquals(1, player.count(objs.beer_glass))
            advance(2)
            assertEquals(1, player.count(objs.beer_glass))
            advance()
            assertEquals(2, player.count(objs.beer_glass))
            assertEquals(0, player.weakQueueList.count(crafting_queues.glassblowing))

            preparePlayer()
            add(objs.glassblowing_pipe)
            add(objs.molten_glass, 2)
            player.withProtectedAccess { deps.glassblowing.start(this, recipe, quantity = 2) }
            advance(3)
            player.inv[player.inv.indexOfFirst { it?.id == objs.glassblowing_pipe.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.beer_glass))
            assertEquals(1, player.count(objs.molten_glass))
        }

    @Test
    fun GameTestState.`membership level tool and material checks are atomic`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.glassblowing_pipe)
            add(objs.molten_glass)
            player.withProtectedAccess { deps.glassblowing.select(this) }
            assertMessageSent("You need to be on a members' world to blow glass.")
            assertEquals(1, player.count(objs.molten_glass))

            preparePlayer()
            player.stats[stats.crafting] = 48
            add(objs.glassblowing_pipe)
            add(objs.molten_glass)
            val lens = deps.recipes.recipes.single { it.output == objs.lantern_lens }
            player.withProtectedAccess { deps.glassblowing.start(this, lens, quantity = 1) }
            assertMessageSent("You need a Crafting level of 49 to make this item.")
            assertEquals(1, player.count(objs.molten_glass))

            preparePlayer()
            add(objs.molten_glass)
            player.withProtectedAccess { deps.glassblowing.select(this) }
            assertMessageSent("You need a glassblowing pipe to blow glass.")
            assertEquals(1, player.count(objs.molten_glass))

            preparePlayer()
            add(objs.glassblowing_pipe)
            player.withProtectedAccess { deps.glassblowing.select(this) }
            assertMessageSent("You need some molten glass to blow glass.")
        }

    @Test
    fun GameTestState.`held item interaction accepts either item order and starts selection`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            add(objs.glassblowing_pipe)
            add(objs.molten_glass)
            val pipeSlot = player.inv.indexOfFirst { it?.id == objs.glassblowing_pipe.id }
            val glassSlot = player.inv.indexOfFirst { it?.id == objs.molten_glass.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.molten_glass],
                    glassSlot,
                    objTypes[objs.glassblowing_pipe],
                    pipeSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_f, quantity = 1)
            advance(3)

            assertEquals(1, player.count(objs.unpowered_orb))
            assertEquals(1, player.count(objs.glassblowing_pipe))
        }

    @Test
    fun GameTestState.`starting glassblowing clears all other crafting batches`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            add(objs.glassblowing_pipe)
            add(objs.molten_glass)
            player.withProtectedAccess {
                weakQueue(crafting_queues.gemCutting, 20)
                weakQueue(crafting_queues.amuletStringing, 20)
                deps.glassblowing.start(this, deps.recipes.recipes.first(), quantity = 1)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.gemCutting))
            assertEquals(0, player.weakQueueList.count(crafting_queues.amuletStringing))
            assertEquals(1, player.weakQueueList.count(crafting_queues.glassblowing))
        }

    @Test
    fun GameTestState
        .`furnace glassmaking uses the cache quantity menu and repeats every two ticks`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            val furnace = prepareFurnace()
            add(objs.bucket_of_sand, 2)
            add(objs.soda_ash, 2)
            val startXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.moltenGlassmaking.select(this, furnace) }

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    13,
                    2,
                    objs.molten_glass.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    2,
                    "What would you like to smelt?",
                ),
                setup.values,
            )
            resumeSkillMulti(components.skillmulti_a, quantity = 2)
            advance()
            assertEquals(0, player.count(objs.molten_glass))
            advance()
            assertEquals(1, player.count(objs.molten_glass))
            assertEquals(1, player.count(objs.bucket_empty))
            advance(2)
            assertEquals(2, player.count(objs.molten_glass))
            assertEquals(2, player.count(objs.bucket_empty))
            assertEquals(0, player.count(objs.bucket_of_sand))
            assertEquals(0, player.count(objs.soda_ash))
            assertEquals(400, player.statMap.getFineXP(stats.crafting) - startXp)
        }

    @Test
    fun GameTestState.`furnace glassmaking is atomic and cancels furnace and handcraft batches`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            val furnace = prepareFurnace()
            add(objs.bucket_of_sand, 2)
            add(objs.soda_ash, 2)
            player.withProtectedAccess {
                weakQueue(crafting_queues.glassblowing, 20)
                weakQueue(furnace_queues.smelting, 20)
                deps.moltenGlassmaking.start(this, furnace, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.glassblowing))
            assertEquals(0, player.weakQueueList.count(furnace_queues.smelting))
            assertEquals(1, player.weakQueueList.count(furnace_queues.moltenGlassmaking))

            advance(2)
            player.placeAt(furnace.coords.translateX(4))
            advance(2)
            assertEquals(1, player.count(objs.molten_glass))
            assertEquals(1, player.count(objs.bucket_of_sand))
            assertEquals(1, player.count(objs.soda_ash))

            preparePlayer()
            add(objs.bucket_of_sand)
            player.withProtectedAccess { deps.moltenGlassmaking.start(this, furnace, quantity = 1) }
            assertMessageSent("You need soda ash and a bucket of sand to make molten glass.")
            assertEquals(1, player.count(objs.bucket_of_sand))
        }

    @Test
    fun GameTestState.`lantern and light orb assembly accept either item order with exact xp`() =
        runGlassmakingTest { deps ->
            preparePlayer()
            add(objs.empty_oil_lamp)
            add(objs.oil_lantern_frame)
            val lampStartXp = player.statMap.getFineXP(stats.crafting)
            val lampSlot = player.inv.indexOfFirst { it?.id == objs.empty_oil_lamp.id }
            val frameSlot = player.inv.indexOfFirst { it?.id == objs.oil_lantern_frame.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.oil_lantern_frame],
                    frameSlot,
                    objTypes[objs.empty_oil_lamp],
                    lampSlot,
                )
            }
            assertEquals(1, player.count(objs.empty_oil_lantern))
            assertEquals(500, player.statMap.getFineXP(stats.crafting) - lampStartXp)

            preparePlayer()
            add(objs.empty_light_orb)
            add(objs.cave_goblin_wire)
            val orbStartXp = player.statMap.getFineXP(stats.crafting)
            val orbSlot = player.inv.indexOfFirst { it?.id == objs.empty_light_orb.id }
            val wireSlot = player.inv.indexOfFirst { it?.id == objs.cave_goblin_wire.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.empty_light_orb],
                    orbSlot,
                    objTypes[objs.cave_goblin_wire],
                    wireSlot,
                )
            }
            assertEquals(1, player.count(objs.light_orb))
            assertEquals(1040, player.statMap.getFineXP(stats.crafting) - orbStartXp)

            preparePlayer()
            player.stats[stats.crafting] = 86
            add(objs.empty_light_orb)
            add(objs.cave_goblin_wire)
            player.withProtectedAccess { deps.assembly.wireLightOrb(this) }
            assertMessageSent("You need a Crafting level of 87 to make this item.")
            assertEquals(1, player.count(objs.empty_light_orb))
            assertEquals(1, player.count(objs.cave_goblin_wire))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
    }

    private fun GameTestScope.prepareFurnace(): BoundLocInfo {
        val furnace = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), furnace_locs.general)
        player.placeAt(furnace.coords.translateX(-1))
        return furnace
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

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runGlassmakingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            GlassblowingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(GlassmakingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val glassblowing: Glassblowing,
        val moltenGlassmaking: MoltenGlassmaking,
        val assembly: GlassAssembly,
        val recipes: GlassblowingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
