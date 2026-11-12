package org.rsmod.content.skills.fletching.ammunition

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class PoisonedAmmunitionTest {
    @Test
    fun GameTestState.`cache 233 pins all 123 poisoned ammunition recipes`() =
        runPoisoningTest { deps ->
            assertEquals(123, deps.recipes.recipes.size)
            assertEquals(
                mapOf(
                    PoisonedAmmunitionFamily.Arrow to 24,
                    PoisonedAmmunitionFamily.Bolt to 24,
                    PoisonedAmmunitionFamily.Dart to 27,
                    PoisonedAmmunitionFamily.Javelin to 24,
                    PoisonedAmmunitionFamily.ThrowingKnife to 24,
                ),
                deps.recipes.recipes.groupingBy { it.family }.eachCount(),
            )
            assertEquals(
                listOf(
                    "Bronze arrow:Arrow:882=883,5616,5622",
                    "Iron arrow:Arrow:884=885,5617,5623",
                    "Steel arrow:Arrow:886=887,5618,5624",
                    "Mithril arrow:Arrow:888=889,5619,5625",
                    "Adamant arrow:Arrow:890=891,5620,5626",
                    "Rune arrow:Arrow:892=893,5621,5627",
                    "Amethyst arrow:Arrow:21326=21332,21334,21336",
                    "Dragon arrow:Arrow:11212=11227,11228,11229",
                    "Bronze bolts:Bolt:877=878,6061,6062",
                    "Blurite bolts:Bolt:9139=9286,9293,9300",
                    "Iron bolts:Bolt:9140=9287,9294,9301",
                    "Silver bolts:Bolt:9145=9292,9299,9306",
                    "Steel bolts:Bolt:9141=9288,9295,9302",
                    "Mithril bolts:Bolt:9142=9289,9296,9303",
                    "Adamant bolts:Bolt:9143=9290,9297,9304",
                    "Runite bolts:Bolt:9144=9291,9298,9305",
                    "Bronze dart:Dart:806=812,5628,5635",
                    "Iron dart:Dart:807=813,5629,5636",
                    "Steel dart:Dart:808=814,5630,5637",
                    "Black dart:Dart:3093=3094,5631,5638",
                    "Mithril dart:Dart:809=815,5632,5639",
                    "Adamant dart:Dart:810=816,5633,5640",
                    "Rune dart:Dart:811=817,5634,5641",
                    "Amethyst dart:Dart:25849=25851,25855,25857",
                    "Dragon dart:Dart:11230=11231,11233,11234",
                    "Bronze javelin:Javelin:825=831,5642,5648",
                    "Iron javelin:Javelin:826=832,5643,5649",
                    "Steel javelin:Javelin:827=833,5644,5650",
                    "Mithril javelin:Javelin:828=834,5645,5651",
                    "Adamant javelin:Javelin:829=835,5646,5652",
                    "Rune javelin:Javelin:830=836,5647,5653",
                    "Amethyst javelin:Javelin:21318=21320,21322,21324",
                    "Dragon javelin:Javelin:19484=19486,19488,19490",
                    "Bronze knife:ThrowingKnife:864=870,5654,5661",
                    "Iron knife:ThrowingKnife:863=871,5655,5662",
                    "Steel knife:ThrowingKnife:865=872,5656,5663",
                    "Black knife:ThrowingKnife:869=874,5658,5665",
                    "Mithril knife:ThrowingKnife:866=873,5657,5664",
                    "Adamant knife:ThrowingKnife:867=875,5659,5666",
                    "Rune knife:ThrowingKnife:868=876,5660,5667",
                    "Dragon knife:ThrowingKnife:22804=22806,22808,22810",
                ),
                deps.recipes.recipes.chunked(3).map { group ->
                    val first = group.first()
                    assertEquals(WeaponPoisonStrength.entries, group.map { it.strength })
                    assertEquals(listOf(187, 5937, 5940), group.map { it.poison.id })
                    "${first.name}:${first.family}:${first.ammunition.id}=" +
                        group.joinToString(",") { it.output.id.toString() }
                },
            )
            assertEquals(229, objs.empty_vial.id)
            assertTrue(deps.recipes.recipes.all { objTypes[it.output].isStackable })
        }

    @Test
    fun GameTestState
        .`every eligible ammunition and poison strength transforms five immediately`() =
        runPoisoningTest { deps ->
            for (recipe in deps.recipes.recipes) {
                preparePlayer()
                add(recipe.ammunition, 8)
                add(recipe.poison)
                val startXp = player.statMap.getFineXP(stats.fletching)

                player.withProtectedAccess { deps.poisoning.poison(this, recipe) }

                assertEquals(3, player.count(recipe.ammunition), recipe.toString())
                assertEquals(5, player.count(recipe.output), recipe.toString())
                assertEquals(0, player.count(recipe.poison), recipe.toString())
                assertEquals(1, player.count(objs.empty_vial), recipe.toString())
                assertEquals(startXp, player.statMap.getFineXP(stats.fletching), recipe.toString())
            }
        }

    @Test
    fun GameTestState.`partial stacks and reverse full inventory use are atomic`() =
        runPoisoningTest { deps ->
            val recipe =
                deps.recipes.recipes.single {
                    it.ammunition == objs.amethyst_javelin &&
                        it.strength == WeaponPoisonStrength.PlusPlus
                }
            preparePlayer()
            add(recipe.ammunition, 3)
            add(recipe.poison)
            player.withProtectedAccess { deps.poisoning.poison(this, recipe) }
            assertEquals(0, player.count(recipe.ammunition))
            assertEquals(3, player.count(recipe.output))
            assertEquals(1, player.count(objs.empty_vial))

            preparePlayer()
            add(recipe.ammunition, 5)
            add(recipe.poison)
            repeat(26) { add(objs.logs) }
            val ammunitionSlot = player.inv.indexOfFirst { it?.id == recipe.ammunition.id }
            val poisonSlot = player.inv.indexOfFirst { it?.id == recipe.poison.id }
            player.withProtectedAccess {
                deps.heldU.interact(
                    this,
                    player.inv,
                    objTypes[recipe.poison],
                    poisonSlot,
                    objTypes[recipe.ammunition],
                    ammunitionSlot,
                )
            }
            assertEquals(0, player.count(recipe.ammunition))
            assertEquals(5, player.count(recipe.output))
            assertEquals(1, player.count(objs.empty_vial))
            assertEquals(28, player.inv.count { it != null })
        }

    @Test
    fun GameTestState.`membership capacity and repeated interaction failures preserve inputs`() =
        runPoisoningTest { deps ->
            val recipe =
                deps.recipes.recipes.single {
                    it.ammunition == objs.dragon_dart &&
                        it.strength == WeaponPoisonStrength.Standard
                }
            preparePlayer()
            player.members = false
            add(recipe.ammunition, 5)
            add(recipe.poison)
            player.withProtectedAccess { deps.poisoning.poison(this, recipe) }
            assertMessageSent("You need to be on a members' world to poison ammunition.")
            assertEquals(5, player.count(recipe.ammunition))
            assertEquals(1, player.count(recipe.poison))
            assertEquals(0, player.count(recipe.output))

            preparePlayer()
            add(recipe.ammunition, 8)
            add(recipe.poison)
            repeat(26) { add(objs.logs) }
            player.withProtectedAccess { deps.poisoning.poison(this, recipe) }
            assertMessageSent("You don't have enough inventory space.")
            assertEquals(8, player.count(recipe.ammunition))
            assertEquals(1, player.count(recipe.poison))
            assertEquals(0, player.count(recipe.output))
            assertEquals(0, player.count(objs.empty_vial))

            preparePlayer()
            add(recipe.ammunition, 12)
            add(recipe.poison, 2)
            repeat(2) { player.withProtectedAccess { deps.poisoning.poison(this, recipe) } }
            assertEquals(2, player.count(recipe.ammunition))
            assertEquals(10, player.count(recipe.output))
            assertEquals(0, player.count(recipe.poison))
            assertEquals(2, player.count(objs.empty_vial))
            player.withProtectedAccess { deps.poisoning.poison(this, recipe) }
            assertEquals(2, player.count(recipe.ammunition))
            assertEquals(10, player.count(recipe.output))
        }

    @Test
    fun GameTestState.`poisoning cancels the shared Fletching queue without scheduling work`() =
        runPoisoningTest { deps ->
            val recipe = deps.recipes.recipes.first()
            preparePlayer()
            add(recipe.ammunition, 5)
            add(recipe.poison)
            player.withProtectedAccess {
                weakQueue(
                    fletching_queues.ammunition,
                    10,
                    AmmunitionFletchingRequest(objs.bronze_arrow, remaining = 1),
                )
                deps.poisoning.poison(this, recipe)
            }
            assertEquals(0, player.weakQueueList.count(fletching_queues.ammunition))
            assertEquals(5, player.count(recipe.output))
        }

    private fun GameTestScope.preparePlayer() {
        player.clearAllInvs()
        client.clear()
        player.members = true
        player.stats[stats.fletching] = 99
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].isStackable) {
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

    private fun GameTestState.runPoisoningTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            AmmunitionFletchingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(AmmunitionFletchingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val poisoning: PoisonedAmmunition,
        val recipes: PoisonedAmmunitionRecipeRegistry,
        val heldU: HeldUInteractions,
    )
}
