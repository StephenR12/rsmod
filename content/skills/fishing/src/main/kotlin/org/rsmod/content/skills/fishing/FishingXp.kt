package org.rsmod.content.skills.fishing

import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player

public object FishingOutfits : ObjReferences() {
    val hat = find("trawler_reward_hat")
    val top = find("trawler_reward_top")
    val legs = find("trawler_reward_legs")
    val boots = find("trawler_reward_boots")
    val spiritHat = find("spirit_angler_hat")
    val spiritTop = find("spirit_angler_top")
    val spiritLegs = find("spirit_angler_legs")
    val spiritBoots = find("spirit_angler_boots")
}

/** Angler's outfit oldid14941834 (2025-07-22): mixed normal/spirit pieces retain the full-set bonus. */
public object FishingXp {
    public fun advance(access: ProtectedAccess, xp: Double) { access.statAdvance(stats.fishing, xp * multiplier(access.player)) }
    public fun multiplier(player: Player): Double {
        if (!player.members) return 1.0
        val pieces = listOf(listOf(FishingOutfits.hat, FishingOutfits.spiritHat) to 4, listOf(FishingOutfits.top, FishingOutfits.spiritTop) to 8, listOf(FishingOutfits.legs, FishingOutfits.spiritLegs) to 6, listOf(FishingOutfits.boots, FishingOutfits.spiritBoots) to 2)
        var count = 0
        var bonus = 0
        for ((types, value) in pieces) if (types.any { it in player.worn }) { count++; bonus += value }
        if (count == 4) bonus += 5
        return 1.0 + bonus / 1000.0
    }
}
