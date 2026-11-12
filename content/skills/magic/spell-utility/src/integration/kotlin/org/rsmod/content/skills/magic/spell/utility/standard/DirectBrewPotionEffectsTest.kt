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
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.herblore.HerbloreModule
import org.rsmod.content.skills.herblore.HerbloreScript
import org.rsmod.content.skills.herblore.PotionConsumptionRepository
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

@Execution(ExecutionMode.SAME_THREAD)
class DirectBrewPotionEffectsTest {
    @Test
    fun GameTestState.`Saradomin brew uses base boosts and compounding current drains`() =
        runPotionTest { deps ->
            prepare(deps, objs.saradomin_brew_4)
            setLevel(stats.hitpoints, base = 99, current = 50)
            setLevel(stats.defence, base = 80, current = 20)
            setLevel(stats.attack, base = 99, current = 80)
            setLevel(stats.strength, base = 99, current = 50)
            setLevel(stats.ranged, base = 99, current = 1)
            setLevel(stats.magic, base = 99, current = 99)

            drink(deps)

            assertLevels(
                hitpoints = 66,
                defence = 38,
                attack = 70,
                strength = 43,
                ranged = 0,
                magic = 88,
            )
            advance(3)
            drink(deps)
            assertLevels(
                hitpoints = 82,
                defence = 56,
                attack = 61,
                strength = 37,
                ranged = 0,
                magic = 78,
            )
        }

    @Test
    fun GameTestState.`Zamorak brew uses exact boosts current drains and current Prayer restore`() =
        runPotionTest { deps ->
            prepare(deps, objs.zamorak_brew_4)
            setLevel(stats.attack, base = 99, current = 50)
            setLevel(stats.strength, base = 99, current = 50)
            setLevel(stats.defence, base = 99, current = 80)
            setLevel(stats.hitpoints, base = 99, current = 9)
            setLevel(stats.prayer, base = 99, current = 50)

            drink(deps)

            assertEquals(71, player.stat(stats.attack))
            assertEquals(63, player.stat(stats.strength))
            assertEquals(70, player.stat(stats.defence))
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(55, player.stat(stats.prayer))
            advance(3)
            drink(deps)
            assertEquals(92, player.stat(stats.attack))
            assertEquals(76, player.stat(stats.strength))
            assertEquals(61, player.stat(stats.defence))
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(60, player.stat(stats.prayer))

            setLevel(stats.hitpoints, base = 99, current = 1)
            advance(3)
            drink(deps)
            assertEquals(1, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`Ancient brew drains current stats and caps Prayer five percent over base`() =
        runPotionTest { deps ->
            prepare(deps, objs.ancient_brew_4)
            setLevel(stats.magic, base = 99, current = 50)
            setLevel(stats.prayer, base = 99, current = 95)
            setLevel(stats.attack, base = 99, current = 80)
            setLevel(stats.strength, base = 99, current = 1)
            setLevel(stats.defence, base = 99, current = 50)

            drink(deps)

            assertEquals(56, player.stat(stats.magic))
            assertEquals(103, player.stat(stats.prayer))
            assertEquals(70, player.stat(stats.attack))
            assertEquals(0, player.stat(stats.strength))
            assertEquals(43, player.stat(stats.defence))
            advance(3)
            drink(deps)
            assertEquals(62, player.stat(stats.magic))
            assertEquals(103, player.stat(stats.prayer))
            assertEquals(61, player.stat(stats.attack))
            assertEquals(0, player.stat(stats.strength))
            assertEquals(37, player.stat(stats.defence))
        }

    @Test
    fun GameTestState.`Forgotten brew upgrades Magic and retains Ancient Prayer and drains`() =
        runPotionTest { deps ->
            prepare(deps, objs.forgotten_brew_4)
            setLevel(stats.magic, base = 99, current = 50)
            setLevel(stats.prayer, base = 99, current = 95)
            setLevel(stats.attack, base = 99, current = 80)
            setLevel(stats.strength, base = 99, current = 1)
            setLevel(stats.defence, base = 99, current = 50)

            drink(deps)

            assertEquals(60, player.stat(stats.magic))
            assertEquals(103, player.stat(stats.prayer))
            assertEquals(70, player.stat(stats.attack))
            assertEquals(0, player.stat(stats.strength))
            assertEquals(43, player.stat(stats.defence))
            advance(3)
            drink(deps)
            assertEquals(70, player.stat(stats.magic))
            assertEquals(103, player.stat(stats.prayer))
            assertEquals(61, player.stat(stats.attack))
            assertEquals(0, player.stat(stats.strength))
            assertEquals(37, player.stat(stats.defence))
        }

    private fun GameTestScope.prepare(deps: Dependencies, potion: ObjType) {
        player.clearAllInvs()
        player.members = true
        player.actionDelay = player.currentMapClock
        player.setVarBit(varbits.auto_smash_vials, 0)
        player.inv[SLOT] = InvObj(potion)
        deps.potions.clear(player)
        client.clear()
    }

    private fun GameTestScope.setLevel(stat: StatType, base: Int, current: Int) {
        player.setBaseLevel(stat, base)
        player.setCurrentLevel(stat, current)
    }

    private fun GameTestScope.drink(deps: Dependencies) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, SLOT, HeldOp.Op1) }
    }

    private fun GameTestScope.assertLevels(
        hitpoints: Int,
        defence: Int,
        attack: Int,
        strength: Int,
        ranged: Int,
        magic: Int,
    ) {
        assertEquals(hitpoints, player.stat(stats.hitpoints))
        assertEquals(defence, player.stat(stats.defence))
        assertEquals(attack, player.stat(stats.attack))
        assertEquals(strength, player.stat(stats.strength))
        assertEquals(ranged, player.stat(stats.ranged))
        assertEquals(magic, player.stat(stats.magic))
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
        val potions: PotionConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
    )

    private companion object {
        const val SLOT = 6
    }
}
