package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.hat
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.content.skills.mining.motherlode.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MotherlodeMineTest {
    @Test
    fun GameTestState.celestialBoostImprovesPayDirtRollWithoutSpendingDustOrDuplicating() =
        game { d ->
            prepare(82)
            player.righthand = InvObj(MiningObjs.runePickaxe)
            // Native celestial ring charged with three units, using the shared charges varobj.
            player.worn[12] = org.rsmod.game.inv.InvObj(objTypes.getValue(25541), vars = 3)
            val loc =
                placeMapLoc(
                    CoordGrid(3713, 5638),
                    MlmLocs.veins[1],
                    LocShape.WallStraight,
                    LocAngle.East,
                )
            player.telejump(loc.coords.translateX(1))
            random.next = 128
            random.then = 39
            random.then = 1
            random.then = 0
            random.then = 39
            player.opLoc1(loc)
            advance(4)
            player.cancelActiveCoroutine()
            assertEquals(1, player.count(MlmObjs.dirt))
            assertEquals(3, player.worn[12]?.vars)
        }

    @Test
    fun GameTestState.walkingClickCancelsPendingPayDirtWithoutWaitingForNextRoll() = game { d ->
        prepare(85)
        player.righthand = InvObj(MiningObjs.runePickaxe)
        val loc =
            placeMapLoc(
                CoordGrid(3713, 5638),
                MlmLocs.veins[1],
                LocShape.WallStraight,
                LocAngle.East,
            )
        player.telejump(loc.coords.translateX(1))
        val origin = player.coords
        player.opLoc1(loc)
        advance()
        player.moveGameClick(CoordGrid(3715, 5638))
        advance(2)
        assertNotEquals(origin, player.coords)
        advance(10)
        assertEquals(0, player.count(MlmObjs.dirt))
    }

    @Test
    fun GameTestState.payDirtConsumesCrystalChargeButClearingRockfallsDoesNot() = game { d ->
        prepare(85)
        VarPlayerIntMapSetter.set(player, org.rsmod.api.config.refs.varbits.song_of_the_elves, 194)
        player.righthand = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(1).pack())
        val loc =
            placeMapLoc(
                CoordGrid(3713, 5638),
                MlmLocs.veins[1],
                LocShape.WallStraight,
                LocAngle.East,
            )
        player.telejump(loc.coords.translateX(1))
        random.next = 1
        random.then = 128
        random.then = 0
        random.then = 1
        random.then = 0
        random.then = 39
        player.opLoc1(loc)
        advance(4)
        player.cancelActiveCoroutine()
        assertEquals(1, player.count(MlmObjs.dirt))
        assertEquals(MiningObjs.crystalPickaxeInactive.id, player.righthand?.id)
        player.righthand = InvObj(MiningObjs.crystalPickaxe, vars = CrystalPickaxeState(2).pack())
        val rock = placeMapLoc(CoordGrid(3715, 5638), MlmLocs.rockfalls[0])
        player.withProtectedAccess { d.mine.rockfall(this, rock) }
        advance(5)
        assertEquals(2, CrystalPickaxeState.fromCharged(player.righthand!!).charges)
    }

    @Test
    fun GameTestState
        .`pay dirt requires thirty base Mining and stores the chosen ore at yield time`() =
        game { d ->
            prepare(29)
            player.setCurrentLevel(stats.mining, 99)
            player.righthand = InvObj(MiningObjs.runePickaxe)
            val loc =
                placeMapLoc(
                    CoordGrid(3713, 5638),
                    MlmLocs.veins[1],
                    LocShape.WallStraight,
                    LocAngle.East,
                )
            player.telejump(loc.coords.translateX(1))
            player.withProtectedAccess { d.mine.mine(this, loc) }
            advance(4)
            assertEquals(0, player.count(MlmObjs.dirt))
            player.setBaseLevel(stats.mining, 85)
            player.setCurrentLevel(stats.mining, 85)
            random.next = 128
            random.then = 0
            random.then = 1
            random.then = 0
            random.then = 39
            player.withProtectedAccess { d.mine.mine(this, loc) }
            advance(4)
            player.cancelActiveCoroutine()
            assertTrue(player.count(MlmObjs.dirt) > 0)
            val dirt = player.inv.filterNotNull().first { it.id == MlmObjs.dirt.id }
            assertEquals(5, dirt.vars)
            assertTrue(player.statMap.getFineXP(stats.mining) >= 600)
        }

    @Test
    fun GameTestState
        .`washing keeps mined runite after a level drain and awards XP only on completion`() =
        game { d ->
            prepare(30)
            player.inv[0] = InvObj(MlmObjs.dirt, vars = 5)
            player.statMap.setFineXP(stats.mining, 0)
            player.withProtectedAccess { assertEquals(1, d.storage.deposit(this)) }
            assertEquals(1, d.storage.count(player))
            repeat(MotherlodeStorage.PROCESS_TICKS - 1) { d.storage.process(player, true) }
            assertEquals(0, player.statMap.getFineXP(stats.mining))
            assertTrue(d.storage.sack(player).isEmpty())
            d.storage.process(player, true)
            assertEquals(750, player.statMap.getFineXP(stats.mining))
            player.withProtectedAccess { assertEquals(1, d.storage.collect(this)) }
            assertEquals(1, player.count(objs.runite_ore))
            assertEquals(0, d.storage.count(player))
        }

    @Test
    fun GameTestState
        .`broken waterwheels pause saved pending processing and nugget batches cap at three`() =
        game { d ->
            prepare(30)
            val beforeXP = player.statMap.getFineXP(stats.mining)
            repeat(6) { player.inv[it] = InvObj(MlmObjs.dirt, vars = 6) }
            player.withProtectedAccess { assertEquals(6, d.storage.deposit(this)) }
            val saved = d.storage.pending(player).filterNotNull().map { it.vars }
            repeat(20) { d.storage.process(player, false) }
            assertEquals(saved, d.storage.pending(player).filterNotNull().map { it.vars })
            d.storage.login(player)
            repeat(MotherlodeStorage.PROCESS_TICKS) { d.storage.process(player, true) }
            player.withProtectedAccess { d.storage.collect(this) }
            assertEquals(3, player.count(MlmObjs.nugget))
            assertEquals(beforeXP, player.statMap.getFineXP(stats.mining))
            assertEquals(0, d.storage.count(player))
        }

    @Test
    fun GameTestState
        .`sack capacities prevent overflow without deleting extra inventory pay dirt`() =
        game { d ->
            prepare(30)
            d.storage.sack(player)[0] = InvObj(objs.coal, 107)
            repeat(5) { player.inv[it] = InvObj(MlmObjs.dirt, vars = 1) }
            player.withProtectedAccess { assertEquals(1, d.storage.deposit(this)) }
            assertEquals(4, player.count(MlmObjs.dirt))
            assertEquals(108, d.storage.count(player))
            VarPlayerIntMapSetter.set(player, MlmBits.big, 1)
            player.withProtectedAccess { assertEquals(4, d.storage.deposit(this)) }
            assertEquals(112, d.storage.count(player))
            assertEquals(189, d.storage.capacity(player))
        }

    @Test
    fun GameTestState.`full inventory retains cleaned ore but an existing nugget stack can grow`() =
        game { d ->
            prepare(30)
            d.storage.sack(player)[0] = InvObj(objs.coal, 10)
            d.storage.sack(player)[1] = InvObj(MlmObjs.nugget, 3)
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.inv[0] = InvObj(MlmObjs.nugget, 2)
            player.withProtectedAccess { assertEquals(3, d.storage.collect(this)) }
            assertEquals(5, player.count(MlmObjs.nugget))
            assertEquals(10, d.storage.count(player))
            player.inv[1] = null
            player.withProtectedAccess { assertEquals(1, d.storage.collect(this)) }
            assertEquals(1, player.count(objs.coal))
            assertEquals(9, d.storage.count(player))
        }

    @Test
    fun GameTestState
        .`real broken strut repair restores shared flow and grants visible Smithing XP and easy diary`() =
        game { d ->
            prepare(30)
            player.stats[stats.smithing] = 10
            player.statMap.setFineXP(stats.smithing, 0)
            player.inv[0] = InvObj(objs.hammer)
            val strut =
                placeMapLoc(
                    CoordGrid(3742, 5663),
                    MlmLocs.broken,
                    LocShape.CentrepieceStraight,
                    LocAngle.West,
                )
            player.telejump(strut.coords.translateX(-1))
            assertFalse(d.world.flowing())
            random.next = 0
            random.then = 2
            player.opLoc1(strut)
            advance(3)
            assertTrue(d.world.flowing())
            assertEquals(150, player.statMap.getFineXP(stats.smithing))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.RepairMotherlodeStrut))
        }

    @Test
    fun GameTestState.`Prospector purchases and eighty percent refunds preserve shop provenance`() =
        game { d ->
            prepare(57)
            player.inv[0] = InvObj(MlmObjs.nugget, 180)
            player.withProtectedAccess { for (i in 0..3) assertTrue(d.percy.buy(this, i)) }
            assertEquals(0, player.count(MlmObjs.nugget))
            for (item in MotherlodePercy.GOODS.take(4)) assertEquals(1, player.count(item))
            player.withProtectedAccess { assertTrue(d.percy.sell(this, 0)) }
            assertEquals(32, player.count(MlmObjs.nugget))
            player.inv[4] = InvObj(ProspectorOutfits.hat)
            player.withProtectedAccess { assertFalse(d.percy.sell(this, 0)) }
            assertEquals(32, player.count(MlmObjs.nugget))
        }

    @Test
    fun GameTestState
        .`upper floor requires unboosted fifty seven and upgrades cannot be bought twice`() =
        game { d ->
            prepare(56)
            player.setCurrentLevel(stats.mining, 99)
            player.inv[0] = InvObj(MlmObjs.nugget, 500)
            player.withProtectedAccess {
                assertFalse(d.percy.upgrade(this, 0))
                assertFalse(d.percy.upgrade(this, 1))
            }
            assertEquals(500, player.count(MlmObjs.nugget))
            player.setBaseLevel(stats.mining, 57)
            player.withProtectedAccess {
                assertTrue(d.percy.upgrade(this, 0))
                assertFalse(d.percy.upgrade(this, 0))
                assertTrue(d.percy.upgrade(this, 1))
                assertTrue(d.percy.upgrade(this, 2))
            }
            assertEquals(150, player.count(MlmObjs.nugget))
            assertEquals(3, player.vars[MlmVars.unlocks])
            assertEquals(189, d.storage.capacity(player))
        }

    @Test
    fun GameTestState
        .`nugget and ore cascade honour level gates and runite is chosen before lower ores`() =
        game { _ ->
            prepare(30)
            random.next = 0
            player.withProtectedAccess { assertEquals(6, MlmRewards.choose(this)) }
            random.next = 1
            player.withProtectedAccess { assertEquals(1, MlmRewards.choose(this)) }
            player.setCurrentLevel(stats.mining, 85)
            random.next = 1
            random.then = 0
            player.withProtectedAccess { assertEquals(5, MlmRewards.choose(this)) }
            assertEquals(1, objTypes[MlmObjs.dirt].param(params.no_bank))
        }

    @Test
    fun GameTestState
        .`guild entry from Motherlode requires sixty and a normal or golden prospector helmet`() =
        game { _ ->
            prepare(59)
            val exit =
                placeMapLoc(
                    CoordGrid(3717, 5678),
                    MlmLocs.guildExit,
                    LocShape.CentrepieceStraight,
                    LocAngle.East,
                )
            player.telejump(exit.coords.translateX(1))
            player.hat = InvObj(ProspectorOutfits.goldenHat)
            player.opLoc1(exit)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.EnterMiningGuildProspector))
            player.setCurrentLevel(stats.mining, 60)
            player.opLoc1(exit)
            advance()
            assertEquals(CoordGrid(3055, 9744), player.coords)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.EnterMiningGuildProspector))
        }

    @Test
    fun GameTestState
        .`all nugget shop goods include usable clay packs with the correct currency`() = game { d ->
        prepare(30)
        player.inv[0] = InvObj(MlmObjs.nugget, 50)
        player.withProtectedAccess {
            assertTrue(d.percy.buy(this, 6))
            assertTrue(d.percy.buy(this, 7))
        }
        assertEquals(0, player.count(MlmObjs.nugget))
        assertEquals(1, player.count(MlmObjs.clayPack))
        assertEquals(1, player.count(MlmObjs.gemPack))
        val slot = player.inv.indices.first { player.inv[it]?.id == MlmObjs.clayPack.id }
        player.members = false
        player.withProtectedAccess { assertTrue(d.packs.openClay(this, slot)) }
        val note = objTypes.getValue(objTypes[objs.soft_clay].certlink)
        assertEquals(100, player.count(note))
    }

    @Test
    fun GameTestState
        .`gem pack gives forty noted gems and inventory rejection preserves an unopened pack`() =
        game { d ->
            prepare(30)
            player.inv[0] = InvObj(MlmObjs.gemPack)
            for (i in 0 until 40) {
                if (i == 0) random.next = 1 else random.then = 1
                random.then = 0
            }
            player.withProtectedAccess { assertTrue(d.packs.openGems(this, 0)) }
            val note = objTypes.getValue(objTypes[objs.uncut_sapphire].certlink)
            assertEquals(40, player.count(note))
            assertEquals(0, player.count(MlmObjs.gemPack))
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.inv[0] = InvObj(MlmObjs.gemPack)
            player.withProtectedAccess { assertFalse(d.packs.openGems(this, 0)) }
            assertEquals(1, player.count(MlmObjs.gemPack))
        }

    @Test
    fun GameTestState.`actual hopper timer continues washing while a bank modal is open`() =
        game { d ->
            prepare(30)
            player.statMap.setFineXP(stats.mining, 0)
            player.inv[0] = InvObj(MlmObjs.dirt, vars = 1)
            val hopper = placeMapLoc(CoordGrid(3748, 5672), MlmLocs.hopper)
            player.telejump(hopper.coords.translateX(-1))
            player.opLoc1(hopper)
            advance()
            assertEquals(0, player.count(MlmObjs.dirt))
            advance(20)
            assertEquals(0, player.statMap.getFineXP(stats.mining))
            val strut = placeMapLoc(CoordGrid(3742, 5663), MlmLocs.broken)
            assertTrue(d.world.repair(strut, 2))
            player.withProtectedAccess {
                ifOpenMainSidePair(interfaces.bank_main, interfaces.bank_side)
            }
            advance(15)
            assertEquals(150, player.statMap.getFineXP(stats.mining))
            assertEquals(1, d.storage.count(player))
            assertTrue(d.storage.pending(player).isEmpty())
        }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.setBaseLevel(stats.mining, level)
        player.setCurrentLevel(stats.mining, level)
    }

    private fun GameTestState.game(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            null,
            MotherlodeMine::class,
            MotherlodePacks::class,
        ) { d ->
            for (c in
                listOf(
                    CoordGrid(3713, 5638),
                    CoordGrid(3742, 5663),
                    CoordGrid(3717, 5678),
                    CoordGrid(3055, 9744),
                    player.coords,
                )) for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(
                c.translate(x, z)
            )
            body(d)
        }

    data class Dependencies
    @Inject
    constructor(
        val mine: MotherlodeMine,
        val storage: MotherlodeStorage,
        val world: MotherlodeWorld,
        val percy: MotherlodePercy,
        val packs: MotherlodePacks,
    )
}
