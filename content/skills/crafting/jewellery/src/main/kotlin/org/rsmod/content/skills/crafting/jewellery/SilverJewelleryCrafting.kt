package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiaryCraft
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.smithing.smelting.FurnaceKind
import org.rsmod.content.skills.smithing.smelting.SHILO_FEE
import org.rsmod.content.skills.smithing.smelting.clearFurnaceQueues
import org.rsmod.content.skills.smithing.smelting.furnace_queues
import org.rsmod.content.skills.smithing.smelting.hasFreeShiloFurnace
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class SilverJewelleryCrafting
@Inject
constructor(private val recipes: SilverCraftingRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, furnace: BoundLocInfo, kind: FurnaceKind) {
        with(access) {
            if (kind == FurnaceKind.Lovakite) {
                mes(WRONG_FURNACE_MESSAGE)
                return
            }
            if (player.inv.count(objTypes[objs.silver_bar]) == 0) {
                mes("You need a silver bar to craft silver jewellery.")
                return
            }
            ifOpenMainModal(interfaces.silver_crafting)
            for (recipe in recipes.recipes) {
                ifSetEvents(recipe.component, 1..MAX_QUANTITY, IfEvent.PauseButton)
            }
            val input = pauseButton()
            val recipe = recipes.forComponent(input.component) ?: return
            if (input.subcomponent !in 1..MAX_QUANTITY) {
                return
            }
            ifClose()
            start(this, furnace, kind, recipe, input.subcomponent)
        }
    }

    public fun process(access: ProtectedAccess, request: SilverCraftingRequest) {
        with(access) {
            if (!isWithinDistance(request.furnace, distance = 1)) {
                resetAnim()
                return
            }
            val recipe = recipes.forOutput(request.output) ?: return
            if (!canCraft(recipe, request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            craft(recipe, request.kind, request.furnace.coords)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canCraft(recipe, request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(seqs.human_furnace)
            weakQueue(
                furnace_queues.silverJewellery,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun start(
        access: ProtectedAccess,
        furnace: BoundLocInfo,
        kind: FurnaceKind,
        recipe: SilverCraftingRecipe,
        quantity: Int,
    ) {
        with(access) {
            if (!canCraft(recipe, kind, sendFailure = true)) {
                return
            }
            clearFurnaceQueues()
            anim(seqs.human_furnace)
            weakQueue(
                furnace_queues.silverJewellery,
                ACTION_TICKS,
                SilverCraftingRequest(furnace, kind, recipe.output, quantity),
            )
        }
    }

    private fun ProtectedAccess.craft(recipe: SilverCraftingRecipe, kind: FurnaceKind, coords: org.rsmod.map.CoordGrid) {
        val fee = furnaceFee(kind)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                for (ingredient in recipe.ingredients) {
                    delete(inventory, ingredient.obj.id, ingredient.count)
                }
                if (fee > 0) {
                    delete(inventory, objs.coins.id, fee)
                }
                add(inventory, recipe.output.id, recipe.outputCount)
            }
        check(result.success) { "Silver-jewellery transaction failed after validation: $recipe" }
        statAdvance(stats.crafting, recipe.xp)
        player.recordDiaryCraft(recipe.output.id, coords)
    }

    private fun ProtectedAccess.canCraft(
        recipe: SilverCraftingRecipe,
        kind: FurnaceKind,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (kind == FurnaceKind.Lovakite) {
            return reject(WRONG_FURNACE_MESSAGE)
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to craft this item.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this item.")
        }
        if (player.inv.none { it?.id == recipe.mould.id }) {
            return reject(
                "You need a ${objTypes[recipe.mould].name.lowercase()} to make this item."
            )
        }
        for (ingredient in recipe.ingredients) {
            if (player.inv.count(objTypes[ingredient.obj]) < ingredient.count) {
                return reject(ingredient.missingMessage)
            }
        }
        if (furnaceFee(kind) > 0 && player.inv.count(objTypes[objs.coins]) < SHILO_FEE) {
            return reject("You need 20 coins to use this furnace.")
        }
        return true
    }

    private fun ProtectedAccess.furnaceFee(kind: FurnaceKind): Int =
        if (kind == FurnaceKind.ShiloVillage && !player.hasFreeShiloFurnace()) SHILO_FEE else 0

    private companion object {
        const val ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAX_QUANTITY = 28
        const val WRONG_FURNACE_MESSAGE =
            "This furnace is not at the right temperature to craft silver jewellery."
    }
}

public data class SilverCraftingRequest(
    val furnace: BoundLocInfo,
    val kind: FurnaceKind,
    val output: ObjType,
    val remaining: Int,
)
