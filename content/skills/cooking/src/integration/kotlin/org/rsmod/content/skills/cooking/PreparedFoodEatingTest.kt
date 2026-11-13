package org.rsmod.content.skills.cooking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class PreparedFoodEatingTest {
    @Test
    fun GameTestState.`every configured bite and hunter meat has native Eat`() = runFoodTest { d ->
        for (bite in PreparedFoods.all) assertEquals(
            "Eat",
            d.types[bite.input].iop[0],
            d.types[bite.input].name,
        )
        for (meat in HunterMeats.all) assertEquals("Eat", d.types[meat.input].iop[0])
        assertEquals("Eat", d.types[PreparedFoodObjs.angler].iop[0])
    }

    @Test
    fun GameTestState
        .`cake bites replace selected full inventory slot and use two two three delays`() =
        runFoodTest { d ->
            setup()
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
            player.inv[5] = InvObj(objs.cake)
            for ((index, bite) in
                PreparedFoods.all
                    .filter {
                        it.input in
                            listOf(
                                objs.cake,
                                PreparedFoodObjs.partialCake,
                                PreparedFoodObjs.cakeSlice,
                            )
                    }
                    .withIndex()) {
                player.withProtectedAccess { d.held.interact(this, player.inv, 5, HeldOp.Op1) }
                assertEquals(14 + 4 * index, player.stat(stats.hitpoints))
                assertEquals(bite.replacement?.id, player.inv[5]?.id)
                assertEquals(bite.delay, d.food.remainingCycles(player))
                advance(bite.delay)
            }
            assertEquals(objs.logs.id, player.inv[4]?.id)
        }

    @Test
    fun GameTestState.`all pies return half then dish with exact fast food cooldown and boosts`() =
        runFoodTest { d ->
            setup()
            for (bite in PreparedFoods.all.filter { it.replacement == objs.pie_dish }) {
                d.food.clear(player)
                player.inv[0] = InvObj(bite.input)
                player.setCurrentLevel(stats.hitpoints, 10)
                player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
                assertEquals(objs.pie_dish.id, player.inv[0]?.id)
                assertEquals(10 + bite.healing, player.stat(stats.hitpoints))
                assertEquals(bite.delay, d.food.remainingCycles(player))
                for ((stat, boost) in bite.boosts) assertEquals(50 + boost, player.stat(stat))
            }
        }

    @Test
    fun GameTestState.`summer pie energy caps and repeated boosts do not stack`() =
        runFoodTest { d ->
            setup()
            player.runEnergy = 9500
            player.inv[0] = InvObj(objs.summer_pie)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(10000, player.runEnergy)
            assertEquals(55, player.stat(stats.agility))
            advance(1)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(55, player.stat(stats.agility))
            assertEquals(objs.pie_dish.id, player.inv[0]?.id)
        }

    @Test
    fun GameTestState
        .`pizza halves combine atomically without healing and reject reusing one slot`() =
        runFoodTest { d ->
            setup()
            val half = PreparedFoodObjs.halfPineapplePizza
            player.inv[3] = InvObj(half)
            player.inv[8] = InvObj(half)
            player.withProtectedAccess {
                d.eating.combinePizza(this, 3, 3, half, objs.pineapple_pizza)
            }
            assertEquals(half.id, player.inv[3]?.id)
            player.withProtectedAccess {
                d.eating.combinePizza(this, 3, 8, half, objs.pineapple_pizza)
            }
            assertEquals(objs.pineapple_pizza.id, player.inv[3]?.id)
            assertNull(player.inv[8])
            assertEquals(10, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`member food rejected without consumption boosts or delayed effect`() =
        runFoodTest { d ->
            setup()
            player.members = false
            player.inv[0] = InvObj(objs.summer_pie)
            player.inv[1] = InvObj(objs.cooked_moonlight_antelope)
            player.withProtectedAccess {
                d.held.interact(this, player.inv, 0, HeldOp.Op1)
                d.held.interact(this, player.inv, 1, HeldOp.Op1)
            }
            advance(8)
            assertNotNull(player.inv[0])
            assertNotNull(player.inv[1])
            assertEquals(10, player.stat(stats.hitpoints))
            assertEquals(50, player.stat(stats.agility))
        }

    @Test
    fun GameTestState.`angler healing thresholds and overhead cap use base hitpoints`() =
        runFoodTest { d ->
            setup()
            for ((base, heal) in
                listOf(
                    10 to 3,
                    20 to 4,
                    25 to 6,
                    30 to 7,
                    40 to 8,
                    50 to 11,
                    60 to 12,
                    70 to 13,
                    75 to 15,
                    80 to 16,
                    90 to 17,
                    93 to 22,
                    99 to 22,
                )) {
                d.food.clear(player)
                player.setBaseLevel(stats.hitpoints, base)
                player.setCurrentLevel(stats.hitpoints, base)
                player.inv[0] = InvObj(PreparedFoodObjs.angler)
                player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
                assertEquals(base + heal, player.stat(stats.hitpoints))
            }
        }

    @Test
    fun GameTestState.`angler cannot overheal in active wilderness combat but can out of combat`() =
        runFoodTest { d ->
            setup()
            player.setVarBit(varbits.inside_wilderness, 1)
            player.setVarp(varps.lastcombat, player.currentMapClock)
            player.setCurrentLevel(stats.hitpoints, 49)
            player.inv[0] = InvObj(PreparedFoodObjs.angler)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(50, player.stat(stats.hitpoints))
            advance(3)
            player.setVarp(varps.lastcombat, -1000)
            player.setVarp(varps.lastcombat_pvp, -1000)
            player.inv[0] = InvObj(PreparedFoodObjs.angler)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(61, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`hunter healing waits seven ticks and ordinary food does not cancel it`() =
        runFoodTest { d ->
            setup()
            player.inv[0] = InvObj(objs.cooked_kyatt)
            player.inv[1] = InvObj(objs.shark)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(19, player.stat(stats.hitpoints))
            advance(3)
            player.withProtectedAccess { d.ordinary.eat(this, 1, objs.shark, 20) }
            assertEquals(39, player.stat(stats.hitpoints))
            advance(3)
            assertEquals(39, player.stat(stats.hitpoints))
            advance(1)
            assertEquals(47, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState
        .`new hunter meat replaces previous pending heal and dashing energy waits for secondary`() =
        runFoodTest { d ->
            setup()
            player.inv[0] = InvObj(objs.cooked_kyatt)
            player.inv[1] = InvObj(objs.cooked_dashing_kebbit)
            player.runEnergy = 0
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            advance(3)
            player.withProtectedAccess { d.held.interact(this, player.inv, 1, HeldOp.Op1) }
            assertEquals(32, player.stat(stats.hitpoints))
            assertEquals(60, player.runEnergy)
            advance(4)
            assertEquals(32, player.stat(stats.hitpoints))
            advance(3)
            assertEquals(42, player.stat(stats.hitpoints))
            assertEquals(1200, player.runEnergy)
        }

    @Test
    fun GameTestState
        .`moonlight cures poison only on secondary and downgrades venom without immunity`() =
        runFoodTest { d ->
            setup()
            player.setVarp(varps.poison, 20)
            player.inv[0] = InvObj(objs.cooked_moonlight_antelope)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(20, player.vars[varps.poison])
            advance(7)
            assertEquals(0, player.vars[varps.poison])
            d.food.clear(player)
            player.setVarp(varps.poison, 1000000)
            player.inv[0] = InvObj(objs.cooked_moonlight_antelope)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            advance(7)
            assertEquals(30, player.vars[varps.poison])
        }

    @Test
    fun GameTestState.`logout cleanup and death suppress a pending secondary heal`() =
        runFoodTest { d ->
            setup()
            player.inv[0] = InvObj(objs.cooked_dashing_kebbit)
            player.runEnergy = 0
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            d.eating.clear(player)
            advance(7)
            assertEquals(23, player.stat(stats.hitpoints))
            assertEquals(140, player.runEnergy)
            d.food.clear(player)
            player.inv[0] = InvObj(objs.cooked_dashing_kebbit)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            player.setCurrentLevel(stats.hitpoints, 0)
            d.eating.secondaryHeal(player)
            assertEquals(0, player.stat(stats.hitpoints))
            assertEquals(140, player.runEnergy)
        }

    private fun GameTestScope.setup() {
        player.members = true
        for (stat in
            listOf(
                stats.hitpoints,
                stats.agility,
                stats.farming,
                stats.fishing,
                stats.herblore,
                stats.crafting,
                stats.fletching,
                stats.ranged,
                stats.slayer,
            )) {
            player.setBaseLevel(stat, 50)
            player.setCurrentLevel(stat, 50)
        }
        player.setCurrentLevel(stats.hitpoints, 10)
    }

    private fun GameTestState.runFoodTest(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, Module, PreparedFoodScript::class, testBody = body)

    private object Module : AbstractModule() {
        override fun configure() {
            install(CookingModule())
        }
    }

    data class Deps
    @Inject
    constructor(
        val eating: PreparedFoodEating,
        val ordinary: OrdinaryFoodEating,
        val food: FoodConsumptionRepository,
        val held: HeldInteractions,
        val types: ObjTypeList,
    )
}
