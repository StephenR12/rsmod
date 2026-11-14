package org.rsmod.content.skills.crafting.specialistleatherworking

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
import org.rsmod.api.config.refs.varbits
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
class SpecialistLeatherworkingTest {
    @Test
    fun GameTestState.`cache 233 exposes specialist materials outputs quest var and animation`() =
        runSpecialistLeatherworkingTest { deps ->
            assertEquals(6289, objs.snakeskin.id)
            assertEquals(10820, objs.cured_yak_hide.id)
            assertEquals(13383, objs.xerician_fabric.id)
            assertEquals(
                listOf(6328, 6330, 6326, 6324, 6322, 10824, 10822, 13385, 13389, 13387),
                deps.recipes.recipes.map { it.output.id },
            )
            assertEquals(3311, varbits.fremennik_isles_progress.id)
            assertEquals(1249, seqs.human_leather_crafting.id)
        }

    @Test
    fun GameTestState.`recipe table pins exact levels materials outputs xp and unlocks`() =
        runSpecialistLeatherworkingTest { deps ->
            assertEquals(
                listOf(
                    "Snakeskin boots:45:6289x6=6328:30.0:null",
                    "Snakeskin vambraces:47:6289x8=6330:35.0:null",
                    "Snakeskin bandana:48:6289x5=6326:45.0:null",
                    "Snakeskin chaps:51:6289x12=6324:50.0:null",
                    "Snakeskin body:53:6289x15=6322:55.0:null",
                    "Yak-hide armour (legs):43:10820x1=10824:32.0:280",
                    "Yak-hide armour (top):46:10820x2=10822:32.0:280",
                    "Xerician hat:14:13383x3=13385:66.0:null",
                    "Xerician robe:17:13383x4=13389:88.0:null",
                    "Xerician top:22:13383x5=13387:110.0:null",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.material.id}x${it.materialCount}=" +
                        "${it.output.id}:${it.xp}:${it.fremennikIslesProgress}"
                },
            )
        }

    @Test
    fun GameTestState.`Xerician fabric opens the native three-product quantity menu`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.costume_needle)
            add(objs.xerician_fabric, 10)

            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.xerician_fabric, costumeNeedle = true)
            }

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    3,
                    objs.xerician_hat.id,
                    objs.xerician_robe.id,
                    objs.xerician_top.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    3,
                    "What would you like to make?",
                ),
                setup.values,
            )
            resumeSkillMulti(components.skillmulti_a, quantity = 3)
            advance(9)
            assertEquals(3, player.count(objs.xerician_hat))
            assertEquals(1, player.count(objs.xerician_fabric))
            assertEquals(0, player.count(objs.thread))
        }

    @Test
    fun GameTestState.`snakeskin opens the native five-product menu capped by thread`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread, 2)
            add(objs.snakeskin, 25)

            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.snakeskin, costumeNeedle = false)
            }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    2,
                    objs.snakeskin_boots.id,
                    objs.snakeskin_vambraces.id,
                    objs.snakeskin_bandana.id,
                    objs.snakeskin_chaps.id,
                    objs.snakeskin_body.id,
                    -1,
                    -1,
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
                    it.interfaceId == 270 && it.componentId in 15..19
                }
            assertEquals(5, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 2 })
        }

    @Test
    fun GameTestState.`yak hide opens the native two-product menu after quest stage 280`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            player.setVarBit(varbits.fremennik_isles_progress, 280)
            add(objs.costume_needle)
            add(objs.cured_yak_hide, 4)

            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.cured_yak_hide, costumeNeedle = true)
            }

            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    4,
                    objs.yak_hide_armour_legs.id,
                    objs.yak_hide_armour_top.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    4,
                    "What would you like to make?",
                ),
                setup.values,
            )
        }

    @Test
    fun GameTestState.`every recipe consumes one thread and awards exact xp after three ticks`() =
        runSpecialistLeatherworkingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                player.setVarBit(varbits.fremennik_isles_progress, 280)
                add(objs.needle)
                add(objs.thread)
                add(recipe.material, recipe.materialCount)
                val startXp = player.statMap.getFineXP(stats.crafting)

                player.withProtectedAccess {
                    deps.leatherworking.start(this, recipe, costumeNeedle = false, quantity = 1)
                }

                assertEquals(seqs.human_leather_crafting.id, player.pendingSequence.id)
                advance(2)
                assertEquals(0, player.count(recipe.output), recipe.name)
                advance()
                assertEquals(0, player.count(recipe.material), recipe.name)
                assertEquals(0, player.count(objs.thread), recipe.name)
                assertEquals(1, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.crafting) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`costume needle supplies thread for a yak top batch`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            player.setVarBit(varbits.fremennik_isles_progress, 340)
            add(objs.costume_needle)
            add(objs.cured_yak_hide, 4)
            val top = deps.recipes.forOutput(objs.yak_hide_armour_top)!!

            player.withProtectedAccess {
                deps.leatherworking.start(this, top, costumeNeedle = true, quantity = 2)
            }
            advance(6)

            assertEquals(2, player.count(objs.yak_hide_armour_top))
            assertEquals(0, player.count(objs.cured_yak_hide))
            assertEquals(0, player.count(objs.thread))
        }

    @Test
    fun GameTestState
        .`ordinary needle consumes a whole thread per item and stops when exhausted`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread, 2)
            add(objs.snakeskin, 18)
            val boots = deps.recipes.forOutput(objs.snakeskin_boots)!!

            player.withProtectedAccess {
                deps.leatherworking.start(this, boots, costumeNeedle = false, quantity = 3)
            }
            advance(9)

            assertEquals(2, player.count(objs.snakeskin_boots))
            assertEquals(6, player.count(objs.snakeskin))
            assertEquals(0, player.count(objs.thread))
            assertEquals(0, player.weakQueueList.count(crafting_queues.specialistLeatherworking))
        }

    @Test
    fun GameTestState.`yak armour rejects state 279 and accepts the exact quest boundary`() =
        runSpecialistLeatherworkingTest { deps ->
            val legs = deps.recipes.forOutput(objs.yak_hide_armour_legs)!!
            preparePlayer()
            player.setVarBit(varbits.fremennik_isles_progress, 279)
            add(objs.needle)
            add(objs.thread)
            add(objs.cured_yak_hide)
            player.withProtectedAccess {
                deps.leatherworking.start(this, legs, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent(
                "You need to progress further in The Fremennik Isles to make yak-hide armour."
            )
            assertEquals(1, player.count(objs.cured_yak_hide))

            player.setVarBit(varbits.fremennik_isles_progress, 280)
            player.withProtectedAccess {
                deps.leatherworking.start(this, legs, costumeNeedle = false, quantity = 1)
            }
            advance(3)
            assertEquals(1, player.count(objs.yak_hide_armour_legs))
        }

    @Test
    fun GameTestState.`membership level tool thread and material failures are atomic`() =
        runSpecialistLeatherworkingTest { deps ->
            val body = deps.recipes.forOutput(objs.snakeskin_body)!!
            preparePlayer()
            player.members = false
            add(objs.needle)
            add(objs.thread)
            add(objs.snakeskin, 15)
            player.withProtectedAccess {
                deps.leatherworking.start(this, body, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need to be on a members' world to work with this material.")
            assertEquals(15, player.count(objs.snakeskin))

            preparePlayer()
            player.stats[stats.crafting] = 52
            add(objs.needle)
            add(objs.thread)
            add(objs.snakeskin, 15)
            player.withProtectedAccess {
                deps.leatherworking.start(this, body, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You need a Crafting level of 53 to make this item.")
            assertEquals(15, player.count(objs.snakeskin))

            preparePlayer()
            add(objs.thread)
            add(objs.snakeskin, 15)
            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.snakeskin, costumeNeedle = false)
            }
            assertMessageSent("You need a needle to work with this material.")
            assertEquals(15, player.count(objs.snakeskin))

            preparePlayer()
            add(objs.needle)
            add(objs.snakeskin, 15)
            player.withProtectedAccess {
                deps.leatherworking.select(this, objs.snakeskin, costumeNeedle = false)
            }
            assertMessageSent("You need some thread to work with this material.")
            assertEquals(15, player.count(objs.snakeskin))

            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.snakeskin, 14)
            player.withProtectedAccess {
                deps.leatherworking.start(this, body, costumeNeedle = false, quantity = 1)
            }
            assertMessageSent("You do not have enough material to make that.")
            assertEquals(14, player.count(objs.snakeskin))
            assertEquals(1, player.count(objs.thread))
        }

    @Test
    fun GameTestState.`full inventory transaction and crafting queue replacement are safe`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.snakeskin, 6)
            add(objs.leather, 20)
            assertEquals(28, player.inv.count { it != null })
            val boots = deps.recipes.forOutput(objs.snakeskin_boots)!!

            player.withProtectedAccess {
                weakQueue(crafting_queues.weaving, 20)
                deps.leatherworking.start(this, boots, costumeNeedle = false, quantity = 1)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.weaving))
            advance(3)

            assertEquals(1, player.count(objs.snakeskin_boots))
            assertEquals(22, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`movement and tool loss cancel a batch without consuming materials`() =
        runSpecialistLeatherworkingTest { deps ->
            val bandana = deps.recipes.forOutput(objs.snakeskin_bandana)!!
            preparePlayer()
            add(objs.needle)
            add(objs.thread, 2)
            add(objs.snakeskin, 10)
            player.withProtectedAccess {
                deps.leatherworking.start(this, bandana, costumeNeedle = false, quantity = 2)
            }
            player.clearPendingAction()
            advance(3)
            assertEquals(0, player.count(objs.snakeskin_bandana))
            assertEquals(10, player.count(objs.snakeskin))
            assertEquals(2, player.count(objs.thread))

            player.withProtectedAccess {
                deps.leatherworking.start(this, bandana, costumeNeedle = false, quantity = 2)
            }
            advance(3)
            player.inv[player.inv.indexOfFirst { it?.id == objs.needle.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.snakeskin_bandana))
            assertEquals(5, player.count(objs.snakeskin))
            assertEquals(1, player.count(objs.thread))
        }

    @Test
    fun GameTestState.`held interaction accepts reverse order and starts the selected recipe`() =
        runSpecialistLeatherworkingTest { deps ->
            preparePlayer()
            add(objs.needle)
            add(objs.thread)
            add(objs.snakeskin, 15)
            val needleSlot = player.inv.indexOfFirst { it?.id == objs.needle.id }
            val snakeskinSlot = player.inv.indexOfFirst { it?.id == objs.snakeskin.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.snakeskin],
                    snakeskinSlot,
                    objTypes[objs.needle],
                    needleSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_e, quantity = 1)
            advance(3)

            assertEquals(1, player.count(objs.snakeskin_body))
            assertEquals(1, player.count(objs.needle))
            assertEquals(0, player.count(objs.thread))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
        player.setVarBit(varbits.fremennik_isles_progress, 0)
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

    private fun GameTestState.runSpecialistLeatherworkingTest(
        body: GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            SpecialistLeatherworkingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(SpecialistLeatherworkingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val leatherworking: SpecialistLeatherworking,
        val recipes: SpecialistLeatherworkingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
