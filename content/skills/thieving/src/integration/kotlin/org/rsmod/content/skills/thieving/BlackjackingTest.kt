package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class BlackjackingTest {
    @Test
    fun GameTestState.`native bearded knockout yields two direct forty coin thefts`() = test { d ->
        prepare(45)
        val npc = target(d, BlackjackNpcs.club)
        random.next = 0
        player.opNpc5(npc)
        advance()
        assertTrue(d.state.owns(player, npc))
        assertEquals(100, player.statMap.getFineXP(stats.thieving))
        player.opNpc3(npc)
        advance()
        advance()
        player.opNpc3(npc)
        advance()
        assertEquals(80, player.count(objs.coins))
        assertEquals(0, player.count(ThievingObjs.bandit2Pouch))
        assertEquals(1400, player.statMap.getFineXP(stats.thieving))
        assertFalse(d.state.owns(player, npc))
    }

    @Test
    fun GameTestState.`native sword bandit uses fifty coin and eighty four point three XP`() =
        test { d ->
            prepare(55)
            val npc = target(d, BlackjackNpcs.sword)
            random.next = 0
            player.opNpc5(npc)
            advance()
            player.opNpc3(npc)
            advance()
            assertEquals(50, player.count(objs.coins))
            assertEquals(943, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`native Menaphite actual knocked out theft grants Desert hard task`() =
        test { d ->
            prepare(65)
            val npc = target(d, BlackjackNpcs.thug)
            random.next = 0
            player.opNpc5(npc)
            advance()
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.BlackjackMenaphiteThug))
            player.opNpc3(npc)
            advance()
            assertEquals(60, player.count(objs.coins))
            assertEquals(1475, player.statMap.getFineXP(stats.thieving))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.BlackjackMenaphiteThug))
        }

    @Test
    fun GameTestState
        .`knockout ignores full pouch count and uses an existing coin stack in a full bag`() =
        test { d ->
            prepare(65)
            val npc = target(d, BlackjackNpcs.thug)
            for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
            player.inv[0] = InvObj(objs.coins, 10)
            player.inv[1] = InvObj(ThievingObjs.menaphitePouch, 28)
            random.next = 0
            player.opNpc5(npc)
            advance()
            player.opNpc3(npc)
            advance()
            assertEquals(70, player.count(objs.coins))
            assertEquals(28, player.count(ThievingObjs.menaphitePouch))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.BlackjackMenaphiteThug))
        }

    @Test
    fun GameTestState
        .`failed coin delivery retains knockout pick and gives no theft XP or diary`() = test { d ->
        prepare(65)
        val npc = target(d, BlackjackNpcs.thug)
        for (i in player.inv.indices) player.inv[i] = InvObj(objs.knife)
        random.next = 0
        player.opNpc5(npc)
        advance()
        player.opNpc3(npc)
        advance()
        assertEquals(100, player.statMap.getFineXP(stats.thieving))
        assertEquals(0, player.count(objs.coins))
        assertTrue(d.state.owns(player, npc))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.BlackjackMenaphiteThug))
    }

    @Test
    fun GameTestState.`expiry and failed knockout cannot grant guaranteed loot or diary`() =
        test { d ->
            prepare(99)
            val npc = target(d, BlackjackNpcs.thug)
            random.next = 0
            player.opNpc5(npc)
            advance()
            advance(6)
            assertFalse(d.state.owns(player, npc))
            random.next = 99
            random.then = 1
            player.opNpc5(npc)
            advance()
            assertFalse(d.state.owns(player, npc))
            assertEquals(100, player.statMap.getFineXP(stats.thieving))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.BlackjackMenaphiteThug))
        }

    @Test
    fun GameTestState.`quest unlock weapon visible level and combat gate native knockout`() =
        test { d ->
            prepare(64)
            val npc = target(d, BlackjackNpcs.thug)
            random.next = 0
            player.opNpc5(npc)
            advance()
            assertFalse(d.state.owns(player, npc))
            player.setCurrentLevel(stats.thieving, 65)
            player.righthand = null
            player.opNpc5(npc)
            advance()
            assertFalse(d.state.owns(player, npc))
            player.righthand = InvObj(BlackjackObjs.willow)
            VarPlayerIntMapSetter.set(player, BlackjackBits.unlocked, 1)
            player.withProtectedAccess {
                d.script.knock(this, npc, StandardPickpockets.menaphiteThug)
            }
            assertFalse(d.state.owns(player, npc))
            VarPlayerIntMapSetter.set(player, BlackjackBits.unlocked, 2)
            VarPlayerIntMapSetter.set(player, varps.lastcombat, player.currentMapClock)
            player.opNpc5(npc)
            advance()
            assertFalse(d.state.owns(player, npc))
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState.`a visible second bandit prevents knockout`() = test { d ->
        prepare(99)
        val npc = target(d, BlackjackNpcs.club)
        spawnNpc(CoordGrid(3355, 2986), d.npcs[BlackjackNpcs.sword])
        random.next = 0
        player.opNpc5(npc)
        advance()
        assertFalse(d.state.owns(player, npc))
        assertEquals(0, player.statMap.getFineXP(stats.thieving))
    }

    @Test
    fun GameTestState
        .`only the knockout owner can use guaranteed attempts and rogue clothing doubles coins`() =
        test { d ->
            prepare(65)
            val npc = target(d, BlackjackNpcs.thug)
            val other = org.rsmod.game.entity.Player()
            other.currentMapClock = player.currentMapClock
            d.state.knock(other, npc)
            player.opNpc3(npc)
            advance()
            assertEquals(0, player.statMap.getFineXP(stats.thieving))
            assertEquals(0, player.count(objs.coins))
            d.state.clear(npc)
            player.worn[0] = InvObj(ThievingObjs.rogueMask)
            player.worn[4] = InvObj(ThievingObjs.rogueTop)
            player.worn[7] = InvObj(ThievingObjs.rogueTrousers)
            player.worn[9] = InvObj(ThievingObjs.rogueGloves)
            player.worn[10] = InvObj(ThievingObjs.rogueBoots)
            random.next = 0
            player.opNpc5(npc)
            advance()
            player.opNpc3(npc)
            advance()
            assertEquals(120, player.count(objs.coins))
            assertEquals(1475, player.statMap.getFineXP(stats.thieving))
        }

    @Test
    fun GameTestState
        .`seller requires native completed quest and buys one reusable willow blackjack atomically`() =
        test { d ->
            prepare(99)
            VarPlayerIntMapSetter.set(player, BlackjackBits.quest, 27)
            player.inv[0] = InvObj(objs.coins, 600)
            player.withProtectedAccess { assertFalse(d.script.buy(this)) }
            assertEquals(600, player.count(objs.coins))
            VarPlayerIntMapSetter.set(player, BlackjackBits.quest, 28)
            player.withProtectedAccess { assertTrue(d.script.buy(this)) }
            assertEquals(0, player.count(objs.coins))
            assertEquals(1, player.count(BlackjackObjs.willow))
            player.withProtectedAccess { assertFalse(d.script.buy(this)) }
            assertEquals(1, player.count(BlackjackObjs.willow))
        }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.stats[stats.thieving] = level
        player.statMap.setFineXP(stats.thieving, 0)
        VarPlayerIntMapSetter.set(player, BlackjackBits.quest, 28)
        VarPlayerIntMapSetter.set(player, BlackjackBits.unlocked, 2)
        VarPlayerIntMapSetter.set(player, varps.lastcombat, -10000)
        VarPlayerIntMapSetter.set(player, varps.lastcombat_pvp, -10000)
        player.righthand = InvObj(BlackjackObjs.willow)
        player.telejump(CoordGrid(3353, 2987))
    }

    private fun GameTestScope.target(d: Dependencies, type: NpcType): Npc =
        spawnNpc(CoordGrid(3354, 2987), d.npcs[type])

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            null,
            Blackjacking::class,
            PickpocketScript::class,
        ) { d ->
            for (x in -16..16 step 8) for (z in -16..16 step 8) allocZoneCollision(
                CoordGrid(3354 + x, 2987 + z)
            )
            body(d)
        }

    data class Dependencies
    @Inject
    constructor(val script: Blackjacking, val state: BlackjackingState, val npcs: NpcTypeList)
}
