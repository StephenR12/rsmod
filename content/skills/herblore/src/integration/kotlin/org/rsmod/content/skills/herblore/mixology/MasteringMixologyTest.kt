package org.rsmod.content.skills.herblore.mixology

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.herblore.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

@Execution(ExecutionMode.SAME_THREAD)
class MasteringMixologyTest {
    @Test
    fun GameTestState.`native recipe and processing enums agree with actual HUD values`() =
        game { _ ->
            for (recipe in MixRecipes.all) {
                assertEquals(
                    recipe.unfinished.id,
                    cacheTypes.enums.getValue(5542).primitiveMap[recipe.nativeType],
                )
                assertEquals(
                    recipe.level,
                    cacheTypes.enums.getValue(5548).primitiveMap[recipe.unfinished.id],
                )
            }
            assertEquals("Homogenous", cacheTypes.enums.getValue(5545).primitiveMap[1])
            assertEquals("Concentrated", cacheTypes.enums.getValue(5545).primitiveMap[2])
            assertEquals("Crystalised", cacheTypes.enums.getValue(5545).primitiveMap[3])
        }

    @Test
    fun GameTestState
        .`all thirty ordinary clean herb and unfinished inputs refine atomically for six XP each`() =
        game { d ->
            setup()
            val refiner = placeMapLoc(CoordGrid(1399, 9312), MixLocs.refiner)
            player.placeAt(refiner.coords.translateX(-1))
            for ((id, output) in MixRecipes.herbs) {
                player.clearInv()
                player.inv[0] = InvObj(d.items.getValue(id))
                val xp = player.statMap.getFineXP(stats.herblore)
                player.withProtectedAccess {
                    d.locU.interactOp(
                        this,
                        refiner,
                        refiner,
                        locTypes.getValue(refiner.id),
                        d.items.getValue(id),
                        inv,
                        0,
                    )
                }
                advance(3)
                assertEquals(output.second, player.count(MixObjs.paste[output.first]))
                assertEquals(xp + 60, player.statMap.getFineXP(stats.herblore))
                assertEquals(0, player.count(d.items.getValue(id)))
            }
        }

    @Test
    fun GameTestState.`refiner rejects noted herbs and walking interrupts the next paste yield`() =
        game { d ->
            setup()
            val source = placeMapLoc(CoordGrid(1399, 9312), MixLocs.refiner)
            player.placeAt(source.coords.translateX(-1))
            val note = d.items.getValue(d.items[objs.guam_leaf].certlink)
            player.inv[0] = InvObj(note, 2)
            player.withProtectedAccess { d.game.refine(this, source) }
            advance(3)
            assertEquals(2, player.count(note))
            assertEquals(0, player.count(MixObjs.paste[0]))
            player.inv[1] = InvObj(objs.guam_leaf)
            player.inv[2] = InvObj(objs.guam_leaf)
            player.withProtectedAccess { d.game.refine(this, source) }
            advance()
            player.moveGameClick(player.coords.translateX(-1))
            advance(6)
            assertTrue(player.count(objs.guam_leaf) >= 1)
        }

    @Test
    fun GameTestState.`hopper caps each paste at three thousand without losing overflow`() =
        game { d ->
            setup()
            for (i in 0..2) {
                player.setVarBit(MixBits.available[i], 2995)
                player.inv[i] = InvObj(MixObjs.paste[i], 12)
            }
            player.withProtectedAccess { assertTrue(d.game.deposit(this)) }
            for (i in 0..2) {
                assertEquals(3000, player.vars[MixBits.available[i]])
                assertEquals(7, player.count(MixObjs.paste[i]))
            }
            player.withProtectedAccess { assertFalse(d.game.deposit(this)) }
        }

    @Test
    fun GameTestState
        .`lever mistakes slide the oldest paste out and do not spend hopper material`() =
        game { d ->
            setup()
            player.withProtectedAccess {
                d.game.pull(this, 0)
                d.game.pull(this, 0)
                d.game.pull(this, 0)
                d.game.pull(this, 1)
            }
            assertEquals(listOf(1, 1, 2), MixBits.mixer.map { player.vars[it] })
            assertEquals(2, player.vars[MixBits.vessel])
            for (bit in MixBits.available) assertEquals(3000, player.vars[bit])
            player.withProtectedAccess { assertTrue(d.game.takeBase(this)) }
            assertEquals(2980, player.vars[MixBits.available[0]])
            assertEquals(2990, player.vars[MixBits.available[1]])
            assertEquals(1, player.count(MixRecipes.all[1].unfinished))
        }

