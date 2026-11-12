package org.rsmod.content.skills.crafting.lunarclothing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class LunarClothingRecipe(
    val option: String,
    val level: Int,
    val output: ObjType,
    val xp: Double,
    val levelFailure: String,
    val success: String,
)

public class LunarClothingRecipeRegistry @Inject constructor() {
    public val recipes: List<LunarClothingRecipe> =
        listOf(
            LunarClothingRecipe(
                option = "Lunar Torso",
                level = 61,
                output = objs.lunar_torso,
                xp = 35.0,
                levelFailure = "You need a Crafting level of 61 to make a Lunar Torso.",
                success = "You make a Lunar Torso.",
            ),
            LunarClothingRecipe(
                option = "Lunar Trousers",
                level = 59,
                output = objs.lunar_legs,
                xp = 30.0,
                levelFailure = "You need a Crafting level of 59 to make Lunar Trousers.",
                success = "You make some Lunar Trousers.",
            ),
            LunarClothingRecipe(
                option = "Lunar Gloves",
                level = 52,
                output = objs.lunar_gloves,
                xp = 25.0,
                levelFailure = "You need a Crafting level of 52 to make Lunar Gloves.",
                success = "You make some Lunar Gloves.",
            ),
            LunarClothingRecipe(
                option = "Lunar Boots",
                level = 52,
                output = objs.lunar_boots,
                xp = 25.0,
                levelFailure = "You need a Crafting level of 52 to make Lunar Boots.",
                success = "You make some Lunar Boots.",
            ),
        )
}
