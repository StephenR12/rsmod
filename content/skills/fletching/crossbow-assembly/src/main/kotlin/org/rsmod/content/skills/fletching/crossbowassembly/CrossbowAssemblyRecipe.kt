package org.rsmod.content.skills.fletching.crossbowassembly

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class CrossbowAssemblyRecipe(
    val name: String,
    val level: Int,
    val stock: ObjType,
    val limbs: ObjType,
    val unstrung: ObjType,
    val output: ObjType,
    val limbXp: Double,
    val stringXp: Double,
    val limbSequence: SeqType,
    val stringSequence: SeqType,
)

public class CrossbowAssemblyRecipeRegistry @Inject constructor() {
    public val recipes: List<CrossbowAssemblyRecipe> =
        listOf(
            CrossbowAssemblyRecipe(
                "Bronze crossbow",
                9,
                objs.wooden_stock,
                objs.bronze_limbs,
                objs.unstrung_bronze_crossbow,
                objs.bronze_crossbow,
                12.0,
                6.0,
                seqs.fletching_bronze_crossbow,
                seqs.stringing_bronze_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Blurite crossbow",
                24,
                objs.oak_stock,
                objs.blurite_limbs,
                objs.unstrung_blurite_crossbow,
                objs.blurite_crossbow,
                32.0,
                16.0,
                seqs.fletching_blurite_crossbow,
                seqs.stringing_blurite_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Iron crossbow",
                39,
                objs.willow_stock,
                objs.iron_limbs,
                objs.unstrung_iron_crossbow,
                objs.iron_crossbow,
                44.0,
                22.0,
                seqs.fletching_iron_crossbow,
                seqs.stringing_iron_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Steel crossbow",
                46,
                objs.teak_stock,
                objs.steel_limbs,
                objs.unstrung_steel_crossbow,
                objs.steel_crossbow,
                54.0,
                27.0,
                seqs.fletching_steel_crossbow,
                seqs.stringing_steel_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Mithril crossbow",
                54,
                objs.maple_stock,
                objs.mithril_limbs,
                objs.unstrung_mithril_crossbow,
                objs.mithril_crossbow,
                64.0,
                32.0,
                seqs.fletching_mithril_crossbow,
                seqs.stringing_mithril_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Adamant crossbow",
                61,
                objs.mahogany_stock,
                objs.adamantite_limbs,
                objs.unstrung_adamant_crossbow,
                objs.adamant_crossbow,
                82.0,
                41.0,
                seqs.fletching_adamant_crossbow,
                seqs.stringing_adamant_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Rune crossbow",
                69,
                objs.yew_stock,
                objs.runite_limbs,
                objs.unstrung_rune_crossbow,
                objs.rune_crossbow,
                100.0,
                50.0,
                seqs.fletching_rune_crossbow,
                seqs.stringing_rune_crossbow,
            ),
            CrossbowAssemblyRecipe(
                "Dragon crossbow",
                78,
                objs.magic_stock,
                objs.dragon_limbs,
                objs.unstrung_dragon_crossbow,
                objs.dragon_crossbow,
                135.0,
                70.0,
                seqs.fletching_dragon_crossbow,
                seqs.stringing_dragon_crossbow,
            ),
        )

    private val byUnstrung: Map<ObjType, CrossbowAssemblyRecipe> =
        recipes.associateBy { it.unstrung }

    public fun find(unstrung: ObjType): CrossbowAssemblyRecipe? = byUnstrung[unstrung]
}

public enum class CrossbowAssemblyStage {
    Limbs,
    String,
}
