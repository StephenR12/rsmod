package org.rsmod.content.skills.fletching.logcutting

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
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class LogFletchingTest {
    @Test
    fun GameTestState.`cache 233 exposes every standard log product tool and animation`() =
        runLogFletchingTest { deps ->
            assertEquals(36, deps.recipes.recipes.size)
            assertEquals(
                listOf(1511, 1521, 1519, 1517, 1515, 1513, 6333, 6332, 19669, 31032),
                deps.recipes.logs.map { it.id },
            )
            assertEquals(946, objs.knife.id)
            assertEquals(31043, objs.fletching_knife.id)
            assertEquals(52, objs.arrow_shafts.id)
            assertEquals(19584, objs.javelin_shafts.id)
            assertEquals(31049, objs.redwood_hiking_staff.id)
            assertEquals(1248, seqs.human_fletching.id)
        }

    @Test
    fun GameTestState.`recipe table pins exact levels materials outputs quantities and xp`() =
        runLogFletchingTest { deps ->
            assertEquals(
                listOf(
                    "Arrow shafts:1:1511:1:52:15:5.0",
                    "Javelin shafts:3:1511:1:19584:15:5.0",
                    "Shortbow (u):5:1511:1:50:1:5.0",
                    "Longbow (u):10:1511:1:48:1:10.0",
                    "Wooden stock:9:1511:1:9440:1:6.0",
                    "Arrow shafts:15:1521:1:52:30:10.0",
                    "Oak shortbow (u):20:1521:1:54:1:16.5",
                    "Oak longbow (u):25:1521:1:56:1:25.0",
                    "Oak stock:24:1521:1:9442:1:16.0",
                    "Oak shield:27:1521:2:22251:1:50.0",
                    "Arrow shafts:30:1519:1:52:45:15.0",
                    "Willow shortbow (u):35:1519:1:60:1:33.3",
                    "Willow longbow (u):40:1519:1:58:1:41.5",
                    "Willow stock:39:1519:1:9444:1:22.0",
                    "Willow shield:42:1519:2:22254:1:83.0",
                    "Arrow shafts:45:1517:1:52:60:20.0",
                    "Maple shortbow (u):50:1517:1:64:1:50.0",
                    "Maple longbow (u):55:1517:1:62:1:58.3",
                    "Maple stock:54:1517:1:9448:1:32.0",
                    "Maple shield:57:1517:2:22257:1:116.5",
                    "Arrow shafts:60:1515:1:52:75:25.0",
                    "Yew shortbow (u):65:1515:1:68:1:67.5",
                    "Yew longbow (u):70:1515:1:66:1:75.0",
                    "Yew stock:69:1515:1:9452:1:50.0",
                    "Yew shield:72:1515:2:22260:1:150.0",
                    "Arrow shafts:75:1513:1:52:90:30.0",
                    "Magic shortbow (u):80:1513:1:72:1:83.3",
                    "Magic longbow (u):85:1513:1:70:1:91.5",
                    "Magic stock:78:1513:1:21952:1:70.0",
                    "Magic shield:87:1513:2:22263:1:183.0",
                    "Teak stock:46:6333:1:9446:1:27.0",
                    "Mahogany stock:61:6332:1:9450:1:41.0",
                    "Arrow shafts:90:19669:1:52:105:35.0",
                    "Redwood hiking staff:90:19669:1:31049:1:10.5",
                    "Redwood shield:92:19669:2:22266:1:216.0",
                    "Atlatl dart shafts:74:31032:1:31004:100:30.0",
                ),
                deps.recipes.recipes.map {
                    "${it.name}:${it.level}:${it.log.id}:${it.logCount}:${it.output.id}:" +
                        "${it.outputCount}:${it.xp}"
                },
            )
        }

    @Test
    fun GameTestState.`normal logs open the native five-product quantity menu in order`() =
        runLogFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.logs, 3)

            player.withProtectedAccess { deps.fletching.select(this, objs.logs) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    3,
                    52,
                    19584,
                    50,
                    48,
                    9440,
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
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 15..19
                }
            assertEquals(5, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 3 })
        }

    @Test
    fun GameTestState.`each log family exposes its complete standard cache 233 choice order`() =
        runLogFletchingTest { deps ->
            assertEquals(
                mapOf(
                    1511 to listOf(52, 19584, 50, 48, 9440),
                    1521 to listOf(52, 54, 56, 9442, 22251),
                    1519 to listOf(52, 60, 58, 9444, 22254),
                    1517 to listOf(52, 64, 62, 9448, 22257),
                    1515 to listOf(52, 68, 66, 9452, 22260),
                    1513 to listOf(52, 72, 70, 21952, 22263),
                    6333 to listOf(9446),
                    6332 to listOf(9450),
                    19669 to listOf(52, 31049, 22266),
                    31032 to listOf(31004),
                ),
                deps.recipes.logs.associate { log ->
                    log.id to deps.recipes.forLog(log).map { it.output.id }
                },
            )
        }

    @Test
    fun GameTestState.`all recipes consume exact logs create exact output and award exact xp`() =
        runLogFletchingTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(objs.knife)
                add(recipe.log, recipe.logCount)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess {
                    deps.fletching.process(
                        this,
                        LogFletchingRequest(recipe.log, recipe.output, remaining = 1),
                    )
                }

                assertEquals(0, player.count(recipe.log), recipe.name)
                assertEquals(recipe.outputCount, player.count(recipe.output), recipe.name)
                assertEquals(
                    (recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.fletching) - startXp,
                    recipe.name,
                )
            }
        }

    @Test
    fun GameTestState.`membership level knife and two-log checks are atomic`() =
        runLogFletchingTest { deps ->
            val magicLongbow =
                deps.recipes.recipes.single { it.output == objs.unstrung_magic_longbow }
            val redwoodShield = deps.recipes.recipes.single { it.output == objs.redwood_shield }

            preparePlayer()
            player.members = false
            add(objs.knife)
            add(objs.logs)
            player.withProtectedAccess { deps.fletching.select(this, objs.logs) }
            assertMessageSent("You need to be on a members' world to train Fletching.")
            assertEquals(1, player.count(objs.logs))

            preparePlayer()
            add(objs.logs)
            player.withProtectedAccess { deps.fletching.select(this, objs.logs) }
            assertMessageSent("You need a knife to fletch logs.")
            assertEquals(1, player.count(objs.logs))

            preparePlayer()
            player.worn[Wearpos.LeftHand.slot] = InvObj(objs.fletching_knife)
            add(objs.logs)
            player.withProtectedAccess { deps.fletching.select(this, objs.logs) }
            assertMessageSent("You need a knife to fletch logs.")
            assertEquals(1, player.count(objs.logs))

            preparePlayer()
            player.stats[stats.fletching] = 84
            add(objs.knife)
            add(objs.magic_logs)
            player.withProtectedAccess { deps.fletching.start(this, magicLongbow, quantity = 1) }
            assertMessageSent("You need a Fletching level of 85 to make this item.")
            assertEquals(1, player.count(objs.magic_logs))

            preparePlayer()
            add(objs.knife)
            add(objs.redwood_logs)
            player.withProtectedAccess { deps.fletching.start(this, redwoodShield, quantity = 1) }
            assertMessageSent("You do not have enough logs to make that.")
            assertEquals(1, player.count(objs.redwood_logs))
        }

    @Test
    fun GameTestState.`ordinary knife uses two-tick first action then three-tick repeats`() =
        runLogFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.logs, 3)
            val shortbow = deps.recipes.recipes.single { it.output == objs.unstrung_shortbow }

            player.withProtectedAccess { deps.fletching.start(this, shortbow, quantity = 3) }
            advance()
            assertEquals(0, player.count(objs.unstrung_shortbow))
            advance()
            assertEquals(1, player.count(objs.unstrung_shortbow))
            advance(2)
            assertEquals(1, player.count(objs.unstrung_shortbow))
            advance()
            assertEquals(2, player.count(objs.unstrung_shortbow))
            advance(3)
            assertEquals(3, player.count(objs.unstrung_shortbow))
            assertEquals(0, player.weakQueueList.count(fletching_queues.logCutting))
        }

    @Test
    fun GameTestState.`fletching knife bonus works from inventory and off-hand after first item`() =
        runLogFletchingTest { deps ->
            val shortbow = deps.recipes.recipes.single { it.output == objs.unstrung_shortbow }

            preparePlayer()
            add(objs.fletching_knife)
            add(objs.logs, 3)
            player.withProtectedAccess { deps.fletching.start(this, shortbow, quantity = 3) }
            advance(2)
            assertEquals(1, player.count(objs.unstrung_shortbow))
            advance()
            assertEquals(1, player.count(objs.unstrung_shortbow))
            advance()
            assertEquals(2, player.count(objs.unstrung_shortbow))
            advance(2)
            assertEquals(3, player.count(objs.unstrung_shortbow))

            preparePlayer()
            add(objs.knife)
            add(objs.logs, 2)
            player.worn[Wearpos.LeftHand.slot] = InvObj(objs.fletching_knife)
            player.withProtectedAccess { deps.fletching.start(this, shortbow, quantity = 2) }
            advance(2)
            assertEquals(1, player.count(objs.unstrung_shortbow))
            advance(2)
            assertEquals(2, player.count(objs.unstrung_shortbow))
        }

    @Test
    fun GameTestState.`removing the inventory knife interrupts the batch without mutation`() =
        runLogFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.oak_logs, 2)
            val stock = deps.recipes.recipes.single { it.output == objs.oak_stock }

            player.withProtectedAccess { deps.fletching.start(this, stock, quantity = 2) }
            advance(2)
            assertEquals(1, player.count(objs.oak_stock))
            player.inv[player.inv.indexOfFirst { it?.id == objs.knife.id }] = null
            advance(3)

            assertEquals(1, player.count(objs.oak_stock))
            assertEquals(1, player.count(objs.oak_logs))
            assertEquals(0, player.weakQueueList.count(fletching_queues.logCutting))
        }

    @Test
    fun GameTestState.`held interaction accepts reverse order and starts selected output`() =
        runLogFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.logs)
            val knifeSlot = player.inv.indexOfFirst { it?.id == objs.knife.id }
            val logSlot = player.inv.indexOfFirst { it?.id == objs.logs.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.logs],
                    logSlot,
                    objTypes[objs.knife],
                    knifeSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_c, quantity = 1)
            advance(2)

            assertEquals(1, player.count(objs.unstrung_shortbow))
            assertEquals(1, player.count(objs.knife))
            assertEquals(0, player.count(objs.logs))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
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

    private fun GameTestState.runLogFletchingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            LogFletchingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(LogFletchingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val fletching: LogFletching,
        val recipes: LogFletchingRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
