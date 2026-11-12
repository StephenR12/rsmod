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
import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType

@Execution(ExecutionMode.SAME_THREAD)
class UnicornDropsTest {
    @Test
    fun GameTestState.`loaded Unicorn has exact cache and dated override contract`() =
        runUnicornTest { deps ->
            val type = deps.type
            assertEquals(2_837, type.id)
            assertEquals("Unicorn", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(2, type.size)
            assertEquals(listOf(11, 13, 13, 19, 1, 1), type.combatStats())
            assertEquals(90, type.respawnRate)
            assertEquals(setOf(type.id), deps.registry.registeredTypeIds())
            assertEquals(listOf(7_821, 7_822, 7_823), deps.questBone.lifecycleIds.sorted())
            assertEquals(13_465, objs.ensouled_unicorn_head_initial.id)
            assertEquals(13_466, objs.ensouled_unicorn_head.id)
        }

    @Test
    fun GameTestState.`horn quest bone and one in thirty five head have exact independent gates`() =
        runUnicornTest { deps ->
            player.members = false
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            assertDrops(deps, objs.bones)

            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, 0)
            random.next = UnicornDrops.ENSOULED_HEAD_DENOMINATOR - 1
            assertDrops(deps, objs.bones, objs.unicorn_horn)

            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            random.next = UnicornDrops.ENSOULED_HEAD_DENOMINATOR - 1
            assertDrops(deps, objs.bones, objs.unicorn_horn, objs.unicorn_bone)

            random.next = 0
            val drops = drops(deps)
            assertEquals(
                listOf(
                    objs.bones.id,
                    objs.unicorn_horn.id,
                    objs.unicorn_bone.id,
                    objs.ensouled_unicorn_head_initial.id,
                ),
                drops.map { it.obj.id },
            )
            assertEquals(player.coords, FreshDropOrigin.unpack(drops.last().obj.vars))
            assertNull(FreshDropOrigin.unpack(drops.first().obj.vars))
        }

    @Test
    fun GameTestState.`all Unicorn bone lifecycle forms and hand in state suppress replacement`() =
        runUnicornTest { deps ->
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            random.next = UnicornDrops.ENSOULED_HEAD_DENOMINATOR - 1

            listOf(objs.unicorn_bone, objs.unicorn_bone_in_vinegar, objs.polished_unicorn_bone)
                .forEach { obj ->
                    player.inv[0] = InvObj(obj)
                    assertDrops(deps, objs.bones, objs.unicorn_horn)
                    player.clearInv()

                    val bank = player.invMap.getOrPut(deps.invTypes[invs.bank])
                    bank[0] = InvObj(obj)
                    assertDrops(deps, objs.bones, objs.unicorn_horn)
                    bank[0] = null
                }

            deps.questBones.markHandedIn(player, deps.questBone)
            assertEquals(
                1 shl UnicornDrops.UNICORN_BONE_INDEX,
                player.vars[varps.rag_and_bone_man_i_handed_mask],
            )
            assertDrops(deps, objs.bones, objs.unicorn_horn)
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

    private fun GameTestState.runUnicornTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(UnicornDrops::class),
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
            get() = npcTypes[npcs.unicorn]

        val questBone: RagAndBoneManIDropPolicy.QuestBone
            get() =
                RagAndBoneManIDropPolicy.QuestBone(
                    raw = objs.unicorn_bone,
                    inVinegar = objs.unicorn_bone_in_vinegar,
                    polished = objs.polished_unicorn_bone,
                    handedIndex = UnicornDrops.UNICORN_BONE_INDEX,
                )
    }
}
