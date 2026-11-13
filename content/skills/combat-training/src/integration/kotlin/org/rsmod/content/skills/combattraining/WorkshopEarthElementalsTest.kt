package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class WorkshopEarthElementalsTest {
    @Test
    fun GameTestState.`mine wakes the ore elemental and only its defeat supplies elemental ore`() =
        runInjectedGameTest(
            Drops::class,
            null,
            WorkshopEarthElementals::class,
            NpcDeathScript::class,
            NpcHitScript::class,
        ) { deps ->
            val c = CoordGrid(2695, 9870)
            val dormant = spawnNpc(c, npcTypes.getValue(1368))
            player.members = true
            player.observerUUID = player.uuid
            player.placeAt(c.translate(-1, 0))
            player.stats[stats.mining] = 19
            player.opNpc1(dormant)
            advance()
            assertMessageSent("You need a Mining level of 20 to mine this rock.")
            player.stats[stats.mining] = 20
            player.opNpc1(dormant)
            advance()
            assertEquals(1368, dormant.visType.id)
            assertMessageSent("You need a pickaxe that you can use to mine this rock.")
            player.inv[0] = InvObj(objTypes.getValue(1265))
            dormant.queueHit(player, 1, HitType.Melee, 80)
            advance()
            assertEquals(35, dormant.hitpoints)
            player.opNpc1(dormant)
            advance()
            assertEquals(1367, dormant.visType.id)
            dormant.queueHit(player, 1, HitType.Melee, 80)
            advance(8)
            assertEquals(1, deps.ground.findAll(c).count { it.type == 2892 })
            assertEquals(1, deps.ground.findAll(c).count { it.type == 1480 })
            val ordinaryTile = c.translate(2, 0)
            val ordinary = spawnNpc(ordinaryTile, npcTypes.getValue(1366))
            ordinary.queueHit(player, 1, HitType.Melee, 80)
            advance(8)
            assertEquals(1, deps.ground.findAll(ordinaryTile).count { it.type == 1480 })
            assertFalse(deps.ground.findAll(ordinaryTile).any { it.type == 2892 })
        }

    private class Drops @Inject constructor(val ground: ObjRepository)
}
