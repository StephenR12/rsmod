package org.rsmod.api.player.minigames

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player

public object BarbarianAssaultBits : VarBitReferences() {
    public val tutorial: org.rsmod.game.type.varbit.VarBitType = find("barbassault_arenanewb")
}

/** Cache interface 496 has ten chapters. Their buttons turn green when progress exceeds the chapter. */
public fun Player.completedBarbarianAssaultTutorial(): Boolean =
    vars[BarbarianAssaultBits.tutorial] > 10

internal object AssaultActivityVar : org.rsmod.api.type.refs.varp.VarpReferences() {
    val playing = find("assault_playing")
}

public fun Player.inBarbarianAssault(): Boolean = vars[AssaultActivityVar.playing] != 0
