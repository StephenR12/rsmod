package org.rsmod.content.skills.crafting.customfur

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CustomFurClothingScript
@Inject
constructor(private val service: CustomFurClothing, private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(npcs.asyff) { talk(it.npc) }
        onOpNpc3(npcs.asyff) { openShopOrClosed(it.npc) }
        onOpNpc4(npcs.asyff) { openOrClosed(it.npc) }
        onIfModalButton(components.hunting_customfurs_choices_items) {
            service.make(this, it.comsub, it.obj)
        }
        onIfClose(interfaces.hunting_customfurs) { service.close(player) }
    }

    private suspend fun ProtectedAccess.openShopOrClosed(npc: Npc) {
        if (!player.members) {
            startDialogue(npc) { chatNpc(neutral, "Sorry, we're closed at the moment.") }
            return
        }
        openShop(npc)
    }

    private suspend fun ProtectedAccess.openOrClosed(npc: Npc) {
        if (!player.members) {
            startDialogue(npc) { chatNpc(neutral, "Sorry, we're closed at the moment.") }
            return
        }
        service.open(this)
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        if (!player.members) {
            startDialogue(npc) { chatNpc(neutral, "Sorry, we're closed at the moment.") }
            return
        }
        startDialogue(npc) {
            chatNpc(neutral, "Now you look like someone who goes to a lot of fancy dress parties.")
            chatPlayer(quiz, "Errr...what are you saying exactly?")
            chatNpc(
                neutral,
                "I'm just saying that perhaps you would like to peruse my selection of garments.",
            )
            chatNpc(
                neutral,
                "Or, if that doesn't interest you, then maybe you have something else to offer? " +
                    "I'm always on the look out for interesting or unusual new materials.",
            )
            asyffOptions(npc)
        }
    }

    private suspend fun Dialogue.asyffOptions(npc: Npc) {
        when (
            choice4(
                "Okay, lets see what you've got then.",
                1,
                "I think I might just leave the perusing for now thanks.",
                2,
                "Can you make clothing suitable for hunting in?",
                3,
                "What sort of unusual materials did you have in mind?",
                4,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Okay, let's see what you've got then.")
                openShop(npc)
            }
            2 -> chatPlayer(neutral, "I think I might just leave the perusing for now, thanks.")
            3 -> {
                chatPlayer(neutral, "Can you make clothing suitable for hunting?")
                chatNpc(
                    neutral,
                    "Certainly. Take a look at my range of made-to-order items. If you can " +
                        "supply the furs, I'll gladly make any of these for you.",
                )
                service.open(access)
            }
            4 -> {
                chatPlayer(quiz, "What sort of unusual materials did you have in mind?")
                chatNpc(
                    neutral,
                    "Well, some more colourful feathers might be useful. For some surreal " +
                        "reason, all I normally seem to get offered are large quantities of " +
                        "rather beaten-up looking chicken feathers.",
                )
                chatNpc(
                    neutral,
                    "People must have some very strange pastimes around these parts, that's all " +
                        "I can say.",
                )
                chatPlayer(neutral, "Ok, let's see what you've got then.")
                openShop(npc)
            }
        }
    }

    private fun Dialogue.openShop(npc: Npc) {
        shops.open(player, npc, "Fancy Clothes Store", invs.fancyclothesstore)
    }

    private fun ProtectedAccess.openShop(npc: Npc) {
        shops.open(player, npc, "Fancy Clothes Store", invs.fancyclothesstore)
    }
}
