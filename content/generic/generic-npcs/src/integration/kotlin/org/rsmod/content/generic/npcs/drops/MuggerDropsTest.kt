package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class MuggerDropsTest {
    @Test
    fun GameTestState.`loaded Lumbridge mugger has exact cache and registry contract`() =
        runMuggerTest { deps ->
            val type = deps.muggerType
            assertEquals(513, type.id)
            assertEquals("Mugger", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(listOf(5, 5, 5, 8, 1, 1), type.combatStats())
            assertEquals(setOf(513), deps.registry.registeredTypeIds())
        }

    @Test
    fun GameTestState.`members main table has every exact 128 weight boundary`() =
        runMuggerTest { deps ->
            player.members = true
            val cases =
                listOf(
                    MainCase(0, objs.bronze_bolts.id, 2, 2),
                    MainCase(27, objs.mind_rune.id, 9),
                    MainCase(30, objs.water_rune.id, 6),
                    MainCase(32, objs.earth_rune.id, 5),
                    MainCase(34, objs.grimy_guam_leaf.id, 1, 0),
                    MainCase(47, objs.coins.id, 5),
                    MainCase(59, objs.coins.id, 15),
                    MainCase(62, objs.coins.id, 25),
                    MainCase(63, objs.rope.id, 1),
                    MainCase(103, null, 0),
                    MainCase(116, objs.knife.id, 1),
                    MainCase(117, objs.fishing_bait.id, 1),
                    MainCase(123, objs.copper_ore.id, 1),
                    MainCase(125, objs.bronze_med_helm.id, 1),
                    MainCase(127, objs.cabbage.id, 1),
                )

            cases.forEach { case ->
                val sequence = SequenceRandom()
                sequence.next = case.roll
                case.nestedRoll?.let { sequence.then = it }
                sequence.then = 79
                sequence.then = 127

                val drops = deps.table.roll(context(deps, sequence))
                val expectedIds = listOfNotNull(objs.bones.id, case.objId)
                assertEquals(expectedIds, drops.map { it.obj.id }, "main roll ${case.roll}")
                if (case.objId != null) {
                    assertEquals(case.count, drops.last().obj.count, "main roll ${case.roll}")
                }
            }
        }

    @Test
    fun GameTestState.`free worlds replace herbs and suppress member-only rewards`() =
        runMuggerTest { deps ->
            player.members = false

            val bolts = SequenceRandom()
            bolts.next = 0
            bolts.then = 79
            assertEquals(
                listOf(objs.bones.id),
                deps.table.roll(context(deps, bolts)).map { it.obj.id },
            )

            val herbs = SequenceRandom()
            herbs.next = 34
            herbs.then = 79
            val herbReplacement = deps.table.roll(context(deps, herbs))
            assertEquals(listOf(objs.bones.id, objs.coins.id), herbReplacement.map { it.obj.id })
            assertEquals(10, herbReplacement.last().obj.count)
        }

    @Test
    fun GameTestState.`beginner and easy tertiaries are independent and ordered`() =
        runMuggerTest { deps ->
            player.members = true
            random.next = 127
            random.then = 0
            random.then = 0
            random.then = Cache233EasyClueSteps.SIZE - 1

            val drops = deps.table.roll(context(deps))

            assertEquals(
                listOf(objs.bones.id, objs.cabbage.id, objs.clue_scroll_beginner.id, 30_928),
                drops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, drops[2].duration)
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, drops[3].duration)
        }

    private fun GameTestState.runMuggerTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(MuggerDrops::class),
            testBody = body,
        )

    private fun GameTestScope.context(
        deps: Dependencies,
        random: GameRandom = deps.random,
    ): NpcDropContext {
        val npc = spawnNpc(player.coords, deps.muggerType)
        return NpcDropContext(npc, player, npc.coords, random)
    }

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class MainCase(
        val roll: Int,
        val objId: Int?,
        val count: Int,
        val nestedRoll: Int? = null,
    )

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    ) {
        val muggerType: UnpackedNpcType
            get() = npcTypes[npcs.mugger]

        val table
            get() = checkNotNull(registry[muggerType])
    }
}
