package org.rsmod.content.bosses.modern

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
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class ModernBossTest {
    @Test
    fun GameTestState.`five cache native bosses spawn with signature drops`() =
        runBossTest(arrayOf(ModernBossScript::class, ModernBossDrops::class)) { deps ->
            val bosses =
                ModernBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(5, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(ModernBosses.definitions.all { deps.drops[it.npc] != null })
        }

    @Test
    fun GameTestState.`all modern encounters activate native signature adds`() =
        runBossTest(arrayOf(ModernBossScript::class)) { deps ->
            ModernBosses.definitions.forEach { definition ->
                val boss = deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                boss.hitpoints = definition.hitpoints * 2 / 3
                deps.controller.onBossDamaged(boss, definition)
                assertTrue(deps.controller.state(boss).firstSpecial)
                assertTrue(deps.controller.state(boss).adds > 0)
            }
        }

    @Test
    fun GameTestState.`doom delve health and shield phase scale through native forms`() =
        runBossTest(arrayOf(ModernBossScript::class)) { deps ->
            val definition = ModernBosses.definitions.single { it.family == ModernBossFamily.Doom }
            val boss = deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
            deps.controller.setDoomDelve(boss, 8)
            assertEquals(700, boss.hitpoints)
            assertEquals(8, deps.controller.state(boss).delve)
            boss.hitpoints = boss.baseHitpointsLvl / 3
            deps.controller.onBossDamaged(boss, definition)
            assertTrue(deps.controller.state(boss).secondSpecial)
            assertTrue(deps.controller.state(boss).adds >= 2)
            deps.controller.setDoomDelve(boss, 9)
            assertEquals(625, boss.hitpoints)
        }

    private fun GameTestState.runBossTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>>,
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val controller: ModernBossController,
    )
}
