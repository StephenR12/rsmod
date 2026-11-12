package org.rsmod.content.skills.crafting.glassmaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class GlassblowingRecipe(
    val name: String,
    val level: Int,
    val output: ObjType,
    val xp: Double,
    val button: ComponentType,
)

public class GlassblowingRecipeRegistry @Inject constructor() {
    public val recipes: List<GlassblowingRecipe> =
        listOf(
            GlassblowingRecipe("Beer glass", 1, objs.beer_glass, 17.5, components.skillmulti_a),
            GlassblowingRecipe(
                "Empty candle lantern",
                4,
                objs.empty_candle_lantern,
                19.0,
                components.skillmulti_b,
            ),
            GlassblowingRecipe(
                "Empty oil lamp",
                12,
                objs.empty_oil_lamp,
                25.0,
                components.skillmulti_c,
            ),
            GlassblowingRecipe("Vial", 33, objs.empty_vial, 35.0, components.skillmulti_d),
            GlassblowingRecipe(
                "Empty fishbowl",
                42,
                objs.empty_fishbowl,
                42.5,
                components.skillmulti_e,
            ),
            GlassblowingRecipe(
                "Unpowered orb",
                46,
                objs.unpowered_orb,
                52.5,
                components.skillmulti_f,
            ),
            GlassblowingRecipe(
                "Lantern lens",
                49,
                objs.lantern_lens,
                55.0,
                components.skillmulti_g,
            ),
            GlassblowingRecipe(
                "Empty light orb",
                87,
                objs.empty_light_orb,
                70.0,
                components.skillmulti_h,
            ),
        )

    private val byButton: Map<ComponentType, GlassblowingRecipe> = recipes.associateBy { it.button }

    public fun forButton(button: ComponentType): GlassblowingRecipe? =
        byButton.entries.firstOrNull { it.key.isType(button) }?.value
}
