package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpLocT
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class OrbChargingSpellScript @Inject constructor(private val charging: OrbChargingSpell) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLocT(components.magic_spellbook_charge_water_orb) {
            charging.castManual(this, objs.spell_chargeorb_water, it.vis)
        }
        onOpLocT(components.magic_spellbook_charge_earth_orb) {
            charging.castManual(this, objs.spell_chargeorb_earth, it.vis)
        }
        onOpLocT(components.magic_spellbook_charge_fire_orb) {
            charging.castManual(this, objs.spell_chargeorb_fire, it.vis)
        }
        onOpLocT(components.magic_spellbook_charge_air_orb) {
            charging.castManual(this, objs.spell_chargeorb_air, it.vis)
        }
        onPlayerQueueWithArgs<OrbChargingRequest>(orb_charging_queues.manual) {
            charging.castQueued(this, it.args)
        }
        onPlayerQueueWithArgs<OrbChargingRequest>(orb_charging_queues.auto) {
            charging.castQueued(this, it.args)
        }
    }
}
