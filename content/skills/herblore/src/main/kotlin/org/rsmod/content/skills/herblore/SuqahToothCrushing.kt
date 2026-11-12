package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class SuqahToothCrushing @Inject constructor() {
    public fun start(access: ProtectedAccess, slot: Int) {
        with(access) {
            clearHerbloreQueues()
            if (!canCrush(slot, sendFailure = true)) {
                return
            }
            anim(seqs.human_herbing_grind)
            weakQueue(
                herblore_queues.suqahToothCrushing,
                INITIAL_RESULT_TICKS,
                SuqahToothCrushingRequest(firstContinuation = true),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: SuqahToothCrushingRequest) {
        with(access) {
            val slot = player.inv.indexOfFirst { it?.id == objs.suqah_tooth.id }
            if (slot < 0 || !canCrush(slot, sendFailure = false)) {
                return
            }
            crush(slot)
            if (hasPestleAndMortar() && player.inv.any { it?.id == objs.suqah_tooth.id }) {
                anim(seqs.human_herbing_grind_restart)
                val delay =
                    if (request.firstContinuation) {
                        FIRST_CONTINUATION_QUEUE_DELAY
                    } else {
                        REPEAT_QUEUE_DELAY
                    }
                weakQueue(
                    herblore_queues.suqahToothCrushing,
                    delay,
                    SuqahToothCrushingRequest(firstContinuation = false),
                )
            }
        }
    }

    private fun ProtectedAccess.crush(slot: Int) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.suqah_tooth.id, 1, slot = slot)
                add(inventory, objs.ground_suqah_tooth.id, 1, slot = slot)
            }
        check(result.success) { "Suqah-tooth crushing transaction failed after validation." }
    }

    private fun ProtectedAccess.canCrush(slot: Int, sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to use this item.")
            }
            return false
        }
        if (!hasPestleAndMortar()) {
            return false
        }
        return player.inv[slot]?.id == objs.suqah_tooth.id
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    private companion object {
        const val INITIAL_RESULT_TICKS = 1
        // Queue handlers consume one cycle while running, so two- and three-tick gaps compensate.
        const val FIRST_CONTINUATION_QUEUE_DELAY = 3
        const val REPEAT_QUEUE_DELAY = 4
    }
}

public data class SuqahToothCrushingRequest(val firstContinuation: Boolean)
