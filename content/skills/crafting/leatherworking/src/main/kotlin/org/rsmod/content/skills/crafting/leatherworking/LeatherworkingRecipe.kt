package org.rsmod.content.skills.crafting.leatherworking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class LeatherworkingRecipe(
    val name: String,
    val level: Int,
    val material: ObjType,
    val materialCount: Int,
    val output: ObjType,
    val xp: Double,
    val members: Boolean,
    val button: ComponentType,
)

public class LeatherworkingRecipeRegistry @Inject constructor() {
    public val recipes: List<LeatherworkingRecipe> =
        listOf(
            LeatherworkingRecipe(
                "Leather gloves",
                1,
                objs.leather,
                1,
                objs.leather_gloves,
                13.8,
                false,
                components.skillmulti_a,
            ),
            LeatherworkingRecipe(
                "Leather boots",
                7,
                objs.leather,
                1,
                objs.leather_boots,
                16.2,
                false,
                components.skillmulti_b,
            ),
            LeatherworkingRecipe(
                "Leather cowl",
                9,
                objs.leather,
                1,
                objs.leather_cowl,
                18.5,
                false,
                components.skillmulti_c,
            ),
            LeatherworkingRecipe(
                "Leather vambraces",
                11,
                objs.leather,
                1,
                objs.leather_vambraces,
                22.0,
                false,
                components.skillmulti_d,
            ),
            LeatherworkingRecipe(
                "Leather body",
                14,
                objs.leather,
                1,
                objs.leather_body,
                25.0,
                false,
                components.skillmulti_e,
            ),
            LeatherworkingRecipe(
                "Leather chaps",
                18,
                objs.leather,
                1,
                objs.leather_chaps,
                27.0,
                false,
                components.skillmulti_f,
            ),
            LeatherworkingRecipe(
                "Coif",
                38,
                objs.leather,
                1,
                objs.leather_coif,
                37.0,
                false,
                components.skillmulti_g,
            ),
            LeatherworkingRecipe(
                "Hardleather body",
                28,
                objs.hard_leather,
                1,
                objs.hardleather_body,
                35.0,
                false,
                components.skillmulti_a,
            ),
            LeatherworkingRecipe(
                "Green d'hide vambraces",
                57,
                objs.green_dragon_leather,
                1,
                objs.green_dhide_vambraces,
                62.0,
                true,
                components.skillmulti_a,
            ),
            LeatherworkingRecipe(
                "Green d'hide chaps",
                60,
                objs.green_dragon_leather,
                2,
                objs.green_dhide_chaps,
                124.0,
                true,
                components.skillmulti_b,
            ),
            LeatherworkingRecipe(
                "Green d'hide body",
                63,
                objs.green_dragon_leather,
                3,
                objs.green_dhide_body,
                186.0,
                true,
                components.skillmulti_c,
            ),
            LeatherworkingRecipe(
                "Blue d'hide vambraces",
                66,
                objs.blue_dragon_leather,
                1,
                objs.blue_dhide_vambraces,
                70.0,
                true,
                components.skillmulti_a,
            ),
            LeatherworkingRecipe(
                "Blue d'hide chaps",
                68,
                objs.blue_dragon_leather,
                2,
                objs.blue_dhide_chaps,
                140.0,
                true,
                components.skillmulti_b,
            ),
            LeatherworkingRecipe(
                "Blue d'hide body",
                71,
                objs.blue_dragon_leather,
                3,
                objs.blue_dhide_body,
                210.0,
                true,
                components.skillmulti_c,
            ),
            LeatherworkingRecipe(
                "Red d'hide vambraces",
                73,
                objs.red_dragon_leather,
                1,
                objs.red_dhide_vambraces,
                78.0,
                true,
                components.skillmulti_a,
            ),
            LeatherworkingRecipe(
                "Red d'hide chaps",
                75,
                objs.red_dragon_leather,
                2,
                objs.red_dhide_chaps,
                156.0,
                true,
                components.skillmulti_b,
            ),
            LeatherworkingRecipe(
                "Red d'hide body",
                77,
                objs.red_dragon_leather,
                3,
                objs.red_dhide_body,
                234.0,
                true,
                components.skillmulti_c,
            ),
            LeatherworkingRecipe(
                "Black d'hide vambraces",
                79,
                objs.black_dragon_leather,
                1,
                objs.black_dhide_vambraces,
                86.0,
                true,
                components.skillmulti_a,
            ),
            LeatherworkingRecipe(
                "Black d'hide chaps",
                82,
                objs.black_dragon_leather,
                2,
                objs.black_dhide_chaps,
                172.0,
                true,
                components.skillmulti_b,
            ),
            LeatherworkingRecipe(
                "Black d'hide body",
                84,
                objs.black_dragon_leather,
                3,
                objs.black_dhide_body,
                258.0,
                true,
                components.skillmulti_c,
            ),
        )

    private val byMaterial: Map<ObjType, List<LeatherworkingRecipe>> =
        recipes.groupBy { it.material }

    public fun forMaterial(material: ObjType): List<LeatherworkingRecipe> =
        byMaterial[material].orEmpty()

    public fun forButton(material: ObjType, button: ComponentType): LeatherworkingRecipe? =
        forMaterial(material).firstOrNull { it.button.isType(button) }

    public val materials: Set<ObjType> = byMaterial.keys
}
