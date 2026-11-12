package org.rsmod.content.skills.magic.spell.attacks.standard.config

import org.rsmod.api.type.refs.queue.QueueReferences

internal typealias binding_queues = BindingQueues

object BindingQueues : QueueReferences() {
    val impact = find("magic_binding_impact")
    val warning = find("magic_binding_warning")
}
