package org.rsmod.content.skills.fletching.ogrearrowshafts

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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.ui.UserInterface

@Execution(ExecutionMode.SAME_THREAD)
class OgreArrowShaftFletchingTest {
    @Test
    fun GameTestState.`cache 233 pins the shared ogre log fletching contract`() =
        runOgreArrowShaftFletchingTest {
            assertEquals(2859, objs.wolf_bones.id)
            assertEquals(2862, objs.achey_tree_logs.id)
            assertEquals(2864, objs.ogre_arrow_shaft.id)
            assertEquals(4825, objs.unstrung_comp_ogre_bow.id)
            assertEquals(946, objs.knife.id)
            assertEquals(31043, objs.fletching_knife.id)
            assertEquals(4433, seqs.human_ogre_fletching.id)
            assertEquals(293, varps.big_chompy_bird_hunting.id)
            assertEquals(500, varbits.comp_ogre_bow_unlocked.id)
        }

    @Test
    fun GameTestState.`achey logs open the native two-product single-action menu`() =
        runOgreArrowShaftFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.achey_tree_logs)
            add(objs.wolf_bones)

            player.withProtectedAccess { deps.fletching.select(this) }

            assertTrue(client.filterIsInstance<IfOpenSub>().any { it.interfaceId == 270 })
            val setup = client.filterIsInstance<RunClientScript>().single { it.id == 2046 }
            assertEquals(
                listOf<Any>(
                    0,
                    1,
                    2864,
                    4825,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    -1,
                    1,
                    "What would you like to make?",
                ),
                setup.values,
            )
            val events =
                client.filterIsInstance<IfSetEventsV2>().filter {
                    it.interfaceId == 270 && it.componentId in 15..16
                }
            assertEquals(2, events.size)
            assertTrue(events.all { it.start == 1 && it.end == 1 })
        }

    @Test
    fun GameTestState
        .`one log yields the inclusive random range after three ticks with exact xp`() =
        runOgreArrowShaftFletchingTest { deps ->
            for (yield in 2..6) {
                preparePlayer()
                add(objs.knife)
                add(objs.achey_tree_logs, 2)
                random.next = yield
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess {
                    deps.fletching.start(this, OgreLogProduct.ArrowShafts)
                }

                assertEquals(seqs.human_ogre_fletching.id, player.pendingSequence.id)
                advance(2)
                assertEquals(0, player.count(objs.ogre_arrow_shaft))
                advance()
                assertEquals(1, player.count(objs.achey_tree_logs))
                assertEquals(yield, player.count(objs.ogre_arrow_shaft))
                assertEquals(yield * 16, player.statMap.getFineXP(stats.fletching) - startXp)
                assertEquals(0, player.weakQueueList.count(fletching_queues.ogreArrowShafts))
                advance(3)
                assertEquals(1, player.count(objs.achey_tree_logs))
                assertEquals(yield, player.count(objs.ogre_arrow_shaft))
            }
        }

    @Test
    fun GameTestState.`membership quest and boosted level requirements are atomic`() =
        runOgreArrowShaftFletchingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.knife)
            add(objs.achey_tree_logs)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.ArrowShafts) }
            assertMessageSent("You need to be on a members' world to fletch Achey tree logs.")
            assertEquals(1, player.count(objs.achey_tree_logs))

            preparePlayer()
            player.setVarp(varps.big_chompy_bird_hunting, 0)
            add(objs.knife)
            add(objs.achey_tree_logs)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.ArrowShafts) }
            assertMessageSent("You need to start Big Chompy Bird Hunting first.")
            assertEquals(1, player.count(objs.achey_tree_logs))

            preparePlayer()
            player.stats[stats.fletching] = 4
            add(objs.knife)
            add(objs.achey_tree_logs)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.ArrowShafts) }
            assertMessageSent("You need a Fletching level of 5 to make these shafts.")
            assertEquals(1, player.count(objs.achey_tree_logs))
        }

    @Test
    fun GameTestState.`composite bow uses one log and bone after three ticks for exact xp`() =
        runOgreArrowShaftFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.achey_tree_logs, 2)
            add(objs.wolf_bones, 2)
            val startXp = player.statMap.getFineXP(stats.fletching)

            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.CompositeBow) }

            assertEquals(seqs.human_ogre_fletching.id, player.pendingSequence.id)
            advance(2)
            assertEquals(0, player.count(objs.unstrung_comp_ogre_bow))
            advance()
            assertEquals(1, player.count(objs.achey_tree_logs))
            assertEquals(1, player.count(objs.wolf_bones))
            assertEquals(1, player.count(objs.unstrung_comp_ogre_bow))
            assertEquals(450, player.statMap.getFineXP(stats.fletching) - startXp)
            advance(3)
            assertEquals(1, player.count(objs.unstrung_comp_ogre_bow))
        }

    @Test
    fun GameTestState.`composite bow unlock level and wolf bone gates are atomic`() =
        runOgreArrowShaftFletchingTest { deps ->
            preparePlayer()
            player.setVarBit(varbits.comp_ogre_bow_unlocked, 0)
            add(objs.knife)
            add(objs.achey_tree_logs)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.CompositeBow) }
            assertMessageSent("You need to learn how to make a composite ogre bow first.")
            assertEquals(1, player.count(objs.achey_tree_logs))
            assertEquals(1, player.count(objs.wolf_bones))

            preparePlayer()
            player.stats[stats.fletching] = 29
            add(objs.knife)
            add(objs.achey_tree_logs)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.CompositeBow) }
            assertMessageSent("You need a Fletching level of 30 to make this bow.")
            assertEquals(1, player.count(objs.achey_tree_logs))
            assertEquals(1, player.count(objs.wolf_bones))

            preparePlayer()
            add(objs.knife)
            add(objs.achey_tree_logs)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.CompositeBow) }
            assertMessageSent("You need some wolf bones to make a composite ogre bow.")
            assertEquals(1, player.count(objs.achey_tree_logs))
        }

    @Test
    fun GameTestState.`fletching knife reverse menu use makes a bow from a full inventory`() =
        runOgreArrowShaftFletchingTest { deps ->
            preparePlayer()
            add(objs.fletching_knife)
            add(objs.achey_tree_logs)
            add(objs.wolf_bones, 26)
            assertEquals(0, player.inv.freeSpace())
            val knifeSlot = player.inv.indexOfFirst { it?.id == objs.fletching_knife.id }
            val logSlot = player.inv.indexOfFirst { it?.id == objs.achey_tree_logs.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.achey_tree_logs],
                    logSlot,
                    objTypes[objs.fletching_knife],
                    knifeSlot,
                )
            }
            resumeSkillMulti(components.skillmulti_b)
            advance(3)

            assertEquals(0, player.count(objs.achey_tree_logs))
            assertEquals(25, player.count(objs.wolf_bones))
            assertEquals(1, player.count(objs.unstrung_comp_ogre_bow))
            assertEquals(1, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.`resource loss and another fletching queue stop production safely`() =
        runOgreArrowShaftFletchingTest { deps ->
            preparePlayer()
            add(objs.knife)
            add(objs.achey_tree_logs)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.ArrowShafts) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.achey_tree_logs.id }] = null
            advance(3)
            assertEquals(0, player.count(objs.ogre_arrow_shaft))

            preparePlayer()
            add(objs.knife)
            add(objs.achey_tree_logs)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.fletching.start(this, OgreLogProduct.CompositeBow) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.wolf_bones.id }] = null
            advance(3)
            assertEquals(1, player.count(objs.achey_tree_logs))
            assertEquals(0, player.count(objs.unstrung_comp_ogre_bow))

            preparePlayer()
            add(objs.knife)
            add(objs.achey_tree_logs)
            player.withProtectedAccess {
                weakQueue(fletching_queues.ammunition, 20)
                deps.fletching.start(this, OgreLogProduct.ArrowShafts)
            }
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(1, player.weakQueueList.count(fletching_queues.ogreArrowShafts))

            player.withProtectedAccess { clearFletchingQueues() }
            assertEquals(0, player.weakQueueList.count(fletching_queues.ogreArrowShafts))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.fletching] = 99
        player.setVarp(varps.big_chompy_bird_hunting, 1)
        player.setVarBit(varbits.comp_ogre_bow_unlocked, 1)
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

    private fun GameTestScope.resumeSkillMulti(component: ComponentType) {
        val target =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id)))
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = 1))
    }

    private fun GameTestState.runOgreArrowShaftFletchingTest(
        body: GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            OgreArrowShaftFletchingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(OgreArrowShaftFletchingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val fletching: OgreArrowShaftFletching, val heldU: HeldUInteractions)
}
