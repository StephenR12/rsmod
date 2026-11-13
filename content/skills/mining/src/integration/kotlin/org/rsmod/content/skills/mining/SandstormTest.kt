package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class SandstormTest {
    @Test
    fun GameTestState.grindDepositThenClaimOnlyWhatThePlayerCanPayFor() =
        runGameTest(Sandstorm::class) {
            player.members = true
            val mill = placeMapLoc(CoordGrid(3150, 2907), locTypes.getValue(26199))
            val drew = spawnNpc(CoordGrid(3153, 2910), npcTypes.getValue(6771))
            for ((slot, stone) in QuarryObjs.sandstone.withIndex()) player.inv[slot] = InvObj(stone)
            player.telejump(mill.coords.translate(-1, 0))
            player.opLoc1(mill)
            advance(2)
            assertEquals(15, player.vars[cacheTypes.varps.getValue(65811)])
            val buckets = objTypes.cert(objTypes[objs.bucket_empty])
            player.inv[0] = InvObj(buckets, 12)
            player.inv[1] = InvObj(objs.bucket_empty)
            player.telejump(drew.coords.translate(-1, 0))
            player.opNpc5(drew)
            advance(2)
            assertEquals(13, player.vars[cacheTypes.varps.getValue(65810)])
            player.fillInv(with = InvObj(objs.logs))
            player.inv[0] = InvObj(objs.coins, 201)
            player.opNpc3(drew)
            advance(2)
            assertEquals(201, player.count(objs.coins))
            assertEquals(13, player.vars[cacheTypes.varps.getValue(65810)])
            player.inv[1] = null
            player.opNpc3(drew)
            advance(2)
            assertEquals(4, player.count(objTypes.cert(objTypes[objs.bucket_of_sand])))
            assertEquals(1, player.count(objs.coins))
            assertEquals(9, player.vars[cacheTypes.varps.getValue(65810)])
            assertEquals(11, player.vars[cacheTypes.varps.getValue(65811)])
        }

    @Test
    fun GameTestState.fullGrinderPreservesOversizedStonesAndExcessBuckets() =
        runGameTest(Sandstorm::class) {
            player.members = true
            player.setVarp(cacheTypes.varps.getValue(65811), 24999)
            player.setVarp(cacheTypes.varps.getValue(65810), 24998)
            val mill = placeMapLoc(CoordGrid(3150, 2907), locTypes.getValue(26199))
            player.inv[0] = InvObj(QuarryObjs.sandstone.last())
            player.inv[1] = InvObj(QuarryObjs.sandstone.first())
            player.telejump(mill.coords.translate(-1, 0))
            player.opLoc1(mill)
            advance(2)
            assertEquals(25000, player.vars[cacheTypes.varps.getValue(65811)])
            assertEquals(1, player.count(QuarryObjs.sandstone.last()))
            val drew = spawnNpc(CoordGrid(3153, 2910), npcTypes.getValue(6771))
            val noted = objTypes.cert(objTypes[objs.bucket_empty])
            player.inv[2] = InvObj(noted, 3)
            player.telejump(drew.coords.translate(-1, 0))
            player.opNpc5(drew)
            advance(2)
            assertEquals(25000, player.vars[cacheTypes.varps.getValue(65810)])
            assertEquals(1, player.count(noted))
        }
}
