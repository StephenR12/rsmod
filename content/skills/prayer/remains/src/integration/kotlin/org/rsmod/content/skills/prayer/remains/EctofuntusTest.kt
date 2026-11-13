package org.rsmod.content.skills.prayer.remains

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class EctofuntusTest {
    @Test
    fun GameTestState.`load grind and fill two pots then worship and collect the earned tokens`() =
        runInjectedGameTest(LocUInteractions::class, null, Ectofuntus::class) { use ->
            player.members = true
            val loader = placeMapLoc(player.coords.translate(1, 0), locTypes.getValue(16654))
            val pool = placeMapLoc(player.coords.translate(-1, 0), locTypes.getValue(17116))
            val fountain = placeMapLoc(player.coords.translate(0, 1), locTypes.getValue(16648))
            player.inv[0] = InvObj(objs.dragon_bones)
            player.inv[1] = InvObj(objs.dragon_bones)
            player.inv[2] = InvObj(objs.pot_empty)
            player.inv[3] = InvObj(objs.pot_empty)
            player.inv[4] = InvObj(objs.bucket_empty)
            player.inv[5] = InvObj(objs.bucket_empty)
            player.withProtectedAccess {
                use.interactOp(
                    this,
                    loader,
                    loader,
                    locTypes[loader],
                    objTypes[objs.dragon_bones],
                    inv,
                    0,
                )
            }
            advance(20)
            assertEquals(2, player.count(objTypes.getValue(4261)))
            player.withProtectedAccess {
                use.interactOp(
                    this,
                    pool,
                    pool,
                    locTypes[pool],
                    objTypes[objs.bucket_empty],
                    inv,
                    4,
                )
            }
            advance(4)
            assertEquals(2, player.count(objTypes.getValue(4286)))
            val startXp = player.statMap.getFineXP(stats.prayer)
            repeat(2) {
                player.opLoc1(fountain)
                advance(3)
            }
            assertEquals(startXp + 5760, player.statMap.getFineXP(stats.prayer))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.WorshipEctofuntus))
            assertEquals(2, player.count(objs.bucket_empty))
            assertEquals(2, player.count(objs.pot_empty))
            val ghost = spawnNpc(player.coords.translate(0, -1), npcTypes.getValue(2988))
            player.opNpc1(ghost)
            advance()
            assertEquals(0, player.count(objTypes.getValue(4278)))
            player.worn[2] = InvObj(objTypes.getValue(552))
            player.opNpc1(ghost)
            advance()
            assertEquals(10, player.count(objTypes.getValue(4278)))
            player.opNpc1(ghost)
            advance()
            assertEquals(10, player.count(objTypes.getValue(4278)))
        }

    @Test
    fun GameTestState
        .`Robin exchanges mixed bones up to the medium daily entitlement and available space`() =
        runGameTest(Ectofuntus::class) {
            player.members = true
            val robin = spawnNpc(player.coords.translate(1, 0), npcTypes.getValue(2995))
            repeat(14) { player.inv[it] = InvObj(if (it == 0) objs.big_bones else objs.bones) }
            player.opNpc3(robin)
            advance()
            assertEquals(0, player.count(objTypes.getValue(4286)))
            player.withProtectedAccess {
                vars[DiaryRegion.Morytania.tier(DiaryDifficulty.Medium).reward] = 1
            }
            player.opNpc3(robin)
            advance()
            assertEquals(13, player.count(objTypes.getValue(4286)))
            assertEquals(1, player.count(objTypes.getValue(4257)))
            assertEquals(12, player.count(objTypes.getValue(4255)))
            assertEquals(1, player.count(objs.bones))
            player.opNpc3(robin)
            advance()
            assertEquals(13, player.count(objTypes.getValue(4286)))
        }
}
