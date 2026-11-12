package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.refs.mesanims
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.shops.Shops
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.areas.city.lumbridge.configs.lumbridge_invs
import org.rsmod.content.areas.city.lumbridge.configs.lumbridge_npcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Bob @Inject constructor(private val shops: Shops, private val repairs: BobRepairService) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(lumbridge_npcs.bob) { startDialogue(it.npc) }
        onOpNpc3(lumbridge_npcs.bob) { player.openShop(it.npc) }
        onOpNpc4(lumbridge_npcs.bob) { repairOp(it.npc) }
        onOpNpcU(lumbridge_npcs.bob) { repairItem(it.npc, it.invSlot) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Bob's Brilliant Axes", lumbridge_invs.axeshop)
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { bobDialogue(npc) }
    }

    private suspend fun Dialogue.bobDialogue(npc: Npc) {
        val choice =
            choice3(
                "Give me a quest!",
                1,
                "Have you anything to sell?",
                2,
                "Can you repair my items for me?",
                3,
            )
        when (choice) {
            1 -> requestQuest()
            2 -> openShop(npc)
            3 -> repairItems()
        }
    }

    private suspend fun Dialogue.requestQuest() {
        chatPlayer(happy, "Give me a quest!")
        chatNpc(angry, "Get yer own!")
    }

    private suspend fun Dialogue.openShop(npc: Npc) {
        chatPlayer(quiz, "Have you anything to sell?")
        chatNpc(happy, "Yes! I buy and sell axes! Take your pick (or axe)!")
        player.openShop(npc)
    }

    private suspend fun Dialogue.repairItems() {
        chatPlayer(sad, "Can you repair my items for me?")
        chatNpc(
            shocked,
            "Of course I'll repair it, though the materials may cost " +
                "you. Just hand me the item and I'll have a look.",
        )
    }

    private suspend fun ProtectedAccess.repairOp(npc: Npc) {
        val plan = repairs.quoteAll(this)
        if (plan.quotes.isEmpty()) {
            chatNpc(npc, mesanims.confused, "You don't have anything I can repair.")
            return
        }
        repairDialogue(npc, plan, repairAll = true)
    }

    private suspend fun ProtectedAccess.repairItem(npc: Npc, slot: Int) {
        val plan = repairs.quoteSlot(this, slot)
        if (plan.quotes.isEmpty()) {
            chatNpc(npc, mesanims.confused, "Sorry friend, but I can't do anything with that.")
            return
        }
        repairDialogue(npc, plan, repairAll = false)
    }

    private suspend fun ProtectedAccess.repairDialogue(
        npc: Npc,
        plan: BobRepairPlan,
        repairAll: Boolean,
    ) {
        startDialogue(npc) {
            val repairText =
                if (repairAll) {
                    "Repair all items: ${plan.totalCost.formatAmount} coins."
                } else {
                    "Repair that item: ${plan.totalCost.formatAmount} coins."
                }
            if (!choice2(repairText, true, "Cancel.", false)) {
                return@startDialogue
            }
            when (val result = repairs.repair(access, plan)) {
                is BobRepairResult.Repaired -> {
                    mesbox(
                        if (result.itemCount == 1) {
                            "The item is repaired."
                        } else {
                            "The items are repaired."
                        }
                    )
                    chatNpc(happy, "There you go, happy doing business with you!")
                }
                is BobRepairResult.InsufficientCoins ->
                    chatNpc(
                        sad,
                        "You don't have enough money. You'll need ${result.cost.formatAmount} coins.",
                    )
                BobRepairResult.InventoryChanged ->
                    chatNpc(confused, "That item doesn't seem to be there anymore.")
                BobRepairResult.NothingToRepair ->
                    chatNpc(confused, "You don't have anything I can repair.")
            }
        }
    }
}
