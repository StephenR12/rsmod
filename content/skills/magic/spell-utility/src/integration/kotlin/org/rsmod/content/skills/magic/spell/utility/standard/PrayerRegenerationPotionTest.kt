package org.rsmod.content.skills.magic.spell.utility.standard

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
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.herblore.HerbloreModule
import org.rsmod.content.skills.herblore.HerbloreScript
import org.rsmod.content.skills.herblore.PotionConsumptionRepository
import org.rsmod.content.skills.herblore.PrayerRegenerationPotionDoses
import org.rsmod.content.skills.herblore.StandardPotionDrinking
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class PrayerRegenerationPotionTest {
    @Test
    fun GameTestState.`cache 233 identities descriptions timer and dispatch are pinned`() =
        runPotionTest { deps ->
            val doses = PrayerRegenerationPotionDoses.all

            assertEquals(listOf(30125, 30128, 30131, 30134), doses.map { it.potion.id })
            assertEquals(
                listOf(30128, 30131, 30134, objs.empty_vial.id),
                doses.map { it.replacement.id },
            )
            assertEquals(listOf(3, 2, 1, 0), doses.map { it.dosesRemaining })
            assertEquals(
                listOf(
                    "4 doses of Prayer Regeneration potion.",
                    "3 doses of Prayer Regeneration potion.",
                    "2 doses of Prayer Regeneration potion.",
                    "1 dose of Prayer Regeneration potion.",
                ),
                doses.map { deps.objs[it.potion].desc },
            )
            assertEquals(11361, varbits.prayer_regeneration_potion_timer.id)
            for (dose in doses) {
                assertEquals("Drink", deps.objs[dose.potion].iop[0])
                assertTrue(deps.objs[dose.potion].members)
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState.`all doses consume in place and each starts sixty six restores`() =
        runPotionTest { deps ->
            for (dose in PrayerRegenerationPotionDoses.all) {
                prepare(deps)
                repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
                player.inv[SLOT] = InvObj(dose.potion)

                drink(deps)

                assertEquals(dose.replacement.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertEquals(
                    PrayerRegenerationPotionRepository.RESTORES_PER_DOSE,
                    deps.regeneration.restoresRemaining(player),
                )
                assertEquals(
                    PrayerRegenerationPotionRepository.RESTORES_PER_DOSE,
                    player.vars[varbits.prayer_regeneration_potion_timer],
                )
                assertMessageSent(dose.drinkMessage)
            }
        }

    @Test
    fun GameTestState.`final dose honors vial smashing and free worlds reject atomically`() =
        runPotionTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.auto_smash_vials, 1)
            player.inv[SLOT] = InvObj(objs.prayer_regeneration_potion_1)

            drink(deps)

            assertEquals(null, player.inv[SLOT])
            assertTrue(deps.regeneration.isActive(player))
            assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)

            prepare(deps)
            player.members = false
            player.inv[SLOT] = InvObj(objs.prayer_regeneration_potion_4)
            drink(deps)

            assertEquals(objs.prayer_regeneration_potion_4.id, player.inv[SLOT]?.id)
            assertFalse(deps.regeneration.isActive(player))
            assertEquals(0, player.vars[varbits.prayer_regeneration_potion_timer])
            assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)
        }

    @Test
    fun GameTestState.`effect restores one point every twelve cycles exactly sixty six times`() =
        runPotionTest { deps ->
            prepare(deps, prayer = 99, currentPrayer = 0)
            player.inv[SLOT] = InvObj(objs.prayer_regeneration_potion_4)

            drink(deps)

            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES - 1)
            assertEquals(0, player.stat(stats.prayer))
            assertEquals(66, deps.regeneration.restoresRemaining(player))
            advance()
            assertEquals(1, player.stat(stats.prayer))
            assertEquals(65, deps.regeneration.restoresRemaining(player))

            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES * 64)
            assertEquals(65, player.stat(stats.prayer))
            assertEquals(1, deps.regeneration.restoresRemaining(player))
            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES)
            assertEquals(66, player.stat(stats.prayer))
            assertFalse(deps.regeneration.isActive(player))
            assertEquals(0, player.vars[varbits.prayer_regeneration_potion_timer])
        }

    @Test
    fun GameTestState.`restoration caps at base and holy equipment never increases a grant`() =
        runPotionTest { deps ->
            prepare(deps, prayer = 99, currentPrayer = 98)
            player.inv[0] = InvObj(objs.holy_wrench)
            player.worn[1] = InvObj(objs.prayer_skillcape)
            player.worn[12] = InvObj(objs.ring_of_the_gods_i)

            deps.regeneration.apply(player)
            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES)

            assertEquals(99, player.stat(stats.prayer))
            assertEquals(65, deps.regeneration.restoresRemaining(player))
            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES)
            assertEquals(99, player.stat(stats.prayer))
            assertEquals(64, deps.regeneration.restoresRemaining(player))
        }

    @Test
    fun GameTestState.`another dose resets rather than stacks the remaining grants and phase`() =
        runPotionTest { deps ->
            prepare(deps, prayer = 99, currentPrayer = 0)
            player.inv[SLOT] = InvObj(objs.prayer_regeneration_potion_2)

            drink(deps)
            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES * 2)
            assertEquals(2, player.stat(stats.prayer))
            assertEquals(64, deps.regeneration.restoresRemaining(player))
            advance(StandardPotionDrinking.POTION_DELAY_CYCLES)

            drink(deps)
            assertEquals(66, deps.regeneration.restoresRemaining(player))
            advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES - 1)
            assertEquals(2, player.stat(stats.prayer))
            advance()
            assertEquals(3, player.stat(stats.prayer))
            assertEquals(65, deps.regeneration.restoresRemaining(player))
        }

    @Test
    fun GameTestState.`death clears the effect and no later grant occurs`() =
        runPotionTest { deps ->
            prepare(deps, prayer = 99, currentPrayer = 0)
            deps.regeneration.apply(player)

            deps.events.publish(PlayerDeathEvent.Completed(player))
            advance(PrayerRegenerationPotionRepository.TOTAL_ACTIVE_CYCLES)

            assertEquals(0, player.stat(stats.prayer))
            assertFalse(deps.regeneration.isActive(player))
            assertEquals(0, player.vars[varbits.prayer_regeneration_potion_timer])
        }

    @Test
    fun GameTestState.`logout pauses the partial interval and login resumes it`() =
        runPotionTest { deps ->
            prepare(deps, prayer = 99, currentPrayer = 0)
            deps.regeneration.apply(player)
            advance(5)

            deps.events.publish(SessionStateEvent.Logout(player))
            advance(20)
            assertEquals(0, player.stat(stats.prayer))
            assertEquals(66, deps.regeneration.restoresRemaining(player))

            deps.events.publish(SessionStateEvent.Login(player))
            advance(6)
            assertEquals(0, player.stat(stats.prayer))
            advance()
            assertEquals(1, player.stat(stats.prayer))
            assertEquals(65, deps.regeneration.restoresRemaining(player))
        }

    @Test
    fun GameTestState.`persisted display count reconstructs on login`() = runPotionTest { deps ->
        prepare(deps, prayer = 99, currentPrayer = 0)
        player.setVarBit(varbits.prayer_regeneration_potion_timer, 5)

        deps.events.publish(SessionStateEvent.Login(player))
        advance(PrayerRegenerationPotionRepository.RESTORE_INTERVAL_CYCLES)

        assertEquals(1, player.stat(stats.prayer))
        assertEquals(4, deps.regeneration.restoresRemaining(player))
    }

    private fun GameTestScope.prepare(
        deps: Dependencies,
        prayer: Int = 99,
        currentPrayer: Int = prayer,
    ) {
        player.clearAllInvs()
        player.members = true
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        player.setBaseLevel(stats.prayer, prayer)
        player.setCurrentLevel(stats.prayer, currentPrayer)
        deps.potions.clear(player)
        deps.regeneration.clear(player)
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, SLOT, HeldOp.Op1) }
    }

    private fun GameTestState.runPotionTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            HerbloreScript::class,
            PotionProtectionScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val regeneration: PrayerRegenerationPotionRepository,
        val potions: PotionConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private companion object {
        const val SLOT = 6
    }
}
