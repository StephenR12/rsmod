package org.rsmod.content.skills.crafting.spinning

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class SpinningRecipe(
    val name: String,
    val level: Int,
    val inputs: List<ObjType>,
    val output: ObjType,
    val xp: Double,
    val members: Boolean,
    val button: ComponentType,
)

public class SpinningRecipeRegistry @Inject constructor() {
    public val recipes: List<SpinningRecipe> =
        listOf(
            SpinningRecipe(
                "Ball of wool",
                1,
                listOf(objs.wool),
                objs.ball_of_wool,
                2.5,
                false,
                components.skillmulti_a,
            ),
            SpinningRecipe(
                "Bow string",
                10,
                listOf(objs.flax),
                objs.bow_string,
                15.0,
                true,
                components.skillmulti_b,
            ),
            SpinningRecipe(
                "Crossbow string",
                10,
                listOf(
                    objs.sinew,
                    objs.oak_roots,
                    objs.willow_roots,
                    objs.maple_roots,
                    objs.yew_roots,
                    objs.magic_roots,
                ),
                objs.crossbow_string,
                15.0,
                true,
                components.skillmulti_c,
            ),
            SpinningRecipe(
                "Magic string",
                19,
                listOf(objs.magic_roots),
                objs.magic_string,
                30.0,
                true,
                components.skillmulti_d,
            ),
            SpinningRecipe(
                "Rope",
                30,
                listOf(objs.yak_hair),
                objs.rope,
                25.0,
                true,
                components.skillmulti_e,
            ),
        )

    private val byButton = recipes.associateBy(SpinningRecipe::button)
    private val inputs = recipes.flatMap(SpinningRecipe::inputs).toSet()

    public fun forButton(button: ComponentType): SpinningRecipe? =
        byButton.entries.firstOrNull { it.key.isType(button) }?.value

    public fun isInput(input: ObjType): Boolean = input in inputs
}
