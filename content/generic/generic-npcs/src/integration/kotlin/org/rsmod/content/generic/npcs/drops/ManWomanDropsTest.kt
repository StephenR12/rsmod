package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class ManWomanDropsTest {
    @Test
    fun GameTestState.`seven loaded civilians have exact cache combat and registry contracts`() =
        runCivilianTest { deps ->
            assertEquals(MAN_IDS + WOMAN_IDS, deps.registry.registeredTypeIds())
            val all = deps.manTypes + deps.womanTypes
            all.forEach { type ->
                assertEquals("Attack", type.op[1])
                assertEquals("Pickpocket", type.op[2])
                assertEquals(1, type.size)
                assertEquals(listOf(1, 1, 1, 7, 1, 1), type.combatStats())
                assertEquals(25, type.respawnRate)
                assertEquals(4, type.param(params.attackrate))
                assertEquals(categories.attacktype_crush, type.param(params.npc_attack_type))
                assertNull(type.huntMode)
                assertNull(type.paramOrNull(params.elemental_weakness_type))
            }
            assertEquals(setOf("Man"), deps.manTypes.mapTo(hashSetOf()) { it.name })
            assertEquals(setOf("Woman"), deps.womanTypes.mapTo(hashSetOf()) { it.name })
            deps.manTypes.forEach { assertSame(deps.manTable, deps.registry[it]) }
            deps.womanTypes.forEach { assertSame(deps.womanTable, deps.registry[it]) }
            assertNotSame(deps.manTable, deps.womanTable)
        }

    @Test
    fun GameTestState.`members main table has every exact 128 weight boundary`() =
        runCivilianTest { deps ->
            player.members = true
            val cases =
                listOf(
                    MainCase(0, objs.bronze_med_helm.id),
                    MainCase(2, objs.iron_dagger.id),
                    MainCase(3, objs.bronze_bolts.id, 2, nestedRoll = 2),
                    MainCase(25, objs.bronze_arrow.id, 7),
                    MainCase(28, objs.earth_rune.id, 4),
                    MainCase(30, objs.fire_rune.id, 6),
                    MainCase(32, objs.mind_rune.id, 9),
                    MainCase(34, objs.chaos_rune.id, 2),
                    MainCase(35, objs.grimy_guam_leaf.id, nestedRoll = 0),
                    MainCase(58, objs.coins.id, 3),
                    MainCase(96, objs.coins.id, 5),
                    MainCase(105, objs.coins.id, 15),
                    MainCase(109, objs.coins.id, 25),
                    MainCase(110, null),
                    MainCase(118, objs.fishing_bait.id),
                    MainCase(123, objs.copper_ore.id),
                    MainCase(125, objs.earth_talisman.id),
                    MainCase(127, objs.cabbage.id),
                )

            cases.forEach { case ->
                val result = deps.manTable.roll(context(deps, deps.manTypes.first(), misses(case)))
                assertMain(case, result)
            }
        }

    @Test
    fun GameTestState.`free worlds preserve the bolt empty selection and replace herbs`() =
        runCivilianTest { deps ->
            player.members = false
            val bolts = deps.manTable.roll(context(deps, deps.manTypes.first(), freeMisses(3)))
            assertEquals(listOf(objs.bones.id), bolts.map { it.obj.id })

            val herbs = deps.womanTable.roll(context(deps, deps.womanTypes.first(), freeMisses(35)))
            assertEquals(listOf(objs.bones.id, objs.coins.id), herbs.map { it.obj.id })
            assertEquals(10, herbs.last().obj.count)
        }

    @Test
    fun GameTestState.`medium step key is exact carried members Man condition`() =
        runCivilianTest { deps ->
            player.members = true
            player.inv[0] = InvObj(objs.chicken_clue_scroll_medium)
            assertEquals(2_837, objs.chicken_clue_scroll_medium.id)
            assertEquals(2_838, objs.chicken_clue_key_medium.id)

            val man =
                deps.manTable.roll(context(deps, deps.manTypes.first(), misses(MainCase(110))))
            assertEquals(
                listOf(objs.bones.id, objs.chicken_clue_key_medium.id),
                man.map { it.obj.id },
            )

            val woman =
                deps.womanTable.roll(context(deps, deps.womanTypes.first(), misses(MainCase(110))))
            assertEquals(listOf(objs.bones.id), woman.map { it.obj.id })

            player.members = false
            val freeMan =
                deps.manTable.roll(context(deps, deps.manTypes.first(), freeMisses(main = 110)))
            assertEquals(listOf(objs.bones.id), freeMan.map { it.obj.id })
        }

    @Test
    fun GameTestState.`beginner and Easy clues are independent ordered and membership exact`() =
        runCivilianTest { deps ->
            player.members = true
            val members =
                SequenceRandom().apply {
                    next = 127
                    then = 0
                    then = 0
                    then = Cache233EasyClueSteps.SIZE - 1
                }
            val memberDrops = deps.womanTable.roll(context(deps, deps.womanTypes.first(), members))
            assertEquals(
                listOf(objs.bones.id, objs.cabbage.id, objs.clue_scroll_beginner.id, 31_268),
                memberDrops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, memberDrops[2].duration)
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, memberDrops[3].duration)

            player.members = false
            val free =
                SequenceRandom().apply {
                    next = 127
                    then = 0
                }
            val freeDrops = deps.manTable.roll(context(deps, deps.manTypes.first(), free))
            assertEquals(
                listOf(objs.bones.id, objs.cabbage.id, objs.clue_scroll_beginner.id),
                freeDrops.map { it.obj.id },
            )
        }

    private fun assertMain(case: MainCase, drops: List<org.rsmod.api.death.NpcDrop>) {
        assertEquals(
            listOfNotNull(objs.bones.id, case.objId),
            drops.map { it.obj.id },
            "main roll ${case.roll}",
        )
        if (case.objId != null) {
            assertEquals(case.count, drops.last().obj.count, "main roll ${case.roll}")
        }
    }

    private fun misses(case: MainCase): SequenceRandom =
        SequenceRandom().apply {
            next = case.roll
            case.nestedRoll?.let { then = it }
            then = ManWomanDrops.BEGINNER_CLUE_DENOMINATOR - 1
            then = ManWomanDrops.EASY_CLUE_DENOMINATOR - 1
        }

    private fun freeMisses(main: Int): SequenceRandom =
        SequenceRandom().apply {
            next = main
            then = ManWomanDrops.BEGINNER_CLUE_DENOMINATOR - 1
        }

    private fun GameTestScope.context(
        deps: Dependencies,
        type: UnpackedNpcType,
        random: GameRandom,
    ): NpcDropContext =
        NpcDropContext(
            npc = spawnNpc(player.coords, type),
            hero = player,
            coords = player.coords,
            random = random,
        )

    private fun GameTestState.runCivilianTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(ManWomanDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class MainCase(
        val roll: Int,
        val objId: Int? = null,
        val count: Int = 1,
        val nestedRoll: Int? = null,
    )

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    ) {
        val manTypes: List<UnpackedNpcType>
            get() = ManWomanDrops.manTypes.map { npcTypes[it] }

        val womanTypes: List<UnpackedNpcType>
            get() = ManWomanDrops.womanTypes.map { npcTypes[it] }

        val manTable: NpcDropTable
            get() = checkNotNull(registry[manTypes.first()])

        val womanTable: NpcDropTable
            get() = checkNotNull(registry[womanTypes.first()])
    }

    private companion object {
        val MAN_IDS: Set<Int> = setOf(3_106, 3_107, 3_108, 6_818)
        val WOMAN_IDS: Set<Int> = setOf(3_111, 3_112, 3_113)
    }
}
