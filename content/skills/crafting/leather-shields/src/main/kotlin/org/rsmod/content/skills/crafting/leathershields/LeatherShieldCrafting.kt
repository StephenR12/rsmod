package org.rsmod.content.skills.crafting.leathershields

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
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

public class LeatherShieldCrafting
@Inject
constructor(private val recipes: LeatherShieldRecipeRegistry, private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, recipe: LeatherShieldRecipe) {
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(recipe.animation)
            weakQueue(
                crafting_queues.leatherShieldCrafting,
                ACTION_TICKS,
                LeatherShieldCraftingRequest(recipe.output),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: LeatherShieldCraftingRequest) {
        val recipe = recipes.forOutput(request.output) ?: return
        with(access) {
            if (!canMake(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.leather.id, LEATHER_COUNT)
                    delete(inventory, recipe.base.id, 1)
                    delete(inventory, recipe.nails.id, NAIL_COUNT)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) { "Leather shield transaction failed after validation: $recipe" }
            statAdvance(stats.crafting, recipe.xp)
            resetAnim()
        }
    }

    private fun ProtectedAccess.canMake(
        recipe: LeatherShieldRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make this shield.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this shield.")
        }
        if (!hasHammer()) {
            return reject("You need a hammer to make this shield.")
        }
        if (player.inv.count(objTypes[recipe.leather]) < LEATHER_COUNT) {
            return reject("You need two pieces of the correct leather to make this shield.")
        }
        if (player.inv.count(objTypes[recipe.base]) == 0) {
            return reject("You need the correct wooden shield to make this shield.")
        }
        if (player.inv.count(objTypes[recipe.nails]) < NAIL_COUNT) {
            return reject("You need 15 of the correct nails to make this shield.")
        }
        return true
    }

    private fun ProtectedAccess.hasHammer(): Boolean {
        val valid = setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
        return player.inv.any { it?.id in valid } || player.worn.any { it?.id in valid }
    }

    private companion object {
        const val ACTION_TICKS = 5
        const val LEATHER_COUNT = 2
        const val NAIL_COUNT = 15
    }
}

public data class LeatherShieldCraftingRequest(val output: ObjType)
