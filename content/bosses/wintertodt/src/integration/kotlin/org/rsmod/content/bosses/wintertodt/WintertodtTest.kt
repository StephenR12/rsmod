package org.rsmod.content.bosses.wintertodt

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
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.cooking.OrdinaryFoodEating
import org.rsmod.content.skills.firemaking.FiremakingXp
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Execution(ExecutionMode.SAME_THREAD)
class WintertodtTest {
    @Test
    fun GameTestState.`native tools actions HUD and exact material tables are available`() =
        test { d ->
            assertEquals("Chop", d.locs[WinterLocs.roots].op[0])
            assertEquals("Light", d.locs[WinterLocs.unlit].op[0])
            assertEquals("Fix", d.locs[WinterLocs.broken].op[0])
            assertEquals("Feed", d.locs[WinterLocs.lit].op[0])
            assertEquals("Search", d.locs[WinterLocs.cart].op[0])
            assertEquals("Big-search", d.locs[WinterLocs.cart].op[2])
            assertTrue(d.events.contains(LocEvents.Op1::class.java, WinterLocs.roots.id))
            assertEquals(60, WinterMaterials.tables.sumOf { it.size })
            assertEquals(6, WinterMaterials.tables.size)
            for (table in WinterMaterials.tables) {
                assertEquals(255, table.last().low)
                assertEquals(255, table.last().high)
            }
            assertEquals(0, WinterMaterials.threshold(1, -60, 60))
            assertEquals(61, WinterMaterials.threshold(99, -60, 60))
            assertTrue(WinterWarmClothing.all.size >= 240)
            assertEquals(20695, WinterObjs.root.id)
            assertEquals(20699, WinterObjs.potions[3].id)
            assertEquals(11434, WinterBits.warmth.id)
            assertEquals(11435, WinterBits.rewards.id)
        }

