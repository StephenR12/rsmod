package org.rsmod.content.bosses.instanced

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
class InstancedBossTest {
    @Test
    fun GameTestState.`four classic instanced bosses spawn with drops`() =
        runBossTest(arrayOf(InstancedBossScript::class, InstancedBossDrops::class)) { deps ->
            val bosses =
                InstancedBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(4, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(InstancedBosses.definitions.all { deps.drops[it.npc] != null })
        }

    @Test
    fun GameTestState.`vorkath and nightmare variants activate signature adds`() =
        runBossTest(arrayOf(InstancedBossScript::class)) { deps ->
            InstancedBosses.definitions
                .filter { it.family != InstancedBossFamily.Muspah }
                .forEach { definition ->
                    val boss =
                        deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                    boss.hitpoints = definition.hitpoints * 2 / 3
                    deps.controller.onBossDamaged(boss, definition)
                    assertTrue(deps.controller.state(boss).firstSpecial)
                    assertTrue(deps.controller.state(boss).adds > 0)
                }
        }

    private fun GameTestState.runBossTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>>,
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val controller: InstancedBossController,
    )
}
