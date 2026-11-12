package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcHerbDropRolls
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList

class StandardHerbDropTableTest {
    @Test
    fun GameTestState.`cache 233 standard herb table resolves exact typed boundaries`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val expectedIds = listOf(199, 201, 203, 205, 207, 209, 211, 213, 215, 2_485, 217)
            val weights = listOf(32, 24, 18, 14, 11, 8, 6, 5, 4, 3, 3)
            val table = NpcDropTable(NpcHerbDropRolls.standard())
            val npc = spawnNpc(player.coords, deps.npcTypes[npcs.frog])
            val context = NpcDropContext(npc, player, npc.coords, deps.random)

            var lowerBound = 0
            expectedIds.zip(weights).forEach { (expectedId, weight) ->
                random.next = lowerBound
                assertEquals(expectedId, table.roll(context).single().obj.id)
                random.next = lowerBound + weight - 1
                assertEquals(expectedId, table.roll(context).single().obj.id)
                lowerBound += weight
            }

            assertEquals(128, lowerBound)
        }

    private data class Dependencies
    @Inject
    constructor(val npcTypes: NpcTypeList, val random: GameRandom)
}
