package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class CannonballSmithing
@Inject
constructor(private val recipes: CannonballRecipeRegistry, private val objTypes: ObjTypeList) {
    public suspend fun select(
        access: ProtectedAccess,
        furnace: BoundLocInfo,
        kind: FurnaceKind,
        requested: ObjType,
    ) {
        with(access) {
            if (kind == FurnaceKind.Lovakite) {
                mes(WRONG_FURNACE_MESSAGE)
                return
            }
            val recipe = chooseRecipe(player, requested)
            if (recipe == null) {
                mes("You need an ammo mould to make cannonballs.")
                return
            }
            if (!canMake(recipe, kind, sendFailure = true)) {
                return
            }
            val maximum = maximumActions(recipe, kind)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(objs.cannonball),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = SMELT_ACTION_MODE,
                )
            if (!input.component.isType(org.rsmod.api.config.refs.components.skillmulti_a)) {
                return
            }
            start(this, furnace, kind, recipe, input.subcomponent)
        }
    }

    public fun process(access: ProtectedAccess, request: CannonballSmithingRequest) {
        with(access) {
            if (!isWithinDistance(request.furnace, distance = 1)) {
                resetAnim()
                return
            }
            val recipe = recipes.forMould(request.mould) ?: return
            if (!canMake(recipe, request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            make(recipe, request.kind)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMake(recipe, request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction(recipe)
            weakQueue(
                furnace_queues.cannonballs,
                REPEAT_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun start(
        access: ProtectedAccess,
        furnace: BoundLocInfo,
        kind: FurnaceKind,
        recipe: CannonballRecipe,
        quantity: Int,
    ) {
        with(access) {
            require(quantity >= 1)
            if (!canMake(recipe, kind, sendFailure = true)) {
                return
            }
            clearFurnaceQueues()
            beginAction(recipe)
            weakQueue(
                furnace_queues.cannonballs,
                INITIAL_TICKS,
                CannonballSmithingRequest(furnace, kind, recipe.mould, quantity),
            )
        }
    }

    private fun ProtectedAccess.beginAction(recipe: CannonballRecipe) {
        anim(seqs.human_smith_cannonball)
        spam("The molten metal cools slowly to form ${recipe.outputCount} cannonballs.")
    }

    private fun ProtectedAccess.make(recipe: CannonballRecipe, kind: FurnaceKind) {
        val fee = furnaceFee(player, kind)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.steel_bar.id, recipe.bars)
                if (fee > 0) {
                    delete(inventory, objs.coins.id, fee)
                }
                add(inventory, objs.cannonball.id, recipe.outputCount)
            }
        check(result.success) { "Cannonball transaction failed after validation: $recipe" }
        statAdvance(stats.smithing, recipe.xp)
        spam("You remove the cannonballs from the mould")
    }

    private fun ProtectedAccess.canMake(
        recipe: CannonballRecipe,
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
        if (!player.members) {
            return reject("You need to be on a members' world to make cannonballs.")
        }
        if (player.vars[varps.mcannon] < DWARF_CANNON_MOULD_PROGRESS) {
            return reject("You need to progress further in Dwarf Cannon to make cannonballs.")
        }
        if (player.stat(stats.smithing) < SMITHING_LEVEL) {
            return reject("You need a Smithing level of 35 to make cannonballs.")
        }
        if (player.inv.none { it?.id == recipe.mould.id }) {
            return reject("You need the correct ammo mould to make these cannonballs.")
        }
        if (player.inv.count(objTypes[objs.steel_bar]) < recipe.bars) {
            return reject("You do not have enough steel bars to make cannonballs.")
        }
        val fee = furnaceFee(player, kind)
        if (fee > 0 && player.inv.count(objTypes[objs.coins]) < fee) {
            return reject("You need 20 coins to use this furnace.")
        }
        return true
    }

    private fun ProtectedAccess.maximumActions(recipe: CannonballRecipe, kind: FurnaceKind): Int {
        val bars = player.inv.count(objTypes[objs.steel_bar]) / recipe.bars
        val fee = furnaceFee(player, kind)
        if (fee == 0) {
            return bars
        }
        val paid = player.inv.count(objTypes[objs.coins]) / fee
        return min(bars, paid)
    }

    private fun chooseRecipe(player: Player, requested: ObjType): CannonballRecipe? {
        recipes.forMould(requested)?.let {
            return it
        }
        if (requested.id != objs.steel_bar.id) {
            return null
        }
        val hasDouble = player.inv.any { it?.id == recipes.double.mould.id }
        val hasRegular = player.inv.any { it?.id == recipes.regular.mould.id }
        val bars = player.inv.count(objTypes[objs.steel_bar])
        return when {
            hasDouble && bars >= recipes.double.bars -> recipes.double
            hasRegular -> recipes.regular
            hasDouble -> recipes.double
            else -> null
        }
    }

    private fun furnaceFee(player: Player, kind: FurnaceKind): Int =
        if (kind == FurnaceKind.ShiloVillage && !player.hasFreeShiloFurnace()) SHILO_FEE else 0

    private companion object {
        const val SMITHING_LEVEL = 35
        const val DWARF_CANNON_MOULD_PROGRESS = 10
        const val SMELT_ACTION_MODE = 13
        const val INITIAL_TICKS = 8
        const val REPEAT_TICKS = 10
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MENU_TITLE = "How many would you like to make?"
        const val WRONG_FURNACE_MESSAGE =
            "This furnace is not at the right temperature to make cannonballs."
    }
}

public data class CannonballSmithingRequest(
    val furnace: BoundLocInfo,
    val kind: FurnaceKind,
    val mould: ObjType,
    val remaining: Int,
)
