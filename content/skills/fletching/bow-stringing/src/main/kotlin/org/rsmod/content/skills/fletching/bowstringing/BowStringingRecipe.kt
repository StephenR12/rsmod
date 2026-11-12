package org.rsmod.content.skills.fletching.bowstringing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class BowStringingRecipe(
    val name: String,
    val level: Int,
    val unstrung: ObjType,
    val output: ObjType,
    val xp: Double,
    val sequence: SeqType,
    val requiresCompOgreBowUnlock: Boolean = false,
)

public class BowStringingRecipeRegistry @Inject constructor() {
    public val recipes: List<BowStringingRecipe> =
        listOf(
            BowStringingRecipe(
                "Shortbow",
                5,
                objs.unstrung_shortbow,
                objs.shortbow,
                5.0,
                seqs.stringing_shortbow,
            ),
            BowStringingRecipe(
                "Longbow",
                10,
                objs.unstrung_longbow,
                objs.longbow,
                10.0,
                seqs.stringing_longbow,
            ),
            BowStringingRecipe(
                "Oak shortbow",
                20,
                objs.unstrung_oak_shortbow,
                objs.oak_shortbow,
                16.5,
                seqs.stringing_oak_shortbow,
            ),
            BowStringingRecipe(
                "Oak longbow",
                25,
                objs.unstrung_oak_longbow,
                objs.oak_longbow,
                25.0,
                seqs.stringing_oak_longbow,
            ),
            BowStringingRecipe(
                "Willow shortbow",
                35,
                objs.unstrung_willow_shortbow,
                objs.willow_shortbow,
                33.2,
                seqs.stringing_willow_shortbow,
            ),
            BowStringingRecipe(
                "Willow longbow",
                40,
                objs.unstrung_willow_longbow,
                objs.willow_longbow,
                41.5,
                seqs.stringing_willow_longbow,
            ),
            BowStringingRecipe(
                "Maple shortbow",
                50,
                objs.unstrung_maple_shortbow,
                objs.maple_shortbow,
                50.0,
                seqs.stringing_maple_shortbow,
            ),
            BowStringingRecipe(
                "Maple longbow",
                55,
                objs.unstrung_maple_longbow,
                objs.maple_longbow,
                58.2,
                seqs.stringing_maple_longbow,
            ),
            BowStringingRecipe(
                "Yew shortbow",
                65,
                objs.unstrung_yew_shortbow,
                objs.yew_shortbow,
                67.5,
                seqs.stringing_yew_shortbow,
            ),
            BowStringingRecipe(
                "Yew longbow",
                70,
                objs.unstrung_yew_longbow,
                objs.yew_longbow,
                75.0,
                seqs.stringing_yew_longbow,
            ),
            BowStringingRecipe(
                "Magic shortbow",
                80,
                objs.unstrung_magic_shortbow,
                objs.magic_shortbow,
                83.2,
                seqs.stringing_magic_shortbow,
            ),
            BowStringingRecipe(
                "Magic longbow",
                85,
                objs.unstrung_magic_longbow,
                objs.magic_longbow,
                91.5,
                seqs.stringing_magic_longbow,
            ),
            BowStringingRecipe(
                "Comp ogre bow",
                30,
                objs.unstrung_comp_ogre_bow,
                objs.comp_ogre_bow,
                45.0,
                seqs.human_ogre_fletching,
                requiresCompOgreBowUnlock = true,
            ),
        )

    private val byUnstrung: Map<ObjType, BowStringingRecipe> = recipes.associateBy { it.unstrung }

    public fun find(unstrung: ObjType): BowStringingRecipe? = byUnstrung[unstrung]
}
