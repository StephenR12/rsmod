package org.rsmod.content.skills.crafting.customfur

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class CustomFurRecipe(
    val name: String,
    val output: ObjType,
    val furs: List<ObjType>,
    val furCount: Int,
    val fee: Int,
)

public class CustomFurRecipeRegistry @Inject constructor() {
    public val recipes: List<CustomFurRecipe> =
        listOf(
            recipe("Kyatt legs", objs.kyatt_legs, objs.tatty_kyatt_fur, objs.kyatt_fur, fee = 200),
            recipe("Kyatt top", objs.kyatt_top, objs.tatty_kyatt_fur, objs.kyatt_fur, fee = 200),
            recipe("Kyatt hat", objs.kyatt_hat, objs.kyatt_fur, fee = 1000),
            recipe(
                "Larupia legs",
                objs.larupia_legs,
                objs.tatty_larupia_fur,
                objs.larupia_fur,
                fee = 100,
            ),
            recipe(
                "Larupia top",
                objs.larupia_top,
                objs.tatty_larupia_fur,
                objs.larupia_fur,
                fee = 100,
            ),
            recipe("Larupia hat", objs.larupia_hat, objs.larupia_fur, fee = 500),
            recipe(
                "Graahk legs",
                objs.graahk_legs,
                objs.tatty_graahk_fur,
                objs.graahk_fur,
                fee = 150,
            ),
            recipe(
                "Graahk top",
                objs.graahk_top,
                objs.tatty_graahk_fur,
                objs.graahk_fur,
                fee = 150,
            ),
            recipe("Graahk headdress", objs.graahk_headdress, objs.graahk_fur, fee = 750),
            recipe("Wood camo top", objs.wood_camo_top, objs.common_kebbit_fur, 2, fee = 20),
            recipe("Wood camo legs", objs.wood_camo_legs, objs.common_kebbit_fur, 2, fee = 20),
            recipe("Jungle camo top", objs.jungle_camo_top, objs.feldip_weasel_fur, 2, fee = 20),
            recipe("Jungle camo legs", objs.jungle_camo_legs, objs.feldip_weasel_fur, 2, fee = 20),
            recipe("Desert camo top", objs.desert_camo_top, objs.desert_devil_fur, 2, fee = 20),
            recipe("Desert camo legs", objs.desert_camo_legs, objs.desert_devil_fur, 2, fee = 20),
            recipe("Polar camo top", objs.polar_camo_top, objs.polar_kebbit_fur, 2, fee = 20),
            recipe("Polar camo legs", objs.polar_camo_legs, objs.polar_kebbit_fur, 2, fee = 20),
            recipe("Spotted cape", objs.spotted_cape, objs.spotted_kebbit_fur, 2, fee = 400),
            recipe("Spottier cape", objs.spottier_cape, objs.dashing_kebbit_fur, 2, fee = 800),
            recipe("Gloves of silence", objs.gloves_of_silence, objs.dark_kebbit_fur, 2, fee = 600),
        )

    private val byOutput: Map<Int, CustomFurRecipe> = recipes.associateBy { it.output.id }

    public fun forOutput(output: ObjType): CustomFurRecipe? = byOutput[output.id]

    private fun recipe(
        name: String,
        output: ObjType,
        vararg furs: ObjType,
        fee: Int,
    ): CustomFurRecipe = CustomFurRecipe(name, output, furs.toList(), 1, fee)

    private fun recipe(
        name: String,
        output: ObjType,
        fur: ObjType,
        furCount: Int,
        fee: Int,
    ): CustomFurRecipe = CustomFurRecipe(name, output, listOf(fur), furCount, fee)
}
