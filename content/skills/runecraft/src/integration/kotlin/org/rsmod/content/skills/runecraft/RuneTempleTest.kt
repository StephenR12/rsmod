package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class RuneTempleTest {
    @Test
    fun GameTestState.`matching talisman enters Chaos ruins and the portal returns outside`() =
        runInjectedGameTest(LocUInteractions::class, null, RuneTempleScript::class) { use ->
            val outside = CoordGrid(3059, 3590)
            val before = outside.translate(0, -1)
            player.members = true
            allocZoneCollision(CoordGrid(2275, 4847, 3))
            player.teleport(before)
            val ruinsType = locTypes.values.single { it.internalName == "chaostemple_ruined" }
            val ruins = placeMapLoc(outside, ruinsType)
            val talisman = objTypes.values.single { it.internalName == "chaos_talisman" }
            player.inv[0] = InvObj(talisman)
            player.withProtectedAccess {
                use.interactOp(this, ruins, ruins, ruinsType, talisman, inv, 0)
            }
            assertEquals(CoordGrid(2275, 4847, 3), player.coords)
            assertEquals(1, player.count(talisman))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterChaosTemple))
            val portalType = locTypes.values.single { it.internalName == "chaostemple_exit_portal" }
            val portal = placeMapLoc(player.coords.translate(0, 1), portalType)
            player.opLoc1(portal)
            advance()
            assertTrue(
                player.coords.x in 3058..3062 &&
                    player.coords.z in 3589..3593 &&
                    player.coords.level == 0
            )
        }

    @Test
    fun GameTestState
        .`mind tiara binding consumes exactly the selected blank and matching talisman`() =
        runInjectedGameTest(LocUInteractions::class, null, RuneTempleScript::class) { use ->
            player.members = true
            val altar = placeMapLoc(player.coords.translate(1, 0), RunecraftLocs.mindAltar)
            val blank = objTypes[objs.tiara]
            val talisman = objTypes.values.single { it.internalName == "mind_talisman" }
            val tiara = objTypes.values.single { it.internalName == "tiara_mind" }
            val xp = player.statMap.getFineXP(stats.runecrafting)
            player.fillInv()
            player.inv[22] = InvObj(blank)
            fun bind() =
                player.withProtectedAccess {
                    use.interactOp(this, altar, altar, locTypes[altar], blank, inv, 22)
                }
            bind()
            assertEquals(0, player.count(tiara))
            player.inv[12] = InvObj(talisman)
            bind()
            assertEquals(tiara.id, player.inv[22]?.id)
            assertEquals(0, player.count(talisman))
            assertEquals(275, player.statMap.getFineXP(stats.runecrafting) - xp)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MakeMindTiara))
        }
}
