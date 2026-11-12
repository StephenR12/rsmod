package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.game.loc.BoundLocInfo

public class PrayerElidinisStatuetteRestoration
@Inject
constructor(private val altarRestoration: PrayerAltarRestoration, private val realm: Realm) {
    public fun restore(access: ProtectedAccess, statuette: BoundLocInfo) {
        with(access) {
            if (vars[varbits.elidquest] < QUEST_COMPLETE) {
                return
            }

            altarRestoration.restore(this, statuette)

            val deadman = realm.config.hasWorldFlag(WorldFlag.Deadman)
            val pvp = realm.config.hasWorldFlag(WorldFlag.Pvp)
            if (deadman) {
                healHitpointsToBase()
            } else if (!pvp) {
                // The lost Hitpoints are restored before the visible +7 boost is applied.
                healHitpointsToBase()
                statBoost(stats.hitpoints, constant = HITPOINTS_BOOST, percent = 0)
            }
            player.runEnergy = constants.run_max_energy
            UpdateRun.energy(player, player.runEnergy)
            vars[varps.sa_energy] = constants.sa_max_energy

            // Cache/RuneLite poison state. Both backing values are cleared for poison and venom.
            vars[varps.poison] = 0
            vars[varps.poison_2] = 0
            vars[varbits.poison_type] = 0
        }
    }

    private fun ProtectedAccess.healHitpointsToBase() {
        if (stat(stats.hitpoints) < statBase(stats.hitpoints)) {
            statHeal(stats.hitpoints, constant = 0, percent = 100)
        }
    }

    public companion object {
        public const val QUEST_COMPLETE: Int = 60
        public const val HITPOINTS_BOOST: Int = 7
    }
}
