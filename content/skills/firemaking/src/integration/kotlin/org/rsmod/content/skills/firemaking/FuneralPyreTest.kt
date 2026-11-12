package org.rsmod.content.skills.firemaking

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FuneralPyreTest {
    @Test
    fun GameTestState.`cache 233 pins every funeral pyre remain key and presentation type`() =
        runFuneralPyreTest { deps ->
            assertEquals(4489, varbits.morytania_diary_hard_complete.id)
            assertEquals(4490, varbits.morytania_diary_elite_complete.id)
            assertEquals(1303, seqs.temple_pyre_nowood.id)
            assertEquals(1304, seqs.temple_pyre_nobones.id)
            assertEquals(1305, seqs.temple_pyre_fire.id)
            assertEquals(292, spotanims.temple_pyre_fire.id)
            assertEquals(4093, firemaking_locs.funeralPyre.id)
            assertEquals(4065, firemaking_locs.stoneStand.id)
            assertEquals(
                listOf(
                    "3438:4094:4100:5:50.0",
                    "3440:4095:4101:20:70.0",
                    "3442:4096:4102:35:100.0",
                    "6211:9006:9008:40:120.0",
                    "10808:21271:21272:47:158.0",
                    "3444:4097:4103:50:175.0",
                    "6213:9007:9009:55:210.0",
                    "3446:4098:4104:65:255.0",
                    "3448:4099:4105:80:404.5",
                    "19672:28865:28866:95:500.0",
                ),
                FuneralPyreLogs.all.map {
                    "${it.obj.id}:${it.logLoc.id}:${it.remainsLoc.id}:${it.level}:${it.firemakingXp}"
                },
            )
            assertEquals(
                listOf(3396, 3398, 3400, 3402, 3404, 25419),
                ShadeRemains.all.map { it.obj.id },
            )
            assertEquals("Build", locTypes[firemaking_locs.funeralPyre].op[0])
            assertTrue(FuneralPyreLogs.all.all { locTypes[it.logLoc].op[0] == "Use" })
            assertTrue(FuneralPyreLogs.all.all { locTypes[it.remainsLoc].op[0] == "Light" })
            assertTrue(FuneralPyreLogs.all.all { deps.objTypes[it.obj].members })
            assertTrue(ShadeRemains.all.all { deps.objTypes[it.obj].members })
        }

    @Test
    fun GameTestState
        .`all valid log and remains combinations consume materials and award exact xp`() =
        runFuneralPyreTest { deps ->
            var index = 0
            for (log in FuneralPyreLogs.all) {
                for (remains in ShadeRemains.all.filter { it.prayerXp(log) != null }) {
                    preparePlayer()
                    add(log.obj)
                    add(remains.obj)
                    add(objs.tinderbox)
                    val pyre = preparePyre(index++)
                    val expectedPrayer = requireNotNull(remains.prayerXp(log))

                    player.withProtectedAccess {
                        assertTrue(deps.pyre.build(this, pyre, slotOf(log.obj), log))
                    }
                    val logStage = currentPyre(pyre.coords, log.logLoc)
                    assertEquals(seqs.temple_pyre_nowood.id, player.pendingSequence.id)
                    advance()
                    player.withProtectedAccess {
                        assertTrue(
                            deps.pyre.addRemains(this, logStage, slotOf(remains.obj), log, remains)
                        )
                    }
                    val remainsStage = currentPyre(pyre.coords, log.remainsLoc)
                    assertEquals(seqs.temple_pyre_nobones.id, player.pendingSequence.id)
                    advance()
                    random.next = 99
                    random.then = 0
                    player.withProtectedAccess {
                        assertTrue(deps.pyre.light(this, remainsStage, log))
                    }

                    assertEquals(0, player.count(log.obj))
                    assertEquals(0, player.count(remains.obj))
                    assertEquals(1, player.count(objs.tinderbox))
                    assertEquals((log.firemakingXp * 10).toInt(), fineXp(stats.firemaking))
                    assertEquals((expectedPrayer * 10).toInt(), fineXp(stats.prayer))
                    assertEquals(seqs.temple_pyre_fire.id, player.pendingSequence.id)
                    assertNotNull(findLoc(pyre.coords, firemaking_locs.funeralPyre))
                }
            }
        }

    @Test
    fun GameTestState.`left click stages prioritise best compatible log and remains`() =
        runFuneralPyreTest { deps ->
            preparePlayer()
            add(objs.magic_pyre_logs)
            add(objs.redwood_pyre_logs)
            add(objs.loar_remains)
            add(objs.urium_remains)
            val pyre = preparePyre(40)

            player.withProtectedAccess { assertTrue(deps.pyre.buildBest(this, pyre)) }
            assertEquals(1, player.count(objs.magic_pyre_logs))
            assertEquals(0, player.count(objs.redwood_pyre_logs))
            val logStage = currentPyre(pyre.coords, firemaking_locs.funeralPyreRedwood)

            player.withProtectedAccess {
                assertTrue(
                    deps.pyre.addBestRemains(
                        this,
                        logStage,
                        FuneralPyreLogs.byLogLoc.getValue(logStage.id),
                    )
                )
            }
            assertEquals(1, player.count(objs.loar_remains))
            assertEquals(0, player.count(objs.urium_remains))
            assertNotNull(findLoc(pyre.coords, firemaking_locs.funeralPyreRemainsRedwood))
        }

    @Test
    fun GameTestState.`membership quest level tinderbox and material gates preserve state`() =
        runFuneralPyreTest { deps ->
            val redwood = FuneralPyreLogs.byObj.getValue(objs.redwood_pyre_logs.id)
            val pyre = preparePyre(50)

            preparePlayer(members = false)
            add(redwood.obj)
            player.withProtectedAccess {
                assertFalse(deps.pyre.build(this, pyre, slotOf(redwood.obj), redwood))
            }
            assertMessageSent("You need to be on a members' world to use a funeral pyre.")
            assertEquals(1, player.count(redwood.obj))

            preparePlayer(questState = FuneralPyre.MIN_QUEST_STATE - 1)
            add(redwood.obj)
            player.withProtectedAccess {
                assertFalse(deps.pyre.build(this, pyre, slotOf(redwood.obj), redwood))
            }
            assertMessageSent(
                "You need to progress further in Shades of Mort'ton to use a funeral pyre."
            )
            assertEquals(1, player.count(redwood.obj))

            preparePlayer(level = redwood.level - 1)
            add(redwood.obj)
            player.withProtectedAccess {
                assertFalse(deps.pyre.build(this, pyre, slotOf(redwood.obj), redwood))
            }
            assertMessageSent("You need a Firemaking level of 95 to use these pyre logs.")
            assertEquals(1, player.count(redwood.obj))

            preparePlayer()
            add(redwood.obj)
            add(objs.urium_remains)
            player.withProtectedAccess {
                assertTrue(deps.pyre.build(this, pyre, slotOf(redwood.obj), redwood))
            }
            val logStage = currentPyre(pyre.coords, redwood.logLoc)
            player.withProtectedAccess {
                assertTrue(
                    deps.pyre.addRemains(
                        this,
                        logStage,
                        slotOf(objs.urium_remains),
                        redwood,
                        ShadeRemains.byObj.getValue(objs.urium_remains.id),
                    )
                )
            }
            val remainsStage = currentPyre(pyre.coords, redwood.remainsLoc)
            player.withProtectedAccess { assertFalse(deps.pyre.light(this, remainsStage, redwood)) }
            assertMessageSent("You need a tinderbox to light the funeral pyre.")
            assertNotNull(findLoc(pyre.coords, redwood.remainsLoc))
        }

    @Test
    fun GameTestState
        .`quest cremation state permits only the required normal log and Loar remains`() =
        runFuneralPyreTest { deps ->
            preparePlayer(questState = FuneralPyre.MIN_QUEST_STATE)
            val normal = FuneralPyreLogs.byObj.getValue(objs.pyre_logs.id)
            val redwood = FuneralPyreLogs.byObj.getValue(objs.redwood_pyre_logs.id)
            val loar = ShadeRemains.byObj.getValue(objs.loar_remains.id)
            val phrin = ShadeRemains.byObj.getValue(objs.phrin_remains.id)
            add(normal.obj)
            add(redwood.obj)
            add(loar.obj)
            add(phrin.obj)
            val pyre = preparePyre(55)

            player.withProtectedAccess {
                assertFalse(deps.pyre.build(this, pyre, slotOf(redwood.obj), redwood))
            }
            assertEquals(1, player.count(redwood.obj))
            player.withProtectedAccess {
                assertTrue(deps.pyre.build(this, pyre, slotOf(normal.obj), normal))
            }
            val logStage = currentPyre(pyre.coords, normal.logLoc)
            player.withProtectedAccess {
                assertFalse(deps.pyre.addRemains(this, logStage, slotOf(phrin.obj), normal, phrin))
            }
            assertEquals(1, player.count(phrin.obj))
            player.withProtectedAccess {
                assertTrue(deps.pyre.addRemains(this, logStage, slotOf(loar.obj), normal, loar))
            }
            assertEquals(0, player.count(loar.obj))
        }

    @Test
    fun GameTestState
        .`incompatible remains are rejected and automatic build warns without a pair`() =
        runFuneralPyreTest { deps ->
            preparePlayer()
            val normal = FuneralPyreLogs.byObj.getValue(objs.pyre_logs.id)
            val urium = ShadeRemains.byObj.getValue(objs.urium_remains.id)
            add(normal.obj)
            add(urium.obj)
            val pyre = preparePyre(60)

            player.withProtectedAccess { assertFalse(deps.pyre.buildBest(this, pyre)) }
            assertMessageSent("You do not have the appropriate shade remains for your pyre logs.")
            player.withProtectedAccess {
                assertTrue(deps.pyre.build(this, pyre, slotOf(normal.obj), normal))
            }
            val logStage = currentPyre(pyre.coords, normal.logLoc)
            player.withProtectedAccess {
                assertFalse(deps.pyre.addRemains(this, logStage, slotOf(urium.obj), normal, urium))
            }
            assertMessageSent("These remains need a higher tier of pyre logs.")
            assertEquals(1, player.count(urium.obj))
            assertNotNull(findLoc(pyre.coords, normal.logLoc))
        }

    @Test
    fun GameTestState
        .`diaries boost only their stated skill and pyromancer never boosts cremation`() =
        runFuneralPyreTest { deps ->
            preparePlayer()
            player.worn[0] = InvObj(objs.pyromancer_hood)
            player.worn[1] = InvObj(objs.pyromancer_garb)
            player.worn[2] = InvObj(objs.pyromancer_robe)
            player.worn[3] = InvObj(objs.pyromancer_boots)
            player.setVarBit(varbits.morytania_diary_hard_complete, 1)
            player.setVarBit(varbits.morytania_diary_elite_complete, 1)
            val log = FuneralPyreLogs.byObj.getValue(objs.redwood_pyre_logs.id)
            val remains = ShadeRemains.byObj.getValue(objs.fiyr_remains.id)
            add(log.obj)
            add(remains.obj)
            add(objs.tinderbox)
            val pyre = preparePyre(70)

            cremate(deps, pyre, log, remains)

            assertEquals(7500, fineXp(stats.firemaking))
            assertEquals(1507, fineXp(stats.prayer))
        }

    @Test
    fun GameTestState.`coin branch is exactly twenty one percent with inclusive value ranges`() =
        runFuneralPyreTest { deps ->
            for (remains in ShadeRemains.all) {
                random.next = 20
                random.then = remains.coinRange.first
                assertEquals(
                    ShadeReward(objs.coins, remains.coinRange.first),
                    deps.pyre.rollReward(remains),
                )
                random.next = 20
                random.then = remains.coinRange.last
                assertEquals(
                    ShadeReward(objs.coins, remains.coinRange.last),
                    deps.pyre.rollReward(remains),
                )
                random.next = 21
                random.then = 0
                assertEquals(remains.keyWeights.first().obj, deps.pyre.rollReward(remains).obj)
            }
        }

    @Test
    fun GameTestState.`every weighted key boundary maps to its cache key`() = runFuneralPyreTest {
        for (remains in ShadeRemains.all) {
            var start = 0
            for ((key, weight) in remains.keyWeights) {
                assertEquals(key, remains.keyAt(start))
                assertEquals(key, remains.keyAt(start + weight - 1))
                start += weight
            }
            assertEquals(remains.totalKeyWeight, start)
        }
    }

    @Test
    fun GameTestState.`reward spawns privately on nearest stone stand for five minutes`() =
        runFuneralPyreTest { deps ->
            preparePlayer()
            val log = FuneralPyreLogs.byObj.getValue(objs.pyre_logs.id)
            val remains = ShadeRemains.byObj.getValue(objs.loar_remains.id)
            add(log.obj)
            add(remains.obj)
            add(objs.tinderbox)
            val pyre = preparePyre(80)
            val north = pyre.adjustedCentre.translateZ(FuneralPyre.REWARD_STAND_OFFSET)
            val east = pyre.adjustedCentre.translateX(FuneralPyre.REWARD_STAND_OFFSET)
            val south = pyre.adjustedCentre.translateZ(-FuneralPyre.REWARD_STAND_OFFSET)
            val west = pyre.adjustedCentre.translateX(-FuneralPyre.REWARD_STAND_OFFSET)
            for (coords in listOf(north, east, south, west)) {
                placeMapLoc(coords, firemaking_locs.stoneStand)
            }
            player.placeAt(north.translateZ(1))
            random.next = 99
            random.then = 0

            cremate(deps, pyre, log, remains)

            val reward = deps.objRepo.findAll(north).single()
            assertEquals(objs.bronze_key_red.id, reward.type)
            assertTrue(reward.isPrivate)
            assertEquals(player.observerUUID, reward.nullableOwnerId)
            assertEquals(player.observerUUID, reward.nullableReceiverId)
            advance(FuneralPyre.REWARD_DURATION_TICKS - 1)
            assertTrue(deps.objRepo.findAll(north).any { it === reward })
            advance()
            assertTrue(deps.objRepo.findAll(north).none { it === reward })
        }

    @Test
    fun GameTestState.`stale interactions cannot duplicate stages and unfinished pyres expire`() =
        runFuneralPyreTest { deps ->
            preparePlayer()
            val log = FuneralPyreLogs.byObj.getValue(objs.oak_pyre_logs.id)
            add(log.obj, count = 2)
            val pyre = preparePyre(90)

            player.withProtectedAccess {
                assertTrue(deps.pyre.build(this, pyre, slotOf(log.obj), log))
            }
            player.withProtectedAccess {
                assertFalse(deps.pyre.build(this, pyre, slotOf(log.obj), log))
            }
            assertEquals(1, player.count(log.obj))
            assertNotNull(findLoc(pyre.coords, log.logLoc))

            advance(FuneralPyre.STAGE_DURATION_TICKS - 1)
            assertNotNull(findLoc(pyre.coords, log.logLoc))
            advance()
            assertNotNull(findLoc(pyre.coords, firemaking_locs.funeralPyre))
        }

    private fun GameTestScope.cremate(
        deps: Dependencies,
        pyre: BoundLocInfo,
        log: FuneralPyreLog,
        remains: ShadeRemain,
    ) {
        player.withProtectedAccess { assertTrue(deps.pyre.build(this, pyre, slotOf(log.obj), log)) }
        val logStage = currentPyre(pyre.coords, log.logLoc)
        advance()
        player.withProtectedAccess {
            assertTrue(deps.pyre.addRemains(this, logStage, slotOf(remains.obj), log, remains))
        }
        val remainsStage = currentPyre(pyre.coords, log.remainsLoc)
        advance()
        random.next = 99
        random.then = 0
        player.withProtectedAccess { assertTrue(deps.pyre.light(this, remainsStage, log)) }
    }

    private fun GameTestScope.preparePlayer(
        members: Boolean = true,
        questState: Int = PyreLogPreparation.QUEST_COMPLETE_STATE,
        level: Int = 99,
    ) {
        player.clearAllInvs()
        player.observerUUID = 1L
        player.members = members
        player.stats[stats.firemaking] = level
        player.stats[stats.prayer] = 99
        player.statMap.setXP(stats.firemaking, 0)
        player.statMap.setXP(stats.prayer, 0)
        player.setVarp(varps.morttonquest, questState)
        player.setVarBit(varbits.morytania_diary_hard_complete, 0)
        player.setVarBit(varbits.morytania_diary_elite_complete, 0)
        player.weakQueueList.clear()
        player.withProtectedAccess { resetAnim() }
        client.clear()
    }

    private fun GameTestScope.preparePyre(index: Int): BoundLocInfo {
        val coords = CoordGrid(1500 + index * 8, 1500, 0)
        val pyre =
            placeMapLoc(
                coords,
                firemaking_locs.funeralPyre,
                LocShape.CentrepieceStraight,
                LocAngle.West,
            )
        player.placeAt(pyre.coords.translateX(-1))
        return pyre
    }

    private fun GameTestScope.currentPyre(coords: CoordGrid, type: LocType): BoundLocInfo {
        val info =
            requireNotNull(findLocs(coords).firstOrNull { it.coords == coords && it.id == type.id })
        return BoundLocInfo(info, locTypes.getValue(type.id))
    }

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        if (objTypes[obj].stackable) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj, count)
            return
        }
        repeat(count) {
            val slot = player.inv.indexOfFirst { it == null }
            require(slot >= 0)
            player.inv[slot] = InvObj(obj)
        }
    }

    private fun GameTestScope.slotOf(obj: ObjType): Int =
        player.inv.indexOfFirst { it?.id == obj.id }.also { require(it >= 0) }

    private fun GameTestScope.fineXp(stat: org.rsmod.game.type.stat.StatType): Int =
        player.statMap.getFineXP(stat)

    private fun GameTestState.runFuneralPyreTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            FiremakingScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            install(FiremakingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val pyre: FuneralPyre,
        val locRepo: LocRepository,
        val objRepo: ObjRepository,
        val objTypes: ObjTypeList,
    )
}
