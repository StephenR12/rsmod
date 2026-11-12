package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class GorakClawCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins the Gorak claw contract and dispatch`() =
        runCrushingTest { deps ->
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(9016, objs.gorak_claws.id)
            assertEquals(9018, objs.gorak_claw_powder.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            assertEquals(11095, seqs.human_herbing_grind_restart.id)
            assertTrue(deps.objs[objs.gorak_claws].members)
            assertTrue(deps.objs[objs.gorak_claw_powder].members)
            assertTrue(
                deps.eventBus.contains(
                    HeldUEvents.Type::class.java,
                    EventBus.composeLongKey(objs.pestle_and_mortar.id, objs.gorak_claws.id),
                )
            )
        }

    @Test
    fun GameTestState.`Gorak claws crush immediately then every three ticks with zero xp`() =
        runCrushingTest { deps ->
            preparePlayer(level = 1, questState = 0)
            add(objs.pestle_and_mortar)
            add(objs.gorak_claws, 3)

            val slot = player.inv.indexOfFirst { it?.id == objs.gorak_claws.id }
            player.withProtectedAccess { deps.crushing.start(this, slot) }
            assertEquals(1, player.count(objs.gorak_claw_powder))
            assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
            advance(2)
            assertEquals(1, player.count(objs.gorak_claw_powder))
            advance()
            assertEquals(2, player.count(objs.gorak_claw_powder))
            advance(2)
            assertEquals(2, player.count(objs.gorak_claw_powder))
            advance()
            assertEquals(3, player.count(objs.gorak_claw_powder))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
            assertEquals(0, player.weakQueueList.count(herblore_queues.gorakClawCrushing))
        }

    @Test
    fun GameTestState.`both orders work for members and free worlds fail atomically`() =
        runCrushingTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[3] = InvObj(objs.pestle_and_mortar)
                player.inv[8] = InvObj(objs.gorak_claws)
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                            deps.objs[objs.gorak_claws],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.gorak_claws],
                            8,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                        )
                    }
                }
                assertEquals(objs.gorak_claw_powder.id, player.inv[8]?.id, "reverse=$reverse")
            }

            preparePlayer(members = false)
            add(objs.pestle_and_mortar)
            add(objs.gorak_claws)
            val slot = player.inv.indexOfFirst { it?.id == objs.gorak_claws.id }
            player.withProtectedAccess { deps.crushing.start(this, slot) }
            assertEquals(1, player.count(objs.gorak_claws))
            assertEquals(0, player.count(objs.gorak_claw_powder))
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    @Test
    fun GameTestState.`full inventory and missing pestle remain safe`() = runCrushingTest { deps ->
        preparePlayer()
        player.inv[0] = InvObj(objs.pestle_and_mortar)
        player.inv[9] = InvObj(objs.gorak_claws)
        for (slot in 1 until player.inv.size) {
            if (slot != 9) {
                player.inv[slot] = InvObj(objs.coins)
            }
        }
        player.withProtectedAccess { deps.crushing.start(this, 9) }
        assertEquals(objs.gorak_claw_powder.id, player.inv[9]?.id)
        assertEquals(28, player.inv.count { it != null })

        preparePlayer()
        add(objs.pestle_and_mortar)
        add(objs.gorak_claws, 2)
        val inputSlot = player.inv.indexOfFirst { it?.id == objs.gorak_claws.id }
        player.withProtectedAccess { deps.crushing.start(this, inputSlot) }
        player.inv[player.inv.indexOfFirst { it?.id == objs.pestle_and_mortar.id }] = null
        advance(3)
        assertEquals(1, player.count(objs.gorak_claw_powder))
        assertEquals(1, player.count(objs.gorak_claws))
        assertEquals(0, player.weakQueueList.count(herblore_queues.gorakClawCrushing))
    }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        members: Boolean = true,
        questState: Int = 4,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, level)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestState.runCrushingTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val crushing: GorakClawCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
