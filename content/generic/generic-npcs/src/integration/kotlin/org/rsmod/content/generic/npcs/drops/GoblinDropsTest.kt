package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.Cache233EasyClueSteps
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcChampionScrollDropRolls
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropRollResult
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.random.SequenceRandom
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class GoblinDropsTest {
    @Test
    fun GameTestState.`nine loaded Goblins have exact cache and production combat contracts`() =
        runGoblinTest { deps ->
            assertEquals(TABLE_ONE_IDS + TABLE_TWO_ID, deps.registry.registeredTypeIds())
            deps.tableOneTypes.forEach { type ->
                assertEquals("Goblin", type.name)
                assertEquals("Attack", type.op[1])
                assertEquals(1, type.size)
                assertEquals(listOf(1, 1, 1, 5, 1, 1), type.combatStats())
                assertEquals(35, type.respawnRate)
                assertEquals(4, type.param(params.attackrate))
                assertEquals(categories.attacktype_crush, type.param(params.npc_attack_type))
                assertNull(type.huntMode)
                assertNull(type.paramOrNull(params.elemental_weakness_type))
            }
            with(deps.tableTwoType) {
                assertEquals(TABLE_TWO_ID, id)
                assertEquals("Goblin", name)
                assertEquals("Attack", op[1])
                assertEquals(1, size)
                assertEquals(listOf(3, 1, 4, 12, 1, 1), combatStats())
                assertEquals(35, respawnRate)
                assertEquals(6, param(params.attackrate))
                assertEquals(categories.attacktype_stab, param(params.npc_attack_type))
                assertNull(huntMode)
                assertNull(paramOrNull(params.elemental_weakness_type))
            }
            assertEquals(1_455, varbits.champions_defeated_goblin.id)
            assertEquals(listOf(7_812, 7_813, 7_814), deps.questBone.lifecycleIds.sorted())
        }

    @Test
    fun GameTestState.`table one has every exact 128 weight boundary`() = runGoblinTest { deps ->
        player.members = true
        val cases =
            listOf(
                MainCase(0, objs.bronze_sq_shield.id),
                MainCase(3, objs.bronze_spear.id),
                MainCase(7, objs.body_rune.id, 7),
                MainCase(12, objs.water_rune.id, 6),
                MainCase(18, objs.earth_rune.id, 4),
                MainCase(21, objs.bronze_bolts.id, 8),
                MainCase(24, objs.coins.id, 5),
                MainCase(52, objs.coins.id, 9),
                MainCase(55, objs.coins.id, 15),
                MainCase(58, objs.coins.id, 20),
                MainCase(60, objs.coins.id),
                MainCase(61, null),
                MainCase(99, objs.hammer.id),
                MainCase(114, objs.goblin_book.id),
                MainCase(116, objs.goblin_mail.id),
                MainCase(121, objs.chefs_hat.id),
                MainCase(124, objs.beer.id),
                MainCase(126, objs.brass_necklace.id),
                MainCase(127, objs.air_talisman.id),
            )

        cases.forEach { case ->
            val result =
                deps.tableOne.roll(
                    context(deps, deps.tableOneTypes.first(), tableOneMisses(case.roll))
                )
            assertMain(case, result)
        }
    }

    @Test
    fun GameTestState.`table two has every exact 128 weight boundary and nested herb table`() =
        runGoblinTest { deps ->
            player.members = true
            val cases =
                listOf(
                    MainCase(0, objs.bronze_axe.id),
                    MainCase(3, objs.bronze_scimitar.id),
                    MainCase(4, objs.bronze_spear.id),
                    MainCase(13, objs.bronze_arrow.id, 7),
                    MainCase(16, objs.mind_rune.id, 2),
                    MainCase(19, objs.earth_rune.id, 4),
                    MainCase(22, objs.body_rune.id, 2),
                    MainCase(25, objs.bronze_javelin.id, 5),
                    MainCase(27, objs.chaos_rune.id),
                    MainCase(28, objs.nature_rune.id),
                    MainCase(29, objs.grimy_guam_leaf.id, nestedRoll = 0),
                    MainCase(31, objs.coins.id),
                    MainCase(65, objs.coins.id, 3),
                    MainCase(78, objs.coins.id, 5),
                    MainCase(86, objs.coins.id, 16),
                    MainCase(93, objs.coins.id, 24),
                    MainCase(96, null),
                    MainCase(104, objs.hammer.id),
                    MainCase(113, objs.goblin_book.id),
                    MainCase(115, objs.green_goblin_mail.id),
                    MainCase(125, objs.grapes.id),
                    MainCase(126, objs.red_cape.id),
                    MainCase(127, objs.tin_ore.id),
                )

            cases.forEach { case ->
                val sequence = tableTwoMisses(case.roll, case.nestedRoll)
                val result = deps.tableTwo.roll(context(deps, deps.tableTwoType, sequence))
                assertMain(case, result)
            }
        }

    @Test
    fun GameTestState.`free worlds preserve selected empty slots and replace the herb branch`() =
        runGoblinTest { deps ->
            player.members = false
            listOf(3, 114).forEach { roll ->
                val drops =
                    deps.tableOne.roll(
                        context(deps, deps.tableOneTypes.first(), freeMisses(roll, 63))
                    )
                assertEquals(listOf(objs.bones.id), drops.map { it.obj.id })
            }
            listOf(4, 25, 113).forEach { roll ->
                val drops =
                    deps.tableTwo.roll(context(deps, deps.tableTwoType, freeMisses(roll, 79)))
                assertEquals(listOf(objs.bones.id), drops.map { it.obj.id })
            }

            val replacement =
                deps.tableTwo.roll(context(deps, deps.tableTwoType, freeMisses(29, 79)))
            assertEquals(listOf(objs.bones.id, objs.coins.id), replacement.map { it.obj.id })
            assertEquals(10, replacement.last().obj.count)
        }

    @Test
    fun GameTestState.`table one tertiaries use exact independent rates order and provenance`() =
        runGoblinTest { deps ->
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            player.setVarp(varps.quest_points, NpcChampionScrollDropRolls.MIN_QUEST_POINTS)
            val sequence =
                SequenceRandom().apply {
                    next = 127
                    then = 0
                    then = 0
                    then = 0
                    then = 0
                    then = Cache233EasyClueSteps.SIZE - 1
                    then = 0
                }

            val result = deps.tableOne.resolve(context(deps, deps.tableOneTypes.first(), sequence))
            assertEquals(
                listOf(
                    objs.bones.id,
                    objs.air_talisman.id,
                    objs.goblin_skull.id,
                    objs.ensouled_goblin_head_initial.id,
                    objs.clue_scroll_beginner.id,
                    30_928,
                    objs.goblin_champion_scroll.id,
                ),
                result.drops.map { it.obj.id },
            )
            assertEquals(player.coords, FreshDropOrigin.unpack(result.drops[3].obj.vars))
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, result.drops[4].duration)
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, result.drops[5].duration)
            assertEquals(listOf(NpcChampionScrollDropRolls.DROP_MESSAGE), result.messages)
        }

    @Test
    fun GameTestState.`Goblin skull chance ownership lifecycle and hand in bit are exact`() =
        runGoblinTest { deps ->
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            val type = deps.tableOneTypes.first()

            val miss = activeQuestSequence(main = 61, skull = 3)
            assertEquals(
                listOf(objs.bones.id),
                deps.tableOne.roll(context(deps, type, miss)).map { it.obj.id },
            )

            val hit = activeQuestSequence(main = 61, skull = 0)
            assertEquals(
                listOf(objs.bones.id, objs.goblin_skull.id),
                deps.tableOne.roll(context(deps, type, hit)).map { it.obj.id },
            )

            listOf(objs.goblin_skull, objs.goblin_skull_in_vinegar, objs.polished_goblin_skull)
                .forEach { obj ->
                    player.inv[0] = InvObj(obj)
                    assertFalse(deps.questBones.isNeeded(player, deps.questBone))
                    player.clearInv()

                    val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])
                    bank[0] = InvObj(obj)
                    assertFalse(deps.questBones.isNeeded(player, deps.questBone))
                    bank[0] = null
                }
            assertTrue(deps.questBones.isNeeded(player, deps.questBone))
            deps.questBones.markHandedIn(player, deps.questBone)
            assertEquals(
                1 shl GoblinDrops.GOBLIN_SKULL_INDEX,
                player.vars[varps.rag_and_bone_man_i_handed_mask],
            )
            assertFalse(deps.questBones.isNeeded(player, deps.questBone))
        }

    @Test
    fun GameTestState.`table two head rate and champion completion policy are exact`() =
        runGoblinTest { deps ->
            player.members = true
            val headMiss = tableTwoMisses(main = 96)
            assertEquals(
                listOf(objs.bones.id),
                deps.tableTwo.roll(context(deps, deps.tableTwoType, headMiss)).map { it.obj.id },
            )

            val headHit =
                SequenceRandom().apply {
                    next = 96
                    then = 0
                    then = 79
                    then = 127
                }
            val headDrops = deps.tableTwo.roll(context(deps, deps.tableTwoType, headHit))
            assertEquals(
                listOf(objs.bones.id, objs.ensouled_goblin_head_initial.id),
                headDrops.map { it.obj.id },
            )
            assertEquals(player.coords, FreshDropOrigin.unpack(headDrops.last().obj.vars))

            player.setVarp(varps.quest_points, NpcChampionScrollDropRolls.MIN_QUEST_POINTS)
            val scroll = deps.resolveTableTwo(context(deps, deps.tableTwoType, championSequence()))
            assertEquals(
                listOf(objs.bones.id, objs.goblin_champion_scroll.id),
                scroll.drops.map { it.obj.id },
            )
            assertEquals(listOf(NpcChampionScrollDropRolls.DROP_MESSAGE), scroll.messages)

            player.setVarBit(varbits.champions_defeated_goblin, 1)
            val duplicate =
                deps.resolveTableTwo(context(deps, deps.tableTwoType, championSequence()))
            assertEquals(listOf(objs.bones.id), duplicate.drops.map { it.obj.id })
            assertEquals(listOf(NpcChampionScrollDropRolls.DUPLICATE_MESSAGE), duplicate.messages)
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

    private fun tableOneMisses(main: Int): SequenceRandom =
        SequenceRandom().apply {
            next = main
            then = GoblinDrops.ENSOULED_HEAD_TABLE_ONE_DENOMINATOR - 1
            then = GoblinDrops.BEGINNER_TABLE_ONE_DENOMINATOR - 1
            then = GoblinDrops.EASY_CLUE_DENOMINATOR - 1
        }

    private fun tableTwoMisses(main: Int, nested: Int? = null): SequenceRandom =
        SequenceRandom().apply {
            next = main
            nested?.let { then = it }
            then = GoblinDrops.ENSOULED_HEAD_TABLE_TWO_DENOMINATOR - 1
            then = GoblinDrops.BEGINNER_TABLE_TWO_DENOMINATOR - 1
            then = GoblinDrops.EASY_CLUE_DENOMINATOR - 1
        }

    private fun freeMisses(main: Int, beginnerMiss: Int): SequenceRandom =
        SequenceRandom().apply {
            next = main
            then = beginnerMiss
            then = GoblinDrops.EASY_CLUE_DENOMINATOR - 1
        }

    private fun activeQuestSequence(main: Int, skull: Int): SequenceRandom =
        SequenceRandom().apply {
            next = main
            then = skull
            then = GoblinDrops.ENSOULED_HEAD_TABLE_ONE_DENOMINATOR - 1
            then = GoblinDrops.BEGINNER_TABLE_ONE_DENOMINATOR - 1
            then = GoblinDrops.EASY_CLUE_DENOMINATOR - 1
        }

    private fun championSequence(): SequenceRandom =
        SequenceRandom().apply {
            next = 96
            then = GoblinDrops.ENSOULED_HEAD_TABLE_TWO_DENOMINATOR - 1
            then = GoblinDrops.BEGINNER_TABLE_TWO_DENOMINATOR - 1
            then = GoblinDrops.EASY_CLUE_DENOMINATOR - 1
            then = 0
        }

    private fun GameTestScope.context(
        deps: Dependencies,
        type: UnpackedNpcType,
        random: GameRandom = deps.random,
    ): NpcDropContext =
        NpcDropContext(
            npc = spawnNpc(player.coords, type),
            hero = player,
            coords = player.coords,
            random = random,
        )

    private fun GameTestState.runGoblinTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(GoblinDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class MainCase(
        val roll: Int,
        val objId: Int?,
        val count: Int = 1,
        val nestedRoll: Int? = null,
    )

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val npcTypes: NpcTypeList,
        val questBones: RagAndBoneManIDropPolicy,
        val invTypes: InvTypeList,
        val random: GameRandom,
    ) {
        val tableOneTypes: List<UnpackedNpcType>
            get() = GoblinDrops.tableOneTypes.map { npcTypes[it] }

        val tableTwoType: UnpackedNpcType
            get() = npcTypes[npcs.goblin_level_5]

        val tableOne: NpcDropTable
            get() = checkNotNull(registry[tableOneTypes.first()])

        val tableTwo: NpcDropTable
            get() = checkNotNull(registry[tableTwoType])

        val questBone: RagAndBoneManIDropPolicy.QuestBone
            get() =
                RagAndBoneManIDropPolicy.QuestBone(
                    raw = objs.goblin_skull,
                    inVinegar = objs.goblin_skull_in_vinegar,
                    polished = objs.polished_goblin_skull,
                    handedIndex = GoblinDrops.GOBLIN_SKULL_INDEX,
                )

        fun resolveTableTwo(context: NpcDropContext): NpcDropRollResult = tableTwo.resolve(context)
    }

    private companion object {
        val TABLE_ONE_IDS: Set<Int> = (3_029..3_036).toSet()
        const val TABLE_TWO_ID: Int = 3_045
    }
}
