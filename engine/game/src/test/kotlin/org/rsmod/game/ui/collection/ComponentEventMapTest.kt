package org.rsmod.game.ui.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.game.type.comp.HashedComponentType
import org.rsmod.game.ui.UserInterface

class ComponentEventMapTest {
    @Test
    fun `static overrides including an empty mask are independent of child slots`() {
        val events = ComponentEventMap()
        val component = HashedComponentType(null, "example:button", 25 shl 16 or 3)
        events.add(component, -1..-1, 8L)
        events.add(component, 0..4, 2L)
        assertEquals(8L, events.overrideFor(component, -1))
        assertEquals(2L, events.overrideFor(component, 0))
        assertNull(events.overrideFor(component, 5))
        events.add(component, -1..0, 0L)
        assertEquals(0L, events.overrideFor(component, -1))
        assertEquals(0L, events.overrideFor(component, 0))
        assertEquals(2L, events.overrideFor(component, 1))
        events.clear(UserInterface(25))
        assertNull(events.overrideFor(component, -1))
    }
}
