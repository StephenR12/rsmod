package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcClueDropRolls
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList

class EasyClueDropRollsTest {
    @Test
    fun GameTestState.`cache 238 easy pool has exact active decoder identities`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val steps = deps.easySteps.steps

            assertEquals(Cache233EasyClueSteps.SIZE, steps.size)
            assertEquals(Cache233EasyClueSteps.CACHE_238_IDS, steps.map { it.id })
            assertEquals(
                120,
                steps.count {
                    "_simple" in it.internalName.orEmpty() || "_vague" in it.internalName.orEmpty()
                },
            )
            assertEquals(33, steps.count { "_emote" in it.internalName.orEmpty() })
            assertEquals(7, steps.count { "_map" in it.internalName.orEmpty() })
            assertEquals(6, steps.count { "_music" in it.internalName.orEmpty() })
            assertTrue(steps.all { it.name == "Clue scroll (easy)" })
            assertTrue(steps.all { it.iop[0] == "Read" && it.iop[1] == "Check steps" })

            assertThrows<IllegalArgumentException> { deps.easySteps.select(-1) }
            assertThrows<IllegalArgumentException> { deps.easySteps.select(steps.size) }
        }

    @Test
    fun GameTestState.`npc easy clue success selects both pool endpoints`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            player.members = true
            val npc = spawnNpc(player.coords, deps.npcTypes[npcs.frog])
            val context = NpcDropContext(npc, player, npc.coords, deps.random)
            val table = deps.clueRolls.independentEasy(denominator = 128)

            random.next = 0
            random.then = 0
            assertEquals(2_677, table.roll(context).drops.single().obj.id)

            random.next = 0
            random.then = Cache233EasyClueSteps.SIZE - 1
            assertEquals(31_268, table.roll(context).drops.single().obj.id)
        }

    @Test
    fun GameTestState.`scroll box success does not consume an initial step roll`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            player.members = true
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            val npc = spawnNpc(player.coords, deps.npcTypes[npcs.frog])
            val context = NpcDropContext(npc, player, npc.coords, deps.random)
            val table = deps.clueRolls.independentEasy(denominator = 128)

            random.next = 0
            random.then = 77
            assertEquals(objs.scroll_box_easy.id, table.roll(context).drops.single().obj.id)
            assertEquals(77, deps.random.of(Cache233EasyClueSteps.SIZE))
        }

    private data class Dependencies
    @Inject
    constructor(
        val easySteps: Cache233EasyClueSteps,
        val clueRolls: NpcClueDropRolls,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    )
}
