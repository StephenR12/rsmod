package org.rsmod.content.skills.cooking.gnome

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerGnomeCookingSuccess
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.*
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Native mould and post-2006 intermediate objects; recipes are dated in the production ledger. */
public class GnomeCooking @Inject constructor(private val objects: ObjTypeList) : PluginScript() {
    private val buttons =
        listOf(
            components.skillmulti_a,
            components.skillmulti_b,
            components.skillmulti_c,
            components.skillmulti_d,
            components.skillmulti_e,
            components.skillmulti_f,
            components.skillmulti_g,
        )

    override fun ScriptContext.startup() {
        for (mould in GnomeMould.entries) {
            onOpHeldU(g("gianne_dough"), mould.container) { form(this, it.firstSlot, mould) }
            onOpHeld1(mould.half) { selectDish(this, it.slot, mould) }
            onOpHeld1(mould.burnt) { emptyMould(this, it.slot, mould) }
        }
        for (dish in GnomeRecipes.dishes.filterNot { it.finishesBaked }) {
            val items =
                dish.garnish.map { it.obj } +
                    if (dish.garnishSpice) listOf(g("gnome_spice")) else emptyList()
            for (item in items) onOpHeldU(dish.baked, item) { finish(this, it.firstSlot, dish) }
        }
        onOpHeld1(g("cocktail_shaker")) { selectCocktail(this, it.slot) }
        for (drink in GnomeRecipes.cocktails) onOpHeld1(drink.mixed) { pour(this, it.slot, drink) }
        onOpHeld1(g("aluft_shaker_drunk_dragon_prefinish")) { garnishDragon(this, it.slot) }
        onOpHeld1(g("aluft_shaker_choc_saturday_heated")) { finishSaturday(this, it.slot) }
        for (item in listOf(g("pineapple_chunks"), g("pot_of_cream"))) onOpHeldU(
            g("aluft_shaker_drunk_dragon_prefinish"),
            item,
        ) {
            garnishDragon(this, it.firstSlot)
        }
        for (item in listOf(g("chocolate_dust"), g("pot_of_cream"))) onOpHeldU(
            g("aluft_shaker_choc_saturday_heated"),
            item,
        ) {
            finishSaturday(this, it.firstSlot)
        }
        for (name in listOf("lemon", "orange", "lime", "pineapple")) onOpHeldU(
            g(name),
            g("knife"),
        ) {
            cutFruit(this, it.firstSlot, name)
        }
        onOpHeld1(g("swamp_toad")) { transform(this, it.slot, g("swamp_toad"), g("toads_legs")) }
        onOpHeldU(g("village_spider_carcass"), g("tbw_skewer_stick")) {
            transform(
                this,
                it.firstSlot,
                g("village_spider_carcass"),
                g("tbw_spider_on_stick_raw"),
                ingredients = listOf(GnomeMaterial(g("tbw_skewer_stick"))),
            )
        }
        onOpHeldU(g("village_spider_carcass"), g("arrow_shaft")) {
            transform(
                this,
                it.firstSlot,
                g("village_spider_carcass"),
                g("tbw_spider_on_shaft_raw"),
                ingredients = listOf(GnomeMaterial(g("arrow_shaft"))),
            )
        }
    }

    public fun form(access: ProtectedAccess, slot: Int, mould: GnomeMould) {
        transform(
            access,
            slot,
            g("gianne_dough"),
            mould.raw,
            ingredients = listOf(GnomeMaterial(mould.container)),
        )
    }

    public fun emptyMould(access: ProtectedAccess, slot: Int, mould: GnomeMould) {
        transform(access, slot, mould.burnt, mould.container)
    }

    public suspend fun selectDish(access: ProtectedAccess, slot: Int, mould: GnomeMould): Unit =
        with(access) {
            if (!player.members || inv[slot]?.id != mould.half.id) return
            val options = GnomeRecipes.dishes.filter { it.mould == mould }
            val selection =
                skillMulti(
                    title = "What would you like to prepare?",
                    choices = options.map { it.result },
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = 0,
                )
            val index = buttons.indexOfFirst { it.isType(selection.component) }
            options.getOrNull(index)?.let { prepare(this, slot, it) }
        }

    public fun prepare(access: ProtectedAccess, slot: Int, dish: GnomeDish) {
        transform(
            access,
            slot,
            dish.mould.half,
            dish.prepared,
            dish.ingredients,
            if (dish.spice) listOf(g("gnome_spice")) else emptyList(),
            dish.level,
            dish.prepXp,
        )
    }

    public fun finish(access: ProtectedAccess, slot: Int, dish: GnomeDish) {
        if (dish.finishesBaked) return
        transform(
            access,
            slot,
            dish.baked,
            dish.result,
            dish.garnish,
            if (dish.garnishSpice) listOf(g("gnome_spice")) else emptyList(),
            xp = dish.finishXp,
            completed = true,
        )
    }

