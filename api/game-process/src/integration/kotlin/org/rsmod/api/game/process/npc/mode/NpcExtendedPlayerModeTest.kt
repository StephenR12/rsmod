package org.rsmod.api.game.process.npc.mode

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.rsmod.api.npc.apPlayer6
import org.rsmod.api.npc.apPlayer7
import org.rsmod.api.npc.apPlayer8
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer6
import org.rsmod.api.npc.opPlayer7
import org.rsmod.api.npc.opPlayer8
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onAiApPlayer6
import org.rsmod.api.script.onAiApPlayer7
import org.rsmod.api.script.onAiApPlayer8
import org.rsmod.api.script.onAiOpPlayer6
import org.rsmod.api.script.onAiOpPlayer7
import org.rsmod.api.script.onAiOpPlayer8
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.factory.npcTypeFactory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NpcExtendedPlayerModeTest {
    @Test
    fun GameTestState.`npc player operation modes six through eight dispatch`() =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            ExtendedPlayerModeScript::class,
        ) { deps ->
            val npc = spawnNpc(player.coords.translateX(1), targetNpc)

            npc.opPlayer6(player, deps.interactions)
            advance()
            assertMessageSent("op-player-6")

            npc.opPlayer7(player, deps.interactions)
            advance()
            assertMessageSent("op-player-7")

            npc.opPlayer8(player, deps.interactions)
            advance()
            assertMessageSent("op-player-8")
        }

    @Test
    fun GameTestState.`npc player approach modes six through eight dispatch`() =
        runInjectedGameTest(
            Dependencies::class,
            childModule = null,
            ExtendedPlayerModeScript::class,
        ) { deps ->
            val npc = spawnNpc(player.coords.translateX(1), targetNpc)

            npc.apPlayer6(player, deps.interactions)
            advance()
            assertMessageSent("ap-player-6")

            npc.apPlayer7(player, deps.interactions)
            advance()
            assertMessageSent("ap-player-7")

            npc.apPlayer8(player, deps.interactions)
            advance()
            assertMessageSent("ap-player-8")
        }

    private class Dependencies @Inject constructor(val interactions: AiPlayerInteractions)

    private class ExtendedPlayerModeScript : PluginScript() {
        override fun ScriptContext.startup() {
            onAiOpPlayer6(targetNpc) { it.target.mes("op-player-6") }
            onAiOpPlayer7(targetNpc) { it.target.mes("op-player-7") }
            onAiOpPlayer8(targetNpc) { it.target.mes("op-player-8") }
            onAiApPlayer6(targetNpc) { it.target.mes("ap-player-6") }
            onAiApPlayer7(targetNpc) { it.target.mes("ap-player-7") }
            onAiApPlayer8(targetNpc) { it.target.mes("ap-player-8") }
        }
    }

    private companion object {
        private val targetNpc =
            npcTypeFactory.create {
                name = "Extended player-mode test NPC"
                size = 1
                maxRange = 100
                attackRange = 1
            }
    }
}
