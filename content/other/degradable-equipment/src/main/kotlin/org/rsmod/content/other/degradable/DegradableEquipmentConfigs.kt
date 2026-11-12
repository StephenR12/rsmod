package org.rsmod.content.other.degradable

import org.rsmod.api.type.refs.timer.TimerReferences

internal typealias degradation_timers = DegradableEquipmentTimers

internal object DegradableEquipmentTimers : TimerReferences() {
    val combat = find("degradable_equipment_combat")
}
