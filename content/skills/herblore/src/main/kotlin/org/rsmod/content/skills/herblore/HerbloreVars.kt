package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player

public typealias herblore_varps = HerbloreVarps

public object HerbloreVarps : VarpReferences() {
    public val chemistryAmuletUses = find("herblore_chemistry_amulet_uses")
}

internal object HerbloreVarpBuilder : VarpBuilder() {
    init {
        build("herblore_chemistry_amulet_uses") { permanent = true }
    }
}

public class ChemistryAmuletRepository @Inject constructor() {
    private var Player.uses by intVarp(herblore_varps.chemistryAmuletUses)

    public fun remaining(player: Player): Int = MAX_CHARGES - player.uses

    public fun consume(player: Player): Boolean {
        require(player.uses in 0 until MAX_CHARGES)
        player.uses++
        if (player.uses < MAX_CHARGES) {
            return false
        }
        player.uses = 0
        return true
    }

    public fun reset(player: Player) {
        player.uses = 0
    }

    public companion object {
        public const val MAX_CHARGES: Int = 5
    }
}
