package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.content.skills.magic.spell.utility.arena.ArenaItems
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class JewelleryEnchantRecipe(
    public val spell: ObjType,
    public val source: ObjType,
    public val output: ObjType,
    public val membersOnly: Boolean,
    public val ringSound: Boolean = false,
    public val gate: Gate = Gate.None,
) {
    public enum class Gate {
        None,
        Arena,
        DigsitePendant,
        NatureAmulet,
    }
}

/** Cache-233.4 item identities accepted by the seven standard jewellery enchantment spells. */
public class JewelleryEnchantRecipeRegistry @Inject constructor() {
    public val recipes: List<JewelleryEnchantRecipe> =
        listOf(
            // Lvl-1 Enchant: sapphire and opal, plus In Aid of the Myreque's Silvthrill rod.
            recipe(objs.spell_enchant_lvl_1, objs.sapphire_ring, objs.ring_of_recoil, true),
            recipe(objs.spell_enchant_lvl_1, objs.sapphire_necklace, objs.games_necklace_8, true),
            recipe(objs.spell_enchant_lvl_1, objs.sapphire_bracelet, objs.bracelet_of_clay, true),
            recipe(objs.spell_enchant_lvl_1, objs.sapphire_amulet, objs.amulet_of_magic, false),
            recipe(objs.spell_enchant_lvl_1, objs.opal_ring, objs.ring_of_pursuit, true),
            recipe(objs.spell_enchant_lvl_1, objs.opal_necklace, objs.dodgy_necklace, true),
            recipe(objs.spell_enchant_lvl_1, objs.opal_bracelet, objs.expeditious_bracelet, true),
            recipe(objs.spell_enchant_lvl_1, objs.opal_amulet, objs.amulet_of_bounty, true),
            recipe(
                objs.spell_enchant_lvl_1,
                objs.silvthrill_rod,
                objs.enchanted_silvthrill_rod,
                true,
            ),
            // Lvl-2 Enchant: emerald and jade, plus Farming and Myreque special targets.
            recipe(objs.spell_enchant_lvl_2, objs.emerald_ring, objs.ring_of_dueling_8, true),
            recipe(objs.spell_enchant_lvl_2, objs.emerald_necklace, objs.binding_necklace, true),
            recipe(
                objs.spell_enchant_lvl_2,
                objs.emerald_bracelet,
                objs.castle_wars_bracelet_3,
                true,
            ),
            recipe(objs.spell_enchant_lvl_2, objs.emerald_amulet, objs.amulet_of_defence, false),
            recipe(
                objs.spell_enchant_lvl_2,
                objs.pre_nature_amulet,
                objs.amulet_of_nature,
                true,
                gate = JewelleryEnchantRecipe.Gate.NatureAmulet,
            ),
            recipe(objs.spell_enchant_lvl_2, objs.jade_ring, objs.ring_of_returning_5, true),
            recipe(objs.spell_enchant_lvl_2, objs.jade_necklace, objs.necklace_of_passage_5, true),
            recipe(objs.spell_enchant_lvl_2, objs.jade_bracelet, objs.flamtaer_bracelet, true),
            recipe(objs.spell_enchant_lvl_2, objs.jade_amulet, objs.amulet_of_chemistry, true),
            recipe(
                objs.spell_enchant_lvl_2,
                objs.emerald_sickle_b,
                objs.enchanted_emerald_sickle_b,
                true,
            ),
            // Lvl-3 Enchant: ruby and red topaz, plus Sins of the Father's ruby sickle.
            recipe(objs.spell_enchant_lvl_3, objs.ruby_ring, objs.ring_of_forging, true),
            recipe(
                objs.spell_enchant_lvl_3,
                objs.ruby_necklace,
                objs.digsite_pendant_5,
                true,
                gate = JewelleryEnchantRecipe.Gate.DigsitePendant,
            ),
            recipe(objs.spell_enchant_lvl_3, objs.ruby_bracelet, objs.inoculation_bracelet, true),
            recipe(objs.spell_enchant_lvl_3, objs.ruby_amulet, objs.amulet_of_strength, false),
            recipe(objs.spell_enchant_lvl_3, objs.topaz_ring, objs.efaritays_aid, true),
            recipe(objs.spell_enchant_lvl_3, objs.topaz_necklace, objs.necklace_of_faith, true),
            recipe(objs.spell_enchant_lvl_3, objs.topaz_bracelet, objs.bracelet_of_slaughter, true),
            recipe(objs.spell_enchant_lvl_3, objs.topaz_amulet, objs.burning_amulet_5, true),
            recipe(
                objs.spell_enchant_lvl_3,
                objs.ruby_sickle_b,
                objs.enchanted_ruby_sickle_b,
                true,
            ),
            // Lvl-4 through Lvl-7 Enchant.
            recipe(objs.spell_enchant_lvl_4, objs.diamond_ring, objs.ring_of_life, true),
            recipe(objs.spell_enchant_lvl_4, objs.diamond_necklace, objs.phoenix_necklace, true),
            recipe(objs.spell_enchant_lvl_4, objs.diamond_bracelet, objs.abyssal_bracelet_5, true),
            recipe(objs.spell_enchant_lvl_4, objs.diamond_amulet, objs.amulet_of_power, false),
            recipe(objs.spell_enchant_lvl_5, objs.dragonstone_ring, objs.ring_of_wealth, true),
            recipe(
                objs.spell_enchant_lvl_5,
                objs.dragonstone_necklace,
                objs.skills_necklace_uncharged,
                true,
            ),
            recipe(
                objs.spell_enchant_lvl_5,
                objs.dragonstone_bracelet,
                objs.combat_bracelet_uncharged,
                true,
            ),
            recipe(
                objs.spell_enchant_lvl_5,
                objs.dragonstone_amulet,
                objs.amulet_of_glory_uncharged,
                true,
            ),
            recipe(objs.spell_enchant_lvl_6, objs.onyx_ring, objs.ring_of_stone, true),
            recipe(objs.spell_enchant_lvl_6, objs.onyx_necklace, objs.berserker_necklace, true),
            recipe(objs.spell_enchant_lvl_6, objs.onyx_bracelet, objs.regen_bracelet, true),
            recipe(objs.spell_enchant_lvl_6, objs.onyx_amulet, objs.amulet_of_fury, true),
            recipe(objs.spell_enchant_lvl_7, objs.zenyte_ring, objs.ring_of_suffering, true),
            recipe(objs.spell_enchant_lvl_7, objs.zenyte_necklace, objs.necklace_of_anguish, true),
            recipe(objs.spell_enchant_lvl_7, objs.zenyte_bracelet, objs.tormented_bracelet, true),
            recipe(objs.spell_enchant_lvl_7, objs.zenyte_amulet, objs.amulet_of_torture, true),
        )

