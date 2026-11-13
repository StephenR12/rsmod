package org.rsmod.api.net.rsprot.player

import org.rsmod.game.type.comp.UnpackedComponentType
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.ui.UserInterfaceMap

internal object InterfaceEvents {
    fun isEnabled(
        ui: UserInterfaceMap,
        component: UnpackedComponentType,
        comsub: Int,
        event: IfEvent,
    ): Boolean {
        val override = ui.events.overrideFor(component, comsub)
        return if (override != null) (override and event.bitmask) != 0L
        else comsub == -1 && component.hasEvent(event)
    }
}
