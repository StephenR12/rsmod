package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjTypeList

class ChickenDropsTest {
    @Test
    fun GameTestState.`cache 233 chicken family and weighted main table are exact`() =
        runChickenTest { deps ->
            val types = deps.chickenTypes()
            val expectedIds =
                listOf(1_173, 1_174, 2_804, 2_805, 2_806, 3_661, 3_662, 9_488) + (10_494..10_497)

            assertEquals(expectedIds, types.map { it.id })
            assertTrue(types.all { it.name == "Chicken" && it.op[1] == "Attack" })
            assertEquals(expectedIds.toSet(), deps.registry.registeredTypeIds())
            val table = checkNotNull(deps.registry[types.first()])
            types.forEach { assertSame(table, deps.registry[it]) }
            assertFalse(deps.registry.contains(deps.npcTypes.types.getValue(10_498)))
            assertFalse(deps.registry.contains(deps.npcTypes.types.getValue(10_499)))

            assertEquals(526, objs.bones.id)
            assertEquals(2_138, objs.raw_chicken.id)
            assertEquals(314, objs.feather.id)
            assertTrue(deps.objTypes[objs.feather].isStackable)

            assertEquals(
                listOf(526 to 1, 2_138 to 1, 314 to 5),
                roll(deps, table, types.first(), mainRoll = 0).map { it.obj.id to it.obj.count },
            )
            assertEquals(
                listOf(526 to 1, 2_138 to 1, 314 to 15),
                roll(deps, table, types.first(), mainRoll = 64).map { it.obj.id to it.obj.count },
            )
            assertEquals(
                listOf(526 to 1, 2_138 to 1),
                roll(deps, table, types.first(), mainRoll = 96).map { it.obj.id to it.obj.count },
            )
        }

    @Test
    fun GameTestState.`medium clue step key requires members and the exact carried clue`() =
        runChickenTest { deps ->
            val type = deps.chickenTypes().first()
            val table = checkNotNull(deps.registry[type])
            assertEquals(2_837, objs.chicken_clue_scroll_medium.id)
            assertEquals(2_838, objs.chicken_clue_key_medium.id)
            assertEquals(
                "Clue scroll (medium)",
                deps.objTypes[objs.chicken_clue_scroll_medium].name,
            )
            assertEquals("Key (medium)", deps.objTypes[objs.chicken_clue_key_medium].name)

            player.members = true
            player.inv[0] = InvObj(objs.chicken_clue_scroll_medium)
            assertEquals(
                listOf(526, 2_138, 2_838),
                roll(deps, table, type, mainRoll = 96).map { it.obj.id },
            )

            player.members = false
            assertEquals(
                listOf(526, 2_138),
                roll(deps, table, type, mainRoll = 96).map { it.obj.id },
            )
        }

    @Test
    fun GameTestState.`beginner clue roll resolves ownership quest boxes and capacity`() =
        runChickenTest { deps ->
            val type = deps.chickenTypes().first()
            val table = checkNotNull(deps.registry[type])
            assertEquals(23_182, objs.clue_scroll_beginner.id)

            var resolution = resolve(deps, table, type, mainRoll = 96, clueRoll = 0)
            var reward = resolution.drops.single { it.obj.id == objs.clue_scroll_beginner.id }
            assertEquals(ClueInventory.CLUE_SCROLL_GROUND_DURATION, reward.duration)
            assertTrue(resolution.messages.isEmpty())

            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            resolution = resolve(deps, table, type, mainRoll = 96, clueRoll = 0)
            reward = resolution.drops.single { it.obj.id == objs.scroll_box_beginner.id }
            assertEquals(ClueInventory.SCROLL_BOX_GROUND_DURATION, reward.duration)

            player.inv[0] = InvObj(objs.scroll_box_beginner, 2)
            resolution = resolve(deps, table, type, mainRoll = 96, clueRoll = 0)
            assertEquals(listOf(526, 2_138), resolution.drops.map { it.obj.id })
            assertEquals(
                listOf(
                    "You have a sneaking suspicion that you would have received a " +
                        "beginner scroll box."
                ),
                resolution.messages,
            )

            player.inv[0] = InvObj(objs.clue_scroll_beginner)
            player.setVarBit(varbits.x_marks_the_spot, 0)
            random.next = 96 // Weighted nothing.
            random.then = 42 // Ownership must skip the clue roll.
            resolution = table.resolve(context(deps, type))
            assertEquals(listOf(526, 2_138), resolution.drops.map { it.obj.id })
            assertEquals(42, random.next)
        }

