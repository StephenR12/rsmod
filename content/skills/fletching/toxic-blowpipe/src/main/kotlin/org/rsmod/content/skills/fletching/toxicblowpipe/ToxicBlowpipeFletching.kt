package org.rsmod.content.skills.fletching.toxicblowpipe

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat

public class ToxicBlowpipeFletching @Inject constructor() {
    public fun fletch(access: ProtectedAccess) {
        with(access) {
            if (!canFletch()) {
                return
            }
            clearFletchingQueues()
            anim(seqs.snakeboss_fletching)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.tanzanite_fang.id, 1)
                    add(inventory, objs.toxic_blowpipe_empty.id, 1)
                }
            check(result.success) { "Toxic-blowpipe transaction failed after validation." }
            statAdvance(stats.fletching, XP)
        }
    }

    private fun ProtectedAccess.canFletch(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to make a toxic blowpipe.")
            return false
        }
        if (player.stat(stats.fletching) < LEVEL) {
            mes("You need a Fletching level of $LEVEL to make a toxic blowpipe.")
            return false
        }
        if (!player.inv.contains(objs.chisel)) {
            mes("You need a chisel to make a toxic blowpipe.")
            return false
        }
        if (!player.inv.contains(objs.tanzanite_fang)) {
            mes("You need a tanzanite fang to make a toxic blowpipe.")
            return false
        }
        return true
    }

    private companion object {
        const val LEVEL = 78
        const val XP = 120.0
    }
}
