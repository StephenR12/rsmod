package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class AmuletStringingRecipe(
    val name: String,
    val unstrung: ObjType,
    val output: ObjType,
    val members: Boolean,
    val xp: Double = 4.0,
)

public class AmuletStringingRecipeRegistry @Inject constructor() {
    public val recipes: List<AmuletStringingRecipe> =
        listOf(
            AmuletStringingRecipe(
                "Gold amulet",
                objs.unstrung_gold_amulet,
                objs.gold_amulet,
                members = false,
            ),
            AmuletStringingRecipe(
                "Sapphire amulet",
                objs.unstrung_sapphire_amulet,
                objs.sapphire_amulet,
                members = false,
            ),
            AmuletStringingRecipe(
                "Emerald amulet",
                objs.unstrung_emerald_amulet,
                objs.emerald_amulet,
                members = false,
            ),
            AmuletStringingRecipe(
                "Ruby amulet",
                objs.unstrung_ruby_amulet,
                objs.ruby_amulet,
                members = false,
            ),
            AmuletStringingRecipe(
                "Diamond amulet",
                objs.unstrung_diamond_amulet,
                objs.diamond_amulet,
                members = false,
            ),
            AmuletStringingRecipe(
                "Unblessed symbol",
                objs.unstrung_holy_symbol,
                objs.unblessed_symbol,
                members = false,
            ),
            AmuletStringingRecipe(
                "Opal amulet",
                objs.unstrung_opal_amulet,
                objs.opal_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Jade amulet",
                objs.unstrung_jade_amulet,
                objs.jade_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Topaz amulet",
                objs.unstrung_topaz_amulet,
                objs.topaz_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Dragonstone amulet",
                objs.unstrung_dragonstone_amulet,
                objs.dragonstone_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Onyx amulet",
                objs.unstrung_onyx_amulet,
                objs.onyx_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Zenyte amulet",
                objs.unstrung_zenyte_amulet,
                objs.zenyte_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Unpowered symbol",
                objs.unstrung_unholy_symbol,
                objs.unpowered_symbol,
                members = true,
            ),
            AmuletStringingRecipe(
                "M'speak amulet",
                objs.unstrung_m_speak_amulet,
                objs.m_speak_amulet,
                members = true,
            ),
            AmuletStringingRecipe(
                "Salve amulet",
                objs.salve_shard,
                objs.salve_amulet,
                members = true,
                xp = 0.0,
            ),
        )

    private val byUnstrung: Map<ObjType, AmuletStringingRecipe> =
        recipes.associateBy { it.unstrung }

    public fun forUnstrung(unstrung: ObjType): AmuletStringingRecipe? = byUnstrung[unstrung]
}
