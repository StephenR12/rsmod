package org.rsmod.content.skills.crafting.specialistleatherworking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class SpecialistLeatherworkingRecipe(
    val name: String,
    val level: Int,
    val material: ObjType,
    val materialCount: Int,
    val output: ObjType,
    val xp: Double,
    val button: ComponentType,
    val fremennikIslesProgress: Int? = null,
)

public class SpecialistLeatherworkingRecipeRegistry @Inject constructor() {
    public val recipes: List<SpecialistLeatherworkingRecipe> =
        listOf(
            SpecialistLeatherworkingRecipe(
                "Snakeskin boots",
                45,
                objs.snakeskin,
                6,
                objs.snakeskin_boots,
                30.0,
                components.skillmulti_a,
            ),
            SpecialistLeatherworkingRecipe(
                "Snakeskin vambraces",
                47,
                objs.snakeskin,
                8,
                objs.snakeskin_vambraces,
                35.0,
                components.skillmulti_b,
            ),
            SpecialistLeatherworkingRecipe(
                "Snakeskin bandana",
                48,
                objs.snakeskin,
                5,
                objs.snakeskin_bandana,
                45.0,
                components.skillmulti_c,
            ),
            SpecialistLeatherworkingRecipe(
                "Snakeskin chaps",
                51,
                objs.snakeskin,
                12,
                objs.snakeskin_chaps,
                50.0,
                components.skillmulti_d,
            ),
            SpecialistLeatherworkingRecipe(
                "Snakeskin body",
                53,
                objs.snakeskin,
                15,
                objs.snakeskin_body,
                55.0,
                components.skillmulti_e,
            ),
            SpecialistLeatherworkingRecipe(
                "Yak-hide armour (legs)",
                43,
                objs.cured_yak_hide,
                1,
                objs.yak_hide_armour_legs,
                32.0,
                components.skillmulti_a,
                FREMENNIK_ISLES_ARMOUR_STAGE,
            ),
            SpecialistLeatherworkingRecipe(
                "Yak-hide armour (top)",
                46,
                objs.cured_yak_hide,
                2,
                objs.yak_hide_armour_top,
                32.0,
                components.skillmulti_b,
                FREMENNIK_ISLES_ARMOUR_STAGE,
            ),
            SpecialistLeatherworkingRecipe(
                "Xerician hat",
                14,
                objs.xerician_fabric,
                3,
                objs.xerician_hat,
                66.0,
                components.skillmulti_a,
            ),
            SpecialistLeatherworkingRecipe(
                "Xerician robe",
                17,
                objs.xerician_fabric,
                4,
                objs.xerician_robe,
                88.0,
                components.skillmulti_b,
            ),
            SpecialistLeatherworkingRecipe(
                "Xerician top",
                22,
                objs.xerician_fabric,
                5,
                objs.xerician_top,
                110.0,
                components.skillmulti_c,
            ),
        )

    private val byMaterial: Map<ObjType, List<SpecialistLeatherworkingRecipe>> =
        recipes.groupBy { it.material }

    public fun forMaterial(material: ObjType): List<SpecialistLeatherworkingRecipe> =
        byMaterial[material].orEmpty()

    public fun forButton(
        material: ObjType,
        button: ComponentType,
    ): SpecialistLeatherworkingRecipe? =
        forMaterial(material).firstOrNull { it.button.isType(button) }

    public fun forOutput(output: ObjType): SpecialistLeatherworkingRecipe? =
        recipes.firstOrNull { it.output == output }

    public val materials: Set<ObjType> = byMaterial.keys

    public companion object {
        public const val FREMENNIK_ISLES_ARMOUR_STAGE: Int = 280
    }
}
