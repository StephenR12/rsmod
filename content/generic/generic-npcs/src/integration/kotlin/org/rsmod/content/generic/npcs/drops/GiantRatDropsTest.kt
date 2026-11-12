package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class GiantRatDropsTest {
    @Test
    fun GameTestState.`six loaded Giant rats have exact cache and aggressive override contracts`() =
        runGiantRatTest { deps ->
            val levelThree = setOf(2_856, 2_859, 2_860, 2_861)
            val levelSix = setOf(2_863, 2_864)
            assertEquals(levelThree + levelSix, deps.registry.registeredTypeIds())

            deps.types.forEach { type ->
                assertEquals("Giant rat", type.name)
                assertEquals("Attack", type.op[1])
                assertEquals(2, type.size)
                assertEquals(30, type.respawnRate)
                assertEquals(huntmodes.constant_melee.id, type.huntMode)
                assertEquals(5, type.huntRange)
                when (type.id) {
                    in levelThree -> assertEquals(listOf(2, 3, 2, 5, 1, 1), type.combatStats())
                    in levelSix -> assertEquals(listOf(6, 5, 2, 10, 1, 1), type.combatStats())
                    else -> error("Unexpected loaded Giant rat: ${type.id}")
                }
            }
            assertEquals(listOf(7_824, 7_825, 7_826), deps.questBone.lifecycleIds.sorted())
        }

    @Test
    fun GameTestState
        .`every loaded identity guarantees bones and meat with independent quest and clue gates`() =
        runGiantRatTest { deps ->
            player.members = false
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            deps.types.forEach { type ->
                random.next = 0
                assertDrops(deps, type, objs.bones, objs.raw_rat_meat, objs.clue_scroll_beginner)
            }

            val type = deps.types.first()
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, 0)
            random.next = GiantRatDrops.BEGINNER_CLUE_DENOMINATOR - 1
            assertDrops(deps, type, objs.bones, objs.raw_rat_meat)

            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            random.next = GiantRatDrops.BEGINNER_CLUE_DENOMINATOR - 1
            assertDrops(deps, type, objs.bones, objs.raw_rat_meat, objs.giant_rat_bone)

            random.next = 0
            val drops = drops(deps, type)
            assertEquals(
                listOf(
                    objs.bones.id,
                    objs.raw_rat_meat.id,
                    objs.giant_rat_bone.id,
                    objs.clue_scroll_beginner.id,
                ),
                drops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, drops.last().duration)
        }

    @Test
    fun GameTestState
        .`all Giant rat bone lifecycle forms and hand in state suppress replacement`() =
        runGiantRatTest { deps ->
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            val type = deps.types.first()

            listOf(
                    objs.giant_rat_bone,
                    objs.giant_rat_bone_in_vinegar,
                    objs.polished_giant_rat_bone,
                )
                .forEach { obj ->
                    player.inv[0] = InvObj(obj)
                    random.next = GiantRatDrops.BEGINNER_CLUE_DENOMINATOR - 1
                    assertDrops(deps, type, objs.bones, objs.raw_rat_meat)
                    player.clearInv()

                    val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])
                    bank[0] = InvObj(obj)
                    random.next = GiantRatDrops.BEGINNER_CLUE_DENOMINATOR - 1
                    assertDrops(deps, type, objs.bones, objs.raw_rat_meat)
                    bank[0] = null
                }

            deps.questBones.markHandedIn(player, deps.questBone)
            assertEquals(
                1 shl GiantRatDrops.GIANT_RAT_BONE_INDEX,
                player.vars[varps.rag_and_bone_man_i_handed_mask],
            )
            random.next = GiantRatDrops.BEGINNER_CLUE_DENOMINATOR - 1
            assertDrops(deps, type, objs.bones, objs.raw_rat_meat)
        }

    private fun GameTestScope.assertDrops(
        deps: Dependencies,
        type: UnpackedNpcType,
        vararg expected: ObjType,
    ) {
        assertEquals(expected.map { it.id }, drops(deps, type).map { it.obj.id })
    }

    private fun GameTestScope.drops(deps: Dependencies, type: UnpackedNpcType) =
        checkNotNull(deps.registry[type])
            .roll(
                NpcDropContext(
                    npc = spawnNpc(player.coords, type),
                    hero = player,
                    coords = player.coords,
                    random = deps.random,
                )
            )

    private fun GameTestState.runGiantRatTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(GiantRatDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val questBones: RagAndBoneManIDropPolicy,
        val npcTypes: NpcTypeList,
        val invTypes: InvTypeList,
        val random: GameRandom,
    ) {
        val types: List<UnpackedNpcType>
            get() =
                listOf(
                    npcTypes[npcs.giant_rat],
                    npcTypes[npcs.giant_rat_grey],
                    npcTypes[npcs.giant_rat_grey_2],
                    npcTypes[npcs.giant_rat_grey_3],
                    npcTypes[npcs.giant_rat_level_6],
                    npcTypes[npcs.giant_rat_level_6_2],
                )

        val questBone: RagAndBoneManIDropPolicy.QuestBone
            get() =
                RagAndBoneManIDropPolicy.QuestBone(
                    raw = objs.giant_rat_bone,
                    inVinegar = objs.giant_rat_bone_in_vinegar,
                    polished = objs.polished_giant_rat_bone,
                    handedIndex = GiantRatDrops.GIANT_RAT_BONE_INDEX,
                )
    }
}
