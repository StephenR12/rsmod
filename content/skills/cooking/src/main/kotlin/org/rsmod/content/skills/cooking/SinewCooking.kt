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
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class SinewCooking @Inject constructor(private val objTypes: ObjTypeList) {
    public fun availableInputs(
        player: Player,
        kind: CookingSourceKind,
        requestedRaw: ObjType?,
    ): List<ObjType> {
        if (!player.members || kind == CookingSourceKind.Fire) {
            return emptyList()
        }
        return INPUTS.filter { raw ->
            (requestedRaw == null || requestedRaw == raw) && player.inv.contains(raw)
        }
    }

    public fun start(
        access: ProtectedAccess,
        source: BoundLocInfo,
        kind: CookingSourceKind,
        raw: ObjType,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Cooking quantity must be positive." }
        with(access) {
            if (!canCook(raw, kind, sendFailure = true)) {
                return
            }
            clearCookingQueues()
            beginAction()
            process(
                this,
                SinewCookingRequest(source, kind, raw, quantity, completed = 0),
                compensateQueueHandler = false,
            )
        }
    }

    public fun process(access: ProtectedAccess, request: SinewCookingRequest) {
        process(access, request, compensateQueueHandler = true)
    }

    public fun chanceFor(kind: CookingSourceKind, eliteHosidius: Boolean = false): CookingChance =
        when (kind) {
            CookingSourceKind.Fire -> error("Sinew cannot be made on a fire.")
            CookingSourceKind.Range,
            CookingSourceKind.Lumbridge -> ORDINARY_CHANCE
            CookingSourceKind.Hosidius ->
                if (eliteHosidius) HOSIDIUS_ELITE_CHANCE else HOSIDIUS_CHANCE
        }

    private fun process(
        access: ProtectedAccess,
        request: SinewCookingRequest,
        compensateQueueHandler: Boolean,
    ) {
        with(access) {
            if (
                !isWithinDistance(request.source, distance = 1) ||
                    !canCook(request.raw, request.kind, sendFailure = false)
            ) {
                resetAnim()
                return
            }
            cook(request.raw, request.kind)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canCook(request.raw, request.kind, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction()
            val nextCompleted = request.completed + 1
            val gap = if (nextCompleted == 1) SECOND_RESULT_GAP_TICKS else REPEAT_GAP_TICKS
            weakQueue(
                cooking_queues.sinew,
                gap + if (compensateQueueHandler) QUEUE_HANDLER_TICK_COMPENSATION else 0,
                request.copy(remaining = remaining, completed = nextCompleted),
            )
        }
    }

    private fun ProtectedAccess.cook(raw: ObjType, kind: CookingSourceKind) {
        val chance =
            chanceFor(kind, eliteHosidius = player.vars[varbits.kourend_diary_elite_complete] != 0)
        val success =
            hasCookingBurnImmunity() ||
                statRandom(stats.cooking, chance.low, chance.high, invisibleBoost = 0)
        val output = if (success) objs.sinew else objs.burnt_meat
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, raw.id, 1)
                add(inventory, output.id, 1)
            }
        check(result.success) { "Sinew transaction failed after validation." }
        if (success) {
            statAdvance(stats.cooking, XP)
            mes("You dry a piece of meat and extract the sinew.")
        } else {
            mes("You accidentally burn the meat.")
        }
    }

    private fun ProtectedAccess.canCook(
        raw: ObjType,
        kind: CookingSourceKind,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (raw !in INPUTS) {
            return false
        }
        if (kind == CookingSourceKind.Fire) {
            return reject("You need a range to extract sinew from this meat.")
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make sinew.")
        }
        if (
            kind == CookingSourceKind.Hosidius &&
                player.vars[varbits.kourend_diary_easy_complete] == 0
        ) {
            return reject("You need to complete the Easy Kourend & Kebos Diary to use this range.")
        }
        if (kind == CookingSourceKind.Lumbridge && player.vars[varps.cookquest] < 2) {
            return reject("You need to complete Cook's Assistant to use this range.")
        }
        return player.inv.count(objTypes[raw]) > 0
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_cooking)
    }

    private companion object {
        const val XP = 3.0
        const val SECOND_RESULT_GAP_TICKS = 3
        const val REPEAT_GAP_TICKS = 4
        const val QUEUE_HANDLER_TICK_COMPENSATION = 1
        val ORDINARY_CHANCE = CookingChance(128, 512)
        val HOSIDIUS_CHANCE = CookingChance(140, 524)
        val HOSIDIUS_ELITE_CHANCE = CookingChance(153, 537)
        val INPUTS = listOf(objs.raw_beef, objs.raw_bear_meat, objs.raw_boar_meat)
    }
}

public data class SinewCookingRequest(
    val source: BoundLocInfo,
    val kind: CookingSourceKind,
    val raw: ObjType,
    val remaining: Int,
    val completed: Int,
)
