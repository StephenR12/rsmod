package org.rsmod.content.skills.crafting.weaving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class WeavingRecipe(
    val name: String,
    val level: Int,
    val input: ObjType,
    val inputCount: Int,
    val output: ObjType,
    val xp: Double,
    val firstActionTicks: Int,
    val repeatActionTicks: Int,
    val button: ComponentType,
)

public class WeavingRecipeRegistry @Inject constructor() {
    public val recipes: List<WeavingRecipe> =
        listOf(
            WeavingRecipe(
                "Strip of cloth",
                10,
                objs.ball_of_wool,
                4,
                objs.strip_of_cloth,
                12.0,
                4,
                3,
                components.skillmulti_a,
            ),
            WeavingRecipe(
                "Empty sack",
                21,
                objs.jute_fibre,
                4,
                objs.empty_sack,
                38.0,
                4,
                3,
                components.skillmulti_b,
            ),
            WeavingRecipe(
                "Drift net",
                26,
                objs.jute_fibre,
                2,
                objs.drift_net,
                55.0,
                3,
                3,
                components.skillmulti_c,
            ),
            WeavingRecipe(
                "Basket",
                36,
                objs.willow_branch,
                6,
                objs.basket,
                56.0,
                4,
                3,
                components.skillmulti_d,
            ),
        )

    private val byButton = recipes.associateBy(WeavingRecipe::button)
    private val inputs = recipes.map(WeavingRecipe::input).toSet()

    public fun forButton(button: ComponentType): WeavingRecipe? =
        byButton.entries.firstOrNull { it.key.isType(button) }?.value

    public fun isInput(input: ObjType): Boolean = input in inputs
}
