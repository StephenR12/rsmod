package org.rsmod.content.skills.mining

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.stats.xpmod.StatXpMod
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.module.PluginModule

public object ProspectorOutfits : ObjReferences() {
    val hat = find("motherlode_reward_hat")
    val top = find("motherlode_reward_top")
    val legs = find("motherlode_reward_legs")
    val boots = find("motherlode_reward_boots")
    val goldenHat = find("motherlode_reward_hat_gold")
    val goldenTop = find("motherlode_reward_top_gold")
    val goldenLegs = find("motherlode_reward_legs_gold")
    val goldenBoots = find("motherlode_reward_boots_gold")
}

/** Prospector kit oldid=14989966 (2025-09-22): mixed colours and Varrock armour 4 retain set bonus. */
public class MiningOutfitXp : StatXpMod(stats.mining) {
    override fun Player.modifier(): Double {
        if (!members) return 0.0
        val pieces = listOf(
            listOf(ProspectorOutfits.hat, ProspectorOutfits.goldenHat) to 4,
            listOf(ProspectorOutfits.top, ProspectorOutfits.goldenTop, objs.diary_varrock_armour_4) to 8,
            listOf(ProspectorOutfits.legs, ProspectorOutfits.goldenLegs) to 6,
            listOf(ProspectorOutfits.boots, ProspectorOutfits.goldenBoots) to 2,
        )
        var count = 0
        var tenthsOfPercent = 0
        for ((alternatives, bonus) in pieces) {
            if (alternatives.any { worn.contains(it) }) { count++; tenthsOfPercent += bonus }
        }
        if (count == 4) tenthsOfPercent += 5
        return tenthsOfPercent / 1000.0
    }
}

public class MiningOutfitModule : PluginModule() {
    override fun bind() { addSetBinding<XpMod>(MiningOutfitXp::class.java) }
}
