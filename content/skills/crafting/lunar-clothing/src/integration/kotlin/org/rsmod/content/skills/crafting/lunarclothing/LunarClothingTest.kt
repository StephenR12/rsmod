package org.rsmod.content.skills.crafting.lunarclothing

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.LeatherThreadUsageRepository
import org.rsmod.content.skills.crafting.leatherworking.Leatherworking
import org.rsmod.content.skills.crafting.leatherworking.LeatherworkingModule
import org.rsmod.content.skills.crafting.leatherworking.LeatherworkingRecipeRegistry
import org.rsmod.content.skills.crafting.leatherworking.LeatherworkingRequest
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class LunarClothingTest {
    @Test
    fun GameTestState.`cache 233 exposes the Lunar clothing materials and quest boundary`() =
        runLunarClothingTest { deps ->
            assertEquals(9081, objs.suqah_leather.id)
            assertEquals(9097, objs.lunar_torso.id)
            assertEquals(9098, objs.lunar_legs.id)
            assertEquals(9099, objs.lunar_gloves.id)
            assertEquals(9100, objs.lunar_boots.id)
            assertEquals(2448, varbits.lunar_quest_main.id)
            assertEquals(1249, seqs.human_leather_crafting.id)

            assertEquals("Suqah leather", deps.objTypes[objs.suqah_leather].name)
            assertTrue(deps.objTypes[objs.suqah_leather].members)
            assertEquals("Lunar torso", deps.objTypes[objs.lunar_torso].name)
            assertEquals("Lunar legs", deps.objTypes[objs.lunar_legs].name)
            assertEquals("Lunar gloves", deps.objTypes[objs.lunar_gloves].name)
            assertEquals("Lunar boots", deps.objTypes[objs.lunar_boots].name)
            assertEquals(listOf(190), deps.dbRows.types.getValue(LUNAR_DIPLOMACY_ROW).data[19])
        }

    @Test
    fun GameTestState.`recipe table pins transcript labels levels outputs xp and messages`() =
        runLunarClothingTest { deps ->
            assertEquals(
                listOf(
                    "Lunar Torso:61:9097:35.0:You need a Crafting level of 61 to make a Lunar Torso.:You make a Lunar Torso.",
                    "Lunar Trousers:59:9098:30.0:You need a Crafting level of 59 to make Lunar Trousers.:You make some Lunar Trousers.",
                    "Lunar Gloves:52:9099:25.0:You need a Crafting level of 52 to make Lunar Gloves.:You make some Lunar Gloves.",
                    "Lunar Boots:52:9100:25.0:You need a Crafting level of 52 to make Lunar Boots.:You make some Lunar Boots.",
                ),
                deps.recipes.recipes.map {
                    "${it.option}:${it.level}:${it.output.id}:${it.xp}:${it.levelFailure}:${it.success}"
                },
            )
        }

    @Test
    fun GameTestState.`needle on leather opens the exact cache-era five-option menu`() =
        runLunarClothingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.suqah_leather)

            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = false) }

            assertTrue(
                client.filterIsInstance<RunClientScript>().any {
                    it.id == CHATBOX_MULTI_INIT &&
                        it.values ==
                            listOf(
                                LunarClothingCrafting.MENU_TITLE,
                                "Lunar Torso|Lunar Trousers|Lunar Gloves|Lunar Boots|Cancel",
                            )
                }
            )
            choose(5)
            assertEquals(1, player.count(objs.suqah_leather))
        }

    @Test
    fun GameTestState.`every recipe takes three ticks and consumes one leather for exact xp`() {
        repeat(RECIPE_COUNT) { index ->
            runLunarClothingTest { deps ->
                val recipe = deps.recipes.recipes[index]
                preparePlayer()
                player.stats[stats.crafting] = recipe.level
                add(objs.costume_needle)
                add(objs.suqah_leather)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
                choose(index + 1)
                assertEquals(seqs.human_leather_crafting.id, player.pendingSequence.id)
                advance(2)
                assertEquals(1, player.count(objs.suqah_leather), recipe.option)
                advance()

                assertEquals(0, player.count(objs.suqah_leather), recipe.option)
                assertEquals(1, player.count(recipe.output), recipe.option)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.option,
                )
                assertMessageSent(recipe.success)
            }
        }
    }

    @Test
    fun GameTestState.`visible Crafting boundaries use each exact transcript failure`() {
        repeat(RECIPE_COUNT) { index ->
            runLunarClothingTest { deps ->
                val recipe = deps.recipes.recipes[index]
                preparePlayer()
                player.stats[stats.crafting] = recipe.level - 1
                add(objs.costume_needle)
                add(objs.suqah_leather)

                player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
                choose(index + 1)

                assertMessageSent(recipe.levelFailure)
                assertEquals(1, player.count(objs.suqah_leather), recipe.option)
                assertEquals(0, player.count(recipe.output), recipe.option)
            }
        }
    }

    @Test
    fun GameTestState
        .`quest state allows the clothing stage only and rejects completion exactly`() {
        runLunarClothingTest { deps ->
            preparePlayer(questState = LunarClothingCrafting.CLOTHING_STAGE_STATE - 1)
            add(objs.costume_needle)
            add(objs.suqah_leather)
            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
            assertMessageSent("You are not ready to make Lunar clothing yet.")
        }

        for (questState in
            listOf(
                LunarClothingCrafting.CLOTHING_STAGE_STATE,
                LunarClothingCrafting.QUEST_COMPLETE_STATE - 1,
            )) {
            runLunarClothingTest { deps ->
                preparePlayer(questState)
                add(objs.costume_needle)
                add(objs.suqah_leather)
                player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
                assertTrue(
                    client.filterIsInstance<RunClientScript>().any { it.id == CHATBOX_MULTI_INIT }
                )
                choose(5)
            }
        }

        runLunarClothingTest { deps ->
            preparePlayer(questState = LunarClothingCrafting.QUEST_COMPLETE_STATE)
            add(objs.costume_needle)
            add(objs.suqah_leather)
            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
            assertMessageSent(LunarClothingCrafting.POST_QUEST_MESSAGE)
            assertEquals(1, player.count(objs.suqah_leather))
        }
    }

    @Test
    fun GameTestState.`costume needle supplies thread and full inventory replacement is atomic`() =
        runLunarClothingTest { deps ->
            preparePlayer()
            add(objs.costume_needle)
            add(objs.suqah_leather)
            add(objs.leather, 26)
            assertEquals(28, player.inv.count { it != null })

            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
            choose(4)
            advance(3)

            assertEquals(1, player.count(objs.lunar_boots))
            assertEquals(0, player.count(objs.thread))
            assertEquals(28, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`ordinary leather and Suqah leather share one thread spool lifecycle`() =
        runLunarClothingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread, 2)
            add(objs.leather, 4)
            val gloves = deps.leatherRecipes.recipes.first()
            repeat(4) {
                player.withProtectedAccess {
                    deps.leatherworking.process(
                        this,
                        LeatherworkingRequest(gloves.output, costumeNeedle = false, remaining = 1),
                    )
                }
            }
            assertEquals(4, deps.threadUses.uses(player))
            assertEquals(2, player.count(objs.thread))

            add(objs.suqah_leather)
            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = false) }
            choose(3)
            advance(3)

            assertEquals(1, player.count(objs.lunar_gloves))
            assertEquals(1, player.count(objs.thread))
            assertEquals(0, deps.threadUses.uses(player))
        }

    @Test
    fun GameTestState.`held interaction accepts reverse item order`() =
        runLunarClothingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.suqah_leather)
            val needleSlot = player.inv.indexOfFirst { it?.id == objs.needle.id }
            val leatherSlot = player.inv.indexOfFirst { it?.id == objs.suqah_leather.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.suqah_leather],
                    leatherSlot,
                    objTypes[objs.needle],
                    needleSlot,
                )
            }
            choose(2)
            advance(3)

            assertEquals(1, player.count(objs.lunar_legs))
        }

    @Test
    fun GameTestState.`tool material and quest changes during the action cancel atomically`() =
        runLunarClothingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.suqah_leather)
            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = false) }
            choose(4)
            player.inv[player.inv.indexOfFirst { it?.id == objs.needle.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.suqah_leather))
            assertEquals(0, player.count(objs.lunar_boots))

            preparePlayer()
            add(objs.costume_needle)
            add(objs.suqah_leather)
            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
            choose(4)
            player.inv[player.inv.indexOfFirst { it?.id == objs.suqah_leather.id }] = null
            advance(3)
            assertEquals(0, player.count(objs.lunar_boots))

            preparePlayer()
            add(objs.costume_needle)
            add(objs.suqah_leather)
            player.withProtectedAccess { deps.crafting.select(this, costumeNeedle = true) }
            choose(4)
            player.setVarBit(varbits.lunar_quest_main, LunarClothingCrafting.QUEST_COMPLETE_STATE)
            advance(3)
            assertEquals(1, player.count(objs.suqah_leather))
            assertEquals(0, player.count(objs.lunar_boots))
        }

    private fun GameTestScope.preparePlayer(
        questState: Int = LunarClothingCrafting.CLOTHING_STAGE_STATE
    ) {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
        player.setVarBit(varbits.lunar_quest_main, questState)
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

    private fun GameTestScope.choose(option: Int) {
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.chatmenu_pbutton, subcomponent = option)
        )
    }

    private fun GameTestScope.assertMessageSent(text: String) {
        val messages =
            client.filterIsInstance<MessageGame>().map { it.message.replace("<br>", " ") }
        assertTrue(messages.contains(text)) {
            "Message not found. (search=$text) | (messages=$messages)"
        }
    }

    private fun GameTestState.runLunarClothingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            LunarClothingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(LunarClothingModule())
            install(LeatherworkingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crafting: LunarClothingCrafting,
        val recipes: LunarClothingRecipeRegistry,
        val leatherworking: Leatherworking,
        val leatherRecipes: LeatherworkingRecipeRegistry,
        val threadUses: LeatherThreadUsageRepository,
        val heldU: HeldUInteractions,
        val objTypes: ObjTypeList,
        val dbRows: DbRowTypeList,
    )

    private companion object {
        const val CHATBOX_MULTI_INIT = 58
        const val LUNAR_DIPLOMACY_ROW = 88
        const val RECIPE_COUNT = 4
    }
}
