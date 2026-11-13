package org.rsmod.content.skills.fishing.trawler

import com.google.inject.AbstractModule
import com.google.inject.Provides
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.cooking.*
import org.rsmod.content.skills.fishing.FishingOutfits
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
class TrawlerTest {
    @Test
    fun GameTestState.`native boat tools supplies reward inventory and handlers are registered`() =
        test { d ->
            assertEquals("Cross", d.locs[TrawlerLocs.board].op[0])
            assertEquals("Fill", d.locs[TrawlerLocs.leak].op[0])
            assertEquals("Fix", d.locs[TrawlerLocs.rails[0]].op[0])
            assertEquals("Chop", d.npcs[TrawlerNpcs.tentacle].op[0])
            assertEquals(InvScope.Perm, d.invs[TrawlerInvs.rewards].scope)
            assertEquals(
                5000,
                d.invs[TrawlerInvs.shop]
                    .stock!!
                    .filterNotNull()
                    .first { it.obj == TrawlerObjs.paste.id }
                    .count,
            )
        }

    @Test
    fun GameTestState
        .`native gangplank starts one minute boarding and credits actual departure only`() =
        test { d ->
            prepare()
            player.telejump(CoordGrid(2676, 3170))
            val board = placeMapLoc(CoordGrid(2675, 3170), TrawlerLocs.board)
            player.opLoc1(board)
            advance(2)
            assertTrue(TrawlerRound.lobby(player.coords))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.BoardFishingTrawler))
            advance(95)
            assertFalse(d.round.active)
            advance(5)
            assertTrue(d.round.active)
            assertTrue(TrawlerRound.atSea(player.coords))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.BoardFishingTrawler))
        }

    @Test
    fun GameTestState
        .`ordinary leak repair sustains entire five minute voyage and earns the catch`() =
        test { d ->
            depart(d)
            player.telejump(CoordGrid(2020, 4825))
            player.inv[0] = InvObj(TrawlerObjs.paste, 100)
            val leak = placeMapLoc(CoordGrid(2020, 4823), TrawlerLocs.leak, angle = LocAngle.South)
            repeat(500) {
                if (d.round.leaks[15] && !player.isAccessProtected) player.opLoc1(leak)
                advance()
            }
            assertFalse(d.round.active)
            assertEquals(CoordGrid(2676, 3170), player.coords)
            assertTrue(d.rewards.rewards(player).count(d.objs[objs.raw_manta_ray]) >= 12)
            assertEquals(0, player.statMap.getFineXP(stats.fishing))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta))
            player.withProtectedAccess { d.rewards.open(this) }
            assertTrue(player.statMap.getFineXP(stats.fishing) > 0)
            val xp = player.statMap.getFineXP(stats.fishing)
            player.moveGameClick(player.coords)
            advance()
            player.withProtectedAccess { d.rewards.open(this) }
            assertEquals(xp, player.statMap.getFineXP(stats.fishing))
        }

    @Test
    fun GameTestState.`less than fifty contribution and below fifteen Fishing receive no catch`() =
        test { d ->
            depart(d)
            d.round.addPoints(player, 49)
            d.round.finish(true)
            assertTrue(d.rewards.rewards(player).isEmpty())
            player.telejump(CoordGrid(2672, 3170))
            d.round.board(player)
            advance(100)
            player.stats[stats.fishing] = 14
            d.round.addPoints(player, 255)
            d.round.finish(true)
            assertTrue(d.rewards.rewards(player).isEmpty())
        }

    @Test
    fun GameTestState.`bailing uses and empties bucket without consuming the reusable container`() =
        test { d ->
            depart(d)
            player.inv[0] = InvObj(TrawlerObjs.bucket)
            advance(20)
            assertTrue(d.round.water > 0)
            player.withProtectedAccess { d.held.interact(this, inv, 0, HeldOp.Op1) }
            advance(3)
            assertEquals(TrawlerObjs.full.id, player.inv[0]?.id)
            assertEquals(2, d.round.points(player))
            player.withProtectedAccess { d.held.interact(this, inv, 0, HeldOp.Op1) }
            advance()
            assertEquals(TrawlerObjs.bucket.id, player.inv[0]?.id)
        }

    @Test
    fun GameTestState.`leak repair consumes paste and only house owners gain Construction XP`() =
        test { d ->
            depart(d)
            player.telejump(CoordGrid(2020, 4825))
            val leak = placeMapLoc(CoordGrid(2020, 4823), TrawlerLocs.leak, angle = LocAngle.South)
            d.round.leaks[15] = true
            player.opLoc1(leak)
            advance(3)
            assertEquals(0, d.round.points(player))
            player.inv[0] = InvObj(TrawlerObjs.paste, 2)
            player.opLoc1(leak)
            advance(3)
            assertEquals(5, d.round.points(player))
            assertEquals(0, player.statMap.getFineXP(stats.construction))
            player.withProtectedAccess { vars[varbits.poh_house_location] = 1 }
            d.round.leaks[15] = true
            player.opLoc1(leak)
            advance(3)
            assertEquals(10, d.round.points(player))
            assertEquals(50, player.statMap.getFineXP(stats.construction))
            assertNull(player.inv[0])
        }

    @Test
    fun GameTestState
        .`full inventory preserves rewards and manta provenance requires successful delivery`() =
        test { d ->
            prepare()
            d.rewards.generate(player, 255, 1, 1000)
            player.withProtectedAccess { d.rewards.open(this) }
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.coins)
            player.ifButton(TrawlerButtons.items, comsub = 0, obj = objs.raw_manta_ray.id)
            advance()
            player.recordDiaryCook(objs.manta_ray.id, CoordGrid(2660, 3157), true)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta))
            assertTrue(d.rewards.rewards(player).isNotEmpty())
            for (i in player.inv.indices) player.inv[i] = null
            player.ifButton(TrawlerButtons.bank)
            advance()
            assertEquals(
                512,
                player.vars[OrderedProductionVars.progress] and 512,
                "manta flag after bank; remaining=${player.trawlerRewardManta}, inspected=${player.trawlerInspected}, pending=${d.rewards.rewards(player)}",
            )
            player.recordDiaryCook(objs.manta_ray.id, CoordGrid(3000, 3000), true)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta))
            player.recordDiaryCook(objs.manta_ray.id, CoordGrid(2660, 3157), true)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta))
        }

    @Test
    fun GameTestState
        .`ordinary cooked manta fails then actual successful Trawler reward and cooking completes diary`() =
        test { d ->
            prepare()
            player.stats[stats.cooking] = 99
            player.telejump(CoordGrid(2660, 3158))
            val range = placeMapLoc(CoordGrid(2660, 3157), cooking_locs.range)
            fun cook() {
                player.inv[0] = InvObj(objs.raw_manta_ray)
                random.next = 0
                player.withProtectedAccess {
                    d.cooking.start(
                        this,
                        range,
                        CookingSourceKind.Range,
                        StandardCookingRecipes.byRaw.getValue(objs.raw_manta_ray.id),
                        1,
                    )
                }
                advance(5)
            }
            cook()
            assertEquals(1, player.count(objs.manta_ray))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta))
            d.rewards.generate(player, 50, 1, 1000)
            player.withProtectedAccess { d.rewards.open(this) }
            player.ifButton(TrawlerButtons.bank)
            advance()
            player.moveGameClick(player.coords)
            advance()
            cook()
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CatchCookTrawlerManta))
        }

    @Test
    fun GameTestState.`sinking and logout cannot leave session or reward provenance behind`() =
        test { d ->
            depart(d)
            d.round.leaks[0] = true
            advance(500)
            assertTrue(TrawlerRound.wreck(player.coords))
            assertTrue(d.rewards.rewards(player).isEmpty())
            assertEquals(0, player.trawlerSession)
            player.telejump(CoordGrid(2016, 4825))
            d.round.login(player)
            assertEquals(CoordGrid(2653, 3174), player.coords)
        }

    @Test
    fun GameTestState.`closing rewards drops catch while logout preserves the persistent catch`() =
        test { d ->
            prepare()
            player.telejump(CoordGrid(2676, 3170))
            d.rewards.generate(player, 50, 1, 1000)
            player.withProtectedAccess { d.rewards.open(this) }
            player.loggingOut = true
            d.rewards.close(player)
            assertTrue(d.rewards.rewards(player).isNotEmpty())
            assertEquals(0, player.vars[OrderedProductionVars.progress] and 512)
            player.loggingOut = false
            player.moveGameClick(player.coords)
            advance()
            assertTrue(d.rewards.rewards(player).isEmpty())
            assertEquals(512, player.vars[OrderedProductionVars.progress] and 512)
        }

    @Test
    fun GameTestState.`angler rolls exclude owned pieces until a complete set is collected`() =
        test { d ->
            prepare()
            player.inv[0] = InvObj(FishingOutfits.hat)
            d.rewards.generate(player, 50, 1, 1000)
            assertEquals(0, d.rewards.rewards(player).count(d.objs[FishingOutfits.hat]))
            assertEquals(1, d.rewards.rewards(player).count(d.objs[FishingOutfits.top]))
        }

    private fun GameTestScope.prepare() {
        player.observerUUID = 1L
        player.members = true
        player.stats[stats.fishing] = 99
        player.statMap.setFineXP(stats.fishing, 0)
        player.statMap.setFineXP(stats.construction, 0)
    }

    private fun GameTestScope.depart(d: Dependencies) {
        prepare()
        player.telejump(CoordGrid(2672, 3170))
        d.round.board(player)
        advance(100)
        assertTrue(d.round.active)
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            Module,
            TrawlerScript::class,
            CookingScript::class,
        ) { d ->
            for (c in
                listOf(
                    CoordGrid(2672, 3170),
                    CoordGrid(2016, 4825),
                    CoordGrid(2016, 4825, 1),
                    CoordGrid(1953, 4826),
                    CoordGrid(2653, 3174),
                    CoordGrid(2660, 3157),
                )) for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(
                c.translate(x, z)
            )
            d.round.startup()
            body(d)
        }

    private object Module : AbstractModule() {
        override fun configure() {
            install(CookingModule())
        }

        @Provides
        @Singleton
        fun round(
            controllers: ControllerRepository,
            npcs: NpcRepository,
            types: NpcTypeList,
            rewards: TrawlerRewards,
            collision: CollisionFlagMap,
            events: EventBus,
        ) = TrawlerRound(controllers, npcs, types, rewards, Quiet, collision, events)

        @Provides
        @Singleton
        fun loot(
            petRates: org.rsmod.content.skills.pets.SkillPetRates,
            types: InvTypeList,
            objects: ObjTypeList,
            ground: org.rsmod.api.repo.obj.ObjRepository,
        ) = TrawlerRewards(petRates, types, Zero, objects, ground)
    }

    private object Quiet : GameRandom {
        override fun of(maxExclusive: Int) = maxExclusive - 1

        override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive

        override fun randomDouble() = 0.99
    }

    private object Zero : GameRandom {
        override fun of(maxExclusive: Int) = 0

        override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive

        override fun randomDouble() = 0.0
    }

    data class Dependencies
    @Inject
    constructor(
        val held: HeldInteractions,
        val round: TrawlerRound,
        val rewards: TrawlerRewards,
        val cooking: StandardCooking,
        val locs: LocTypeList,
        val npcs: NpcTypeList,
        val invs: InvTypeList,
        val objs: ObjTypeList,
    )
}
