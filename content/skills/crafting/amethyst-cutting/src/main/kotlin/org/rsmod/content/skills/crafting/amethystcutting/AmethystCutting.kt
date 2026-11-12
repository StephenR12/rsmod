package org.rsmod.content.skills.crafting.amethystcutting

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

public class AmethystCutting
@Inject
constructor(private val recipes: AmethystCuttingRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess) {
        with(access) {
            if (!canOpen(sendFailure = true)) {
                return
            }
            val maximum = player.inv.count(objTypes[objs.amethyst])
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.recipes.map(AmethystCuttingRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(input.component) ?: return
            start(this, recipe, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: AmethystCuttingRecipe, quantity: Int) {
        require(quantity >= 1) { "Amethyst-cutting quantity must be positive." }
        with(access) {
            if (!canCut(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(seqs.human_amethystcutting)
            weakQueue(
                crafting_queues.amethystCutting,
                ACTION_TICKS,
                AmethystCuttingRequest(recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: AmethystCuttingRequest) {
        val recipe = recipes.find(request.output) ?: return
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
            anim(seqs.human_amethystcutting)
            weakQueue(
                crafting_queues.amethystCutting,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.cut(recipe: AmethystCuttingRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.amethyst.id, 1)
                add(inventory, recipe.output.id, recipe.outputCount)
            }
        check(result.success) { "Amethyst-cutting transaction failed after validation: $recipe" }
        statAdvance(stats.crafting, XP_PER_AMETHYST)
    }

    private fun ProtectedAccess.canOpen(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to cut amethyst.")
        }
        if (!player.inv.contains(objs.chisel)) {
            return reject("You need a chisel to cut amethyst.")
        }
        if (player.inv.count(objTypes[objs.amethyst]) == 0) {
            return reject("You do not have any amethyst left to cut.")
        }
        return true
    }

    private fun ProtectedAccess.canCut(
        recipe: AmethystCuttingRecipe,
        sendFailure: Boolean,
    ): Boolean {
        if (!canOpen(sendFailure)) {
            return false
        }
        if (player.stat(stats.crafting) < recipe.level) {
            if (sendFailure) {
                mes(
                    "You need a Crafting level of ${recipe.level} to make ${recipe.name.lowercase()}."
                )
            }
            return false
        }
        return true
    }

    private companion object {
        const val XP_PER_AMETHYST = 60.0
        const val ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to make?"
    }
}

public data class AmethystCuttingRequest(val output: ObjType, val remaining: Int)
