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

public class OneShotIngredientCrushing @Inject constructor(private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, slot: Int, recipe: OneShotIngredientCrushingRecipe) {
        with(access) {
            clearHerbloreQueues()
            if (!player.members) {
                mes("You need to be on a members' world to use this item.")
                return
            }
            val outputSlot = outputSlot(slot, recipe)
            if (
                !hasPestleAndMortar() || player.inv[slot]?.id != recipe.input.id || outputSlot < 0
            ) {
                return
            }
            anim(seqs.human_herbing_grind)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.input.id, 1, slot = slot)
                    add(inventory, recipe.output.id, 1, slot = outputSlot)
                }
            check(result.success) {
                "One-shot crushing transaction failed after validation: $recipe"
            }
        }
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    private fun ProtectedAccess.outputSlot(
        slot: Int,
        recipe: OneShotIngredientCrushingRecipe,
    ): Int {
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
}

public data class OneShotIngredientCrushingRecipe(val input: ObjType, val output: ObjType)

public object OneShotIngredientCrushingRecipes {
    public val all: List<OneShotIngredientCrushingRecipe> =
        listOf(
            OneShotIngredientCrushingRecipe(objs.dramen_branch, objs.cinnamon),
            OneShotIngredientCrushingRecipe(objs.tchiki_monkey_nuts, objs.tchiki_nut_paste),
            OneShotIngredientCrushingRecipe(objs.mysterious_meat, objs.mysterious_crushed_meat),
            OneShotIngredientCrushingRecipe(objs.cooked_karambwanji, objs.cooked_karambwanji_paste),
        )
}
