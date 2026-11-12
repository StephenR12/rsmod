package org.rsmod.content.skills.herblore

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.queue.QueueReferences

public typealias herblore_queues = HerbloreQueues

public object HerbloreQueues : QueueReferences() {
    public val herbCleaning = find("herblore_herb_cleaning")
    public val ingredientCrushing = find("herblore_ingredient_crushing")
    public val lavaScaleCrushing = find("herblore_lava_scale_crushing")
    public val gorakClawCrushing = find("herblore_gorak_claw_crushing")
    public val suqahToothCrushing = find("herblore_suqah_tooth_crushing")
    public val karambwanjiPasteCrushing = find("herblore_karambwanji_paste_crushing")
    public val blamishSnailSlime = find("herblore_blamish_snail_slime")
    public val oilyFishingRod = find("herblore_oily_fishing_rod")
    public val unfinishedPotions = find("herblore_unfinished_potions")
    public val finishedPotions = find("herblore_finished_potions")
    public val surgePotionRestore = find("herblore_surge_potion_restore")
    public val forgottenBrewUpgrade = find("herblore_forgotten_brew_upgrade")
    public val barbarianMix = find("herblore_barbarian_mix")
    public val swampTar = find("herblore_swamp_tar")
    public val coconutMilk = find("herblore_coconut_milk")
    public val doseScaledPotions = find("herblore_dose_scaled_potions")
}

internal fun ProtectedAccess.clearHerbloreQueues() {
    clearWeakQueue(herblore_queues.herbCleaning)
    clearWeakQueue(herblore_queues.ingredientCrushing)
    clearWeakQueue(herblore_queues.lavaScaleCrushing)
    clearWeakQueue(herblore_queues.gorakClawCrushing)
    clearWeakQueue(herblore_queues.suqahToothCrushing)
    clearWeakQueue(herblore_queues.karambwanjiPasteCrushing)
    clearWeakQueue(herblore_queues.blamishSnailSlime)
    clearWeakQueue(herblore_queues.oilyFishingRod)
    clearWeakQueue(herblore_queues.unfinishedPotions)
    clearWeakQueue(herblore_queues.finishedPotions)
    clearWeakQueue(herblore_queues.forgottenBrewUpgrade)
    clearWeakQueue(herblore_queues.barbarianMix)
    clearWeakQueue(herblore_queues.swampTar)
    clearWeakQueue(herblore_queues.coconutMilk)
    clearWeakQueue(herblore_queues.doseScaledPotions)
}
