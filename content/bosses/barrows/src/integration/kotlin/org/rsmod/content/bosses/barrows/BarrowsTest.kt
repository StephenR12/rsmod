package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun GameTestState.`all six brothers spawn and unlock seven reward rolls`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(BarrowsScript::class)) { deps ->
            val brothers =
                BarrowsBrothers.all.map { brother ->
                    deps.npcs.findAll(brother.spawn).single { it.id == brother.npc.id }
                }
            brothers.forEach { deps.controller.onKill(player, it) }
            assertEquals(6, deps.controller.defeatedCount(player))

            random.next = 100
            player.withProtectedAccess { deps.controller.claim(this) }

            assertEquals(0, deps.controller.defeatedCount(player))
            assertTrue(player.inv.count(objs.death_rune) + player.inv.count(objs.blood_rune) > 0)
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
