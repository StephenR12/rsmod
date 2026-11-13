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
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class HouseStoveCookingTest {
    @Test
    fun GameTestState.`all fourteen early house stove variants cook food through native item use`() = runInjectedGameTest(Dependencies::class, scripts = arrayOf(CookingScript::class)) { deps ->
        player.members = true
        player.stats[stats.cooking] = 99
        val stoves = cooking_locs.houseFireStoves + cooking_locs.houseRangeStoves
        assertEquals(14, stoves.size)
        for ((index, stove) in stoves.withIndex()) {
            player.clearInv()
            val source = placeMapLoc(CoordGrid(2000 + index * 5, 2000), stove)
            player.placeAt(source.coords.translateX(-1))
            player.inv[0] = InvObj(objs.raw_shrimps)
            player.withProtectedAccess { deps.locU.interactOp(this, source, source, locTypes.getValue(source.id), objTypes[objs.raw_shrimps], player.inv, 0) }
            advance()
            assertEquals(1, player.count(objs.shrimps), "stove ${stove.id}")
        }
    }

    @Test
    fun GameTestState.`seaweed item use also accepts a house firepit`() = runInjectedGameTest(Dependencies::class, scripts = arrayOf(SeaweedCookingScript::class)) { deps ->
        player.members = true
        val source = placeMapLoc(CoordGrid(2000, 2000), cooking_locs.houseFireStoves.first())
        player.placeAt(source.coords.translateX(-1))
        player.inv[0] = InvObj(objs.seaweed)
        player.withProtectedAccess { deps.locU.interactOp(this, source, source, locTypes.getValue(source.id), objTypes[objs.seaweed], player.inv, 0) }
        advance()
        assertEquals(1, player.count(objs.soda_ash))
    }

    data class Dependencies @Inject constructor(val locU: LocUInteractions)
}
