package org.rsmod.content.skills.magic.spell.utility.standard

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.cooking.FoodConsumptionRepository
import org.rsmod.content.skills.herblore.HerbloreModule
import org.rsmod.content.skills.herblore.HerbloreScript
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

@Execution(ExecutionMode.SAME_THREAD)
class BarbarianMixEffectsTest {
    @Test
    fun GameTestState.`every stat-changing mix combines its ordinary effect with exact healing`() =
        runMixTest { deps ->
            assertStatMix(deps, objs.attack_mix_2, stats.attack, 63, hitpoints = 53)
            assertStatMix(deps, objs.strength_mix_2, stats.strength, 63, hitpoints = 53)
            assertStatMix(deps, objs.restore_mix_2, stats.attack, 90, hitpoints = 53)
            assertStatMix(deps, objs.defence_mix_2, stats.defence, 63, hitpoints = 56)
            assertStatMix(deps, objs.agility_mix_2, stats.agility, 53, hitpoints = 56)
            assertStatMix(deps, objs.combat_mix_2, stats.attack, 63, hitpoints = 53)
            assertEquals(63, player.stat(stats.strength))
            assertStatMix(deps, objs.prayer_mix_2, stats.prayer, 82, hitpoints = 56)
            assertStatMix(deps, objs.superattack_mix_2, stats.attack, 70, hitpoints = 56)
            assertStatMix(deps, objs.fishing_mix_2, stats.fishing, 53, hitpoints = 56)
            assertStatMix(deps, objs.hunting_mix_2, stats.hunter, 53, hitpoints = 56)
            assertStatMix(deps, objs.super_strength_mix_2, stats.strength, 70, hitpoints = 56)
            assertStatMix(deps, objs.magic_essence_mix_2, stats.magic, 53, hitpoints = 56)
            assertStatMix(deps, objs.super_restore_mix_2, stats.attack, 83, hitpoints = 56)
            assertEquals(83, player.stat(stats.prayer))
            assertStatMix(deps, objs.super_defence_mix_2, stats.defence, 70, hitpoints = 56)
            assertStatMix(deps, objs.ranging_mix_2, stats.ranged, 64, hitpoints = 56)
            assertStatMix(deps, objs.magic_mix_2, stats.magic, 54, hitpoints = 56)

            prepare(deps, objs.ancient_mix_2)
            drink(deps)
            assertEquals(43, player.stat(stats.attack))
            assertEquals(43, player.stat(stats.strength))
            assertEquals(43, player.stat(stats.defence))
            assertEquals(57, player.stat(stats.magic))
            assertEquals(62, player.stat(stats.prayer))
            assertEquals(56, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`recovery protection and stamina mixes use their ordinary exact effects`() =
        runMixTest { deps ->
            prepare(deps, objs.energy_mix_2)
            player.runEnergy = 1_234
            drink(deps)
            assertEquals(2_234, player.runEnergy)
            assertEquals(53, player.stat(stats.hitpoints))

            prepare(deps, objs.super_energy_mix_2)
            player.runEnergy = 1_234
            drink(deps)
            assertEquals(3_234, player.runEnergy)

            prepare(deps, objs.stamina_mix_2)
            player.runEnergy = 1_234
            drink(deps)
            assertEquals(3_234, player.runEnergy)
            assertEquals(200, deps.stamina.remainingCycles(player))

            assertPoisonMix(deps, objs.antipoison_mix_2, expected = -5, hitpoints = 53)
            assertPoisonMix(deps, objs.antipoison_supermix_2, expected = -20, hitpoints = 56)
            assertPoisonMix(deps, objs.antidote_plus_mix_2, expected = -30, hitpoints = 56)

            prepare(deps, objs.relicyms_mix_2)
            VarPlayerIntMapSetter.set(player, varps.disease, 10)
            drink(deps)
            assertEquals(0, player.vars[varps.disease])

            assertAntifireMix(deps, objs.antifire_mix_2, cycles = 600, superior = false)
            assertAntifireMix(deps, objs.extended_antifire_mix_2, cycles = 1_200, superior = false)
            assertAntifireMix(deps, objs.super_antifire_mix_2, cycles = 300, superior = true)
            assertAntifireMix(
                deps,
                objs.extended_super_antifire_mix_2,
                cycles = 600,
                superior = true,
            )
        }

    @Test
    fun GameTestState.`Zamorak mix damages then heals with the dated Wiki formula`() =
        runMixTest { deps ->
            val cases =
                mapOf(
                    91 to 86,
                    81 to 77,
                    78 to 75,
                    75 to 72,
                    31 to 34,
                    34 to 36,
                    36 to 38,
                    38 to 40,
                    50 to 50,
                )
            for ((current, expected) in cases) {
                prepare(deps, objs.zamorak_mix_2, hitpointsBase = 91, hitpointsCurrent = current)
                drink(deps)
                assertEquals(expected, player.stat(stats.hitpoints), "starting Hitpoints=$current")
            }

            prepare(deps, objs.zamorak_mix_2)
            drink(deps)
            assertEquals(72, player.stat(stats.attack))
            assertEquals(64, player.stat(stats.strength))
            assertEquals(43, player.stat(stats.defence))
            assertEquals(55, player.stat(stats.prayer))
        }

    private fun GameTestScope.assertStatMix(
        deps: Dependencies,
        mix: ObjType,
        stat: StatType,
        expected: Int,
        hitpoints: Int,
    ) {
        prepare(deps, mix)
        drink(deps)
        assertEquals(expected, player.stat(stat), "mix=${mix.id}")
        assertEquals(hitpoints, player.stat(stats.hitpoints), "mix=${mix.id}")
    }

    private fun GameTestScope.assertPoisonMix(
        deps: Dependencies,
        mix: ObjType,
        expected: Int,
        hitpoints: Int,
    ) {
        prepare(deps, mix)
        VarPlayerIntMapSetter.set(player, varps.poison, 0)
        drink(deps)
        assertEquals(expected, player.vars[varps.poison], "mix=${mix.id}")
        assertEquals(hitpoints, player.stat(stats.hitpoints))
    }

    private fun GameTestScope.assertAntifireMix(
        deps: Dependencies,
        mix: ObjType,
        cycles: Int,
        superior: Boolean,
    ) {
        prepare(deps, mix)
        drink(deps)
        assertEquals(cycles, deps.antifire.remainingCycles(player, superior), "mix=${mix.id}")
        assertEquals(56, player.stat(stats.hitpoints))
    }

    private fun GameTestScope.prepare(
        deps: Dependencies,
        mix: ObjType,
        hitpointsBase: Int = 100,
        hitpointsCurrent: Int = 50,
    ) {
        player.clearAllInvs()
        player.members = true
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        player.inv[SLOT] = InvObj(mix)
        for (stat in testStats) {
            player.setBaseLevel(stat, 100)
            player.setCurrentLevel(stat, 50)
        }
        player.setBaseLevel(stats.hitpoints, hitpointsBase)
        player.setCurrentLevel(stats.hitpoints, hitpointsCurrent)
        player.runEnergy = 5_000
        VarPlayerIntMapSetter.set(player, varps.poison, 0)
        VarPlayerIntMapSetter.set(player, varps.disease, 0)
        deps.food.clear(player)
        deps.antifire.clear(player)
        deps.stamina.clear(player)
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, SLOT, HeldOp.Op1) }
    }

    private fun GameTestState.runMixTest(body: GameTestScope.(Dependencies) -> Unit) =
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
        val food: FoodConsumptionRepository,
        val held: HeldInteractions,
        val antifire: AntifireProtectionRepository,
        val stamina: StaminaEffectRepository,
    )

    private companion object {
        const val SLOT = 6
        val testStats =
            listOf(
                stats.attack,
                stats.strength,
                stats.defence,
                stats.ranged,
                stats.magic,
                stats.prayer,
                stats.agility,
                stats.fishing,
                stats.hunter,
            )
    }
}
