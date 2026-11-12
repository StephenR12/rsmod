package org.rsmod.content.skills.hunter.birdsnares

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.controller.events.ControllerAIEvents
import org.rsmod.api.npc.events.interact.AiLocEvents
import org.rsmod.api.npc.interact.AiLocInteractions
import org.rsmod.api.npc.opLoc1
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.movement.MoveRestrict
import org.rsmod.game.type.hunt.HuntModeTypeList
import org.rsmod.game.type.hunt.HuntType
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BirdSnareTrapTest {
    @Test
    fun GameTestState.`cache 233 trap birds rewards rates and operations are pinned`() =
        runBirdSnareTest { deps ->
            assertEquals(10006, BirdSnareObjs.snare.id)
            assertEquals(listOf(5549, 5551, 5552, 5550, 5548), BirdSnareBirds.all.map { it.npc.id })
            assertEquals(listOf(1, 5, 9, 11, 19), BirdSnareBirds.all.map { it.level })
            assertEquals(listOf(34.0, 47.0, 61.2, 64.5, 95.2), BirdSnareBirds.all.map { it.xp })
            assertEquals(listOf(100, 92, 85, 82, 75), BirdSnareBirds.all.map { it.lowRate })
            assertEquals(listOf(420, 400, 390, 380, 370), BirdSnareBirds.all.map { it.highRate })
            assertEquals(
                listOf(10088, 10090, 10091, 10089, 10087),
                BirdSnareBirds.all.map { it.feather.id },
            )
            assertEquals(
                listOf(9373, 9377, 9379, 9375, 9348),
                BirdSnareBirds.all.map { it.full.id },
            )
            assertEquals(5208, BirdSnareSeqs.lay.id)
            assertEquals(5207, BirdSnareSeqs.check.id)
            assertEquals(100, BirdSnareTrap.IDLE_TICKS)
            assertEquals(4, BirdSnareTrap.LAY_TICKS)

            assertEquals(
                listOf("Lay", null, null, null, null),
                deps.objs[BirdSnareObjs.snare].iop.toList(),
            )
            assertEquals(
                listOf("Dismantle", "Investigate", null, null, null),
                deps.locs[BirdSnareLocs.open].op.toList(),
            )
            assertEquals(
                listOf("Dismantle", null, null, null, null),
                deps.locs[BirdSnareLocs.broken].op.toList(),
            )
            for (bird in BirdSnareBirds.all) {
                assertEquals(
                    listOf("Check", null, null, null, null),
                    deps.locs[bird.full].op.toList(),
                )
            }

            val hunt = deps.hunts[BirdSnareHunts.openTrap]
            assertEquals(HuntType.Scenery, hunt.type)
            assertEquals(BirdSnareLocs.open.id, hunt.checkLoc?.loc)
            assertEquals(NpcMode.OpLoc1, hunt.findNewMode)
            assertTrue(hunt.findKeepHunting)
            for (bird in BirdSnareBirds.all) {
                val type = deps.npcs[bird.npc]
                assertEquals(NpcMode.Wander, type.defaultMode)
                assertEquals(MoveRestrict.Normal, type.moveRestrict)
                assertEquals(BirdSnareHunts.openTrap.id, type.huntMode)
                assertEquals(2, type.huntRange)
            }

            assertTrue(
                deps.events.contains(HeldObjEvents.Op1::class.java, BirdSnareObjs.snare.id.toLong())
            )
            assertTrue(
                deps.events.contains(LocEvents.Op1::class.java, BirdSnareLocs.open.id.toLong())
            )
            assertTrue(
                deps.events.contains(AiLocEvents.Op1::class.java, BirdSnareLocs.open.id.toLong())
            )
            assertNotNull(
                deps.events.keyed[
                        ControllerAIEvents.Timer::class.java,
                        BirdSnareControllerBuilder.trap.id.toLong(),
                    ]
            )
        }

    @Test
    fun GameTestState.`lay takes four ticks consumes one stack unit and creates owned trap`() =
        runBirdSnareTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(BirdSnareObjs.snare, 3)
            val origin = player.coords

            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(3)
            assertEquals(3, player.inv.count(BirdSnareObjs.snare))
            assertFalse(locExists(origin, BirdSnareLocs.open))
            advance()

            assertEquals(2, player.inv.count(BirdSnareObjs.snare))
            assertTrue(locExists(origin, BirdSnareLocs.open))
            assertEquals(1, deps.traps.activeTrapCount(player))
            assertFalse(player.coords == origin)
        }

    @Test
    fun GameTestState.`boosted level and Wilderness determine the exact concurrent trap cap`() =
        runBirdSnareTest { deps ->
            preparePlayer()
            val cases = listOf(1 to 1, 19 to 1, 20 to 2, 40 to 3, 60 to 4, 80 to 5, 120 to 5)
            for ((level, expected) in cases) {
                player.setCurrentLevel(stats.hunter, level)
                player.setVarBit(varbits.inside_wilderness, 0)
                assertEquals(expected, deps.traps.maximumTraps(player))
                player.setVarBit(varbits.inside_wilderness, 1)
                assertEquals(expected + 1, deps.traps.maximumTraps(player))
            }
        }

    @Test
    fun GameTestState.`smoke and complete guild outfit apply configured catch bonuses`() =
        runBirdSnareTest { deps ->
            preparePlayer(level = 19)
            val bird = BirdSnareBirds.all.last()
            val base = deps.traps.catchRate(player, bird, smoked = false)
            assertEquals(base + 0.02, deps.traps.catchRate(player, bird, smoked = true), 0.0000001)

            player.worn[0] = InvObj(BirdSnareObjs.guildHood)
            player.worn[1] = InvObj(BirdSnareObjs.guildTop)
            player.worn[2] = InvObj(BirdSnareObjs.guildLegs)
            assertEquals(base, deps.traps.catchRate(player, bird, smoked = false), 0.0000001)
            player.worn[3] = InvObj(BirdSnareObjs.guildBoots)
            assertEquals(
                base + 0.025,
                deps.traps.catchRate(player, bird, smoked = false),
                0.0000001,
            )
            assertEquals(base + 0.045, deps.traps.catchRate(player, bird, smoked = true), 0.0000001)
        }

    @Test
    fun GameTestState.`bird AI catch transitions then check grants all four rewards atomically`() =
        runBirdSnareTest { deps ->
            preparePlayer(level = 99)
            player.inv[0] = InvObj(BirdSnareObjs.snare)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(BirdSnareTrap.LAY_TICKS)
            player.teleport(origin.translate(3, 0))

            random.next = 0
            val npc = spawnNpc(origin.translate(1, 0), deps.npcs[BirdSnareBirds.all.last().npc])
            val open = bound(deps, origin, BirdSnareLocs.open)
            npc.opLoc1(open, deps.aiLoc)
            advance()
            assertTrue(locExists(origin, BirdSnareLocs.trappingTropical))
            advance(BirdSnareTrap.TRANSITION_TICKS)
            assertTrue(locExists(origin, BirdSnareLocs.fullTropical))

            val startXp = player.statMap.getFineXP(stats.hunter)
            random.next = 7
            val full = bound(deps, origin, BirdSnareLocs.fullTropical)
            player.withProtectedAccess { deps.traps.check(this, full) }

            assertEquals(1, player.inv.count(BirdSnareObjs.snare))
            assertEquals(1, player.inv.count(BirdSnareObjs.bones))
            assertEquals(1, player.inv.count(BirdSnareObjs.rawBirdMeat))
            assertEquals(7, player.inv.count(BirdSnareObjs.stripyFeather))
            assertEquals(952, player.statMap.getFineXP(stats.hunter) - startXp)
            assertEquals(0, deps.traps.activeTrapCount(player))
            assertFalse(locExists(origin, BirdSnareLocs.fullTropical))
        }

    @Test
    fun GameTestState.`failed catch can be dismantled and abandoned trap falls after one minute`() =
        runBirdSnareTest { deps ->
            preparePlayer()
            player.inv[0] = InvObj(BirdSnareObjs.snare)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(BirdSnareTrap.LAY_TICKS)
            player.teleport(origin.translate(3, 0))

            random.next = 99
            val npc = spawnNpc(origin.translate(1, 0), deps.npcs[BirdSnareBirds.all.first().npc])
            npc.opLoc1(bound(deps, origin, BirdSnareLocs.open), deps.aiLoc)
            advance()
            assertTrue(locExists(origin, BirdSnareLocs.failing))
            advance(BirdSnareTrap.TRANSITION_TICKS)
            assertTrue(locExists(origin, BirdSnareLocs.broken))
            deps.npcRepo.despawn(npc, Int.MAX_VALUE)

            player.withProtectedAccess {
                deps.traps.dismantle(this, bound(deps, origin, BirdSnareLocs.broken))
            }
            assertFalse(locExists(origin, BirdSnareLocs.broken))
            assertEquals(1, player.inv.count(BirdSnareObjs.snare))

            player.teleport(origin)
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(BirdSnareTrap.LAY_TICKS)
            assertTrue(locExists(origin, BirdSnareLocs.open))
            advance(BirdSnareTrap.IDLE_TICKS)

            assertFalse(locExists(origin, BirdSnareLocs.open))
            assertEquals(0, deps.traps.activeTrapCount(player))
            val fallen =
                deps.objRepo.findAll(origin).singleOrNull { it.type == BirdSnareObjs.snare.id }
            assertNotNull(fallen)
            assertTrue(requireNotNull(fallen).isPrivate)
        }

    @Test
    fun GameTestState.`full inventory cannot lose a caught bird or partially insert rewards`() =
        runBirdSnareTest { deps ->
            preparePlayer(level = 99)
            player.inv[0] = InvObj(BirdSnareObjs.snare)
            val origin = player.coords
            player.withProtectedAccess { deps.traps.lay(this, 0) }
            advance(BirdSnareTrap.LAY_TICKS)
            player.teleport(origin.translate(3, 0))
            random.next = 0
            val npc = spawnNpc(origin.translate(1, 0), deps.npcs[BirdSnareBirds.all.first().npc])
            npc.opLoc1(bound(deps, origin, BirdSnareLocs.open), deps.aiLoc)
            advance(1 + BirdSnareTrap.TRANSITION_TICKS)
            val full = bound(deps, origin, BirdSnareLocs.fullCrimson)

            player.fillInv()
            val before = player.inv.toList()
            val startXp = player.statMap.getFineXP(stats.hunter)
            random.next = 5
            player.withProtectedAccess { deps.traps.check(this, full) }
            assertEquals(before, player.inv.toList())
            assertEquals(startXp, player.statMap.getFineXP(stats.hunter))
            assertTrue(locExists(origin, BirdSnareLocs.fullCrimson))

            player.clearInv()
            random.next = 5
            player.withProtectedAccess { deps.traps.check(this, full) }
            assertEquals(1, player.inv.count(BirdSnareObjs.snare))
            assertEquals(1, player.inv.count(BirdSnareObjs.bones))
            assertEquals(1, player.inv.count(BirdSnareObjs.rawBirdMeat))
            assertEquals(5, player.inv.count(BirdSnareObjs.redFeather))
        }

    private fun GameTestScope.preparePlayer(level: Int = 1) {
        val origin = CoordGrid(3200, 3200)
        allocZoneCollision(origin)
        player.placeAt(origin)
        player.observerUUID = 1L
        player.members = true
        player.setBaseLevel(stats.hunter, level.coerceAtMost(99))
        player.setCurrentLevel(stats.hunter, level)
    }

    private fun GameTestScope.bound(
        deps: Dependencies,
        coords: CoordGrid,
        type: LocType,
    ): BoundLocInfo {
        val info = deps.locRepo.findExact(coords, type)
        return BoundLocInfo(requireNotNull(info), deps.locs[type])
    }

    private fun GameTestState.runBirdSnareTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            TestModule,
            BirdSnareScript::class,
            testBody = body,
        )

    private object TestModule : AbstractModule() {
        override fun configure() {
            bind(BirdSnarePlacement::class.java).`in`(Scopes.SINGLETON)
            bind(BirdSnareTrap::class.java).`in`(Scopes.SINGLETON)
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val traps: BirdSnareTrap,
        val events: EventBus,
        val locs: LocTypeList,
        val objs: ObjTypeList,
        val npcs: NpcTypeList,
        val hunts: HuntModeTypeList,
        val locRepo: LocRepository,
        val npcRepo: NpcRepository,
        val objRepo: ObjRepository,
        val aiLoc: AiLocInteractions,
    )
}
