package org.rsmod.content.skills.magic.spell.attacks.standard.config

import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences

internal typealias teleblock_queues = TeleBlockQueues

object TeleBlockQueues : QueueReferences() {
    val impact = find("magic_teleblock_impact")
}

internal typealias teleblock_timers = TeleBlockTimers

object TeleBlockTimers : TimerReferences() {
    val status = find("magic_teleblock_status")
}
