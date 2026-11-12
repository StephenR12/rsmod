package org.rsmod.content.bosses.sporadic

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
class SporadicBossTest {
    @Test
    fun GameTestState.`five sporadic bosses spawn permanently with registered drops`() =
        runSporadicBossTest(
            scripts = arrayOf(SporadicBossScript::class, SporadicBossDrops::class)
        ) { deps ->
            val bosses =
                SporadicBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(5, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(SporadicBosses.definitions.all { deps.drops[it.npc] != null })
        }

    @Test
    fun GameTestState.`bryophyta and hespori become protected by their phase minions`() =
        runSporadicBossTest(scripts = arrayOf(SporadicBossScript::class)) { deps ->
            val bryophytaDefinition =
                SporadicBosses.definitions.single { it.family == SporadicBossFamily.Bryophyta }
            val bryophyta =
                deps.npcs.findAll(bryophytaDefinition.spawn).single {
                    it.id == bryophytaDefinition.npc.id
                }
            bryophyta.hitpoints = 58
            deps.controller.onBossDamaged(bryophyta, bryophytaDefinition)
            assertEquals(3, deps.controller.state(bryophyta).activeMinions.size)

            val hesporiDefinition =
                SporadicBosses.definitions.single { it.family == SporadicBossFamily.Hespori }
            val hespori =
                deps.npcs.findAll(hesporiDefinition.spawn).single {
                    it.id == hesporiDefinition.npc.id
                }
            hespori.hitpoints = 200
            deps.controller.onBossDamaged(hespori, hesporiDefinition)
            assertEquals(4, deps.controller.state(hespori).activeMinions.size)
            hespori.hitpoints = 100
            deps.controller
                .state(hespori)
                .activeMinions
                .toList()
                .forEach(deps.controller::onMinionKilled)
            deps.controller.onBossDamaged(hespori, hesporiDefinition)
            assertEquals(4, deps.controller.state(hespori).activeMinions.size)
            assertTrue(deps.controller.state(hespori).secondWave)
        }

    @Test
    fun `signature reward rates stay explicit`() {
        assertEquals(118, SporadicBossDrops.OBOR_UNIQUE_RATE)
        assertEquals(118, SporadicBossDrops.BRYOPHYTA_UNIQUE_RATE)
        assertEquals(40, SporadicBossDrops.MIMIC_RING_RATE)
        assertEquals(35, SporadicBossDrops.HESPORI_BUCKET_RATE)
        assertEquals(25, SporadicBossDrops.SKOTIZO_CLAW_RATE)
        assertEquals(200, SporadicBossDrops.SKOTIZO_JAR_RATE)
        assertEquals(65, SporadicBossDrops.SKOTIZO_PET_RATE)
    }

    private fun GameTestState.runSporadicBossTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val controller: SporadicBossController,
    )
}
