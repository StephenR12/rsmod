package org.rsmod.content.skills.farming

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class FarmingCapeTest {
    @Test
    fun GameTestState.`cape respects Kourend travel, teleport blocks and wilderness level`() =
        runGameTest(FarmingCape::class) {
            player.members = true
            val from = CoordGrid(3200, 3200)
            player.teleport(from)
            player.inv[0] = InvObj(objs.farming_skillcape)
            fun use() {
                player.withProtectedAccess { opHeld3(0) }
                advance(6)
            }
            use()
            assertEquals(from, player.coords)
            player.setVarBit(cacheTypes.varbits.getValue(4897), 1)
            player.activityBlocksTeleport = true
            use()
            assertEquals(from, player.coords)
            player.activityBlocksTeleport = false
            player.setVarBit(varbits.teleblock_cycles, 101)
            use()
            assertEquals(from, player.coords)
            player.setVarBit(varbits.teleblock_cycles, 0)
            player.setVarBit(varbits.inside_wilderness, 1)
            val wild = CoordGrid(3200, 3680)
            player.teleport(wild)
            use()
            assertEquals(wild, player.coords)
            player.setVarBit(varbits.inside_wilderness, 0)
            use()
            assertEquals(CoordGrid(1248, 3725), player.coords)
        }
}
