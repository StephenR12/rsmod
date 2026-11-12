package org.rsmod.api.game.process.npc

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.factory.npcTypeFactory

class NpcDropSpawnTest {
    @Test
    fun GameTestState.`unregistered npc death table does not fabricate bones`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val npc = spawnNpc(player.coords, unregisteredNpc)
            npc.heroPoints(player, 1)
            val dropCoords = player.coords.translate(xOffset = 20, zOffset = 20)
            val before = deps.objRepo.findAll(dropCoords).count()

            deps.death.spawnDrops(npc, dropCoords)

            assertEquals(before, deps.objRepo.findAll(dropCoords).count())
        }

    @Test
    fun GameTestState.`registered table spawns private owned drop through repository`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            player.observerUUID = player.uuid
            val npc = spawnNpc(player.coords, registeredNpc)
            npc.heroPoints(player, 1)
            deps.registry.register(npc.type, NpcDropTable(item(objs.bones)))
            val dropCoords = player.coords.translate(xOffset = 20, zOffset = 20)

            deps.death.spawnDrops(npc, dropCoords)

            val drop = deps.objRepo.findAll(dropCoords).single { it.type == objs.bones.id }
            assertEquals(objs.bones.id, drop.type)
            assertEquals(1, drop.count)
            assertTrue(drop.isPrivate)
            assertTrue(drop.isVisibleTo(player))
            assertTrue(drop.isOriginalOwner(player))
        }

    private class Dependencies
    @Inject
    constructor(
        val death: NpcDeath,
        val registry: NpcDropTableRegistry,
        val objRepo: ObjRepository,
    )

    private companion object {
        private val unregisteredNpc = npcTypeFactory.create(id = 62_001) { name = "No table" }
        private val registeredNpc = npcTypeFactory.create(id = 62_002) { name = "Registered table" }
    }
}
