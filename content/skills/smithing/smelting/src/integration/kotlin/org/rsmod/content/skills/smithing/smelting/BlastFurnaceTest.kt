package org.rsmod.content.skills.smithing.smelting.blast

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.hands
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.ring
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.smithing.smelting.AnvilInterfaces
import org.rsmod.content.skills.smithing.smelting.AnvilLocs
import org.rsmod.content.skills.smithing.smelting.AnvilSmithing
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BlastFurnaceTest {
    @Test
    fun GameTestState.nativeForemanConversationAwardsFreePermissionAtUnboosted60() = game { d ->
        prepare(60)
        val foreman = Npc(d.npcs[BlastNpcs.foreman], CoordGrid(1944, 4958))
        npcRepo.add(foreman, Int.MAX_VALUE)
        player.telejump(CoordGrid(1944, 4959))
        player.opNpc1(foreman)
        advance()
        choose(1)
        advance()
        assertEquals(1, player.vars[BlastVars.permission])
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.ObtainFreeBlastFurnacePermission))
        assertTrue(d.blast.allowed(player))
    }

    @Test
    fun GameTestState.visibleBoostAndNonmemberDoNotGrantPermissionOrDiary() = game { d ->
        prepare(59)
        player.setCurrentLevel(stats.smithing, 99)
        player.withProtectedAccess { assertFalse(d.blast.permission(this)) }
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.ObtainFreeBlastFurnacePermission))
        player.stats[stats.smithing] = 60
        player.members = false
        player.withProtectedAccess { assertFalse(d.blast.permission(this)) }
        assertEquals(0, player.vars[BlastVars.permission])
    }

    @Test
    fun GameTestState.nativePayRequiresCoinsAndCharosHalvesTheTenMinuteFee() = game { d ->
        prepare(30)
        val foreman = Npc(d.npcs[BlastNpcs.foreman], CoordGrid(1944, 4958))
        npcRepo.add(foreman, Int.MAX_VALUE)
        player.telejump(CoordGrid(1944, 4959))
        player.opNpc3(foreman)
        advance()
        assertFalse(d.blast.allowed(player))
        player.inv[0] = InvObj(objs.coins, 1250)
        player.ring = InvObj(BlastObjs.charos)
        player.opNpc3(foreman)
        advance()
        assertEquals(0, player.count(objs.coins))
        assertTrue(d.blast.allowed(player))
        assertTrue(player.vars[BlastVars.paid] in 998..1000)
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.ObtainFreeBlastFurnacePermission))
        player.telejump(CoordGrid(3200, 3200))
        advance(2)
        val left = player.vars[BlastVars.paid]
        advance(10)
        assertEquals(left, player.vars[BlastVars.paid])
    }

    @Test
    fun GameTestState.normalBeltCoalThenIronProducesSteelWithHalfCoalAndExactXp() = game { d ->
        prepare()
        free(d)
        fund(d, 1000)
        val belt = belt()
        val before = player.statMap.getFineXP(stats.smithing)
        repeat(4) { player.inv[it] = InvObj(objs.coal) }
        player.opLoc1(belt)
        advance(3)
        assertEquals(
            4,
            player.vars[BlastBits.ores[8]],
            "pos=${player.coords},flags=${belt.forceApproachFlags},angle=${belt.angleId}",
        )
        repeat(4) { player.inv[it] = InvObj(objs.iron_ore) }
        player.opLoc1(belt)
        advance(4)
        assertEquals(4, player.vars[BlastBits.bars[2]])
        assertEquals(0, player.vars[BlastBits.ores[8]])
        assertEquals(0, player.count(objs.steel_bar))
        assertEquals(700, player.statMap.getFineXP(stats.smithing) - before)
        assertTrue(player.vars[BlastBits.coins] < 1000)
    }

    @Test
    fun GameTestState.ironWithoutCoalAlwaysSmeltsAndDoesNotConsumeForgingCharges() = game { d ->
        prepare()
        free(d)
        fund(d, 1000)
        player.ring = InvObj(objs.ring_of_forging)
        val belt = belt()
        repeat(5) { player.inv[it] = InvObj(objs.iron_ore) }
        val before = player.statMap.getFineXP(stats.smithing)
        player.opLoc1(belt)
        advance(4)
        assertEquals(5, player.vars[BlastBits.bars[1]])
        assertEquals(625, player.statMap.getFineXP(stats.smithing) - before)
        assertNotNull(player.ring)
    }

    @Test
    fun GameTestState.allEightMetalOutputsUseNativePersistentCountersAndPublishedXp() = game { d ->
        prepare()
        free(d)
        for (i in 0..7) set(BlastBits.ores[i], if (i == 2) 2 else 1)
        set(BlastBits.ores[8], 11)
        val before = player.statMap.getFineXP(stats.smithing)
        assertEquals(8, d.blast.process(player))
        set(BlastBits.ores[2], 1)
        assertEquals(1, d.blast.process(player))
        for (i in 0..7) assertEquals(if (i == 2) 2 else 1, player.vars[BlastBits.bars[i]], "bar $i")
        assertEquals(0, player.vars[BlastBits.ores[8]])
        assertEquals(2074, player.statMap.getFineXP(stats.smithing) - before)
    }

    @Test
    fun GameTestState.smeltingReadsGauntletsAtProductionAndCatalystsDoubleCoalBarXpOnly() =
        game { d ->
            prepare()
            free(d)
            set(BlastBits.ores[4], 2)
            set(BlastBits.ores[5], 2)
            set(BlastBits.ores[8], 4)
            set(BlastBits.catalysts, 2)
            player.hands = InvObj(objs.goldsmith_gauntlets)
            val before = player.statMap.getFineXP(stats.smithing)
            assertEquals(4, d.blast.process(player))
            assertEquals(2324, player.statMap.getFineXP(stats.smithing) - before)
            assertEquals(0, player.vars[BlastBits.catalysts])
            assertEquals(0, player.vars[BlastBits.ores[8]])
            assertEquals(2, player.vars[BlastBits.bars[4]])
            assertEquals(2, player.vars[BlastBits.bars[5]])
        }

    @Test
    fun GameTestState.beltRejectsPermissionAndLevelAndCapsCoalTinAndPrimaryOreWithoutLoss() =
        game { d ->
            prepare(30)
            val belt = belt()
            player.inv[0] = InvObj(objs.iron_ore)
            player.inv[1] = InvObj(objs.runite_ore)
            player.withProtectedAccess { assertEquals(0, d.blast.deposit(this, belt)) }
            player.inv[2] = InvObj(objs.coins, 2500)
            player.withProtectedAccess { assertTrue(d.blast.pay(this)) }
            set(BlastBits.ores[2], 28)
            set(BlastBits.ores[8], 253)
            set(BlastBits.ores[1], 254)
            player.inv[2] = InvObj(objs.coal)
            player.inv[3] = InvObj(objs.coal)
            player.inv[4] = InvObj(objs.tin_ore)
            player.withProtectedAccess { assertEquals(1, d.blast.deposit(this, belt)) }
            assertEquals(1, player.count(objs.coal))
            assertEquals(1, player.count(objs.tin_ore))
            assertEquals(1, player.count(objs.iron_ore))
            assertEquals(1, player.count(objs.runite_ore))
            assertEquals(254, player.vars[BlastBits.ores[8]])
        }

    @Test
    fun GameTestState.barCapacityPreservesUnprocessedOreAndCollectionResumesProduction() =
        game { d ->
            prepare()
            free(d)
            fund(d, 1000)
            set(BlastBits.ores[7], 2)
            set(BlastBits.ores[8], 8)
            set(BlastBits.bars[7], 27)
            assertEquals(1, d.blast.process(player))
            assertEquals(28, player.vars[BlastBits.bars[7]])
            assertEquals(1, player.vars[BlastBits.ores[7]])
            assertEquals(0, d.blast.process(player))
            player.hands = InvObj(objs.ice_gloves)
            player.withProtectedAccess { assertEquals(28, d.blast.collect(this)) }
            player.inv.indices.forEach { player.inv[it] = null }
            advance()
            assertEquals(1, player.vars[BlastBits.bars[7]])
            assertEquals(0, player.vars[BlastBits.ores[7]])
        }

    @Test
    fun GameTestState.hotBarsRequireCoolingAndWaterReturnsTheBucketBeforeNativeTake() = game { d ->
        prepare()
        free(d)
        set(BlastBits.ores[3], 3)
        d.blast.process(player)
        val loc = placeMapLoc(CoordGrid(1940, 4963), BlastLocs.dispenser)
        player.telejump(CoordGrid(1939, 4963))
        player.opLoc1(loc)
        advance()
        assertEquals(0, player.count(objs.silver_bar))
        assertEquals(3, player.vars[BlastBits.bars[3]])
        player.inv[0] = InvObj(objs.bucket_water)
        player.withProtectedAccess { assertTrue(d.blast.cool(this)) }
        assertEquals(1, player.count(objs.bucket_empty))
        assertEquals(0, player.count(objs.bucket_water))
        player.opLoc1(loc)
        advance()
        assertEquals(3, player.count(objs.silver_bar))
        assertEquals(0, player.vars[BlastBits.hot])
    }

    @Test
    fun GameTestState.iceAndImbuedSmithGlovesCollectHotBarsAndFullInventoryPreservesRemainder() =
        game { d ->
            prepare()
            free(d)
            set(BlastBits.bars[5], 5)
            set(BlastBits.hot, 2)
            repeat(26) { player.inv[it] = InvObj(objs.knife) }
            player.hands = InvObj(BlastObjs.smithIce)
            player.withProtectedAccess { assertEquals(2, d.blast.collect(this)) }
            assertEquals(3, player.vars[BlastBits.bars[5]])
            player.withProtectedAccess { assertEquals(0, d.blast.collect(this)) }
            player.inv.indices.forEach { player.inv[it] = null }
            player.hands = InvObj(objs.ice_gloves)
            player.withProtectedAccess { assertEquals(3, d.blast.collect(this)) }
            assertEquals(0, player.vars[BlastBits.bars[5]])
        }

    @Test
    fun GameTestState.cofferTransactionsAreAtomicAndDrainOnlyWhileInside() = game { d ->
        prepare()
        player.inv[0] = InvObj(objs.coins, 100)
        player.withProtectedAccess { assertEquals(100, d.blast.transferCoins(this, 500, true)) }
        advance(2)
        assertEquals(76, player.vars[BlastBits.coins])
        player.telejump(CoordGrid(3200, 3200))
        advance(5)
        assertEquals(76, player.vars[BlastBits.coins])
        repeat(28) { player.inv[it] = InvObj(objs.knife) }
        player.withProtectedAccess { assertEquals(0, d.blast.transferCoins(this, 76, false)) }
        assertEquals(76, player.vars[BlastBits.coins])
        player.inv[0] = null
        player.withProtectedAccess { assertEquals(76, d.blast.transferCoins(this, 500, false)) }
        assertEquals(0, player.vars[BlastBits.coins])
        assertEquals(0, player.vars[BlastBits.hasCoins])
    }

    @Test
    fun GameTestState.manualCokeRequiresToolsAndLevelsReturnsSpadeAndAwardsFiveFiremakingXp() =
        game { d ->
            prepare()
            player.stats[stats.firemaking] = 29
            player.inv[0] = InvObj(objs.spade)
            player.withProtectedAccess { assertFalse(d.blast.coke(this)) }
            player.stats[stats.firemaking] = 30
            val before = player.statMap.getFineXP(stats.firemaking)
            val coke = placeMapLoc(CoordGrid(1950, 4964), BlastLocs.coke)
            player.telejump(CoordGrid(1950, 4963))
            player.opLoc1(coke)
            advance()
            assertEquals(1, player.count(BlastObjs.coke))
            assertEquals(0, player.count(objs.spade))
            val stove = placeMapLoc(CoordGrid(1948, 4963), BlastLocs.stoves[0])
            player.telejump(CoordGrid(1950, 4963))
            player.opLoc1(stove)
            advance()
            assertEquals(0, player.count(BlastObjs.coke))
            assertEquals(1, player.count(objs.spade))
            assertEquals(50, player.statMap.getFineXP(stats.firemaking) - before)
        }

    @Test
    fun GameTestState.manualPumpAwardsOnlyStrengthAndWalkingCancelsItImmediately() = game { d ->
        prepare()
        player.stats[stats.strength] = 30
        player.inv[0] = InvObj(objs.spade)
        player.withProtectedAccess {
            assertTrue(d.blast.coke(this))
            assertTrue(d.blast.refuel(this))
        }
        val pump =
            placeMapLoc(
                CoordGrid(1950, 4961),
                BlastLocs.pump,
                LocShape.CentrepieceStraight,
                LocAngle.North,
            )
        player.telejump(CoordGrid(1951, 4961))
        val hp = player.statMap.getFineXP(stats.hitpoints)
        val before = player.statMap.getFineXP(stats.strength)
        player.opLoc1(pump)
        advance(4)
        assertTrue(player.statMap.getFineXP(stats.strength) > before)
        player.moveGameClick(CoordGrid(1955, 4960))
        advance(4)
        val stopped = player.statMap.getFineXP(stats.strength)
        advance(5)
        assertEquals(stopped, player.statMap.getFineXP(stats.strength))
        assertEquals(hp, player.statMap.getFineXP(stats.hitpoints))
        assertEquals(CoordGrid(1955, 4960), player.coords)
    }

    @Test
    fun GameTestState.nativeFactoryStairsProvideOrdinaryAccessAndVisitedState() = game { d ->
        prepare()
        val down =
            placeMapLoc(
                CoordGrid(2930, 10196),
                BlastLocs.down,
                LocShape.CentrepieceStraight,
                LocAngle.North,
            )
        player.telejump(CoordGrid(2931, 10196))
        player.opLoc1(down)
        advance()
        assertEquals(CoordGrid(1940, 4957), player.coords)
        assertEquals(1, player.vars[BlastBits.visited])
        val up =
            placeMapLoc(
                CoordGrid(1939, 4956),
                BlastLocs.up,
                LocShape.CentrepieceStraight,
                LocAngle.East,
            )
        player.telejump(CoordGrid(1939, 4958))
        player.opLoc1(up)
        advance()
        assertEquals(CoordGrid(2931, 10197), player.coords)
    }

    @Test
    fun GameTestState.cooperatingManualStationsSmeltWithoutCoinsAndShareContributionXp() =
        game { d ->
            prepare()
            free(d)
            player.inv[0] = InvObj(objs.spade)
            player.withProtectedAccess {
                assertTrue(d.blast.coke(this))
                assertTrue(d.blast.refuel(this))
            }
            set(BlastBits.ores[4], 1)
            val pump =
                placeMapLoc(
                    CoordGrid(1950, 4961),
                    BlastLocs.pump,
                    LocShape.CentrepieceStraight,
                    LocAngle.North,
                )
            val pedal =
                placeMapLoc(
                    CoordGrid(1947, 4966),
                    BlastLocs.pedals,
                    LocShape.CentrepieceStraight,
                    LocAngle.North,
                )
            val other = registerPlayer(CoordGrid(1951, 4961))
            other.members = true
            other.stats[stats.strength] = 30
            player.telejump(CoordGrid(1947, 4965))
            val before = player.statMap.getFineXP(stats.smithing)
            other.withProtectedAccess { d.blast.operate(this, pump, true) }
            player.withProtectedAccess { d.blast.operate(this, pedal, false) }
            random.next = 128
            repeat(30) { random.then = 128 }
            advance(5)
            assertEquals(1, player.vars[BlastBits.bars[4]])
            assertEquals(240, player.statMap.getFineXP(stats.smithing) - before)
            assertEquals(15, other.statMap.getFineXP(stats.smithing))
            assertEquals(0, player.vars[BlastBits.coins])
            player.cancelActiveCoroutine()
            other.cancelActiveCoroutine()
        }

    @Test
    fun GameTestState.brokenMachineryRequiresHammerAndCraftingThenAwardsXpOnce() = game { d ->
        prepare()
        player.stats[stats.crafting] = 29
        val normal = placeMapLoc(BlastFurnace.PART_COORDS[2], BlastLocs.normalParts[2])
        assertTrue(d.blast.breakPart(2))
        val broken =
            BoundLocInfo(
                requireNotNull(d.locs.findExact(normal.coords, BlastLocs.brokenParts[2])),
                d.types[BlastLocs.brokenParts[2]],
            )
        player.telejump(broken.coords.translateZ(-1))
        player.inv[0] = InvObj(objs.hammer)
        player.withProtectedAccess { assertFalse(d.blast.repair(this, broken)) }
        player.stats[stats.crafting] = 30
        val before = player.statMap.getFineXP(stats.crafting)
        player.withProtectedAccess { d.blast.repair(this, broken) }
        player.inv[0] = null
        advance(4)
        assertNotNull(d.locs.findExact(normal.coords, BlastLocs.brokenParts[2]))
        assertEquals(before, player.statMap.getFineXP(stats.crafting))
        player.inv[0] = InvObj(objs.hammer)
        player.withProtectedAccess { assertTrue(d.blast.repair(this, broken)) }
        advance(4)
        assertEquals(500, player.statMap.getFineXP(stats.crafting) - before)
        assertNotNull(d.locs.findExact(normal.coords, BlastLocs.normalParts[2]))
        player.withProtectedAccess { assertFalse(d.blast.repair(this, broken)) }
        assertEquals(500, player.statMap.getFineXP(stats.crafting) - before)
    }

    @Test
    fun GameTestState.blastFurnaceAnvilRequiresUnboosted60IndependentlyOfPaidPermission() =
        game { d ->
            prepare(59)
            player.setCurrentLevel(stats.smithing, 99)
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(objs.bronze_bar)
            val anvil = placeMapLoc(CoordGrid(1956, 4964), AnvilLocs.ordinary.first())
            player.telejump(anvil.coords.translateX(-1))
            player.withProtectedAccess { d.anvil.open(this, anvil, objs.bronze_bar) }
            assertFalse(AnvilInterfaces.smithing in player.ui)
            player.stats[stats.smithing] = 60
            player.withProtectedAccess { d.anvil.open(this, anvil, objs.bronze_bar) }
            assertTrue(AnvilInterfaces.smithing in player.ui)
        }

    @Test
    fun GameTestState.ordanHasAllSevenDatedOresAndNativeTradeOperation() = game { d ->
        assertEquals(1560, BlastNpcs.ordan.id)
        assertEquals("Trade", d.npcs[BlastNpcs.ordan].op[2])
        val stock = requireNotNull(d.invs[BlastInvs.shop].stock).filterNotNull()
        assertEquals(7, stock.size)
        for (ore in
            listOf(
                objs.copper_ore,
                objs.tin_ore,
                objs.iron_ore,
                objs.mithril_ore,
                objs.silver_ore,
                objs.gold_ore,
                objs.coal,
            )) {
            val item = stock.single { it.obj == ore.id }
            assertEquals(100, item.count)
            assertEquals(200, item.restockCycles)
        }
    }

    private fun GameTestScope.belt(): BoundLocInfo {
        val loc = placeMapLoc(CoordGrid(1943, 4967), BlastLocs.belt)
        player.telejump(CoordGrid(1942, 4967))
        return loc
    }

    private fun GameTestScope.free(d: Deps) {
        player.withProtectedAccess { assertTrue(d.blast.permission(this)) }
    }

    private fun GameTestScope.fund(d: Deps, count: Int) {
        player.inv[27] = InvObj(objs.coins, count)
        player.withProtectedAccess { assertEquals(count, d.blast.transferCoins(this, count, true)) }
    }

    private fun GameTestScope.set(bit: VarBitType, value: Int) {
        VarPlayerIntMapSetter.set(player, bit, value)
    }

    private fun GameTestScope.prepare(level: Int = 99) {
        player.members = true
        player.stats[stats.smithing] = level
        player.stats[stats.agility] = 99
        player.stats[stats.firemaking] = 99
        player.stats[stats.strength] = 99
        player.telejump(CoordGrid(1940, 4957))
    }

    private fun GameTestScope.choose(option: Int) {
        val modal =
            requireNotNull(player.ui.modals.getComponent(UserInterface(interfaces.chatmenu.id)))
        player.ui.queueClose(modal)
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, option))
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, null, BlastFurnace::class) { d ->
            for (c in
                listOf(
                    CoordGrid(1940, 4960),
                    CoordGrid(2930, 10196),
                    CoordGrid(3200, 3200),
                )) for (dx in -16..24 step 8) for (dz in -16..24 step 8) allocZoneCollision(
                c.translate(dx, dz)
            )
            body(d)
        }

    data class Deps
    @Inject
    constructor(
        val blast: BlastFurnace,
        val npcs: NpcTypeList,
        val locs: LocRepository,
        val types: LocTypeList,
        val invs: InvTypeList,
        val anvil: AnvilSmithing,
    )
}
