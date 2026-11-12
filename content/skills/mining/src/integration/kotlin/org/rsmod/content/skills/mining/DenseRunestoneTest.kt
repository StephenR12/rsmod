package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.back
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.configs.MiningVarbits
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class DenseRunestoneTest {
    @Test
    fun GameTestState.`cache 233 pins dense runestone objects morphs and personal varbits`() =
        runGameTest(Mining::class) {
            assertEquals(8975, MiningLocs.denseRunestone.id)
            assertEquals(8976, MiningLocs.depletedRunestone.id)
            assertEquals(8981, MiningLocs.denseRunestoneOne.id)
            assertEquals(10796, MiningLocs.denseRunestoneTwo.id)
            assertEquals(4927, MiningVarbits.denseRunestoneOne.id)
            assertEquals(4928, MiningVarbits.denseRunestoneTwo.id)
            assertEquals(4929, MiningVarbits.denseRunestones.id)
            assertEquals(13445, MiningObjs.denseEssenceBlock.id)

            val active = locTypes[MiningLocs.denseRunestone]
            val depleted = locTypes[MiningLocs.depletedRunestone]
            assertEquals("Dense runestone", active.name)
            assertEquals("Chip", active.op[0])
            assertEquals("Depleted runestone", depleted.name)
            assertEquals("Check", depleted.op[0])

            val first = locTypes[MiningLocs.denseRunestoneOne]
            val second = locTypes[MiningLocs.denseRunestoneTwo]
            assertEquals(MiningVarbits.denseRunestoneOne.id, first.multiVarBit)
            assertEquals(MiningVarbits.denseRunestoneTwo.id, second.multiVarBit)
            assertEquals(listOf(8975, 8976), first.multiLoc.map { it.toInt() and 0xFFFF })
            assertEquals(listOf(8975, 8976), second.multiLoc.map { it.toInt() and 0xFFFF })
        }

    @Test
    fun GameTestState.`requires membership both levels chisel carried pickaxe and space`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.denseRunestoneOne)
            player.placeAt(rock.coords.translateX(-1))
            player.stats[stats.mining] = 38
            player.stats[stats.crafting] = 38
            player.inv[0] = InvObj(MiningObjs.chisel)
            player.inv[1] = InvObj(MiningObjs.blackPickaxe)

            player.members = false
            player.opLoc1(rock)
            advance()
            assertMessageSent("You need to be on a members' world to mine this runestone.")

            player.members = true
            player.stats[stats.mining] = 37
            player.opLoc1(rock)
            advance()
            assertMessageSent("You need a Mining level of 38 to mine this runestone.")

            player.stats[stats.mining] = 38
            player.stats[stats.crafting] = 37
            player.opLoc1(rock)
            advance()
            assertMessageSent("You need a Crafting level of 38 to mine this runestone.")

            player.stats[stats.crafting] = 38
            player.inv[0] = null
            player.opLoc1(rock)
            advance()
            assertMessageSent("You need a chisel to mine this runestone.")

            player.inv[0] = InvObj(MiningObjs.chisel)
            player.inv[1] = null
            player.righthand = InvObj(MiningObjs.blackPickaxe)
            player.opLoc1(rock)
            advance()
            assertMessagesSent(
                "You need a pickaxe to mine this rock.",
                "You do not have a pickaxe which you have the Mining level to use.",
            )

            player.righthand = null
            player.fillInv()
            player.inv[0] = InvObj(MiningObjs.chisel)
            player.inv[1] = InvObj(MiningObjs.blackPickaxe)
            player.opLoc1(rock)
            advance()
            assertMessageSent("Your inventory is too full to hold any more dense essence blocks.")
        }

    @Test
    fun GameTestState.`nine tick reward gives exact mining and crafting xp and loops`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            val miningStart = player.statMap.getFineXP(stats.mining)
            val craftingStart = player.statMap.getFineXP(stats.crafting)
            random.next = 0

            player.opLoc1(rock)
            advance()
            advance(8)
            assertEquals(0, player.count(MiningObjs.denseEssenceBlock))

            advance()
            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))
            assertEquals(120, player.statMap.getFineXP(stats.mining) - miningStart)
            assertEquals(80, player.statMap.getFineXP(stats.crafting) - craftingStart)
            assertMessageSent("You chip some dense essence from the runestone.")
            assertEquals(0, player.vars[MiningVarbits.denseRunestoneOne])
        }

    @Test
    fun GameTestState.`medium diary roll gives two blocks without extra xp`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            player.setVarBit(varbits.kourend_diary_medium_complete, 1)
            val miningStart = player.statMap.getFineXP(stats.mining)
            val craftingStart = player.statMap.getFineXP(stats.crafting)
            random.next = 0
            random.then = 0

            player.opLoc1(rock)
            advance(10)

            assertEquals(2, player.count(MiningObjs.denseEssenceBlock))
            assertEquals(120, player.statMap.getFineXP(stats.mining) - miningStart)
            assertEquals(80, player.statMap.getFineXP(stats.crafting) - craftingStart)
        }

    @Test
    fun GameTestState.`visible boosts satisfy both level requirements`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            player.stats.setBaseLevel(stats.mining, 37)
            player.stats.setCurrentLevel(stats.mining, 38)
            player.stats.setBaseLevel(stats.crafting, 37)
            player.stats.setCurrentLevel(stats.crafting, 38)
            random.next = 99

            player.opLoc1(rock)
            advance(10)

            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))
        }

    @Test
    fun GameTestState.`mining cape and Varrock armour never duplicate dense essence`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            player.back = InvObj(objs.mining_skillcape)
            player.torso = InvObj(objs.diary_varrock_armour_4)
            random.next = 0

            player.opLoc1(rock)
            advance(10)

            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))
        }

    @Test
    fun GameTestState.`crystal pickaxe is preserved without consuming state`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            player.stats[stats.mining] = 99
            player.setVarBit(varbits.song_of_the_elves, 194)
            val crystal = InvObj(MiningObjs.crystalPickaxe)
            player.inv[1] = crystal
            random.next = 99

            player.opLoc1(rock)
            advance(10)

            assertEquals(crystal, player.inv[1])
            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))
        }

    @Test
    fun GameTestState.`losing the chisel during the delay interrupts silently without a reward`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            player.opLoc1(rock)
            advance(6)
            player.inv[0] = null

            advance(5)

            assertEquals(0, player.count(MiningObjs.denseEssenceBlock))
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`slow bronze pickaxe still uses the fixed nine tick interval`() =
        runGameTest(Mining::class) {
            val first = preparedRock(MiningLocs.denseRunestoneOne)
            player.inv[1] = InvObj(MiningObjs.bronzePickaxe)
            random.next = 99
            player.opLoc1(first)
            advance(9)
            assertEquals(0, player.count(MiningObjs.denseEssenceBlock))
            advance()
            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))
        }

    @Test
    fun GameTestState.`depletion alternates personal runestone state`() =
        runGameTest(Mining::class) {
            val first = preparedRock(MiningLocs.denseRunestoneOne)
            random.next = 99
            player.opLoc1(first)
            advance(10)
            assertEquals(1, player.vars[MiningVarbits.denseRunestoneOne])
            assertEquals(0, player.vars[MiningVarbits.denseRunestoneTwo])

            val other = registerPlayer(TEST_COORDS.translateX(3))
            assertEquals(0, other.vars[MiningVarbits.denseRunestoneOne])
            assertEquals(0, other.vars[MiningVarbits.denseRunestoneTwo])
        }

    @Test
    fun GameTestState.`second runestone depletion activates the first`() =
        runGameTest(Mining::class) {
            val second = preparedRock(MiningLocs.denseRunestoneTwo)
            random.next = 99
            player.opLoc1(second)
            advance(10)
            assertEquals(0, player.vars[MiningVarbits.denseRunestoneOne])
            assertEquals(1, player.vars[MiningVarbits.denseRunestoneTwo])
        }

    @Test
    fun GameTestState.`full inventory silently stops the automatic loop after its last reward`() =
        runGameTest(Mining::class) {
            val rock = preparedRock(MiningLocs.denseRunestoneOne)
            repeat(25) { slot -> player.inv[slot + 2] = InvObj(MiningObjs.chisel) }
            random.next = 0
            player.opLoc1(rock)
            advance(10)
            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))

            advance(10)
            assertEquals(1, player.count(MiningObjs.denseEssenceBlock))
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`depleted runestone check reports its state`() =
        runGameTest(Mining::class) {
            val rock = placeMapLoc(TEST_COORDS, MiningLocs.depletedRunestone)
            player.placeAt(rock.coords.translateX(-1))
            player.opLoc1(rock)
            advance()
            assertMessageSent("The runestone has been mined to depletion.")
        }

    @Test
    fun GameTestState.`persistence chance exactly matches the cache 233 wiki chart`() =
        runGameTest {
            val expectedSuccessfulRolls =
                listOf(
                    38 to 219,
                    42 to 219,
                    43 to 220,
                    47 to 220,
                    48 to 221,
                    52 to 221,
                    53 to 222,
                    57 to 222,
                    58 to 223,
                    62 to 223,
                    63 to 224,
                    67 to 224,
                    68 to 225,
                    72 to 225,
                    73 to 226,
                    76 to 226,
                    77 to 227,
                    81 to 227,
                    82 to 228,
                    86 to 228,
                    87 to 229,
                    91 to 229,
                    92 to 230,
                    96 to 230,
                    97 to 231,
                    99 to 231,
                )
            for ((level, successfulRolls) in expectedSuccessfulRolls) {
                val rate =
                    SkillingSuccessRate.successRate(
                        Mining.DENSE_PERSIST_LOW,
                        Mining.DENSE_PERSIST_HIGH,
                        level,
                        maxLevel = 99,
                    )
                assertEquals(successfulRolls, (rate * 256).toInt(), "level=$level")
            }
        }

    private fun org.rsmod.api.testing.scope.GameTestScope.preparedRock(
        type: org.rsmod.game.type.loc.LocType
    ): org.rsmod.game.loc.BoundLocInfo {
        val rock = placeMapLoc(TEST_COORDS, type)
        player.placeAt(rock.coords.translateX(-1))
        player.members = true
        player.stats[stats.mining] = 38
        player.stats[stats.crafting] = 38
        player.inv[0] = InvObj(MiningObjs.chisel)
        player.inv[1] = InvObj(MiningObjs.blackPickaxe)
        return rock
    }

    companion object {
        private val TEST_COORDS = CoordGrid(0, 50, 50, 34, 31)
    }
}
