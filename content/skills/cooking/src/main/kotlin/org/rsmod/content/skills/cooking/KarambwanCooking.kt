package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class KarambwanCooking @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(
        access: ProtectedAccess,
        source: BoundLocInfo,
        kind: CookingSourceKind,
    ) {
        with(access) {
            if (!canCook(kind, sendFailure = true)) {
                return
            }
            val products = availableProducts(player)
            val count = player.inv.count(objTypes[objs.raw_karambwan])
            if (products.size == 1 && count == 1) {
                start(this, source, kind, products.single(), quantity = 1)
                return
            }
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = products.map(KarambwanProduct::output),
                    maxQuantity = count,
                    initialQuantity = count,
                    actionMode = COOK_ACTION_MODE,
                )
            val index = SKILL_MULTI_BUTTONS.indexOfFirst { it.isType(input.component) }
            val product = products.getOrNull(index) ?: return
            start(this, source, kind, product, input.subcomponent)
        }
    }

    public suspend fun start(
        access: ProtectedAccess,
        source: BoundLocInfo,
        kind: CookingSourceKind,
        product: KarambwanProduct,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Cooking quantity must be positive." }
        with(access) {
            if (!canCook(kind, sendFailure = true)) {
                return
            }
            if (product == KarambwanProduct.Thorough && !hasThoroughCookingUnlock(player)) {
                return
            }
            var confirmedWarning = false
            if (
                product == KarambwanProduct.Poison &&
                    player.vars[varbits.poison_karambwan_warning_disabled] == 0
            ) {
                val confirmed = choice2("No.", false, "Yes.", true, title = POISON_WARNING)
                if (!confirmed || !canCook(kind, sendFailure = false)) {
                    return
                }
                confirmedWarning = true
            }
            clearCookingQueues()
            beginAction(kind)
            val request = KarambwanCookingRequest(source, kind, product, quantity, completed = 0)
            if (confirmedWarning) {
                delay(FIRST_RESULT_TICKS)
                process(this, request, compensateQueueHandler = false)
            } else {
                weakQueue(cooking_queues.karambwan, FIRST_RESULT_TICKS, request)
            }
        }
    }

    public fun process(access: ProtectedAccess, request: KarambwanCookingRequest) {
        process(access, request, compensateQueueHandler = true)
    }

    private fun process(
        access: ProtectedAccess,
        request: KarambwanCookingRequest,
        compensateQueueHandler: Boolean,
    ) {
        with(access) {
            if (
                !isWithinDistance(request.source, distance = 1) ||
                    !canCook(request.kind, sendFailure = false) ||
                    (request.product == KarambwanProduct.Thorough &&
                        !hasThoroughCookingUnlock(player))
            ) {
                resetAnim()
                return
            }
            cook(request.product, request.kind)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canCook(request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction(request.kind)
            val nextCompleted = request.completed + 1
            val gap = if (nextCompleted == 1) SECOND_RESULT_GAP_TICKS else REPEAT_GAP_TICKS
            weakQueue(
                cooking_queues.karambwan,
                gap + if (compensateQueueHandler) QUEUE_HANDLER_TICK_COMPENSATION else 0,
                request.copy(remaining = remaining, completed = nextCompleted),
            )
        }
    }

    public fun reheatBurnt(access: ProtectedAccess, kind: CookingSourceKind) {
        with(access) {
            if (!canUseSource(kind, sendFailure = true)) {
                return
            }
            mes("There is nothing to be gained from heating a burnt octopus.")
        }
    }

    public fun availableProducts(player: Player): List<KarambwanProduct> =
        if (hasThoroughCookingUnlock(player)) {
            listOf(KarambwanProduct.Thorough, KarambwanProduct.Poison)
        } else {
            listOf(KarambwanProduct.Poison)
        }

    public fun hasThoroughCookingUnlock(player: Player): Boolean =
        player.vars[varps.tbwt_main] >= QUEST_COMPLETE &&
            player.vars[varbits.tbwt_tinsay_reward_claimed] != 0

    public fun chanceFor(kind: CookingSourceKind, eliteHosidius: Boolean = false): CookingChance =
        when (kind) {
            CookingSourceKind.Fire,
            CookingSourceKind.Range,
            CookingSourceKind.Lumbridge -> ORDINARY_CHANCE
            CookingSourceKind.Hosidius ->
                if (eliteHosidius) HOSIDIUS_ELITE_CHANCE else HOSIDIUS_CHANCE
        }

    private fun ProtectedAccess.cook(product: KarambwanProduct, kind: CookingSourceKind) {
        val chance =
            chanceFor(kind, eliteHosidius = player.vars[varbits.kourend_diary_elite_complete] != 0)
        val success =
            hasCookingBurnImmunity() ||
                statRandom(stats.cooking, chance.low, chance.high, invisibleBoost = 0)
        val output = if (success) product.output else objs.burnt_karambwan
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.raw_karambwan.id, 1)
                add(inventory, output.id, 1)
            }
        check(result.success) { "Karambwan cooking transaction failed after validation." }
        if (!success) {
            mes("You accidentally burn the karambwan.")
            return
        }
        statAdvance(stats.cooking, product.xp)
        mes(product.successMessage)
        if (product == KarambwanProduct.Thorough) {
            player.completeDiaryTask(DiaryTask.KARAMJA_HARD_COOK_KARAMBWAN)
        }
    }

    private fun ProtectedAccess.canCook(kind: CookingSourceKind, sendFailure: Boolean): Boolean {
        if (!canUseSource(kind, sendFailure)) {
            return false
        }
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to cook this food.")
            }
            return false
        }
        return player.inv.contains(objs.raw_karambwan)
    }

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

    private fun ProtectedAccess.beginAction(kind: CookingSourceKind) {
        anim(if (kind == CookingSourceKind.Fire) seqs.human_firecooking else seqs.human_cooking)
    }

    private companion object {
        const val QUEST_COMPLETE = 6
        const val COOK_ACTION_MODE = 1
        const val FIRST_RESULT_TICKS = 1
        const val SECOND_RESULT_GAP_TICKS = 3
        const val REPEAT_GAP_TICKS = 4
        const val QUEUE_HANDLER_TICK_COMPENSATION = 1
        const val MENU_TITLE = "How would you like to cook your karambwan?"
        const val POISON_WARNING = "This will poison the Karambwan. Do you still want to cook it?"
        val ORDINARY_CHANCE = CookingChance(70, 255)
        val HOSIDIUS_CHANCE = CookingChance(82, 267)
        val HOSIDIUS_ELITE_CHANCE = CookingChance(95, 280)
        val SKILL_MULTI_BUTTONS: List<ComponentType> =
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

public enum class KarambwanProduct(
    public val output: ObjType,
    public val xp: Double,
    public val successMessage: String,
) {
    Thorough(objs.cooked_karambwan, 190.0, "You cook the karambwan. It looks delicious."),
    Poison(objs.poison_karambwan, 80.0, "You cook the karambwan. It looks very dodgy."),
}

public data class KarambwanCookingRequest(
    val source: BoundLocInfo,
    val kind: CookingSourceKind,
    val product: KarambwanProduct,
    val remaining: Int,
    val completed: Int,
)
