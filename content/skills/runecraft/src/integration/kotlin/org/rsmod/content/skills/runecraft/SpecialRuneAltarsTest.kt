package org.rsmod.content.skills.runecraft

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@Execution(ExecutionMode.SAME_THREAD)
class SpecialRuneAltarsTest {
    @Test
    fun GameTestState
        .`Ourania rolls each essence by level and independently doubles diary runes`() =
        runGameTest(SpecialRuneAltars::class) {
            player.members = true
            player.stats[stats.runecrafting] = 20
            player.withProtectedAccess { vars[varbits.ardougne_diary_medium_complete] = 1 }
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.inv[1] = InvObj(RunecraftObjs.daeyaltEssence)
            player.inv[2] = InvObj(RunecraftObjs.warpedExtract)
            val altar = placeMapLoc(player.coords.translate(1, 0), RunecraftLocs.ouraniaAltar)
            random.next = 8
            random.then = 149
            random.then = 9999
            random.then = 250
            val xp = player.statMap.getFineXP(stats.runecrafting)
            player.opLoc1(altar)
            advance()
            assertEquals(2, player.count(objs.blood_rune))
            assertEquals(1, player.count(objs.air_rune))
            assertEquals(1, player.count(RunecraftObjs.warpedExtract))
            assertEquals(306, player.statMap.getFineXP(stats.runecrafting) - xp)
        }

    @Test
    fun GameTestState.`Ourania tier boundaries include the separate level 99 distribution`() =
        runGameTest(SpecialRuneAltars::class) {
            player.members = true
            val altar = placeMapLoc(player.coords.translate(1, 0), RunecraftLocs.ouraniaAltar)
            fun craft(level: Int, draw: Int, expected: org.rsmod.game.type.obj.ObjType) {
                player.clearInv()
                player.stats[stats.runecrafting] = level
                player.inv[0] = InvObj(RunecraftObjs.pureEssence)
                random.next = draw
                player.opLoc1(altar)
                advance()
                assertEquals(1, player.count(expected), "level=$level draw=$draw")
            }
            craft(9, 2, objs.blood_rune)
            craft(10, 2, objs.soul_rune)
            craft(98, 899, objs.blood_rune)
            craft(99, 899, objs.soul_rune)
            craft(99, 9999, objs.air_rune)
        }

    @Test
    fun GameTestState
        .`sunfire consumes matched ingredients and adds one twisted extract without extra xp`() =
        runInjectedGameTest(LocUInteractions::class, null, SpecialRuneAltars::class) { use ->
            player.members = true
            player.stats[stats.runecrafting] = 98
            player.withProtectedAccess { vars[RunecraftVarbits.desertTreasureTwo] = 1 }
            player.inv[0] = InvObj(RunecraftObjs.pureEssence)
            player.inv[1] = InvObj(RunecraftObjs.daeyaltEssence)
            player.inv[2] = InvObj(RunecraftObjs.sunfireSplinters, 3)
            player.inv[3] = InvObj(objs.fire_rune, 2)
            player.inv[4] = InvObj(RunecraftObjs.twistedExtract)
            val xp = player.statMap.getFineXP(stats.runecrafting)
            val shrine =
                placeMapLoc(player.coords.translate(1, 0), RunecraftLocs.ralosShrines.first())
            player.withProtectedAccess {
                use.interactOp(
                    this,
                    shrine,
                    shrine,
                    locTypes[shrine],
                    objTypes[objs.fire_rune],
                    inv,
                    3,
                )
            }
            assertEquals(66, player.count(RunecraftObjs.sunfireRune))
            assertEquals(1, player.count(RunecraftObjs.sunfireSplinters))
            assertEquals(0, player.count(RunecraftObjs.twistedExtract))
            assertEquals(225, player.statMap.getFineXP(stats.runecrafting) - xp)
        }
}
