package org.rsmod.content.skills.mining

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CelestialMiningTest {
    @Test
    fun GameTestState.chargedRingDeliversExtraOreXpAndSpendsExactlyOneDustCharge() = game {
        prepare(85)
        player.worn[12] = ring(3)
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 0
        random.then = 0
        advance(4)
        assertEquals(2, player.count(objs.iron_ore))
        assertEquals(700, player.statMap.getFineXP(stats.mining))
        assertEquals(2, charges())
    }

    @Test
    fun GameTestState.fullBagLosesBonusAndBonusXpButStillSpendsDust() = game {
        prepare(85)
        player.worn[12] = ring(1)
        for (slot in 0..26) player.inv[slot] = InvObj(objs.knife)
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 0
        random.then = 0
        advance(4)
        assertEquals(1, player.count(objs.iron_ore))
        assertEquals(350, player.statMap.getFineXP(stats.mining))
        assertEquals(25539, player.worn[12]?.id)
    }

    @Test
    fun GameTestState.unchargedRingInvisibleBoostImprovesRollWithoutGrantingHigherLevelOres() =
        game {
            prepare(15)
            player.righthand = InvObj(MiningObjs.ironPickaxe)
            player.worn[12] = InvObj(objTypes.getValue(25539))
            val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
            player.telejump(rock.coords.translateX(-1))
            player.opLoc1(rock)
            advance()
            // 54% fails the level15 iron curve, succeeds with the invisible four levels.
            random.next = 128
            random.then = 54
            advance(8)
            assertEquals(1, player.count(objs.iron_ore))
            assertEquals(25539, player.worn[12]?.id)
            player.stats[stats.mining] = 19
            val silver = placeMapLoc(CoordGrid(3299, 3311), MiningLocs.silver1)
            player.telejump(silver.coords.translateX(-1))
            player.opLoc1(silver)
            advance(9)
            assertEquals(0, player.count(objs.silver_ore))
        }

    @Test
    fun GameTestState.runiteDoesNotSpendStardustOrRollABonus() = game {
        prepare(85)
        player.worn[12] = ring(3)
        player.worn[4] = InvObj(objs.diary_varrock_armour_3)
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.runite1)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 0
        advance(4)
        assertEquals(1, player.count(objs.runite_ore))
        assertEquals(1250, player.statMap.getFineXP(stats.mining))
        assertEquals(3, charges())
    }

    @Test
    fun GameTestState.failedMiningRollPreservesCharge() = game {
        prepare(15)
        player.righthand = InvObj(MiningObjs.ironPickaxe)
        player.worn[12] = ring(3)
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 99
        advance(7)
        player.moveGameClick(player.coords.translate(-1, 0))
        advance()
        assertEquals(0, player.count(objs.iron_ore))
        assertEquals(3, charges())
    }

    @Test
    fun GameTestState.quarryBonusRepeatsTheActualLargestGraniteProductAndItsXp() = game {
        prepare(99)
        player.worn[12] = ring(3)
        val rock = placeMapLoc(CoordGrid(3297, 3311), QuarryLocs.granite)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 0
        random.then = 0
        advance(4)
        assertEquals(2, player.count(QuarryObjs.granite[2]))
        assertEquals(1500, player.statMap.getFineXP(stats.mining))
        assertEquals(2, charges())
    }

    @Test
    fun GameTestState.varrockArmourAndChargedRingCanBothAwardOreAndXp() = game {
        prepare(85)
        player.worn[4] = InvObj(objs.diary_varrock_armour_1)
        player.worn[12] = ring(3)
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.iron1)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        repeat(3) { random.then = 0 }
        advance(4)
        assertEquals(3, player.count(objs.iron_ore))
        assertEquals(1050, player.statMap.getFineXP(stats.mining))
        assertEquals(2, charges())
    }

    @Test
    fun GameTestState.clayBraceletMakesSoftClayAndDisintegratesOnItsLastUse() = game {
        prepare(85)
        player.worn[9] = InvObj(objs.bracelet_of_clay)
        player.setVarp(cacheTypes.varps.getValue(65818), 27)
        val rock = placeMapLoc(CoordGrid(3297, 3311), MiningLocs.clay1)
        player.telejump(rock.coords.translateX(-1))
        player.opLoc1(rock)
        advance()
        random.next = 128
        random.then = 0
        advance(4)
        assertEquals(1, player.count(objs.soft_clay))
        assertEquals(0, player.count(objs.clay))
        assertNull(player.worn[9])
        assertEquals(0, player.vars[cacheTypes.varps.getValue(65818)])
    }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.stats[stats.mining] = level
        player.statMap.setFineXP(stats.mining, 0)
        player.righthand = InvObj(MiningObjs.runePickaxe)
    }

    private fun GameTestScope.ring(charges: Int): InvObj =
        InvObj(objTypes.getValue(25541), vars = charges)

    private fun GameTestScope.charges(): Int = player.worn[12]!!.vars

    private fun GameTestState.game(body: GameTestScope.() -> Unit) =
        runGameTest(Mining::class, testBody = body)
}
