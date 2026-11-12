package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.death.NpcRareBoneDropRolls
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class GiantFrogDropsTest {
    @Test
    fun GameTestState.`loaded level thirteen Giant frog has exact cache and override contract`() =
        runGiantFrogTest { deps ->
            val type = deps.type
            assertEquals(8_700, type.id)
            assertEquals("Giant frog", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(listOf(10, 8, 6, 23, 1, 1), type.combatStats())
            assertEquals(50, type.respawnRate)
            assertEquals(
                constants.elemental_weakness_earth,
                type.param(params.elemental_weakness_type),
            )
            assertEquals(25, type.param(params.elemental_weakness_percent))
            assertEquals(setOf(type.id), deps.registry.registeredTypeIds())
            assertEquals(10_976, objs.long_bone.id)
            assertEquals(10_977, objs.curved_bone.id)
        }

    @Test
    fun GameTestState
        .`free worlds receive big bones and independent one in sixty four clue only`() =
        runGiantFrogTest { deps ->
            player.members = false

            random.next = 0
            assertDrops(deps, objs.big_bones, objs.clue_scroll_beginner)

            random.next = GiantFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1
            assertDrops(deps, objs.big_bones)
        }

    @Test
    fun GameTestState.`members rare bones are ordered mutually exclusive and clue independent`() =
        runGiantFrogTest { deps ->
            player.members = true

            random.next = GiantFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1
            random.then = 0
            assertDrops(deps, objs.big_bones, objs.long_bone)

            random.next = GiantFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1
            random.then = NpcRareBoneDropRolls.LONG_BONE_DENOMINATOR - 1
            random.then = 0
            assertDrops(deps, objs.big_bones, objs.curved_bone)

            random.next = 0
            random.then = 0
            assertDrops(deps, objs.big_bones, objs.clue_scroll_beginner, objs.long_bone)

            random.next = GiantFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1
            random.then = NpcRareBoneDropRolls.LONG_BONE_DENOMINATOR - 1
            random.then = NpcRareBoneDropRolls.CURVED_BONE_DENOMINATOR - 1
            assertDrops(deps, objs.big_bones)
        }

    private fun GameTestScope.assertDrops(deps: Dependencies, vararg expected: ObjType) {
        assertEquals(expected.map { it.id }, drops(deps).map { it.obj.id })
    }

    private fun GameTestScope.drops(deps: Dependencies) =
        checkNotNull(deps.registry[deps.type])
            .roll(
                NpcDropContext(
                    npc = spawnNpc(player.coords, deps.type),
                    hero = player,
                    coords = player.coords,
                    random = deps.random,
                )
            )

    private fun GameTestState.runGiantFrogTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(GiantFrogDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    ) {
        val type: UnpackedNpcType
            get() = npcTypes[npcs.giant_frog]
    }
}
