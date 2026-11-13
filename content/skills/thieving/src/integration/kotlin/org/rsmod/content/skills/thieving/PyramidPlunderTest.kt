package org.rsmod.content.skills.thieving.pyramid

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.NpcHitScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.events.interact.LocUEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.util.Rotations

@Execution(ExecutionMode.SAME_THREAD)
class PyramidPlunderTest {
    @Test
    fun GameTestState.nativeCatalogueAndMapPinExactRoomsUrnsAndDiaryRows() = game { d ->
        assertEquals(
            listOf(13, 13, 12, 13, 13, 13, 13, 13),
            (0..7).map { room -> PyramidRooms.urns.values.count { it.first == room } },
        )
        assertTrue(DiaryRegion.Desert.tasks(DiaryDifficulty.Easy)[8].text.contains("sarcophagus"))
        assertTrue(
            DiaryRegion.Desert.tasks(DiaryDifficulty.Elite)[4].text.contains("Grand Gold Chest")
        )
        assertEquals(4605, d.objects[PyramidObjs.charm].id)
    }

    @Test
    fun GameTestState.membershipAndUnboostedTwentyOneAreRequired() = game { d ->
        prepare(20)
        player.setCurrentLevel(stats.thieving, 99)
        player.withProtectedAccess { assertFalse(d.game.start(this)) }
        player.stats[stats.thieving] = 99
        player.members = false
        player.withProtectedAccess { assertFalse(d.game.start(this)) }
        assertNull(d.game.currentRoom(player))
    }

    @Test
    fun GameTestState.nativeGuardianStartOpCreatesFirstRoomAndFiveMinuteExpiry() = game { d ->
        prepare()
        player.telejump(PyramidRooms.lobby.translate(0, -1))
        val guardian = spawnNpc(CoordGrid(1934, 4426, 3), d.npcs[PyramidNpcs.guardian])
        zeros(8)
        player.opNpc3(guardian)
        advance()
        assertEquals(0, d.game.currentRoom(player))
        assertEquals(PyramidRooms.all[0].entry, player.coords)
        assertTrue(PyramidInterfaces.overlay in player.ui)
        advance(480)
        assertNotNull(d.game.currentRoom(player))
        advance(22)
        assertNull(d.game.currentRoom(player))
        assertEquals(PyramidRooms.outside, player.coords)
    }

