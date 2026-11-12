package org.rsmod.content.bosses.giantmole

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
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.plugin.scripts.PluginScript

@Execution(ExecutionMode.SAME_THREAD)
class GiantMoleTest {
    @Test
    fun GameTestState.`boss config and permanent lair spawn are registered`() =
        runMoleTest(scripts = arrayOf(GiantMoleScript::class)) { deps ->
            val type = deps.type
            assertEquals(5_779, type.id)
            assertEquals("Giant Mole", type.name)
            val spawns =
                deps.npcRepo.findAll(GiantMoleLair.spawn).filter { it.id == type.id }.toList()
            assertEquals(1, spawns.size)
            val mole = spawns.single()
            assertEquals(listOf(200, 200, 200, 200, 1, 1), mole.combatStats())
            assertEquals(GiantMoleScript.HUNT_RANGE, mole.huntRange)
            assertTrue(mole.respawns)
        }

    @Test
    fun GameTestState.`guaranteed drops contain bones claw and one to three skins`() =
        runMoleTest(scripts = arrayOf(GiantMoleDrops::class)) { deps ->
            random.next = 1
            assertEquals(
                listOf(
                    GiantMoleObjs.bigBones.id to 1,
                    GiantMoleObjs.claw.id to 1,
                    GiantMoleObjs.skin.id to 1,
                ),
                deps.rollDrops(this),
            )

            random.next = 3
            assertEquals(3, deps.rollDrops(this).last().second)
        }

    @Test
    fun GameTestState.`burrow destination always changes to a configured lair tile`() =
        runMoleTest { deps ->
            random.next = 0
            val destination =
                deps.combat.burrowDestination(GiantMoleLair.burrowDestinations.first())
            assertTrue(destination in GiantMoleLair.burrowDestinations)
            assertTrue(destination != GiantMoleLair.burrowDestinations.first())
        }

    private fun GameTestState.runMoleTest(
        scripts: Array<kotlin.reflect.KClass<out PluginScript>> = emptyArray(),
        body: GameTestScope.(Dependencies) -> Unit,
    ) = runInjectedGameTest(Dependencies::class, scripts = scripts, testBody = body)

    private fun Dependencies.rollDrops(scope: GameTestScope): List<Pair<Int, Int>> {
        val npc = scope.spawnNpc(scope.player.coords, type)
        val table = checkNotNull(registry[type])
        return table.roll(NpcDropContext(npc, scope.player, scope.player.coords, random)).map {
            it.obj.id to it.obj.count
        }
    }

    private fun org.rsmod.game.entity.Npc.combatStats(): List<Int> =
        listOf(attackLvl, strengthLvl, defenceLvl, hitpoints, rangedLvl, magicLvl)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val npcRepo: NpcRepository,
        val combat: GiantMoleCombat,
        val random: GameRandom,
    ) {
        val type: UnpackedNpcType
            get() = npcTypes[GiantMoleNpcs.boss]
    }
}
