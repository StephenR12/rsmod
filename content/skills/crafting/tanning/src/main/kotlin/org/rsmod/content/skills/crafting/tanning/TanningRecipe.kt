package org.rsmod.content.skills.crafting.tanning

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class TanningButtons(
    val all: ComponentType,
    val x: ComponentType,
    val five: ComponentType,
    val one: ComponentType,
) {
    public val values: List<ComponentType> = listOf(all, x, five, one)
}

public data class TanningRecipe(
    val name: String,
    val input: ObjType,
    val output: ObjType,
    val members: Boolean,
    val standardPrice: Int,
    val premiumPrice: Int,
    val eodanPrice: Int,
    val modelComponent: ComponentType,
    val textComponent: ComponentType,
    val priceComponent: ComponentType,
    val buttons: TanningButtons,
)

public class TanningRecipeRegistry @Inject constructor() {
    public val recipes: List<TanningRecipe> =
        listOf(
            TanningRecipe(
                "Soft leather",
                objs.cow_hide,
                objs.leather,
                false,
                1,
                2,
                10,
                components.tanner_tanning_a_model,
                components.tanner_tanning_a_text,
                components.tanner_tanning_a_price,
                TanningButtons(
                    components.tanner_tanning_a_all,
                    components.tanner_tanning_a_x,
                    components.tanner_tanning_a_5,
                    components.tanner_tanning_a_1,
                ),
            ),
            TanningRecipe(
                "Hard leather",
                objs.cow_hide,
                objs.hard_leather,
                false,
                3,
                5,
                30,
                components.tanner_tanning_b_model,
                components.tanner_tanning_b_text,
                components.tanner_tanning_b_price,
                TanningButtons(
                    components.tanner_tanning_b_all,
                    components.tanner_tanning_b_x,
                    components.tanner_tanning_b_5,
                    components.tanner_tanning_b_1,
                ),
            ),
            TanningRecipe(
                "Snakeskin",
                objs.snake_hide,
                objs.snakeskin,
                true,
                15,
                25,
                150,
                components.tanner_tanning_c_model,
                components.tanner_tanning_c_text,
                components.tanner_tanning_c_price,
                TanningButtons(
                    components.tanner_tanning_c_all,
                    components.tanner_tanning_c_x,
                    components.tanner_tanning_c_5,
                    components.tanner_tanning_c_1,
                ),
            ),
            TanningRecipe(
                "Swamp snakeskin",
                objs.swamp_snake_hide,
                objs.snakeskin,
                true,
                20,
                45,
                200,
                components.tanner_tanning_d_model,
                components.tanner_tanning_d_text,
                components.tanner_tanning_d_price,
                TanningButtons(
                    components.tanner_tanning_d_all,
                    components.tanner_tanning_d_x,
                    components.tanner_tanning_d_5,
                    components.tanner_tanning_d_1,
                ),
            ),
            TanningRecipe(
                "Green dragon leather",
                objs.green_dragonhide,
                objs.green_dragon_leather,
                true,
                20,
                45,
                200,
                components.tanner_tanning_e_model,
                components.tanner_tanning_e_text,
                components.tanner_tanning_e_price,
                TanningButtons(
                    components.tanner_tanning_e_all,
                    components.tanner_tanning_e_x,
                    components.tanner_tanning_e_5,
                    components.tanner_tanning_e_1,
                ),
            ),
            TanningRecipe(
                "Blue dragon leather",
                objs.blue_dragonhide,
                objs.blue_dragon_leather,
                true,
                20,
                45,
                200,
                components.tanner_tanning_f_model,
                components.tanner_tanning_f_text,
                components.tanner_tanning_f_price,
                TanningButtons(
                    components.tanner_tanning_f_all,
                    components.tanner_tanning_f_x,
                    components.tanner_tanning_f_5,
                    components.tanner_tanning_f_1,
                ),
            ),
            TanningRecipe(
                "Red dragon leather",
                objs.red_dragonhide,
                objs.red_dragon_leather,
                true,
                20,
                45,
                200,
                components.tanner_tanning_g_model,
                components.tanner_tanning_g_text,
                components.tanner_tanning_g_price,
                TanningButtons(
                    components.tanner_tanning_g_all,
                    components.tanner_tanning_g_x,
                    components.tanner_tanning_g_5,
                    components.tanner_tanning_g_1,
                ),
            ),
            TanningRecipe(
                "Black dragon leather",
                objs.black_dragonhide,
                objs.black_dragon_leather,
                true,
                20,
                45,
                200,
                components.tanner_tanning_h_model,
                components.tanner_tanning_h_text,
                components.tanner_tanning_h_price,
                TanningButtons(
                    components.tanner_tanning_h_all,
                    components.tanner_tanning_h_x,
                    components.tanner_tanning_h_5,
                    components.tanner_tanning_h_1,
                ),
            ),
        )
}
