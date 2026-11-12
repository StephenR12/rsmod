package org.rsmod.content.skills.crafting.leatherworking

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
import org.rsmod.content.skills.crafting.commons.LeatherThreadUsageRepository
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class Leatherworking
@Inject
constructor(
    private val recipes: LeatherworkingRecipeRegistry,
    private val objTypes: ObjTypeList,
    private val threadUses: LeatherThreadUsageRepository,
) {

    public suspend fun select(access: ProtectedAccess, material: ObjType, costumeNeedle: Boolean) {
        with(access) {
            val choices = recipes.forMaterial(material)
            if (choices.isEmpty() || !canOpen(material, costumeNeedle, sendFailure = true)) {
                return
            }
            val maximum = choices.maxOf { maximumActions(it) }.coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = choices.map(LeatherworkingRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val recipe = recipes.forButton(material, input.component) ?: return
            start(this, recipe, costumeNeedle, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: LeatherworkingRecipe,
        costumeNeedle: Boolean,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Leatherworking quantity must be positive." }
        with(access) {
            if (!canMake(recipe, costumeNeedle, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            beginAction()
            weakQueue(
                crafting_queues.leatherworking,
                ACTION_TICKS,
                LeatherworkingRequest(recipe.output, costumeNeedle, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: LeatherworkingRequest) {
        val recipe = recipes.recipes.firstOrNull { it.output == request.output } ?: return
        with(access) {
            if (!canMake(recipe, request.costumeNeedle, sendFailure = false)) {
                resetAnim()
                return
            }
            make(recipe, request.costumeNeedle)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMake(recipe, request.costumeNeedle, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction()
            weakQueue(
                crafting_queues.leatherworking,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_leather_crafting)
    }

    private fun ProtectedAccess.make(recipe: LeatherworkingRecipe, costumeNeedle: Boolean) {
        val consumeThread = !costumeNeedle && threadUses.consumesThread(player)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.material.id, recipe.materialCount)
                if (consumeThread) {
                    delete(inventory, objs.thread.id, 1)
                }
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Leatherworking transaction failed after validation: $recipe" }
        if (!costumeNeedle) {
            threadUses.completeUse(player)
        }
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canOpen(
        material: ObjType,
        costumeNeedle: Boolean,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        val needle = if (costumeNeedle) objs.costume_needle else objs.needle
        if (player.inv.none { it?.id == needle.id }) {
            return reject("You need a needle to work with leather.")
        }
        if (!costumeNeedle && player.inv.none { it?.id == objs.thread.id }) {
            return reject("You need some thread to work with leather.")
        }
        if (player.inv.count(objTypes[material]) == 0) {
            return reject("You do not have the leather needed to make that.")
        }
        return true
    }

    private fun ProtectedAccess.canMake(
        recipe: LeatherworkingRecipe,
        costumeNeedle: Boolean,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to craft dragonhide armour.")
        }
        if (costumeNeedle && !player.members) {
            return reject("You need to be on a members' world to use the costume needle.")
        }
        if (!canOpen(recipe.material, costumeNeedle, sendFailure)) {
            return false
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        if (player.inv.count(objTypes[recipe.material]) < recipe.materialCount) {
            return reject("You do not have enough leather to make that.")
        }
        return true
    }

    private fun ProtectedAccess.maximumActions(recipe: LeatherworkingRecipe): Int =
        player.inv.count(objTypes[recipe.material]) / recipe.materialCount

    private companion object {
        const val ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to make?"
    }
}

public data class LeatherworkingRequest(
    val output: ObjType,
    val costumeNeedle: Boolean,
    val remaining: Int,
)