    @Test
    fun GameTestState.`npc death path preserves one hour clue ground lifetime`() =
        runChickenTest { deps ->
            val type = deps.chickenTypes().first()
            val npc = spawnNpc(player.coords, type)
            player.observerUUID = player.uuid
            npc.heroPoints(player, 1)
            random.next = 96 // Weighted nothing.
            random.then = 0 // Beginner clue success.

            deps.death.spawnDrops(npc)
            assertEquals(
                setOf(
                    objs.bones.id,
                    objs.raw_chicken.id,
                    objs.feather.id,
                    objs.clue_scroll_beginner.id,
                ),
                deps.objRepo.findAll(npc.coords).map { it.type }.toSet(),
            )

            advance(200)
            assertEquals(
                listOf(objs.clue_scroll_beginner.id),
                deps.objRepo.findAll(npc.coords).map { it.type }.toList(),
            )
            advance(ClueInventory.CLUE_SCROLL_GROUND_DURATION - 200)
            assertTrue(deps.objRepo.findAll(npc.coords).none())
        }

    @Test
    fun GameTestState.`npc death path delivers the exact capped scroll box message`() =
        runChickenTest { deps ->
            val type = deps.chickenTypes().first()
            val npc = spawnNpc(player.coords, type)
            player.observerUUID = player.uuid
            player.setVarBit(varbits.x_marks_the_spot, ClueInventory.X_MARKS_COMPLETE)
            player.inv[0] = InvObj(objs.scroll_box_beginner, 2)
            npc.heroPoints(player, 1)
            random.next = 96 // Weighted nothing.
            random.then = 0 // Successful clue roll blocked by capacity.

            deps.death.spawnDrops(npc)

            assertEquals(
                setOf(objs.bones.id, objs.raw_chicken.id, objs.feather.id),
                deps.objRepo.findAll(npc.coords).map { it.type }.toSet(),
            )
            assertMessageSent(
                "You have a sneaking suspicion that you would have received a " +
                    "beginner scroll box."
            )
        }

    private fun GameTestState.runChickenTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(ChickenDrops::class),
            testBody = body,
        )

    private fun GameTestScope.context(deps: Dependencies, type: UnpackedNpcType): NpcDropContext {
        val npc = spawnNpc(player.coords, type)
        return NpcDropContext(npc, player, npc.coords, deps.random)
    }

    private fun GameTestScope.roll(
        deps: Dependencies,
        table: NpcDropTable,
        type: UnpackedNpcType,
        mainRoll: Int,
        clueRoll: Int = 299,
    ) = resolve(deps, table, type, mainRoll, clueRoll).drops

    private fun GameTestScope.resolve(
        deps: Dependencies,
        table: NpcDropTable,
        type: UnpackedNpcType,
        mainRoll: Int,
        clueRoll: Int,
    ) = run {
        random.next = mainRoll
        random.then = clueRoll
        table.resolve(context(deps, type))
    }

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val death: NpcDeath,
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val objTypes: ObjTypeList,
        val random: GameRandom,
    ) {
        fun chickenTypes(): List<UnpackedNpcType> =
            listOf(
                npcTypes[npcs.chicken],
                npcTypes[npcs.chicken_brown],
                npcTypes[npcs.farm_chicken_brown],
                npcTypes[npcs.farm_chicken_dark_brown],
                npcTypes[npcs.farm_chicken_tan],
                npcTypes[npcs.misc_chicken],
                npcTypes[npcs.misc_chicken_brown],
                npcTypes[npcs.tutorial_chicken],
                npcTypes[npcs.farm_chicken_brown_outdoors],
                npcTypes[npcs.farm_chicken_dark_brown_outdoors],
                npcTypes[npcs.farm_chicken_tan_outdoors],
                npcTypes[npcs.farm_chicken_brown_indoors],
            )
    }
}
