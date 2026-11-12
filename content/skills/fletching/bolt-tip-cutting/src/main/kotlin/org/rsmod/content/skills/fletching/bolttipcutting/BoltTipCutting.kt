package org.rsmod.content.skills.fletching.bolttipcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class BoltTipCutting
@Inject
constructor(private val recipes: BoltTipCuttingRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, input: ObjType) {
        val recipe = recipes.forInput(input) ?: return
        with(access) {
            if (!canCut(recipe, sendFailure = true)) {
                return
            }
            if (recipe.warnsBeforeCutting && !confirmDragonstone(this)) {
                mes(DRAGONSTONE_DECLINE_MESSAGE)
                return
            }
            if (!canCut(recipe, sendFailure = false)) {
                return
            }
            val maximum = player.inv.count(objTypes[recipe.input])
            val selection =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, selection.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: BoltTipCuttingRecipe, quantity: Int) {
        require(quantity >= 1) { "Bolt-tip cutting quantity must be positive." }
        with(access) {
            if (!canCut(recipe, sendFailure = true)) {
                return
            }
            clearFletchingQueues()
            anim(recipe.sequence)
            weakQueue(
                fletching_queues.boltTipCutting,
                ACTION_TICKS,
                BoltTipCuttingRequest(recipe.input, recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: BoltTipCuttingRequest) {
        val recipe = recipes.find(request.input, request.output) ?: return
        with(access) {
            if (!canCut(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            cut(recipe)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canCut(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(recipe.sequence)
            weakQueue(
                fletching_queues.boltTipCutting,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private suspend fun confirmDragonstone(access: ProtectedAccess): Boolean {
        var confirmed = false
        access.startDialogue {
            confirmed =
                choice2(
                    DRAGONSTONE_CONFIRM_CHOICE,
                    true,
                    DRAGONSTONE_DECLINE_CHOICE,
                    false,
                    title = DRAGONSTONE_CONFIRM_TITLE,
                )
        }
        return confirmed
    }

    private fun ProtectedAccess.cut(recipe: BoltTipCuttingRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.input.id, 1)
                add(inventory, recipe.output.id, recipe.outputCount)
            }
        check(result.success) { "Bolt-tip cutting transaction failed after validation: $recipe" }
        statAdvance(stats.fletching, recipe.xp)
        mes(SUCCESS_MESSAGE)
    }

    private fun ProtectedAccess.canCut(
        recipe: BoltTipCuttingRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to train Fletching.")
        }
        if (player.stat(stats.fletching) < recipe.level) {
            return reject("You need a Fletching level of ${recipe.level} to make these bolt tips.")
        }
        if (!player.inv.contains(objs.chisel)) {
            return reject("You need a chisel to cut bolt tips.")
        }
        if (player.inv.count(objTypes[recipe.input]) == 0) {
            return reject("You do not have any ${objTypes[recipe.input].name.lowercase()} left.")
        }
        return true
    }

    private companion object {
        const val ACTION_TICKS = 5
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "How many would you like to make?"
        const val SUCCESS_MESSAGE = "You use your chisel to fletch small bolt tips."
        const val DRAGONSTONE_CONFIRM_TITLE = "Are you sure you want to cut this?"
        const val DRAGONSTONE_CONFIRM_CHOICE = "Yes, I want to cut my dragonstone into bolt tips."
        const val DRAGONSTONE_DECLINE_CHOICE = "No! Don't cut my lovely dragonstone!"
        const val DRAGONSTONE_DECLINE_MESSAGE =
            "You carefully take the chisel away from the expensive dragonstone."
    }
}

public data class BoltTipCuttingRequest(
    val input: ObjType,
    val output: ObjType,
    val remaining: Int,
)
