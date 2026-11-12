package org.rsmod.api.game.process.player

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.players.OpPlayer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.rsmod.api.net.rsprot.handlers.OpPlayerHandler
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.interact.InteractionPlayerOp

class PlayerExtendedOperationHandlerTest {
    @Test
    fun GameTestState.`client player operations six through eight retain their slots`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            val target = registerPlayer(coords = player.coords.translateX(1))
            val operations =
                listOf(6 to InteractionOp.Op6, 7 to InteractionOp.Op7, 8 to InteractionOp.Op8)

            for ((wireOp, expected) in operations) {
                deps.handler.handle(
                    player,
                    OpPlayer(index = target.slotId, controlKey = false, op = wireOp),
                )

                val interaction =
                    assertInstanceOf(InteractionPlayerOp::class.java, player.interaction)
                assertEquals(expected, interaction.op)
                assertSame(target, interaction.target)
            }
        }

    private class Dependencies @Inject constructor(val handler: OpPlayerHandler)
}
