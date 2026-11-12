package org.rsmod.content.skills.cooking

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
import org.rsmod.game.loc.BoundLocInfo

public class ChompyFireCooking @Inject constructor() {
    public fun skewer(access: ProtectedAccess, rawSlot: Int, spitSlot: Int) {
        with(access) {
            clearCookingQueues()
            if (!canSkewer(rawSlot, spitSlot, sendFailure = true)) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.raw_chompy.id, 1, slot = rawSlot)
                    delete(inventory, objs.iron_spit.id, 1, slot = spitSlot)
                    add(inventory, objs.skewered_chompy.id, 1, slot = rawSlot)
                }
            check(result.success) { "Chompy skewering transaction failed after validation." }
        }
    }

    public fun start(access: ProtectedAccess, source: BoundLocInfo, inputSlot: Int) {
        with(access) {
            clearCookingQueues()
            if (!canCook(inputSlot, sendFailure = true)) {
                return
            }
            anim(seqs.spit_fire)
            weakQueue(
                cooking_queues.chompyFire,
                FIRST_RESULT_TICKS,
                ChompyFireCookingRequest(source, inputSlot),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: ChompyFireCookingRequest) {
        with(access) {
            if (
                !isWithinDistance(request.source, distance = 1) ||
                    !canCook(request.inputSlot, sendFailure = false)
            ) {
                resetAnim()
                return
            }
            val success = hasCookingBurnImmunity() || rollSuccess()
            val output = if (success) objs.cooked_chompy else objs.ruined_chompy
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.skewered_chompy.id, 1, slot = request.inputSlot)
                    add(inventory, output.id, 1)
                    add(inventory, objs.iron_spit.id, 1)
                }
            check(result.success) { "Chompy fire-cooking transaction failed after validation." }
            if (success) {
                statAdvance(stats.cooking, COOKING_XP)
                mes("You successfully cook the chompy.")
            } else {
                mes("You accidentally burn the chompy.")
            }
            resetAnim()
        }
    }

    private fun ProtectedAccess.canSkewer(
        rawSlot: Int,
        spitSlot: Int,
        sendFailure: Boolean,
    ): Boolean {
        if (!checkMembership(sendFailure)) {
            return false
        }
        if (player.stat(stats.cooking) < COOKING_LEVEL) {
            if (sendFailure) {
                mes("You need a Cooking level of $COOKING_LEVEL to skewer this chompy.")
            }
            return false
        }
        if (player.stat(stats.firemaking) < FIREMAKING_LEVEL) {
            if (sendFailure) {
                mes("You need a Firemaking level of $FIREMAKING_LEVEL to use an iron spit.")
            }
            return false
        }
        return player.inv[rawSlot]?.id == objs.raw_chompy.id &&
            player.inv[spitSlot]?.id == objs.iron_spit.id
    }

    private fun ProtectedAccess.canCook(inputSlot: Int, sendFailure: Boolean): Boolean {
        if (!checkMembership(sendFailure)) {
            return false
        }
        if (player.stat(stats.cooking) < COOKING_LEVEL) {
            if (sendFailure) {
                mes("You need a Cooking level of $COOKING_LEVEL to cook this chompy.")
            }
            return false
        }
        if (player.stat(stats.firemaking) < FIREMAKING_LEVEL) {
            if (sendFailure) {
                mes("You need a Firemaking level of $FIREMAKING_LEVEL to use an iron spit.")
            }
            return false
        }
        if (player.inv[inputSlot]?.id != objs.skewered_chompy.id) {
            return false
        }
        if (player.inv.freeSpace() < 1) {
            if (sendFailure) {
                mes("You don't have enough inventory space.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.checkMembership(sendFailure: Boolean): Boolean {
        if (player.members) {
            return true
        }
        if (sendFailure) {
            mes("You need to be on a members' world to cook this food.")
        }
        return false
    }

    private fun ProtectedAccess.rollSuccess(): Boolean =
        statRandom(stats.cooking, CHANCE.low, CHANCE.high, invisibleBoost = 0)

    public companion object {
        public const val COOKING_LEVEL: Int = 30
        public const val FIREMAKING_LEVEL: Int = 20
        public const val COOKING_XP: Double = 100.0
        public const val FIRST_RESULT_TICKS: Int = 3
        public val CHANCE: CookingChance = CookingChance(200, 255)
    }
}

public data class ChompyFireCookingRequest(val source: BoundLocInfo, val inputSlot: Int)
