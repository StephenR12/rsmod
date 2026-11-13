package org.rsmod.game.ui.collection

import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.ui.UserInterface

public class ComponentEventMap(
    private val interfaces: Int2ObjectMap<MutableList<Event>> = Int2ObjectOpenHashMap()
) {
    public operator fun get(type: ComponentType, slot: Int): Long =
        overrideFor(type, slot) ?: 0L

    /** Null means the cache default applies. A zero mask explicitly disables the operations. */
    public fun overrideFor(type: ComponentType, slot: Int): Long? {
        val eventList = interfaces[type.interfaceId] ?: return null
        for (index in eventList.lastIndex downTo 0) {
            val event = eventList[index]
            if (event.component == type.component && slot in event.start..event.end) {
                return event.events
            }
        }
        return null
    }

    public fun add(type: ComponentType, range: IntRange, events: Long) {
        val eventList = interfaces.computeIfAbsent(type.interfaceId) { mutableListOf() }
        val event = Event.from(type.component, range.first, range.last, events)
        eventList.add(event)
    }

    public fun clear(interf: UserInterface) {
        interfaces.remove(interf.id)
    }

    public data class Event(val component: Int, val start: Int, val end: Int, val events: Long) {
        public companion object {
            public fun from(component: Int, start: Int, end: Int, events: Long): Event {
                // The client stores -1 as the static component itself. Keep support for
                // callers that use an open-ended range starting at a nonnegative child.
                val last = if (start >= 0 && end == -1) Int.MAX_VALUE else end
                return Event(component, start, last, events)
            }
        }
    }
}
