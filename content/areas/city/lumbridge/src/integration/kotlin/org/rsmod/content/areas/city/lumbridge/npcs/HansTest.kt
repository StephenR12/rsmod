package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import java.time.LocalDateTime
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.varps
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.areas.city.lumbridge.configs.lumbridge_npcs
import org.rsmod.game.type.npc.NpcTypeList

class HansTest {
    @Test
    fun `age message converts playtime cycles and clamps future creation time`() {
        val now = LocalDateTime.of(2025, 10, 1, 12, 0)
        val playtimeMinutes = (3 * 24 + 4) * 60 + 5
        assertEquals(
            "You've spent 3 days, 4 hours, 5 minutes in the world since you arrived 42 days ago.",
            Hans.ageMessage(playtimeMinutes * Hans.CLIENT_CYCLES_PER_MINUTE, now.minusDays(42), now),
        )
        assertTrue(Hans.ageMessage(0, now.plusDays(1), now).endsWith("arrived 0 days ago."))
    }

    @Test
    fun GameTestState.`Age operation reports persisted playtime and character age`() =
        runInjectedGameTest(Dependencies::class, null, Hans::class) { deps ->
            val playtimeMinutes = (7 * 24 + 8) * 60 + 9
            player.setVarp(varps.playtime, playtimeMinutes * Hans.CLIENT_CYCLES_PER_MINUTE)
            player.createdAt = LocalDateTime.now().minusDays(365).minusMinutes(1)
            val hans = spawnNpc(player.coords.translate(1, 0), deps.npcs[lumbridge_npcs.hans])

            player.opNpc3(hans)
            advance()

            assertDialogueText(
                "You've spent 7 days, 8 hours, 9 minutes in the world since you arrived 365 days ago."
            )
        }

    private fun GameTestScope.assertDialogueText(text: String) {
        val texts = client.filterIsInstance<IfSetText>().map { it.text.replace("<br>", " ") }
        assertTrue(texts.contains(text)) { "Dialogue text not found: $text | actual=$texts" }
    }

    data class Dependencies @Inject constructor(val npcs: NpcTypeList)
}
