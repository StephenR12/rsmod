package org.rsmod.content.skills.fletching.logcutting

import org.rsmod.api.player.diary.recordDiaryFletch
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class LogFletching
@Inject
constructor(private val recipes: LogFletchingRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, log: ObjType) {
        with(access) {
            val choices = recipes.forLog(log)
            if (choices.isEmpty() || !canOpen(log, sendFailure = true)) {
                return
            }
            val maximum = choices.maxOf { maximumActions(it) }.coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = choices.map(LogFletchingRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(log, input.component) ?: return
            start(this, recipe, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: LogFletchingRecipe, quantity: Int) {
        require(quantity >= 1) { "Log-fletching quantity must be positive." }
        with(access) {
            if (!canFletch(recipe, sendFailure = true)) {
                return
            }
            clearFletchingQueues()
            beginAction()
            weakQueue(
                fletching_queues.logCutting,
                FIRST_ACTION_TICKS,
                LogFletchingRequest(recipe.log, recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: LogFletchingRequest) {
        val recipe =
            recipes.recipes.firstOrNull { it.log == request.log && it.output == request.output }
                ?: return
        with(access) {
            if (!canFletch(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            fletch(recipe)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canFletch(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction()
            weakQueue(
                fletching_queues.logCutting,
                repeatActionTicks() + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_fletching)
    }

    private fun ProtectedAccess.fletch(recipe: LogFletchingRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.log.id, recipe.logCount)
                add(inventory, recipe.output.id, recipe.outputCount)
            }
        check(result.success) { "Log-fletching transaction failed after validation: $recipe" }
        player.recordDiaryFletch(recipe.output.id, recipe.outputCount, player.coords)
        statAdvance(stats.fletching, recipe.xp)
    }

    private fun ProtectedAccess.canOpen(log: ObjType, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to train Fletching.")
        }
        if (!hasInventoryKnife()) {
            return reject("You need a knife to fletch logs.")
        }
        if (player.inv.count(objTypes[log]) == 0) {
            return reject("You do not have any logs to fletch.")
        }
        return true
    }

    private fun ProtectedAccess.canFletch(
        recipe: LogFletchingRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!canOpen(recipe.log, sendFailure)) {
            return false
        }
        if (player.stat(stats.fletching) < recipe.level) {
            return reject("You need a Fletching level of ${recipe.level} to make this item.")
        }
        if (player.inv.count(objTypes[recipe.log]) < recipe.logCount) {
            return reject("You do not have enough logs to make that.")
        }
        return true
    }

    private fun ProtectedAccess.hasInventoryKnife(): Boolean =
        player.inv.contains(objs.knife) || player.inv.contains(objs.fletching_knife)

    private fun ProtectedAccess.hasFletchingKnifeBonus(): Boolean =
        player.inv.contains(objs.fletching_knife) || player.worn.contains(objs.fletching_knife)

    private fun ProtectedAccess.repeatActionTicks(): Int =
        if (hasFletchingKnifeBonus()) FLETCHING_KNIFE_ACTION_TICKS else KNIFE_ACTION_TICKS

    private fun ProtectedAccess.maximumActions(recipe: LogFletchingRecipe): Int =
        (player.inv.count(objTypes[recipe.log]) / recipe.logCount).let {
            if (recipe.log == objs.ent_branch) it.coerceAtMost(28) else it
        }

    private companion object {
        const val FIRST_ACTION_TICKS = 2
        const val KNIFE_ACTION_TICKS = 3
        const val FLETCHING_KNIFE_ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to make?"
    }
}

public data class LogFletchingRequest(val log: ObjType, val output: ObjType, val remaining: Int)
