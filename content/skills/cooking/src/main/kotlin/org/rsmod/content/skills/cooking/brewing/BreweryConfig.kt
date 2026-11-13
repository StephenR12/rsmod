package org.rsmod.content.skills.cooking.brewing

import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.varp.VarpEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object BreweryObjs : ObjReferences() {
    val names =
        listOf(
            "dwarven_stout",
            "asgarnian_ale",
            "greenmans_ale",
            "wizards_mind_bomb",
            "dragon_bitter",
            "moonlight_mead",
            "axemans_folly",
            "chefs_delight",
            "slayers_respite",
            "cider",
        )
    val normal = names.map(::find)
    val mature = names.map { find("mature_" + it) }
    val kegs = names.map { name -> (1..4).map { find("keg_${name}_$it") } }
    val matureKegs = names.map { name -> (1..4).map { find("keg_mature_${name}_$it") } }
    val water = find("bucket_water")
    val bucket = find("bucket_empty")
    val apple = find("cooking_apple")
    val barley = find("barley")
    val malt = find("barley_malt")
    val yeast = find("ale_yeast")
    val stuff = find("brew_hyper_yeast")
    val pot = find("pot_empty")
    val glass = find("beer_glass")
    val fruit = find("calquat_fruit")
    val keg = find("calquat_fruit_keg_empty")
    val mush = find("apple_mush")
    val ghostAmulets = listOf(find("amulet_of_ghostspeak"), find("amulet_of_ghostspeak_enchanted"))
    val tokens = find("ectotoken")
    val ingredients =
        listOf(
                "hammerstone_hops",
                "asgarnian_hops",
                "harralander",
                "yanillian_hops",
                "krandorian_hops",
                "bittercap_mushroom",
                "oak_roots",
                "chocolate_dust",
                "wildblood_hops",
                "apple_mush",
            )
            .map(::find)
}

public object BreweryLocs : LocReferences() {
    val vats = (1..3).map { find("brewing_vat_$it") }
    val valves = (1..3).map { find("vat_valve_$it") }
    val barrels = (1..3).map { find("brewing_barrel_$it") }
    val press = find("farming_applebarrel_empty")
    val mush = find("farming_applebarrel_mush")
    val tap = find("farming_applebarrel_tap")
    val ladder = find("phasmatys_brewery_ladder")
    val trapdoor = find("phasmatys_brewery_trapdoor")
    val trapdoorOpen = find("phasmatys_brewery_trapdoor_open")
    val vatForms =
        listOf("vat_empty", "vat_water", "vat_barley", "vat_bad_ale", "vat_bad_cider") +
            BreweryObjs.names.flatMap { name ->
                listOf(
                    "vat_${name}_hops",
                    "vat_${name}_brewing_1",
                    "vat_${name}_brewing_2",
                    "vat_${name}_normal",
                    "vat_${name}_mature",
                )
            }
    val transformedVats = vatForms.map(::find)
    val barrelForms =
        listOf("barrel_empty", "barrel_bad_ale", "barrel_bad_cider", "barrel_unfinished_ale") +
            BreweryObjs.names.flatMap { name -> listOf("barrel_" + name, "barrel_${name}_mature") }
    val transformedBarrels = barrelForms.map(::find)
}

public object BreweryBits : VarBitReferences() {
    val vat = (1..3).map { find("brewing_vat_varbit_$it") }
    val barrel = (1..3).map { find("brewing_barrel_varbit_$it") }
}

public object BreweryVars : VarpReferences() {
    val started = (1..3).map { find("brewing_started_cycle_$it") }
    val control = find("brewing_batch_control")
    val native = listOf(find("farming_varp_9"), find("farming_varp_32"))
}

internal object BrewerySaves : VarpBuilder() {
    init {
        for (i in 1..3) build("brewing_started_cycle_$i") {
            permanent = true
            transmitNever = true
        }
        build("brewing_batch_control") {
            permanent = true
            transmitNever = true
        }
    }
}

internal object BreweryNativeSaves : VarpEditor() {
    init {
        for (save in BreweryVars.native) edit(save) { permanent = true }
    }
}

public object BreweryNpcs : NpcReferences() {
    val all = listOf(find("blandebir"), find("metarialus"), find("aldarin_brewing_helper"))
}

public data class BrewerySite(
    val vat: CoordGrid,
    val valve: CoordGrid,
    val barrel: CoordGrid,
    val press: CoordGrid,
    val tap: CoordGrid,
)

public object Breweries {
    val all =
        listOf(
            BrewerySite(
                CoordGrid(2917, 10194, 1),
                CoordGrid(2918, 10193, 1),
                CoordGrid(2917, 10193, 1),
                CoordGrid(2914, 10192, 1),
                CoordGrid(2915, 10192, 1),
            ),
            BrewerySite(
                CoordGrid(3679, 9961),
                CoordGrid(3680, 9960),
                CoordGrid(3679, 9960),
                CoordGrid(3676, 9958),
                CoordGrid(3677, 9958),
            ),
            BrewerySite(
                CoordGrid(1359, 2924),
                CoordGrid(1360, 2923),
                CoordGrid(1359, 2923),
                CoordGrid(1356, 2923),
                CoordGrid(1357, 2923),
            ),
        )
}

public data class AleRecipe(val index: Int, val level: Int, val xp: Double, val quantity: Int) {
    val ingredient: ObjType
        get() = BreweryObjs.ingredients[index]

    val normal: ObjType
        get() = BreweryObjs.normal[index]

    val mature: ObjType
        get() = BreweryObjs.mature[index]

    val preparation: Int
        get() = 4 + index * 6
}

public object AleRecipes {
    val all =
        listOf(
            AleRecipe(0, 19, 215.0, 4),
            AleRecipe(1, 24, 248.0, 4),
            AleRecipe(2, 29, 281.0, 4),
            AleRecipe(3, 34, 314.0, 4),
            AleRecipe(4, 39, 347.0, 4),
            AleRecipe(5, 44, 380.0, 4),
            AleRecipe(6, 49, 413.0, 1),
            AleRecipe(7, 54, 446.0, 4),
            AleRecipe(8, 59, 479.0, 4),
            AleRecipe(9, 14, 182.0, 4),
        )

    fun fromVat(value: Int): AleRecipe? =
        if (value in 4..62 && (value - 4) % 6 in 0..4) all.getOrNull((value - 4) / 6) else null
}
