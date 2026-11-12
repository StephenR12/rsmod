package org.rsmod.content.skills.herblore

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class HerbCleaningRecipe(
    public val grimy: ObjType,
    public val clean: ObjType,
    public val level: Int,
    public val xp: Double,
)

public object HerbCleaningRecipes {
    public val all: List<HerbCleaningRecipe> =
        listOf(
            HerbCleaningRecipe(objs.grimy_guam_leaf, objs.guam_leaf, 3, 2.5),
            HerbCleaningRecipe(objs.grimy_marrentill, objs.marrentill, 5, 3.8),
            HerbCleaningRecipe(objs.grimy_tarromin, objs.tarromin, 11, 5.0),
            HerbCleaningRecipe(objs.grimy_harralander, objs.harralander, 20, 6.3),
            HerbCleaningRecipe(objs.grimy_ranarr_weed, objs.ranarr_weed, 25, 7.5),
            HerbCleaningRecipe(objs.grimy_toadflax, objs.toadflax, 30, 8.0),
            HerbCleaningRecipe(objs.grimy_irit_leaf, objs.irit_leaf, 40, 8.8),
            HerbCleaningRecipe(objs.grimy_avantoe, objs.avantoe, 48, 10.0),
            HerbCleaningRecipe(objs.grimy_kwuarm, objs.kwuarm, 54, 11.3),
            HerbCleaningRecipe(objs.grimy_huasca, objs.huasca, 58, 11.8),
            HerbCleaningRecipe(objs.grimy_snapdragon, objs.snapdragon, 59, 11.8),
            HerbCleaningRecipe(objs.grimy_cadantine, objs.cadantine, 65, 12.5),
            HerbCleaningRecipe(objs.grimy_lantadyme, objs.lantadyme, 67, 13.1),
            HerbCleaningRecipe(objs.grimy_dwarf_weed, objs.dwarf_weed, 70, 13.8),
            HerbCleaningRecipe(objs.grimy_torstol, objs.torstol, 75, 15.0),
        )

    public val byGrimy: Map<Int, HerbCleaningRecipe> = all.associateBy { it.grimy.id }
}
