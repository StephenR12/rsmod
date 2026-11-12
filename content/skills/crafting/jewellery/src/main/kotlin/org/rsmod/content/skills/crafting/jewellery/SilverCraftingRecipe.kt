package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public data class SilverCraftingRecipe(
    val name: String,
    val component: ComponentType,
    val mould: ObjType,
    val ingredients: List<SilverCraftingIngredient>,
    val output: ObjType,
    val outputCount: Int,
    val level: Int,
    val xp: Double,
    val members: Boolean,
)

public data class SilverCraftingIngredient(
    val obj: ObjType,
    val count: Int = 1,
    val missingMessage: String,
)

public class SilverCraftingRecipeRegistry @Inject constructor() {
    public val recipes: List<SilverCraftingRecipe> =
        listOf(
            recipe(
                "Opal ring",
                components.silver_crafting_opal_ring,
                objs.ring_mould,
                objs.opal,
                objs.opal_ring,
                1,
                10.0,
            ),
            recipe(
                "Jade ring",
                components.silver_crafting_jade_ring,
                objs.ring_mould,
                objs.jade,
                objs.jade_ring,
                13,
                32.0,
            ),
            recipe(
                "Topaz ring",
                components.silver_crafting_topaz_ring,
                objs.ring_mould,
                objs.red_topaz,
                objs.topaz_ring,
                16,
                35.0,
            ),
            recipe(
                "Opal necklace",
                components.silver_crafting_opal_necklace,
                objs.necklace_mould,
                objs.opal,
                objs.opal_necklace,
                16,
                35.0,
            ),
            recipe(
                "Jade necklace",
                components.silver_crafting_jade_necklace,
                objs.necklace_mould,
                objs.jade,
                objs.jade_necklace,
                25,
                54.0,
            ),
            recipe(
                "Topaz necklace",
                components.silver_crafting_topaz_necklace,
                objs.necklace_mould,
                objs.red_topaz,
                objs.topaz_necklace,
                32,
                70.0,
            ),
            recipe(
                "Opal amulet (u)",
                components.silver_crafting_opal_amulet,
                objs.amulet_mould,
                objs.opal,
                objs.unstrung_opal_amulet,
                27,
                55.0,
            ),
            recipe(
                "Jade amulet (u)",
                components.silver_crafting_jade_amulet,
                objs.amulet_mould,
                objs.jade,
                objs.unstrung_jade_amulet,
                34,
                70.0,
            ),
            recipe(
                "Topaz amulet (u)",
                components.silver_crafting_topaz_amulet,
                objs.amulet_mould,
                objs.red_topaz,
                objs.unstrung_topaz_amulet,
                45,
                80.0,
            ),
            recipe(
                "Opal bracelet",
                components.silver_crafting_opal_bracelet,
                objs.bracelet_mould,
                objs.opal,
                objs.opal_bracelet,
                22,
                45.0,
            ),
            recipe(
                "Jade bracelet",
                components.silver_crafting_jade_bracelet,
                objs.bracelet_mould,
                objs.jade,
                objs.jade_bracelet,
                29,
                60.0,
            ),
            recipe(
                "Topaz bracelet",
                components.silver_crafting_topaz_bracelet,
                objs.bracelet_mould,
                objs.red_topaz,
                objs.topaz_bracelet,
                38,
                75.0,
            ),
            recipe(
                "Unstrung symbol",
                components.silver_crafting_holy_symbol,
                objs.holy_symbol_mould,
                null,
                objs.unstrung_holy_symbol,
                16,
                50.0,
                members = false,
            ),
            recipe(
                "Unstrung emblem",
                components.silver_crafting_unholy_symbol,
                objs.unholy_symbol_mould,
                null,
                objs.unstrung_unholy_symbol,
                17,
                50.0,
            ),
            recipe(
                "Silver sickle",
                components.silver_crafting_sickle,
                objs.sickle_mould,
                null,
                objs.silver_sickle,
                18,
                50.0,
            ),
            specialRecipe(
                "Conductor",
                components.silver_crafting_lightning_rod,
                objs.conductor_mould,
                objs.conductor,
                20,
                50.0,
                materials = listOf(silverBar()),
            ),
            recipe(
                "Silver bolts (unf)",
                components.silver_crafting_crossbow_bolt,
                objs.silver_bolt_mould,
                null,
                objs.silver_bolts_unf,
                21,
                50.0,
                outputCount = 10,
            ),
            recipe(
                "Tiara",
                components.silver_crafting_tiara,
                objs.tiara_mould,
                null,
                objs.tiara,
                23,
                52.5,
                members = false,
            ),
            specialRecipe(
                "Silvthrill rod",
                components.silver_crafting_ivandis,
                objs.rod_mould,
                objs.silvthrill_rod,
                25,
                55.0,
                materials =
                    listOf(
                        silverBar(),
                        ingredient(objs.mithril_bar, "You need a mithril bar to make this item."),
                        cutGem(objs.sapphire, "sapphire"),
                    ),
            ),
            specialRecipe(
                "Demonic sigil",
                components.silver_crafting_agrith_sigil,
                objs.demonic_sigil_mould,
                objs.demonic_sigil,
                30,
                50.0,
                materials = listOf(silverBar()),
            ),
        )

    public fun forComponent(component: ComponentType): SilverCraftingRecipe? =
        recipes.firstOrNull { it.component.isType(component) }

    public fun forOutput(output: ObjType): SilverCraftingRecipe? =
        recipes.firstOrNull { it.output.id == output.id }

    private fun recipe(
        name: String,
        component: ComponentType,
        mould: ObjType,
        gem: ObjType?,
        output: ObjType,
        level: Int,
        xp: Double,
        members: Boolean = true,
        outputCount: Int = 1,
    ): SilverCraftingRecipe =
        SilverCraftingRecipe(
            name,
            component,
            mould,
            buildList {
                add(silverBar())
                gem?.let {
                    add(cutGem(it, if (it.id == objs.red_topaz.id) "red topaz" else nameOf(it)))
                }
            },
            output,
            outputCount,
            level,
            xp,
            members,
        )

    private fun specialRecipe(
        name: String,
        component: ComponentType,
        mould: ObjType,
        output: ObjType,
        level: Int,
        xp: Double,
        materials: List<SilverCraftingIngredient>,
    ): SilverCraftingRecipe =
        SilverCraftingRecipe(
            name,
            component,
            mould,
            materials,
            output,
            1,
            level,
            xp,
            members = true,
        )

    private fun silverBar(): SilverCraftingIngredient =
        ingredient(objs.silver_bar, "You need a silver bar to make this item.")

    private fun cutGem(obj: ObjType, name: String): SilverCraftingIngredient =
        ingredient(obj, "You need a cut $name to make this item.")

    private fun ingredient(obj: ObjType, message: String): SilverCraftingIngredient =
        SilverCraftingIngredient(obj, missingMessage = message)

    private fun nameOf(obj: ObjType): String =
        when (obj.id) {
            objs.opal.id -> "opal"
            objs.jade.id -> "jade"
            objs.sapphire.id -> "sapphire"
            else -> error("Missing silver-crafting ingredient name for obj=${obj.id}")
        }
}
