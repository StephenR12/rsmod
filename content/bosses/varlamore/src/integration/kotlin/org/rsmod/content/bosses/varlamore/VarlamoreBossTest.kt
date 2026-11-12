package org.rsmod.content.bosses.varlamore

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
class VarlamoreBossTest {
    @Test
    fun GameTestState.`huey gemstone crab and all three moons spawn with drops`() =
        runBossTest(arrayOf(VarlamoreBossScript::class, VarlamoreBossDrops::class)) { deps ->
            val bosses =
                VarlamoreBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(5, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(VarlamoreBosses.definitions.all { deps.drops[it.npc] != null })
        }

    @Test
    fun GameTestState.`huey and moons activate their signature phase adds`() =
        runBossTest(arrayOf(VarlamoreBossScript::class)) { deps ->
            VarlamoreBosses.definitions
                .filter { it.family != VarlamoreBossFamily.GemstoneCrab }
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
        val controller: VarlamoreBossController,
    )
}
