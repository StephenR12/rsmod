package org.rsmod.content.skills.herblore

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
class KarambwanjiPasteCrushingTest {
    @Test
    fun GameTestState.`cache 233 pins both karambwanji paste contracts and dispatch`() =
        runCrushingTest { deps ->
            assertEquals(233, objs.pestle_and_mortar.id)
            assertEquals(3150, objs.raw_karambwanji.id)
            assertEquals(3151, objs.cooked_karambwanji.id)
            assertEquals(3155, objs.raw_karambwanji_paste.id)
            assertEquals(3156, objs.cooked_karambwanji_paste.id)
            assertEquals(364, seqs.human_herbing_grind.id)
            assertEquals(11095, seqs.human_herbing_grind_restart.id)
            assertTrue(deps.objs[objs.raw_karambwanji].stackable)
            assertFalse(deps.objs[objs.cooked_karambwanji].stackable)
            assertFalse(deps.objs[objs.raw_karambwanji_paste].stackable)
            assertFalse(deps.objs[objs.cooked_karambwanji_paste].stackable)
            for (input in listOf(objs.raw_karambwanji, objs.cooked_karambwanji)) {
                assertTrue(
                    deps.eventBus.contains(
                        HeldUEvents.Type::class.java,
                        EventBus.composeLongKey(objs.pestle_and_mortar.id, input.id),
                    )
                )
            }
        }

    @Test
    fun GameTestState.`raw karambwanji paste is immediate then repeats every three ticks`() =
        runCrushingTest { deps ->
            preparePlayer(level = 1, questState = 0)
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(objs.raw_karambwanji, 3)
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            assertEquals(1, player.count(objs.raw_karambwanji_paste))
            assertEquals(2, player.count(objs.raw_karambwanji))
            advance(2)
            assertEquals(1, player.count(objs.raw_karambwanji_paste))
            advance()
            assertEquals(2, player.count(objs.raw_karambwanji_paste))
            advance(2)
            assertEquals(2, player.count(objs.raw_karambwanji_paste))
            advance()
            assertEquals(3, player.count(objs.raw_karambwanji_paste))
            assertEquals(0, player.count(objs.raw_karambwanji))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState.`raw paste supports both orders and requires its starting tool`() =
        runCrushingTest { deps ->
            repeat(2) { reverse ->
                preparePlayer()
                player.inv[3] = InvObj(objs.pestle_and_mortar)
                player.inv[8] = InvObj(objs.raw_karambwanji)
                player.withProtectedAccess {
                    if (reverse == 0) {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                            deps.objs[objs.raw_karambwanji],
                            8,
                        )
                    } else {
                        deps.heldU.interact(
                            this,
                            player.inv,
                            deps.objs[objs.raw_karambwanji],
                            8,
                            deps.objs[objs.pestle_and_mortar],
                            3,
                        )
                    }
                }
                assertEquals(objs.raw_karambwanji_paste.id, player.inv[8]?.id, "reverse=$reverse")
            }

            preparePlayer()
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(objs.raw_karambwanji, 2)
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            player.inv[0] = null
            advance(3)
            assertEquals(1, player.count(objs.raw_karambwanji))
            assertEquals(1, player.count(objs.raw_karambwanji_paste))
        }

    @Test
    fun GameTestState.`raw paste handles stack capacity and free worlds atomically`() =
        runCrushingTest { deps ->
            preparePlayer()
            for (slot in player.inv.indices) {
                player.inv[slot] = InvObj(objs.coins)
            }
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(objs.raw_karambwanji, 2)
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            assertEquals(2, player.count(objs.raw_karambwanji))
            assertEquals(0, player.count(objs.raw_karambwanji_paste))

            player.inv[9] = InvObj(objs.raw_karambwanji)
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            assertEquals(objs.raw_karambwanji_paste.id, player.inv[9]?.id)
            assertEquals(28, player.inv.count { it != null })

            preparePlayer(members = false)
            player.inv[0] = InvObj(objs.pestle_and_mortar)
            player.inv[9] = InvObj(objs.raw_karambwanji)
            player.withProtectedAccess { deps.crushing.start(this, 9) }
            assertEquals(objs.raw_karambwanji.id, player.inv[9]?.id)
            assertEquals(0, player.count(objs.raw_karambwanji_paste))
            assertMessageSent("You need to be on a members' world to use this item.")
        }

    private fun GameTestScope.preparePlayer(
        level: Int = 99,
        members: Boolean = true,
        questState: Int = 4,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.cooking, level)
        player.setCurrentLevel(stats.cooking, level)
        player.statMap.setXP(stats.cooking, 0)
        player.setBaseLevel(stats.herblore, level)
        player.setCurrentLevel(stats.herblore, level)
        player.statMap.setXP(stats.herblore, 0)
        player.setVarp(varps.druidic_ritual, questState)
        player.weakQueueList.clear()
        client.clear()
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
        val crushing: KarambwanjiPasteCrushing,
        val heldU: HeldUInteractions,
        val eventBus: EventBus,
        val objs: ObjTypeList,
    )
}
