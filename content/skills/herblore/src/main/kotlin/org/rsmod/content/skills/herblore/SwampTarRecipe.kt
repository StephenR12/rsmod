package org.rsmod.content.skills.herblore

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class SwampTarRecipe(
    public val herb: ObjType,
    public val output: ObjType,
    public val level: Int,
    public val xp: Double,
)

/** Final cache-233 production recipes; irit tar is post-cutoff Sailing content. */
public object SwampTarRecipes {
    public val all: List<SwampTarRecipe> =
        listOf(
            SwampTarRecipe(objs.guam_leaf, objs.guam_tar, 19, 30.0),
            SwampTarRecipe(objs.ground_guam, objs.guam_tar, 19, 30.0),
            SwampTarRecipe(objs.marrentill, objs.marrentill_tar, 31, 42.5),
            SwampTarRecipe(objs.tarromin, objs.tarromin_tar, 39, 55.0),
            SwampTarRecipe(objs.harralander, objs.harralander_tar, 44, 72.5),
        )

    public val byHerb: Map<Int, SwampTarRecipe> = all.associateBy { it.herb.id }
}
