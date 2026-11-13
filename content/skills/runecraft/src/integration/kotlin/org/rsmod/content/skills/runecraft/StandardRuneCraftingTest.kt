package org.rsmod.content.skills.runecraft

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class StandardRuneCraftingTest {
    @Test
    fun GameTestState
        .`cache 233 ordinary altar table has exact ids operations levels xp and thresholds`() =
        runRunecraftTest { _ ->
            assertEquals(
                listOf(
                    34760,
                    34761,
                    34762,
                    34763,
                    34764,
                    34765,
                    34766,
                    34769,
                    34771,
                    34768,
                    34767,
                    34770,
                    43479,
                    34772,
                ),
                StandardRuneRecipes.all.map { it.altar.id },
            )
            assertTrue(StandardRuneRecipes.all.all { locTypes[it.altar].op[0] == "Craft-rune" })
            assertEquals(
                listOf(1, 2, 5, 9, 14, 20, 27, 35, 40, 44, 54, 65, 77, 95),
                StandardRuneRecipes.all.map { it.level },
            )
            assertEquals(
                listOf(5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 8.7, 9.0, 9.5, 10.0, 10.5, 8.0),
                StandardRuneRecipes.all.map { it.xp },
            )
            assertEquals(
                listOf(1436, 7936, 24704),
                listOf(
                    RunecraftObjs.runeEssence.id,
                    RunecraftObjs.pureEssence.id,
                    RunecraftObjs.daeyaltEssence.id,
                ),
            )
            assertEquals(
                listOf(28593, 28595, 28597, 28599),
                listOf(
                    RunecraftObjs.warpedExtract.id,
                    RunecraftObjs.twistedExtract.id,
                    RunecraftObjs.mangledExtract.id,
                    RunecraftObjs.scarredExtract.id,
                ),
            )
            assertEquals(
                listOf(2448, 1103, 7255, 6104, 14862),
                listOf(
                    RunecraftVarbits.lunarDiplomacy.id,
                    RunecraftVarbits.mourningEndsPartTwo.id,
                    RunecraftVarbits.sinsOfTheFather.id,
                    RunecraftVarbits.dragonSlayerTwo.id,
                    RunecraftVarbits.desertTreasureTwo.id,
                ),
            )
            assertEquals(10, StandardRuneRecipes.air.outputMultiplier(99))
            assertEquals(8, StandardRuneRecipes.mind.outputMultiplier(99))
            assertEquals(6, StandardRuneRecipes.water.outputMultiplier(99))
            assertEquals(5, StandardRuneRecipes.earth.outputMultiplier(104))
            assertEquals(3, StandardRuneRecipes.fire.outputMultiplier(99))
            assertEquals(3, StandardRuneRecipes.body.outputMultiplier(99))
            assertEquals(2, StandardRuneRecipes.cosmic.outputMultiplier(99))
            assertEquals(2, StandardRuneRecipes.chaos.outputMultiplier(99))
            assertEquals(2, StandardRuneRecipes.astral.outputMultiplier(99))
            assertEquals(2, StandardRuneRecipes.nature.outputMultiplier(99))
            assertEquals(2, StandardRuneRecipes.law.outputMultiplier(99))
            assertEquals(2, StandardRuneRecipes.death.outputMultiplier(99))
            assertEquals(1, StandardRuneRecipes.blood.outputMultiplier(99))
            assertEquals(1, StandardRuneRecipes.wrath.outputMultiplier(99))
            assertFalse(objTypes[RunecraftObjs.runeEssence].stackable)
        }

    @Test
    fun GameTestState
        .`every ordinary altar converts all pure essence with pre-action visible multiplier and directional xp`() =
        runRunecraftTest { deps ->
            for (recipe in StandardRuneRecipes.all) {
                preparePlayer()
                completeQuest(recipe)
                player.inv[0] = InvObj(RunecraftObjs.pureEssence)
                player.inv[1] = InvObj(RunecraftObjs.pureEssence)
                val startXp = player.statMap.getFineXP(stats.runecrafting)

                player.withProtectedAccess { deps.standard.craft(this, recipe) }

                assertEquals(
                    2 * recipe.outputMultiplier(99),
                    player.count(recipe.output),
                    recipe.name,
                )
                assertEquals(0, player.count(RunecraftObjs.pureEssence), recipe.name)
                assertEquals(
                    (recipe.xp * 20).toInt(),
                    player.statMap.getFineXP(stats.runecrafting) - startXp,
                    recipe.name,
                )
                assertEquals(RunecraftSeqs.craft.id, player.pendingSequence.id, recipe.name)
            }
        }

    @Test
    fun GameTestState
        .`rune essence works at only the six free altars while daeyalt awards fifty percent extra xp`() =
        runRunecraftTest { deps ->
            for (recipe in StandardRuneRecipes.all.take(6)) {
                preparePlayer()
                player.inv[0] = InvObj(RunecraftObjs.runeEssence)
                player.withProtectedAccess { deps.standard.craft(this, recipe) }
                assertEquals(recipe.outputMultiplier(99), player.count(recipe.output), recipe.name)
            }

            preparePlayer()
            player.inv[0] = InvObj(RunecraftObjs.runeEssence)
            player.withProtectedAccess {
                deps.standard.craft(this, StandardRuneRecipes.cosmic)
                assertMessageSent("You do not have any essence to bind.")
            }
            assertEquals(1, player.count(RunecraftObjs.runeEssence))

            preparePlayer(level = 1)
            player.inv[0] = InvObj(RunecraftObjs.runeEssence)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            player.inv[2] = InvObj(RunecraftObjs.daeyaltEssence)
            val startXp = player.statMap.getFineXP(stats.runecrafting)
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.air) }
            assertEquals(3, player.count(StandardRuneRecipes.air.output))
            assertEquals(175, player.statMap.getFineXP(stats.runecrafting) - startXp)

            preparePlayer(level = 99)
            player.members = false
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.inv[1] = InvObj(RunecraftObjs.daeyaltEssence)
            player.inv[2] = InvObj(RunecraftObjs.warpedExtract)
            wearFullRaiments()
            player.withProtectedAccess { vars[RunecraftVarbits.desertTreasureTwo] = 1 }
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.air) }
            assertEquals(10, player.count(StandardRuneRecipes.air.output))
            assertEquals(1, player.count(RunecraftObjs.daeyaltEssence))
            assertEquals(1, player.count(RunecraftObjs.warpedExtract))
        }

    @Test
    fun GameTestState.`membership level and four quest completion gates fail atomically`() =
        runRunecraftTest { deps ->
            val membersRecipe = StandardRuneRecipes.cosmic
            preparePlayer()
            player.members = false
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.withProtectedAccess { deps.standard.craft(this, membersRecipe) }
            assertEquals(1, player.count(RunecraftObjs.pureEssence))

            preparePlayer(level = 94)
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.wrath) }
            assertEquals(1, player.count(RunecraftObjs.pureEssence))

            for (recipe in
                listOf(
                    StandardRuneRecipes.astral,
                    StandardRuneRecipes.death,
                    StandardRuneRecipes.blood,
                    StandardRuneRecipes.wrath,
                )) {
                preparePlayer()
                player.inv[0] = InvObj(RunecraftObjs.pureEssence)
                player.withProtectedAccess { deps.standard.craft(this, recipe) }
                assertEquals(1, player.count(RunecraftObjs.pureEssence), recipe.name)
                assertEquals(0, player.count(recipe.output), recipe.name)
            }
        }

    @Test
    fun GameTestState.`matching extracts are additive consume one per essence and never add xp`() =
        runRunecraftTest { deps ->
            preparePlayer(level = 60)
            player.withProtectedAccess { vars[RunecraftVarbits.desertTreasureTwo] = 1 }
            repeat(3) { player.inv[it] = InvObj(RunecraftObjs.pureEssence) }
            player.inv[3] = InvObj(RunecraftObjs.warpedExtract)
            val startXp = player.statMap.getFineXP(stats.runecrafting)
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.air) }
            assertEquals(268, player.count(StandardRuneRecipes.air.output))
            assertEquals(150, player.statMap.getFineXP(stats.runecrafting) - startXp)
            assertEquals(0, player.count(RunecraftObjs.warpedExtract))

            preparePlayer(level = 99)
            player.withProtectedAccess { vars[RunecraftVarbits.desertTreasureTwo] = 0 }
            repeat(2) { player.inv[it] = InvObj(RunecraftObjs.pureEssence) }
            player.inv[2] = InvObj(RunecraftObjs.warpedExtract, 2)
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.air) }
            assertEquals(20, player.count(StandardRuneRecipes.air.output))
            assertEquals(2, player.count(RunecraftObjs.warpedExtract))
        }

    @Test
    fun GameTestState
        .`Raiments use block and remainder rolls with full-set twenty-percent bonus`() =
        runRunecraftTest { deps ->
            preparePlayer(level = 42)
            wearFullRaiments()
            repeat(7) { player.inv[it] = InvObj(RunecraftObjs.pureEssence) }
            random.next = 7
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.mind) }
            assertEquals(46, player.count(StandardRuneRecipes.mind.output))
            assertEquals(0 until 10, random.lastInclusiveRange)

            preparePlayer(level = 42)
            wearFullRaiments()
            repeat(7) { player.inv[it] = InvObj(RunecraftObjs.pureEssence) }
            random.next = 8
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.mind) }
            assertEquals(40, player.count(StandardRuneRecipes.mind.output))
        }

    @Test
    fun GameTestState.`water diary batch requires 140 runes earned by essence and outfit`() =
        runRunecraftTest { deps ->
            preparePlayer(level = 76)
            for (slot in 0 until 27) player.inv[slot] = InvObj(RunecraftObjs.pureEssence)
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.water) }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CraftWaterRunes))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.Craft140WaterRunes))
            player.clearInv()
            for (slot in player.inv.indices) player.inv[slot] = InvObj(RunecraftObjs.pureEssence)
            player.withProtectedAccess { deps.standard.craft(this, StandardRuneRecipes.water) }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.Craft140WaterRunes))
        }

    @Test
    fun GameTestState.guardianUsesTheAltarColourAndRespectsItsColourLockOnTheNextCraft() =
        runRunecraftTest { deps ->
            preparePlayer(level = 50)
            val altar = placeMapLoc(player.coords.translate(1,0),locTypes[StandardRuneRecipes.air.altar])
            val random = deps.petRandom as org.rsmod.api.testing.random.FixedRandom
            random.set(1_795_758 - 25 * 50 - 1)
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.opLoc1(altar)
            advance(6)
            val following = cacheTypes.varps.getValue(65725)
            assertEquals(20667,player.vars[following])
            player.setVarBit(cacheTypes.varbits.getValue(6093),1)
            random.set(0)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            player.withProtectedAccess { deps.standard.craft(this,StandardRuneRecipes.mind) }
            assertEquals(20667,player.vars[following])
            player.setVarBit(cacheTypes.varbits.getValue(6093),0)
            player.inv[1] = InvObj(RunecraftObjs.pureEssence)
            player.withProtectedAccess { deps.standard.craft(this,StandardRuneRecipes.mind) }
            assertEquals(20669,player.vars[following])
        }

    private fun GameTestScope.preparePlayer(level: Int = 99) {
        player.members = true
        player.stats[stats.runecrafting] = level
        player.clearInv()
        player.hat = null
        player.torso = null
        player.legs = null
        player.feet = null
        client.clear()
    }

    private fun GameTestScope.completeQuest(recipe: StandardRuneRecipe) {
        val quest = recipe.quest ?: return
        player.withProtectedAccess { vars[quest.varbit] = quest.completionValue }
    }

    @Test
    fun GameTestState
        .`cores supply eleven essence outputs each and cannot satisfy the Falador batch`() =
        runRunecraftTest { deps ->
            for ((core, recipe) in
                listOf(
                    RunecraftObjs.mindCore to StandardRuneRecipes.mind,
                    RunecraftObjs.bodyCore to StandardRuneRecipes.body,
                    RunecraftObjs.chaosCore to StandardRuneRecipes.chaos,
                )) {
                preparePlayer()
                player.inv[0] = InvObj(core)
                player.inv[1] = InvObj(core)
                player.inv[2] = InvObj(RunecraftObjs.pureEssence)
                val before = player.statMap.getFineXP(stats.runecrafting)
                player.withProtectedAccess { deps.standard.craft(this, recipe) }
                assertEquals(23 * recipe.outputMultiplier(99), player.count(recipe.output))
                assertEquals(
                    (21 * recipe.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.runecrafting) - before,
                )
                assertEquals(0, player.count(core))
                assertFalse(player.hasCompletedDiaryTask(DiaryTask.Craft140MindRunes))
            }
        }

    private fun GameTestScope.wearFullRaiments() {
        player.hat = InvObj(RunecraftObjs.raimentsHat)
        player.torso = InvObj(RunecraftObjs.raimentsTorso)
        player.legs = InvObj(RunecraftObjs.raimentsLegsBase)
        player.feet = InvObj(RunecraftObjs.raimentsBoots)
    }

    private fun GameTestState.runRunecraftTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            RunecraftScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(RunecraftModule())
        }
    }

    data class Dependencies @Inject constructor(val standard: StandardRuneCrafting,@org.rsmod.api.random.CoreRandom val petRandom:org.rsmod.api.random.GameRandom)
}
