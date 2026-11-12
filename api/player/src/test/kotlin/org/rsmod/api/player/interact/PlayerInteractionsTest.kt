package org.rsmod.api.player.interact

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.rsmod.api.player.events.interact.ApDefaultEvent
import org.rsmod.api.player.events.interact.OpDefaultEvent
import org.rsmod.api.player.events.interact.PlayerEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp

class PlayerInteractionsTest {
    @Test
    fun `player options six through eight resolve registered operation events`() {
        val eventBus = EventBus()
        val interactions = PlayerInteractions(eventBus)
        eventBus.subscribeSuspend(PlayerEvents.Op6::class.java, OpDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(PlayerEvents.Op7::class.java, OpDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(PlayerEvents.Op8::class.java, OpDefaultEvent.ID.toLong()) {}
        val target = Player()

        assertInstanceOf(
            PlayerEvents.Op6::class.java,
            interactions.opTrigger(target, InteractionOp.Op6),
        )
        assertInstanceOf(
            PlayerEvents.Op7::class.java,
            interactions.opTrigger(target, InteractionOp.Op7),
        )
        assertInstanceOf(
            PlayerEvents.Op8::class.java,
            interactions.opTrigger(target, InteractionOp.Op8),
        )
    }

    @Test
    fun `player options six through eight resolve registered approach events`() {
        val eventBus = EventBus()
        val interactions = PlayerInteractions(eventBus)
        eventBus.subscribeSuspend(PlayerEvents.Ap6::class.java, ApDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(PlayerEvents.Ap7::class.java, ApDefaultEvent.ID.toLong()) {}
        eventBus.subscribeSuspend(PlayerEvents.Ap8::class.java, ApDefaultEvent.ID.toLong()) {}
        val target = Player()

        assertInstanceOf(
            PlayerEvents.Ap6::class.java,
            interactions.apTrigger(target, InteractionOp.Op6),
        )
        assertInstanceOf(
            PlayerEvents.Ap7::class.java,
            interactions.apTrigger(target, InteractionOp.Op7),
        )
        assertInstanceOf(
            PlayerEvents.Ap8::class.java,
            interactions.apTrigger(target, InteractionOp.Op8),
        )
    }
}
