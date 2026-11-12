package org.rsmod.content.skills.runecraft

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.feet
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.isType

@Execution(ExecutionMode.SAME_THREAD)
class KourendRuneCraftingTest {
    @Test
    fun GameTestState.`cache 233 Kourend altars essence and recipe contracts are exact`() =
        runRunecraftTest { _ ->
            assertEquals(
                listOf(27978, 27979, 27980),
                listOf(
                    RunecraftLocs.kourendBloodAltar.id,
                    RunecraftLocs.darkAltar.id,
                    RunecraftLocs.kourendSoulAltar.id,
                ),
            )
            assertEquals("Bind", locTypes[RunecraftLocs.kourendBloodAltar].op[0])
            assertEquals("Venerate", locTypes[RunecraftLocs.darkAltar].op[0])
            assertEquals("Bind", locTypes[RunecraftLocs.kourendSoulAltar].op[0])
            assertEquals(
                listOf(13445, 13446, 7938),
                listOf(
                    RunecraftObjs.denseEssenceBlock.id,
                    RunecraftObjs.darkEssenceBlock.id,
                    RunecraftObjs.darkEssenceFragments.id,
                ),
            )
            assertTrue(objTypes[RunecraftObjs.denseEssenceBlock].members)
            assertTrue(objTypes[RunecraftObjs.darkEssenceBlock].members)
            assertTrue(objTypes[RunecraftObjs.darkEssenceFragments].members)
            assertFalse(objTypes[RunecraftObjs.darkEssenceFragments].stackable)
            assertEquals("Count", objTypes[RunecraftObjs.darkEssenceFragments].iop[0])
            assertEquals(7202, RunecraftSeqs.chiselDarkEssence.id)
            assertEquals(110, RunecraftQueues.darkAltarVenerate.id)
            assertEquals(listOf(77, 90), KourendRuneRecipes.all.map { it.level })
            assertEquals(listOf(23.8, 29.7), KourendRuneRecipes.all.map { it.xp })
            assertEquals(listOf(565, 566), KourendRuneRecipes.all.map { it.output.id })
        }

