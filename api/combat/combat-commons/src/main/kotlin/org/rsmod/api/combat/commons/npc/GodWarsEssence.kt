package org.rsmod.api.combat.commons.npc

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player

internal object GodWarsRewards : VarBitReferences() {
    val hard = find("ca_tier_status_hard")
    val elite = find("ca_tier_status_elite")
    val master = find("ca_tier_status_master")
    val grandmaster = find("ca_tier_status_grandmaster")
}

/** Cost charged by the generals' chamber doors and the Ancient Prison's respite entrance. */
public fun Player.godWarsEssenceCost(): Int = when {
    vars[GodWarsRewards.grandmaster] >= 2 -> 15
    vars[GodWarsRewards.master] >= 2 -> 25
    vars[GodWarsRewards.elite] >= 2 -> 30
    vars[GodWarsRewards.hard] >= 2 -> 35
    else -> 40
}
