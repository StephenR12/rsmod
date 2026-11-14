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
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.herblore.HerbloreModule
import org.rsmod.content.skills.herblore.HerbloreScript
import org.rsmod.content.skills.herblore.MenaphiteRemedyPotionDoses
import org.rsmod.content.skills.herblore.PotionConsumptionRepository
import org.rsmod.content.skills.herblore.StandardPotionDrinking
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.varbit.VarBitType

@Execution(ExecutionMode.SAME_THREAD)
class MenaphiteRemedyPotionTest {
    @Test
    fun GameTestState.`cache 238 identities descriptions timers and dispatch are pinned`() =
        runPotionTest { deps ->
            val doses = MenaphiteRemedyPotionDoses.all

            assertEquals(listOf(27202, 27205, 27208, 27211), doses.map { it.potion.id })
            assertEquals(
                listOf(27205, 27208, 27211, objs.empty_vial.id),
                doses.map { it.replacement.id },
            )
            assertEquals(listOf(3, 2, 1, 0), doses.map { it.dosesRemaining })
            assertEquals(
                listOf(
                    "4 doses of Menaphite remedy.",
                    "3 doses of Menaphite remedy.",
                    "2 doses of Menaphite remedy.",
                    "1 dose of Menaphite remedy.",
                ),
                doses.map { deps.objs[it.potion].desc },
            )
            assertEquals(14448, varbits.menaphite_remedy_timer.id)
            assertEquals(23, timers.menaphite_remedy_effect.id)
            assertEquals(
                listOf(8429, 8430, 8431, 8432, 8433, 13663, 13664, 13665, 14704),
                divineTimers.map { it.id },
            )
            for (dose in doses) {
                val type = deps.objs[dose.potion]
                assertEquals("Drink", type.iop[0])
                assertTrue(type.members)
                assertTrue(type.stockmarket)
                assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, dose.potion.id))
            }
        }

    @Test
    fun GameTestState.`all doses consume in place and start twenty delayed restores`() =
        runPotionTest { deps ->
            for (dose in MenaphiteRemedyPotionDoses.all) {
                prepare(deps)
                repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
                player.inv[SLOT] = InvObj(dose.potion)

                drink(deps)

                assertEquals(dose.replacement.id, player.inv[SLOT]?.id)
                assertEquals(player.inv.size, player.inv.count { it != null })
                assertEquals(seqs.human_eat.id, player.pendingSequence.id)
                assertEquals(
                    MenaphiteRemedyRepository.RESTORES_PER_DOSE,
                    deps.remedy.restoresRemaining(player),
                )
                assertEquals(
                    MenaphiteRemedyRepository.RESTORES_PER_DOSE,
                    player.vars[varbits.menaphite_remedy_timer],
                )
                assertMessageSent(dose.drinkMessage)
            }
        }

    @Test
    fun GameTestState.`final dose vial smashing access delay and stale slots are atomic`() =
        runPotionTest { deps ->
            prepare(deps)
            player.setVarBit(varbits.auto_smash_vials, 1)
            player.inv[SLOT] = InvObj(objs.menaphite_remedy_1)

            drink(deps)

            assertEquals(null, player.inv[SLOT])
            assertTrue(deps.remedy.isActive(player))
            assertMessageSent(StandardPotionDrinking.FINISHED_MESSAGE)

            prepare(deps, members = false)
            player.inv[SLOT] = InvObj(objs.menaphite_remedy_4)
            drink(deps)
            assertEquals(objs.menaphite_remedy_4.id, player.inv[SLOT]?.id)
            assertFalse(deps.remedy.isActive(player))
            assertMessageSent(StandardPotionDrinking.MEMBERS_MESSAGE)

            prepare(deps)
            player.inv[SLOT] = InvObj(objs.menaphite_remedy_4)
            drink(deps)
            val replacement = player.inv[SLOT]
            drink(deps)
            assertEquals(replacement, player.inv[SLOT])
            assertEquals(20, deps.remedy.restoresRemaining(player))

            prepare(deps)
            player.inv[SLOT] = InvObj(objs.logs)
            player.withProtectedAccess {
                deps.drinking.drink(this, SLOT, MenaphiteRemedyPotionDoses.all.first())
            }
            assertEquals(objs.logs.id, player.inv[SLOT]?.id)
            assertFalse(deps.remedy.isActive(player))
        }

    @Test
    fun GameTestState.`first pulse waits twenty five cycles and restores only five combat stats`() =
        runPotionTest { deps ->
            prepare(deps)
            setLevel(stats.attack, base = 100, current = 0)
            setLevel(stats.strength, base = 99, current = 0)
            setLevel(stats.defence, base = 50, current = 0)
            setLevel(stats.ranged, base = 1, current = 0)
            setLevel(stats.magic, base = 80, current = 90)
            setLevel(stats.hitpoints, base = 99, current = 1)
            setLevel(stats.prayer, base = 99, current = 0)
            deps.remedy.apply(player)

            advance(MenaphiteRemedyRepository.RESTORE_INTERVAL_CYCLES - 1)
            assertEquals(0, player.stat(stats.attack))
            assertEquals(20, deps.remedy.restoresRemaining(player))
            advance()

            assertEquals(22, player.stat(stats.attack))
            assertEquals(21, player.stat(stats.strength))
            assertEquals(14, player.stat(stats.defence))
            assertEquals(1, player.stat(stats.ranged))
            assertEquals(90, player.stat(stats.magic))
            assertEquals(1, player.stat(stats.hitpoints))
            assertEquals(0, player.stat(stats.prayer))
            assertEquals(19, deps.remedy.restoresRemaining(player))
            assertEquals(19, player.vars[varbits.menaphite_remedy_timer])
        }

    @Test
    fun GameTestState.`effect performs exactly twenty pulses and expires at five minutes`() =
        runPotionTest { deps ->
            prepare(deps)
            setLevel(stats.attack, base = 99, current = 0)
            deps.remedy.apply(player)

            repeat(MenaphiteRemedyRepository.RESTORES_PER_DOSE) { index ->
                advance(MenaphiteRemedyRepository.RESTORE_INTERVAL_CYCLES)
                assertEquals(
                    MenaphiteRemedyRepository.RESTORES_PER_DOSE - index - 1,
                    deps.remedy.restoresRemaining(player),
                )
            }

            assertEquals(99, player.stat(stats.attack))
            assertFalse(deps.remedy.isActive(player))
            assertEquals(0, player.vars[varbits.menaphite_remedy_timer])
            setLevel(stats.attack, base = 99, current = 0)
            advance(MenaphiteRemedyRepository.RESTORE_INTERVAL_CYCLES)
            assertEquals(0, player.stat(stats.attack))
        }

    @Test
    fun GameTestState.`redose resets the count and twenty five cycle phase`() =
        runPotionTest { deps ->
            prepare(deps)
            setLevel(stats.attack, base = 99, current = 0)
            player.inv[SLOT] = InvObj(objs.menaphite_remedy_4)

            drink(deps)
            advance(10)
            drink(deps)
            assertEquals(20, deps.remedy.restoresRemaining(player))
            advance(24)
            assertEquals(0, player.stat(stats.attack))
            advance()
            assertEquals(21, player.stat(stats.attack))
            assertEquals(19, deps.remedy.restoresRemaining(player))
        }

    @Test
    fun GameTestState.`consumption clears existing divine state without changing visible boosts`() =
        runPotionTest { deps ->
            prepare(deps)
            setLevel(stats.attack, base = 99, current = 118)
            setLevel(stats.strength, base = 99, current = 118)
            setLevel(stats.defence, base = 99, current = 118)
            setLevel(stats.ranged, base = 99, current = 112)
            setLevel(stats.magic, base = 99, current = 103)
            for (timer in divineTimers) {
                VarPlayerIntMapSetter.set(player, timer, 50)
            }
            player.inv[SLOT] = InvObj(objs.menaphite_remedy_4)

            drink(deps)

            assertTrue(divineTimers.all { player.vars[it] == 0 })
            assertFalse(deps.divine.anyActive(player))
            assertEquals(118, player.stat(stats.attack))
            assertEquals(118, player.stat(stats.strength))
            assertEquals(118, player.stat(stats.defence))
            assertEquals(112, player.stat(stats.ranged))
            assertEquals(103, player.stat(stats.magic))

            VarPlayerIntMapSetter.set(player, varbits.divine_ranging_timer, 50)
            advance(MenaphiteRemedyRepository.RESTORE_INTERVAL_CYCLES)
            assertEquals(50, player.vars[varbits.divine_ranging_timer])
        }

    @Test
    fun GameTestState.`resident logout preserves phase cold login reconstructs and death clears`() =
        runPotionTest { deps ->
            prepare(deps)
            setLevel(stats.attack, base = 99, current = 0)
            deps.remedy.apply(player)
            advance(7)

            deps.events.publish(SessionStateEvent.Logout(player))
            advance(100)
            assertEquals(0, player.stat(stats.attack))
            assertEquals(20, deps.remedy.restoresRemaining(player))
            deps.events.publish(SessionStateEvent.Login(player))
            advance(17)
            assertEquals(0, player.stat(stats.attack))
            advance()
            assertEquals(21, player.stat(stats.attack))

            deps.events.publish(PlayerDeathEvent.Completed(player))
            assertFalse(deps.remedy.isActive(player))
            assertEquals(0, player.vars[varbits.menaphite_remedy_timer])
            setLevel(stats.attack, base = 99, current = 0)
            advance(MenaphiteRemedyRepository.TOTAL_ACTIVE_CYCLES)
            assertEquals(0, player.stat(stats.attack))

            prepare(deps)
            setLevel(stats.attack, base = 99, current = 0)
            player.setVarBit(varbits.menaphite_remedy_timer, 5)
            deps.events.publish(SessionStateEvent.Login(player))
            advance(MenaphiteRemedyRepository.RESTORE_INTERVAL_CYCLES)
            assertEquals(21, player.stat(stats.attack))
            assertEquals(4, deps.remedy.restoresRemaining(player))
        }

    private fun GameTestScope.prepare(deps: Dependencies, members: Boolean = true) {
        player.clearAllInvs()
        player.members = members
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        deps.potions.clear(player)
        deps.remedy.clear(player)
        for (timer in divineTimers) {
            VarPlayerIntMapSetter.set(player, timer, 0)
        }
        client.clear()
    }

    private fun GameTestScope.setLevel(stat: StatType, base: Int, current: Int) {
        player.setBaseLevel(stat, base)
        player.setCurrentLevel(stat, current)
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
        val remedy: MenaphiteRemedyRepository,
        val divine: DivinePotionEffectRepository,
        val drinking: StandardPotionDrinking,
        val potions: PotionConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
    )

    private companion object {
        const val SLOT = 6

        val divineTimers: List<VarBitType> =
            listOf(
                varbits.divine_super_attack_timer,
                varbits.divine_super_strength_timer,
                varbits.divine_super_defence_timer,
                varbits.divine_ranging_timer,
                varbits.divine_magic_timer,
                varbits.divine_super_combat_timer,
                varbits.divine_bastion_timer,
                varbits.divine_battlemage_timer,
                varbits.saturated_heart_timer,
            )
    }
}
