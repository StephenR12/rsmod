package org.rsmod.content.skills.crafting.leatheraddons

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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class LeatherAddonCrafting
@Inject
constructor(private val recipes: LeatherAddonRecipeRegistry, private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, recipe: LeatherAddonRecipe) {
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(seqs.human_leather_crafting)
            weakQueue(
                crafting_queues.leatherAddonCrafting,
                ACTION_TICKS,
                LeatherAddonCraftingRequest(recipe.output),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: LeatherAddonCraftingRequest) {
        val recipe = recipes.forOutput(request.output) ?: return
        with(access) {
            if (!canMake(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.base.id, 1)
                    delete(inventory, recipe.addon.id, 1)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) { "Leather add-on transaction failed after validation: $recipe" }
            statAdvance(stats.crafting, recipe.xp)
            resetAnim()
        }
    }

    private fun ProtectedAccess.canMake(recipe: LeatherAddonRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make this item.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        if (player.inv.count(objTypes[recipe.base]) == 0) {
            return reject("You do not have the armour needed to make that.")
        }
        if (player.inv.count(objTypes[recipe.addon]) == 0) {
            return reject("You do not have the add-on needed to make that.")
        }
        return true
    }

    private companion object {
        const val ACTION_TICKS = 3
    }
}

public data class LeatherAddonCraftingRequest(val output: ObjType)
