package org.rsmod.content.skills.fletching.battlestafffletching

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiaryFletch
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjTypeList

public class BattlestaffFletching @Inject constructor(private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, quantity: Int = Int.MAX_VALUE) {
        require(quantity >= 1) { "Battlestaff-fletching quantity must be positive." }
        with(access) {
            if (!canFletch(sendFailure = true)) {
                return
            }
            val available = player.inv.count(objTypes[objs.celastrus_bark])
            clearFletchingQueues()
            beginAction()
            weakQueue(
                fletching_queues.battlestaffFletching,
                FIRST_ACTION_TICKS,
                BattlestaffFletchingRequest(minOf(quantity, available)),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: BattlestaffFletchingRequest) {
        with(access) {
            if (!canFletch(sendFailure = false)) {
                resetAnim()
                return
            }
            fletch()
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canFletch(sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction()
            weakQueue(
                fletching_queues.battlestaffFletching,
                repeatActionTicks() + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_fletching)
    }

    private fun ProtectedAccess.fletch() {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.celastrus_bark.id, 1)
                add(inventory, objs.battlestaff.id, 1)
            }
        check(result.success) { "Battlestaff-fletching transaction failed after validation." }
        player.recordDiaryFletch(objs.battlestaff.id,1,player.coords)
        statAdvance(stats.fletching, XP)
    }

    private fun ProtectedAccess.canFletch(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to fletch a battlestaff.")
        }
        if (player.stat(stats.fletching) < LEVEL) {
            return reject("You need a Fletching level of $LEVEL to make a battlestaff.")
        }
        if (!hasKnife()) {
            return reject("You need a knife to fletch the celastrus bark.")
        }
        if (!player.inv.contains(objs.celastrus_bark)) {
            return reject("You do not have any celastrus bark to fletch.")
        }
        return true
    }

    private fun ProtectedAccess.hasKnife(): Boolean =
        player.inv.contains(objs.knife) ||
            player.inv.contains(objs.fletching_knife) ||
            player.worn.contains(objs.fletching_knife)

    private fun ProtectedAccess.hasFletchingKnifeBonus(): Boolean =
        player.inv.contains(objs.fletching_knife) || player.worn.contains(objs.fletching_knife)

    private fun ProtectedAccess.repeatActionTicks(): Int =
        if (hasFletchingKnifeBonus()) FLETCHING_KNIFE_ACTION_TICKS else KNIFE_ACTION_TICKS

    private companion object {
        const val LEVEL = 40
        const val XP = 80.0
        const val FIRST_ACTION_TICKS = 4
        const val KNIFE_ACTION_TICKS = 4
        const val FLETCHING_KNIFE_ACTION_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
    }
}

public data class BattlestaffFletchingRequest(val remaining: Int)
