package org.rsmod.content.skills.crafting.wolfbonearrowtips

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues

public class WolfboneArrowtipCutting @Inject constructor(private val random: GameRandom) {
    public fun start(access: ProtectedAccess) {
        with(access) {
            if (!canCut(sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            clearFletchingQueues()
            anim(seqs.human_ogre_fletching)
            weakQueue(fletching_queues.wolfboneArrowtips, ACTION_TICKS)
        }
    }

    public fun process(access: ProtectedAccess) {
        with(access) {
            if (!canCut(sendFailure = false)) {
                resetAnim()
                return
            }
            val outputCount = random.of(MIN_OUTPUT..MAX_OUTPUT)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.wolf_bones.id, 1)
                    add(inventory, objs.wolfbone_arrowtips.id, outputCount)
                }
            check(result.success) {
                "Wolfbone-arrowtip transaction failed after validation: output=$outputCount"
            }
            val experience = outputCount * XP_PER_TIP
            statAdvance(stats.crafting, experience)
            statAdvance(stats.fletching, experience)
            resetAnim()
        }
    }

    private fun ProtectedAccess.canCut(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make wolfbone arrowtips.")
        }
        if (player.vars[varps.big_chompy_bird_hunting] == BIG_CHOMPY_UNSTARTED_STATE) {
            return reject("You need to start Big Chompy Bird Hunting first.")
        }
        if (player.stat(stats.crafting) < REQUIRED_LEVEL) {
            return reject("You need a Crafting level of $REQUIRED_LEVEL to make these arrowtips.")
        }
        if (player.stat(stats.fletching) < REQUIRED_LEVEL) {
            return reject("You need a Fletching level of $REQUIRED_LEVEL to make these arrowtips.")
        }
        if (!player.inv.contains(objs.chisel)) {
            return reject("You need a chisel to cut wolf bones.")
        }
        if (!player.inv.contains(objs.wolf_bones)) {
            return reject("You do not have any wolf bones left to cut.")
        }
        return true
    }

    private companion object {
        const val REQUIRED_LEVEL = 5
        const val MIN_OUTPUT = 2
        const val MAX_OUTPUT = 6
        const val XP_PER_TIP = 2.5
        const val ACTION_TICKS = 2
        const val BIG_CHOMPY_UNSTARTED_STATE = 0
    }
}
