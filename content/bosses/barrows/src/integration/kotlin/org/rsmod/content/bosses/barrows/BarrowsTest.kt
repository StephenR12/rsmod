package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BarrowsTest {
    @Test
    fun GameTestState.`all six credited brothers unlock seven reward rolls`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(BarrowsScript::class)) { deps ->
            val brothers =
                BarrowsBrothers.all.map { brother ->
                    spawnNpc(brother.spawn, npcTypes[brother.npc])
                }
            brothers.forEach { deps.controller.onKill(player, it) }
            assertEquals(6, deps.controller.defeatedCount(player))

            repeat(7) {
                random.then = 1
                random.then = 630
                random.then = 1
            }
            player.withProtectedAccess { deps.controller.claim(this) }

            assertEquals(0, deps.controller.defeatedCount(player))
            assertEquals(7 * 70, player.inv.count(objs.death_rune))
        }

    private data class Dependencies
    @Inject
    constructor(
        val types: NpcTypeList,
        val npcs: NpcRepository,
        val random: GameRandom,
        val controller: BarrowsController,
    )
}
