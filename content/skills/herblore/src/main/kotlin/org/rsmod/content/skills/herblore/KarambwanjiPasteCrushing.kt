package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class KarambwanjiPasteCrushing @Inject constructor() {
    public fun start(access: ProtectedAccess, slot: Int) {
        with(access) {
            clearHerbloreQueues()
            if (!canCrush(slot, sendFailure = true)) {
                return
            }
            anim(seqs.human_herbing_grind)
            crush(slot)
            scheduleNext(INITIAL_AUTO_CRUSH_TICKS)
        }
    }

    public fun process(access: ProtectedAccess) {
        with(access) {
            val slot = player.inv.indexOfFirst { it?.id == objs.raw_karambwanji.id }
            if (slot < 0 || !canCrush(slot, sendFailure = false)) {
                return
            }
            anim(seqs.human_herbing_grind_restart)
            crush(slot)
            scheduleNext(REPEAT_AUTO_CRUSH_TICKS)
        }
    }

    private fun ProtectedAccess.crush(slot: Int) {
        val outputSlot = outputSlot(slot)
        check(outputSlot >= 0) { "Karambwanji-paste capacity changed after validation." }
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.raw_karambwanji.id, 1, slot = slot)
                add(inventory, objs.raw_karambwanji_paste.id, 1, slot = outputSlot)
            }
        check(result.success) { "Karambwanji-paste transaction failed after validation." }
    }

    private fun ProtectedAccess.scheduleNext(delay: Int) {
        val slot = player.inv.indexOfFirst { it?.id == objs.raw_karambwanji.id }
        if (slot >= 0 && canCrush(slot, sendFailure = false)) {
            weakQueue(herblore_queues.karambwanjiPasteCrushing, delay)
        }
    }

    private fun ProtectedAccess.canCrush(slot: Int, sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to use this item.")
            }
            return false
        }
        return hasPestleAndMortar() &&
            player.inv[slot]?.id == objs.raw_karambwanji.id &&
            outputSlot(slot) >= 0
    }

    private fun ProtectedAccess.outputSlot(slot: Int): Int {
        val selected = player.inv[slot] ?: return -1
        if (selected.count == 1) {
            return slot
        }
        return player.inv.indexOfFirst { it == null }
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    private companion object {
        const val INITIAL_AUTO_CRUSH_TICKS = 3
        // Queue handlers consume one cycle while running, so three-tick repeats need compensation.
        const val REPEAT_AUTO_CRUSH_TICKS = 4
    }
}
