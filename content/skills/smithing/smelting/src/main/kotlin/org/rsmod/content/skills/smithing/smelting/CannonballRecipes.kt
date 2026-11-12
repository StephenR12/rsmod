package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class CannonballRecipe(
    val mould: ObjType,
    val bars: Int,
    val outputCount: Int,
    val xp: Double,
)

public class CannonballRecipeRegistry @Inject constructor() {
    public val regular: CannonballRecipe =
        CannonballRecipe(mould = objs.ammo_mould, bars = 1, outputCount = 4, xp = 25.6)

    public val double: CannonballRecipe =
        CannonballRecipe(mould = objs.double_ammo_mould, bars = 2, outputCount = 8, xp = 51.2)

    public val recipes: List<CannonballRecipe> = listOf(regular, double)

    public fun forMould(mould: ObjType): CannonballRecipe? =
        recipes.firstOrNull { it.mould.id == mould.id }
}
