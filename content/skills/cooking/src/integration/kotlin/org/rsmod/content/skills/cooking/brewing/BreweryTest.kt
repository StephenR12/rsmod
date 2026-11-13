package org.rsmod.content.skills.cooking.brewing

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.front
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.cooking.*
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

@Execution(ExecutionMode.SAME_THREAD)
class BreweryTest {
    @Test
    fun GameTestState.`all ten native brews finish and deliver exactly eight pints with XP once`() =
        runBrewTest { d ->
            setup()
            for (recipe in AleRecipes.all) {
                resetVat(0)
                val vat = vat(0)
                prepare(d, vat, recipe)
                random.next = 100
                add(d, vat, BreweryObjs.yeast)
                assertEquals(recipe.preparation + 1, player.vars[BreweryBits.vat[0]])
                finish(d, 0)
                assertEquals(recipe.preparation + 3, player.vars[BreweryBits.vat[0]])
                val xp = player.statMap.getFineXP(stats.cooking)
                player.withProtectedAccess { assertTrue(d.brew.turn(this, valve(0))) }
                assertEquals(xp + (recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.cooking))
                player.withProtectedAccess { assertFalse(d.brew.turn(this, valve(0))) }
                assertEquals(xp + (recipe.xp * 10).toInt(), player.statMap.getFineXP(stats.cooking))
                repeat(8) {
                    player.inv[0] = InvObj(BreweryObjs.glass)
                    player.withProtectedAccess {
                        assertTrue(d.brew.collect(this, barrel(0), BreweryObjs.glass, 0))
                    }
                    assertEquals(recipe.normal.id, player.inv[0]?.id)
                }
                assertEquals(0, player.vars[BreweryBits.barrel[0]])
            }
        }

    @Test
    fun GameTestState
        .`three native sites keep independent persistent vat barrel and control bytes`() =
        runBrewTest { d ->
            setup()
            for (i in 0..2) {
                resetVat(i)
                val vat = vat(i)
                prepare(d, vat, AleRecipes.all[i])
                random.next = 17
                add(d, vat, BreweryObjs.yeast)
            }
            val first = player.vars[BreweryBits.vat[0]]
            player.withProtectedAccess { assertTrue(d.brew.turn(this, valve(1))) }
            assertEquals(first, player.vars[BreweryBits.vat[0]])
            assertEquals(4, player.vars[BreweryBits.barrel[1]])
            for (i in listOf(0, 2)) {
                finish(d, i)
                assertEquals(AleRecipes.all[i].preparation + 4, player.vars[BreweryBits.vat[i]])
            }
        }

