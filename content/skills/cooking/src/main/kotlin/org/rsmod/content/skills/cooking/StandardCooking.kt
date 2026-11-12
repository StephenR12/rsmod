package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class StandardCooking
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val karambwan: KarambwanCooking,
    private val sinew: SinewCooking,
) {
    public suspend fun select(
        access: ProtectedAccess,
        source: BoundLocInfo,
        kind: CookingSourceKind,
        requestedRaw: ObjType? = null,
    ) {
        with(access) {
            if (!canUseSource(kind, sendFailure = true)) {
                return
            }
            val standardCandidates =
                if (requestedRaw == null) {
                    StandardCookingRecipes.all.filter {
                        kind in it.allowedSources && player.inv.contains(it.raw)
                    }
                } else {
                    listOfNotNull(StandardCookingRecipes.byRaw[requestedRaw.id]).filter {
                        kind in it.allowedSources && player.inv.contains(it.raw)
                    }
                }
            val sinewCandidates =
                sinew.availableInputs(player, kind, requestedRaw).map { CookingSelection.Sinew(it) }
            val candidates =
                standardCandidates.map { CookingSelection.Standard(it) } +
                    sinewCandidates +
                    if (requestedRaw == null && player.inv.contains(objs.raw_karambwan)) {
                        karambwan.availableProducts(player).map { CookingSelection.Karambwan(it) }
                    } else {
                        emptyList()
                    }
            if (candidates.isEmpty()) {
                mes("You don't have anything suitable to cook.")
                return
            }
            if (
                requestedRaw != null &&
                    player.inv.count(objTypes[requestedRaw]) == 1 &&
                    candidates.size == 1
            ) {
                start(
                    this,
                    source,
                    kind,
                    (candidates.single() as CookingSelection.Standard).recipe,
                    quantity = 1,
                )
                return
            }
            val presented =
                if (requestedRaw == null) {
                    candidates.distinctBy { it.output.id }.take(SKILL_MULTI_BUTTONS.size)
                } else {
                    candidates
                }
            val maximum = presented.maxOf { player.inv.count(objTypes[it.raw]) }
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = presented.map(CookingSelection::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = COOK_ACTION_MODE,
                )
            val index = SKILL_MULTI_BUTTONS.indexOfFirst { it.isType(input.component) }
            val selection = presented.getOrNull(index) ?: return
            if (requestedRaw != null && selection.raw != requestedRaw) {
                return
            }
            when (selection) {
                is CookingSelection.Standard ->
                    start(this, source, kind, selection.recipe, input.subcomponent)
                is CookingSelection.Karambwan ->
                    karambwan.start(this, source, kind, selection.product, input.subcomponent)
                is CookingSelection.Sinew ->
                    sinew.start(this, source, kind, selection.raw, input.subcomponent)
            }
        }
    }

    public fun start(
        access: ProtectedAccess,
        source: BoundLocInfo,
        kind: CookingSourceKind,
        recipe: StandardCookingRecipe,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Cooking quantity must be positive." }
        with(access) {
            if (!canCook(recipe, kind, sendFailure = true)) {
                return
            }
            clearCookingQueues()
            beginAction(kind)
            val request = StandardCookingRequest(source, kind, recipe.raw, quantity, completed = 0)
            if (recipe.firstResultTicks == 0) {
                process(this, request, compensateQueueHandler = false)
            } else {
                weakQueue(cooking_queues.standardFood, recipe.firstResultTicks, request)
            }
        }
    }

    public fun process(access: ProtectedAccess, request: StandardCookingRequest) {
        process(access, request, compensateQueueHandler = true)
    }

    private fun process(
        access: ProtectedAccess,
        request: StandardCookingRequest,
        compensateQueueHandler: Boolean,
    ) {
        val recipe = StandardCookingRecipes.byRaw[request.raw.id] ?: return
        with(access) {
            if (
                !isWithinDistance(request.source, distance = 1) ||
                    !canCook(
                        recipe,
                        request.kind,
                        sendFailure = false,
                        checkLevel = recipe.maintainLevelDuringAction,
                    )
            ) {
                resetAnim()
                return
            }
            cook(recipe, request.kind)
            val remaining = request.remaining - 1
            if (
                remaining <= 0 ||
                    !canCook(
                        recipe,
                        request.kind,
                        sendFailure = false,
                        checkLevel = recipe.maintainLevelDuringAction,
                    )
            ) {
                resetAnim()
                return
            }
            beginAction(request.kind)
            val nextCompleted = request.completed + 1
            val resultGap =
                if (nextCompleted == 1) {
                    recipe.secondResultGapTicks
                } else {
                    recipe.repeatResultGapTicks
                }
            weakQueue(
                cooking_queues.standardFood,
                resultGap + if (compensateQueueHandler) QUEUE_HANDLER_TICK_COMPENSATION else 0,
                request.copy(remaining = remaining, completed = nextCompleted),
            )
        }
    }

    private fun ProtectedAccess.cook(recipe: StandardCookingRecipe, kind: CookingSourceKind) {
        val success =
            !recipe.deliberateBurn &&
                (recipe.alwaysSucceeds || hasCookingBurnImmunity() || rollSuccess(recipe, kind))
        val output = if (success) recipe.cooked else requireNotNull(recipe.burnt)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.raw.id, 1)
                add(inventory, output.id, 1)
                recipe.returnedContainer?.let { add(inventory, it.id, 1) }
            }
        check(result.success) { "Cooking transaction failed after validation: $recipe" }
        if (recipe.deliberateBurn) {
            mes(requireNotNull(recipe.burnMessage))
        } else if (success) {
            statAdvance(stats.cooking, recipe.xp)
            mes(
                recipe.successMessage
                    ?: "You successfully cook the ${objTypes[recipe.cooked].name.lowercase()}."
            )
        } else {
            mes(
                recipe.burnMessage
                    ?: "You accidentally burn the ${objTypes[recipe.raw].name.removePrefix("Raw ").lowercase()}."
            )
        }
    }

    private fun ProtectedAccess.rollSuccess(
        recipe: StandardCookingRecipe,
        kind: CookingSourceKind,
    ): Boolean {
        val chance = chanceFor(recipe, kind, wearingGauntlets())
        return statRandom(stats.cooking, chance.low, chance.high, invisibleBoost = 0)
    }

    public fun chanceFor(
        recipe: StandardCookingRecipe,
        kind: CookingSourceKind,
        wearingGauntlets: Boolean,
        eliteHosidius: Boolean = false,
    ): CookingChance {
        if (kind == CookingSourceKind.Hosidius) {
            if (wearingGauntlets && recipe.gauntletsStackAtHosidius) {
                if (eliteHosidius) {
                    recipe.hosidiusEliteGauntlets?.let {
                        return it
                    }
                } else {
                    recipe.hosidiusGauntlets?.let {
                        return it
                    }
                }
            }
            return if (eliteHosidius) recipe.hosidiusElite else recipe.hosidius
        }
        if (wearingGauntlets) {
            recipe.gauntlets?.let {
                return it
            }
        }
        return when (kind) {
            CookingSourceKind.Fire -> recipe.fire
            CookingSourceKind.Range -> recipe.range
            CookingSourceKind.Lumbridge -> recipe.lumbridge ?: recipe.range
            CookingSourceKind.Hosidius -> error("Hosidius handled above")
        }
    }

    private fun ProtectedAccess.chanceFor(
        recipe: StandardCookingRecipe,
        kind: CookingSourceKind,
        wearingGauntlets: Boolean,
    ): CookingChance =
        chanceFor(
            recipe,
            kind,
            wearingGauntlets,
            eliteHosidius = player.vars[varbits.kourend_diary_elite_complete] != 0,
        )

    private fun ProtectedAccess.canUseSource(
        kind: CookingSourceKind,
        sendFailure: Boolean,
    ): Boolean {
        if (
            kind == CookingSourceKind.Hosidius &&
                player.vars[varbits.kourend_diary_easy_complete] == 0
        ) {
            if (sendFailure) {
                mes("You need to complete the Easy Kourend & Kebos Diary to use this range.")
            }
            return false
        }
        if (kind == CookingSourceKind.Lumbridge && player.vars[varps.cookquest] < 2) {
            if (sendFailure) {
                mes("You need to complete Cook's Assistant to use this range.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.canCook(
        recipe: StandardCookingRecipe,
        kind: CookingSourceKind,
        sendFailure: Boolean,
        checkLevel: Boolean = true,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!canUseSource(kind, sendFailure)) {
            return false
        }
        if (kind !in recipe.allowedSources) {
            return reject("You need to cook this on a range.")
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to cook this food.")
        }
        if (checkLevel && player.stat(stats.cooking) < recipe.level) {
            return reject("You need a Cooking level of ${recipe.level} to cook this food.")
        }
        if (!player.inv.contains(recipe.raw)) {
            return false
        }
        if (recipe.returnedContainer != null && player.inv.freeSpace() < 1) {
            return reject("You don't have enough inventory space.")
        }
        return true
    }

    private fun ProtectedAccess.beginAction(kind: CookingSourceKind) {
        anim(if (kind == CookingSourceKind.Fire) seqs.human_firecooking else seqs.human_cooking)
    }

    private fun ProtectedAccess.wearingGauntlets(): Boolean =
        player.worn.contains(objs.cooking_gauntlets)

    private companion object {
        // Queue handlers consume a cycle, so result gaps need one tick of compensation.
        const val QUEUE_HANDLER_TICK_COMPENSATION = 1
        const val COOK_ACTION_MODE = 1
        const val MENU_TITLE = "What would you like to cook?"
        val SKILL_MULTI_BUTTONS: List<ComponentType> =
            listOf(
                org.rsmod.api.config.refs.components.skillmulti_a,
                org.rsmod.api.config.refs.components.skillmulti_b,
                org.rsmod.api.config.refs.components.skillmulti_c,
                org.rsmod.api.config.refs.components.skillmulti_d,
                org.rsmod.api.config.refs.components.skillmulti_e,
                org.rsmod.api.config.refs.components.skillmulti_f,
                org.rsmod.api.config.refs.components.skillmulti_g,
                org.rsmod.api.config.refs.components.skillmulti_h,
                org.rsmod.api.config.refs.components.skillmulti_i,
                org.rsmod.api.config.refs.components.skillmulti_j,
            )
    }
}

private sealed interface CookingSelection {
    val raw: ObjType
    val output: ObjType

    data class Standard(val recipe: StandardCookingRecipe) : CookingSelection {
        override val raw: ObjType = recipe.raw
        override val output: ObjType = recipe.cooked
    }

    data class Karambwan(val product: KarambwanProduct) : CookingSelection {
        override val raw: ObjType = objs.raw_karambwan
        override val output: ObjType = product.output
    }

    data class Sinew(override val raw: ObjType) : CookingSelection {
        override val output: ObjType = objs.sinew
    }
}

public enum class CookingSourceKind {
    Fire,
    Range,
    Lumbridge,
    Hosidius,
}

public data class StandardCookingRequest(
    val source: BoundLocInfo,
    val kind: CookingSourceKind,
    val raw: ObjType,
    val remaining: Int,
    val completed: Int,
)
