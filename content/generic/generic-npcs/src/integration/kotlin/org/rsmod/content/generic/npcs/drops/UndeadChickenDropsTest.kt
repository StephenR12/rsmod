package org.rsmod.content.generic.npcs.drops

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropContext
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList

class UndeadChickenDropsTest {
    @Test
    fun GameTestState
        .`cache 233 undead chicken table is complete and routes through death drops`() =
        runInjectedGameTest(Dependencies::class, scripts = arrayOf(UndeadChickenDrops::class)) {
            deps ->
            val type = deps.npcTypes[npcs.undead_chicken]
            val table = checkNotNull(deps.registry[npcs.undead_chicken])
            val npc = spawnNpc(player.coords, type)
            val context = NpcDropContext(npc, player, npc.coords, deps.testRandom)

            assertEquals(2_993, type.id)
            assertEquals("Undead chicken", type.name)
            assertEquals(526, objs.bones.id)
            assertEquals(314, objs.feather.id)
            assertEquals(4_289, objs.raw_chicken_undead.id)
            assertEquals("Raw chicken", deps.objTypes[objs.raw_chicken_undead].name)
            assertTrue(deps.objTypes[objs.feather].isStackable)

            random.next = 0
            assertEquals(
                listOf(526 to 1, 4_289 to 1, 314 to 5),
                table.roll(context).map { it.obj.id to it.obj.count },
            )
            random.next = 64
            assertEquals(
                listOf(526 to 1, 4_289 to 1, 314 to 15),
                table.roll(context).map { it.obj.id to it.obj.count },
            )
            random.next = 96
            assertEquals(
                listOf(526 to 1, 4_289 to 1),
                table.roll(context).map { it.obj.id to it.obj.count },
            )

            player.observerUUID = player.uuid
            npc.heroPoints(player, 1)
            val dropCoords = player.coords.translate(xOffset = 18, zOffset = 18)
            random.next = 0
            deps.death.spawnDrops(npc, dropCoords)
            val ground =
                deps.objRepo
                    .findAll(dropCoords)
                    .filter { it.isVisibleTo(player) }
                    .associate { it.type to it.count }
            assertEquals(mapOf(526 to 1, 4_289 to 1, 314 to 5), ground)
            assertTrue(deps.objRepo.findAll(dropCoords).all { it.isOriginalOwner(player) })
        }

    private data class Dependencies
    @Inject
    constructor(
        val registry: NpcDropTableRegistry,
        val death: NpcDeath,
        val objRepo: ObjRepository,
        val npcTypes: NpcTypeList,
        val objTypes: ObjTypeList,
        val testRandom: GameRandom,
    )
}
