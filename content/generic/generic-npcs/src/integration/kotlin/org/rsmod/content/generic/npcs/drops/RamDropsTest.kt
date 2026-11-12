package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.generic.npcs.ram.RAM_TYPES
import org.rsmod.content.generic.npcs.ram.Ram
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.inv.UnpackedInvType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varp.VarpLifetime
import org.rsmod.game.type.varp.VarpTypeList

@Execution(ExecutionMode.SAME_THREAD)
class RamDropsTest {
    @Test
    fun GameTestState.`all five dated Ram identities have exact cache and override contracts`() =
        runRamTest { deps ->
            val types = deps.ramTypes()
            assertEquals(listOf(1_262, 1_263, 1_264, 1_265, 1_261), types.map { it.id })
            assertTrue(types.all { it.name == "Ram" && it.op[1] == "Attack" })
            assertTrue(types.all { it.combatStats() == listOf(1, 1, 1, 8, 1, 1) })
            assertTrue(types.all { it.respawnRate == 45 })
            assertTrue(
                types.all {
                    it.param(params.elemental_weakness_type) == constants.elemental_weakness_fire
                }
            )
            assertTrue(types.all { it.param(params.elemental_weakness_percent) == 20 })
            assertEquals(
                VarpLifetime.Perm,
                deps.varpTypes[varps.rag_and_bone_man_i_handed_mask].scope,
            )
            assertEquals(types.map { it.id }.toSet(), deps.registry.registeredTypeIds())
            val table = checkNotNull(deps.registry[types.first()])
            types.forEach { assertSame(table, deps.registry[it]) }
        }

    @Test
    fun GameTestState.`bones are unconditional while quest skull requires active members state`() =
        runRamTest { deps ->
            val type = deps.ramTypes().first()

            player.members = false
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            assertDrops(deps, type, objs.bones)

            player.members = true
            for (state in
                listOf(
                    0,
                    RagAndBoneManIDropPolicy.COLLECTION_STATE - 1,
                    RagAndBoneManIDropPolicy.COLLECTION_STATE + 1,
                    RagAndBoneManIDropPolicy.QUEST_COMPLETE_STATE,
                )) {
                player.setVarp(varps.rag_and_bone_man_i, state)
                assertDrops(deps, type, objs.bones)
            }

            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            assertDrops(deps, type, objs.bones, objs.ram_skull)
        }

    @Test
    fun GameTestState
        .`every skull lifecycle object and persistent hand in state suppress duplicates`() =
        runRamTest { deps ->
            val type = deps.ramTypes().first()
            player.members = true
            player.setVarp(varps.rag_and_bone_man_i, RagAndBoneManIDropPolicy.COLLECTION_STATE)
            val lifecycle =
                listOf(objs.ram_skull, objs.ram_skull_in_vinegar, objs.polished_ram_skull)

            lifecycle.forEach { obj ->
                player.inv[0] = InvObj(obj)
                assertDrops(deps, type, objs.bones)
                player.clearInv()

                val bank = player.invMap.getOrPut(deps.bankType)
                bank[0] = InvObj(obj)
                assertDrops(deps, type, objs.bones)
                bank[0] = null
            }

            val questBone = deps.ramSkull()
            deps.questBones.markHandedIn(player, questBone)
            assertEquals(
                1 shl RamDrops.RAM_SKULL_INDEX,
                player.vars[varps.rag_and_bone_man_i_handed_mask],
            )
            assertDrops(deps, type, objs.bones)
        }

    @Test
    fun GameTestState.`shears use has exact refusal and preserves both objects`() =
        runRamTest { deps ->
            for (type in deps.ramTypes()) {
                client.clear()
                player.clearInv()
                val npc = spawnNpc(player.coords, type)
                useOnNpc(deps, npc, objs.shears)
                assertMessageSent("That one looks a little too violent to shear...")
                assertEquals(objs.shears.id, player.inv[0]?.id)
                assertEquals(type.id, npc.type.id)
            }
        }

    private fun GameTestScope.assertDrops(
        deps: Dependencies,
        type: UnpackedNpcType,
        vararg expected: ObjType,
    ) {
        val npc = spawnNpc(player.coords, type)
        val context = NpcDropContext(npc, player, npc.coords, deps.random)
        val drops = checkNotNull(deps.registry[type]).roll(context)
        assertEquals(expected.map { it.id }, drops.map { it.obj.id })
    }

    private fun GameTestScope.useOnNpc(deps: Dependencies, npc: Npc, obj: ObjType) {
        player.inv[0] = InvObj(obj)
        player.withProtectedAccess {
            deps.interactions.interactOp(
                this,
                npc,
                player.inv,
                invSlot = 0,
                npc.type,
                deps.objTypes[obj],
            )
        }
    }

    private fun GameTestState.runRamTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(RamDrops::class, Ram::class),
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
        val objTypes: ObjTypeList,
        val invTypes: InvTypeList,
        val varpTypes: VarpTypeList,
        val interactions: NpcUInteractions,
        val random: GameRandom,
    ) {
        val bankType: UnpackedInvType
            get() = invTypes[invs.bank]

        fun ramTypes(): List<UnpackedNpcType> = RAM_TYPES.map { npcTypes[it] }

        fun ramSkull(): RagAndBoneManIDropPolicy.QuestBone =
            RagAndBoneManIDropPolicy.QuestBone(
                objs.ram_skull,
                objs.ram_skull_in_vinegar,
                objs.polished_ram_skull,
                RamDrops.RAM_SKULL_INDEX,
            )
    }
}
