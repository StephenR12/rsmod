package org.rsmod.content.bosses.kalphitequeen

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
class KalphiteQueenTest {
    @Test
    fun GameTestState.`queen spawns and transforms into a full health airborne phase`() =
        runQueenTest(scripts = arrayOf(KalphiteQueenScript::class)) { deps ->
            val queen =
                deps.npcs.findAll(KalphiteQueenDefinition.spawn).single {
                    it.id == KalphiteQueenNpcs.ground.id
                }
            deps.controller.transformToAirborne(queen)
            assertEquals(KalphiteQueenNpcs.airborne.id, queen.visType.id)
            assertEquals(KalphiteQueenDefinition.HITPOINTS, queen.hitpoints)
            assertTrue(deps.controller.state(queen).airborne)
        }

    @Test
    fun GameTestState.`queen drop table always supplies both consumable and standard loot`() =
        runQueenTest(scripts = arrayOf(KalphiteQueenDrops::class)) { deps ->
            random.next = 100
            val queen = spawnNpc(player.coords, deps.types[KalphiteQueenNpcs.ground])
            val drops =
                checkNotNull(deps.drops[KalphiteQueenNpcs.ground])
                    .roll(NpcDropContext(queen, player, player.coords, deps.random))
            assertTrue(drops.size >= 2)
        }

    private fun GameTestState.runQueenTest(
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
        val controller: KalphiteQueenController,
    )
}
