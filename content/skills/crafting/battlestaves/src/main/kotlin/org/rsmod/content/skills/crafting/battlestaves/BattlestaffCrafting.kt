package org.rsmod.content.skills.crafting.battlestaves

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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class BattlestaffCrafting
@Inject
constructor(private val recipes: BattlestaffRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, orb: ObjType) {
        val recipe = recipes.forOrb(orb) ?: return
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            val maximum =
                minOf(
                    player.inv.count(objTypes[recipe.orb]),
                    player.inv.count(objTypes[objs.battlestaff]),
                )
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: BattlestaffRecipe, quantity: Int) {
        require(quantity >= 1) { "Battlestaff quantity must be positive." }
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            beginAction(recipe)
            weakQueue(
                crafting_queues.battlestaffCrafting,
                FIRST_ACTION_TICKS,
                BattlestaffCraftingRequest(recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: BattlestaffCraftingRequest) {
        val recipe = recipes.forOutput(request.output) ?: return
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
            beginAction(recipe)
            weakQueue(
                crafting_queues.battlestaffCrafting,
                REPEAT_ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction(recipe: BattlestaffRecipe) {
        anim(seqs.human_battlestaff_crafting)
        spotanim(recipe.spotanim)
    }

    private fun ProtectedAccess.make(recipe: BattlestaffRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.orb.id, 1)
                delete(inventory, objs.battlestaff.id, 1)
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Battlestaff transaction failed after validation: $recipe" }
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canMake(recipe: BattlestaffRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make battlestaves.")
        }
        if (player.inv.count(objTypes[recipe.orb]) == 0) {
            return reject("You need an elemental orb to make this battlestaff.")
        }
        if (player.inv.count(objTypes[objs.battlestaff]) == 0) {
            return reject("You need a battlestaff to attach the orb to.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        return true
    }

    private companion object {
        const val FIRST_ACTION_TICKS = 3
        const val REPEAT_ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "How many would you like to make?"
    }
}

public data class BattlestaffCraftingRequest(val output: ObjType, val remaining: Int)
