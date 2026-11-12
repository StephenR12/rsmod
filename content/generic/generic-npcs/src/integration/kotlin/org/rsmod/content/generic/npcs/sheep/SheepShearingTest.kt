package org.rsmod.content.generic.npcs.sheep

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class SheepShearingTest {
    @Test
    fun GameTestState.`cache 233 sheep identities and transformations are exact`() =
        runSheepTest { deps ->
            val ordinary = SheepNpcs.ordinaryUnsheared.map(deps.npcTypes::get)
            val sheared = SheepNpcs.ordinarySheared.map(deps.npcTypes::get)
            val zanaris = SheepNpcs.zanarisUnsheared.map(deps.npcTypes::get)

            assertEquals(
                listOf(2693, 2696, 2699, 2694, 2697, 2786, 2695, 2698, 2787, 2788, 2789),
                ordinary.map { it.id },
            )
            assertEquals(
                listOf(1178, 1301, 1304, 1299, 1302, 1308, 1300, 1303, 1309, 2691, 2692),
                sheared.map { it.id },
            )
            assertEquals(listOf(5843, 5844), zanaris.map { it.id })
            assertTrue(ordinary.all { it.op[0] == "Shear" })
            assertTrue(zanaris.all { it.op[0] == "Talk-to" && "Shear" !in it.op })
            assertTrue((ordinary + zanaris).all { it.contentGroup == content.sheep.id })
            assertTrue(sheared.all { it.contentGroup == content.sheared_sheep.id })
            assertEquals(sheared.map { it.id }, ordinary.map { it.param(params.next_npc_stage).id })
            assertEquals(
                listOf(SheepNpcs.sheared_5845.id, SheepNpcs.sheared_5845.id),
                zanaris.map { it.param(params.next_npc_stage).id },
            )
            assertEquals(5846, SheepNpcs.sheared_5846.id)
            assertEquals(1735, objs.shears.id)
            assertEquals(1737, objs.wool.id)
        }

    @Test
    fun GameTestState.`ordinary sheep op requires shears then always gives wool`() =
        runSheepTest { deps ->
            val npc = spawnNpc(player.coords, deps.npcTypes[SheepNpcs.unsheared_2693])

            player.opNpc1(npc)
            advance()
            assertMessageSent("You need a set of shears to do this.")
            assertEquals(SheepNpcs.unsheared_2693.id, npc.visType.id)
            assertEquals(0, player.count(objs.wool))

            client.clear()
            player.inv[0] = InvObj(objs.shears)
            player.opNpc1(npc)
            advanceUntil(predicate = { npc.visType.id == SheepNpcs.sheared_1178.id })
            assertMessageSent("You get some wool.")
            assertEquals(1, player.count(objs.wool))
            assertEquals(SheepNpcs.sheared_1178.id, npc.visType.id)
            assertEquals(
                Sheep.ORDINARY_REGROW_CYCLES,
                npc.lifecycleChangeCycle - npc.currentMapClock,
            )

            advance(Sheep.ORDINARY_REGROW_CYCLES - 2)
            assertEquals(SheepNpcs.sheared_1178.id, npc.visType.id)
            advance()
            assertEquals(SheepNpcs.unsheared_2693.id, npc.visType.id)
        }

    @Test
    fun GameTestState.`Zanaris Talk-to cannot shear and item use preserves legacy failure`() =
        runSheepTest { deps ->
            val npc = spawnNpc(player.coords, deps.npcTypes[SheepNpcs.unsheared_5843])
            player.inv[0] = InvObj(objs.shears)

            random.next = 0
            useOnNpc(deps, npc, objs.shears)
            assertMessageSent(Sheep.ZANARIS_FAILURE_MESSAGE)
            assertEquals(SheepNpcs.unsheared_5843.id, npc.visType.id)
            assertEquals(0, player.count(objs.wool))

            client.clear()
            player.opNpc1(npc)
            advance()
            assertEquals(SheepNpcs.unsheared_5843.id, npc.visType.id)
            assertEquals(0, player.count(objs.wool))
        }

    @Test
    fun GameTestState.`both Zanaris colours shear to dark grey and regrow after 100 ticks`() =
        runSheepTest { deps ->
            for (type in SheepNpcs.zanarisUnsheared) {
                client.clear()
                player.clearInv()
                val npc = spawnNpc(player.coords, deps.npcTypes[type])
                random.next = 1

                useOnNpc(deps, npc, objs.shears)
                advanceUntil(predicate = { npc.visType.id == SheepNpcs.sheared_5845.id })
                assertMessageSent("You get some wool.")
                assertEquals(1, player.count(objs.wool))
                assertEquals(SheepNpcs.sheared_5845.id, npc.visType.id)
                assertEquals(
                    Sheep.ZANARIS_REGROW_CYCLES,
                    npc.lifecycleChangeCycle - npc.currentMapClock,
                )

                advance(Sheep.ZANARIS_REGROW_CYCLES - 2)
                assertEquals(SheepNpcs.sheared_5845.id, npc.visType.id)
                advance()
                assertEquals(type.id, npc.visType.id)
            }
        }

    @Test
    fun GameTestState.`full inventory drops wool at the sheep`() = runSheepTest { deps ->
        val npc = spawnNpc(player.coords, deps.npcTypes[SheepNpcs.unsheared_2693])
        player.observerUUID = player.uuid
        player.fillInv()
        player.inv[0] = InvObj(objs.shears)

        player.opNpc1(npc)
        advanceUntil(predicate = { npc.visType.id == SheepNpcs.sheared_1178.id })

        assertEquals(0, player.count(objs.wool))
        val ground = deps.objRepo.findAll(player.coords).filter { it.isVisibleTo(player) }
        assertTrue(ground.any { it.type == objs.wool.id && it.count == 1 })
        assertFalse(ground.any { it.type == objs.shears.id })
    }

    private fun GameTestState.runSheepTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(Sheep::class), testBody = body)

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

    private data class Dependencies
    @Inject
    constructor(
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val objTypes: ObjTypeList,
        val interactions: NpcUInteractions,
    )
}
