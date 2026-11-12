package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType

public class BlamishSnailSlimeMaking @Inject constructor() {
    public fun start(access: ProtectedAccess, snailSlot: Int, snail: ObjType) {
        with(access) {
            clearHerbloreQueues()
            if (!canMake(snailSlot, snail, sendFailure = true)) {
                return
            }
            anim(seqs.human_herbing_grind)
            make(snailSlot, snail)
            scheduleNext(snail, FIRST_AUTO_RESULT_TICKS, firstContinuation = true)
        }
    }

    public fun process(access: ProtectedAccess, request: BlamishSnailSlimeRequest) {
        with(access) {
            val snail = request.snail
            val snailSlot = player.inv.indexOfFirst { it?.id == snail.id }
            if (snailSlot < 0 || !canMake(snailSlot, snail, sendFailure = false)) {
                return
            }
            anim(seqs.human_herbing_grind_restart)
            make(snailSlot, snail)
            val delay =
                if (request.firstContinuation) {
                    SECOND_AUTO_RESULT_QUEUE_DELAY
                } else {
                    REPEAT_AUTO_RESULT_QUEUE_DELAY
                }
            scheduleNext(snail, delay, firstContinuation = false)
        }
    }

    private fun ProtectedAccess.make(snailSlot: Int, snail: ObjType) {
        val bottleSlot = player.inv.indexOfFirst { it?.id == objs.sample_bottle.id }
        check(bottleSlot >= 0) { "Blamish-slime bottle disappeared after validation." }
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, snail.id, 1, slot = snailSlot)
                delete(inventory, objs.sample_bottle.id, 1, slot = bottleSlot)
                add(inventory, objs.blamish_snail_slime.id, 1, slot = bottleSlot)
            }
        check(result.success) { "Blamish-slime transaction failed after validation." }
    }

    private fun ProtectedAccess.scheduleNext(
        snail: ObjType,
        delay: Int,
        firstContinuation: Boolean,
    ) {
        if (
            player.inv.any { it?.id == snail.id } &&
                player.inv.any { it?.id == objs.sample_bottle.id } &&
                hasPestleAndMortar()
        ) {
            weakQueue(
                herblore_queues.blamishSnailSlime,
                delay,
                BlamishSnailSlimeRequest(snail, firstContinuation),
            )
        }
    }

    private fun ProtectedAccess.canMake(
        snailSlot: Int,
        snail: ObjType,
        sendFailure: Boolean,
    ): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to use this item.")
            }
            return false
        }
        return snail in snails &&
            player.inv[snailSlot]?.id == snail.id &&
            player.inv.any { it?.id == objs.sample_bottle.id } &&
            hasPestleAndMortar()
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    public companion object {
        public val snails: List<ObjType> = listOf(objs.thin_snail, objs.lean_snail, objs.fat_snail)

        private const val FIRST_AUTO_RESULT_TICKS = 1
        // Queue handlers consume one cycle, so two- and three-tick gaps need compensation.
        private const val SECOND_AUTO_RESULT_QUEUE_DELAY = 3
        private const val REPEAT_AUTO_RESULT_QUEUE_DELAY = 4
    }
}

public data class BlamishSnailSlimeRequest(val snail: ObjType, val firstContinuation: Boolean)
