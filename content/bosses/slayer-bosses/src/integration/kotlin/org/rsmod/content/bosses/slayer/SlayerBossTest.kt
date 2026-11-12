package org.rsmod.content.bosses.slayer

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
class SlayerBossTest {
    @Test
    fun GameTestState.`eight cache backed slayer bosses spawn with signature drops`() =
        runSlayerBossTest(scripts = arrayOf(SlayerBossScript::class, SlayerBossDrops::class)) { deps
            ->
            val bosses =
                SlayerBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(8, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(SlayerBosses.definitions.all { deps.drops[it.npc] != null })
        }

    @Test
    fun GameTestState.`summon based slayer bosses spawn their phase combatants`() =
        runSlayerBossTest(scripts = arrayOf(SlayerBossScript::class)) { deps ->
            SlayerBosses.definitions
                .filter {
                    it.family in
                        setOf(
                            SlayerBossFamily.Cerberus,
                            SlayerBossFamily.Kraken,
                            SlayerBossFamily.Sire,
                            SlayerBossFamily.Araxxor,
                        )
                }
                .forEach { definition ->
                    val boss =
                        deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                    boss.hitpoints = definition.hitpoints / 2
                    deps.controller.onBossDamaged(boss, definition)
                    assertTrue(deps.controller.state(boss).firstWave)
                    assertTrue(deps.controller.state(boss).activeMinions.size >= 3)
                }
        }

    @Test
    fun GameTestState.`hydra and grotesque guardian thresholds advance encounter state`() =
        runSlayerBossTest(scripts = arrayOf(SlayerBossScript::class)) { deps ->
            listOf(SlayerBossFamily.Dusk, SlayerBossFamily.Hydra).forEach { family ->
                val definition = SlayerBosses.definitions.single { it.family == family }
                val boss = deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                boss.hitpoints = definition.hitpoints / 2
                deps.controller.onBossDamaged(boss, definition)
                assertTrue(deps.controller.state(boss).firstWave)
            }
            val hydra = SlayerBosses.definitions.single { it.family == SlayerBossFamily.Hydra }
            val npc = deps.npcs.findAll(hydra.spawn).single { it.id == hydra.npc.id }
            assertTrue(deps.controller.state(npc).secondWave)
        }

    private fun GameTestState.runSlayerBossTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val controller: SlayerBossController,
    )
}
