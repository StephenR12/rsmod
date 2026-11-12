package org.rsmod.content.skills.fletching.ballistaassembly

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class BallistaAssemblyRecipe(
    val name: String,
    val level: Int,
    val primary: ObjType,
    val secondary: ObjType,
    val output: ObjType,
    val xp: Double,
)

public class BallistaAssemblyRecipeRegistry @Inject constructor() {
    public val recipes: List<BallistaAssemblyRecipe> =
        listOf(
            BallistaAssemblyRecipe(
                "Incomplete light ballista",
                47,
                objs.light_ballista_frame,
                objs.ballista_limbs,
                objs.incomplete_light_ballista,
                15.0,
            ),
            BallistaAssemblyRecipe(
                "Unstrung light ballista",
                47,
                objs.incomplete_light_ballista,
                objs.ballista_spring,
                objs.unstrung_light_ballista,
                15.0,
            ),
            BallistaAssemblyRecipe(
                "Light ballista",
                47,
                objs.unstrung_light_ballista,
                objs.monkey_tail,
                objs.light_ballista,
                300.0,
            ),
            BallistaAssemblyRecipe(
                "Incomplete heavy ballista",
                72,
                objs.heavy_ballista_frame,
                objs.ballista_limbs,
                objs.incomplete_heavy_ballista,
                30.0,
            ),
            BallistaAssemblyRecipe(
                "Unstrung heavy ballista",
                72,
                objs.incomplete_heavy_ballista,
                objs.ballista_spring,
                objs.unstrung_heavy_ballista,
                30.0,
            ),
            BallistaAssemblyRecipe(
                "Heavy ballista",
                72,
                objs.unstrung_heavy_ballista,
                objs.monkey_tail,
                objs.heavy_ballista,
                600.0,
            ),
        )
}
