package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Npc

/** Guildmaster Apatura's cache-era Hunter-cape teleport unlock route. */
public class HunterCapeGuildmasterDialogue @Inject constructor(private val state: HunterCapeState) {
    public suspend fun talk(access: ProtectedAccess, npc: Npc) {
        access.startDialogue(npc) {
            chatNpc(neutral, "Nilsal, hunter.")
            chatPlayer(neutral, "Nilsal, Apatura.")
            if (hasCape() && !state.hasGuildTeleport(access)) {
                offerUpgrade()
            } else {
                standardMenu()
            }
        }
    }

    private suspend fun Dialogue.offerUpgrade() {
        chatNpc(neutral, "Is that a skillcape you have with you?")
        chatPlayer(neutral, "It is.")
        chatNpc(
            neutral,
            "Would you like me to enhance it with the ability to teleport directly to the guild?",
        )
        when (choice2("Yes please.", true, "No thanks.", false)) {
            true -> {
                chatPlayer(neutral, "Yes please.")
                state.unlockGuildTeleport(access)
                chatNpc(neutral, "Kuani! That should be done for you now.")
            }
            false -> chatPlayer(neutral, "No thanks.")
        }
        standardMenu()
    }

    private suspend fun Dialogue.standardMenu() {
        when (choice2("What do you do here?", true, "I'd better be off.", false)) {
            true -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(
                    neutral,
                    "We're mostly here to keep an eye on the local animal populations, and provide animal parts to the city.",
                )
                chatNpc(neutral, "As for me personally, I'm the current master of this guild.")
                chatPlayer(quiz, "Current master?")
                chatNpc(
                    neutral,
                    "A hunter's work is dangerous. We tend to go through members faster than other guilds. That said, I'm usually too busy with politics and paperwork to go out hunting these days.",
                )
            }
            false -> chatPlayer(neutral, "I'd better be off.")
        }
    }

    private fun Dialogue.hasCape(): Boolean =
        player.inv.contains(objs.hunter_skillcape) ||
            player.inv.contains(objs.hunter_skillcape_t) ||
            player.worn.contains(objs.hunter_skillcape) ||
            player.worn.contains(objs.hunter_skillcape_t)
}
