package org.rsmod.content.bosses.godwars

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
class GodWarsTest {
    @Test
    fun GameTestState.`all four generals and twelve bodyguards spawn permanently`() =
        runGodWarsTest(scripts = arrayOf(GodWarsScript::class)) { deps ->
            val spawned =
                GodWarsDungeon.combatants.map { config ->
                    deps.npcs.findAll(config.spawn).single { it.id == config.npc.id }
                }
            assertEquals(16, spawned.size)
            assertTrue(spawned.all { it.respawns })
            assertEquals(
                listOf(255, 255, 255, 255),
                GodWarsDungeon.combatants
                    .filter { it.boss != null }
                    .map { config -> spawned.single { it.id == config.npc.id }.baseHitpointsLvl },
            )
        }

    @Test
    fun GameTestState.`boss rooms contain their configured generals`() = runGodWarsTest { _ ->
        GodWarsDungeon.combatants
            .filter { it.boss != null }
            .forEach { config ->
                val room = GodWarsDungeon.roomByBoss.getValue(config.boss!!)
                assertTrue(config.spawn.x in room.minX..room.maxX)
                assertTrue(config.spawn.z in room.minZ..room.maxZ)
                assertEquals(room.level, config.spawn.level)
            }
    }

    @Test
    fun GameTestState.`all general drop tables register with guaranteed remains`() =
        runGodWarsTest(scripts = arrayOf(GodWarsDrops::class)) { deps ->
            val bosses =
                listOf(
                    GodWarsNpcs.graardor to GodWarsObjs.bigBones,
                    GodWarsNpcs.kree to GodWarsObjs.bigBones,
                    GodWarsNpcs.zilyana to GodWarsObjs.bigBones,
                    GodWarsNpcs.kril to GodWarsObjs.infernalAshes,
                )
            random.next = 100
            bosses.forEach { (type, remains) ->
                val table = deps.drops[type]
                assertNotNull(table)
                val npc = spawnNpc(player.coords, deps.types[type])
                val rolled =
                    checkNotNull(table)
                        .roll(NpcDropContext(npc, player, player.coords, deps.random))
                        .map { it.obj.id }
                assertTrue(remains.id in rolled)
            }
        }

    @Test
    fun `published unique and pet denominators are represented exactly`() {
        assertEquals(1_524, GodWarsDropTables.UNIQUE_DENOMINATOR)
        assertEquals(5_000, GodWarsDropTables.PET_DENOMINATOR)
    }

    private fun GameTestState.runGodWarsTest(
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
