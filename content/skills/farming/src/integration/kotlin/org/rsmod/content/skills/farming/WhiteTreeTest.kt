package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.varbit.VarBitTypeList
import org.rsmod.game.type.varp.VarpTypeList
import org.rsmod.map.CoordGrid

class WhiteTreeTest {
    data class Deps @Inject constructor(val bits: VarBitTypeList, val vars: VarpTypeList)

    @Test
    fun GameTestState.`the palace tree holds four fruit then regrows one per five minutes`() =
        runInjectedGameTest(Deps::class, null, WhiteTree::class, FarmingScript::class) { deps ->
            player.members = true
            val item = objTypes.values.single { it.internalName == "garden_white_tree_fruit" }
            val at = CoordGrid(3229, 3474)
            val target =
                placeMapLoc(
                    at,
                    locTypes.values.single { it.internalName == "garden_white_tree_patch" },
                )
            player.teleport(at.translateX(-1))
            player.setVarBit(deps.bits[985]!!, 12)
            player.opLoc1(target)
            advance(3)
            assertEquals(0, player.count(item))
            player.setVarBit(deps.bits[961]!!, 60)
            repeat(4) {
                player.opLoc1(target)
                advance(3)
            }
            assertEquals(4, player.count(item))
            assertEquals(480, player.statMap.getFineXP(stats.farming))
            assertEquals(8, player.vars[deps.bits[985]!!])
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.PickVarrockWhiteTreeFruit))
            player.setVarp(deps.vars[65630]!!, (System.currentTimeMillis() / 60000).toInt() - 5)
            eventBus.publish(org.rsmod.game.entity.player.SessionStateEvent.Login(player))
            advance(6)
            player.opLoc1(target)
            advance(3)
            assertEquals(5, player.count(item))
            assertEquals(9, player.vars[deps.bits[985]!!])
        }
}
