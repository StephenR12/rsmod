package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class DagannothKingsTest {
    @Test
    fun GameTestState.`all three kings spawn with the complete combat triangle`() =
        runKingsTest(scripts = arrayOf(DagannothKingsScript::class)) { deps ->
            val kings =
                DagannothKings.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(3, kings.size)
            assertTrue(kings.all { it.baseHitpointsLvl == 255 && it.respawns })
            assertEquals(3, DagannothKings.definitions.map { it.style }.distinct().size)
        }

    @Test
    fun GameTestState.`all kings always drop dagannoth bones`() =
        runKingsTest(scripts = arrayOf(DagannothKingsDrops::class)) { deps ->
            random.next = 100
            DagannothKings.definitions.forEach { definition ->
                val npc = spawnNpc(player.coords, deps.types[definition.npc])
                val drops =
                    checkNotNull(deps.drops[definition.npc])
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                assertTrue(drops.any { it.obj.id == DagannothKingObjs.bones.id })
            }
        }

    private fun GameTestState.runKingsTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val drops: NpcDropTableRegistry,
        val random: GameRandom,
    )
}
