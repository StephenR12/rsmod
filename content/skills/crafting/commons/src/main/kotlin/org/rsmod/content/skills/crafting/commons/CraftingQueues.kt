package org.rsmod.content.skills.crafting.commons

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.queue.QueueReferences

public typealias crafting_queues = CraftingQueues

public object CraftingQueues : QueueReferences() {
    val gemCutting = find("gem_cutting_batch")
    val amuletStringing = find("amulet_stringing_batch")
    val glassblowing = find("glassblowing_batch")
    val spinning = find("spinning_batch")
    val potteryShaping = find("pottery_shaping_batch")
    val potteryFiring = find("pottery_firing_batch")
    val leatherworking = find("leatherworking_batch")
    val battlestaffCrafting = find("battlestaff_crafting_batch")
    val weaving = find("weaving_batch")
    val leatherAddonCrafting = find("leather_addon_crafting")
    val leatherShieldCrafting = find("leather_shield_crafting")
    val specialistLeatherworking = find("specialist_leatherworking_batch")
    val hunterPouchCrafting = find("hunter_pouch_crafting_batch")
    val amethystCutting = find("amethyst_cutting_batch")
    val wolfboneArrowtips = find("wolfbone_arrowtip_cutting")
    val birdhouseCrafting = find("birdhouse_crafting_batch")
}

public fun ProtectedAccess.clearCraftingQueues() {
    clearWeakQueue(crafting_queues.gemCutting)
    clearWeakQueue(crafting_queues.amuletStringing)
    clearWeakQueue(crafting_queues.glassblowing)
    clearWeakQueue(crafting_queues.spinning)
    clearWeakQueue(crafting_queues.potteryShaping)
    clearWeakQueue(crafting_queues.potteryFiring)
    clearWeakQueue(crafting_queues.leatherworking)
    clearWeakQueue(crafting_queues.battlestaffCrafting)
    clearWeakQueue(crafting_queues.weaving)
    clearWeakQueue(crafting_queues.leatherAddonCrafting)
    clearWeakQueue(crafting_queues.leatherShieldCrafting)
    clearWeakQueue(crafting_queues.specialistLeatherworking)
    clearWeakQueue(crafting_queues.hunterPouchCrafting)
    clearWeakQueue(crafting_queues.amethystCutting)
    clearWeakQueue(crafting_queues.wolfboneArrowtips)
    clearWeakQueue(crafting_queues.birdhouseCrafting)
}
