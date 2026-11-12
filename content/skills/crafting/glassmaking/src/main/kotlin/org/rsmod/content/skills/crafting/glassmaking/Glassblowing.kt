package org.rsmod.content.skills.crafting.glassmaking

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
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.obj.ObjTypeList

public class Glassblowing
@Inject
constructor(private val recipes: GlassblowingRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess) {
        with(access) {
            if (!canOpen(sendFailure = true)) {
                return
            }
            val maximum = player.inv.count(objTypes[objs.molten_glass])
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.recipes.map(GlassblowingRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(input.component) ?: return
            start(this, recipe, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: GlassblowingRecipe, quantity: Int) {
        require(quantity >= 1) { "Glassblowing quantity must be positive." }
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            beginAction()
            weakQueue(
                crafting_queues.glassblowing,
                ACTION_TICKS,
                GlassblowingRequest(recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: GlassblowingRequest) {
        val recipe = recipes.recipes.firstOrNull { it.output == request.output } ?: return
        with(access) {
            if (!canMake(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            make(recipe)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMake(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction()
            weakQueue(
                crafting_queues.glassblowing,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_glassblowing)
    }

    private fun ProtectedAccess.make(recipe: GlassblowingRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.molten_glass.id, 1)
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Glassblowing transaction failed after validation: $recipe" }
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canOpen(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (!player.members) {
            return reject("You need to be on a members' world to blow glass.")
        }
        if (player.inv.none { it?.id == objs.glassblowing_pipe.id }) {
            return reject("You need a glassblowing pipe to blow glass.")
        }
        if (player.inv.count(objTypes[objs.molten_glass]) == 0) {
            return reject("You need some molten glass to blow glass.")
        }
        return true
    }

    private fun ProtectedAccess.canMake(recipe: GlassblowingRecipe, sendFailure: Boolean): Boolean {
        if (!canOpen(sendFailure)) {
            return false
        }
        if (player.stat(stats.crafting) < recipe.level) {
            if (sendFailure) {
                mes("You need a Crafting level of ${recipe.level} to make this item.")
            }
            return false
        }
        return true
    }

    private companion object {
        const val ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to make?"
    }
}

public data class GlassblowingRequest(
    val output: org.rsmod.game.type.obj.ObjType,
    val remaining: Int,
)
