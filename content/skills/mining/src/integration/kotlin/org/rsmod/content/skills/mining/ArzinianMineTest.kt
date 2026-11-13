package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.hat
import org.rsmod.api.player.righthand
import org.rsmod.api.player.ring
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningLocs
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class ArzinianMineTest {
    @Test
    fun GameTestState
        .`cannon repeat access requires appropriate native progress and worn helmet`() = test { d ->
        prepare(89)
        player.withProtectedAccess { assertFalse(d.mine.enter(this)) }
        assertFalse(ArzinianMine.inside(player.coords))
        VarPlayerIntMapSetter.set(player, ArzinianBits.quest, 90)
        player.hat = null
        player.withProtectedAccess { assertFalse(d.mine.enter(this)) }
        player.hat = InvObj(ArzinianObjs.helmet)
        player.withProtectedAccess { assertTrue(d.mine.enter(this)) }
        assertEquals(ArzinianMine.ENTRY, player.coords)
        advance(3)
        assertTrue(ArzinianMine.inside(player.coords))
        player.hat = null
        advance(2)
        assertEquals(ArzinianMine.EXIT, player.coords)
    }

    @Test
    fun GameTestState
        .`native Arzinian vein yields gold XP and Fremennik diary only on successful delivery`() =
        test { _ ->
            prepare(110)
            player.stats[stats.mining] = 99
            player.righthand = InvObj(MiningObjs.runePickaxe)
            val rock =
                placeMapLoc(
                    CoordGrid(2370, 4935),
                    ArzinianLocs.veins[0],
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            player.telejump(CoordGrid(2370, 4934))
            random.next = 128
            random.then = 0
            val xp = player.statMap.getFineXP(stats.mining)
            player.opLoc1(rock)
            advance()
            assertMessageSent("You swing your pick at the rock.")
            advance(8)
            assertEquals(1, player.count(objs.gold_ore))
            assertEquals(650, player.statMap.getFineXP(stats.mining) - xp)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineArzinianGold))
        }

    @Test
    fun GameTestState
        .`ordinary gold rocks inside the mine also award diary with native quest progress`() =
        test { _ ->
            prepare(90)
            player.stats[stats.mining] = 99
            player.righthand = InvObj(MiningObjs.runePickaxe)
            val rock = placeMapLoc(CoordGrid(2365, 4961), MiningLocs.gold1)
            player.telejump(rock.coords.translateZ(-1))
            random.next = 128
            random.then = 0
            player.opLoc1(rock)
            advance(9)
            assertEquals(1, player.count(objs.gold_ore))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MineArzinianGold))
        }

    @Test
    fun GameTestState.`full inventory and missing pickaxe cannot grant Arzinian gold diary`() =
        test { _ ->
            prepare(110)
            player.stats[stats.mining] = 99
            val rock = placeMapLoc(CoordGrid(2365, 4961), MiningLocs.gold1)
            player.telejump(rock.coords.translateZ(-1))
            player.opLoc1(rock)
            advance(9)
            assertEquals(0, player.count(objs.gold_ore))
            player.righthand = InvObj(MiningObjs.runePickaxe)
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.opLoc1(rock)
            advance(9)
            assertEquals(0, player.count(objs.gold_ore))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.MineArzinianGold))
        }

    @Test
    fun GameTestState.`gold hauler uses NPC-specific floor plus one fee and charos reduction`() =
        test { d ->
            prepare(110)
            repeat(25) { player.inv[it] = InvObj(objs.gold_ore) }
            player.withProtectedAccess {
                assertTrue(d.mine.bank(this))
                assertEquals(19, inv(invs.bank).count(d.objects[objs.gold_ore]))
            }
            assertEquals(0, player.count(objs.gold_ore))
            player.ring = InvObj(ArzinianObjs.charos)
            repeat(25) { player.inv[it] = InvObj(objs.gold_ore) }
            player.withProtectedAccess {
                assertTrue(d.mine.bank(this))
                assertEquals(41, inv(invs.bank).count(d.objects[objs.gold_ore]))
            }
        }

    @Test
    fun GameTestState
        .`ore hauler rejects uncompleted quest one ore and full bank without consuming inventory`() =
        test { d ->
            prepare(100)
            repeat(5) { player.inv[it] = InvObj(objs.gold_ore) }
            player.withProtectedAccess { assertFalse(d.mine.bank(this)) }
            assertEquals(5, player.count(objs.gold_ore))
            VarPlayerIntMapSetter.set(player, ArzinianBits.quest, 110)
            player.withProtectedAccess {
                for (i in inv(invs.bank).indices) inv(invs.bank)[i] = InvObj(objs.knife)
                assertFalse(d.mine.bank(this))
            }
            assertEquals(5, player.count(objs.gold_ore))
        }

    @Test
    fun GameTestState.`ferrymen charge two coins while charos and penniless return are free`() =
        test { d ->
            prepare(110)
            player.withProtectedAccess { assertFalse(d.mine.ferry(this, 0)) }
            player.inv[0] = InvObj(objs.coins, 2)
            player.withProtectedAccess { assertTrue(d.mine.ferry(this, 0)) }
            assertEquals(CoordGrid(2854, 10144), player.coords)
            assertEquals(0, player.count(objs.coins))
            player.withProtectedAccess { assertTrue(d.mine.ferry(this, 1)) }
            assertEquals(CoordGrid(2838, 10128), player.coords)
            player.ring = InvObj(ArzinianObjs.charos)
            player.withProtectedAccess { assertTrue(d.mine.ferry(this, 0)) }
        }

    private fun GameTestScope.prepare(progress: Int) {
        player.members = true
        player.hat = InvObj(ArzinianObjs.helmet)
        VarPlayerIntMapSetter.set(player, ArzinianBits.quest, progress)
        player.telejump(ArzinianMine.EXIT)
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, null, ArzinianMine::class, Mining::class) { d ->
            for (c in
                listOf(
                    ArzinianMine.ENTRY,
                    ArzinianMine.EXIT,
                    CoordGrid(2370, 4935),
                    CoordGrid(2365, 4961),
                    CoordGrid(2854, 10144),
                    CoordGrid(2838, 10128),
                )) for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(
                c.translate(x, z)
            )
            body(d)
        }

    data class Dependencies @Inject constructor(val mine: ArzinianMine, val objects: ObjTypeList)
}
