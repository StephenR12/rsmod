package org.rsmod.content.skills.crafting.leatheraddons

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class LeatherAddonRecipe(
    val name: String,
    val level: Int,
    val base: ObjType,
    val addon: ObjType,
    val output: ObjType,
    val xp: Double,
)

public class LeatherAddonRecipeRegistry @Inject constructor() {
    public val recipes: List<LeatherAddonRecipe> =
        listOf(
            LeatherAddonRecipe(
                "Studded body",
                41,
                objs.leather_body,
                objs.steel_studs,
                objs.studded_body,
                40.0,
            ),
            LeatherAddonRecipe(
                "Studded chaps",
                44,
                objs.leather_chaps,
                objs.steel_studs,
                objs.studded_chaps,
                42.0,
            ),
            LeatherAddonRecipe(
                "Spiky vambraces",
                32,
                objs.leather_vambraces,
                objs.kebbit_claws,
                objs.spiky_vambraces,
                6.0,
            ),
            LeatherAddonRecipe(
                "Green spiky vambraces",
                32,
                objs.green_dhide_vambraces,
                objs.kebbit_claws,
                objs.green_spiky_vambraces,
                6.0,
            ),
            LeatherAddonRecipe(
                "Blue spiky vambraces",
                32,
                objs.blue_dhide_vambraces,
                objs.kebbit_claws,
                objs.blue_spiky_vambraces,
                6.0,
            ),
            LeatherAddonRecipe(
                "Red spiky vambraces",
                32,
                objs.red_dhide_vambraces,
                objs.kebbit_claws,
                objs.red_spiky_vambraces,
                6.0,
            ),
            LeatherAddonRecipe(
                "Black spiky vambraces",
                32,
                objs.black_dhide_vambraces,
                objs.kebbit_claws,
                objs.black_spiky_vambraces,
                6.0,
            ),
        )

    private val byOutput = recipes.associateBy(LeatherAddonRecipe::output)

    public fun forOutput(output: ObjType): LeatherAddonRecipe? = byOutput[output]
}