    // Arena shapes share the ordinary spell and autocast handlers. Tablets accept only jewellery.
    private val arenaRecipes = listOf(objs.spell_enchant_lvl_1,objs.spell_enchant_lvl_2,objs.spell_enchant_lvl_3,objs.spell_enchant_lvl_4,objs.spell_enchant_lvl_5,objs.spell_enchant_lvl_6,objs.spell_enchant_lvl_7).flatMap { spell ->
        (ArenaItems.shapes + ArenaItems.stone).map { shape -> recipe(spell,shape,ArenaItems.orb,true,JewelleryEnchantRecipe.Gate.Arena) }
    }

    private val bySpellAndSource: Map<Long, JewelleryEnchantRecipe> =
        (recipes + arenaRecipes).associateBy { key(it.spell.id, it.source.id) }

    public fun find(spell: ObjType, sourceId: Int): JewelleryEnchantRecipe? =
        bySpellAndSource[key(spell.id, sourceId)]

    private companion object {
        fun recipe(
            spell: ObjType,
            source: ObjType,
            output: ObjType,
            membersOnly: Boolean,
            gate: JewelleryEnchantRecipe.Gate = JewelleryEnchantRecipe.Gate.None,
        ): JewelleryEnchantRecipe =
            JewelleryEnchantRecipe(
                spell,
                source,
                output,
                membersOnly,
                ringSound = source.id in ringSourceIds,
                gate = gate,
            )

        val ringSourceIds =
            setOf(
                objs.sapphire_ring.id,
                objs.opal_ring.id,
                objs.emerald_ring.id,
                objs.jade_ring.id,
                objs.ruby_ring.id,
                objs.topaz_ring.id,
                objs.diamond_ring.id,
                objs.dragonstone_ring.id,
                objs.onyx_ring.id,
                objs.zenyte_ring.id,
            )

        fun key(spell: Int, source: Int): Long =
            (spell.toLong() shl Int.SIZE_BITS) or (source.toLong() and 0xFFFF_FFFFL)
    }
}
