package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
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
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList

@Execution(ExecutionMode.SAME_THREAD)
class RatDropsTest {
    @Test
    fun GameTestState.`loaded regular Rat has exact cache and production override contract`() =
        runRatTest { deps ->
            val type = deps.type
            assertEquals(setOf(2_854), deps.registry.registeredTypeIds())
            assertEquals("Rat", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(1, type.size)
            assertEquals(listOf(1, 1, 1, 2, 1, 1), type.combatStats())
            assertEquals(2, type.respawnRate)
            assertNull(type.huntMode)
            assertEquals(listOf(7_836, 7_837, 7_838), deps.questBone.lifecycleIds.sorted())
            assertEquals(
                VarpLifetime.Perm,
                deps.varpTypes[varps.rag_and_bone_man_ii_handed_mask].scope,
            )
        }

    @Test
    fun GameTestState.`tail and Rat bone obey exact independent quest gates and order`() =
        runRatTest { deps ->
            player.members = false
            assertDrops(deps)

            player.setVarp(varps.hetty, RatDrops.WITCHS_POTION_STARTED)
            player.inv[0] = InvObj(objs.rats_tail)
            assertDrops(deps, objs.rats_tail)
            player.clearInv()

            player.setVarp(varps.hetty, RatDrops.WITCHS_POTION_COMPLETE - 1)
            assertDrops(deps, objs.rats_tail)

            player.setVarp(varps.hetty, RatDrops.WITCHS_POTION_COMPLETE)
            assertDrops(deps)

            player.setVarp(varps.rag_and_bone_man_ii, RagAndBoneManIIDropPolicy.COLLECTION_STATE)
            assertDrops(deps)

            player.members = true
            for (state in
                listOf(
                    0,
                    RagAndBoneManIIDropPolicy.COLLECTION_STATE - 1,
                    RagAndBoneManIIDropPolicy.COLLECTION_STATE + 1,
                    RagAndBoneManIIDropPolicy.QUEST_COMPLETE_STATE,
                )) {
                player.setVarp(varps.rag_and_bone_man_ii, state)
                assertDrops(deps)
            }

            player.setVarp(varps.rag_and_bone_man_ii, RagAndBoneManIIDropPolicy.COLLECTION_STATE)
            random.next = 0
            assertDrops(deps, objs.rat_bone)

            random.next = RagAndBoneManIIDropPolicy.DEFAULT_DENOMINATOR - 1
            assertDrops(deps)

            player.setVarp(varps.hetty, RatDrops.WITCHS_POTION_STARTED)
            random.next = 0
            assertDrops(deps, objs.rats_tail, objs.rat_bone)
        }

    @Test
    fun GameTestState.`every Rat bone lifecycle form and hand in state suppress replacement`() =
        runRatTest { deps ->
            player.members = true
            player.setVarp(varps.rag_and_bone_man_ii, RagAndBoneManIIDropPolicy.COLLECTION_STATE)

            listOf(objs.rat_bone, objs.rat_bone_in_vinegar, objs.polished_rat_bone).forEach { obj ->
                player.inv[0] = InvObj(obj)
                assertDrops(deps)
                player.clearInv()

                val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])
                bank[0] = InvObj(obj)
                assertDrops(deps)
                bank[0] = null
            }

            player.setVarp(varps.rag_and_bone_man_ii_handed_mask, 1 shl 2)
            deps.questBones.markHandedIn(player, deps.questBone)
            assertEquals(
                (1 shl 2) or (1 shl RatDrops.RAT_BONE_INDEX),
                player.vars[varps.rag_and_bone_man_ii_handed_mask],
            )
            assertDrops(deps)
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

    private fun GameTestState.runRatTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(RatDrops::class),
            testBody = body,
        )

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val questBones: RagAndBoneManIIDropPolicy,
        val npcTypes: NpcTypeList,
        val invTypes: InvTypeList,
        val varpTypes: VarpTypeList,
        val random: GameRandom,
    ) {
        val type: UnpackedNpcType
            get() = npcTypes[npcs.rat]

        val questBone: RagAndBoneManIIDropPolicy.QuestBone
            get() =
                RagAndBoneManIIDropPolicy.QuestBone(
                    objs.rat_bone,
                    objs.rat_bone_in_vinegar,
                    objs.polished_rat_bone,
                    RatDrops.RAT_BONE_INDEX,
                )
    }
}
