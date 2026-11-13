package org.rsmod.content.skills.cooking.gnome

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.map.obj.MapObjSpawnBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object GnomeFoodNpcs : NpcReferences() {
    val hudo = find("gnome_hudo")
    val funch = find("gnome_heckelfunch")
    val waiter = find("gnome_waiter")
}

public object GnomeFoodInvs : InvReferences() {
    val hudo = find("gnomeshop_hudo")
    val funch = find("gnomeshop_heck")
    val waiter = find("giannerestaurant")
}

internal object GnomeFoodStock : InvEditor() {
    init {
        edit(GnomeFoodInvs.waiter) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (name in
                listOf(
                    "premade_toad_crunchies",
                    "premade_spicy_crunchies",
                    "premade_worm_crunchies",
                    "premade_chocchip_crunchies",
                    "premade_fruit_batta",
                    "premade_toad_batta",
                    "premade_worm_batta",
                    "premade_vegetable_batta",
                    "premade_cheese+tom_batta",
                    "premade_worm_hole",
                    "premade_veg_ball",
                    "premade_tangled_toads_legs",
                    "premade_chocolate_bomb",
                )) stock += stock(g(name), 3, 100)
        }
        edit(GnomeFoodInvs.hudo) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for ((name, count, ticks) in
                listOf(
                    Triple("gianne_dough", 50, 100),
                    Triple("equa_leaves", 20, 100),
                    Triple("pot_flour", 10, 200),
                    Triple("gnome_spice", 10, 100),
                    Triple("onion", 10, 100),
                    Triple("potato", 10, 100),
                    Triple("cabbage", 10, 100),
                    Triple("tomato", 10, 100),
                    Triple("cheese", 10, 100),
                    Triple("tbw_spider_on_stick_cooked", 0, 100),
                    Triple("tbw_spider_on_shaft_cooked", 0, 100),
                    Triple("lime", 10, 100),
                    Triple("orange", 10, 100),
                    Triple("lemon", 10, 100),
                    Triple("pineapple", 10, 100),
                    Triple("dwellberries", 10, 100),
                    Triple("cocktail_shaker", 10, 200),
                    Triple("chocolate_bar", 20, 100),
                    Triple("chocolate_dust", 10, 100),
                    Triple("pot_of_cream", 5, 100),
                    Triple("bucket_milk", 5, 100),
                    Triple("knife", 5, 200),
                    Triple("giannes_cook_book", 5, 100),
                    Triple("batta_tin", 5, 200),
                    Triple("crunchy_tray", 5, 200),
                    Triple("gnomebowl_mould", 5, 200),
                )) stock += stock(g(name), count, ticks)
        }
        edit(GnomeFoodInvs.funch) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for ((name, count, ticks) in
                listOf(
                    Triple("brandy", 10, 100),
                    Triple("gin", 10, 100),
                    Triple("vodka", 10, 1),
                    Triple("whisky", 10, 100),
                    Triple("pineapple", 10, 200),
                    Triple("equa_leaves", 20, 100),
                    Triple("orange", 20, 100),
                    Triple("lemon", 20, 100),
                    Triple("lime", 20, 100),
                    Triple("dwellberries", 5, 100),
                    Triple("cocktail_shaker", 10, 200),
                    Triple("chocolate_bar", 20, 100),
                    Triple("chocolate_dust", 10, 100),
                    Triple("pot_of_cream", 5, 100),
                    Triple("bucket_milk", 5, 100),
                    Triple("knife", 5, 200),
                    Triple("cocktail_glass_empty", 20, 10),
                )) stock += stock(g(name), count, ticks)
        }
    }
}

internal object GnomeIngredientPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<GnomeIngredientShops>("ingredient-people.toml")
    }
}

internal object GnomeKingWorms : MapObjSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<GnomeIngredientShops>("king-worms.toml")
    }
}

public class GnomeIngredientShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(GnomeFoodNpcs.waiter) {
            if (player.members)
                shops.open(player, "Gianne's Restaurant", GnomeFoodInvs.waiter, 25.0, 100.0, 1.0)
        }
        onOpNpc1(GnomeFoodNpcs.waiter) {
            if (player.members)
                shops.open(player, "Gianne's Restaurant", GnomeFoodInvs.waiter, 25.0, 100.0, 1.0)
        }
        onOpNpc3(GnomeFoodNpcs.hudo) { trade(true) }
        onOpNpc3(GnomeFoodNpcs.funch) { trade(false) }
        onOpNpc1(GnomeFoodNpcs.hudo) {
            if (choice2("Show me your ingredients.", true, "Leave.", false)) trade(true)
        }
        onOpNpc1(GnomeFoodNpcs.funch) {
            if (choice2("Show me your ingredients.", true, "Leave.", false)) trade(false)
        }
    }

    private fun ProtectedAccess.trade(hudo: Boolean) {
        if (player.members)
            shops.open(
                player,
                if (hudo) "Grand Tree Groceries" else "Funch's Fine Groceries",
                if (hudo) GnomeFoodInvs.hudo else GnomeFoodInvs.funch,
                55.0,
                100.0,
                1.0,
            )
    }
}
