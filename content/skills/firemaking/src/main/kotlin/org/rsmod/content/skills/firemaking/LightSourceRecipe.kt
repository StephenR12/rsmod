package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class LightSourceRecipe(
    public val unlit: ObjType,
    public val lit: ObjType,
    public val level: Int,
    public val extinguishOp: Int,
    public val canUseDrySticks: Boolean = false,
    public val wearable: Boolean = false,
)

public object LightSourceRecipes {
    public val all: List<LightSourceRecipe> =
        listOf(
            LightSourceRecipe(objs.unlit_torch, objs.lit_torch, 1, 4, canUseDrySticks = true),
            LightSourceRecipe(objs.unlit_candle, objs.lit_candle, 1, 1),
            LightSourceRecipe(objs.unlit_black_candle, objs.lit_black_candle, 1, 1),
            LightSourceRecipe(objs.unlit_candle_lantern, objs.lit_candle_lantern, 4, 2),
            LightSourceRecipe(objs.unlit_black_candle_lantern, objs.lit_black_candle_lantern, 4, 2),
            LightSourceRecipe(objs.unlit_oil_lamp, objs.lit_oil_lamp, 12, 2),
            LightSourceRecipe(objs.unlit_oil_lantern, objs.lit_oil_lantern, 26, 2),
            LightSourceRecipe(objs.unlit_bullseye_lantern, objs.lit_bullseye_lantern, 49, 2),
            LightSourceRecipe(objs.unlit_sapphire_lantern, objs.lit_sapphire_lantern, 49, 1),
            LightSourceRecipe(objs.unlit_emerald_lantern, objs.lit_emerald_lantern, 49, 3),
            LightSourceRecipe(
                objs.unlit_mining_helmet,
                objs.lit_mining_helmet,
                65,
                3,
                wearable = true,
            ),
        )

    public val byUnlit: Map<Int, LightSourceRecipe> = all.associateBy { it.unlit.id }

    public val byLit: Map<Int, LightSourceRecipe> = all.associateBy { it.lit.id }
}

public data class CandleLanternRecipe(
    public val candle: ObjType,
    public val output: ObjType,
    public val level: Int,
)

public object CandleLanternRecipes {
    public val all: List<CandleLanternRecipe> =
        listOf(
            CandleLanternRecipe(objs.unlit_candle, objs.unlit_candle_lantern, 1),
            CandleLanternRecipe(objs.unlit_black_candle, objs.unlit_black_candle_lantern, 1),
            CandleLanternRecipe(objs.lit_candle, objs.lit_candle_lantern, 4),
            CandleLanternRecipe(objs.lit_black_candle, objs.lit_black_candle_lantern, 4),
        )
}
