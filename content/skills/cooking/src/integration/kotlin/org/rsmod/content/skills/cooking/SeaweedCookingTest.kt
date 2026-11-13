package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SeaweedCookingTest {
    @Test
    fun GameTestState.`all three ordinary weeds produce one ash without experience even with a full inventory`() = runSeaweedTest { deps ->
        player.members = true
        val source = source()
        assertEquals(21504, SeaweedInputs.giant.id)
        assertEquals(403, SeaweedInputs.edible.id)
        for (raw in SeaweedInputs.all.filter { it != SeaweedInputs.giant }) {
            player.fillInv()
            player.inv[0] = InvObj(raw)
            val xp = player.statMap.getFineXP(stats.cooking)
            player.withProtectedAccess { deps.cooking.cook(this, source, raw, 1) }
            assertEquals(0, player.count(objs.soda_ash))
            advance()
            assertEquals(1, player.count(objs.soda_ash))
            assertEquals(0, player.count(raw))
            assertEquals(xp, player.statMap.getFineXP(stats.cooking))
        }
    }

    @Test
    fun GameTestState.`giant seaweed has one ash failure and six ash success with level fifty one immunity`() = runSeaweedTest { deps ->
        player.members = true
        val source = source()
        player.stats[stats.cooking] = 1
        player.inv[0] = InvObj(SeaweedInputs.giant)
        random.next = 255
        player.withProtectedAccess { deps.cooking.cook(this, source, SeaweedInputs.giant, 1) }
        advance()
        assertEquals(1, player.count(objs.soda_ash))
        player.clearInv()
        player.stats[stats.cooking] = 51
        player.inv[0] = InvObj(SeaweedInputs.giant)
        random.next = 255
        player.withProtectedAccess { deps.cooking.cook(this, source, SeaweedInputs.giant, 1) }
        advance()
        assertEquals(6, player.count(objs.soda_ash))
    }

    @Test
    fun GameTestState.`batch produces at one four and eight ticks and stops after moving away`() = runSeaweedTest { deps ->
        player.members = true
        val source = source()
        repeat(4) { player.inv[it] = InvObj(objs.seaweed) }
        player.withProtectedAccess { deps.cooking.cook(this, source, objs.seaweed, 4) }
        advance()
        assertEquals(1, player.count(objs.soda_ash))
        advance(2)
        assertEquals(1, player.count(objs.soda_ash))
        advance()
        assertEquals(2, player.count(objs.soda_ash))
        advance(4)
        assertEquals(3, player.count(objs.soda_ash))
        player.placeAt(player.coords.translateX(10))
        advance(4)
        assertEquals(3, player.count(objs.soda_ash))
        assertEquals(1, player.count(objs.seaweed))
    }

    @Test
    fun GameTestState.`native seaweed use works on a fire and insufficient giant output space preserves input`() = runSeaweedTest { deps ->
        player.members = true
        val source = source()
        player.inv[0] = InvObj(objs.seaweed)
        player.withProtectedAccess { deps.locU.interactOp(this, source, source, locTypes.getValue(source.id), objTypes[objs.seaweed], player.inv, 0) }
        advance()
        assertEquals(1, player.count(objs.soda_ash))
        player.fillInv()
        player.inv[0] = InvObj(SeaweedInputs.giant)
        player.stats[stats.cooking] = 99
        val before = player.inv.toList()
        player.withProtectedAccess { deps.cooking.cook(this, source, SeaweedInputs.giant, 1) }
        advance()
        assertEquals(before, player.inv.toList())
        player.members = false
        player.inv[0] = InvObj(objs.seaweed)
        player.withProtectedAccess { deps.cooking.cook(this, source, objs.seaweed, 1) }
        advance()
        assertEquals(1, player.count(objs.seaweed))
    }

    private fun GameTestScope.source() = placeMapLoc(CoordGrid(2000, 2000), cooking_locs.fire, LocShape.CentrepieceStraight, LocAngle.West).also { player.placeAt(it.coords.translateX(-1)) }
    private fun GameTestState.runSeaweedTest(body: GameTestScope.(Dependencies) -> Unit) = runInjectedGameTest(Dependencies::class, scripts = arrayOf(SeaweedCookingScript::class), testBody = body)
    data class Dependencies @Inject constructor(val cooking: SeaweedCooking, val locU: LocUInteractions)
}
