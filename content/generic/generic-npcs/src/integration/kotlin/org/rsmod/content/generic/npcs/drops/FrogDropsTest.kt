package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType

@Execution(ExecutionMode.SAME_THREAD)
class FrogDropsTest {
    @Test
    fun GameTestState.`dated level five frog has exact production contract`() =
        runFrogTest { deps ->
            val type = deps.frogType
            assertEquals(8_702, type.id)
            assertEquals("Frog", type.name)
            assertEquals("Attack", type.op[1])
            assertEquals(listOf(5, 4, 3, 8, 1, 1), type.combatStats())
            assertEquals(50, type.respawnRate)
            assertEquals(
                constants.elemental_weakness_earth,
                type.param(params.elemental_weakness_type),
            )
            assertEquals(40, type.param(params.elemental_weakness_percent))
            assertEquals(setOf(8_702), deps.registry.registeredTypeIds())
            assertEquals(526, objs.bones.id)
            assertEquals(23_182, objs.clue_scroll_beginner.id)
        }

    @Test
    fun GameTestState.`bones and one in ninety beginner clue preserve order and lifetime`() =
        runFrogTest { deps ->
            val table = checkNotNull(deps.registry[deps.frogType])

            random.next = 89
            assertEquals(listOf(objs.bones.id), table.roll(context(deps)).map { it.obj.id })

            random.next = 0
            val drops = table.roll(context(deps))
            assertEquals(
                listOf(objs.bones.id, objs.clue_scroll_beginner.id),
                drops.map { it.obj.id },
            )
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, drops.last().duration)

            val npc = spawnNpc(player.coords, deps.frogType)
            player.observerUUID = player.uuid
            npc.heroPoints(player, 1)
            deps.death.spawnDrops(npc)
            assertEquals(
                setOf(objs.bones.id, objs.clue_scroll_beginner.id),
                deps.objRepo.findAll(npc.coords).map { it.type }.toSet(),
            )
            advance(200)
            assertEquals(
                listOf(objs.clue_scroll_beginner.id),
                deps.objRepo.findAll(npc.coords).map { it.type }.toList(),
            )
        }

    @Test
    fun GameTestState.`beginner clue uses shared ownership box and capacity policy`() =
        runFrogTest { deps ->
            val table = checkNotNull(deps.registry[deps.frogType])

            player.inv[0] = InvObj(objs.clue_scroll_beginner)
            random.next = 42
            var result = table.resolve(context(deps))
            assertEquals(listOf(objs.bones.id), result.drops.map { it.obj.id })
            assertEquals(42, random.next)
            assertEquals(42, deps.random.of(90))

            player.clearInv()
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            random.next = 0
            result = table.resolve(context(deps))
            assertEquals(
                listOf(objs.bones.id, objs.scroll_box_beginner.id),
                result.drops.map { it.obj.id },
            )
            assertEquals(ClueInventory.SCROLL_BOX_GROUND_DURATION, result.drops.last().duration)

            player.inv[0] = InvObj(objs.scroll_box_beginner, 2)
            random.next = 0
            result = table.resolve(context(deps))
            assertEquals(listOf(objs.bones.id), result.drops.map { it.obj.id })
            assertEquals(
                listOf(
                    "You have a sneaking suspicion that you would have received a " +
                        "beginner scroll box."
                ),
                result.messages,
            )
        }

    private fun GameTestState.runFrogTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(FrogDrops::class),
            testBody = body,
        )

    private fun GameTestScope.context(deps: Dependencies): NpcDropContext {
        val npc = spawnNpc(player.coords, deps.frogType)
        return NpcDropContext(npc, player, npc.coords, deps.random)
    }

    private fun UnpackedNpcType.combatStats(): List<Int> =
        listOf(attack, strength, defence, hitpoints, ranged, magic)

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val death: NpcDeath,
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val random: GameRandom,
    ) {
        val frogType: UnpackedNpcType
            get() = npcTypes[npcs.frog]
    }
}
