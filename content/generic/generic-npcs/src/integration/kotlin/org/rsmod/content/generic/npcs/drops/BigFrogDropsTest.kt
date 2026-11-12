package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
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
class BigFrogDropsTest {
    @Test
    fun GameTestState.`loaded level ten Big frog has exact cache and override contract`() =
        runBigFrogTest { deps ->
            val type = deps.type
            assertEquals(8_701, type.id)
            assertEquals("Big frog", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(listOf(8, 6, 4, 18, 1, 1), type.combatStats())
            assertEquals(50, type.respawnRate)
            assertEquals(
                constants.elemental_weakness_earth,
                type.param(params.elemental_weakness_type),
            )
            assertEquals(25, type.param(params.elemental_weakness_percent))
            assertEquals(setOf(type.id), deps.registry.registeredTypeIds())
            assertEquals(listOf(7_908, 7_909, 7_910), deps.questBone.lifecycleIds.sorted())
        }

    @Test
    fun GameTestState.`bones quest leg and one in seventy clue have independent exact gates`() =
        runBigFrogTest { deps ->
            player.members = false
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            random.next = 0
            assertDrops(deps, objs.bones, objs.clue_scroll_beginner)

            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, 0)
            random.next = BigFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1
            assertDrops(deps, objs.bones)

            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            random.next = BigFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1
            assertDrops(deps, objs.bones, objs.big_frog_leg)

            random.next = 0
            val drops = drops(deps)
            assertEquals(
                listOf(objs.bones.id, objs.big_frog_leg.id, objs.clue_scroll_beginner.id),
                drops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, drops.last().duration)
        }

    @Test
    fun GameTestState.`all Big frog leg lifecycle forms and hand in state suppress replacement`() =
        runBigFrogTest { deps ->
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            random.next = BigFrogDrops.BEGINNER_CLUE_DENOMINATOR - 1

            listOf(objs.big_frog_leg, objs.big_frog_leg_in_vinegar, objs.polished_big_frog_leg)
                .forEach { obj ->
                    player.inv[0] = InvObj(obj)
                    assertDrops(deps, objs.bones)
                    player.clearInv()

                    val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])
                    bank[0] = InvObj(obj)
                    assertDrops(deps, objs.bones)
                    bank[0] = null
                }

            deps.questBones.markHandedIn(player, deps.questBone)
            assertEquals(
                1 shl BigFrogDrops.BIG_FROG_LEG_INDEX,
                player.vars[varps.rag_and_bone_man_i_handed_mask],
            )
            assertDrops(deps, objs.bones)
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

    private fun GameTestState.runBigFrogTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(BigFrogDrops::class),
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
        val type: UnpackedNpcType
            get() = npcTypes[npcs.big_frog]

        val questBone: RagAndBoneManIDropPolicy.QuestBone
            get() =
                RagAndBoneManIDropPolicy.QuestBone(
                    raw = objs.big_frog_leg,
                    inVinegar = objs.big_frog_leg_in_vinegar,
                    polished = objs.polished_big_frog_leg,
                    handedIndex = BigFrogDrops.BIG_FROG_LEG_INDEX,
                )
    }
}
