package org.rsmod.content.skills.crafting.weaving

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.config.refs.objs
import jakarta.inject.Inject
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
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class Weaving
@Inject
constructor(private val recipes: WeavingRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(
        access: ProtectedAccess,
        loom: BoundLocInfo,
        requestedInput: ObjType? = null,
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use a loom.")
                return
            }
            if (requestedInput != null && !recipes.isInput(requestedInput)) {
                return
            }
            if (!hasAnyInput()) {
                mes("You need something suitable to weave.")
                return
            }
            val maximum = recipes.recipes.maxOf { maximumActions(it) }.coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.recipes.map(WeavingRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(input.component) ?: return
            start(this, loom, recipe, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        loom: BoundLocInfo,
        recipe: WeavingRecipe,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Weaving quantity must be positive." }
        with(access) {
            if (!canWeave(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(seqs.human_loom_weaving)
            weakQueue(
                crafting_queues.weaving,
                recipe.firstActionTicks,
                WeavingRequest(loom, recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: WeavingRequest) {
        val recipe = recipes.recipes.firstOrNull { it.output == request.output } ?: return
        with(access) {
            if (!isWithinDistance(request.loom, distance = 1) || !canWeave(recipe, false)) {
                resetAnim()
                return
            }
            weave(recipe, request.loom)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canWeave(recipe, false)) {
                resetAnim()
                return
            }
            anim(seqs.human_loom_weaving)
            weakQueue(
                crafting_queues.weaving,
                recipe.repeatActionTicks + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.weave(recipe: WeavingRecipe, loom: BoundLocInfo) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.input.id, recipe.inputCount)
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Weaving transaction failed after validation: $recipe" }
        if (recipe.output == objs.basket && loom.coords == org.rsmod.map.CoordGrid(3039, 3287)) player.completeDiaryTask(DiaryTask.WeaveFaladorBasket)
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canWeave(recipe: WeavingRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to use a loom.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        if (player.inv.count(objTypes[recipe.input]) < recipe.inputCount) {
            return reject("You do not have enough material to make that.")
        }
        return true
    }

    private fun ProtectedAccess.hasAnyInput(): Boolean =
        recipes.recipes.any { player.inv.contains(it.input) }

    private fun ProtectedAccess.maximumActions(recipe: WeavingRecipe): Int =
        player.inv.count(objTypes[recipe.input]) / recipe.inputCount

    private companion object {
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to weave?"
    }
}

public data class WeavingRequest(val loom: BoundLocInfo, val output: ObjType, val remaining: Int)
