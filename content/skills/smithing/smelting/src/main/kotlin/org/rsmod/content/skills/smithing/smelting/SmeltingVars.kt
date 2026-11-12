package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player

public typealias smelting_varps = SmeltingVarps

public object SmeltingVarps : VarpReferences() {
    val ring_of_forging_uses = find("ring_of_forging_uses")
}

internal object SmeltingVarpBuilder : VarpBuilder() {
    init {
        build("ring_of_forging_uses") { permanent = true }
    }
}

public class RingOfForgingRepository @Inject constructor() {
    private var Player.uses by intVarp(smelting_varps.ring_of_forging_uses)

    public fun remaining(player: Player): Int = MAX_CHARGES - player.uses

    public fun consume(player: Player, count: Int): Boolean {
        require(count in 1..remaining(player))
        player.uses += count
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
        public const val MAX_CHARGES: Int = 140
    }
}