    @Test
    fun GameTestState.nativeFirstSarcophagusAwardsStrengthOnlyAndEasyDiaryOnce() = game { d ->
        begin(d)
        pass(d)
        val thieving = player.statMap.getFineXP(stats.thieving)
        val strength = player.statMap.getFineXP(stats.strength)
        val sarc = place(PyramidLocs.sarcophagus, SARCS[0])
        near(sarc)
        random.next = 0
        random.then = 1
        random.then = 0
        player.opLoc1(sarc)
        advance(7)
        assertEquals(strength + 200, player.statMap.getFineXP(stats.strength))
        assertEquals(thieving, player.statMap.getFineXP(stats.thieving))
        assertEquals(1, player.count(PyramidObjs.potteryScarab))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.OpenFirstPyramidSarcophagus))
        player.opLoc1(sarc)
        advance(7)
        assertEquals(1, player.count(PyramidObjs.potteryScarab))
    }

    @Test
    fun GameTestState.closedCheckedAndCharmedUrnsHaveSameTotalXpAndNoRepeatLoot() = game { d ->
        begin(d)
        pass(d)
        val entries = PyramidRooms.urns.filterValues { it.first == 0 }.entries.take(2)
        val (c, info) = entries[0]
        val urn = placeMapLoc(c, PyramidLocs.urns[info.second])
        near(urn)
        val xp = player.statMap.getFineXP(stats.thieving)
        random.next = 0
        random.then = 0
        player.opLoc1(urn)
        advance(3)
        assertEquals(xp + 600, player.statMap.getFineXP(stats.thieving))
        player.opLoc1(urn)
        advance(3)
        assertEquals(xp + 600, player.statMap.getFineXP(stats.thieving))
        val (c2, info2) = PyramidRooms.urns.entries.first { it.key == CoordGrid(1926, 4465) }
        val next = placeMapLoc(c2, PyramidLocs.urns[info2.second])
        near(next)
        player.inv[27] = InvObj(PyramidObjs.charm)
        random.next = 0
        player.opLoc2(next)
        advance(3)
        assertEquals(2, player.vars[PyramidBits.urns[info2.second]])
        player.opLoc3(next)
        advance(4)
        assertEquals(3, player.vars[PyramidBits.urns[info2.second]])
        random.next = 0
        player.opLoc1(next)
        advance(3)
        assertEquals(xp + 1200, player.statMap.getFineXP(stats.thieving))
    }

    @Test
    fun GameTestState.urnFailureDamagesAndPoisonsWithoutRewardOrDiary() = game { d ->
        begin(d)
        pass(d)
        val (c, info) = PyramidRooms.urns.entries.first { it.value.first == 0 }
        val urn = placeMapLoc(c, PyramidLocs.urns[info.second])
        near(urn)
        val xp = player.statMap.getFineXP(stats.thieving)
        random.next = 99
        random.then = 4
        player.opLoc1(urn)
        advance(4)
        assertEquals(95, player.stats[stats.hitpoints])
        assertEquals(20, player.vars[varps.poison])
        assertEquals(xp, player.statMap.getFineXP(stats.thieving))
        assertEquals(0, player.vars[PyramidBits.urns[info.second]])
    }

    @Test
    fun GameTestState.boostCannotUnlockNextRoomAndLockpickHalvesDoorXp() = game { d ->
        begin(d, 21)
        pass(d)
        player.setCurrentLevel(stats.thieving, 99)
        player.inv[0] = InvObj(PyramidObjs.lockpick)
        val door = place(PyramidLocs.doors[0], DOORS[0])
        near(door)
        val xp = player.statMap.getFineXP(stats.thieving)
        random.next = 0
        player.withProtectedAccess { assertFalse(d.game.door(this, door, 0)) }
        advance(3)
        assertEquals(0, d.game.currentRoom(player))
        assertEquals(xp + 200, player.statMap.getFineXP(stats.thieving))
    }

    @Test
    fun GameTestState.allEightRoomsAndNativeFinalChestGrantExactEliteCredit() = game { d ->
        begin(d)
        repeat(7) { room ->
            assertEquals(room, d.game.currentRoom(player))
            pass(d)
            val door = place(PyramidLocs.doors[0], DOORS[room])
            near(door)
            random.next = 0
            player.withProtectedAccess { assertTrue(d.game.door(this, door, 0)) }
            advance(3)
        }
        assertEquals(7, d.game.currentRoom(player))
        pass(d)
        val chest = place(PyramidLocs.chest, CHESTS[7])
        near(chest)
        val xp = player.statMap.getFineXP(stats.thieving)
        random.next = 0
        random.then = 1
        random.then = 0
        player.opLoc1(chest)
        advance(3)
        assertEquals(xp + 5500, player.statMap.getFineXP(stats.thieving))
        assertEquals(1, player.count(PyramidObjs.goldScarab))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.LootFinalPyramidChest))
    }

    @Test
    fun GameTestState.sceptreRareRollExitsAndDropsUnderFullInventory() = game { d ->
        begin(d)
        pass(d)
        for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.knife)
        val chest = place(PyramidLocs.chest, CHESTS[0])
        near(chest)
        random.next = 0
        random.then = 0
        player.opLoc1(chest)
        advance(3)
        assertNull(d.game.currentRoom(player))
        assertEquals(PyramidRooms.outside, player.coords)
        assertTrue(
            d.ground.findAll(PyramidRooms.outside).any {
                it.type == PyramidObjs.initial.id && it.ownerId == player.observerUUID
            }
        )
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.LootFinalPyramidChest))
    }

    @Test
    fun GameTestState.logoutAndAcceptedTeleportEndRunAndClearOverlay() = game { d ->
        begin(d)
        d.events.publish(PlayerTeleportEvent.Start(player, CoordGrid(3200, 3200)))
        assertNull(d.game.currentRoom(player))
        assertFalse(PyramidInterfaces.overlay in player.ui)
        begin(d)
        d.events.publish(SessionStateEvent.Logout(player))
        assertNull(d.game.currentRoom(player))
        assertEquals(PyramidRooms.outside, player.coords)
    }

    @Test
    fun GameTestState.nativeWornSceptreJalsavrahUsesOneChargeAndLeavesRemoveAvailable() =
        game { d ->
            prepare()
            player.worn[3] = InvObj(PyramidObjs.initial)
            player.withProtectedAccess { d.worn.interact(this, player.worn, 3, IfButtonOp.Op2) }
            advance(5)
            assertEquals(PyramidRooms.lobby, player.coords)
            assertEquals(2, d.sceptre.charges(player.worn[3]!!))
            player.withProtectedAccess { d.worn.interact(this, player.worn, 3, IfButtonOp.Op1) }
            assertNull(player.worn[3])
        }

    @Test
    fun GameTestState.sceptreNecropolisAndTeleblockRestrictionsPreserveCharges() = game { d ->
        prepare()
        player.inv[0] = InvObj(PyramidObjs.initial)
        player.withProtectedAccess { assertFalse(d.sceptre.teleport(this, inv, 0, 3)) }
        VarPlayerIntMapSetter.set(player, varbits.teleblock_cycles, 101)
        player.withProtectedAccess { assertFalse(d.sceptre.teleport(this, inv, 0, 0)) }
        assertEquals(3, d.sceptre.charges(player.inv[0]!!))
    }

    @Test
    fun GameTestState.moneyPotCoinUseProducesOrdinaryReusableCharmAndBasket() = game { d ->
        prepare()
        val pot = placeMapLoc(CoordGrid(3355, 2953), PyramidLocs.money)
        near(pot)
        player.inv[0] = InvObj(objs.coins, 2)
        player.withProtectedAccess {
            d.events.publish(
                this,
                LocUEvents.Op(pot, pot, d.locs[PyramidLocs.money], d.objects[objs.coins], 0),
            )
        }
        assertEquals(0, player.count(objs.coins))
        assertEquals(1, player.count(PyramidObjs.charm))
        assertEquals(1, player.count(PyramidObjs.basket))
    }

    @Test
    fun GameTestState.charmPurchaseRejectsBankedCharmAndPreservesFeeWhenOutputsDoNotFit() =
        game { d ->
            prepare()
            player.inv[0] = InvObj(objs.coins, 2)
            player.withProtectedAccess {
                inv(invs.bank)[0] = InvObj(PyramidObjs.charm)
                assertFalse(d.sceptre.buyCharm(this))
            }
            assertEquals(2, player.count(objs.coins))
            player.withProtectedAccess { inv(invs.bank)[0] = null }
            for (slot in 1..27) player.inv[slot] = InvObj(objs.knife)
            player.withProtectedAccess { assertFalse(d.sceptre.buyCharm(this)) }
            assertEquals(2, player.count(objs.coins))
            player.inv[27] = null
            player.withProtectedAccess { assertTrue(d.sceptre.buyCharm(this)) }
            assertEquals(1, player.count(PyramidObjs.charm))
            assertEquals(1, player.count(PyramidObjs.basket))
        }

    @Test
    fun GameTestState.nativeNecropolisCommuneRequiresCarriedSceptreAndUnlocksItsFourthTeleport() =
        game { d ->
            prepare()
            val obelisk =
                placeMapLoc(
                    CoordGrid(3316, 2718),
                    PyramidLocs.obelisk,
                    LocShape.CentrepieceStraight,
                    LocAngle.North,
                )
            player.telejump(CoordGrid(3315, 2718))
            player.opLoc1(obelisk)
            advance()
            assertEquals(0, player.vars[PyramidBits.necropolis])
            player.inv[0] = InvObj(PyramidObjs.initial)
            player.opLoc1(obelisk)
            advance()
            assertEquals(1, player.vars[PyramidBits.necropolis])
            player.withProtectedAccess { assertTrue(d.sceptre.teleport(this, inv, 0, 3)) }
            advance(5)
            assertEquals(CoordGrid(3313, 2718), player.coords)
            assertEquals(2, d.sceptre.charges(player.inv[0]!!))
        }

    @Test
    fun GameTestState.mummyRechargeAcceptsMixedGoldShapesAndNotesAndRespectsClaimedDiaryCap() =
        game { d ->
            prepare()
            player.inv[0] = InvObj(PyramidObjs.sceptre)
            repeat(3) { player.inv[it + 1] = InvObj(PyramidObjs.goldSeal) }
            val note = d.objects.getValue(d.objects[PyramidObjs.goldScarab].certlink)
            player.inv[4] = InvObj(note, 3)
            player.withProtectedAccess { assertTrue(d.sceptre.recharge(this)) }
            choose(1)
            advance()
            assertEquals(3, d.sceptre.charges(player.inv[0]!!))
            assertEquals(0, player.count(PyramidObjs.goldSeal) + player.count(note))
            VarPlayerIntMapSetter.set(
                player,
                DiaryRegion.Desert.tier(DiaryDifficulty.Easy).reward,
                1,
            )
            repeat(6) { player.inv[it + 1] = InvObj(PyramidObjs.goldSeal) }
            player.withProtectedAccess { assertTrue(d.sceptre.recharge(this)) }
            choose(1)
            advance()
            assertEquals(10, d.sceptre.charges(player.inv[0]!!))
            assertEquals(0, player.count(PyramidObjs.goldSeal))
        }

    @Test
    fun GameTestState.rechargeFailureKeepsToolAndArtefactsAndAllDiaryCapacitiesMatch() = game { d ->
        prepare()
        player.inv[0] = InvObj(PyramidObjs.sceptre)
        player.inv[1] = InvObj(PyramidObjs.goldSeal)
        player.withProtectedAccess { assertFalse(d.sceptre.recharge(this)) }
        choose(1)
        advance()
        assertEquals(PyramidObjs.sceptre.id, player.inv[0]?.id)
        assertEquals(1, player.count(PyramidObjs.goldSeal))
        for ((tier, max) in
            listOf(
                DiaryDifficulty.Easy to 10,
                DiaryDifficulty.Medium to 25,
                DiaryDifficulty.Hard to 50,
                DiaryDifficulty.Elite to 100,
            )) {
            VarPlayerIntMapSetter.set(player, DiaryRegion.Desert.tier(tier).reward, 1)
            player.withProtectedAccess { assertEquals(max, d.sceptre.capacity(this)) }
        }
    }

    @Test
    fun GameTestState
        .sceptreWorksAtThirtyWildernessAndDuringTeleblockImmunityButRejectsThirtyOne() = game { d ->
        prepare()
        player.inv[0] = InvObj(PyramidObjs.initial)
        player.telejump(CoordGrid(3200, 3760))
        VarPlayerIntMapSetter.set(player, varbits.inside_wilderness, 1)
        player.withProtectedAccess { assertFalse(d.sceptre.teleport(this, inv, 0, 0)) }
        assertEquals(3, d.sceptre.charges(player.inv[0]!!))
        player.telejump(CoordGrid(3200, 3752))
        VarPlayerIntMapSetter.set(player, varbits.teleblock_cycles, 100)
        player.withProtectedAccess { assertTrue(d.sceptre.teleport(this, inv, 0, 0)) }
        advance(5)
        assertEquals(PyramidRooms.lobby, player.coords)
        assertEquals(2, d.sceptre.charges(player.inv[0]!!))
    }

    @Test
    fun GameTestState.swarmIncomingHitsAreOwnerOnlyAndActualCreditedDeathDropsBonesOnce() =
        game { d ->
            begin(d)
            pass(d)
            val chest = place(PyramidLocs.chest, CHESTS[0])
            near(chest)
            random.next = 99
            random.then = 1
            random.then = 0
            random.then = 1
            player.opLoc1(chest)
            advance(3)
            val swarm =
                npcRepo.findAll(org.rsmod.map.zone.ZoneKey.from(player.coords), 1).first {
                    it.id == PyramidNpcs.swarm.id
                }
            val other = registerPlayer(player.coords.translate(2, 0))
            other.members = true
            swarm.queueHit(other, 1, HitType.Melee, 10)
            advance()
            assertEquals(25, swarm.hitpoints)
            d.events.publish(NpcKillEvents.Credited(other, swarm))
            swarm.queueHit(player, 1, HitType.Melee, 25)
            advance()
            assertEquals(0, swarm.hitpoints)
            assertEquals(1, d.ground.findAll(swarm.coords).count { it.type == objs.bones.id })
            d.events.publish(NpcKillEvents.Credited(player, swarm))
            assertEquals(1, d.ground.findAll(swarm.coords).count { it.type == objs.bones.id })
            advance(12)
        }

    @Test
    fun GameTestState.mummyDeathPublishesAndCommitsAConvertedBoneExactlyOnce() = game { d ->
        begin(d)
        pass(d)
        player.stats[stats.strength] = 1
        val sarc = place(PyramidLocs.sarcophagus, SARCS[0])
        near(sarc)
        random.next = 99
        random.then = 1
        random.then = 0
        player.opLoc1(sarc)
        advance(7)
        val mummy =
            npcRepo.findAll(org.rsmod.map.zone.ZoneKey.from(player.coords), 1).first { n ->
                PyramidNpcs.mummies.any { it.id == n.id }
            }
        var published = 0
        var committed = 0
        d.events.subscribeUnbound(RemainsDrop::class.java) {
            if (this.player === this@game.player && obj.id == objs.bones.id) {
                published++
                convert(1) { committed++ }
            }
        }
        mummy.queueHit(player, 1, HitType.Melee, 90)
        advance()
        assertEquals(1, published)
        assertEquals(1, committed)
        assertEquals(0, d.ground.findAll(mummy.coords).count { it.type == objs.bones.id })
        d.events.publish(NpcKillEvents.Credited(player, mummy))
        assertEquals(1, published)
        advance(12)
    }

    private fun GameTestScope.choose(option: Int) {
        val modal =
            requireNotNull(
                player.ui.modals.getComponent(
                    UserInterface(org.rsmod.api.config.refs.interfaces.chatmenu.id)
                )
            )
        player.ui.queueClose(modal)
        player.resumeActiveCoroutine(ResumePauseButtonInput(components.chatmenu_pbutton, option))
    }

    private fun GameTestScope.prepare(level: Int = 99) {
        player.observerUUID = 1L
        player.members = true
        player.stats[stats.thieving] = level
        player.stats[stats.strength] = 99
        player.stats[stats.hitpoints] = 99
        player.telejump(PyramidRooms.lobby)
    }

    private fun GameTestScope.zeros(n: Int) {
        random.next = 0
        repeat(n - 1) { random.then = 0 }
    }

    private fun GameTestScope.begin(d: Deps, level: Int = 99) {
        prepare(level)
        zeros(8)
        player.withProtectedAccess { assertTrue(d.game.start(this)) }
    }

    private fun GameTestScope.pass(d: Deps) {
        val room = requireNotNull(d.game.currentRoom(player))
        val trap = place(PyramidLocs.spear, SPIKES[room])
        near(trap)
        random.next = 0
        player.withProtectedAccess { assertTrue(d.game.pass(this, trap)) }
        advance(3)
        assertEquals(0, player.vars[PyramidBits.trap])
    }

    private fun GameTestScope.place(type: LocType, fx: Fx): BoundLocInfo =
        placeMapLoc(fx.c, type, fx.shape, fx.angle)

    private fun GameTestScope.near(loc: BoundLocInfo) {
        val flags = Rotations.rotate(loc.angleId, loc.forceApproachFlags)
        val c = loc.coords
        player.telejump(
            when {
                flags and 1 == 0 -> c.translate(0, loc.adjustedLength)
                flags and 2 == 0 -> c.translate(loc.adjustedWidth, 0)
                flags and 4 == 0 -> c.translate(0, -1)
                else -> c.translate(-1, 0)
            }
        )
    }

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            null,
            PyramidPlunder::class,
            PlayerHitScript::class,
            NpcHitScript::class,
            NpcDeathScript::class,
        ) { d ->
            for (level in 0..3) for (x in 1920..1983 step 8) for (z in
                4416..4479 step 8) allocZoneCollision(CoordGrid(x, z, level))
            for (c in PharaohSceptre.DESTINATIONS + PyramidRooms.outside) for (x in
                -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(c.translate(x, z))
            body(d)
        }

    data class Deps
    @Inject
    constructor(
        val game: PyramidPlunder,
        val sceptre: PharaohSceptre,
        val npcs: NpcTypeList,
        val locs: LocTypeList,
        val objects: ObjTypeList,
        val ground: ObjRepository,
        val events: EventBus,
        val worn: WornInteractions,
    )

    private data class Fx(val c: CoordGrid, val shape: LocShape, val angle: LocAngle)

    private companion object {
        val SPIKES =
            listOf(
                Fx(CoordGrid(1926, 4472), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1953, 4473), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1975, 4465), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1930, 4451), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1961, 4443), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1925, 4427), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1943, 4424), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1973, 4423), LocShape.CentrepieceStraight, LocAngle.East),
            )
        val DOORS =
            listOf(
                Fx(CoordGrid(1924, 4472), LocShape.WallStraight, LocAngle.South),
                Fx(CoordGrid(1957, 4472), LocShape.WallStraight, LocAngle.South),
                Fx(CoordGrid(1980, 4458), LocShape.WallStraight, LocAngle.West),
                Fx(CoordGrid(1937, 4459), LocShape.WallStraight, LocAngle.South),
                Fx(CoordGrid(1952, 4447), LocShape.WallStraight, LocAngle.East),
                Fx(CoordGrid(1923, 4432), LocShape.WallStraight, LocAngle.East),
                Fx(CoordGrid(1946, 4433), LocShape.WallStraight, LocAngle.South),
                Fx(CoordGrid(1969, 4427), LocShape.WallStraight, LocAngle.North),
            )
        val CHESTS =
            listOf(
                Fx(CoordGrid(1927, 4468), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1953, 4468), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1975, 4458), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1936, 4452), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1955, 4450), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1926, 4434), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1951, 4427), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1972, 4431), LocShape.CentrepieceStraight, LocAngle.West),
            )
        val SARCS =
            listOf(
                Fx(CoordGrid(1927, 4464), LocShape.CentrepieceStraight, LocAngle.East),
                Fx(CoordGrid(1949, 4468), LocShape.CentrepieceStraight, LocAngle.South),
                Fx(CoordGrid(1971, 4455), LocShape.CentrepieceStraight, LocAngle.South),
                Fx(CoordGrid(1933, 4456), LocShape.CentrepieceStraight, LocAngle.South),
                Fx(CoordGrid(1954, 4446), LocShape.CentrepieceStraight, LocAngle.West),
                Fx(CoordGrid(1928, 4431), LocShape.CentrepieceStraight, LocAngle.North),
                Fx(CoordGrid(1948, 4431), LocShape.CentrepieceStraight, LocAngle.West),
                Fx(CoordGrid(1976, 4432), LocShape.CentrepieceStraight, LocAngle.North),
            )
    }
}
