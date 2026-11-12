package org.rsmod.content.skills.crafting.pottery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class PotteryRecipe(
    val name: String,
    val level: Int,
    val unfired: ObjType,
    val fired: ObjType,
    val shapingXp: Double,
    val firingXp: Double,
    val outputCount: Int,
    val members: Boolean,
    val questLocked: Boolean,
    val button: ComponentType,
)

public class PotteryRecipeRegistry @Inject constructor() {
    public val recipes: List<PotteryRecipe> =
        listOf(
            PotteryRecipe(
                "Pot",
                1,
                objs.unfired_pot,
                objs.pot_empty,
                6.3,
                6.3,
                1,
                false,
                false,
                components.skillmulti_a,
            ),
            PotteryRecipe(
                "Empty cup",
                3,
                objs.unfired_cup,
                objs.cup_empty,
                8.5,
                8.5,
                4,
                true,
                false,
                components.skillmulti_b,
            ),
            PotteryRecipe(
                "Pie dish",
                7,
                objs.unfired_pie_dish,
                objs.pie_dish,
                15.0,
                10.0,
                1,
                false,
                false,
                components.skillmulti_c,
            ),
            PotteryRecipe(
                "Bowl",
                8,
                objs.unfired_bowl,
                objs.bowl_empty,
                18.0,
                15.0,
                1,
                false,
                false,
                components.skillmulti_d,
            ),
            PotteryRecipe(
                "Empty plant pot",
                19,
                objs.unfired_plant_pot,
                objs.empty_plant_pot,
                20.0,
                17.5,
                1,
                true,
                false,
                components.skillmulti_e,
            ),
            PotteryRecipe(
                "Pot lid",
                25,
                objs.unfired_pot_lid,
                objs.pot_lid,
                20.0,
                20.0,
                1,
                true,
                true,
                components.skillmulti_f,
            ),
        )

    private val byButton = recipes.associateBy(PotteryRecipe::button)
    private val byUnfired = recipes.associateBy(PotteryRecipe::unfired)

    public fun forButton(button: ComponentType): PotteryRecipe? =
        byButton.entries.firstOrNull { it.key.isType(button) }?.value

    public fun forUnfired(obj: ObjType): PotteryRecipe? = byUnfired[obj]
}
