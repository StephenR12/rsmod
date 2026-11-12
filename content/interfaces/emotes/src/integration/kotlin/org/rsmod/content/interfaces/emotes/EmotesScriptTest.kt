package org.rsmod.content.interfaces.emotes

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.back
import org.rsmod.api.script.onEvent
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.spot.EntitySpotanim
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EmotesScriptTest {
    @Test
    fun GameTestState.`validate all emotes`() =
        runGameTest(EmotesScript::class) {
            // Open the Emotes tab, otherwise button clicks would be discarded.
            player.ifOpenOverlay(interfaces.emote, components.toplevel_target_side12)

            val emoteSlots = cacheTypes.enums[emote_enums.emote_names].keys
            for (slot in emoteSlots) {
                assertDoesNotThrow("Emote for slot is not implemented: slot=$slot") {
                    player.ifButton(emote_components.emote_list, slot)
                    advance()
                }
            }
        }

    @Test
    fun GameTestState.`all ordinary skillcapes dispatch their cache 233 emotes`() =
        runGameTest(EmotesScript::class, SkillCapeEmoteCaptureScript::class) {
            val slot =
                cacheTypes.enums[emote_enums.emote_names]
                    .entries
                    .single { it.value == "Skill Cape" }
                    .key
            val anims = cacheTypes.enums[emote_enums.skill_cape_anims]
            val spots = cacheTypes.enums[emote_enums.skill_cape_spots]
            val capes = cacheTypes.objs.values.filter { it.isContentType(content.skill_cape) }

            for (cape in capes) {
                val stat = cape.param(params.statreq1_skill)
                val subject = registerPlayer()
                subject.setVarp(varps.lastcombat, -100)
                subject.setVarp(varps.lastcombat_pvp, -100)
                subject.back = InvObj(cape)
                SkillCapeEmoteCaptureScript.reset()
                openEmoteTab(subject)

                subject.ifButton(emote_components.emote_list, slot)
                advance()

                assertMessageNotSent(
                    "You need to be wearing a skillcape in order to perform that emote.",
                    subject,
                )
                assertMessageNotSent("You can't perform that emote now.", subject)
                assertEquals(
                    anims.getValue(stat).id,
                    SkillCapeEmoteCaptureScript.sequence,
                    cape.name,
                )
                assertEquals(
                    spots.getValue(stat).id,
                    SkillCapeEmoteCaptureScript.spotanim,
                    cape.name,
                )
            }
        }

    @Test
    fun GameTestState.`skillcape emote requires a worn cape and rejects combat`() =
        runGameTest(EmotesScript::class) {
            val slot =
                cacheTypes.enums[emote_enums.emote_names]
                    .entries
                    .single { it.value == "Skill Cape" }
                    .key
            player.inv[0] = InvObj(objs.attack_skillcape)
            openEmoteTab(player)

            player.ifButton(emote_components.emote_list, slot)
            advance()

            assertMessageSent("You need to be wearing a skillcape in order to perform that emote.")
            assertEquals(65535, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isEmpty)

            client.clear()
            player.back = InvObj(objs.attack_skillcape)
            player.setVarp(varps.lastcombat, player.currentMapClock)
            openEmoteTab(player)

            player.ifButton(emote_components.emote_list, slot)
            advance()

            assertMessageSent("You can't perform that emote now.")
            assertEquals(65535, player.pendingSequence.id)
            assertTrue(player.pendingSpotanims.isEmpty)
        }

    private fun GameTestScope.openEmoteTab(subject: Player) {
        subject.ifOpenOverlay(interfaces.emote, components.toplevel_target_side12)
    }
}

private class SkillCapeEmoteCaptureScript : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<PlayEmote> {
            sequence = seq.id
            spotanim = EntitySpotanim(player.pendingSpotanims.getLong(0)).id
        }
    }

    companion object {
        var sequence: Int = -1
        var spotanim: Int = -1

        fun reset() {
            sequence = -1
            spotanim = -1
        }
    }
}
