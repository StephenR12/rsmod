package org.rsmod.content.skills.crafting.wolfbonearrowtips

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class WolfboneArrowtipCuttingTest {
    @Test
    fun GameTestState.`cache 233 pins the wolfbone arrowtip contract`() =
        runWolfboneArrowtipCuttingTest {
            assertEquals(2859, objs.wolf_bones.id)
            assertEquals(2861, objs.wolfbone_arrowtips.id)
            assertEquals(1755, objs.chisel.id)
            assertEquals(4433, seqs.human_ogre_fletching.id)
            assertEquals(293, varps.big_chompy_bird_hunting.id)
        }

    @Test
    fun GameTestState.`one bone yields the inclusive random range after two ticks with dual xp`() =
        runWolfboneArrowtipCuttingTest { deps ->
            for (yield in 2..6) {
                preparePlayer()
                add(objs.chisel)
                add(objs.wolf_bones)
                random.next = yield
                val startCraftingXp = player.statMap.getFineXP(stats.crafting)
                val startFletchingXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess { deps.cutting.start(this) }

                assertEquals(seqs.human_ogre_fletching.id, player.pendingSequence.id)
                advance()
                assertEquals(0, player.count(objs.wolfbone_arrowtips))
                advance()
                assertEquals(0, player.count(objs.wolf_bones))
                assertEquals(yield, player.count(objs.wolfbone_arrowtips))
                assertEquals(yield * 25, player.statMap.getFineXP(stats.crafting) - startCraftingXp)
                assertEquals(
                    yield * 25,
                    player.statMap.getFineXP(stats.fletching) - startFletchingXp,
                )
                assertEquals(0, player.weakQueueList.count(fletching_queues.wolfboneArrowtips))
            }
        }

    @Test
    fun GameTestState.`membership quest and both boosted level requirements are atomic`() =
        runWolfboneArrowtipCuttingTest { deps ->
            preparePlayer()
            player.members = false
            add(objs.chisel)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.cutting.start(this) }
            assertMessageSent("You need to be on a members' world to make wolfbone arrowtips.")
            assertEquals(1, player.count(objs.wolf_bones))

            preparePlayer()
            player.setVarp(varps.big_chompy_bird_hunting, 0)
            add(objs.chisel)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.cutting.start(this) }
            assertMessageSent("You need to start Big Chompy Bird Hunting first.")
            assertEquals(1, player.count(objs.wolf_bones))

            preparePlayer()
            player.stats[stats.crafting] = 4
            add(objs.chisel)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.cutting.start(this) }
            assertMessageSent("You need a Crafting level of 5 to make these arrowtips.")
            assertEquals(1, player.count(objs.wolf_bones))

            preparePlayer()
            player.stats[stats.fletching] = 4
            add(objs.chisel)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.cutting.start(this) }
            assertMessageSent("You need a Fletching level of 5 to make these arrowtips.")
            assertEquals(1, player.count(objs.wolf_bones))
        }

    @Test
    fun GameTestState.`reverse held use works from a full inventory`() =
        runWolfboneArrowtipCuttingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.wolf_bones, 27)
            assertEquals(0, player.inv.freeSpace())
            random.next = 6
            val chiselSlot = player.inv.indexOfFirst { it?.id == objs.chisel.id }
            val bonesSlot = player.inv.indexOfFirst { it?.id == objs.wolf_bones.id }

            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[objs.wolf_bones],
                    bonesSlot,
                    objTypes[objs.chisel],
                    chiselSlot,
                )
            }
            advance(2)

            assertEquals(26, player.count(objs.wolf_bones))
            assertEquals(6, player.count(objs.wolfbone_arrowtips))
            assertEquals(0, player.inv.freeSpace())
        }

    @Test
    fun GameTestState.`resource loss and either skill queue cancellation stop production safely`() =
        runWolfboneArrowtipCuttingTest { deps ->
            preparePlayer()
            add(objs.chisel)
            add(objs.wolf_bones)
            player.withProtectedAccess { deps.cutting.start(this) }
            player.inv[player.inv.indexOfFirst { it?.id == objs.wolf_bones.id }] = null
            advance(2)
            assertEquals(0, player.count(objs.wolfbone_arrowtips))

            preparePlayer()
            add(objs.chisel)
            add(objs.wolf_bones)
            player.withProtectedAccess {
                weakQueue(crafting_queues.gemCutting, 20)
                weakQueue(fletching_queues.ammunition, 20)
                deps.cutting.start(this)
            }
            assertEquals(0, player.weakQueueList.count(crafting_queues.gemCutting))
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(1, player.weakQueueList.count(fletching_queues.wolfboneArrowtips))

            player.withProtectedAccess { clearCraftingQueues() }
            assertEquals(0, player.weakQueueList.count(fletching_queues.wolfboneArrowtips))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        player.members = true
        player.stats[stats.crafting] = 99
        player.stats[stats.fletching] = 99
        player.setVarp(varps.big_chompy_bird_hunting, 1)
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

    private fun GameTestState.runWolfboneArrowtipCuttingTest(
        body: GameTestScope.(Dependencies) -> Unit
    ) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            WolfboneArrowtipCuttingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(WolfboneArrowtipCuttingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(val cutting: WolfboneArrowtipCutting, val heldU: HeldUInteractions)
}
