package org.rsmod.api.game.process.player

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class PlayerRunUpdateProcessorTest {
    @Test
    fun GameTestState.`run energy drain block includes clocks before its exclusive expiry`() =
        runInjectedGameTest(Dependencies::class) { deps ->
            with(player) {
                runEnergy = 1_000
                pendingStepCount = 2
                runEnergyDrainBlockedUntil = currentMapClock + 4

                repeat(4) {
                    deps.processor.process(this)
                    assertEquals(1_000, runEnergy)
                    currentMapClock++
                }

                deps.processor.process(this)
                assertTrue(runEnergy < 1_000)
            }
        }

    private class Dependencies @Inject constructor(val processor: PlayerRunUpdateProcessor)
}
