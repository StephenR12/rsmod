package org.rsmod.content.skills.crafting.jewellery

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class GemCuttingAndStringingTest {
    @Test
    fun GameTestState.`cache 233 exposes the exact ten gem cutting contracts`() =
        runHandcraftTest { deps ->
            assertEquals(
                listOf(
                    "Opal:1:15.0:3.8:890:true",
                    "Jade:13:20.0:5.0:891:true",
                    "Red topaz:16:25.0:6.3:892:true",
                    "Sapphire:20:50.0:none:888:false",
                    "Emerald:27:67.5:none:889:false",
                    "Ruby:34:85.0:none:887:false",
                    "Diamond:43:107.5:none:886:false",
                    "Dragonstone:55:137.5:none:885:true",
                    "Onyx:67:167.5:none:2717:true",
                    "Zenyte:89:50.0:none:7185:true",
                ),
                deps.gemRecipes.recipes.map {
                    "${it.name}:${it.level}:${it.successXp}:${it.crush?.xp ?: "none"}:" +
                        "${it.animation.id}:${it.members}"
                },
            )
            assertEquals(
                listOf("128:250", "100:245", "90:240"),
                deps.gemRecipes.recipes.mapNotNull {
                    it.crush?.let { chance -> "${chance.low}:${chance.high}" }
                },
            )
            assertEquals(129.0 / 256.0, SkillingSuccessRate.successRate(128, 250, 1, 99))
            assertEquals(251.0 / 256.0, SkillingSuccessRate.successRate(128, 250, 99, 99))
            assertEquals(119.0 / 256.0, SkillingSuccessRate.successRate(100, 245, 13, 99))
            assertEquals(246.0 / 256.0, SkillingSuccessRate.successRate(100, 245, 99, 99))
            assertEquals(114.0 / 256.0, SkillingSuccessRate.successRate(90, 240, 16, 99))
            assertEquals(241.0 / 256.0, SkillingSuccessRate.successRate(90, 240, 99, 99))
        }

    @Test
    fun GameTestState
        .`all ten gems consume one uncut gem preserve the chisel and award exact xp`() =
        runHandcraftTest { deps ->
            for (recipe in deps.gemRecipes.recipes) {
                preparePlayer()
                add(objs.chisel)
                add(recipe.uncut)
                random.next = 0
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.gemCutting.process(this, GemCuttingRequest(recipe.uncut))
                }

                assertEquals(0, player.count(recipe.uncut), recipe.name)
                assertEquals(1, player.count(recipe.cut), recipe.name)
                assertEquals(1, player.count(objs.chisel), recipe.name)
                assertEquals(
                    (recipe.successXp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`semiprecious failures create crushed gems and use failure xp`() =
        runHandcraftTest { deps ->
            for (recipe in deps.gemRecipes.recipes.filter { it.crush != null }) {
                preparePlayer()
                add(objs.chisel)
                add(recipe.uncut)
                random.next = 99
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.gemCutting.process(this, GemCuttingRequest(recipe.uncut))
                }

                assertEquals(0, player.count(recipe.cut), recipe.name)
                assertEquals(1, player.count(objs.crushed_gem), recipe.name)
                assertEquals(
                    (requireNotNull(recipe.crush).xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
                assertMessageSent("You accidentally crush the gem.")
            }
        }

    @Test
    fun GameTestState.`gem batches process every two ticks and stop when inputs disappear`() =
        runHandcraftTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.uncut_sapphire, 2)

            player.withProtectedAccess { deps.gemCutting.start(this, objs.uncut_sapphire) }
            advance()
            assertEquals(0, player.count(objs.sapphire))
            advance()
            assertEquals(1, player.count(objs.sapphire))
            advance()
            assertEquals(1, player.count(objs.sapphire))
            advance()
            assertEquals(2, player.count(objs.sapphire))
            assertEquals(0, player.weakQueueList.count(handcraft_queues.gemCutting))

            preparePlayer()
            add(objs.chisel)
            add(objs.uncut_sapphire, 2)
            player.withProtectedAccess { deps.gemCutting.start(this, objs.uncut_sapphire) }
            advance(2)
            player.inv[player.inv.indexOfFirst { it?.id == objs.chisel.id }] = null
            advance(2)
            assertEquals(1, player.count(objs.sapphire))
            assertEquals(1, player.count(objs.uncut_sapphire))
        }

    @Test
    fun GameTestState.`gem membership level and chisel failures are atomic`() =
        runHandcraftTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.chisel)
            add(objs.uncut_opal)
            player.withProtectedAccess { deps.gemCutting.start(this, objs.uncut_opal) }
            assertMessageSent("You need to be on a members' world to cut this gem.")
            assertEquals(1, player.count(objs.uncut_opal))

            preparePlayer()
            player.stats[stats.crafting] = 42
            add(objs.chisel)
            add(objs.uncut_diamond)
            player.withProtectedAccess { deps.gemCutting.start(this, objs.uncut_diamond) }
            assertMessageSent("You need a Crafting level of 43 to cut this gem.")
            assertEquals(1, player.count(objs.uncut_diamond))

            preparePlayer()
            add(objs.uncut_diamond)
            player.withProtectedAccess { deps.gemCutting.start(this, objs.uncut_diamond) }
            assertMessageSent("You need a chisel to cut this gem.")
            assertEquals(1, player.count(objs.uncut_diamond))
        }

    @Test
    fun GameTestState.`all fifteen amulet and symbol recipes consume wool and award exact xp`() =
        runHandcraftTest { deps ->
            assertEquals(15, deps.stringRecipes.recipes.size)
            assertEquals(
                listOf(
                    "Gold amulet:false:4.0",
                    "Sapphire amulet:false:4.0",
                    "Emerald amulet:false:4.0",
                    "Ruby amulet:false:4.0",
                    "Diamond amulet:false:4.0",
                    "Unblessed symbol:false:4.0",
                    "Opal amulet:true:4.0",
                    "Jade amulet:true:4.0",
                    "Topaz amulet:true:4.0",
                    "Dragonstone amulet:true:4.0",
                    "Onyx amulet:true:4.0",
                    "Zenyte amulet:true:4.0",
                    "Unpowered symbol:true:4.0",
                    "M'speak amulet:true:4.0",
                    "Salve amulet:true:0.0",
                ),
                deps.stringRecipes.recipes.map { "${it.name}:${it.members}:${it.xp}" },
            )
            for (recipe in deps.stringRecipes.recipes) {
                preparePlayer()
                add(objs.ball_of_wool)
                add(recipe.unstrung)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.amuletStringing.process(this, AmuletStringingRequest(recipe.unstrung))
                }

                assertEquals(0, player.count(recipe.unstrung), recipe.name)
                assertEquals(0, player.count(objs.ball_of_wool), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`amulet stringing batches every two ticks and is membership safe`() =
        runHandcraftTest { deps ->
            preparePlayer()
            add(objs.ball_of_wool, 2)
            add(objs.unstrung_gold_amulet, 2)
            player.withProtectedAccess {
                deps.amuletStringing.start(this, objs.unstrung_gold_amulet)
            }
            advance()
            assertEquals(0, player.count(objs.gold_amulet))
            advance()
            assertEquals(1, player.count(objs.gold_amulet))
            advance(2)
            assertEquals(2, player.count(objs.gold_amulet))
            assertEquals(0, player.weakQueueList.count(handcraft_queues.amuletStringing))

            preparePlayer()
            player.members = false
            add(objs.ball_of_wool)
            add(objs.unstrung_onyx_amulet)
            player.withProtectedAccess {
                deps.amuletStringing.start(this, objs.unstrung_onyx_amulet)
            }
            assertMessageSent("You need to be on a members' world to string this item.")
            assertEquals(1, player.count(objs.ball_of_wool))
            assertEquals(1, player.count(objs.unstrung_onyx_amulet))
        }

    @Test
    fun GameTestState.`starting either handcraft replaces the other batch`() =
        runHandcraftTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.uncut_sapphire, 2)
            add(objs.ball_of_wool)
            add(objs.unstrung_gold_amulet)
            player.withProtectedAccess { deps.gemCutting.start(this, objs.uncut_sapphire) }
            assertEquals(1, player.weakQueueList.count(handcraft_queues.gemCutting))

            player.withProtectedAccess {
                deps.amuletStringing.start(this, objs.unstrung_gold_amulet)
            }
            assertEquals(0, player.weakQueueList.count(handcraft_queues.gemCutting))
            assertEquals(1, player.weakQueueList.count(handcraft_queues.amuletStringing))
            advance(2)
            assertEquals(0, player.count(objs.sapphire))
            assertEquals(1, player.count(objs.gold_amulet))
            assertTrue(player.count(objs.uncut_sapphire) == 2)
        }

    @Test
    fun GameTestState.`held item interactions accept either selected item order`() =
        runHandcraftTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.uncut_sapphire)
            val chiselSlot = player.inv.indexOfFirst { it?.id == objs.chisel.id }
            val gemSlot = player.inv.indexOfFirst { it?.id == objs.uncut_sapphire.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.uncut_sapphire],
                    gemSlot,
                    objTypes[objs.chisel],
                    chiselSlot,
                )
            }
            advance(2)
            assertEquals(1, player.count(objs.sapphire))

            preparePlayer()
            add(objs.ball_of_wool)
            add(objs.unstrung_gold_amulet)
            val woolSlot = player.inv.indexOfFirst { it?.id == objs.ball_of_wool.id }
            val amuletSlot = player.inv.indexOfFirst { it?.id == objs.unstrung_gold_amulet.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.ball_of_wool],
                    woolSlot,
                    objTypes[objs.unstrung_gold_amulet],
                    amuletSlot,
                )
            }
            advance(2)
            assertEquals(1, player.count(objs.gold_amulet))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
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

    private fun GameTestState.runHandcraftTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            GemCuttingScript::class,
            AmuletStringingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(JewelleryModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val gemCutting: GemCutting,
        val gemRecipes: GemCuttingRecipeRegistry,
        val amuletStringing: AmuletStringing,
        val stringRecipes: AmuletStringingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