    @Test
    fun GameTestState
        .`all ten mixtures consume exact paste and every workstation delivers a native finished potion`() =
        game { d ->
            setup()
            for (recipe in MixRecipes.all) for (machine in 0..2) {
                player.clearInv()
                fillHopper()
                make(d, recipe)
                assertEquals(1, player.count(recipe.unfinished))
                for (i in 0..2) assertEquals(
                    3000 - recipe.mix[i] * 10,
                    player.vars[MixBits.available[i]],
                )
                val source = machine(machine)
                player.opLoc1(source)
                advance(33)
                val finished = player.inv.firstOrNull { it?.id == recipe.finished.id }
                assertNotNull(finished, "${recipe.index}/$machine")
                assertEquals(
                    listOf(2, 1, 3)[machine],
                    requireNotNull(finished).vars.getBits(MixState.state.bits) and 3,
                )
            }
        }

    @Test
    fun GameTestState
        .`insufficient paste level and full inventory leave mixture and hopper unchanged`() =
        game { d ->
            setup()
            val recipe = MixRecipes.all[9]
            player.withProtectedAccess { for (i in 0..2) d.game.pull(this, i) }
            player.stats[stats.herblore] = 80
            player.withProtectedAccess { assertFalse(d.game.takeBase(this)) }
            player.stats[stats.herblore] = 99
            player.setVarBit(MixBits.available[2], 0)
            player.withProtectedAccess { assertFalse(d.game.takeBase(this)) }
            player.setVarBit(MixBits.available[2], 3000)
            player.fillInv()
            player.withProtectedAccess { assertFalse(d.game.takeBase(this)) }
            assertEquals(recipe.nativeType, player.vars[MixBits.vessel])
            for (bit in MixBits.available) assertEquals(3000, player.vars[bit])
        }

    @Test
    fun GameTestState
        .`ordered one two and three deliveries grant exact twenty and forty percent resin bonuses`() =
        game { d ->
            setup()
            val recipe = MixRecipes.all[9]
            for (count in 1..3) {
                player.clearInv()
                clearPoints()
                for (i in 0..2) {
                    player.setVarBit(MixBits.orderType[i], recipe.nativeType)
                    player.setVarBit(MixBits.orderModifier[i], 2)
                }
                repeat(count) { player.inv[it] = finished(recipe, 2) }
                val xp = player.statMap.getFineXP(stats.herblore)
                player.withProtectedAccess { assertTrue(d.game.submit(this)) }
                for (point in MixVars.points) assertEquals(
                    count * 20 * (10 + 2 * (count - 1)) / 10,
                    player.vars[point],
                )
                assertEquals(xp + 3650 * count, player.statMap.getFineXP(stats.herblore))
                assertEquals(0, player.count(recipe.finished))
            }
        }

