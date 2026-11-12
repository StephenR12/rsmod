package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType

public data class OrbChargingRecipe(
    val spell: ObjType,
    val component: ComponentType,
    val obelisk: LocType,
    val output: ObjType,
    val spotanim: SpotanimType,
    val sound: SynthType,
    val targetName: String,
)

public class OrbChargingRecipeRegistry @Inject constructor() {
    public val recipes: List<OrbChargingRecipe> =
        listOf(
            OrbChargingRecipe(
                spell = objs.spell_chargeorb_water,
                component = components.magic_spellbook_charge_water_orb,
                obelisk = orb_charging_locs.water,
                output = objs.water_orb,
                spotanim = spotanims.chargewaterorb_casting,
                sound = synths.charge_water_orb,
                targetName = "Water",
            ),
            OrbChargingRecipe(
                spell = objs.spell_chargeorb_earth,
                component = components.magic_spellbook_charge_earth_orb,
                obelisk = orb_charging_locs.earth,
                output = objs.earth_orb,
                spotanim = spotanims.chargeearthorb_casting,
                sound = synths.charge_earth_orb,
                targetName = "Earth",
            ),
            OrbChargingRecipe(
                spell = objs.spell_chargeorb_fire,
                component = components.magic_spellbook_charge_fire_orb,
                obelisk = orb_charging_locs.fire,
                output = objs.fire_orb,
                spotanim = spotanims.chargefireorb_casting,
                sound = synths.charge_fire_orb,
                targetName = "Fire",
            ),
            OrbChargingRecipe(
                spell = objs.spell_chargeorb_air,
                component = components.magic_spellbook_charge_air_orb,
                obelisk = orb_charging_locs.air,
                output = objs.air_orb,
                spotanim = spotanims.chargewindorb_casting,
                sound = synths.charge_air_orb,
                targetName = "Air",
            ),
        )

    private val bySpell: Map<Int, OrbChargingRecipe> = recipes.associateBy { it.spell.id }

    public operator fun get(spell: ObjType): OrbChargingRecipe? = bySpell[spell.id]
}