    public suspend fun selectCocktail(access: ProtectedAccess, slot: Int): Unit =
        with(access) {
            if (!player.members || inv[slot]?.id != g("cocktail_shaker").id) return
            val selection =
                skillMulti(
                    title = "What would you like to mix?",
                    choices = GnomeRecipes.cocktails.map { it.result },
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = 0,
                )
            val index = buttons.indexOfFirst { it.isType(selection.component) }
            GnomeRecipes.cocktails.getOrNull(index)?.let { mix(this, slot, it) }
        }

    public fun mix(access: ProtectedAccess, slot: Int, drink: GnomeCocktail) {
        transform(
            access,
            slot,
            g("cocktail_shaker"),
            drink.mixed,
            drink.ingredients,
            level = drink.level,
            xp = 30.0,
        )
    }

    public fun pour(access: ProtectedAccess, slot: Int, drink: GnomeCocktail) {
        val output =
            when (drink.result) {
                g("drunk_dragon") -> g("aluft_shaker_drunk_dragon_prefinish")
                g("chocolate_saturday") -> g("aluft_shaker_choc_saturday_preheat")
                else -> drink.result
            }
        transform(
            access,
            slot,
            drink.mixed,
            output,
            drink.garnish + GnomeMaterial(g("cocktail_glass_empty")),
            level = drink.level,
            xp = drink.finishXp,
            returned = listOf(GnomeMaterial(g("cocktail_shaker"))),
            completed = output == drink.result,
        )
    }

    public fun garnishDragon(access: ProtectedAccess, slot: Int) {
        transform(
            access,
            slot,
            g("aluft_shaker_drunk_dragon_prefinish"),
            g("aluft_shaker_drunk_dragon_preheat"),
            listOf(GnomeMaterial(g("pineapple_chunks")), GnomeMaterial(g("pot_of_cream"))),
            level = 32,
        )
    }

    public fun finishSaturday(access: ProtectedAccess, slot: Int) {
        transform(
            access,
            slot,
            g("aluft_shaker_choc_saturday_heated"),
            g("chocolate_saturday"),
            listOf(GnomeMaterial(g("chocolate_dust")), GnomeMaterial(g("pot_of_cream"))),
            level = 33,
            xp = 140.0,
            completed = true,
        )
    }

    public suspend fun cutFruit(access: ProtectedAccess, slot: Int, name: String): Unit =
        with(access) {
            if (!player.members || inv[slot]?.id != g(name).id || g("knife") !in inv) return
            val choices =
                listOf(
                    g(name + "_chunks"),
                    g(if (name == "pineapple") "pineapple_ring" else name + "_slices"),
                )
            val selection =
                skillMulti(
                    title = "How would you like to cut the fruit?",
                    choices = choices,
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = 0,
                )
            val index = buttons.indexOfFirst { it.isType(selection.component) }
            val output = choices.getOrNull(index) ?: return
            transform(
                this,
                slot,
                g(name),
                output,
                tools = listOf(g("knife")),
                outputCount = if (output == g("pineapple_ring")) 4 else 1,
            )
        }

    private fun transform(
        access: ProtectedAccess,
        slot: Int,
        input: ObjType,
        output: ObjType,
        ingredients: List<GnomeMaterial> = emptyList(),
        tools: List<ObjType> = emptyList(),
        level: Int = 1,
        xp: Double = 0.0,
        returned: List<GnomeMaterial> = emptyList(),
        completed: Boolean = false,
        outputCount: Int = 1,
    ): Boolean =
        with(access) {
            if (!player.members || slot !in inv.indices || inv[slot]?.id != input.id) return false
            if (player.stat(stats.cooking) < level) {
                mes("You need a Cooking level of $level to make this.")
                return false
            }
            val missingTool = tools.firstOrNull { it !in inv }
            if (missingTool != null) {
                mes("You need ${objects[missingTool].name.lowercase()} to make this.")
                return false
            }
            val missing = ingredients.firstOrNull { inv.count(objects[it.obj]) < it.count }
            if (missing != null) {
                mes(
                    "You need ${missing.count} ${objects[missing.obj].name.lowercase()} to make this."
                )
                return false
            }
            val containers =
                ingredients
                    .filter { it.obj == g("pot_of_cream") || it.obj == g("bucket_milk") }
                    .map {
                        GnomeMaterial(
                            g(if (it.obj == g("pot_of_cream")) "pot_empty" else "bucket_empty"),
                            it.count,
                        )
                    }
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, input.id, 1, slot)
                    for (item in ingredients) delete(bag, item.obj.id, item.count)
                    add(bag, output.id, outputCount)
                    for (item in returned + containers) add(bag, item.obj.id, item.count)
                }
            if (!tx.success) {
                mes("You don't have enough inventory space.")
                return false
            }
            if (xp > 0.0) statAdvance(stats.cooking, xp)
            if (completed) {
                publish(PlayerGnomeCookingSuccess(player, output))
                val c = player.coords
                if (
                    output == g("chocolate_bomb") &&
                        c.level in 1..3 &&
                        c.x in 2440..2505 &&
                        c.z in 3480..3520
                )
                    player.completeDiaryTask(DiaryTask.MakeGrandTreeChocolateBomb)
            }
            return true
        }
}
