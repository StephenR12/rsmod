package org.rsmod.content.skills.crafting.amethystcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class AmethystCuttingRecipe(
    val name: String,
    val level: Int,
    val output: ObjType,
    val outputCount: Int,
)

public class AmethystCuttingRecipeRegistry @Inject constructor() {
    public val recipes: List<AmethystCuttingRecipe> =
        listOf(
            AmethystCuttingRecipe("Amethyst bolt tips", 83, objs.amethyst_bolt_tips, 15),
            AmethystCuttingRecipe("Amethyst arrowtips", 85, objs.amethyst_arrowheads, 15),
            AmethystCuttingRecipe("Amethyst javelin tips", 87, objs.amethyst_javelin_tips, 5),
            AmethystCuttingRecipe("Amethyst dart tips", 89, objs.amethyst_dart_tip, 8),
        )

    private val byOutput: Map<ObjType, AmethystCuttingRecipe> = recipes.associateBy { it.output }

    public fun find(output: ObjType): AmethystCuttingRecipe? = byOutput[output]

    public fun forButton(button: ComponentType): AmethystCuttingRecipe? {
        val index = buttons.indexOfFirst { it.isType(button) }
        return recipes.getOrNull(index)
    }

    private companion object {
        val buttons =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
            )
    }
}