    @Test
    fun GameTestState
        .`unrequested and wrong process give reduced highest resin and rounded third XP`() =
        game { d ->
            setup()
            val recipe = MixRecipes.all[9]
            player.inv[0] = finished(recipe, 3)
            player.setVarBit(MixBits.orderType[0], recipe.nativeType)
            player.setVarBit(MixBits.orderModifier[0], 2)
            val xp = player.statMap.getFineXP(stats.herblore)
            player.withProtectedAccess { assertTrue(d.game.submit(this)) }
            assertEquals(0, player.vars[MixVars.points[0]])
            assertEquals(0, player.vars[MixVars.points[1]])
            assertEquals(10, player.vars[MixVars.points[2]])
            assertEquals(xp + 1220, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState
        .`digweed doubles ordered rewards survives processing and cannot be applied twice`() =
        game { d ->
            setup()
            val recipe = MixRecipes.all[0]
            make(d, recipe)
            player.inv[1] = InvObj(MixObjs.digweed)
            player.withProtectedAccess { assertTrue(d.game.enrich(this, 1, 0)) }
            player.inv[1] = InvObj(MixObjs.digweed)
            player.withProtectedAccess { assertFalse(d.game.enrich(this, 1, 0)) }
            assertEquals(MixObjs.digweed.id, player.inv[1]?.id)
            player.opLoc1(machine(0))
            advance(33)
            assertEquals(6, player.inv[0]?.vars?.getBits(MixState.state.bits))
            player.setVarBit(MixBits.orderType[0], 1)
            player.setVarBit(MixBits.orderModifier[0], 2)
            player.setVarBit(MixBits.orderType[1], 0)
            player.setVarBit(MixBits.orderType[2], 0)
            player.withProtectedAccess { assertTrue(d.game.submit(this)) }
            assertEquals(40, player.vars[MixVars.points[0]])
            assertEquals(3800, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState
        .`walking cancels processing without consuming the unfinished potion or granting completion`() =
        game { d ->
            setup()
            val recipe = MixRecipes.all[0]
            make(d, recipe)
            val source = machine(0)
            player.opLoc1(source)
            advance(3)
            player.moveGameClick(player.coords.translateX(-1))
            advance(35)
            assertEquals(1, player.count(recipe.unfinished))
            assertEquals(0, player.count(recipe.finished))
            assertEquals(0, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState
        .`retort repeated clicks give bounded speedups and finish faster than passive processing`() =
        game { d ->
            setup()
            val recipe = MixRecipes.all[0]
            make(d, recipe)
            val source = machine(0)
            player.opLoc1(source)
            advance()
            repeat(8) {
                player.opLoc1(source)
                advance()
            }
            advance(8)
            assertEquals(1, player.count(recipe.finished))
            assertTrue(player.statMap.getFineXP(stats.herblore) in 20..160)
        }

    @Test
    fun GameTestState.`entry rejects boosted below sixty missing native quest and free worlds`() =
        game { d ->
            setup()
            player.stats[stats.herblore] = 59
            player.setCurrentLevel(stats.herblore, 70)
            assertFalse(d.game.gate(player))
            player.stats[stats.herblore] = 60
            player.setVarBit(MixBits.quest, 23)
            assertFalse(d.game.gate(player))
            player.setVarBit(MixBits.quest, 24)
            player.members = false
            assertFalse(d.game.gate(player))
            player.members = true
            assertTrue(d.game.gate(player))
        }

    @Test
    fun GameTestState
        .`native rewards charge all three resin currencies and reject full bag without charging`() =
        game { d ->
            setup()
            for (v in MixVars.points) player.setVarp(v, 100000)
            val index = d.shop.goods.indexOfFirst { it.item == 29993 }
            val good = d.shop.goods[index]
            player.fillInv()
            player.withProtectedAccess { assertEquals(0, d.shop.buy(this, index, 1)) }
            for (v in MixVars.points) assertEquals(100000, player.vars[v])
            player.clearInv()
            player.withProtectedAccess { assertEquals(1, d.shop.buy(this, index, 1)) }
            assertEquals(1, player.count(objs.aldarium))
            for (i in 0..2) assertEquals(100000 - good.cost[i], player.vars[MixVars.points[i]])
            player.withProtectedAccess {
                assertEquals(1, d.shop.buy(this, d.shop.goods.indexOfFirst { it.item == 29999 }, 1))
                assertEquals(0, d.shop.buy(this, d.shop.goods.indexOfFirst { it.item == 29999 }, 1))
            }
            assertEquals(1, player.vars[MixBits.potionStorage])
        }

    @Test
    fun GameTestState
        .`goggles clothing and amulet acquisition preserve native prices and forty percent clothing refunds`() =
        game { d ->
            setup()
            for (v in MixVars.points) player.setVarp(v, 100000)
            for (id in listOf(29974, 29978, 29982, 29986)) {
                val index = d.shop.goods.indexOfFirst { it.item == id }
                val good = d.shop.goods[index]
                val before = MixVars.points.map { player.vars[it] }
                player.withProtectedAccess { assertEquals(1, d.shop.buy(this, index, 1)) }
                val slot = player.inv.indexOfFirst { it?.id == id }
                player.withProtectedAccess { assertEquals(1, d.shop.refund(this, slot, 1)) }
                for (i in 0..2) assertEquals(
                    before[i] - good.cost[i] + good.cost[i] * 4 / 10,
                    player.vars[MixVars.points[i]],
                )
            }
            val amulet =
                d.shop.goods.indexOfFirst {
                    it.item == objs.alchemists_amulet_charged.id ||
                        it.item == objs.alchemists_amulet_uncharged.id
                }
            assertTrue(amulet >= 0)
            player.withProtectedAccess { assertEquals(1, d.shop.buy(this, amulet, 1)) }
        }

    @Test
    fun GameTestState
        .`all potion pack tiers make four noted rolls and reject low level and full bag atomically`() =
        game { d ->
            setup()
            for (i in 0..2) {
                player.clearInv()
                player.inv[0] = InvObj(MixPackObjs.packs[i])
                repeat(4) {
                    random.next = 0
                    random.then = 2
                }
                player.withProtectedAccess { assertTrue(d.packs.open(this, i, 0)) }
                val output = MixPackTables.all[i][0].first
                assertEquals(8, player.count(d.items.getValue(d.items[output].certlink)))
            }
            player.clearInv()
            player.inv[0] = InvObj(MixPackObjs.packs[2])
            player.stats[stats.herblore] = 84
            player.withProtectedAccess { assertFalse(d.packs.open(this, 2, 0)) }
            assertEquals(1, player.count(MixPackObjs.packs[2]))
            player.stats[stats.herblore] = 99
            player.fillInv()
            player.inv[0] = InvObj(MixPackObjs.packs[0])
            for (roll in listOf(0, 11, 22, 32)) {
                random.next = roll
                random.then = 2
            }
            player.withProtectedAccess { assertFalse(d.packs.open(this, 0, 0)) }
            assertEquals(1, player.count(MixPackObjs.packs[0]))
        }

    @Test
    fun GameTestState
        .`pouch stores every dated secondary up to twenty six and rejects noted stacked herb and quest inputs`() =
        game { d ->
            setup()
            player.inv[0] = InvObj(ReagentPouchObjs.closed)
            for (id in d.pouch.allowedIds) {
                player.inv[1] = InvObj(d.items.getValue(id))
                player.withProtectedAccess { assertEquals(1, d.pouch.fill(this, 1)) }
                assertEquals(1, d.pouch.stored(player).count(d.items.getValue(id)))
            }
            for (id in listOf(objs.torstol.id, objs.swamp_tar.id, 1526, objs.garlic.id)) {
                player.inv[1] = InvObj(d.items.getValue(id))
                player.withProtectedAccess { assertEquals(0, d.pouch.fill(this, 1)) }
            }
            val stored = d.pouch.stored(player)
            val item = objs.snape_grass
            stored[stored.indexOfFirst { it?.id == item.id }] = InvObj(item, 26)
            player.inv[1] = InvObj(item)
            player.withProtectedAccess { assertEquals(0, d.pouch.fill(this, 1)) }
            assertEquals(item.id, player.inv[1]?.id)
        }

    @Test
    fun GameTestState
        .`open near bank reagent pouch supplies twenty six standard potions while keeping trigger secondary`() =
        game { d ->
            setup()
            val bank =
                placeMapLoc(
                    CoordGrid(2000, 2000),
                    locTypes.values.first {
                        it.name.contains("Bank chest", true) && it.op.any { it.equals("Use", true) }
                    },
                )
            player.placeAt(bank.coords.translateX(-1))
            player.inv[0] = InvObj(ReagentPouchObjs.open)
            player.inv[1] = InvObj(objs.snape_grass)
            d.pouch.stored(player)[0] = InvObj(objs.snape_grass, 26)
            for (i in 2..27) player.inv[i] = InvObj(objs.ranarr_potion_unf)
            val recipe = FinishedPotionRecipes.byOutput.getValue(objs.prayer_potion_3.id)
            player.withProtectedAccess { d.finished.start(this, recipe, 26) }
            advance(80)
            assertEquals(26, player.count(objs.prayer_potion_3))
            assertEquals(1, player.count(objs.snape_grass))
            assertEquals(0, d.pouch.stored(player).count(d.items[objs.snape_grass]))
            assertEquals(22750, player.statMap.getFineXP(stats.herblore))
        }

    @Test
    fun GameTestState
        .`native supervisor Rewards and omnishop buy acquire aldarium using earned resin`() =
        game { d ->
            setup()
            player.setVarp(MixVars.points[0], 80)
            player.setVarp(MixVars.points[1], 60)
            player.setVarp(MixVars.points[2], 90)
            val npc = spawnNpc(CoordGrid(1394, 9313), npcTypes[MixNpcs.supervisors[1]])
            player.placeAt(CoordGrid(1395, 9313))
            player.opNpc3(npc)
            advance(3)
            assertTrue(cacheTypes.interfaces.getValue(819) in player.ui)
            val index = d.shop.goods.indexOfFirst { it.item == objs.aldarium.id }
            val button =
                cacheTypes.components.values.single { it.internalName == "omnishop_main:list" }
            player.ifButton(button, index * 12, org.rsmod.game.type.interf.IfButtonOp.Op2)
            advance()
            assertEquals(1, player.count(objs.aldarium))
            for (point in MixVars.points) assertEquals(0, player.vars[point])
        }

    @Test
    fun GameTestState
        .`digweed blooms only after active orders expires and withers when leaving the activity`() =
        game { d ->
            setup()
            player.setVarBit(MixBits.sinceHerb, 31)
            player.inv[0] = finished(MixRecipes.all[0], 2)
            random.next = 0
            player.withProtectedAccess { assertTrue(d.game.submit(this)) }
            assertEquals(1, MixBits.herbs.count { player.vars[it] != 0 })
            advance(35)
            assertTrue(MixBits.herbs.all { player.vars[it] == 0 })
            player.setVarBit(MixBits.herbs[0], 1)
            player.withProtectedAccess { assertTrue(d.game.pickDigweed(this, 0)) }
            assertEquals(1, player.count(MixObjs.digweed))
            player.placeAt(CoordGrid(1399, 9312))
            advance(3)
            assertEquals(0, player.count(MixObjs.digweed))
            assertFalse(MixUi.overlay in player.ui)
        }

    @Test
    fun GameTestState
        .`open reagent pickup stores only the acquired amount and honours the twenty six cap`() =
        game { d ->
            setup()
            player.inv[0] = InvObj(ReagentPouchObjs.open)
            player.inv[1] = InvObj(objs.snape_grass)
            player.inv[2] = InvObj(objs.snape_grass)
            player.inv[3] = InvObj(objs.snape_grass)
            assertEquals(2, d.pouch.storeAcquired(player, objs.snape_grass, 2))
            assertEquals(1, player.count(objs.snape_grass))
            assertEquals(2, d.pouch.stored(player).count(d.items[objs.snape_grass]))
            d.pouch.stored(player)[0] = InvObj(objs.snape_grass, 26)
            assertEquals(0, d.pouch.storeAcquired(player, objs.snape_grass, 1))
            assertEquals(1, player.count(objs.snape_grass))
        }

    @Test
    fun GameTestState.`closed low level and distant reagent pouch cannot supply a batch`() =
        game { d ->
            setup()
            player.inv[0] = InvObj(ReagentPouchObjs.closed)
            d.pouch.stored(player)[0] = InvObj(objs.snape_grass, 26)
            player.withProtectedAccess { assertEquals(0, d.pouch.quantity(this, objs.snape_grass)) }
            player.inv[0] = InvObj(ReagentPouchObjs.open)
            player.stats[stats.herblore] = 80
            player.withProtectedAccess { assertEquals(0, d.pouch.quantity(this, objs.snape_grass)) }
            player.stats[stats.herblore] = 81
            player.placeAt(CoordGrid(2200, 2200))
            player.withProtectedAccess { assertEquals(0, d.pouch.quantity(this, objs.snape_grass)) }
        }

    private fun GameTestScope.setup() {
        player.members = true
        player.stats[stats.herblore] = 99
        player.statMap.setFineXP(stats.herblore, 0)
        player.setVarBit(MixBits.quest, 24)
        player.setVarp(varps.druidic_ritual, 4)
        player.placeAt(CoordGrid(1394, 9324))
        fillHopper()
        for (i in 0..2) {
            player.setVarBit(MixBits.orderType[i], 1)
            player.setVarBit(MixBits.orderModifier[i], 2)
        }
        clearPoints()
    }

    private fun GameTestScope.clearPoints() {
        for (v in MixVars.points) player.setVarp(v, 0)
    }

    private fun GameTestScope.fillHopper() {
        for (v in MixBits.available) player.setVarBit(v, 3000)
    }

    private fun GameTestScope.make(d: Deps, r: MixRecipe) {
        player.withProtectedAccess {
            for (i in 0..2) repeat(r.mix[i]) { d.game.pull(this, i) }
            assertTrue(d.game.takeBase(this))
        }
    }

    private fun GameTestScope.machine(i: Int): org.rsmod.game.loc.BoundLocInfo {
        val source =
            placeMapLoc(
                listOf(CoordGrid(1397, 9325), CoordGrid(1393, 9329), CoordGrid(1391, 9325))[i],
                MixLocs.machines[i],
                angle = listOf(LocAngle.North, LocAngle.West, LocAngle.South)[i],
            )
        player.placeAt(source.coords.translateX(-1))
        return source
    }

    private fun finished(recipe: MixRecipe, modifier: Int) =
        InvObj(recipe.finished, vars = 0.withBits(MixState.state.bits, modifier))

    private fun GameTestState.game(body: GameTestScope.(Deps) -> Unit) =
        runInjectedGameTest(
            Deps::class,
            Module,
            MasteringMixology::class,
            MixologyRewards::class,
            MixologyPotionPacks::class,
            ReagentPouch::class,
            HerbloreScript::class,
            org.rsmod.api.shops.OmniShopButtons::class,
            testBody = body,
        )

    private object Module : AbstractModule() {
        override fun configure() {
            install(HerbloreModule())
        }
    }

    data class Deps
    @Inject
    constructor(
        val game: MasteringMixology,
        val shop: MixologyRewards,
        val packs: MixologyPotionPacks,
        val pouch: ReagentPouch,
        val items: ObjTypeList,
        val finished: FinishedPotionMixing,
        val locU: LocUInteractions,
        val held: HeldInteractions,
    )
}
