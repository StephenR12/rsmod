package org.rsmod.content.skills.crafting.pottery

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiaryPottery
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList

public class PotteryShaping
@Inject
constructor(private val recipes: PotteryRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, wheel: BoundLocInfo) {
        with(access) {
            if (!player.inv.contains(objs.soft_clay)) {
                mes("You need some soft clay to use the potter's wheel.")
                return
            }
            val maximum = player.inv.count(objTypes[objs.soft_clay])
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.recipes.map(PotteryRecipe::unfired),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(input.component) ?: return
            start(this, wheel, recipe, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        wheel: BoundLocInfo,
        recipe: PotteryRecipe,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Pottery-shaping quantity must be positive." }
        with(access) {
            if (!canShape(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(seqs.human_potterywheel)
            weakQueue(
                crafting_queues.potteryShaping,
                ACTION_TICKS,
                PotteryShapingRequest(wheel, recipe.unfired, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: PotteryShapingRequest) {
        val recipe = recipes.forUnfired(request.unfired) ?: return
        with(access) {
            if (
                !isWithinDistance(request.wheel, distance = 1) ||
                    !canShape(recipe, sendFailure = false)
            ) {
                resetAnim()
                return
            }
            shape(recipe, request.wheel.coords)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canShape(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(seqs.human_potterywheel)
            weakQueue(
                crafting_queues.potteryShaping,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.shape(recipe: PotteryRecipe, coords: org.rsmod.map.CoordGrid) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.soft_clay.id, 1)
                add(inventory, recipe.unfired.id, recipe.outputCount)
            }
        check(result.success) { "Pottery-shaping transaction failed after validation: $recipe" }
        statAdvance(stats.crafting, recipe.shapingXp)
        player.recordDiaryPottery(recipe.unfired.id, coords)
    }

    private fun ProtectedAccess.canShape(recipe: PotteryRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to make this item.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        if (recipe.questLocked && player.vars[varps.onesmallfavour] < POT_LID_UNLOCK_STATE) {
            return reject("You need to learn how to make pot lids during One Small Favour.")
        }
        if (!player.inv.contains(objs.soft_clay)) {
            return reject("You need some soft clay to make this item.")
        }
        val capacityAfterClay = player.inv.freeSpace() + 1
        if (capacityAfterClay < recipe.outputCount) {
            return reject("You do not have enough inventory space to make this item.")
        }
        return true
    }

    private companion object {
        const val ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val POT_LID_UNLOCK_STATE = 235
        const val MENU_TITLE = "What would you like to make?"
    }
}

public data class PotteryShapingRequest(
    val wheel: BoundLocInfo,
    val unfired: org.rsmod.game.type.obj.ObjType,
    val remaining: Int,
)
