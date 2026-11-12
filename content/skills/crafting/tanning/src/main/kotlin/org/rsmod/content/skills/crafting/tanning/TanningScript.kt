package org.rsmod.content.skills.crafting.tanning

import jakarta.inject.Inject
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TanningScript
@Inject
constructor(private val tanning: Tanning, private val recipes: TanningRecipeRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(npcs.ellis_tanner) { tanning.open(this, TannerPricing.Standard) }
        onOpNpc3(npcs.tanner) { tanning.open(this, TannerPricing.Standard) }
        onOpNpc1(npcs.ranging_guild_leatherworker) { leatherworkerDialogue(it.npc) }
        onOpNpc3(npcs.werewolf_tanner) { tanning.open(this, TannerPricing.Premium) }
        onOpNpc3(npcs.auburn_tanner) { tanning.open(this, TannerPricing.Premium) }
        onOpNpc3(npcs.eodan) { tanning.open(this, TannerPricing.Eodan) }

        for (recipe in recipes.recipes) {
            onIfModalButton(recipe.buttons.all) { tanning.tan(this, recipe, Int.MAX_VALUE) }
            onIfModalButton(recipe.buttons.x) {
                val count = countDialog("How many hides would you like to tan?")
                tanning.tan(this, recipe, count)
            }
            onIfModalButton(recipe.buttons.five) { tanning.tan(this, recipe, 5) }
            onIfModalButton(recipe.buttons.one) { tanning.tan(this, recipe, 1) }
        }
    }

    private suspend fun ProtectedAccess.leatherworkerDialogue(npc: Npc) {
        startDialogue(npc) {
            chatPlayer(neutral, "Hello.")
            chatNpc(neutral, "Can I help you?")
            leatherworkerOptions()
        }
    }

    private suspend fun Dialogue.leatherworkerOptions() {
        while (true) {
            val choice =
                if (hasTannableHides()) {
                    choice3(
                        "I'd like you to tan something for me.",
                        1,
                        "What do you do here?",
                        2,
                        "No thanks.",
                        3,
                    )
                } else {
                    choice2("What do you do here?", 2, "No thanks.", 3)
                }
            when (choice) {
                1 -> {
                    tanning.open(access, TannerPricing.Standard)
                    return
                }
                2 -> describeLeatherworking()
                3 -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(neutral, "Suit yourself.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.describeLeatherworking() {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "Well, I can cure plain cowhides into pieces of leather ready for crafting.",
        )
        chatNpc(
            neutral,
            "I work with ordinary, hard or dragonhide leather and also snakeskin. " +
                "What would you like to know about?",
        )
        when (
            choice5(
                "Ordinary leather.",
                1,
                "Hard leather.",
                2,
                "Snakeskin.",
                3,
                "Dragonhide leather.",
                4,
                "I'm bored of leather already.",
                5,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Ordinary leather.")
                chatNpc(
                    neutral,
                    "I can work one piece of cow hide into ordinary leather. " +
                        "It only costs one piece of gold.",
                )
            }
            2 -> {
                chatPlayer(neutral, "Hard leather.")
                chatNpc(
                    neutral,
                    "Hard leather is made by specially treating normal leather by wetting " +
                        "and drying several times.",
                )
                chatNpc(
                    neutral,
                    "It's then treated with pine oils. It's harder wearing than ordinary leather.",
                )
            }
            3 -> {
                chatPlayer(neutral, "Snake skins.")
                chatNpc(
                    neutral,
                    "I can work one piece of snake hide into a snakeskin. It only costs 15 " +
                        "pieces of gold. Snakeskin armour is great for intermediate level " +
                        "rangers and provides more defence than hard and studded leather.",
                )
            }
            4 -> {
                chatPlayer(neutral, "Dragonhide leather.")
                chatNpc(
                    neutral,
                    "Dragon leather is made from dragonhides. One piece of hide can be worked " +
                        "into one piece of dragon leather. It costs 20 coins each piece.",
                )
                chatNpc(
                    neutral,
                    "Dragon leather is hard-wearing and has excellent flexibility. You can " +
                        "make strong and supple armour with this.",
                )
            }
            5 -> {
                chatPlayer(neutral, "I'm bored of leather already.")
                chatNpc(
                    neutral,
                    "You may be bored now, but an arrow in the backside will soon fix that!",
                )
                return
            }
        }
        chatNpc(neutral, "Is there anything else I can help you with?")
    }

    private fun Dialogue.hasTannableHides(): Boolean {
        val tannableIds = recipes.recipes.mapTo(hashSetOf()) { it.input.id }
        return player.inv.any { it?.id in tannableIds }
    }
}
