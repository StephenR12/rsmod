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
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BlackDyeMakingTest {
    @Test
    fun GameTestState.`cache 233 pins black dye contract and dispatch`() = runDyeTest { deps ->
        assertEquals(229, objs.empty_vial.id)
        assertEquals(233, objs.pestle_and_mortar.id)
        assertEquals(4620, objs.black_mushroom.id)
        assertEquals(4622, objs.black_dye.id)
        assertEquals(437, varps.golem_quest.id)
        assertEquals(364, seqs.human_herbing_grind.id)
        assertTrue(deps.objs[objs.black_mushroom].members)
        assertTrue(deps.objs[objs.black_dye].members)
        assertTrue(
            deps.eventBus.contains(
                HeldUEvents.Type::class.java,
                EventBus.composeLongKey(objs.pestle_and_mortar.id, objs.black_mushroom.id),
            )
        )
    }

    @Test
    fun GameTestState.`black dye is immediate atomic and uses the vial slot`() =
        runDyeTest { deps ->
            preparePlayer(questState = 1)
            for (slot in player.inv.indices) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.inv[3] = InvObj(objs.pestle_and_mortar)
            player.inv[8] = InvObj(objs.black_mushroom)
            player.inv[17] = InvObj(objs.empty_vial)
            player.withProtectedAccess { deps.dye.start(this, 8) }
            assertEquals(null, player.inv[8])
            assertEquals(objs.black_dye.id, player.inv[17]?.id)
            assertEquals(27, player.inv.count { it != null })
            assertEquals(seqs.human_herbing_grind.id, player.pendingSequence.id)
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`black dye requires The Golem to be started and a vial`() =
        runDyeTest { deps ->
            preparePlayer(questState = 0)
            player.inv[3] = InvObj(objs.pestle_and_mortar)
            player.inv[8] = InvObj(objs.black_mushroom)
            player.inv[17] = InvObj(objs.empty_vial)
            player.withProtectedAccess { deps.dye.start(this, 8) }
            assertEquals(objs.black_mushroom.id, player.inv[8]?.id)
            assertEquals(objs.empty_vial.id, player.inv[17]?.id)
            assertEquals(0, player.count(objs.black_dye))

            player.setVarp(varps.golem_quest, 1)
            player.inv[17] = null
            player.withProtectedAccess { deps.dye.start(this, 8) }
            assertEquals(objs.black_mushroom.id, player.inv[8]?.id)
            assertEquals(0, player.count(objs.black_dye))
        }

    @Test
    fun GameTestState.`both orders work after starting quest and free worlds reject`() =
        runDyeTest { deps ->
            repeat(2) { reverse ->
                preparePlayer(questState = 1)
                player.inv[3] = InvObj(objs.pestle_and_mortar)
                player.inv[8] = InvObj(objs.black_mushroom)
                player.inv[17] = InvObj(objs.empty_vial)
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                            deps.objs[objs.black_mushroom],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.black_mushroom],
                            8,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                        )
                    }
                }
                assertEquals(objs.black_dye.id, player.inv[17]?.id, "reverse=$reverse")
            }

            preparePlayer(questState = 1, members = false)
            player.inv[3] = InvObj(objs.pestle_and_mortar)
            player.inv[8] = InvObj(objs.black_mushroom)
            player.inv[17] = InvObj(objs.empty_vial)
            player.withProtectedAccess { deps.dye.start(this, 8) }
            assertEquals(objs.black_mushroom.id, player.inv[8]?.id)
            assertEquals(objs.empty_vial.id, player.inv[17]?.id)
            assertEquals(0, player.count(objs.black_dye))
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    private fun GameTestScope.preparePlayer(questState: Int, members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.herblore, 1)
        player.setCurrentLevel(stats.herblore, 1)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, 0)
        player.setVarp(varps.golem_quest, questState)
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestState.runDyeTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, TestModule, HerbloreScript::class, testBody = body)

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val dye: BlackDyeMaking,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
