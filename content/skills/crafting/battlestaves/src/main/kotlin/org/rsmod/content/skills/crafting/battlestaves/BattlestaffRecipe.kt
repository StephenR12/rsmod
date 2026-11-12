package org.rsmod.content.skills.crafting.battlestaves

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType

public data class BattlestaffRecipe(
    val name: String,
    val level: Int,
    val orb: ObjType,
    val output: ObjType,
    val xp: Double,
    val spotanim: SpotanimType,
)

public class BattlestaffRecipeRegistry @Inject constructor() {
    public val recipes: List<BattlestaffRecipe> =
        listOf(
            BattlestaffRecipe(
                "Water battlestaff",
                54,
                objs.water_orb,
                objs.water_battlestaff,
                100.0,
                spotanims.battlestaff_water_crafting,
            ),
            BattlestaffRecipe(
                "Earth battlestaff",
                58,
                objs.earth_orb,
                objs.earth_battlestaff,
                112.5,
                spotanims.battlestaff_earth_crafting,
            ),
            BattlestaffRecipe(
                "Fire battlestaff",
                62,
                objs.fire_orb,
                objs.fire_battlestaff,
                125.0,
                spotanims.battlestaff_fire_crafting,
            ),
            BattlestaffRecipe(
                "Air battlestaff",
                66,
                objs.air_orb,
                objs.air_battlestaff,
                137.5,
                spotanims.battlestaff_air_crafting,
            ),
        )

    public fun forOrb(orb: ObjType): BattlestaffRecipe? = recipes.firstOrNull { it.orb == orb }

    public fun forOutput(output: ObjType): BattlestaffRecipe? =
        recipes.firstOrNull { it.output == output }
}
