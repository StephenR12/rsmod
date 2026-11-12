package org.rsmod.content.skills.crafting.jewellery

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

public class GemCutting
@Inject
constructor(private val recipes: GemCuttingRecipeRegistry, private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, uncut: ObjType) {
        val recipe = recipes.forUncut(uncut) ?: return
        with(access) {
            if (!canCut(recipe, sendFailure = true)) {
                return
            }
            clearHandcraftQueues()
            anim(recipe.animation)
            weakQueue(handcraft_queues.gemCutting, ACTION_TICKS, GemCuttingRequest(uncut))
        }
    }

    public fun process(access: ProtectedAccess, request: GemCuttingRequest) {
        val recipe = recipes.forUncut(request.uncut) ?: return
        with(access) {
            if (!canCut(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            cut(recipe)
            if (!canCut(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(recipe.animation)
            weakQueue(
                handcraft_queues.gemCutting,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request,
            )
        }
    }

    private fun ProtectedAccess.cut(recipe: GemCuttingRecipe) {
        val success =
            recipe.crush == null ||
                statRandom(stats.crafting, recipe.crush.low, recipe.crush.high, invisibleBoost = 0)
        val output = if (success) recipe.cut else objs.crushed_gem
        val xp = if (success) recipe.successXp else requireNotNull(recipe.crush).xp
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.uncut.id, 1)
                add(inventory, output.id, 1)
            }
        check(result.success) { "Gem-cutting transaction failed after validation: $recipe" }
        statAdvance(stats.crafting, xp)
        if (success) {
            mes("You cut the ${recipe.name.lowercase()}.")
        } else {
            mes("You accidentally crush the gem.")
        }
    }

    private fun ProtectedAccess.canCut(recipe: GemCuttingRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to cut this gem.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to cut this gem.")
        }
        if (player.inv.none { it?.id == objs.chisel.id }) {
            return reject("You need a chisel to cut this gem.")
        }
        if (player.inv.count(objTypes[recipe.uncut]) == 0) {
            return reject("You do not have any ${recipe.name.lowercase()} left to cut.")
        }
        return true
    }

    private companion object {
        const val ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
    }
}
