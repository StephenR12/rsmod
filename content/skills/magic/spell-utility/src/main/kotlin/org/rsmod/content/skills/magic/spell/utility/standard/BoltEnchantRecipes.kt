package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public enum class BoltEnchantRecipe(
    public val spell: ObjType,
    public val button: ComponentType,
    public val regularSource: ObjType,
    public val regularOutput: ObjType,
    public val dragonSource: ObjType,
    public val dragonOutput: ObjType,
) {
    Opal(
        objs.spell_enchant_bolt_opal,
        components.skillmulti_a,
        objs.opal_bolts,
        objs.opal_bolts_e,
        objs.dragon_opal_bolts,
        objs.dragon_opal_bolts_e,
    ),
    Sapphire(
        objs.spell_enchant_bolt_sapphire,
        components.skillmulti_b,
        objs.sapphire_bolts,
        objs.sapphire_bolts_e,
        objs.dragon_sapphire_bolts,
        objs.dragon_sapphire_bolts_e,
    ),
    Jade(
        objs.spell_enchant_bolt_jade,
        components.skillmulti_c,
        objs.jade_bolts,
        objs.jade_bolts_e,
        objs.dragon_jade_bolts,
        objs.dragon_jade_bolts_e,
    ),
    Pearl(
        objs.spell_enchant_bolt_pearl,
        components.skillmulti_d,
        objs.pearl_bolts,
        objs.pearl_bolts_e,
        objs.dragon_pearl_bolts,
        objs.dragon_pearl_bolts_e,
    ),
    Emerald(
        objs.spell_enchant_bolt_emerald,
        components.skillmulti_e,
        objs.emerald_bolts,
        objs.emerald_bolts_e,
        objs.dragon_emerald_bolts,
        objs.dragon_emerald_bolts_e,
    ),
    RedTopaz(
        objs.spell_enchant_bolt_red_topaz,
        components.skillmulti_f,
        objs.red_topaz_bolts,
        objs.red_topaz_bolts_e,
        objs.dragon_red_topaz_bolts,
        objs.dragon_red_topaz_bolts_e,
    ),
    Ruby(
        objs.spell_enchant_bolt_ruby,
        components.skillmulti_g,
        objs.ruby_bolts,
        objs.ruby_bolts_e,
        objs.dragon_ruby_bolts,
        objs.dragon_ruby_bolts_e,
    ),
    Diamond(
        objs.spell_enchant_bolt_diamond,
        components.skillmulti_h,
        objs.diamond_bolts,
        objs.diamond_bolts_e,
        objs.dragon_diamond_bolts,
        objs.dragon_diamond_bolts_e,
    ),
    Dragonstone(
        objs.spell_enchant_bolt_dragonstone,
        components.skillmulti_i,
        objs.dragonstone_bolts,
        objs.dragonstone_bolts_e,
        objs.dragon_dragonstone_bolts,
        objs.dragon_dragonstone_bolts_e,
    ),
    Onyx(
        objs.spell_enchant_bolt_onyx,
        components.skillmulti_j,
        objs.onyx_bolts,
        objs.onyx_bolts_e,
        objs.dragon_onyx_bolts,
        objs.dragon_onyx_bolts_e,
    );

    public companion object {
        public fun forButton(component: ComponentType): BoltEnchantRecipe? =
            entries.firstOrNull { it.button.isType(component) }

        public fun forSpell(spell: ObjType): BoltEnchantRecipe? =
            entries.firstOrNull { it.spell.id == spell.id }

        public val productIcons: List<ObjType> by lazy {
            entries.map(BoltEnchantRecipe::regularOutput)
        }

        public val dragonProductIcons: List<ObjType> by lazy {
            entries.map(BoltEnchantRecipe::dragonOutput)
        }
    }
}
