package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList

@Execution(ExecutionMode.SAME_THREAD)
class WorldBossTest {
    @Test
    fun GameTestState.`four uncovered world bosses now spawn with registered drops`() =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(WorldBossScript::class, WorldBossDrops::class),
        ) { deps ->
            val bosses =
                WorldBosses.definitions.map { definition ->
                    deps.npcs.findAll(definition.spawn).single { it.id == definition.npc.id }
                }
            assertEquals(4, bosses.size)
            assertTrue(bosses.all { it.respawns })
            assertTrue(WorldBosses.definitions.all { deps.drops[it.npc] != null })
        }

    private data class Dependencies
    @Inject
    constructor(val types: NpcTypeList, val npcs: NpcRepository, val drops: NpcDropTableRegistry)
}
