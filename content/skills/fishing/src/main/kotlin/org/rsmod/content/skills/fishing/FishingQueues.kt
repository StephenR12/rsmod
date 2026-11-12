package org.rsmod.content.skills.fishing

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.refs.queue.QueueReferences

public typealias fishing_queues = FishingQueues

public object FishingQueues : QueueReferences() {
    public val leapingFishGutting = find("fishing_leaping_fish_gutting")
    public val sacredEelDissection = find("fishing_sacred_eel_dissection")
}

public fun ProtectedAccess.clearLeapingFishGuttingQueue() {
    clearWeakQueue(fishing_queues.leapingFishGutting)
}

public fun ProtectedAccess.clearSacredEelDissectionQueue() {
    clearWeakQueue(fishing_queues.sacredEelDissection)
}
