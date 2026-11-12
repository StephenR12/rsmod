package org.rsmod.api.specials.energy

import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy {
    private var Player.specialEnergy by intVarp(varps.sa_energy)

    public fun hasSpecialEnergy(player: Player, energyInHundreds: Int): Boolean {
        return player.specialEnergy >= energyInHundreds
    }

    public fun currentSpecialEnergy(player: Player): Int = player.specialEnergy

    public fun takeSpecialEnergy(player: Player, energyInHundreds: Int) {
        require(player.specialEnergy >= energyInHundreds) {
            "Not enough special energy to take. Use `hasSpecialEnergy` first for validation."
        }
        player.specialEnergy -= energyInHundreds
    }

    /** Restores [energyInHundreds], capped at a full special-attack bar. */
    public fun restoreSpecialEnergy(player: Player, energyInHundreds: Int) {
        require(energyInHundreds >= 0) { "Energy to restore must not be negative." }
        player.specialEnergy = (player.specialEnergy + energyInHundreds).coerceAtMost(MAX_ENERGY)
    }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000
    }
}
