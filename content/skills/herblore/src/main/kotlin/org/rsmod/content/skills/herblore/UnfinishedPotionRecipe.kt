package org.rsmod.content.skills.herblore

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class UnfinishedPotionRecipe(
    public val herb: ObjType,
    public val output: ObjType,
    public val level: Int,
    public val base: ObjType = objs.vial_of_water,
    public val ticks: Int = 1,
    public val preserveHerbWithGoggles: Boolean = false,
)

public object UnfinishedPotionRecipes {
    public val all: List<UnfinishedPotionRecipe> =
        listOf(
            UnfinishedPotionRecipe(objs.guam_leaf, objs.guam_potion_unf, 3),
            UnfinishedPotionRecipe(objs.marrentill, objs.marrentill_potion_unf, 5),
            UnfinishedPotionRecipe(objs.tarromin, objs.tarromin_potion_unf, 12),
            UnfinishedPotionRecipe(objs.harralander, objs.harralander_potion_unf, 22),
            UnfinishedPotionRecipe(objs.ranarr_weed, objs.ranarr_potion_unf, 30),
            UnfinishedPotionRecipe(objs.toadflax, objs.toadflax_potion_unf, 34),
            UnfinishedPotionRecipe(objs.irit_leaf, objs.irit_potion_unf, 45),
            UnfinishedPotionRecipe(objs.avantoe, objs.avantoe_potion_unf, 50),
            UnfinishedPotionRecipe(objs.kwuarm, objs.kwuarm_potion_unf, 55),
            UnfinishedPotionRecipe(objs.huasca, objs.huasca_potion_unf, 58),
            UnfinishedPotionRecipe(objs.snapdragon, objs.snapdragon_potion_unf, 63),
            UnfinishedPotionRecipe(objs.cadantine, objs.cadantine_potion_unf, 66),
            UnfinishedPotionRecipe(objs.lantadyme, objs.lantadyme_potion_unf, 69),
            UnfinishedPotionRecipe(objs.dwarf_weed, objs.dwarf_weed_potion_unf, 72),
            UnfinishedPotionRecipe(objs.torstol, objs.torstol_potion_unf, 78),
            UnfinishedPotionRecipe(
                objs.cadantine,
                objs.cadantine_blood_potion_unf,
                80,
                objs.vial_of_blood,
            ),
            UnfinishedPotionRecipe(
                objs.toadflax,
                objs.unfinished_antidote_plus,
                68,
                objs.vial_coconut_milk,
                ticks = 2,
                preserveHerbWithGoggles = true,
            ),
            UnfinishedPotionRecipe(
                objs.irit_leaf,
                objs.unfinished_antidote_plus_plus,
                79,
                objs.vial_coconut_milk,
                preserveHerbWithGoggles = true,
            ),
            UnfinishedPotionRecipe(
                objs.cactus_spine,
                objs.unfinished_weapon_poison_plus,
                73,
                objs.vial_coconut_milk,
                ticks = 2,
            ),
            UnfinishedPotionRecipe(
                objs.nightshade,
                objs.unfinished_weapon_poison_plus_plus,
                82,
                objs.vial_coconut_milk,
                ticks = 2,
                preserveHerbWithGoggles = true,
            ),
            UnfinishedPotionRecipe(
                objs.stackable_nightshade,
                objs.unfinished_weapon_poison_plus_plus,
                82,
                objs.vial_coconut_milk,
                ticks = 2,
                preserveHerbWithGoggles = true,
            ),
        )

    public val byInputs: Map<Pair<Int, Int>, UnfinishedPotionRecipe> =
        all.associateBy { Pair(it.base.id, it.herb.id) }
}
