package org.rsmod.content.skills.prayer.remains

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class TeomatPrayerTest {
    @Test
    fun GameTestState.`bless a mixed inventory then chisel bones with a real tool`() =
        runInjectedGameTest(HeldInteractions::class, null, TeomatPrayer::class) { held ->
            player.members = true
            player.stats[stats.prayer] = 30
            player.inv[0] = InvObj(objs.dragon_bones)
            player.inv[1] = InvObj(objs.jug_of_wine)
            val altar = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(52799))
            player.opLoc1(altar)
            advance()
            assertEquals(29356, player.inv[0]?.id)
            assertEquals(29386, player.inv[1]?.id)
            val xp = player.statMap.getFineXP(stats.crafting)
            player.withProtectedAccess { held.interact(this, inv, 0, HeldOp.Op1) }
            assertEquals(29356, player.inv[0]?.id)
            player.inv[3] = InvObj(objs.chisel)
            player.withProtectedAccess { held.interact(this, inv, 0, HeldOp.Op1) }
            assertEquals(58, player.count(objTypes.getValue(29381)))
            assertEquals(xp + 50, player.statMap.getFineXP(stats.crafting))
        }

    @Test
    fun GameTestState
        .`sunfire wine pays for four sacrifices and stops immediately at one Prayer point`() =
        runGameTest(TeomatPrayer::class) {
            player.members = true
            player.stats[stats.prayer] = 30
            player.setCurrentLevel(stats.prayer, 9)
            player.inv[0] = InvObj(objTypes.getValue(29384))
            player.inv[1] = InvObj(objTypes.getValue(29381), 450)
            val bowl = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(53018))
            val xp = player.statMap.getFineXP(stats.prayer)
            player.opLoc1(bowl)
            advance(18)
            assertEquals(50, player.count(objTypes.getValue(29381)))
            assertEquals(1, player.count(objs.jug_empty))
            assertEquals(1, player.statMap.getCurrentLevel(stats.prayer).toInt())
            assertEquals(xp + 24000, player.statMap.getFineXP(stats.prayer))
        }
}
