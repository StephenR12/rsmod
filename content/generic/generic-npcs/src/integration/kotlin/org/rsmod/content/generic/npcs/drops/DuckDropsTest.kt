package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList

class DuckDropsTest {
    @Test
    fun GameTestState.`cache 233 ducks and duckling intentionally drop nothing`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(DuckDrops::class)) { deps ->
            val types =
                listOf(
                    deps.npcTypes[npcs.duck],
                    deps.npcTypes[npcs.duck_female],
                    deps.npcTypes[npcs.duckling],
                )

            assertEquals(listOf(1_838, 1_839, 2_001), types.map { it.id })
            assertEquals(listOf("Duck", "Duck", "Duckling"), types.map { it.name })
            assertTrue(types.all { "Attack" in it.op })
            assertEquals(setOf(1_838, 1_839, 2_001), deps.registry.registeredTypeIds())

            types.forEachIndexed { index, type ->
                val table = checkNotNull(deps.registry[type])
                val npc = spawnNpc(player.coords, type)
                npc.heroPoints(player, 1)
                val dropCoords = player.coords.translate(xOffset = 21 + index, zOffset = 21)
                val context = NpcDropContext(npc, player, dropCoords, deps.random)

                assertTrue(table.roll(context).isEmpty())
                deps.death.spawnDrops(npc, dropCoords)
                assertEquals(0, deps.objRepo.findAll(dropCoords).count())
            }
        }

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val death: NpcDeath,
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    )
}
