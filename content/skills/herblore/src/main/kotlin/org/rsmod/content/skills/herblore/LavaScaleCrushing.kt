package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class LavaScaleCrushing @Inject constructor() {
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
            val slot = player.inv.indexOfFirst { it?.id == objs.lava_scale.id }
            if (slot < 0 || !canCrush(slot, sendFailure = false)) {
                return
            }
            anim(seqs.human_herbing_grind_restart)
            crush(slot)
            scheduleNext(REPEAT_AUTO_CRUSH_TICKS)
        }
    }

    private fun ProtectedAccess.crush(slot: Int) {
        var yield = BASE_SHARD_YIELD
        if (statRandom(stats.herblore, low = 101, high = 301, invisibleBoost = 0)) {
            yield++
        }
        if (statRandom(stats.herblore, low = 58, high = 251, invisibleBoost = 0)) {
            yield++
        }
        if (statRandom(stats.herblore, low = -14, high = 254, invisibleBoost = 0)) {
            yield++
        }
        if (player.vars[varbits.wilderness_diary_hard_complete] != 0) {
            yield = yield * 3 / 2
        }
        val shardSlot = player.inv.indexOfFirst { it?.id == objs.lava_scale_shard.id }
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.lava_scale.id, 1, slot = slot)
                if (shardSlot < 0) {
                    add(inventory, objs.lava_scale_shard.id, yield, slot = slot)
                } else {
                    add(inventory, objs.lava_scale_shard.id, yield, slot = shardSlot)
                }
            }
        check(result.success) { "Lava-scale crushing transaction failed after validation." }
    }

    private fun ProtectedAccess.scheduleNext(delay: Int) {
        if (hasPestleAndMortar() && player.inv.any { it?.id == objs.lava_scale.id }) {
            weakQueue(herblore_queues.lavaScaleCrushing, delay)
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
        return player.inv[slot]?.id == objs.lava_scale.id
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    private companion object {
        const val BASE_SHARD_YIELD = 3
        const val INITIAL_AUTO_CRUSH_TICKS = 2
        // Queue handlers consume one cycle while running, so three-tick repeats need compensation.
        const val REPEAT_AUTO_CRUSH_TICKS = 4
    }
}