    @Test
    fun GameTestState
        .`Dark Altar converts every block together on tick three and drains bounded prayer`() =
        runRunecraftTest { deps ->
            preparePlayer()
            player.setCurrentLevel(stats.prayer, 2)
            player.inv[0] = InvObj(RunecraftObjs.denseEssenceBlock)
            player.inv[3] = InvObj(RunecraftObjs.denseEssenceBlock)
            player.inv[7] = InvObj(RunecraftObjs.denseEssenceBlock)
            val startXp = player.statMap.getFineXP(stats.runecrafting)

            player.withProtectedAccess { deps.kourend.startVenerate(this) }

            assertEquals(3, player.count(RunecraftObjs.denseEssenceBlock))
            assertEquals(RunecraftSeqs.craft.id, player.pendingSequence.id)
            advance(2)
            assertEquals(3, player.count(RunecraftObjs.denseEssenceBlock))
            advance()
            assertEquals(0, player.count(RunecraftObjs.denseEssenceBlock))
            assertEquals(3, player.count(RunecraftObjs.darkEssenceBlock))
            assertEquals(RunecraftObjs.darkEssenceBlock.id, player.inv[0]?.id)
            assertEquals(RunecraftObjs.darkEssenceBlock.id, player.inv[3]?.id)
            assertEquals(RunecraftObjs.darkEssenceBlock.id, player.inv[7]?.id)
            assertEquals(0, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(75, player.statMap.getFineXP(stats.runecrafting) - startXp)
        }

    @Test
    fun GameTestState
        .`Dark Altar works at zero prayer while membership and material failures are atomic`() =
        runRunecraftTest { deps ->
            preparePlayer()
            player.setCurrentLevel(stats.prayer, 0)
            player.inv[0] = InvObj(RunecraftObjs.denseEssenceBlock)
            player.withProtectedAccess { deps.kourend.startVenerate(this) }
            advance(KourendRuneCrafting.VENERATE_TICKS)
            assertEquals(1, player.count(RunecraftObjs.darkEssenceBlock))
            assertEquals(0, player.statMap.getCurrentLevel(stats.prayer).toInt())

            preparePlayer(members = false)
            player.inv[0] = InvObj(RunecraftObjs.denseEssenceBlock)
            player.withProtectedAccess { deps.kourend.startVenerate(this) }
            assertMessageSent(
                "You need to be on a members' world to venerate dense essence blocks."
            )
            advance(KourendRuneCrafting.VENERATE_TICKS)
            assertEquals(1, player.count(RunecraftObjs.denseEssenceBlock))

            preparePlayer()
            player.withProtectedAccess { deps.kourend.startVenerate(this) }
            assertEquals(0, player.weakQueueList.count(RunecraftQueues.darkAltarVenerate))
            assertMessageSent("You do not have any dense essence blocks to venerate.")
        }

    @Test
    fun GameTestState
        .`chisel makes four fragments in the selected full-inventory slot and awards exact xp`() =
        runRunecraftTest { deps ->
            preparePlayer(crafting = 38)
            player.fillInv(InvObj(objs.coins))
            player.inv[4] = InvObj(objs.chisel)
            player.inv[19] = InvObj(RunecraftObjs.darkEssenceBlock)
            val startXp = player.statMap.getFineXP(stats.crafting)

            player.withProtectedAccess { deps.kourend.chisel(this, 19) }

            assertEquals(0, player.count(RunecraftObjs.darkEssenceBlock))
            assertEquals(4, player.count(RunecraftObjs.darkEssenceFragments))
            assertEquals(RunecraftObjs.darkEssenceFragments.id, player.inv[19]?.id)
            assertEquals(4, player.inv[19]?.count)
            assertEquals(80, player.statMap.getFineXP(stats.crafting) - startXp)
            assertEquals(RunecraftSeqs.chiselDarkEssence.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState.`chisel membership level and tool failures preserve the block`() =
        runRunecraftTest { deps ->
            preparePlayer(members = false, crafting = 99)
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(RunecraftObjs.darkEssenceBlock)
            player.withProtectedAccess { deps.kourend.chisel(this, 1) }
            assertEquals(1, player.count(RunecraftObjs.darkEssenceBlock))

            preparePlayer(crafting = 37)
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(RunecraftObjs.darkEssenceBlock)
            player.withProtectedAccess { deps.kourend.chisel(this, 1) }
            assertEquals(1, player.count(RunecraftObjs.darkEssenceBlock))

            preparePlayer(crafting = 99)
            player.inv[1] = InvObj(RunecraftObjs.darkEssenceBlock)
            player.withProtectedAccess { deps.kourend.chisel(this, 1) }
            assertEquals(1, player.count(RunecraftObjs.darkEssenceBlock))
            assertEquals(0, player.count(RunecraftObjs.darkEssenceFragments))
        }

    @Test
    fun GameTestState
        .`Kourend blood combines diary raiments extracts and active essence additively`() =
        runRunecraftTest { deps ->
            preparePlayer(runecraft = 99)
            repeat(5) { player.inv[it] = InvObj(RunecraftObjs.darkEssenceFragments, 4) }
            player.inv[5] = InvObj(RunecraftObjs.scarredExtract, 20)
            player.inv[6] = activeBloodEssence(1_000)
            wearFullRaiments()
            random.next = 0
            repeat(19) { random.then = 0 }
            player.withProtectedAccess {
                vars[RunecraftVarbits.desertTreasureTwo] = 1
                vars[varbits.kourend_diary_elite_complete] = 1
            }
            val startXp = player.statMap.getFineXP(stats.runecrafting)

            player.withProtectedAccess { deps.kourend.craft(this, KourendRuneRecipes.blood) }

            assertEquals(1_254, player.count(objs.blood_rune))
            assertEquals(0, player.count(RunecraftObjs.darkEssenceFragments))
            assertEquals(0, player.count(RunecraftObjs.scarredExtract))
            assertEquals(4_760, player.statMap.getFineXP(stats.runecrafting) - startXp)
            val active =
                requireNotNull(
                    player.inv.firstOrNull { it.isType(RunecraftObjs.bloodEssenceActive) }
                )
            assertEquals(980, BloodEssenceState.fromActive(active).charges)
            assertEquals(RunecraftSeqs.craft.id, player.pendingSequence.id)
        }

    @Test
    fun GameTestState
        .`Kourend soul accepts Scarred extracts and raiments but never blood essence`() =
        runRunecraftTest { deps ->
            preparePlayer(runecraft = 90)
            repeat(3) { player.inv[it] = InvObj(RunecraftObjs.darkEssenceFragments, 4) }
            player.inv[3] = InvObj(RunecraftObjs.scarredExtract, 12)
            player.inv[4] = activeBloodEssence(321)
            wearFullRaiments()
            player.withProtectedAccess { vars[RunecraftVarbits.desertTreasureTwo] = 1 }
            random.next = 2
            val startXp = player.statMap.getFineXP(stats.runecrafting)

            player.withProtectedAccess { deps.kourend.craft(this, KourendRuneRecipes.soul) }

            assertEquals(738, player.count(objs.soul_rune))
            assertEquals(3_564, player.statMap.getFineXP(stats.runecrafting) - startXp)
            val active =
                requireNotNull(
                    player.inv.firstOrNull { it.isType(RunecraftObjs.bloodEssenceActive) }
                )
            assertEquals(321, BloodEssenceState.fromActive(active).charges)
        }

    @Test
    fun GameTestState.`Kourend altar membership level and fragment failures are atomic`() =
        runRunecraftTest { deps ->
            preparePlayer(members = false, runecraft = 99)
            player.inv[0] = InvObj(RunecraftObjs.darkEssenceFragments, 4)
            player.withProtectedAccess { deps.kourend.craft(this, KourendRuneRecipes.blood) }
            assertEquals(4, player.count(RunecraftObjs.darkEssenceFragments))

            preparePlayer(runecraft = 89)
            player.inv[0] = InvObj(RunecraftObjs.darkEssenceFragments, 4)
            player.withProtectedAccess { deps.kourend.craft(this, KourendRuneRecipes.soul) }
            assertEquals(4, player.count(RunecraftObjs.darkEssenceFragments))

            preparePlayer(runecraft = 99)
            player.withProtectedAccess { deps.kourend.craft(this, KourendRuneRecipes.blood) }
            assertEquals(0, player.count(objs.blood_rune))
            assertMessageSent("You do not have any dark essence fragments to bind.")
        }

    private fun GameTestScope.preparePlayer(
        members: Boolean = true,
        runecraft: Int = 99,
        crafting: Int = 99,
    ) {
        player.members = members
        player.stats[stats.runecrafting] = runecraft
        player.stats[stats.crafting] = crafting
        player.stats[stats.prayer] = 99
        player.clearInv()
        player.hat = null
        player.torso = null
        player.legs = null
        player.feet = null
        player.weakQueueList.clear()
        client.clear()
    }

    private fun GameTestScope.wearFullRaiments() {
        player.hat = InvObj(RunecraftObjs.raimentsHat)
        player.torso = InvObj(RunecraftObjs.raimentsTorso)
        player.legs = InvObj(RunecraftObjs.raimentsLegsBase)
        player.feet = InvObj(RunecraftObjs.raimentsBoots)
    }

    private fun activeBloodEssence(charges: Int): InvObj =
        InvObj(RunecraftObjs.bloodEssenceActive, vars = BloodEssenceState(charges).pack())

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

    data class Dependencies @Inject constructor(val kourend: KourendRuneCrafting)
}
