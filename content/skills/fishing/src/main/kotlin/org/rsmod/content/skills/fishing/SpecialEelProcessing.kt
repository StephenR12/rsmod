package org.rsmod.content.skills.fishing

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
import org.rsmod.game.type.obj.ObjType

public class SpecialEelProcessing @Inject constructor() {
    public fun smashInfernal(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        hammer: ObjType,
    ) {
        with(access) {
            val selectedHammer = selectedSlot(firstSlot, secondSlot, hammer) ?: return
            val selectedEel = selectedSlot(firstSlot, secondSlot, objs.infernal_eel) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to smash this eel.")
                return
            }
            if (
                player.inv[selectedHammer]?.id != hammer.id ||
                    player.inv[selectedEel]?.id != objs.infernal_eel.id
            ) {
                return
            }
            val eelSlot = player.inv.indexOfFirst { it?.id == objs.infernal_eel.id }
            if (eelSlot == -1) {
                return
            }
            val reward = rollInfernalReward(random.of(INFERNAL_REWARD_RATE))
            val count = random.of(reward.count)
            anim(
                if (
                    hammer.id == objs.imcando_hammer.id ||
                        hammer.id == objs.imcando_hammer_offhand.id
                ) {
                    seqs.infernal_eel_smashing_imcando
                } else {
                    seqs.infernal_eel_smashing
                }
            )
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.infernal_eel.id, 1, slot = eelSlot)
                    add(inventory, reward.output.id, count)
                }
            check(result.success) { "Infernal-eel smashing transaction failed after validation." }
        }
    }

    public fun dissectSacred(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val knifeSlot = selectedSlot(firstSlot, secondSlot, objs.knife) ?: return
            val selectedEel = selectedSlot(firstSlot, secondSlot, objs.sacred_eel) ?: return
            if (!canDissect(selectedEel, knifeSlot, exactKnifeSlot = true, sendFailure = true)) {
                return
            }
            val eelSlot = player.inv.indexOfFirst { it?.id == objs.sacred_eel.id }
            if (eelSlot == -1) {
                return
            }
            clearSacredEelDissectionQueue()
            processSacred(eelSlot)
            scheduleSacred(FIRST_AUTOMATIC_TICKS)
        }
    }

    public fun continueSacred(access: ProtectedAccess) {
        with(access) {
            val eelSlot = player.inv.indexOfFirst { it?.id == objs.sacred_eel.id }
            if (eelSlot == -1 || !canDissect(eelSlot, -1)) {
                resetAnim()
                return
            }
            processSacred(eelSlot)
            scheduleSacred(REPEATED_AUTOMATIC_TICKS + QUEUE_HANDLER_COMPENSATION)
        }
    }

    public fun rejectInfernalEating(access: ProtectedAccess) {
        access.mes(INFERNAL_EAT_MESSAGE)
    }

    public fun rejectSacredEating(access: ProtectedAccess) {
        access.mes(SACRED_EAT_MESSAGE)
    }

    private fun ProtectedAccess.processSacred(eelSlot: Int) {
        val scales = sacredEelScaleRange(player.stat(stats.cooking))
        val count = random.of(scales)
        anim(seqs.sacred_eel_dissection)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.sacred_eel.id, 1, slot = eelSlot)
                add(inventory, objs.zulrahs_scales.id, count)
            }
        check(result.success) { "Sacred-eel dissection transaction failed after validation." }
        statAdvance(stats.cooking, SACRED_BASE_XP + SACRED_XP_PER_SCALE * count)
    }

    private fun ProtectedAccess.scheduleSacred(delay: Int) {
        if (canContinueSacred()) {
            weakQueue(fishing_queues.sacredEelDissection, delay, SacredEelDissectionRequest)
        }
    }

    private fun ProtectedAccess.canDissect(
        eelSlot: Int,
        knifeSlot: Int,
        exactKnifeSlot: Boolean = false,
        sendFailure: Boolean = false,
    ): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to dissect this eel.")
            }
            return false
        }
        if (player.stat(stats.cooking) < SACRED_LEVEL_REQUIRED) {
            if (sendFailure) {
                mes("You need a Cooking level of 72 to dissect this eel.")
            }
            return false
        }
        if (player.inv[eelSlot]?.id != objs.sacred_eel.id) {
            return false
        }
        return if (exactKnifeSlot) {
            player.inv[knifeSlot]?.id == objs.knife.id
        } else {
            player.inv.contains(objs.knife)
        }
    }

    private fun ProtectedAccess.canContinueSacred(): Boolean =
        player.members &&
            player.stat(stats.cooking) >= SACRED_LEVEL_REQUIRED &&
            player.inv.contains(objs.knife) &&
            player.inv.contains(objs.sacred_eel)

    private fun ProtectedAccess.selectedSlot(firstSlot: Int, secondSlot: Int, type: ObjType): Int? =
        when {
            player.inv[firstSlot]?.id == type.id -> firstSlot
            player.inv[secondSlot]?.id == type.id -> secondSlot
            else -> null
        }

    public companion object {
        public const val SACRED_LEVEL_REQUIRED: Int = 72
        public const val FIRST_AUTOMATIC_TICKS: Int = 2
        public const val REPEATED_AUTOMATIC_TICKS: Int = 3
        public const val SACRED_BASE_XP: Double = 100.0
        public const val SACRED_XP_PER_SCALE: Double = 3.0
        public const val INFERNAL_EAT_MESSAGE: String =
            "Removing the eel from the lava has caused it to harden. You don't want to test " +
                "your teeth against one of these."
        public const val SACRED_EAT_MESSAGE: String =
            "You've seen what these eels did to the Zul-Andra tribe. Eating one yourself seems " +
                "remarkably unwise."

        private const val INFERNAL_REWARD_RATE = 35
        private const val QUEUE_HANDLER_COMPENSATION = 1
    }
}

public data object SacredEelDissectionRequest

public data class InfernalEelReward(val output: ObjType, val count: IntRange)

public fun rollInfernalReward(roll: Int): InfernalEelReward =
    when (roll) {
        in 0..28 -> InfernalEelReward(objs.tokkul, 14..20)
        in 29..30 -> InfernalEelReward(objs.onyx_bolt_tips, 1..1)
        in 31..34 -> InfernalEelReward(objs.lava_scale_shard, 1..5)
        else -> throw IllegalArgumentException("Infernal-eel reward roll must be in 0..34: $roll")
    }

public fun sacredEelScaleRange(cookingLevel: Int): IntRange {
    val minimum = 3 + ((cookingLevel.coerceIn(72, 104) - 72) / 8)
    return minimum..(minimum + 2)
}
