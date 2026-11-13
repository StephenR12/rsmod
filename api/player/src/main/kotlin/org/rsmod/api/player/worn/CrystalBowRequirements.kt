package org.rsmod.api.player.worn

import org.rsmod.api.type.refs.varp.VarpReferences

internal object CrystalBowRequirements : VarpReferences() {
    val roving = find("roving_elves_quest")
    val bows = setOf(23983, 23985, 24123)
}
