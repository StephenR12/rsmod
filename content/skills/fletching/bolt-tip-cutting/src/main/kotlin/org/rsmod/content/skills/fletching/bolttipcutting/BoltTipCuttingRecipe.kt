package org.rsmod.content.skills.fletching.bolttipcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class BoltTipCuttingRecipe(
    val name: String,
    val level: Int,
    val input: ObjType,
    val output: ObjType,
    val outputCount: Int,
    val xp: Double,
    val sequence: SeqType,
    val warnsBeforeCutting: Boolean = false,
)

public class BoltTipCuttingRecipeRegistry @Inject constructor() {
    public val recipes: List<BoltTipCuttingRecipe> =
        listOf(
            BoltTipCuttingRecipe(
                "Opal bolt tips",
                11,
                objs.opal,
                objs.opal_bolt_tips,
                12,
                1.5,
                seqs.human_opalcutting,
            ),
            BoltTipCuttingRecipe(
                "Jade bolt tips",
                26,
                objs.jade,
                objs.jade_bolt_tips,
                12,
                2.0,
                seqs.human_jadecutting,
            ),
            BoltTipCuttingRecipe(
                "Pearl bolt tips",
                41,
                objs.oyster_pearl,
                objs.pearl_bolt_tips,
                6,
                3.2,
                seqs.xbows_fletching_pearl,
            ),
            BoltTipCuttingRecipe(
                "Pearl bolt tips",
                41,
                objs.oyster_pearls,
                objs.pearl_bolt_tips,
                24,
                3.2,
                seqs.xbows_fletching_pearl,
            ),
            BoltTipCuttingRecipe(
                "Red topaz bolt tips",
                48,
                objs.red_topaz,
                objs.red_topaz_bolt_tips,
                12,
                3.9,
                seqs.human_redtopazcutting,
            ),
            BoltTipCuttingRecipe(
                "Sapphire bolt tips",
                56,
                objs.sapphire,
                objs.sapphire_bolt_tips,
                12,
                4.0,
                seqs.human_sapphirecutting,
            ),
            BoltTipCuttingRecipe(
                "Emerald bolt tips",
                58,
                objs.emerald,
                objs.emerald_bolt_tips,
                12,
                5.5,
                seqs.human_emeraldcutting,
            ),
            BoltTipCuttingRecipe(
                "Ruby bolt tips",
                63,
                objs.ruby,
                objs.ruby_bolt_tips,
                12,
                6.3,
                seqs.human_rubycutting,
            ),
            BoltTipCuttingRecipe(
                "Diamond bolt tips",
                65,
                objs.diamond,
                objs.diamond_bolt_tips,
                12,
                7.0,
                seqs.human_diamondcutting,
            ),
            BoltTipCuttingRecipe(
                "Dragonstone bolt tips",
                71,
                objs.dragonstone,
                objs.dragonstone_bolt_tips,
                12,
                8.2,
                seqs.human_dragonstonecutting,
                warnsBeforeCutting = true,
            ),
            BoltTipCuttingRecipe(
                "Onyx bolt tips",
                73,
                objs.onyx,
                objs.onyx_bolt_tips,
                24,
                9.4,
                seqs.human_onyxcutting,
            ),
        )

    private val byInput: Map<ObjType, BoltTipCuttingRecipe> = recipes.associateBy { it.input }

    public fun forInput(input: ObjType): BoltTipCuttingRecipe? = byInput[input]

    public fun find(input: ObjType, output: ObjType): BoltTipCuttingRecipe? =
        recipes.firstOrNull { it.input == input && it.output == output }
}
