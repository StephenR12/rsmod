package org.rsmod.content.skills.fletching.toxicblowpipe

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.queue.QueueReferences

public typealias fletching_queues = FletchingQueues

public object FletchingQueues : QueueReferences() {
    val logCutting = find("log_fletching_batch")
    val bowStringing = find("bow_stringing_batch")
    val ammunition = find("ammunition_fletching_batch")
    val crossbowAssembly = find("crossbow_assembly_batch")
    val boltTipCutting = find("bolt_tip_cutting_batch")
    val battlestaffFletching = find("battlestaff_fletching_batch")
    val wolfboneArrowtips = find("wolfbone_arrowtip_cutting")
    val ogreArrowShafts = find("ogre_arrow_shaft_fletching")
}

public fun ProtectedAccess.clearFletchingQueues() {
    clearWeakQueue(fletching_queues.logCutting)
    clearWeakQueue(fletching_queues.bowStringing)
    clearWeakQueue(fletching_queues.ammunition)
    clearWeakQueue(fletching_queues.crossbowAssembly)
    clearWeakQueue(fletching_queues.boltTipCutting)
    clearWeakQueue(fletching_queues.battlestaffFletching)
    clearWeakQueue(fletching_queues.wolfboneArrowtips)
    clearWeakQueue(fletching_queues.ogreArrowShafts)
}
