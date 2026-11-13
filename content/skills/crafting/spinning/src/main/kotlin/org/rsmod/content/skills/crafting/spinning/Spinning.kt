package org.rsmod.content.skills.crafting.spinning

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiaryCrossbowString
import org.rsmod.map.CoordGrid
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

public class Spinning
@Inject
constructor(private val recipes: SpinningRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(
        access: ProtectedAccess,
        wheel: BoundLocInfo,
        requestedInput: ObjType? = null,
    ) {
        with(access) {
            if (requestedInput != null && !recipes.isInput(requestedInput)) {
                return
            }
            if (!hasAnyInput()) {
                mes("You need something suitable to spin.")
                return
            }
            val maximum = recipes.recipes.maxOf { maximumActions(it) }.coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.recipes.map(SpinningRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(input.component) ?: return
            val material =
                requestedInput?.takeIf { it in recipe.inputs && player.inv.contains(it) }
                    ?: recipe.inputs.firstOrNull { player.inv.contains(it) }
                    ?: run {
                        mes("You do not have the material needed to make that.")
                        return
                    }
            start(this, wheel, recipe, material, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        wheel: BoundLocInfo,
        recipe: SpinningRecipe,
        material: ObjType,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Spinning quantity must be positive." }
        require(material in recipe.inputs) { "Unsupported material for $recipe: $material" }
        with(access) {
            if (!canSpin(recipe, material, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(seqs.human_spinningwheel)
            weakQueue(
                crafting_queues.spinning,
                ACTION_TICKS,
                SpinningRequest(wheel, recipe.output, material, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: SpinningRequest) {
        val recipe = recipes.recipes.firstOrNull { it.output == request.output } ?: return
        with(access) {
            if (
                !isWithinDistance(request.wheel, distance = 1) ||
                    !canSpin(recipe, request.material, sendFailure = false)
            ) {
                resetAnim()
                return
            }
            spin(recipe, request.material,request.wheel.coords)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canSpin(recipe, request.material, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(seqs.human_spinningwheel)
            weakQueue(
                crafting_queues.spinning,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.spin(recipe: SpinningRecipe, material: ObjType,wheel:CoordGrid) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, material.id, 1)
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Spinning transaction failed after validation: $recipe" }
        player.recordDiaryCrossbowString(recipe.output.id,wheel)
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canSpin(
        recipe: SpinningRecipe,
        material: ObjType,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to spin this material.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        if (player.inv.count(objTypes[material]) == 0) {
            return reject("You do not have the material needed to make that.")
        }
        return true
    }

    private fun ProtectedAccess.hasAnyInput(): Boolean =
        recipes.recipes.any { recipe -> recipe.inputs.any { player.inv.contains(it) } }

    private fun ProtectedAccess.maximumActions(recipe: SpinningRecipe): Int =
        recipe.inputs.maxOf { player.inv.count(objTypes[it]) }

    private companion object {
        const val ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to spin?"
    }
}

public data class SpinningRequest(
    val wheel: BoundLocInfo,
    val output: ObjType,
    val material: ObjType,
    val remaining: Int,
)
