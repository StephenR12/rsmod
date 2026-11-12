package org.rsmod.content.skills.crafting.leathershields

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class LeatherShieldRecipe(
    val name: String,
    val level: Int,
    val leather: ObjType,
    val base: ObjType,
    val nails: ObjType,
    val output: ObjType,
    val animation: SeqType,
    val xp: Double,
)

public class LeatherShieldRecipeRegistry @Inject constructor() {
    public val recipes: List<LeatherShieldRecipe> =
        listOf(
            LeatherShieldRecipe(
                "Hard leather shield",
                41,
                objs.hard_leather,
                objs.oak_shield,
                objs.bronze_nails,
                objs.hard_leather_shield,
                seqs.human_shield_crafting_leather,
                70.0,
            ),
            LeatherShieldRecipe(
                "Snakeskin shield",
                56,
                objs.snakeskin,
                objs.willow_shield,
                objs.iron_nails,
                objs.snakeskin_shield,
                seqs.human_shield_crafting_snakeskin,
                100.0,
            ),
            LeatherShieldRecipe(
                "Green d'hide shield",
                62,
                objs.green_dragon_leather,
                objs.maple_shield,
                objs.steel_nails,
                objs.green_dhide_shield,
                seqs.human_shield_crafting_green_dhide,
                124.0,
            ),
            LeatherShieldRecipe(
                "Blue d'hide shield",
                69,
                objs.blue_dragon_leather,
                objs.yew_shield,
                objs.mithril_nails,
                objs.blue_dhide_shield,
                seqs.human_shield_crafting_blue_dhide,
                140.0,
            ),
            LeatherShieldRecipe(
                "Red d'hide shield",
                76,
                objs.red_dragon_leather,
                objs.magic_shield,
                objs.adamant_nails,
                objs.red_dhide_shield,
                seqs.human_shield_crafting_red_dhide,
                156.0,
            ),
            LeatherShieldRecipe(
                "Black d'hide shield",
                83,
                objs.black_dragon_leather,
                objs.redwood_shield,
                objs.rune_nails,
                objs.black_dhide_shield,
                seqs.human_shield_crafting_black_dhide,
                172.0,
            ),
        )

    private val byOutput = recipes.associateBy(LeatherShieldRecipe::output)

    public fun forOutput(output: ObjType): LeatherShieldRecipe? = byOutput[output]
}
