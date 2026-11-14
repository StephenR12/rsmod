package org.rsmod.content.skills.cooking

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
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqTypeList

@Execution(ExecutionMode.SAME_THREAD)
class TeaDrinkingTest {
    @Test
    fun GameTestState.`cache 238 ordinary tea contract and Drink dispatch are pinned`() =
        runTeaTest { deps ->
            assertEquals(1978, objs.cup_of_tea.id)
            assertEquals(1980, objs.empty_cup.id)
            assertEquals(3702, seqs.human_drink_from_teacup_clay.id)
            assertEquals("Cup of tea", deps.objs[objs.cup_of_tea].name)
            assertEquals(
                listOf("Drink", null, null, null, null),
                deps.objs[objs.cup_of_tea].iop.toList(),
            )
            assertTrue(deps.objs[objs.cup_of_tea].members)
            assertTrue(deps.events.contains(HeldObjEvents.Op1::class.java, objs.cup_of_tea.id))
        }

    @Test
    fun GameTestState.`ordinary tea heals boosts speaks animates and replaces its exact slot`() =
        runTeaTest { deps ->
            for ((baseAttack, expected) in listOf(1 to 3, 49 to 51, 50 to 53, 99 to 102)) {
                preparePlayer(deps, baseAttack = baseAttack, currentHitpoints = 40)
                repeat(player.inv.size) { player.inv[it] = InvObj(objs.coins) }
                player.inv[TEA_SLOT] = InvObj(objs.cup_of_tea)

                drink(deps)

                assertEquals(objs.empty_cup.id, player.inv[TEA_SLOT]?.id)
                assertEquals(43, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
                assertEquals(expected, player.statMap.getCurrentLevel(stats.attack).toInt())
                assertEquals(seqs.human_drink_from_teacup_clay.id, player.pendingSequence.id)
                assertEquals(TeaDrinking.DRINK_SAY, player.pendingSay)
                assertEquals(player.inv.size, player.inv.count { it != null })
            }
        }

    @Test
    fun GameTestState.`ordinary tea respects visible caps and remains consumable at full health`() =
        runTeaTest { deps ->
            preparePlayer(deps, baseAttack = 50, currentAttack = 53, currentHitpoints = 50)
            player.inv[TEA_SLOT] = InvObj(objs.cup_of_tea)

            drink(deps)

            assertEquals(50, player.statMap.getCurrentLevel(stats.hitpoints).toInt())
            assertEquals(53, player.statMap.getCurrentLevel(stats.attack).toInt())
            assertEquals(objs.empty_cup.id, player.inv[TEA_SLOT]?.id)

            advance(TeaDrinking.FOOD_DELAY_CYCLES)
            player.inv[TEA_SLOT] = InvObj(objs.cup_of_tea)
            player.setCurrentLevel(stats.attack, 40)
            drink(deps)
            assertEquals(43, player.statMap.getCurrentLevel(stats.attack).toInt())
        }

    @Test
    fun GameTestState.`ordinary tea uses standard food and active attack delays`() =
        runTeaTest { deps ->
            preparePlayer(deps)
            player.inv[TEA_SLOT] = InvObj(objs.cup_of_tea)
            player.inv[SECOND_TEA_SLOT] = InvObj(objs.cup_of_tea)
            player.actionDelay = player.currentMapClock + 4

            drink(deps)

            assertEquals(player.currentMapClock + 7, player.actionDelay)
            assertEquals(3, deps.food.remainingCycles(player))
            drink(deps, SECOND_TEA_SLOT)
            assertEquals(objs.cup_of_tea.id, player.inv[SECOND_TEA_SLOT]?.id)
            advance(2)
            drink(deps, SECOND_TEA_SLOT)
            assertEquals(objs.cup_of_tea.id, player.inv[SECOND_TEA_SLOT]?.id)
            advance()
            drink(deps, SECOND_TEA_SLOT)
            assertEquals(objs.empty_cup.id, player.inv[SECOND_TEA_SLOT]?.id)

            advance(TeaDrinking.FOOD_DELAY_CYCLES)
            player.inv[TEA_SLOT] = InvObj(objs.cup_of_tea)
            player.actionDelay = player.currentMapClock
            drink(deps)
            assertEquals(player.currentMapClock, player.actionDelay)
        }

    @Test
    fun GameTestState.`ordinary tea rejects free worlds and stale slots atomically`() =
        runTeaTest { deps ->
            preparePlayer(deps, members = false)
            player.inv[TEA_SLOT] = InvObj(objs.cup_of_tea)
            drink(deps)
            assertEquals(objs.cup_of_tea.id, player.inv[TEA_SLOT]?.id)
            assertMessageSent(TeaDrinking.MEMBERS_MESSAGE)
            assertEquals(0, deps.food.remainingCycles(player))

            preparePlayer(deps)
            player.inv[TEA_SLOT] = InvObj(objs.logs)
            player.withProtectedAccess { deps.tea.drink(this, TEA_SLOT) }
            assertEquals(objs.logs.id, player.inv[TEA_SLOT]?.id)
            assertFalse(player.pendingSay == TeaDrinking.DRINK_SAY)
            assertEquals(0, deps.food.remainingCycles(player))
        }

    private fun GameTestScope.preparePlayer(
        deps: Dependencies,
        baseAttack: Int = 50,
        currentAttack: Int = baseAttack,
        currentHitpoints: Int = 47,
        members: Boolean = true,
    ) {
        player.clearAllInvs()
        player.members = members
        player.setBaseLevel(stats.attack, baseAttack)
        player.setCurrentLevel(stats.attack, currentAttack)
        player.setBaseLevel(stats.hitpoints, 50)
        player.setCurrentLevel(stats.hitpoints, currentHitpoints)
        player.pendingSay = null
        player.actionDelay = player.currentMapClock
        deps.food.clear(player)
        client.clear()
    }

    private fun GameTestScope.drink(deps: Dependencies, slot: Int = TEA_SLOT) {
        player.withProtectedAccess { deps.held.interact(this, player.inv, slot, HeldOp.Op1) }
    }

    private fun GameTestState.runTeaTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            TeaDrinkingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(CookingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val tea: TeaDrinking,
        val food: FoodConsumptionRepository,
        val held: HeldInteractions,
        val events: EventBus,
        val objs: ObjTypeList,
        val seqs: SeqTypeList,
    )

    private companion object {
        const val TEA_SLOT = 7
        const val SECOND_TEA_SLOT = 11
    }
}
