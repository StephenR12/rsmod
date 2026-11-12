package org.rsmod.content.skills.herblore

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class IngredientCrushingRecipe(public val input: ObjType, public val output: ObjType)

public object IngredientCrushingRecipes {
    public val ordinary: List<IngredientCrushingRecipe> =
        listOf(
            IngredientCrushingRecipe(objs.unicorn_horn, objs.unicorn_horn_dust),
            IngredientCrushingRecipe(objs.chocolate_bar, objs.chocolate_dust),
            IngredientCrushingRecipe(objs.desert_goat_horn, objs.goat_horn_dust),
            IngredientCrushingRecipe(objs.kebbit_teeth, objs.kebbit_teeth_dust),
            IngredientCrushingRecipe(objs.blue_dragon_scale, objs.dragon_scale_dust),
            IngredientCrushingRecipe(objs.empty_bird_nest, objs.crushed_nest),
            IngredientCrushingRecipe(
                objs.superior_dragon_bones,
                objs.crushed_superior_dragon_bones,
            ),
            IngredientCrushingRecipe(objs.nihil_shard, objs.nihil_dust),
        )

    public val additional: List<IngredientCrushingRecipe> =
        listOf(
            IngredientCrushingRecipe(objs.guam_leaf, objs.ground_guam),
            IngredientCrushingRecipe(objs.charcoal, objs.ground_charcoal),
            IngredientCrushingRecipe(objs.raw_karambwan, objs.raw_karambwan_paste),
            IngredientCrushingRecipe(objs.poison_karambwan, objs.poison_karambwan_paste),
            IngredientCrushingRecipe(objs.cooked_karambwan, objs.cooked_karambwan_paste),
            IngredientCrushingRecipe(objs.mud_rune, objs.ground_mud_runes),
            IngredientCrushingRecipe(objs.garlic, objs.garlic_powder),
            IngredientCrushingRecipe(objs.bat_bones, objs.ground_bat_bones),
            IngredientCrushingRecipe(objs.ashes, objs.ground_ashes),
            IngredientCrushingRecipe(objs.rune_shards, objs.rune_dust),
            IngredientCrushingRecipe(objs.anchovies, objs.anchovy_paste),
            IngredientCrushingRecipe(objs.dried_thistle, objs.ground_thistle),
            IngredientCrushingRecipe(objs.seaweed, objs.ground_seaweed),
        )

    public val all: List<IngredientCrushingRecipe> = ordinary + additional

    public val byInput: Map<Int, IngredientCrushingRecipe> = all.associateBy { it.input.id }
}
