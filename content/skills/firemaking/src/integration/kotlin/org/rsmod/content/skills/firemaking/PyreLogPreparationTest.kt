package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfOpenSub
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class PyreLogPreparationTest {
    @Test
    fun GameTestState.`cache 233 pins quest oil pyre log and recipe contracts`() =
        runFiremakingTest { deps ->
            assertEquals(339, varps.morttonquest.id)
            assertEquals(
                listOf(85),
                deps.dbRows.types.getValue(SHADES_OF_MORTTON_ROW).data[QUEST_COMPLETE_COLUMN],
            )
            assertEquals(
                listOf("3430:4", "3432:3", "3434:2", "3436:1"),
                SacredOils.all.map { "${it.obj.id}:${it.doses}" },
            )
            assertEquals(
                listOf(
                    "1511:3438:2:10.0",
                    "2862:3438:2:10.0",
                    "1521:3440:2:10.0",
                    "10810:10808:2:10.0",
                    "1519:3442:3:15.0",
                    "6333:6211:3:15.0",
                    "1517:3444:3:15.0",
                    "6332:6213:3:15.0",
                    "1515:3446:4:20.0",
                    "1513:3448:4:20.0",
                    "19669:19672:4:20.0",
                ),
                PyreLogRecipes.all.map { "${it.input.id}:${it.output.id}:${it.oilDoses}:${it.xp}" },
            )
            assertTrue(SacredOils.all.all { deps.objTypes[it.obj].members })
            assertTrue(PyreLogRecipes.all.all { deps.objTypes[it.output].members })
            assertEquals("Sacred oil(4)", deps.objTypes[objs.sacred_oil_4].name)
            assertEquals("Pyre logs", deps.objTypes[objs.pyre_logs].name)
            assertEquals("Redwood pyre logs", deps.objTypes[objs.redwood_pyre_logs].name)
        }

    @Test
    fun GameTestState.`held use opens native Make-All with vial-fragment-aware maximum`() =
        runFiremakingTest { deps ->
            preparePlayer()
            add(objs.sacred_oil_4)
            add(objs.sacred_oil_3)
            add(objs.oak_logs, 4)
            val oilSlot = player.inv.indexOfFirst { it?.id == objs.sacred_oil_4.id }
            val logSlot = player.inv.indexOfFirst { it?.id == objs.oak_logs.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    deps.objTypes[objs.oak_logs],
                    logSlot,
                    deps.objTypes[objs.sacred_oil_4],
                    oilSlot,
                )
            }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    3,
                    objs.oak_pyre_logs.id,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    3,
                    PyreLogPreparation.MENU_TITLE,
                ),
                setup.values,
            )
            resumeSkillMulti(quantity = 3)
            assertEquals(1, player.count(objs.oak_pyre_logs))
            assertEquals(1, player.weakQueueList.count(firemaking_queues.pyreLogPreparation))
        }

    @Test
    fun GameTestState.`all eleven source recipes consume exact doses and award five xp per dose`() {
        for (recipe in PyreLogRecipes.all) {
            runFiremakingTest { deps ->
                preparePlayer()
                val oil = SacredOils.withDoses(recipe.oilDoses)!!.obj
                add(oil)
                add(recipe.input)
                val startXp = firemakingXp()

                player.withProtectedAccess {
                    deps.preparation.process(
                        this,
                        PyreLogPreparationRequest(recipe.input.id, remaining = 1),
                    )
                }

                assertEquals(0, player.count(recipe.input), recipe.input.toString())
                assertEquals(1, player.count(recipe.output), recipe.input.toString())
                assertEquals(1, player.count(objs.empty_vial), recipe.input.toString())
                assertEquals((recipe.xp * 10).toInt(), firemakingXp() - startXp)
            }
        }
    }

    @Test
    fun GameTestState.`partial sacred oil vials retain the exact remaining dose`() =
        runFiremakingTest { deps ->
            val cases =
                listOf(
                    OilCase(objs.logs, objs.sacred_oil_4, objs.sacred_oil_2),
                    OilCase(objs.logs, objs.sacred_oil_3, objs.sacred_oil_1),
                    OilCase(objs.willow_logs, objs.sacred_oil_4, objs.sacred_oil_1),
                    OilCase(objs.yew_logs, objs.sacred_oil_4, objs.empty_vial),
                )
            for (case in cases) {
                preparePlayer()
                add(case.oil)
                add(case.log)
                val recipe = PyreLogRecipes.byInput.getValue(case.log.id)

                player.withProtectedAccess {
                    deps.preparation.process(
                        this,
                        PyreLogPreparationRequest(recipe.input.id, remaining = 1),
                    )
                }

                assertEquals(1, player.count(case.remainder))
                assertEquals(1, player.count(recipe.output))
            }
        }

    @Test
    fun GameTestState.`selection is immediate then Make-All repeats every four ticks`() =
        runFiremakingTest { deps ->
            preparePlayer()
            add(objs.sacred_oil_4, 2)
            add(objs.oak_logs, 3)
            val recipe = PyreLogRecipes.byInput.getValue(objs.oak_logs.id)

            player.withProtectedAccess { deps.preparation.start(this, recipe, quantity = 4) }
            assertEquals(1, player.count(recipe.output))
            advance(3)
            assertEquals(1, player.count(recipe.output))
            advance()
            assertEquals(2, player.count(recipe.output))
            advance(4)
            assertEquals(3, player.count(recipe.output))
            assertEquals(0, player.count(recipe.input))
            assertEquals(1, player.count(objs.empty_vial))
            assertEquals(1, player.count(objs.sacred_oil_2))
            assertEquals(0, player.weakQueueList.count(firemaking_queues.pyreLogPreparation))
        }

    @Test
    fun GameTestState.`membership quest selected-dose and source gates preserve materials`() =
        runFiremakingTest { deps ->
            val recipe = PyreLogRecipes.byInput.getValue(objs.redwood_logs.id)

            preparePlayer(members = false)
            add(objs.sacred_oil_4)
            add(recipe.input)
            player.withProtectedAccess { deps.preparation.start(this, recipe, quantity = 1) }
            assertMessageSent("You need to be on a members' world to prepare pyre logs.")
            assertEquals(1, player.count(recipe.input))

            preparePlayer(questState = PyreLogPreparation.MIN_QUEST_STATE - 1)
            add(objs.sacred_oil_4)
            add(recipe.input)
            player.withProtectedAccess { deps.preparation.start(this, recipe, quantity = 1) }
            assertMessageSent(
                "You need to progress further in Shades of Mort'ton before preparing pyre logs."
            )
            assertEquals(1, player.count(recipe.input))

            preparePlayer()
            add(objs.sacred_oil_3)
            add(recipe.input)
            val oilSlot = player.inv.indexOfFirst { it?.id == objs.sacred_oil_3.id }
            val logSlot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            player.withProtectedAccess {
                deps.preparation.start(
                    this,
                    recipe,
                    quantity = 1,
                    firstOilSlot = oilSlot,
                    firstOilId = objs.sacred_oil_3.id,
                    firstLogSlot = logSlot,
                )
            }
            assertMessageSent("You do not have enough sacred oil to prepare these logs.")
            assertEquals(1, player.count(recipe.input))

            preparePlayer()
            add(objs.sacred_oil_4)
            player.withProtectedAccess { deps.preparation.start(this, recipe, quantity = 1) }
            assertMessageSent("You do not have any redwood logs left.")
            assertEquals(1, player.count(objs.sacred_oil_4))
        }

    @Test
    fun GameTestState.`quest preparation state permits only the required normal pyre logs`() =
        runFiremakingTest { deps ->
            preparePlayer(questState = PyreLogPreparation.MIN_QUEST_STATE)
            add(objs.sacred_oil_4, 2)
            add(objs.logs)
            add(objs.redwood_logs)

            player.withProtectedAccess {
                deps.preparation.process(
                    this,
                    PyreLogPreparationRequest(objs.logs.id, remaining = 1),
                )
            }
            assertEquals(1, player.count(objs.pyre_logs))

            player.withProtectedAccess {
                deps.preparation.process(
                    this,
                    PyreLogPreparationRequest(objs.redwood_logs.id, remaining = 1),
                )
            }
            assertEquals(0, player.count(objs.redwood_pyre_logs))
            assertEquals(1, player.count(objs.redwood_logs))
        }

    @Test
    fun GameTestState.`selected material changes while the menu is open cancel safely`() =
        runFiremakingTest { deps ->
            val recipe = PyreLogRecipes.byInput.getValue(objs.oak_logs.id)
            preparePlayer()
            add(objs.sacred_oil_4)
            add(recipe.input, 2)
            val oilSlot = player.inv.indexOfFirst { it?.id == objs.sacred_oil_4.id }
            val logSlot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            player.withProtectedAccess { deps.preparation.select(this, oilSlot, logSlot, recipe) }
            player.inv[oilSlot] = InvObj(objs.sacred_oil_3)
            resumeSkillMulti(quantity = 2)
            assertEquals(0, player.count(recipe.output))
            assertEquals(2, player.count(recipe.input))
        }

    @Test
    fun GameTestState.`mid-batch resource loss stops automatic preparation safely`() =
        runFiremakingTest { deps ->
            val recipe = PyreLogRecipes.byInput.getValue(objs.oak_logs.id)
            preparePlayer()
            add(objs.sacred_oil_4)
            add(recipe.input, 2)
            player.withProtectedAccess { deps.preparation.start(this, recipe, quantity = 2) }
            assertEquals(1, player.count(recipe.output))
            player.inv[player.inv.indexOfFirst { it?.id == objs.sacred_oil_2.id }] = null
            advance(PyreLogPreparation.ACTION_TICKS)
            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.count(recipe.input))
            assertEquals(0, player.weakQueueList.count(firemaking_queues.pyreLogPreparation))
        }

    @Test
    fun GameTestState.`full inventory replacement is atomic and lighting interrupts preparation`() =
        runFiremakingTest { deps ->
            val recipe = PyreLogRecipes.byInput.getValue(objs.redwood_logs.id)
            preparePlayer()
            add(objs.sacred_oil_4)
            add(recipe.input)
            add(objs.leather, 26)
            assertEquals(0, player.inv.freeSpace())
            player.withProtectedAccess {
                deps.preparation.process(
                    this,
                    PyreLogPreparationRequest(recipe.input.id, remaining = 1),
                )
            }
            assertEquals(1, player.count(recipe.output))
            assertEquals(1, player.count(objs.empty_vial))
            assertEquals(0, player.inv.freeSpace())

            preparePlayer()
            add(objs.sacred_oil_4)
            add(objs.oak_logs, 2)
            add(objs.tinderbox)
            add(objs.logs)
            player.withProtectedAccess {
                deps.preparation.start(
                    this,
                    PyreLogRecipes.byInput.getValue(objs.oak_logs.id),
                    quantity = 2,
                )
            }
            assertEquals(1, player.weakQueueList.count(firemaking_queues.pyreLogPreparation))
            val logSlot = player.inv.indexOfFirst { it?.id == objs.logs.id }
            player.withProtectedAccess {
                deps.firemaking.lightFromInventory(
                    this,
                    logSlot,
                    FiremakingRecipes.byLog.getValue(objs.logs.id),
                )
            }
            assertEquals(0, player.weakQueueList.count(firemaking_queues.pyreLogPreparation))
            assertEquals(1, player.weakQueueList.count(firemaking_queues.attempt))
        }

    @Test
    fun GameTestState.`pyromancer pieces and full set boost preparation but warm gloves do not`() =
        runFiremakingTest { deps ->
            preparePlayer()
            assertEquals(1.0, deps.firemakingXp.multiplier(player))
            player.worn[0] = InvObj(objs.pyromancer_hood)
            assertEquals(1.004, deps.firemakingXp.multiplier(player))
            player.worn[1] = InvObj(objs.pyromancer_garb)
            player.worn[2] = InvObj(objs.pyromancer_robe)
            player.worn[3] = InvObj(objs.pyromancer_boots)
            player.worn[4] = InvObj(objs.warm_gloves)
            assertEquals(1.025, deps.firemakingXp.multiplier(player))

            add(objs.sacred_oil_4)
            add(objs.redwood_logs)
            player.withProtectedAccess {
                deps.preparation.process(
                    this,
                    PyreLogPreparationRequest(objs.redwood_logs.id, remaining = 1),
                )
            }
            assertEquals(205, firemakingXp())

            preparePlayer()
            player.worn[0] = InvObj(objs.warm_gloves)
            assertEquals(1.0, deps.firemakingXp.multiplier(player))
        }

    private fun GameTestScope.preparePlayer(
        members: Boolean = true,
        questState: Int = PyreLogPreparation.QUEST_COMPLETE_STATE,
    ) {
        player.clearAllInvs()
        player.observerUUID = 1L
        player.members = members
        player.stats[stats.firemaking] = 99
        player.statMap.setXP(stats.firemaking, 0)
        player.setVarp(varps.morttonquest, questState)
        player.weakQueueList.clear()
        client.clear()
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

    private fun GameTestScope.firemakingXp(): Int = player.statMap.getFineXP(stats.firemaking)

    private fun GameTestScope.resumeSkillMulti(quantity: Int) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(
            ResumePauseButtonInput(components.skillmulti_a, subcomponent = quantity)
        )
    }

    private fun GameTestState.runFiremakingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            FiremakingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(FiremakingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val preparation: PyreLogPreparation,
        val firemaking: Firemaking,
        val firemakingXp: FiremakingXp,
        val heldU: HeldUInteractions,
        val objTypes: ObjTypeList,
        val dbRows: DbRowTypeList,
    )

    private data class OilCase(val log: ObjType, val oil: ObjType, val remainder: ObjType)

    private companion object {
        const val SHADES_OF_MORTTON_ROW = 128
        const val QUEST_COMPLETE_COLUMN = 19
    }
}
