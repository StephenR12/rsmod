package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.EntTrunkOwnership
import org.rsmod.api.player.diary.*
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.woodcutting.scripts.EntCaves
import org.rsmod.content.skills.woodcutting.scripts.EntChopping
import org.rsmod.content.skills.woodcutting.scripts.EntLogs
import org.rsmod.content.skills.woodcutting.scripts.EntNpcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class EntChoppingTest {
    @Test
    fun GameTestState.walkingAwayCancelsAnEntYieldBeforeItsFourTickRoll() = test { d ->
        prepare(61)
        val n = trunk(d)
        player.inv[0] = InvObj(objs.mithril_axe)
        d.ownership.assign(n, requireNotNull(player.uuid))
        val origin = player.coords
        player.opNpc1(n)
        advance()
        player.moveGameClick(origin.translate(-1, 0))
        advance(2)
        assertNotEquals(origin, player.coords)
        advance(8)
        assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
    }

    @Test
    fun GameTestState.`native trunk requires the actual killer and a usable axe`() = test { d ->
        prepare(61)
        val trunk = trunk(d)
        player.inv[0] = InvObj(objs.mithril_axe)
        d.ownership.assign(trunk, 2L)
        player.opNpc1(trunk)
        advance(5)
        assertEquals(0, player.count(note(d, objs.yew_logs)))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopWildernessEntYew))
        d.ownership.assign(trunk, 1L)
        player.inv[0] = null
        player.opNpc1(trunk)
        advance(5)
        assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
    }

    @Test
    fun GameTestState
        .`native own Wilderness trunk delivers two noted yew logs for twenty five XP and diary`() =
        test { d ->
            prepare(61)
            player.inv[0] = InvObj(objs.mithril_axe)
            val trunk = trunk(d)
            d.ownership.assign(trunk, 1L)
            random.next = 0
            random.then = 80
            random.then = 1
            player.opNpc1(trunk)
            advance(5)
            assertEquals(2, player.count(note(d, objs.yew_logs)))
            assertEquals(0, player.count(objs.yew_logs))
            assertEquals(250, player.statMap.getFineXP(stats.woodcutting))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.ChopWildernessEntYew))
        }

    @Test
    fun GameTestState.`guild trunk gives one noted log and cannot complete Wilderness diary`() =
        test { d ->
            prepare(61)
            val trunk = trunk(d, CoordGrid(1600, 9900))
            d.ownership.assign(trunk, 1L)
            player.inv[0] = InvObj(objs.mithril_axe)
            random.next = 0
            random.then = 80
            random.then = 1
            player.withProtectedAccess { d.chopping.chop(this, trunk) }
            advance(4)
            assertEquals(1, player.count(note(d, objs.yew_logs)))
            assertEquals(250, player.statMap.getFineXP(stats.woodcutting))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopWildernessEntYew))
        }

    @Test
    fun GameTestState.`base level determines logs while boosts affect only success`() = test { d ->
        prepare(60)
        player.setCurrentLevel(stats.woodcutting, 99)
        val trunk = trunk(d)
        d.ownership.assign(trunk, 1L)
        player.inv[0] = InvObj(objs.mithril_axe)
        random.next = 0
        random.then = 80
        random.then = 1
        player.withProtectedAccess { d.chopping.chop(this, trunk) }
        advance(4)
        assertEquals(2, player.count(note(d, objs.maple_logs)))
        assertEquals(0, player.count(note(d, objs.yew_logs)))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopWildernessEntYew))
        assertNull(EntLogs.select(99, 10))
        assertEquals(objs.magic_logs, EntLogs.select(75, 76))
        assertEquals(objs.maple_logs, EntLogs.select(61, 75))
        assertEquals(objs.yew_logs, EntLogs.select(61, 76))
        assertEquals(objs.maple_logs, EntLogs.select(60, 170))
    }

    @Test
    fun GameTestState.`expiry or axe loss during chopping prevents delivery`() = test { d ->
        prepare(61)
        val trunk = trunk(d)
        d.ownership.assign(trunk, 1L)
        player.inv[0] = InvObj(objs.mithril_axe)
        player.withProtectedAccess { d.chopping.chop(this, trunk) }
        advance(2)
        d.ownership.forget(trunk)
        advance(2)
        assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
        assertEquals(0, player.count(note(d, objs.yew_logs)))
        d.ownership.assign(trunk, 1L)
        player.withProtectedAccess { d.chopping.chop(this, trunk) }
        advance(2)
        player.inv[0] = null
        advance(2)
        assertEquals(0, player.count(note(d, objs.yew_logs)))
    }

    @Test
    fun GameTestState.`full inventory rejects new logs but can extend an existing noted stack`() =
        test { d ->
            prepare(61)
            val trunk = trunk(d)
            d.ownership.assign(trunk, 1L)
            player.inv[0] = InvObj(objs.mithril_axe)
            for (i in 1..27) player.inv[i] = InvObj(objs.knife)
            player.withProtectedAccess { d.chopping.chop(this, trunk) }
            advance(4)
            assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopWildernessEntYew))
            player.inv[1] = InvObj(note(d, objs.yew_logs))
            random.next = 0
            random.then = 80
            random.then = 1
            player.withProtectedAccess { d.chopping.chop(this, trunk) }
            advance(4)
            assertEquals(3, player.count(note(d, objs.yew_logs)))
        }

    @Test
    fun GameTestState
        .`Wilderness medium completion increases actual success without changing log quantity`() =
        test { d ->
            prepare(61)
            val trunk = trunk(d)
            d.ownership.assign(trunk, 1L)
            player.inv[0] = InvObj(objs.mithril_axe)
            random.next = 26
            player.withProtectedAccess { d.chopping.chop(this, trunk) }
            advance(4)
            assertEquals(0, player.count(note(d, objs.yew_logs)))
            player.cancelActiveCoroutine()
            advance(4)
            player.withProtectedAccess {
                vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Medium).complete] = 1
            }
            random.next = 26
            random.then = 80
            random.then = 1
            player.withProtectedAccess { d.chopping.chop(this, trunk) }
            advance(4)
            assertEquals(2, player.count(note(d, objs.yew_logs)))
        }

    @Test
    fun GameTestState.`bird nest substitutes for logs without XP or yew diary`() = test { d ->
        prepare(61)
        val trunk = trunk(d)
        d.ownership.assign(trunk, 1L)
        player.inv[0] = InvObj(objs.mithril_axe)
        random.next = 0
        random.then = 80
        random.then = 0
        random.then = 35
        random.then = 0
        player.withProtectedAccess { d.chopping.chop(this, trunk) }
        advance(4)
        assertEquals(0, player.count(note(d, objs.yew_logs)))
        assertEquals(0, player.statMap.getFineXP(stats.woodcutting))
        assertFalse(player.hasCompletedDiaryTask(DiaryTask.ChopWildernessEntYew))
        assertTrue(d.ground.findAll(player.coords.translate(-1, -1)).any())
    }

    @Test
    fun GameTestState
        .`native guild cave and vine provide gated ordinary access on walkable adjacent tiles`() =
        test { d ->
            prepare(59)
            for (c in listOf(CoordGrid(1606, 3508), CoordGrid(1596, 9900))) for (x in
                -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(c.translate(x, z))
            player.telejump(CoordGrid(1606, 3508))
            val cave =
                placeMapLoc(
                    CoordGrid(1601, 3506),
                    EntCaves.enter,
                    angle = org.rsmod.game.loc.LocAngle.South,
                )
            player.setCurrentLevel(stats.woodcutting, 99)
            player.opLoc1(cave)
            advance(2)
            assertEquals(CoordGrid(1606, 3508), player.coords)
            player.setBaseLevel(stats.woodcutting, 60)
            player.opLoc1(cave)
            advance(2)
            assertEquals(CoordGrid(1596, 9900), player.coords)
            val vine =
                placeMapLoc(
                    CoordGrid(1597, 9900),
                    EntCaves.exit,
                    shape = org.rsmod.game.loc.LocShape.WallStraight,
                )
            player.opLoc1(vine)
            advance(2)
            assertEquals(CoordGrid(1606, 3508), player.coords)
        }

    private fun GameTestScope.prepare(level: Int) {
        player.members = true
        player.uuid = 1L
        player.observerUUID = 1L
        player.stats[stats.woodcutting] = level
        player.statMap.setFineXP(stats.woodcutting, 0)
    }

    private fun GameTestScope.trunk(d: Deps, c: CoordGrid = CoordGrid(3300, 3650)): Npc {
        for (x in -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(c.translate(x, z))
        player.telejump(c.translateX(-1))
        return spawnNpc(c, d.npcs[EntNpcs.trunk])
    }

    private fun note(d: Deps, type: org.rsmod.game.type.obj.ObjType) =
        d.types.getValue(d.types[type].certlink)

    private fun GameTestState.test(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(Deps::class, scripts = arrayOf(EntChopping::class), testBody = body)

    data class Deps
    @Inject
    constructor(
        val chopping: EntChopping,
        val ownership: EntTrunkOwnership,
        val npcs: NpcTypeList,
        val types: ObjTypeList,
        val ground: ObjRepository,
    )
}
