package org.rsmod.api.player.interact

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.rsmod.api.player.events.interact.ApDefaultEvent
import org.rsmod.api.player.events.interact.ObjDefaultEvents
import org.rsmod.api.testing.factory.objFactory
import org.rsmod.api.testing.factory.objTypeFactory
import org.rsmod.events.EventBus
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.type.obj.ObjTypeList

class ObjInteractionsTest {
    @Test
    fun `default object approach options resolve their matching events`() {
        val eventBus = EventBus()
        val type = objTypeFactory.create(id = 1)
        val interactions = ObjInteractions(ObjTypeList(mutableMapOf(type.id to type)), eventBus)
        val obj = objFactory.create(type)
        eventBus.subscribeSuspend(ObjDefaultEvents.Ap1::class.java, ApDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(ObjDefaultEvents.Ap2::class.java, ApDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(ObjDefaultEvents.Ap3::class.java, ApDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(ObjDefaultEvents.Ap4::class.java, ApDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(ObjDefaultEvents.Ap5::class.java, ApDefaultEvent.ID.toLong()) {}

        assertInstanceOf(
            ObjDefaultEvents.Ap1::class.java,
            interactions.apTrigger(obj, InteractionOp.Op1),
        )
        assertInstanceOf(
            ObjDefaultEvents.Ap2::class.java,
            interactions.apTrigger(obj, InteractionOp.Op2),
        )
        assertInstanceOf(
            ObjDefaultEvents.Ap3::class.java,
            interactions.apTrigger(obj, InteractionOp.Op3),
        )
        assertInstanceOf(
            ObjDefaultEvents.Ap4::class.java,
            interactions.apTrigger(obj, InteractionOp.Op4),
        )
        assertInstanceOf(
            ObjDefaultEvents.Ap5::class.java,
            interactions.apTrigger(obj, InteractionOp.Op5),
        )
    }
}
