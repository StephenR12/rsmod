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
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.obj.ObjType

public class KarambwanjiFishing @Inject constructor() {
    public suspend fun fish(access: ProtectedAccess, npc: Npc) {
        with(access) {
            if (!canFish(sendFailure = true)) {
                return
            }
            faceEntitySquare(npc)
            while (true) {
                anim(seqs.human_smallnet)
                delay(ROLL_INTERVAL)
                if (!isWithinDistance(npc, distance = 1) || !canFish(sendFailure = false)) {
                    resetAnim()
                    return
                }

                val rawKarambwanji =
                    statRandom(stats.fishing, KARAMBWANJI_LOW, KARAMBWANJI_HIGH, invisibleBoost = 0)
                val catch =
                    when {
                        rawKarambwanji ->
                            Catch(
                                objs.raw_karambwanji,
                                karambwanjiQuantity(player.stat(stats.fishing)),
                                KARAMBWANJI_MESSAGE,
                            )
                        player.inv.isFull() -> null
                        statRandom(stats.fishing, SHRIMPS_LOW, SHRIMPS_HIGH, invisibleBoost = 0) ->
                            Catch(objs.raw_shrimps, 1, SHRIMPS_MESSAGE)
                        else -> null
                    } ?: continue

                val multiplier = catchMultiplier(catch.output)
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        if (multiplier.consumeSpiritFlake) {
                            delete(inventory, objs.spirit_flakes.id, 1)
                        }
                        add(inventory, catch.output.id, catch.count * multiplier.factor)
                    }
                if (!result.success) {
                    resetAnim()
                    return
                }
                statAdvance(stats.fishing, CATCH_XP)
                mes(catch.message)
            }
        }
    }

    private fun ProtectedAccess.canFish(sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to fish here.")
            }
            return false
        }
        if (player.stat(stats.fishing) < LEVEL_REQUIRED) {
            if (sendFailure) {
                mes("You need a Fishing level of $LEVEL_REQUIRED to fish here.")
            }
            return false
        }
        if (!player.inv.contains(objs.small_fishing_net)) {
            if (sendFailure) {
                mes("You do not have the required equipment to fish here.")
            }
            return false
        }
        if (player.inv.isFull() && !player.inv.contains(objs.raw_karambwanji)) {
            if (sendFailure) {
                mes("You do not have enough inventory space.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.catchMultiplier(output: ObjType): CatchMultiplier {
        val canMultiply = output.id == objs.raw_karambwanji.id || player.inv.freeSpace() >= 2
        if (!canMultiply) {
            return CatchMultiplier.Single
        }
        val blessing =
            StandardFishingMethods.radasBlessings.firstOrNull { player.worn.contains(it.type) }
        val consumeSpiritFlake = player.inv.contains(objs.spirit_flakes)
        val extraCatchPercent =
            (if (consumeSpiritFlake) SPIRIT_FLAKE_EXTRA_CATCH_PERCENT else 0) +
                (blessing?.extraCatchPercent ?: 0)
        if (extraCatchPercent == 0) {
            return CatchMultiplier.Single
        }
        val factor = if (random.of(100) < extraCatchPercent) 2 else 1
        return CatchMultiplier(factor, consumeSpiritFlake)
    }

    private data class Catch(val output: ObjType, val count: Int, val message: String)

    private data class CatchMultiplier(val factor: Int, val consumeSpiritFlake: Boolean) {
        companion object {
            val Single: CatchMultiplier = CatchMultiplier(1, false)
        }
    }

    public companion object {
        public const val LEVEL_REQUIRED: Int = 5
        public const val CATCH_XP: Double = 5.0
        public const val KARAMBWANJI_LOW: Int = 100
        public const val KARAMBWANJI_HIGH: Int = 250
        public const val SHRIMPS_LOW: Int = 48
        public const val SHRIMPS_HIGH: Int = 256
        public const val ROLL_INTERVAL: Int = 6
        public const val SPIRIT_FLAKE_EXTRA_CATCH_PERCENT: Int = 50
        public const val KARAMBWANJI_MESSAGE: String = "You catch some raw karambwanji."
        public const val SHRIMPS_MESSAGE: String = "You catch some raw shrimps."

        public fun karambwanjiQuantity(fishingLevel: Int): Int = fishingLevel / 5 + 1
    }
}
