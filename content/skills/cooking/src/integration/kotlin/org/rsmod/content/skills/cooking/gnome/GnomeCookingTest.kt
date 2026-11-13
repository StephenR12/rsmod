package org.rsmod.content.skills.cooking.gnome

import com.google.inject.AbstractModule
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.PlayerGnomeCookingSuccess
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.interact.HeldUInteractions
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.cooking.*
import org.rsmod.events.EventBus
import org.rsmod.game.interact.HeldOp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.ui.UserInterface
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class GnomeCookingTest {
    @Test
    fun GameTestState
        .`all thirteen source dishes finish from dough with exact total XP and reusable moulds`() =
        test { d ->
            val totals = listOf(100, 102, 104, 106, 150, 152, 154, 156, 158, 170, 175, 185, 190)
            for ((index, dish) in GnomeRecipes.dishes.withIndex()) {
                prepare()
                val range = source()
                val completed = mutableListOf<Int>()
                d.events.subscribeUnbound(PlayerGnomeCookingSuccess::class.java) {
                    completed += output.id
                }
                add(g("gianne_dough"))
                add(dish.mould.container)
                player.withProtectedAccess {
                    d.actions.form(this, slot(g("gianne_dough")), dish.mould)
                }
                bake(d, range, dish.mould.raw)
                assertEquals(1, player.count(dish.mould.half))
                assertEquals(0, player.count(dish.mould.container))
                addMaterials(dish.ingredients)
                if (dish.spice) add(g("gnome_spice"))
                player.withProtectedAccess { d.actions.prepare(this, slot(dish.mould.half), dish) }
                bake(d, range, dish.prepared)
                assertEquals(1, player.count(dish.mould.container), dish.name)
                if (!dish.finishesBaked) {
                    addMaterials(dish.garnish)
                    if (dish.garnishSpice && player.count(g("gnome_spice")) == 0)
                        add(g("gnome_spice"))
                    player.withProtectedAccess { d.actions.finish(this, slot(dish.baked), dish) }
                }
                assertEquals(1, player.count(dish.result), dish.name)
                assertEquals(totals[index] * 10, player.statMap.getFineXP(stats.cooking), dish.name)
                assertEquals(listOf(dish.result.id), completed, dish.name)
                if (dish.spice || dish.garnishSpice)
                    assertEquals(1, player.count(g("gnome_spice")), dish.name)
            }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MakeGrandTreeChocolateBomb))
        }

    @Test
    fun GameTestState.`all seven cocktails include their native pour garnish and heating stages`() =
        test { d ->
            val totals = listOf(50, 70, 110, 120, 160, 170, 180)
            for ((index, drink) in GnomeRecipes.cocktails.withIndex()) {
                prepare()
                val range = source()
                val completed = mutableListOf<Int>()
                d.events.subscribeUnbound(PlayerGnomeCookingSuccess::class.java) {
                    completed += output.id
                }
                add(g("cocktail_shaker"))
                addMaterials(drink.ingredients)
                player.withProtectedAccess {
                    d.actions.mix(this, slot(g("cocktail_shaker")), drink)
                }
                assertEquals(1, player.count(drink.mixed))
                assertTrue(completed.isEmpty())
                add(g("cocktail_glass_empty"))
                addMaterials(drink.garnish)
                player.withProtectedAccess { d.actions.pour(this, slot(drink.mixed), drink) }
                assertEquals(1, player.count(g("cocktail_shaker")))
                when (drink.result) {
                    g("drunk_dragon") -> {
                        add(g("pineapple_chunks"))
                        add(g("pot_of_cream"))
                        player.withProtectedAccess {
                            d.actions.garnishDragon(
                                this,
                                slot(g("aluft_shaker_drunk_dragon_prefinish")),
                            )
                        }
                        bake(d, range, g("aluft_shaker_drunk_dragon_preheat"))
                    }
                    g("chocolate_saturday") -> {
                        bake(d, range, g("aluft_shaker_choc_saturday_preheat"))
                        add(g("chocolate_dust"))
                        add(g("pot_of_cream"))
                        player.withProtectedAccess {
                            d.actions.finishSaturday(
                                this,
                                slot(g("aluft_shaker_choc_saturday_heated")),
                            )
                        }
                    }
                    else -> Unit
                }
                assertEquals(1, player.count(drink.result), drink.name)
                assertEquals(
                    totals[index] * 10,
                    player.statMap.getFineXP(stats.cooking),
                    drink.name,
                )
                assertEquals(listOf(drink.result.id), completed, drink.name)
            }
        }

    @Test
    fun GameTestState.`native Prepare supports fifth batta and Mix supports seventh cocktail`() =
        test { d ->
            prepare()
            val cheese = GnomeRecipes.dishes.single { it.result == g("cheese+tom_batta") }
            add(cheese.mould.half)
            addMaterials(cheese.ingredients)
            player.withProtectedAccess {
                d.held.interact(this, inv, slot(cheese.mould.half), HeldOp.Op1)
            }
            resume(components.skillmulti_e)
            assertEquals(1, player.count(cheese.prepared))
            assertEquals(400, player.statMap.getFineXP(stats.cooking))
            prepare()
            val special = GnomeRecipes.cocktails.last()
            add(g("cocktail_shaker"))
            addMaterials(special.ingredients)
            player.withProtectedAccess {
                d.held.interact(this, inv, slot(g("cocktail_shaker")), HeldOp.Op1)
            }
            resume(components.skillmulti_g)
            assertEquals(1, player.count(special.mixed))
            assertEquals(300, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState
        .`raw and half made burns recover exactly one mould without a finished event`() =
        test { d ->
            prepare(1)
            val range = source()
            val mould = GnomeMould.Bowl
            add(g("gianne_dough"))
            add(mould.container)
            player.withProtectedAccess { d.actions.form(this, slot(g("gianne_dough")), mould) }
            random.next = 99
            player.withProtectedAccess {
                d.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(mould.raw.id),
                    1,
                )
            }
            advance(4)
            assertEquals(1, player.count(mould.burnt))
            assertEquals(0, player.count(mould.container))
            player.withProtectedAccess { d.held.interact(this, inv, slot(mould.burnt), HeldOp.Op1) }
            assertEquals(1, player.count(mould.container))
            prepare(10)
            val dish = GnomeRecipes.dishes.first()
            add(dish.prepared)
            random.next = 99
            player.withProtectedAccess {
                d.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(dish.prepared.id),
                    1,
                )
            }
            advance(4)
            assertEquals(1, player.count(dish.mould.burnt))
            assertEquals(0, player.count(dish.mould.container))
            player.withProtectedAccess {
                d.actions.emptyMould(this, slot(dish.mould.burnt), dish.mould)
            }
            assertEquals(1, player.count(dish.mould.container))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState.`range only baking and full inventory preserve unfinished food and XP`() =
        test { d ->
            prepare()
            val range = source()
            val dish = GnomeRecipes.dishes.first()
            add(dish.prepared)
            player.withProtectedAccess {
                d.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Fire,
                    StandardCookingRecipes.byRaw.getValue(dish.prepared.id),
                    1,
                )
            }
            advance(4)
            assertEquals(1, player.count(dish.prepared))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
            while (!player.inv.isFull()) add(objs.knife)
            player.withProtectedAccess {
                d.cooking.start(
                    this,
                    range,
                    CookingSourceKind.Range,
                    StandardCookingRecipes.byRaw.getValue(dish.prepared.id),
                    1,
                )
            }
            advance(4)
            assertEquals(1, player.count(dish.prepared))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
        }

    @Test
    fun GameTestState
        .`missing level or finishing ingredients cannot consume food or grant progress`() =
        test { d ->
            prepare(41)
            val dish = GnomeRecipes.dishes.last()
            add(dish.mould.half)
            addMaterials(dish.ingredients)
            player.withProtectedAccess { d.actions.prepare(this, slot(dish.mould.half), dish) }
            assertEquals(1, player.count(dish.mould.half))
            assertEquals(4, player.count(g("chocolate_bar")))
            prepare()
            add(dish.baked)
            add(g("chocolate_dust"))
            add(g("pot_of_cream"))
            player.withProtectedAccess { d.actions.finish(this, slot(dish.baked), dish) }
            assertEquals(1, player.count(dish.baked))
            assertEquals(0, player.count(dish.result))
            assertEquals(0, player.statMap.getFineXP(stats.cooking))
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.MakeGrandTreeChocolateBomb))
        }

    @Test
    fun GameTestState.`chocolate diary needs actual final delivery inside the Grand Tree`() =
        test { d ->
            prepare()
            val dish = GnomeRecipes.dishes.last()
            player.telejump(CoordGrid(3200, 3200))
            add(dish.baked)
            addMaterials(dish.garnish)
            player.withProtectedAccess { d.actions.finish(this, slot(dish.baked), dish) }
            assertFalse(player.hasCompletedDiaryTask(DiaryTask.MakeGrandTreeChocolateBomb))
            player.telejump(CoordGrid(2449, 3508, 1))
            add(dish.baked)
            addMaterials(dish.garnish)
            player.withProtectedAccess { d.actions.finish(this, slot(dish.baked), dish) }
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.MakeGrandTreeChocolateBomb))
        }

    @Test
    fun GameTestState
        .`native carcass on shaft then ordinary cooking credits spider diary outside Karamja`() =
        test { d ->
            prepare()
            player.telejump(CoordGrid(3200, 3200))
            val range = placeMapLoc(CoordGrid(3199, 3200), cooking_locs.range)
            add(g("village_spider_carcass"))
            add(g("arrow_shaft"))
            use(d, 0, 1)
            assertEquals(1, player.count(g("tbw_spider_on_shaft_raw")))
            bake(d, range, g("tbw_spider_on_shaft_raw"))
            assertEquals(1, player.count(g("tbw_spider_on_shaft_cooked")))
            assertEquals(800, player.statMap.getFineXP(stats.cooking))
            assertTrue(player.hasCompletedDiaryTask(DiaryTask.CookSpiderOnStick))
        }

    @Test
    fun GameTestState.`native fruit knife menu gives four rings atomically`() = test { d ->
        prepare()
        add(g("pineapple"))
        add(objs.knife)
        while (!player.inv.isFull()) add(objs.coins)
        use(d, 0, 1)
        resume(components.skillmulti_b)
        assertEquals(1, player.count(g("pineapple")))
        assertEquals(0, player.count(g("pineapple_ring")))
        player.inv[25] = null
        player.inv[26] = null
        player.inv[27] = null
        use(d, 0, 1)
        resume(components.skillmulti_b)
        assertEquals(4, player.count(g("pineapple_ring")))
    }

    @Test
    fun GameTestState
        .`gnome and premade foods combo after ordinary food with additive two tick attack delay`() =
        test { d ->
            prepare()
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 1)
            player.actionDelay = player.currentMapClock + 10
            add(objs.shark)
            add(g("chocolate_bomb"), 2)
            player.withProtectedAccess {
                d.ordinary.eat(this, slot(objs.shark), objs.shark, 20)
                d.eating.eat(this, slot(g("chocolate_bomb")), g("chocolate_bomb"), 15)
            }
            assertEquals(36, player.stat(stats.hitpoints))
            assertEquals(player.currentMapClock + 15, player.actionDelay)
            assertEquals(1, player.count(g("chocolate_bomb")))
            player.withProtectedAccess {
                d.eating.eat(this, slot(g("chocolate_bomb")), g("chocolate_bomb"), 15)
            }
            assertEquals(1, player.count(g("chocolate_bomb")))
            advance(3)
            add(g("premade_chocolate_bomb"))
            player.withProtectedAccess {
                d.held.interact(this, inv, slot(g("premade_chocolate_bomb")), HeldOp.Op1)
            }
            assertEquals(51, player.stat(stats.hitpoints))
        }

    @Test
    fun GameTestState.`cooked karambwan participates in the same independent combo timer`() =
        test { d ->
            prepare()
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 1)
            add(objs.shark)
            add(objs.cooked_karambwan)
            player.withProtectedAccess {
                d.ordinary.eat(this, slot(objs.shark), objs.shark, 20)
                d.held.interact(this, inv, slot(objs.cooked_karambwan), HeldOp.Op1)
            }
            assertEquals(39, player.stat(stats.hitpoints))
            assertEquals(0, player.count(objs.cooked_karambwan))
        }

    @Test
    fun GameTestState
        .`cocktail heals boosts Strength and drains from current Attack while returning its glass`() =
        test { d ->
            prepare()
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 1)
            player.setBaseLevel(stats.strength, 50)
            player.setCurrentLevel(stats.strength, 50)
            player.setBaseLevel(stats.attack, 99)
            player.setCurrentLevel(stats.attack, 118)
            val drink = GnomeRecipes.cocktails.last()
            add(drink.result)
            player.withProtectedAccess { d.eating.drink(this, slot(drink.result), drink) }
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(54, player.stat(stats.strength))
            assertEquals(113, player.stat(stats.attack))
            assertEquals(1, player.count(g("cocktail_glass_empty")))
        }

    @Test
    fun GameTestState
        .`spider eating rolls seven to ten and preserves skewer but destroys arrow shaft`() =
        test { d ->
            prepare()
            player.setBaseLevel(stats.hitpoints, 99)
            player.setCurrentLevel(stats.hitpoints, 1)
            val stick = g("tbw_spider_on_stick_cooked")
            add(stick)
            random.next = 7
            player.withProtectedAccess { d.held.interact(this, inv, slot(stick), HeldOp.Op1) }
            assertEquals(8, player.stat(stats.hitpoints))
            assertEquals(1, player.count(g("tbw_skewer_stick")))
            advance(3)
            val shaft = g("tbw_spider_on_shaft_cooked")
            add(shaft)
            random.next = 10
            player.withProtectedAccess { d.held.interact(this, inv, slot(shaft), HeldOp.Op1) }
            assertEquals(18, player.stat(stats.hitpoints))
            assertEquals(0, player.count(g("arrow_shaft")))
        }

    @Test
    fun GameTestState.`ingredient and premade stores carry complete dated stock`() = test { d ->
        val hudo = d.invs[GnomeFoodInvs.hudo].stock!!.filterNotNull()
        val funch = d.invs[GnomeFoodInvs.funch].stock!!.filterNotNull()
        val waiter = d.invs[GnomeFoodInvs.waiter].stock!!.filterNotNull()
        assertEquals(26, hudo.size)
        assertEquals(17, funch.size)
        assertEquals(13, waiter.size)
        assertEquals(50, hudo.single { it.obj == g("gianne_dough").id }.count)
        assertEquals(1, funch.single { it.obj == g("vodka").id }.restockCycles)
        assertTrue(waiter.all { it.count == 3 && it.restockCycles == 100 })
    }

    @Test
    fun GameTestState
        .`choc saturday heating accepts thirty two after mixing and still needs thirty three to finish`() =
        test { d ->
            prepare(33)
            val drink = GnomeRecipes.cocktails.single { it.result == g("chocolate_saturday") }
            add(g("cocktail_shaker"))
            addMaterials(drink.ingredients)
            player.withProtectedAccess { d.actions.mix(this, slot(g("cocktail_shaker")), drink) }
            add(g("cocktail_glass_empty"))
            player.withProtectedAccess { d.actions.pour(this, slot(drink.mixed), drink) }
            player.setCurrentLevel(stats.cooking, 32)
            bake(d, source(), g("aluft_shaker_choc_saturday_preheat"))
            assertEquals(1, player.count(g("aluft_shaker_choc_saturday_heated")))
            add(g("chocolate_dust"))
            add(g("pot_of_cream"))
            player.withProtectedAccess {
                d.actions.finishSaturday(this, slot(g("aluft_shaker_choc_saturday_heated")))
            }
            assertEquals(0, player.count(drink.result))
            player.setCurrentLevel(stats.cooking, 33)
            player.withProtectedAccess {
                d.actions.finishSaturday(this, slot(g("aluft_shaker_choc_saturday_heated")))
            }
            assertEquals(1, player.count(drink.result))
            assertEquals(1700, player.statMap.getFineXP(stats.cooking))
        }

    private fun GameTestScope.prepare(level: Int = 99) {
        player.members = true
        player.observerUUID = 1L
        player.withProtectedAccess { invClear(inv) }
        player.stats[stats.cooking] = level
        player.statMap.setFineXP(stats.cooking, 0)
        player.telejump(CoordGrid(2449, 3508, 1))
    }

    private fun GameTestScope.source(): BoundLocInfo =
        placeMapLoc(CoordGrid(2448, 3508, 1), cooking_locs.range)

    private fun GameTestScope.add(obj: ObjType, count: Int = 1) {
        repeat(count) { player.inv[player.inv.indexOfFirst { it == null }] = InvObj(obj) }
    }

    private fun GameTestScope.addMaterials(items: List<GnomeMaterial>) {
        for (item in items) add(item.obj, item.count)
    }

    private fun GameTestScope.slot(obj: ObjType): Int = player.inv.indexOfFirst { it?.id == obj.id }

    private fun GameTestScope.bake(d: Dependencies, range: BoundLocInfo, raw: ObjType) {
        random.next = 0
        player.withProtectedAccess {
            d.cooking.start(
                this,
                range,
                CookingSourceKind.Range,
                StandardCookingRecipes.byRaw.getValue(raw.id),
                1,
            )
        }
        advance(4)
    }

    private fun GameTestScope.use(d: Dependencies, first: Int, second: Int) {
        player.withProtectedAccess {
            d.heldU.interact(
                this,
                inv,
                d.objects[inv[first]!!],
                first,
                d.objects[inv[second]!!],
                second,
            )
        }
    }

    private fun GameTestScope.resume(component: ComponentType) {
        val target = player.ui.modals.getComponent(UserInterface(interfaces.skillmulti.id))!!
        player.ui.queueClose(target)
        player.resumeActiveCoroutine(ResumePauseButtonInput(component, subcomponent = 1))
        advance()
    }

    private fun GameTestState.test(body: GameTestScope.(Dependencies) -> Unit) =
        runInjectedGameTest(
            Dependencies::class,
            Module,
            CookingScript::class,
            GnomeCooking::class,
            GnomeIngredientShops::class,
            GnomeFoodEating::class,
        ) { d ->
            for (c in listOf(CoordGrid(2449, 3508, 1), CoordGrid(3200, 3200))) for (x in
                -8..8 step 8) for (z in -8..8 step 8) allocZoneCollision(c.translate(x, z))
            body(d)
        }

    private object Module : AbstractModule() {
        override fun configure() {
            install(CookingModule())
        }
    }

    data class Dependencies
    @Inject
    constructor(
        val actions: GnomeCooking,
        val eating: GnomeFoodEating,
        val ordinary: OrdinaryFoodEating,
        val cooking: StandardCooking,
        val held: HeldInteractions,
        val heldU: HeldUInteractions,
        val objects: ObjTypeList,
        val invs: InvTypeList,
        val events: EventBus,
    )
}
