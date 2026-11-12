package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class GemCuttingRecipe(
    val name: String,
    val level: Int,
    val uncut: ObjType,
    val cut: ObjType,
    val successXp: Double,
    val animation: SeqType,
    val members: Boolean,
    val crush: GemCrushChance? = null,
)

public data class GemCrushChance(val low: Int, val high: Int, val xp: Double)

public class GemCuttingRecipeRegistry @Inject constructor() {
    public val recipes: List<GemCuttingRecipe> =
        listOf(
            GemCuttingRecipe(
                "Opal",
                1,
                objs.uncut_opal,
                objs.opal,
                15.0,
                seqs.human_opalcutting,
                members = true,
                GemCrushChance(low = 128, high = 250, xp = 3.8),
            ),
            GemCuttingRecipe(
                "Jade",
                13,
                objs.uncut_jade,
                objs.jade,
                20.0,
                seqs.human_jadecutting,
                members = true,
                GemCrushChance(low = 100, high = 245, xp = 5.0),
            ),
            GemCuttingRecipe(
                "Red topaz",
                16,
                objs.uncut_red_topaz,
                objs.red_topaz,
                25.0,
                seqs.human_redtopazcutting,
                members = true,
                GemCrushChance(low = 90, high = 240, xp = 6.3),
            ),
            GemCuttingRecipe(
                "Sapphire",
                20,
                objs.uncut_sapphire,
                objs.sapphire,
                50.0,
                seqs.human_sapphirecutting,
                members = false,
            ),
            GemCuttingRecipe(
                "Emerald",
                27,
                objs.uncut_emerald,
                objs.emerald,
                67.5,
                seqs.human_emeraldcutting,
                members = false,
            ),
            GemCuttingRecipe(
                "Ruby",
                34,
                objs.uncut_ruby,
                objs.ruby,
                85.0,
                seqs.human_rubycutting,
                members = false,
            ),
            GemCuttingRecipe(
                "Diamond",
                43,
                objs.uncut_diamond,
                objs.diamond,
                107.5,
                seqs.human_diamondcutting,
                members = false,
            ),
            GemCuttingRecipe(
                "Dragonstone",
                55,
                objs.uncut_dragonstone,
                objs.dragonstone,
                137.5,
                seqs.human_dragonstonecutting,
                members = true,
            ),
            GemCuttingRecipe(
                "Onyx",
                67,
                objs.uncut_onyx,
                objs.onyx,
                167.5,
                seqs.human_onyxcutting,
                members = true,
            ),
            GemCuttingRecipe(
                "Zenyte",
                89,
                objs.uncut_zenyte,
                objs.zenyte,
                50.0,
                seqs.human_zenytecutting,
                members = true,
            ),
        )

    private val byUncut: Map<ObjType, GemCuttingRecipe> = recipes.associateBy { it.uncut }

    public fun forUncut(uncut: ObjType): GemCuttingRecipe? = byUncut[uncut]
}
