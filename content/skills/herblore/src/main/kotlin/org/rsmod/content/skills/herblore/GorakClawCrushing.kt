package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class GorakClawCrushing @Inject constructor() {
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
            val slot = player.inv.indexOfFirst { it?.id == objs.gorak_claws.id }
            if (slot < 0 || !canCrush(slot, sendFailure = false)) {
                return
            }
            anim(seqs.human_herbing_grind_restart)
            crush(slot)
            scheduleNext(REPEAT_AUTO_CRUSH_TICKS)
        }
    }

    private fun ProtectedAccess.crush(slot: Int) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.gorak_claws.id, 1, slot = slot)
                add(inventory, objs.gorak_claw_powder.id, 1, slot = slot)
            }
        check(result.success) { "Gorak-claw crushing transaction failed after validation." }
    }

    private fun ProtectedAccess.scheduleNext(delay: Int) {
        if (hasPestleAndMortar() && player.inv.any { it?.id == objs.gorak_claws.id }) {
            weakQueue(herblore_queues.gorakClawCrushing, delay)
        }
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
        return player.inv[slot]?.id == objs.gorak_claws.id
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    private companion object {
        const val INITIAL_AUTO_CRUSH_TICKS = 3
        // Queue handlers consume one cycle while running, so three-tick repeats need compensation.
        const val REPEAT_AUTO_CRUSH_TICKS = 4
    }
}
