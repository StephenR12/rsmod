package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class IngredientCrushing @Inject constructor(private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, slot: Int, recipe: IngredientCrushingRecipe) {
        start(access, slot, recipe, objs.pestle_and_mortar)
    }

    public fun start(
        access: ProtectedAccess,
        slot: Int,
        recipe: IngredientCrushingRecipe,
        tool: ObjType,
    ) {
        with(access) {
            clearHerbloreQueues()
            if (!canCrush(slot, recipe, tool, sendFailure = true)) {
                return
            }
            anim(seqs.human_herbing_grind)
            crush(slot, recipe)
            scheduleNext(recipe, tool, INITIAL_AUTO_CRUSH_TICKS)
        }
    }

    public fun process(access: ProtectedAccess, request: IngredientCrushingRequest) {
        val recipe = IngredientCrushingRecipes.byInput[request.input.id] ?: return
        with(access) {
            val slot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            if (slot < 0 || !canCrush(slot, recipe, request.tool, sendFailure = false)) {
                return
            }
            anim(seqs.human_herbing_grind_restart)
            crush(slot, recipe)
            scheduleNext(recipe, request.tool, REPEAT_AUTO_CRUSH_TICKS)
        }
    }

    private fun ProtectedAccess.crush(slot: Int, recipe: IngredientCrushingRecipe) {
        val outputSlot = outputSlot(slot, recipe)
        check(outputSlot >= 0) { "Ingredient-crushing output capacity changed after validation." }
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.input.id, 1, slot = slot)
                add(inventory, recipe.output.id, 1, slot = outputSlot)
            }
        check(result.success) { "Ingredient-crushing transaction failed after validation: $recipe" }
    }

    private fun ProtectedAccess.scheduleNext(
        recipe: IngredientCrushingRecipe,
        tool: ObjType,
        delay: Int,
    ) {
        if (hasTool(tool) && player.inv.any { it?.id == recipe.input.id }) {
            weakQueue(
                herblore_queues.ingredientCrushing,
                delay,
                IngredientCrushingRequest(recipe.input, tool),
            )
        }
    }

    private fun ProtectedAccess.canCrush(
        slot: Int,
        recipe: IngredientCrushingRecipe,
        tool: ObjType,
        sendFailure: Boolean,
    ): Boolean {
        if (tool.id == objs.pestle_and_mortar.id && !player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to use this item.")
            }
            return false
        }
        if (!hasTool(tool)) {
            return false
        }
        return player.inv[slot]?.id == recipe.input.id && outputSlot(slot, recipe) >= 0
    }

    private fun ProtectedAccess.outputSlot(slot: Int, recipe: IngredientCrushingRecipe): Int {
        val selected = player.inv[slot] ?: return -1
        if (objTypes[recipe.output].stackable) {
            val existing = player.inv.indexOfFirst { it?.id == recipe.output.id }
            if (existing >= 0) {
                return existing
            }
        }
        if (selected.count == 1) {
            return slot
        }
        return player.inv.indexOfFirst { it == null }
    }

    private fun ProtectedAccess.hasTool(tool: ObjType): Boolean =
        player.inv.any { it?.id == tool.id }

    private companion object {
        const val INITIAL_AUTO_CRUSH_TICKS = 2
        // Queue handlers consume one cycle while running, so three-tick repeats need compensation.
        const val REPEAT_AUTO_CRUSH_TICKS = 4
    }
}

public data class IngredientCrushingRequest(val input: ObjType, val tool: ObjType)