    @Test
    fun GameTestState.`water malt and special ingredients require exact order and quantities`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            add(d, vat, BreweryObjs.malt, false)
            add(d, vat, BreweryObjs.yeast, false)
            add(d, vat, BreweryObjs.ingredients[0], false)
            add(d, vat, BreweryObjs.water)
            add(d, vat, BreweryObjs.malt, false)
            add(d, vat, BreweryObjs.water)
            add(d, vat, BreweryObjs.water, false)
            add(d, vat, BreweryObjs.malt)
            add(d, vat, BreweryObjs.stuff, false)
            add(d, vat, BreweryObjs.malt)
            add(d, vat, BreweryObjs.malt, false)
            add(d, vat, BreweryObjs.ingredients[0])
            add(d, vat, BreweryObjs.stuff, false)
            add(d, vat, BreweryObjs.ingredients[1], false)
            add(d, vat, BreweryObjs.yeast, false)
            repeat(3) { add(d, vat, BreweryObjs.ingredients[0]) }
            add(d, vat, BreweryObjs.ingredients[0], false)
            random.next = 100
            add(d, vat, BreweryObjs.yeast)
            assertEquals(BreweryObjs.pot.id, player.inv[0]?.id)
        }

    @Test
    fun GameTestState
        .`stuff improves mature outcome without changing bad chance and can prolong fermentation`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            for ((roll, stuff, state) in
                listOf(
                    Triple(16, false, 8),
                    Triple(16, true, 8),
                    Triple(60, false, 7),
                    Triple(60, true, 8),
                    Triple(0, true, 64),
                )) {
                resetVat(0)
                prepare(d, vat, AleRecipes.all[0], stuff)
                random.next = roll
                if (stuff) random.then = 16
                add(d, vat, BreweryObjs.yeast)
                val now = System.currentTimeMillis()
                val start = d.brew.growthCycle(player, now)
                player.setVarp(BreweryVars.started[0], start - 3)
                d.brew.update(player, now)
                if (stuff) assertEquals(6, player.vars[BreweryBits.vat[0]])
                player.setVarp(BreweryVars.started[0], start - 16)
                d.brew.update(player, now)
                assertEquals(state, player.vars[BreweryBits.vat[0]])
            }
        }

    @Test
    fun GameTestState.`cider uses four apple mush and never water malt or stuff`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            add(d, vat, BreweryObjs.mush)
            assertEquals(BreweryObjs.bucket.id, player.inv[0]?.id)
            add(d, vat, BreweryObjs.water, false)
            add(d, vat, BreweryObjs.malt, false)
            add(d, vat, BreweryObjs.stuff, false)
            add(d, vat, BreweryObjs.yeast, false)
            repeat(3) { add(d, vat, BreweryObjs.mush) }
            add(d, vat, BreweryObjs.mush, false)
            random.next = 17
            add(d, vat, BreweryObjs.yeast)
            finish(d, 0)
            assertEquals(62, player.vars[BreweryBits.vat[0]])
        }

    @Test
    fun GameTestState
        .`fermentation uses player phased 640 minute cycles and preserves completed outcome`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            prepare(d, vat, AleRecipes.all[1])
            random.next = 100
            add(d, vat, BreweryObjs.yeast)
            val now = System.currentTimeMillis()
            val start = d.brew.growthCycle(player, now)
            player.setVarp(BreweryVars.started[0], start)
            d.brew.update(player, now)
            assertEquals(11, player.vars[BreweryBits.vat[0]])
            player.setVarp(BreweryVars.started[0], start - 1)
            d.brew.update(player, now)
            assertEquals(12, player.vars[BreweryBits.vat[0]])
            assertEquals(start + 1, d.brew.growthCycle(player, now + 640L * 60_000))
            finish(d, 0)
            val done = player.vars[BreweryBits.vat[0]]
            d.brew.update(player, now + 640L * 60_000 * 100)
            assertEquals(done, player.vars[BreweryBits.vat[0]])
        }

    @Test
    fun GameTestState.`full barrel blocks valve and draining unfinished brew gives no XP`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            add(d, vat, BreweryObjs.water)
            val xp = player.statMap.getFineXP(stats.cooking)
            player.setVarBit(BreweryBits.barrel[0], 15)
            player.withProtectedAccess {
                assertFalse(d.brew.turn(this, valve(0)))
                assertTrue(d.brew.drain(this, barrel(0)))
                assertTrue(d.brew.turn(this, valve(0)))
            }
            assertEquals(4, player.vars[BreweryBits.barrel[0]])
            assertEquals(0, player.vars[BreweryBits.vat[0]])
            assertEquals(xp, player.statMap.getFineXP(stats.cooking))
            player.inv[0] = InvObj(BreweryObjs.glass)
            player.withProtectedAccess {
                assertFalse(d.brew.collect(this, barrel(0), BreweryObjs.glass, 0))
            }
        }

    @Test
    fun GameTestState
        .`native item use reaches vat and full inventory keg delivery preserves other slots`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            player.inv[0] = InvObj(BreweryObjs.water)
            player.withProtectedAccess {
                d.locU.interactOp(
                    this,
                    vat,
                    vat,
                    locTypes.getValue(vat.id),
                    d.types[BreweryObjs.water],
                    player.inv,
                    0,
                )
            }
            assertEquals(1, player.vars[BreweryBits.vat[0]])
            assertEquals(BreweryObjs.bucket.id, player.inv[0]?.id)
            for (slot in player.inv.indices) player.inv[slot] = InvObj(objs.logs)
            player.inv[7] = InvObj(BreweryObjs.keg)
            player.setVarBit(BreweryBits.barrel[0], 143)
            player.withProtectedAccess {
                assertTrue(d.brew.collect(this, barrel(0), BreweryObjs.keg, 7))
            }
            assertEquals(BreweryObjs.matureKegs[0][3].id, player.inv[7]?.id)
            assertEquals(139, player.vars[BreweryBits.barrel[0]])
            assertEquals(objs.logs.id, player.inv[6]?.id)
        }

    @Test
    fun GameTestState
        .`all normal mature keg doses drink and pour preserving exact remaining containers`() =
        runBrewTest { d ->
            setup()
            for (recipe in AleRecipes.all) for (mature in listOf(false, true)) for (doses in 1..4) {
                player.clearInv()
                val source =
                    (if (mature) BreweryObjs.matureKegs[recipe.index]
                    else BreweryObjs.kegs[recipe.index])[doses - 1]
                assertEquals("Drink", d.types[source].iop[0])
                player.inv[3] = InvObj(source)
                player.inv[8] = InvObj(BreweryObjs.glass)
                player.withProtectedAccess {
                    assertTrue(d.drink.pour(this, recipe, mature, doses, 8, 3))
                }
                assertEquals((if (mature) recipe.mature else recipe.normal).id, player.inv[8]?.id)
                val remaining =
                    if (doses == 1) BreweryObjs.keg
                    else
                        (if (mature) BreweryObjs.matureKegs[recipe.index]
                        else BreweryObjs.kegs[recipe.index])[doses - 2]
                assertEquals(remaining.id, player.inv[3]?.id)
                d.food.clear(player)
                player.inv[3] = InvObj(source)
                player.withProtectedAccess { d.held.interact(this, player.inv, 3, HeldOp.Op1) }
                assertEquals(remaining.id, player.inv[3]?.id)
            }
        }

    @Test
    fun GameTestState
        .`ale effects cap boosts heal and repeat current based penalties with cooldown`() =
        runBrewTest { d ->
            setup()
            player.stats[stats.attack] = 50
            player.stats[stats.strength] = 50
            player.stats[stats.defence] = 50
            player.stats[stats.mining] = 50
            player.stats[stats.smithing] = 50
            player.setCurrentLevel(stats.hitpoints, 10)
            player.inv[0] = InvObj(BreweryObjs.mature[0])
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(52, player.stat(stats.mining))
            assertEquals(52, player.stat(stats.smithing))
            assertEquals(45, player.stat(stats.attack))
            assertEquals(12, player.stat(stats.hitpoints))
            assertEquals(BreweryObjs.glass.id, player.inv[0]?.id)
            player.inv[0] = InvObj(BreweryObjs.mature[0])
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(BreweryObjs.mature[0].id, player.inv[0]?.id)
            advance(3)
            player.withProtectedAccess { d.held.interact(this, player.inv, 0, HeldOp.Op1) }
            assertEquals(52, player.stat(stats.mining))
            assertEquals(41, player.stat(stats.attack))
        }

    @Test
    fun GameTestState
        .`all native yeast suppliers charge currency and an empty pot with ghostspeak gate`() =
        runBrewTest { d ->
            setup()
            for (index in 0..2) {
                player.clearInv()
                val coin = if (index == 1) BreweryObjs.tokens else objs.coins
                val cost = if (index == 1) 5 else 25
                player.inv[0] = InvObj(BreweryObjs.pot)
                player.inv[1] = InvObj(coin, cost)
                if (index == 1) {
                    player.withProtectedAccess { assertFalse(d.brew.buyYeast(this, index)) }
                    player.front = InvObj(BreweryObjs.ghostAmulets[0])
                }
                player.withProtectedAccess { assertTrue(d.brew.buyYeast(this, index)) }
                assertEquals(1, player.count(BreweryObjs.yeast))
                assertEquals(0, player.count(coin))
                assertEquals(0, player.count(BreweryObjs.pot))
            }
            player.clearInv()
            player.inv[0] = InvObj(BreweryObjs.pot)
            player.inv[1] = InvObj(objs.coins, 24)
            player.withProtectedAccess { assertFalse(d.brew.buyYeast(this, 0)) }
            assertEquals(24, player.count(objs.coins))
            assertEquals(1, player.count(BreweryObjs.pot))
        }

    @Test
    fun GameTestState
        .`apple press consumes exactly four apples and one bucket after interruptible fifteen tick crush`() =
        runBrewTest { d ->
            setup()
            val site = Breweries.all[0]
            val press = placeMapLoc(site.press, BreweryLocs.press)
            player.placeAt(site.press.translateZ(1))
            repeat(3) {
                player.inv[0] = InvObj(BreweryObjs.apple)
                player.withProtectedAccess { assertTrue(d.brew.loadApple(this, press, 0)) }
            }
            player.inv[0] = InvObj(BreweryObjs.bucket)
            player.withProtectedAccess { assertFalse(d.brew.drawMush(this, press, 0)) }
            assertEquals(BreweryObjs.bucket.id, player.inv[0]?.id)
            player.inv[0] = InvObj(BreweryObjs.apple)
            player.withProtectedAccess { d.brew.loadApple(this, press, 0) }
            advance(16)
            player.inv[0] = InvObj(BreweryObjs.bucket)
            player.withProtectedAccess { assertTrue(d.brew.drawMush(this, press, 0)) }
            assertEquals(BreweryObjs.mush.id, player.inv[0]?.id)
            player.inv[0] = InvObj(BreweryObjs.bucket)
            player.withProtectedAccess { assertFalse(d.brew.drawMush(this, press, 0)) }
        }

    @Test
    fun GameTestState
        .`member level stale slot and cross container gates leave production unchanged`() =
        runBrewTest { d ->
            setup()
            val vat = vat(0)
            player.members = false
            add(d, vat, BreweryObjs.water, false)
            player.members = true
            player.stats[stats.cooking] = 1
            add(d, vat, BreweryObjs.mush, false)
            player.stats[stats.cooking] = 99
            player.inv[0] = InvObj(objs.logs)
            player.withProtectedAccess { assertFalse(d.brew.add(this, vat, BreweryObjs.water, 0)) }
            assertEquals(0, player.vars[BreweryBits.vat[0]])
            player.inv[0] = InvObj(BreweryObjs.kegs[0][3])
            player.withProtectedAccess {
                assertFalse(d.drink.pour(this, AleRecipes.all[0], false, 4, 0, 0))
            }
            assertEquals(BreweryObjs.kegs[0][3].id, player.inv[0]?.id)
            player.inv[0] = InvObj(BreweryObjs.fruit)
            player.withProtectedAccess { assertTrue(d.brew.hollow(this, 0)) }
            assertEquals(BreweryObjs.keg.id, player.inv[0]?.id)
        }

    @Test
    fun GameTestState.`malt recipe accepts range only with level one zero XP and no burning`() =
        runBrewTest { d ->
            setup()
            player.stats[stats.cooking] = 1
            val recipe = StandardCookingRecipes.all.single { it.raw == BreweryObjs.barley }
            assertEquals(1, recipe.level)
            assertEquals(0.0, recipe.xp)
            assertNull(recipe.burnt)
            assertTrue(recipe.alwaysSucceeds)
            assertFalse(recipe.allowedSources.contains(CookingSourceKind.Fire))
            assertTrue(recipe.allowedSources.contains(CookingSourceKind.Range))
            assertTrue(d.types[BreweryObjs.normal[0]].iop[0] == "Drink")
        }

    @Test
    fun GameTestState
        .`walking interrupts apple crushing and a bucket can resume the loaded press without duplicate mush`() =
        runBrewTest { d ->
            setup()
            val site = Breweries.all[1]
            val press = placeMapLoc(site.press, BreweryLocs.press)
            player.placeAt(site.press.translateZ(2))
            repeat(4) {
                player.inv[0] = InvObj(BreweryObjs.apple)
                player.withProtectedAccess {
                    d.locU.interactOp(
                        this,
                        press,
                        press,
                        locTypes.getValue(press.id),
                        d.types[BreweryObjs.apple],
                        player.inv,
                        0,
                    )
                }
            }
            advance()
            val origin = player.coords
            player.moveGameClick(origin.translateX(-1))
            advance(20)
            assertEquals(0, player.count(BreweryObjs.mush))
            assertNotEquals(origin, player.coords)
            player.inv[0] = InvObj(BreweryObjs.bucket)
            var delivered = false
            player.withProtectedAccess { delivered = d.brew.drawMush(this, press, 0) }
            advance(16)
            assertTrue(delivered)
            assertEquals(BreweryObjs.mush.id, player.inv[0]?.id)
            player.inv[0] = InvObj(BreweryObjs.bucket)
            var duplicate = false
            player.withProtectedAccess { duplicate = d.brew.drawMush(this, press, 0) }
            assertFalse(duplicate)
        }

    @Test
    fun GameTestState
        .`barley is malted through ordinary cooking on a range and rejected on a fire`() =
        runBrewTest { d ->
            setup()
            player.stats[stats.cooking] = 1
            val recipe = StandardCookingRecipes.byRaw.getValue(BreweryObjs.barley.id)
            val range = placeMapLoc(org.rsmod.map.CoordGrid(2000, 2000), cooking_locs.range)
            player.placeAt(range.coords.translateX(-1))
            player.inv[0] = InvObj(BreweryObjs.barley)
            player.withProtectedAccess {
                d.cooking.start(this, range, CookingSourceKind.Fire, recipe, 1)
            }
            advance(5)
            assertEquals(BreweryObjs.barley.id, player.inv[0]?.id)
            player.withProtectedAccess {
                d.cooking.start(this, range, CookingSourceKind.Range, recipe, 1)
            }
            advance(5)
            assertEquals(BreweryObjs.malt.id, player.inv[0]?.id)
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
        }

    private fun GameTestScope.setup() {
        player.members = true
        player.stats[stats.cooking] = 99
        player.stats[stats.hitpoints] = 50
        for (i in 0..2) resetVat(i)
    }

    private fun GameTestScope.resetVat(i: Int) {
        player.setVarBit(BreweryBits.vat[i], 0)
        player.setVarBit(BreweryBits.barrel[i], 0)
        player.setVarp(BreweryVars.started[i], 0)
        player.setVarp(
            BreweryVars.control,
            player.vars[BreweryVars.control] and (255 shl (i * 8)).inv(),
        )
    }

    private fun GameTestScope.vat(i: Int): BoundLocInfo {
        val loc = placeMapLoc(Breweries.all[i].vat, BreweryLocs.vats[i])
        player.placeAt(loc.coords.translateZ(1))
        return loc
    }

    private fun GameTestScope.valve(i: Int): BoundLocInfo =
        placeMapLoc(Breweries.all[i].valve, BreweryLocs.valves[i])

    private fun GameTestScope.barrel(i: Int): BoundLocInfo =
        placeMapLoc(Breweries.all[i].barrel, BreweryLocs.barrels[i])

    private fun GameTestScope.add(
        d: Deps,
        vat: BoundLocInfo,
        input: ObjType,
        expected: Boolean = true,
    ) {
        player.inv[0] = InvObj(input)
        player.withProtectedAccess {
            assertEquals(
                expected,
                d.brew.add(this, vat, input, 0),
                "input ${input.id} at state ${player.vars[BreweryBits.vat[Breweries.all.indexOfFirst{it.vat==vat.coords}]]}",
            )
        }
    }

    private fun GameTestScope.prepare(
        d: Deps,
        vat: BoundLocInfo,
        recipe: AleRecipe,
        stuff: Boolean = false,
    ) {
        if (recipe.index != 9) {
            repeat(2) { add(d, vat, BreweryObjs.water) }
            repeat(2) { add(d, vat, BreweryObjs.malt) }
            if (stuff) add(d, vat, BreweryObjs.stuff)
        }
        repeat(recipe.quantity) { add(d, vat, recipe.ingredient) }
    }

    private fun GameTestScope.finish(d: Deps, i: Int) {
        val now = System.currentTimeMillis()
        player.setVarp(BreweryVars.started[i], d.brew.growthCycle(player, now) - 16)
        d.brew.update(player, now)
    }

    private fun GameTestState.runBrewTest(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            Module,
            Brewery::class,
            AleDrinking::class,
            CookingScript::class,
            testBody = body,
        )

    private object Module : AbstractModule() {
        override fun configure() {
            install(CookingModule())
        }
    }

    data class Deps
    @Inject
    constructor(
        val brew: Brewery,
        val drink: AleDrinking,
        val food: FoodConsumptionRepository,
        val held: HeldInteractions,
        val locU: LocUInteractions,
        val types: ObjTypeList,
        val cooking: StandardCooking,
    )
}
