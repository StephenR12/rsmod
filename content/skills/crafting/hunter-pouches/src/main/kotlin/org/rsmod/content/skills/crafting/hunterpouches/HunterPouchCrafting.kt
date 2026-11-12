package org.rsmod.content.skills.crafting.hunterpouches

import jakarta.inject.Inject
import kotlin.math.min
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

public class HunterPouchCrafting
@Inject
constructor(
    private val recipes: HunterPouchRecipeRegistry,
    private val objTypes: ObjTypeList,
    private val threadUses: LeatherThreadUsageRepository,
) {
    public suspend fun select(access: ProtectedAccess, fur: ObjType, costumeNeedle: Boolean) {
        val recipe = recipes.forFur(fur) ?: return
        with(access) {
            if (!canMake(recipe, costumeNeedle, sendFailure = true)) {
                return
            }
            val maximum = maximumActions(recipe, costumeNeedle)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, costumeNeedle, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: HunterPouchRecipe,
        costumeNeedle: Boolean,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Hunter pouch quantity must be positive." }
        with(access) {
            if (!canMake(recipe, costumeNeedle, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            beginAction()
            weakQueue(
                crafting_queues.hunterPouchCrafting,
                ACTION_TICKS,
                HunterPouchCraftingRequest(recipe.output, costumeNeedle, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: HunterPouchCraftingRequest) {
        val recipe = recipes.forOutput(request.output) ?: return
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
                crafting_queues.hunterPouchCrafting,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_leather_crafting)
    }

    private fun ProtectedAccess.make(recipe: HunterPouchRecipe, costumeNeedle: Boolean) {
        val consumeThread = !costumeNeedle && threadUses.consumesThread(player)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.fur.id, recipe.furCount)
                recipe.pouch?.let { delete(inventory, it.id, 1) }
                if (consumeThread) {
                    delete(inventory, objs.thread.id, 1)
                }
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Hunter pouch transaction failed after validation: $recipe" }
        if (!costumeNeedle) {
            threadUses.completeUse(player)
        }
        statAdvance(stats.crafting, recipe.xp)
    }

    private fun ProtectedAccess.canMake(
        recipe: HunterPouchRecipe,
        costumeNeedle: Boolean,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make Hunter pouches.")
        }
        val needle = if (costumeNeedle) objs.costume_needle else objs.needle
        if (!player.inv.contains(needle)) {
            return reject("You need a needle to make this pouch.")
        }
        if (!costumeNeedle && !player.inv.contains(objs.thread)) {
            return reject("You need some thread to make this pouch.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this pouch.")
        }
        if (player.inv.count(objTypes[recipe.fur]) < recipe.furCount) {
            return reject("You do not have enough of the correct fur to make this pouch.")
        }
        val pouch = recipe.pouch
        if (pouch != null && player.inv.count(objTypes[pouch]) == 0) {
            return reject("You need the correct closed pouch to make this upgrade.")
        }
        return true
    }

    private fun ProtectedAccess.maximumActions(
        recipe: HunterPouchRecipe,
        costumeNeedle: Boolean,
    ): Int {
        val furActions = player.inv.count(objTypes[recipe.fur]) / recipe.furCount
        val pouchActions = recipe.pouch?.let { player.inv.count(objTypes[it]) } ?: Int.MAX_VALUE
        val materialActions = min(furActions, pouchActions)
        if (costumeNeedle) {
            return materialActions
        }
        val availableThreadUses =
            player.inv.count(objTypes[objs.thread]) * THREAD_USES_PER_SPOOL -
                threadUses.uses(player)
        return min(materialActions, availableThreadUses)
    }

    private companion object {
        const val ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val THREAD_USES_PER_SPOOL = 5
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "What would you like to make?"
    }
}

public data class HunterPouchCraftingRequest(
    val output: ObjType,
    val costumeNeedle: Boolean,
    val remaining: Int,
)
