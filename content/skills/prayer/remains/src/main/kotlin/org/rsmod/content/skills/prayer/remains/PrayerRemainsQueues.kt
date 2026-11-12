package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.type.refs.queue.QueueReferences

public typealias prayer_remains_queues = PrayerRemainsQueues

public object PrayerRemainsQueues : QueueReferences() {
    val boneCooldown = find("prayer_bone_burial_cooldown")
    val ashCooldown = find("prayer_demonic_ash_scatter_cooldown")
    val altarOffering = find("prayer_altar_offering")
    val altarManualCooldown = find("prayer_altar_manual_cooldown")
}
