package org.rsmod.content.skills.crafting.hunterpouches

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class HunterPouchRecipe(
    val name: String,
    val level: Int,
    val fur: ObjType,
    val furCount: Int,
    val pouch: ObjType?,
    val output: ObjType,
    val xp: Double,
)

public class HunterPouchRecipeRegistry @Inject constructor() {
    public val recipes: List<HunterPouchRecipe> =
        listOf(
            HunterPouchRecipe(
                "Small fur pouch",
                35,
                objs.larupia_fur,
                4,
                null,
                objs.small_fur_pouch,
                20.0,
            ),
            HunterPouchRecipe(
                "Medium fur pouch",
                50,
                objs.graahk_fur,
                3,
                objs.small_fur_pouch,
                objs.medium_fur_pouch,
                40.0,
            ),
            HunterPouchRecipe(
                "Large fur pouch",
                65,
                objs.kyatt_fur,
                3,
                objs.medium_fur_pouch,
                objs.large_fur_pouch,
                60.0,
            ),
            HunterPouchRecipe(
                "Small meat pouch",
                35,
                objs.fox_fur,
                4,
                null,
                objs.small_meat_pouch,
                30.0,
            ),
            HunterPouchRecipe(
                "Large meat pouch",
                65,
                objs.sunlight_antelope_fur,
                3,
                objs.small_meat_pouch,
                objs.large_meat_pouch,
                60.0,
            ),
        )

    private val byFur: Map<ObjType, HunterPouchRecipe> = recipes.associateBy(HunterPouchRecipe::fur)
    private val byOutput: Map<ObjType, HunterPouchRecipe> =
        recipes.associateBy(HunterPouchRecipe::output)

    public fun forFur(fur: ObjType): HunterPouchRecipe? = byFur[fur]

    public fun forOutput(output: ObjType): HunterPouchRecipe? = byOutput[output]
}
