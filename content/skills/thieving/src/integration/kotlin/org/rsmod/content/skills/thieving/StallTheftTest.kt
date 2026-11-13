package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.events.PlayerStallTheftEvent
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.events.EventBus
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StallTheftTest {
    @Test
    fun GameTestState
        .`all twenty ordinary stall identities have native operation handlers`() =
        runStallTest { deps ->
            assertEquals(20, OrdinaryStalls.all.size)
            for (definition in OrdinaryStalls.all) {
                assertEquals("Steal-from", deps.locs[definition.full].op[definition.operation - 1])
                assertTrue(deps.events.contains(if (definition.operation == 1) LocEvents.Op1::class.java else LocEvents.Op2::class.java, definition.full.id))
                assertTrue(definition.restockTicks > 0)
                assertTrue(definition.totalWeight > 0)
            }
            val bakery = OrdinaryStalls.all.first { it.full.id == StallLocs.bakery.id }
            assertEquals(
                List(5) { objs.bread.id } +
                    List(13) { objs.cake.id } +
                    List(2) { StallObjs.chocolateSlice.id },
                (0 until 20).map { bakery.reward(it).id },
            )
            val gems = OrdinaryStalls.all.first { it.full.id == StallLocs.gems.id }
            assertEquals(105, (0 until 128).count { gems.reward(it).id == objs.uncut_sapphire.id })
            assertEquals(17, (0 until 128).count { gems.reward(it).id == objs.uncut_emerald.id })
            assertEquals(5, (0 until 128).count { gems.reward(it).id == objs.uncut_ruby.id })
            assertEquals(1, (0 until 128).count { gems.reward(it).id == objs.uncut_diamond.id })
        }

    @Test
    fun GameTestState
        .`each stall awards its exact item and xp then returns after its own restock delay`() =
        runStallTest { deps ->
            prepare()
            for ((index, definition) in OrdinaryStalls.all.withIndex()) {
                player.clearInv()
                val stall = placeMapLoc(CoordGrid(3200 + index * 8, 3200), definition.full)
                player.placeAt(stall.coords.translateX(-1))
                val xp = player.statMap.getFineXP(stats.thieving)
                random.next = 0
                player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
                advance(StallTheft.STEAL_TICKS - 1)
                assertEquals(0, player.count(definition.reward(0)))
                advance()
                assertEquals(1, player.count(definition.reward(0)))
                assertEquals(
                    (definition.xp * 10).toInt(),
                    player.statMap.getFineXP(stats.thieving) - xp,
                )
                assertNotNull(deps.locRepo.findExact(stall.coords, definition.empty))
                advance(definition.restockTicks)
                assertNotNull(deps.locRepo.findExact(stall.coords, definition.full))
            }
        }

    @Test
    fun GameTestState.`native second operation produces a cup of tea`() = runStallTest { _ ->
        prepare()
        val stall = placeMapLoc(CoordGrid(3200, 3200), StallLocs.tea)
        player.placeAt(stall.coords.translateX(-1))
        player.opLoc2(stall)
        advance(3)
        assertEquals(1, player.count(objs.cup_of_tea))
    }

    @Test
    fun GameTestState.`membership boosted level capacity and interruption preserve stock`() =
        runStallTest { deps ->
            prepare()
            val definition = OrdinaryStalls.all.first { it.full.id == StallLocs.tea.id }
            val stall = placeMapLoc(CoordGrid(3200, 3200), definition.full)
            player.placeAt(stall.coords.translateX(-1))
            player.members = false
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            assertEquals(0, player.count(objs.cup_of_tea))
            player.members = true
            player.setCurrentLevel(stats.thieving, 4)
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            assertMessageSent("You need a Thieving level of 5 to steal from this stall.")
            player.setCurrentLevel(stats.thieving, 5)
            player.fillInv()
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            assertMessageSent("You don't have enough inventory space.")
            player.clearInv()
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            player.placeAt(stall.coords.translateX(-5))
            advance(StallTheft.STEAL_TICKS)
            assertEquals(0, player.count(objs.cup_of_tea))
            assertNotNull(deps.locRepo.findExact(stall.coords, definition.full))
        }

    @Test
    fun GameTestState.`stale full-stall interaction cannot duplicate a reward during restock`() =
        runStallTest { deps ->
            prepare()
            val definition = OrdinaryStalls.all.first { it.full.id == StallLocs.gems.id }
            val stall = placeMapLoc(CoordGrid(3200, 3200), definition.full)
            player.placeAt(stall.coords.translateX(-1))
            random.next = 0
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            advance(StallTheft.STEAL_TICKS)
            assertEquals(1, player.count(objs.uncut_sapphire))
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            advance(StallTheft.STEAL_TICKS)
            assertEquals(1, player.count(objs.uncut_sapphire))
        }

    @Test
    fun GameTestState.`visible nearby guard blocks loot and queues standard combat retaliation`() =
        runStallTest { deps ->
            prepare()
            val definition = OrdinaryStalls.all.first { it.full.id == StallLocs.tea.id }
            val stall = placeMapLoc(CoordGrid(3200, 3200), definition.full)
            player.placeAt(stall.coords.translateX(-1))
            val guard =
                spawnNpc(player.coords.translateX(-1), deps.npcs[ThievingNpcs.guards.first()])
            player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
            advance(StallTheft.STEAL_TICKS)
            assertEquals(0, player.count(objs.cup_of_tea))
            assertNotNull(deps.locRepo.findExact(stall.coords, definition.full))
            assertMessageSent("You've been spotted trying to steal from the stall.")
            assertTrue(queues.com_retaliate_player in guard.queueList)
        }

    @Test
    fun GameTestState.`Hosidius fruit stall has the exact hundred-weight table and native diary credit`() = runStallTest { deps ->
        prepare()
        val definition = OrdinaryStalls.all.first { it.full == StallLocs.fruit }
        assertEquals(28823, definition.full.id)
        assertEquals(27537, definition.empty.id)
        assertEquals(100, definition.totalWeight)
        assertEquals(40, (0 until 100).count { definition.reward(it) == objs.cooking_apple })
        assertEquals(20, (0 until 100).count { definition.reward(it) == objs.banana })
        assertEquals(2, (0 until 100).count { definition.reward(it) == objs.golovanova_fruit_top })
        assertEquals(StallObjs.papaya, definition.reward(99))
        val coords = CoordGrid(1795, 3607)
        allocZoneCollision(coords)
        val stall = placeMapLoc(coords, definition.full)
        player.placeAt(stall.coords.translateZ(-1))
        random.next = 99
        player.opLoc2(stall)
        advance(10)
        assertEquals(1, player.count(StallObjs.papaya))
        assertTrue(player.hasCompletedDiaryTask(DiaryTask.StealHosidiusFruit))
    }

    @Test
    fun GameTestState.`Hosidius guard dog witnesses fruit theft and retaliates`() = runStallTest { deps ->
        prepare()
        val definition = OrdinaryStalls.all.first { it.full == StallLocs.fruit }
        val stall = placeMapLoc(CoordGrid(3200, 3200), definition.full)
        player.placeAt(stall.coords.translateX(-1))
        val dog = spawnNpc(player.coords.translateX(-1), deps.npcs.getValue(7209))
        player.withProtectedAccess { deps.theft.steal(this, stall, definition) }
        advance(StallTheft.STEAL_TICKS)
        assertEquals(0, player.count(objs.cooking_apple))
        assertTrue(queues.com_retaliate_player in dog.queueList)
    }

    @Test fun GameTestState.`Rellekka fish theft enforces Trials and publishes only delivered loot`() = runStallTest { deps ->
        prepare();VarPlayerIntMapSetter.set(player,StallQuestVars.trials,0)
        val definition=OrdinaryStalls.all.first{it.full==StallLocs.rellekkaFish}
        val coords=CoordGrid(2647,3676);allocZoneCollision(coords)
        val stall=placeMapLoc(coords,definition.full);player.placeAt(coords.translateX(-1))
        val outcomes=mutableListOf<PlayerStallTheftEvent>()
        deps.events.subscribeUnbound(PlayerStallTheftEvent::class.java){outcomes+=this}
        player.withProtectedAccess{deps.theft.steal(this,stall,definition)};advance(2)
        assertEquals(0,outcomes.size)
        VarPlayerIntMapSetter.set(player,StallQuestVars.trials,10);random.next=14
        player.withProtectedAccess{deps.theft.steal(this,stall,definition)};advance(2)
        assertEquals(1,player.count(objs.raw_tuna));assertTrue(player.hasCompletedDiaryTask(DiaryTask.StealRellekkaFish))
        assertEquals(1,outcomes.size);assertEquals(false,outcomes.single().caught);assertEquals(objs.raw_tuna,outcomes.single().reward)
    }

    private fun GameTestScope.prepare() {
        player.members = true
        VarPlayerIntMapSetter.set(player, StallQuestVars.trials, 10)
        player.stats[stats.thieving] = 99
        player.observerUUID = 1L
        allocZoneCollision(CoordGrid(3200, 3200))
    }

    private fun GameTestState.runStallTest(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            scripts = arrayOf(StallTheftScript::class),
            testBody = body,
        )

    data class Dependencies
    @Inject
    constructor(
        val theft: StallTheft,
        val locs: LocTypeList,
        val npcs: NpcTypeList,
        val locRepo: LocRepository,
        val events: EventBus,
    )
}
