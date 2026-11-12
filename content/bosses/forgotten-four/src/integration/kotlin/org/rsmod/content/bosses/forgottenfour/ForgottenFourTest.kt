package org.rsmod.content.bosses.forgottenfour

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class ForgottenFourTest {
    @Test
    fun GameTestState.`all four post quest bosses spawn with registered signature drops`() =
        runForgottenFourTest(
            scripts = arrayOf(ForgottenFourScript::class, ForgottenFourDrops::class)
        ) { deps ->
            val bosses =
                ForgottenFour.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(4, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(ForgottenFour.definitions.all { deps.drops[it.npc] != null })
        }

    @Test
    fun GameTestState.`each encounter enters its authentic threshold state`() =
        runForgottenFourTest(scripts = arrayOf(ForgottenFourScript::class)) { deps ->
            val definitions = ForgottenFour.definitions.associateBy { it.family }

            val leviathan = definitions.getValue(ForgottenBossFamily.Leviathan)
            val leviathanNpc =
                deps.npcs.findAll(leviathan.spawn).single { it.id == leviathan.npc.id }
            leviathanNpc.hitpoints = ForgottenFourController.LEVIATHAN_ENRAGE_HP
            deps.controller.onBossDamaged(leviathanNpc, player, leviathan)
            assertTrue(deps.controller.state(leviathanNpc).enraged)

            val whisperer = definitions.getValue(ForgottenBossFamily.Whisperer)
            val whispererNpc =
                deps.npcs.findAll(whisperer.spawn).single { it.id == whisperer.npc.id }
            whispererNpc.hitpoints = 720
            deps.controller.onBossDamaged(whispererNpc, player, whisperer)
            assertTrue(deps.controller.state(whispererNpc).pendingSpecial != null)

            val vardorvis = definitions.getValue(ForgottenBossFamily.Vardorvis)
            val vardorvisNpc =
                deps.npcs.findAll(vardorvis.spawn).single { it.id == vardorvis.npc.id }
            vardorvisNpc.hitpoints = vardorvis.hitpoints / 3
            deps.controller.onBossDamaged(vardorvisNpc, player, vardorvis)
            assertTrue(deps.controller.state(vardorvisNpc).enraged)
            assertTrue(vardorvisNpc.defenceLvl < vardorvis.defence)
            assertTrue(vardorvisNpc.strengthLvl > vardorvis.strength)

            val duke = definitions.getValue(ForgottenBossFamily.Duke)
            val dukeNpc = deps.npcs.findAll(duke.spawn).single { it.id == duke.npc.id }
            dukeNpc.hitpoints = duke.hitpoints / 4
            deps.controller.onBossDamaged(dukeNpc, player, duke)
            assertTrue(deps.controller.state(dukeNpc).enraged)
        }

    @Test
    fun GameTestState
        .`whisperer sanity and leviathan shadow stun state are tracked per encounter`() =
        runForgottenFourTest(scripts = arrayOf(ForgottenFourScript::class)) { deps ->
            assertEquals(100, deps.controller.sanity(player))
            assertEquals(55, deps.controller.adjustSanity(player, -45))

            val leviathan =
                ForgottenFour.definitions.single { it.family == ForgottenBossFamily.Leviathan }
            val boss = deps.npcs.findAll(leviathan.spawn).single { it.id == leviathan.npc.id }
            deps.controller.rememberShadowCast(player)
            deps.controller.onBossDamaged(boss, player, leviathan)

            assertTrue(deps.controller.state(boss).leviathanStunned)
            assertEquals(1, deps.controller.state(boss).leviathanStuns)
        }

    @Test
    fun `effective vestige and pet rates remain encounter specific`() {
        assertEquals(768, ForgottenFourDrops.LEVIATHAN_UNIQUE_RATE)
        assertEquals(512, ForgottenFourDrops.WHISPERER_UNIQUE_RATE)
        assertEquals(1_088, ForgottenFourDrops.VARDORVIS_UNIQUE_RATE)
        assertEquals(720, ForgottenFourDrops.DUKE_UNIQUE_RATE)
        assertEquals(2_500, ForgottenFourDrops.LEVIATHAN_PET_RATE)
        assertEquals(2_000, ForgottenFourDrops.WHISPERER_PET_RATE)
        assertEquals(3_000, ForgottenFourDrops.VARDORVIS_PET_RATE)
        assertEquals(2_500, ForgottenFourDrops.DUKE_PET_RATE)
        assertEquals(
            listOf(96, 64, 136, 90),
            ForgottenFour.definitions.map { it.uniqueTableDenominator },
        )
    }

    @Test
    fun `vestige progress is independent per boss and completes on the third roll`() {
        val leviathan = ForgottenBossFamily.Leviathan
        val whisperer = ForgottenBossFamily.Whisperer

        val first = ForgottenFourDrops.advanceVestigeProgress(0, leviathan)
        assertEquals(1, first.stage)
        assertTrue(!first.completed)

        val otherBoss = ForgottenFourDrops.advanceVestigeProgress(first.packed, whisperer)
        assertEquals(1, otherBoss.stage)
        assertTrue(!otherBoss.completed)

        val second = ForgottenFourDrops.advanceVestigeProgress(otherBoss.packed, leviathan)
        assertEquals(2, second.stage)
        assertTrue(!second.completed)

        val third = ForgottenFourDrops.advanceVestigeProgress(second.packed, leviathan)
        assertEquals(0, third.stage)
        assertTrue(third.completed)

        val whispererStillPending =
            ForgottenFourDrops.advanceVestigeProgress(third.packed, whisperer)
        assertEquals(2, whispererStillPending.stage)
        assertTrue(!whispererStillPending.completed)
    }

    private fun GameTestState.runForgottenFourTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val controller: ForgottenFourController,
    )
}
