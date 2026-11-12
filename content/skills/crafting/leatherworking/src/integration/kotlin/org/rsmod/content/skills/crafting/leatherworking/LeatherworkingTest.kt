package org.rsmod.content.skills.crafting.leatherworking

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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class LeatherworkingTest {
    @Test
    fun GameTestState.`cache 233 exposes all ordinary leatherworking objects and animation`() =
        runLeatherworkingTest { deps ->
            assertEquals(20, deps.recipes.recipes.size)
            assertEquals(1733, objs.needle.id)
            assertEquals(29920, objs.costume_needle.id)
            assertEquals(1734, objs.thread.id)
            assertEquals(1741, objs.leather.id)
            assertEquals(1743, objs.hard_leather.id)
            assertEquals(
                listOf(1745, 2505, 2507, 2509),
                deps.recipes.materials.drop(2).map { it.id },
            )
            assertEquals(1249, seqs.human_leather_crafting.id)
        }

    @Test
    fun GameTestState.`recipe table pins exact levels materials outputs xp and membership`() =
        runLeatherworkingTest { deps ->
            assertEquals(
                listOf(
                    "Leather gloves:1:1741:1:1059:13.8:false",
                    "Leather boots:7:1741:1:1061:16.2:false",
                    "Leather cowl:9:1741:1:1167:18.5:false",
                    "Leather vambraces:11:1741:1:1063:22.0:false",
                    "Leather body:14:1741:1:1129:25.0:false",
                    "Leather chaps:18:1741:1:1095:27.0:false",
                    "Coif:38:1741:1:1169:37.0:false",
                    "Hardleather body:28:1743:1:1131:35.0:false",
                    "Green d'hide vambraces:57:1745:1:1065:62.0:true",
                    "Green d'hide chaps:60:1745:2:1099:124.0:true",
                    "Green d'hide body:63:1745:3:1135:186.0:true",
                    "Blue d'hide vambraces:66:2505:1:2487:70.0:true",
                    "Blue d'hide chaps:68:2505:2:2493:140.0:true",
                    "Blue d'hide body:71:2505:3:2499:210.0:true",
                    "Red d'hide vambraces:73:2507:1:2489:78.0:true",
                    "Red d'hide chaps:75:2507:2:2495:156.0:true",
                    "Red d'hide body:77:2507:3:2501:234.0:true",
                    "Black d'hide vambraces:79:2509:1:2491:86.0:true",
                    "Black d'hide chaps:82:2509:2:2497:172.0:true",
                    "Black d'hide body:84:2509:3:2503:258.0:true",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.material.id}:${it.materialCount}:${it.output.id}:${it.xp}:${it.members}"
                },
            )
        }

    @Test
    fun GameTestState.`soft leather opens the native seven-product quantity menu`() =
        runLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.leather, 2)

            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.leather, costumeNeedle = false)
            }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.leather_gloves.id,
                    objs.leather_boots.id,
                    objs.leather_cowl.id,
                    objs.leather_vambraces.id,
                    objs.leather_body.id,
                    objs.leather_chaps.id,
                    objs.leather_coif.id,
                    -1,
                    -1,
                    -1,
                    2,
                    "What would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 14..20
                }
            assertEquals(7, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 2 })
        }

    @Test
    fun GameTestState.`all recipes consume exact leather and award exact xp`() =
        runLeatherworkingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.costume_needle)
                add(recipe.material, recipe.materialCount)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.leatherworking.process(
                        this,
                        LeatherworkingRequest(recipe.output, costumeNeedle = true, remaining = 1),
                    )
                }

                assertEquals(0, player.count(recipe.material), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`ordinary needle consumes one thread on every fifth armour action`() =
        runLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread, 2)
            add(objs.leather, 5)
            val gloves = deps.recipes.recipes.first()

            player.withProtectedAccess {
                deps.leatherworking.start(this, gloves, costumeNeedle = false, quantity = 4)
            }
            advance(12)
            assertEquals(4, player.count(objs.leather_gloves))
            assertEquals(2, player.count(objs.thread))

            player.withProtectedAccess {
                deps.leatherworking.start(this, gloves, costumeNeedle = false, quantity = 1)
            }
            advance(3)
            assertEquals(5, player.count(objs.leather_gloves))
            assertEquals(1, player.count(objs.thread))
        }

    @Test
    fun GameTestState
        .`costume needle supplies unlimited thread without changing materials or xp`() =
        runLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.costume_needle)
            add(objs.black_dragon_leather, 6)
            val body = deps.recipes.recipes.single { it.output == objs.black_dhide_body }
            val startXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess {
                deps.leatherworking.start(this, body, costumeNeedle = true, quantity = 2)
            }
            advance(6)

            assertEquals(2, player.count(objs.black_dhide_body))
            assertEquals(0, player.count(objs.black_dragon_leather))
            assertEquals(0, player.count(objs.thread))
            assertEquals(5160, player.statMap.getFineXP(stats.crafting) - startXp)
        }

    @Test
    fun GameTestState.`level membership needle thread and material checks are atomic`() =
        runLeatherworkingTest { deps ->
            val blackBody = deps.recipes.recipes.single { it.output == objs.black_dhide_body }
            preparePlayer()
            player.members = false
            add(objs.needle)
            add(objs.thread)
            add(objs.black_dragon_leather, 3)
            player.withProtectedAccess {
                deps.leatherworking.start(this, blackBody, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need to be on a members' world to craft dragonhide armour.")
            assertEquals(3, player.count(objs.black_dragon_leather))

            preparePlayer()
            player.stats[stats.crafting] = 83
            add(objs.needle)
            add(objs.thread)
            add(objs.black_dragon_leather, 3)
            player.withProtectedAccess {
                deps.leatherworking.start(this, blackBody, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need a Crafting level of 84 to make this item.")
            assertEquals(3, player.count(objs.black_dragon_leather))

            preparePlayer()
            add(objs.leather)
            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.leather, costumeNeedle = false)
            }
            assertMessageSent("You need a needle to work with leather.")
            assertEquals(1, player.count(objs.leather))

            preparePlayer()
            add(objs.needle)
            add(objs.leather)
            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.leather, costumeNeedle = false)
            }
            assertMessageSent("You need some thread to work with leather.")
            assertEquals(1, player.count(objs.leather))

            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.black_dragon_leather, 2)
            player.withProtectedAccess {
                deps.leatherworking.start(this, blackBody, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You do not have enough leather to make that.")
            assertEquals(2, player.count(objs.black_dragon_leather))
        }

    @Test
    fun GameTestState.`batch cadence material stop and crafting queue replacement are exact`() =
        runLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.leather, 2)
            val gloves = deps.recipes.recipes.first()
            player.withProtectedAccess {
                weakQueue(crafting_queues.potteryShaping, 20)
                deps.leatherworking.start(this, gloves, costumeNeedle = false, quantity = 2)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.potteryShaping))
            advance(2)
            assertEquals(0, player.count(objs.leather_gloves))
            advance()
            assertEquals(1, player.count(objs.leather_gloves))
            player.inv[player.inv.indexOfFirst { it?.id == objs.needle.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.leather_gloves))
            assertEquals(1, player.count(objs.leather))
            assertEquals(0, player.weakQueueList.count(crafting_queues.leatherworking))
        }

    @Test
    fun GameTestState
        .`held interaction accepts reverse item order and starts the selected recipe`() =
        runLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.leather)
            val needleSlot = player.inv.indexOfFirst { it?.id == objs.needle.id }
            val leatherSlot = player.inv.indexOfFirst { it?.id == objs.leather.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.leather],
                    leatherSlot,
                    objTypes[objs.needle],
                    needleSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_e, quantity = 1)
            advance(3)

            assertEquals(1, player.count(objs.leather_body))
            assertEquals(1, player.count(objs.needle))
            assertEquals(1, player.count(objs.thread))
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

    private fun GameTestScope.resumeSkillMulti(component: ComponentType, quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = quantity))
    }

    private fun GameTestState.runLeatherworkingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            LeatherworkingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(LeatherworkingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val leatherworking: Leatherworking,
        val recipes: LeatherworkingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
