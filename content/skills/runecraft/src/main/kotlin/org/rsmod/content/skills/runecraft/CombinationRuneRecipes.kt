package org.rsmod.content.skills.runecraft

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

public data class CombinationRuneRecipe(
    val altar: LocType,
    val inputRune: ObjType,
    val talisman: ObjType?,
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val name: String,
    val inputRuneName: String,
    val talismanName: String?,
    val aether: Boolean = false,
)

public object CombinationRuneRecipes {
    public val elemental: List<CombinationRuneRecipe> =
        listOf(
            recipe(
                RunecraftLocs.airAltar,
                objs.water_rune,
                RunecraftObjs.waterTalisman,
                objs.mist_rune,
                6,
                8.0,
                "mist",
                "water",
            ),
            recipe(
                RunecraftLocs.waterAltar,
                objs.air_rune,
                RunecraftObjs.airTalisman,
                objs.mist_rune,
                6,
                8.5,
                "mist",
                "air",
            ),
            recipe(
                RunecraftLocs.airAltar,
                objs.earth_rune,
                RunecraftObjs.earthTalisman,
                objs.dust_rune,
                10,
                8.3,
                "dust",
                "earth",
            ),
            recipe(
                RunecraftLocs.earthAltar,
                objs.air_rune,
                RunecraftObjs.airTalisman,
                objs.dust_rune,
                10,
                9.0,
                "dust",
                "air",
            ),
            recipe(
                RunecraftLocs.waterAltar,
                objs.earth_rune,
                RunecraftObjs.earthTalisman,
                objs.mud_rune,
                13,
                9.3,
                "mud",
                "earth",
            ),
            recipe(
                RunecraftLocs.earthAltar,
                objs.water_rune,
                RunecraftObjs.waterTalisman,
                objs.mud_rune,
                13,
                9.5,
                "mud",
                "water",
            ),
            recipe(
                RunecraftLocs.airAltar,
                objs.fire_rune,
                RunecraftObjs.fireTalisman,
                objs.smoke_rune,
                15,
                8.5,
                "smoke",
                "fire",
            ),
            recipe(
                RunecraftLocs.fireAltar,
                objs.air_rune,
                RunecraftObjs.airTalisman,
                objs.smoke_rune,
                15,
                9.5,
                "smoke",
                "air",
            ),
            recipe(
                RunecraftLocs.waterAltar,
                objs.fire_rune,
                RunecraftObjs.fireTalisman,
                objs.steam_rune,
                19,
                9.3,
                "steam",
                "fire",
            ),
            recipe(
                RunecraftLocs.fireAltar,
                objs.water_rune,
                RunecraftObjs.waterTalisman,
                objs.steam_rune,
                19,
                10.0,
                "steam",
                "water",
            ),
            recipe(
                RunecraftLocs.earthAltar,
                objs.fire_rune,
                RunecraftObjs.fireTalisman,
                objs.lava_rune,
                23,
                10.0,
                "lava",
                "fire",
            ),
            recipe(
                RunecraftLocs.fireAltar,
                objs.earth_rune,
                RunecraftObjs.earthTalisman,
                objs.lava_rune,
                23,
                10.5,
                "lava",
                "earth",
            ),
        )

    public val aether: CombinationRuneRecipe =
        CombinationRuneRecipe(
            altar = RunecraftLocs.cosmicAltar,
            inputRune = objs.soul_rune,
            talisman = null,
            output = RunecraftObjs.aetherRune,
            level = 90,
            xp = 20.0,
            name = "aether",
            inputRuneName = "soul",
            talismanName = null,
            aether = true,
        )

    public val all: List<CombinationRuneRecipe> = elemental + aether

    private fun recipe(
        altar: LocType,
        inputRune: ObjType,
        talisman: ObjType,
        output: ObjType,
        level: Int,
        xp: Double,
        name: String,
        inputElement: String,
    ): CombinationRuneRecipe =
        CombinationRuneRecipe(
            altar,
            inputRune,
            talisman,
            output,
            level,
            xp,
            name,
            "$inputElement rune",
            "$inputElement talisman",
        )
}
