package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiarySmelt
import kotlin.math.min
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.torso
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class FurnaceSmelting
@Inject
constructor(
    private val recipes: SmeltingRecipeRegistry,
    private val objTypes: ObjTypeList,
    private val ringCharges: RingOfForgingRepository,
) {
    public suspend fun select(
        access: ProtectedAccess,
        furnace: org.rsmod.game.loc.BoundLocInfo,
        kind: FurnaceKind,
        requestedOre: ObjType? = null,
    ) {
        with(access) {
            val choices = menuChoices(kind, requestedOre)
            if (choices.isEmpty()) {
                mes(WRONG_TEMPERATURE_MESSAGE)
                return
            }
            val maxQuantity = choices.maxOf { maximumActions(it, kind) }.coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = choices.map(SmeltingRecipe::output),
                    maxQuantity = maxQuantity,
                    initialQuantity = maxQuantity,
                    actionMode = SMELT_ACTION_MODE,
                )
            val recipe = choices.getOrNull(buttonIndex(input.component)) ?: return
            start(this, furnace, kind, recipe, input.subcomponent)
        }
    }

    public fun process(access: ProtectedAccess, request: FurnaceSmeltingRequest) {
        with(access) {
            if (!isWithinDistance(request.furnace, distance = 1)) {
                resetAnim()
                return
            }
            val recipe = recipes.forOutput(request.output) ?: return
            val outcome = smelt(this, recipe, request.kind, request.remaining, sendFailure = false)
            if (outcome.performed) player.recordDiarySmelt(recipe.output.id, request.furnace.coords)
            if (!outcome.performed || outcome.stop || request.remaining <= outcome.units) {
                resetAnim()
                return
            }
            val remaining = request.remaining - outcome.units
            if (!canSmelt(recipe, request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(seqs.human_furnace)
            weakQueue(
                furnace_queues.smelting,
                recipe.furnaceRepeatTicks + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    public fun checkRing(access: ProtectedAccess) {
        access.mes(
            "Your ring of forging has ${ringCharges.remaining(access.player)} charges remaining."
        )
    }

    public fun breakRing(access: ProtectedAccess, slot: Int) {
        with(access) {
            val ring = player.inv[slot] ?: return
            if (ring.id != objs.ring_of_forging.id) {
                return
            }
            player.inv[slot] = null
            ringCharges.reset(player)
            mes("You break the ring and reset your next ring of forging's charges.")
        }
    }

    private fun start(
        access: ProtectedAccess,
        furnace: org.rsmod.game.loc.BoundLocInfo,
        kind: FurnaceKind,
        recipe: SmeltingRecipe,
        quantity: Int,
    ) {
        with(access) {
            require(quantity >= 1)
            if (!canSmelt(recipe, kind, sendFailure = true)) {
                return
            }
            clearFurnaceQueues()
            anim(seqs.human_furnace)
            weakQueue(
                furnace_queues.smelting,
                recipe.furnaceInitialTicks,
                FurnaceSmeltingRequest(furnace, kind, recipe.output, quantity),
            )
        }
    }

    private fun smelt(
        access: ProtectedAccess,
        recipe: SmeltingRecipe,
        kind: FurnaceKind,
        remaining: Int,
        sendFailure: Boolean,
    ): SmeltOutcome {
        with(access) {
            if (!canSmelt(recipe, kind, sendFailure)) {
                return SmeltOutcome(performed = false)
            }

            val wearingRing =
                recipe.output.id == objs.iron_bar.id && player.ring?.id == objs.ring_of_forging.id
            val success =
                wearingRing || recipe.output.id != objs.iron_bar.id || random.of(0, 1) == 1
            if (!success) {
                val failurePlan =
                    planUnits(recipe, kind, units = 1, produceBars = false)
                        ?: return SmeltOutcome(false)
                commit(recipe, failurePlan)
                mes(IRON_FAILURE_MESSAGE)
                return SmeltOutcome(performed = true, units = 1)
            }

            val double =
                remaining >= 2 &&
                    kind == FurnaceKind.Edgeville &&
                    player.hasVarrockDoubleEffect(recipe) &&
                    (!wearingRing || ringCharges.remaining(player) >= 2) &&
                    random.of(1, 10) == 1 &&
                    planUnits(recipe, kind, units = 2, produceBars = true) != null
            val units = if (double) 2 else 1
            val plan = checkNotNull(planUnits(recipe, kind, units, produceBars = true))
            commit(recipe, plan)
            statAdvance(stats.smithing, plan.unitXp.sum())
            spam(
                "You retrieve ${if (units == 1) "a bar" else "$units bars"} of ${barMetal(recipe)}."
            )

            var stop = false
            if (wearingRing && ringCharges.consume(player, units)) {
                player.ring = null
                mes("Your ring of forging has melted.")
                stop = true
            }
            return SmeltOutcome(performed = true, units = units, stop = stop)
        }
    }

    private fun ProtectedAccess.commit(recipe: SmeltingRecipe, plan: SmeltingPlan) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                for ((ore, count) in recipe.oreCosts) {
                    delete(inventory, ore.id, count * plan.units)
                }
                if (plan.looseCoal > 0) {
                    delete(inventory, objs.coal.id, plan.looseCoal)
                }
                if (plan.catalysts > 0) {
                    delete(inventory, objs.smithing_catalyst.id, plan.catalysts)
                }
                if (plan.coins > 0) {
                    delete(inventory, objs.coins.id, plan.coins)
                }
                if (plan.produceBars) {
                    add(inventory, recipe.output.id, plan.units)
                }
            }
        check(result.success) { "Furnace-smelting transaction failed after validation: $plan" }
        if (plan.bagCoal > 0) {
            vars[varbits.coal_bag_storage_count] -= plan.bagCoal
        }
    }

    private fun ProtectedAccess.canSmelt(
        recipe: SmeltingRecipe,
        kind: FurnaceKind,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to smelt this ore.")
        }
        if (recipe.output.id == objs.lovakite_bar.id && kind != FurnaceKind.Lovakite) {
            return reject(WRONG_TEMPERATURE_MESSAGE)
        }
        if (!player.isUnlocked(recipe)) {
            return reject(
                "You need to complete ${recipe.unlock?.questName} before you can smelt this ore."
            )
        }
        if (player.stat(stats.smithing) < recipe.level) {
            return reject("You need a Smithing level of ${recipe.level} to smelt this ore.")
        }
        val missingOre =
            recipe.oreCosts.entries.firstOrNull { (ore, count) ->
                player.inv.count(objTypes[ore]) < count
            }
        if (missingOre != null) {
            return reject("You do not have enough ore to make a ${recipe.name.lowercase()}.")
        }
        val catalysed = recipe.catalystEligible && objs.smithing_catalyst in player.inv
        if (player.availableCoal() < recipe.coalCount(catalysed)) {
            return reject("You do not have enough coal to smelt this ore.")
        }
        if (kind == FurnaceKind.ShiloVillage && !player.hasFreeShiloFurnace()) {
            if (player.inv.count(objTypes[objs.coins]) < SHILO_FEE) {
                return reject("You need 20 coins to use this furnace.")
            }
        }
        return planUnits(recipe, kind, units = 1, produceBars = true) != null
    }

    private fun ProtectedAccess.planUnits(
        recipe: SmeltingRecipe,
        kind: FurnaceKind,
        units: Int,
        produceBars: Boolean,
    ): SmeltingPlan? {
        for ((ore, count) in recipe.oreCosts) {
            if (player.inv.count(objTypes[ore]) < count * units) {
                return null
            }
        }
        val fee =
            if (kind == FurnaceKind.ShiloVillage && !player.hasFreeShiloFurnace()) SHILO_FEE else 0
        val coins = fee * units
        if (player.inv.count(objTypes[objs.coins]) < coins) {
            return null
        }

        var looseCoal = player.inv.count(objTypes[objs.coal])
        var bagCoal = player.availableBagCoal()
        var catalysts = player.inv.count(objTypes[objs.smithing_catalyst])
        var looseUsed = 0
        var bagUsed = 0
        var catalystsUsed = 0
        val unitXp = ArrayList<Double>(units)
        repeat(units) {
            val catalysed = recipe.catalystEligible && catalysts > 0
            val coalNeeded = recipe.coalCount(catalysed)
            if (looseCoal + bagCoal < coalNeeded) {
                return null
            }
            val fromLoose = min(coalNeeded, looseCoal)
            val fromBag = coalNeeded - fromLoose
            looseCoal -= fromLoose
            bagCoal -= fromBag
            looseUsed += fromLoose
            bagUsed += fromBag
            if (catalysed) {
                catalysts--
                catalystsUsed++
            }
            unitXp += smithingXp(recipe, catalysed)
        }
        return SmeltingPlan(
            units = units,
            looseCoal = looseUsed,
            bagCoal = bagUsed,
            catalysts = catalystsUsed,
            coins = coins,
            unitXp = unitXp,
            produceBars = produceBars,
        )
    }

    private fun ProtectedAccess.maximumActions(recipe: SmeltingRecipe, kind: FurnaceKind): Int {
        var count = 0
        while (planUnits(recipe, kind, count + 1, produceBars = true) != null) {
            count++
        }
        return count
    }

    private fun ProtectedAccess.menuChoices(
        kind: FurnaceKind,
        requestedOre: ObjType?,
    ): List<SmeltingRecipe> {
        if (requestedOre != null) {
            val special =
                recipes.forTarget(requestedOre).singleOrNull { recipe -> !recipe.standardMenu }
            if (special != null) {
                return if (
                    special.output.id == objs.lovakite_bar.id && kind != FurnaceKind.Lovakite
                ) {
                    emptyList()
                } else {
                    listOf(special)
                }
            }
        }
        return if (kind == FurnaceKind.Lovakite) {
            listOfNotNull(recipes.forOutput(objs.lovakite_bar))
        } else {
            recipes.standardMenu
        }
    }

    private fun Player.availableCoal(): Int = inv.count(objTypes[objs.coal]) + availableBagCoal()

    private fun Player.availableBagCoal(): Int {
        val hasBag = inv.any { it?.id == objs.coal_bag.id || it?.id == objs.coal_bag_open.id }
        return if (hasBag) vars[varbits.coal_bag_storage_count] else 0
    }

    private fun Player.isUnlocked(recipe: SmeltingRecipe): Boolean =
        when (val unlock = recipe.unlock) {
            null -> true
            is SmeltingUnlock.Varp -> vars[unlock.type] >= unlock.minimumProgress
            is SmeltingUnlock.VarBit -> vars[unlock.type] >= unlock.minimumProgress
        }

    private fun Player.hasVarrockDoubleEffect(recipe: SmeltingRecipe): Boolean {
        if (!recipe.standardMenu) {
            return false
        }
        val maximumLevel =
            when (torso?.id) {
                objs.diary_varrock_armour_1.id -> 30
                objs.diary_varrock_armour_2.id -> 50
                objs.diary_varrock_armour_3.id -> 70
                objs.diary_varrock_armour_4.id -> 85
                else -> return false
            }
        return recipe.level <= maximumLevel
    }

    private fun Player.hasGoldXpEquipment(): Boolean {
        val gloves = hands?.id == objs.goldsmith_gauntlets.id
        val cape =
            back?.id in
                setOf(
                    objs.smithing_skillcape.id,
                    objs.smithing_skillcape_t.id,
                    objs.max_cape_worn.id,
                )
        return gloves || cape
    }

    private fun ProtectedAccess.smithingXp(recipe: SmeltingRecipe, catalysed: Boolean): Double {
        val base =
            if (recipe.output.id == objs.gold_bar.id && player.hasGoldXpEquipment()) {
                GOLDSMITH_XP
            } else {
                recipe.xp
            }
        return if (catalysed && recipe.catalystEligible) base * 2 else base
    }

    private fun buttonIndex(component: ComponentType): Int =
        BUTTONS.indexOfFirst { button -> button.isType(component) }

    private fun barMetal(recipe: SmeltingRecipe): String =
        recipe.name.removeSuffix(" bar").lowercase()

    private data class SmeltingPlan(
        val units: Int,
        val looseCoal: Int,
        val bagCoal: Int,
        val catalysts: Int,
        val coins: Int,
        val unitXp: List<Double>,
        val produceBars: Boolean,
    )

    private data class SmeltOutcome(
        val performed: Boolean,
        val units: Int = 0,
        val stop: Boolean = false,
    )

    private companion object {
        const val SMELT_ACTION_MODE = 13
        const val GOLDSMITH_XP = 56.2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MENU_TITLE = "What would you like to smelt?"
        const val WRONG_TEMPERATURE_MESSAGE =
            "This furnace is not at the right temperature to smelt lovakite ore."
        const val IRON_FAILURE_MESSAGE = "The ore is too impure and you fail to refine it."
        val BUTTONS =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
                components.skillmulti_e,
                components.skillmulti_f,
                components.skillmulti_g,
                components.skillmulti_h,
                components.skillmulti_i,
                components.skillmulti_j,
            )
    }
}
