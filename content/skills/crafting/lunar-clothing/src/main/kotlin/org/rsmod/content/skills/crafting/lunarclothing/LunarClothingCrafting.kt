package org.rsmod.content.skills.crafting.lunarclothing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.crafting.commons.LeatherThreadUsageRepository
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.game.type.obj.ObjTypeList

public class LunarClothingCrafting
@Inject
constructor(
    private val recipes: LunarClothingRecipeRegistry,
    private val objTypes: ObjTypeList,
    private val threadUses: LeatherThreadUsageRepository,
) {
    public suspend fun select(access: ProtectedAccess, costumeNeedle: Boolean) {
        with(access) {
            if (!canOpen(costumeNeedle, sendFailure = true)) {
                return
            }
            startDialogue {
                val recipe =
                    choice5(
                        recipes.recipes[0].option,
                        recipes.recipes[0],
                        recipes.recipes[1].option,
                        recipes.recipes[1],
                        recipes.recipes[2].option,
                        recipes.recipes[2],
                        recipes.recipes[3].option,
                        recipes.recipes[3],
                        "Cancel",
                        null,
                        title = MENU_TITLE,
                    ) ?: return@startDialogue
                if (!canMake(recipe, costumeNeedle, sendFailure = true)) {
                    return@startDialogue
                }
                clearCraftingQueues()
                anim(seqs.human_leather_crafting)
                delay(ACTION_TICKS)
                if (!canMake(recipe, costumeNeedle, sendFailure = false)) {
                    resetAnim()
                    return@startDialogue
                }
                make(recipe, costumeNeedle)
                resetAnim()
                mes(recipe.success)
            }
        }
    }

    private fun ProtectedAccess.make(recipe: LunarClothingRecipe, costumeNeedle: Boolean) {
        val consumeThread = !costumeNeedle && threadUses.consumesThread(player)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.suqah_leather.id, 1)
                if (consumeThread) {
                    delete(inventory, objs.thread.id, 1)
                }
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Lunar-clothing transaction failed after validation: $recipe" }
        if (!costumeNeedle) {
            threadUses.completeUse(player)
        }
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canOpen(costumeNeedle: Boolean, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make Lunar clothing.")
        }
        val questState = player.vars[varbits.lunar_quest_main]
        if (questState >= QUEST_COMPLETE_STATE) {
            return reject(POST_QUEST_MESSAGE)
        }
        if (questState < CLOTHING_STAGE_STATE) {
            return reject("You are not ready to make Lunar clothing yet.")
        }
        val needle = if (costumeNeedle) objs.costume_needle else objs.needle
        if (!player.inv.contains(needle)) {
            return reject("You need a needle to work with Suqah leather.")
        }
        if (!costumeNeedle && !player.inv.contains(objs.thread)) {
            return reject("You need some thread to work with Suqah leather.")
        }
        if (player.inv.count(objTypes[objs.suqah_leather]) == 0) {
            return reject("You do not have any Suqah leather to work with.")
        }
        return true
    }

    private fun ProtectedAccess.canMake(
        recipe: LunarClothingRecipe,
        costumeNeedle: Boolean,
        sendFailure: Boolean,
    ): Boolean {
        if (!canOpen(costumeNeedle, sendFailure)) {
            return false
        }
        if (player.stat(stats.crafting) < recipe.level) {
            if (sendFailure) {
                mes(recipe.levelFailure)
            }
            return false
        }
        return true
    }

    public companion object {
        public const val CLOTHING_STAGE_STATE: Int = 155
        public const val QUEST_COMPLETE_STATE: Int = 190
        public const val MENU_TITLE: String = "Which item would you like to make?"
        public const val POST_QUEST_MESSAGE: String =
            "You can't craft any more clothes from these hides. You will have to speak with the Oneiromancer."

        private const val ACTION_TICKS = 3
    }
}