    @Test
    fun GameTestState
        .`native door requires base fifty Firemaking and exit destroys only temporary supplies`() =
        test { d ->
            player.members = true
            player.stats[stats.firemaking] = 49
            val door = placeMapLoc(CoordGrid(1627, 3964), WinterLocs.door)
            player.telejump(CoordGrid(1629, 3963))
            player.opLoc1(door)
            advance(2)
            assertFalse(WintertodtRound.inside(player.coords))
            player.stats[stats.firemaking] = 50
            player.opLoc1(door)
            advance(2)
            assertTrue(WintertodtRound.inside(player.coords))
            assertEquals(1000, player.winterWarmth)
            player.inv[0] = InvObj(WinterObjs.root)
            player.inv[1] = InvObj(WinterObjs.potions[3])
            player.inv[2] = InvObj(objs.knife)
            d.round.score(player, 500)
            d.round.leave(player)
            assertEquals(0, player.count(WinterObjs.root))
            assertEquals(0, player.count(WinterObjs.potions[3]))
            assertEquals(1, player.count(objs.knife))
            assertEquals(0, player.winterPoints)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SubdueWintertodt))
        }

    @Test
    fun GameTestState
        .`real chopping and fletching produce only their scaled XP and respect lost tools`() =
        test { d ->
            prepare(d, CoordGrid(1621, 3987))
            player.stats[stats.woodcutting] = 99
            player.stats[stats.fletching] = 10
            player.inv[0] = InvObj(objs.rune_axe)
            player.inv[1] = InvObj(objs.knife)
            val root = placeMapLoc(CoordGrid(1620, 3988), WinterLocs.roots)
            val wc = player.statMap.getFineXP(stats.woodcutting)
            random.next = 0
            player.withProtectedAccess { d.actions.chop(this, root) }
            advance(3)
            assertEquals(1, player.count(WinterObjs.root))
            assertEquals(297, player.statMap.getFineXP(stats.woodcutting) - wc)
            player.inv[0] = null
            advance(3)
            assertEquals(1, player.count(WinterObjs.root))
            random.next = 100
            val fletch = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess { d.actions.fletch(this) }
            advance(4)
            assertEquals(0, player.count(WinterObjs.root))
            assertEquals(1, player.count(WinterObjs.kindling))
            assertEquals(60, player.statMap.getFineXP(stats.fletching) - fletch)
            assertEquals(0, player.winterPoints)
        }

    @Test
    fun GameTestState
        .`supplies herbs and Brewma make usable warmth potions without a quest bypass XP reward`() =
        test { d ->
            prepare(d, CoordGrid(1626, 3977))
            player.withProtectedAccess { d.actions.supply(this, WinterObjs.unf, 5) }
            assertEquals(5, player.count(WinterObjs.unf))
            val herbs = placeMapLoc(CoordGrid(1625, 3978), WinterLocs.herb)
            val farming = player.statMap.getFineXP(stats.farming)
            val herblore = player.statMap.getFineXP(stats.herblore)
            player.withProtectedAccess { d.actions.pickHerb(this, herbs) }
            advance(2)
            assertEquals(1, player.count(WinterObjs.herb))
            assertEquals(farming, player.statMap.getFineXP(stats.farming))
            for (slot in player.inv.indices) if (player.inv[slot] == null)
                player.inv[slot] = InvObj(objs.knife)
            advance(3)
            for (slot in player.inv.indices) if (player.inv[slot]?.id == objs.knife.id)
                player.inv[slot] = null
            player.withProtectedAccess { d.actions.mix(this) }
            assertEquals(0, player.count(WinterObjs.potions[3]))
            player.withProtectedAccess { d.actions.mix(this, brewma = true) }
            assertEquals(1, player.count(WinterObjs.potions[3]))
            assertEquals(herblore, player.statMap.getFineXP(stats.herblore))
            player.winterWarmth = 500
            val slot = player.inv.indexOfFirst { it?.id == WinterObjs.potions[3].id }
            player.withProtectedAccess { d.actions.drink(this, slot, WinterObjs.potions[3]) }
            assertEquals(800, player.winterWarmth)
            assertEquals(1, player.count(WinterObjs.potions[2]))
        }

    @Test
    fun GameTestState.`only consumed foods healing at least four restore warmth`() = test { d ->
        prepare(d, CoordGrid(1629, 3971))
        player.winterWarmth = 100
        player.inv[0] = InvObj(objs.bread)
        player.inv[1] = InvObj(objs.shrimps)
        random.next = 100
        player.withProtectedAccess { d.food.eat(this, 0, objs.bread, 5) }
        assertEquals(450, player.winterWarmth)
        assertEquals(0, player.count(objs.bread))
        advance(3)
        val beforeShrimps = player.winterWarmth
        player.withProtectedAccess { d.food.eat(this, 1, objs.shrimps, 3) }
        assertEquals(beforeShrimps, player.winterWarmth)
        assertEquals(0, player.count(objs.shrimps))
    }

    @Test
    fun GameTestState
        .`pyromancer healing consumes one dose awards seventy five and relighting requires a healthy pyro`() =
        test { d ->
            prepare(d, CoordGrid(1619, 3995))
            random.next = 100
            val station = d.round.stations[0]
            val npc = spawnNpc(station.pyroCoords, d.npcs[WinterNpcs.pyro]) { noneMode() }
            station.health = 0
            player.inv[0] = InvObj(WinterObjs.potions[3])
            player.inv[1] = InvObj(objs.tinderbox)
            val brazier = placeMapLoc(station.coords, WinterLocs.unlit)
            player.telejump(station.coords.translate(0, -1))
            player.withProtectedAccess { d.actions.brazier(this, brazier) }
            advance(4)
            assertEquals(WinterBrazier.Unlit, station.state)
            assertEquals(0, player.winterPoints)
            player.telejump(npc.coords.translate(0, -1))
            player.withProtectedAccess { d.actions.help(this, npc) }
            assertEquals(14, station.health)
            assertEquals(75, player.winterPoints)
            assertEquals(1, player.count(WinterObjs.potions[2]))
            player.withProtectedAccess { d.actions.help(this, npc) }
            assertEquals(75, player.winterPoints)
            assertEquals(1, player.count(WinterObjs.potions[2]))
        }

    @Test
    fun GameTestState
        .`actual five hundred point round earns two persistent cart rolls XP and diary`() =
        test { d ->
            prepare(d, CoordGrid(1621, 3996))
            random.next = 100
            player.inv[0] = InvObj(objs.tinderbox)
            repeat(19) { player.inv[it + 1] = InvObj(WinterObjs.kindling) }
            val station = d.round.stations[0]
            val unlit = placeMapLoc(station.coords, WinterLocs.unlit)
            player.withProtectedAccess { d.actions.brazier(this, unlit) }
            advance(4)
            assertEquals(25, player.winterPoints)
            val lit = placeMapLoc(station.coords, WinterLocs.lit)
            player.withProtectedAccess { d.actions.brazier(this, lit) }
            advance(57)
            assertEquals(500, player.winterPoints)
            WinterObjs.pyro.forEachIndexed { slot, obj -> player.worn[slot] = InvObj(obj) }
            val fm = player.statBase(stats.firemaking)
            val xp = player.statMap.getFineXP(stats.firemaking)
            advance(1450)
            assertEquals(2, player.winterRewards)
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.SubdueWintertodt))
            assertEquals(fm * 1000, player.statMap.getFineXP(stats.firemaking) - xp)
        }

    @Test
    fun GameTestState
        .`four hundred ninety five points and leaving before subdual earn no diary or rewards`() =
        test { d ->
            prepare(d, CoordGrid(1621, 3996))
            random.next = 100
            player.inv[0] = InvObj(objs.tinderbox)
            repeat(14) { player.inv[it + 1] = InvObj(WinterObjs.kindling) }
            repeat(12) { player.inv[it + 15] = InvObj(WinterObjs.root) }
            val station = d.round.stations[0]
            val unlit = placeMapLoc(station.coords, WinterLocs.unlit)
            player.withProtectedAccess { d.actions.brazier(this, unlit) }
            advance(4)
            val lit = placeMapLoc(station.coords, WinterLocs.lit)
            player.withProtectedAccess { d.actions.brazier(this, lit) }
            advance(78)
            assertEquals(495, player.winterPoints)
            advance(1450)
            assertEquals(0, player.winterRewards)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SubdueWintertodt))
            d.round.beginRound()
            d.round.score(player, 500)
            d.round.light(d.round.stations[0], player)
            d.round.leave(player)
            advance(1450)
            assertEquals(0, player.winterRewards)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SubdueWintertodt))
        }

    @Test
    fun GameTestState
        .`unique cascade skips owned Phoenix and substitutes three owned warm gloves`() =
        test { d ->
            prepare(d)
            random.next = 0
            assertEquals(WinterObjs.phoenix.id, d.loot.roll(player).id)
            repeat(3) { player.inv[it] = InvObj(WinterObjs.gloves) }
            random.then = 1
            random.then = 1
            random.then = 1
            random.then = 0
            assertEquals(WinterObjs.magicSeed.id, d.loot.roll(player).id)
            player.clearInv()
            player.inv[0] = InvObj(WinterObjs.phoenix)
            random.next = 0
            assertEquals(objs.dragon_axe.id, d.loot.roll(player).id)
        }

    @Test
    fun GameTestState
        .`native cart full inventory preserves owed rolls then delivers committed loot`() =
        test { d ->
            player.members = true
            player.winterRewards = 2
            player.telejump(CoordGrid(1636, 3944))
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.knife)
            val cart = placeMapLoc(CoordGrid(1635, 3941), WinterLocs.cart, angle = LocAngle.East)
            player.opLoc3(cart)
            advance()
            assertEquals(2, player.winterRewards)
            assertMessageSent("Make some inventory space before collecting your rewards.")
            player.inv[0] = null
            random.next = 0
            player.opLoc3(cart)
            advance(4)
            assertEquals(1, player.winterRewards)
            assertEquals(WinterObjs.phoenix.id, player.vars[cacheTypes.varps.getValue(65725)])
        }

    @Test
    fun GameTestState
        .`warm clothing caps at four and cold interrupts kindling while roots continue`() =
        test { d ->
            prepare(d, CoordGrid(1621, 3987))
            assertEquals(32, d.round.damagePercent(player, 0))
            WinterObjs.pyro.forEachIndexed { slot, obj -> player.worn[slot] = InvObj(obj) }
            d.round.stations.forEach { it.state = WinterBrazier.Lit }
            assertEquals(4, d.round.warmItems(player))
            assertEquals(12, d.round.damagePercent(player, 0))
            player.clearAllInvs()
            d.round.stations.forEach { it.state = WinterBrazier.Unlit }
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(WinterObjs.root)
            player.inv[2] = InvObj(objs.rune_axe)
            advance(8)
            NoHazards.attacks = true
            val xp = player.statMap.getFineXP(stats.fletching)
            player.withProtectedAccess { d.actions.fletch(this) }
            advance(4)
            assertEquals(1, player.count(WinterObjs.root))
            assertEquals(0, player.count(WinterObjs.kindling))
            assertEquals(xp, player.statMap.getFineXP(stats.fletching))
            val root = placeMapLoc(CoordGrid(1620, 3988), WinterLocs.roots)
            random.next = 0
            player.withProtectedAccess { d.actions.chop(this, root) }
            advance(9)
            assertEquals(4, player.count(WinterObjs.root))
        }

    @Test
    fun GameTestState.`zero warmth uses unsafe death path and forfeits pending points`() =
        test { d ->
            prepare(d, CoordGrid(1621, 3996))
            player.winterWarmth = 1
            d.round.score(player, 500)
            NoHazards.attacks = true
            advance(10)
            assertEquals(0, player.winterSession)
            assertEquals(0, player.winterPoints)
            assertEquals(0, player.winterRewards)
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.SubdueWintertodt))
        }

    @Test
    fun GameTestState.`brazier shattering damages the adjacent perimeter after its warning`() =
        test { d ->
            prepare(d, CoordGrid(1639, 3996))
            advance()
            NoHazards.attacks = true
            NoHazards.kind = 1
            advance(20)
            player.winterWarmth = 1000
            advance(3)
            assertEquals(WinterBrazier.Broken, d.round.stations[1].state)
            assertEquals(600, player.winterWarmth)
        }

    @Test
    fun GameTestState
        .`native pyromancer attacks incapacitate after two hits and healing restores channeling`() =
        test { d ->
            prepare(d, CoordGrid(1629, 3971))
            NoHazards.attacks = true
            NoHazards.kind = 3
            advance(41)
            val station = d.round.stations[3]
            assertEquals(0, station.health)
            assertEquals(WinterNpcs.down.id, station.npc?.visType?.id)
            player.telejump(station.pyroCoords.translate(0, -1))
            player.inv[0] = InvObj(WinterObjs.potions[3])
            player.withProtectedAccess { d.actions.help(this, station.npc!!) }
            assertEquals(14, station.health)
            assertEquals(75, player.winterPoints)
            assertEquals(1, player.count(WinterObjs.potions[2]))
        }

    private fun GameTestScope.prepare(d: Dependencies, tile: CoordGrid = CoordGrid(1621, 3996)) {
        player.members = true
        player.stats[stats.firemaking] = 50
        player.stats[stats.woodcutting] = 99
        player.telejump(tile)
        allocZoneCollision(tile)
        d.round.join(player)
        d.round.beginRound()
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(Dependencies::class, childModule = Module, WintertodtScript::class) {
            deps ->
            for (station in deps.round.stations) for (dx in -8..8 step 8) for (dz in
                -8..8 step 8) allocZoneCollision(station.coords.translate(dx, dz))
            NoHazards.attacks = false
            NoHazards.kind = 0
            deps.round.startup()
            body(deps)
        }

    private object Module : AbstractModule() {
        @Provides
        @Singleton
        fun round(
            clock: MapClock,
            locs: LocRepository,
            collision: CollisionFlagMap,
            npcs: NpcRepository,
            npcTypes: NpcTypeList,
            controllers: ControllerRepository,
            events: EventBus,
            xp: FiremakingXp,
        ): WintertodtRound =
            WintertodtRound(
                clock,
                locs,
                collision,
                npcs,
                npcTypes,
                controllers,
                NoHazards,
                events,
                xp,
            )
    }

    private object NoHazards : GameRandom {
        var attacks = false
        var kind = 0

        override fun of(maxExclusive: Int) =
            if (attacks) if (maxExclusive == 4) kind else 0 else maxExclusive - 1

        override fun of(minInclusive: Int, maxInclusive: Int) = minInclusive

        override fun randomDouble() = 0.99
    }

    data class Dependencies
    @Inject
    constructor(
        val round: WintertodtRound,
        val actions: WintertodtActions,
        val rewards: WintertodtRewards,
        val loot: WintertodtLoot,
        val food: OrdinaryFoodEating,
        val locs: LocTypeList,
        val npcs: NpcTypeList,
        val objs: ObjTypeList,
        val events: EventBus,
    )
}
