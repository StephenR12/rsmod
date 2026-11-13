package org.rsmod.content.skills.crafting.pottery

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiaryPottery
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class PotteryFiring
@Inject
constructor(
    private val recipes: PotteryRecipeRegistry,
    private val objTypes: ObjTypeList,
    private val worldRepo: WorldRepository,
) {
    public suspend fun select(
        access: ProtectedAccess,
        oven: BoundLocInfo,
        requestedInput: ObjType? = null,
    ) {
        with(access) {
            if (requestedInput != null && recipes.forUnfired(requestedInput) == null) {
                return
            }
            if (!hasAnyUnfiredPottery()) {
                mes("You need some unfired pottery to use the pottery oven.")
                return
            }
            val maximum = recipes.recipes.maxOf { player.inv.count(objTypes[it.unfired]) }
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.recipes.map(PotteryRecipe::unfired),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = FIRE_ACTION_MODE,
                )
            val recipe = recipes.forButton(input.component) ?: return
            if (requestedInput != null && recipe.unfired != requestedInput) {
                return
            }
            start(this, oven, recipe, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        oven: BoundLocInfo,
        recipe: PotteryRecipe,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Pottery-firing quantity must be positive." }
        with(access) {
            if (!canFire(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            locAnim(worldRepo, oven, seqs.potteryoven_quick)
            weakQueue(
                crafting_queues.potteryFiring,
                ACTION_TICKS,
                PotteryFiringRequest(oven, recipe.unfired, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: PotteryFiringRequest) {
        val recipe = recipes.forUnfired(request.unfired) ?: return
        with(access) {
            if (!isWithinDistance(request.oven, distance = 1) || !canFire(recipe, false)) {
                return
            }
            fire(recipe, request.oven.coords)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canFire(recipe, sendFailure = false)) {
                return
            }
            locAnim(worldRepo, request.oven, seqs.potteryoven_quick)
            weakQueue(
                crafting_queues.potteryFiring,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.fire(recipe: PotteryRecipe, coords: org.rsmod.map.CoordGrid) {
        val success = random.of(maxExclusive = SUCCESS_DENOMINATOR) < successThreshold()
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.unfired.id, 1)
                if (success) {
                    add(inventory, recipe.fired.id, 1)
                }
            }
        check(result.success) { "Pottery-firing transaction failed after validation: $recipe" }
        if (success) {
            statAdvance(stats.crafting, recipe.firingXp)
            player.recordDiaryPottery(recipe.fired.id, coords)
        } else {
            mes("The pottery cracks in the oven.")
        }
    }

    private fun ProtectedAccess.canFire(recipe: PotteryRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to fire this item.")
        }
        if (!player.inv.contains(recipe.unfired)) {
            return reject("You do not have the unfired pottery needed to make this item.")
        }
        return true
    }

    private fun ProtectedAccess.hasAnyUnfiredPottery(): Boolean =
        recipes.recipes.any { player.inv.contains(it.unfired) }

    private fun ProtectedAccess.successThreshold(): Int {
        val level = player.stat(stats.crafting).coerceIn(1, GUARANTEED_SUCCESS_LEVEL)
        return SUCCESS_THRESHOLDS[level - 1]
    }

    private companion object {
        const val ACTION_TICKS = 7
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val FIRE_ACTION_MODE = 0
        const val SUCCESS_DENOMINATOR = 256
        const val GUARANTEED_SUCCESS_LEVEL = 13
        const val MENU_TITLE = "What would you like to fire?"
        val SUCCESS_THRESHOLDS =
            intArrayOf(181, 187, 194, 200, 206, 213, 219, 225, 232, 238, 244, 251, 256)
    }
}

public data class PotteryFiringRequest(
    val oven: BoundLocInfo,
    val unfired: ObjType,
    val remaining: Int,
)
