package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class CoconutMilkMaking @Inject constructor() {
    public fun crack(access: ProtectedAccess, coconutSlot: Int) {
        with(access) {
            clearHerbloreQueues()
            if (!canCrack(coconutSlot, sendFailure = true)) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.coconut.id, 1, slot = coconutSlot)
                    add(inventory, objs.coconut_half.id, 1, slot = coconutSlot)
                }
            check(result.success) { "Coconut-cracking transaction failed after validation." }
        }
    }

    public fun fill(access: ProtectedAccess, halfSlot: Int, vialSlot: Int) {
        with(access) {
            clearHerbloreQueues()
            if (!canFill(halfSlot, vialSlot, sendFailure = true)) {
                return
            }
            makeMilk(halfSlot, vialSlot)
            scheduleNext(INITIAL_AUTO_FILL_TICKS)
        }
    }

    public fun process(access: ProtectedAccess) {
        with(access) {
            val halfSlot = player.inv.indexOfFirst { it?.id == objs.coconut_half.id }
            val vialSlot = player.inv.indexOfFirst { it?.id == objs.empty_vial.id }
            if (halfSlot < 0 || vialSlot < 0 || !canFill(halfSlot, vialSlot, sendFailure = false)) {
                return
            }
            makeMilk(halfSlot, vialSlot)
            scheduleNext(REPEAT_AUTO_FILL_QUEUE_DELAY)
        }
    }

    private fun ProtectedAccess.makeMilk(halfSlot: Int, vialSlot: Int) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.coconut_half.id, 1, slot = halfSlot)
                delete(inventory, objs.empty_vial.id, 1, slot = vialSlot)
                add(inventory, objs.coconut_shell.id, 1, slot = halfSlot)
                add(inventory, objs.vial_coconut_milk.id, 1, slot = vialSlot)
            }
        check(result.success) { "Coconut-milk transaction failed after validation." }
    }

    private fun ProtectedAccess.scheduleNext(delay: Int) {
        if (
            player.inv.any { it?.id == objs.coconut_half.id } &&
                player.inv.any { it?.id == objs.empty_vial.id }
        ) {
            weakQueue(herblore_queues.coconutMilk, delay)
        }
    }

    private fun ProtectedAccess.canCrack(coconutSlot: Int, sendFailure: Boolean): Boolean {
        if (!checkMembers(sendFailure)) {
            return false
        }
        return player.inv.any { it?.id == objs.hammer.id } &&
            player.inv[coconutSlot]?.id == objs.coconut.id
    }

    private fun ProtectedAccess.canFill(
        halfSlot: Int,
        vialSlot: Int,
        sendFailure: Boolean,
    ): Boolean {
        if (!checkMembers(sendFailure)) {
            return false
        }
        return player.inv[halfSlot]?.id == objs.coconut_half.id &&
            player.inv[vialSlot]?.id == objs.empty_vial.id
    }

    private fun ProtectedAccess.checkMembers(sendFailure: Boolean): Boolean {
        if (player.members) {
            return true
        }
        if (sendFailure) {
            mes("You need to be on a members' world to use this item.")
        }
        return false
    }

    private companion object {
        const val INITIAL_AUTO_FILL_TICKS = 3
        // Queue handlers consume one cycle, so a four-tick repeat needs compensation.
        const val REPEAT_AUTO_FILL_QUEUE_DELAY = 5
    }
}
