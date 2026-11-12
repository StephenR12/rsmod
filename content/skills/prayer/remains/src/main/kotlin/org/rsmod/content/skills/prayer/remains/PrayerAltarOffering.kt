package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.zone.ZoneKey

public class PrayerAltarOffering @Inject constructor(private val locRepo: LocRepository) {
    public fun start(
        access: ProtectedAccess,
        altar: BoundLocInfo,
        selectedSlot: Int,
        recipe: PrayerRemainsRecipe,
    ) {
        with(access) {
            if (prayer_remains_queues.altarManualCooldown in player.weakQueueList) {
                return
            }
            val altarType = altarType(altar)
            if (altarType == null) {
                mes(WRATH_MESSAGE)
                return
            }
            if (!canOffer(recipe, sendFailure = true)) {
                return
            }
            val slot =
                if (altarType is PrayerAltarType.Chapel) player.inv.firstSlot(recipe)
                else selectedSlot.takeIf { player.inv[it]?.id == recipe.obj.id }
            if (slot == null || !offer(this, altar, altarType, recipe, slot)) {
                return
            }
            clearWeakQueue(prayer_remains_queues.altarOffering)
            weakQueue(prayer_remains_queues.altarManualCooldown, MANUAL_ACTION_TICKS)
            if (player.inv.firstSlot(recipe) != null) {
                weakQueue(
                    prayer_remains_queues.altarOffering,
                    AUTOMATIC_ACTION_TICKS,
                    PrayerAltarOfferingRequest(altar, recipe.obj.id),
                )
            }
        }
    }

    public fun process(access: ProtectedAccess, request: PrayerAltarOfferingRequest) {
        with(access) {
            if (!isWithinDistance(request.altar, distance = 1) || !altarExists(request.altar)) {
                resetAnim()
                return
            }
            val recipe = PrayerRemainsRecipes.byObj[request.objId] ?: return
            val altarType = altarType(request.altar) ?: return
            val slot = player.inv.firstSlot(recipe) ?: return
            if (!canOffer(recipe, sendFailure = false)) {
                return
            }
            if (!offer(this, request.altar, altarType, recipe, slot)) {
                return
            }
            if (player.inv.firstSlot(recipe) != null) {
                weakQueue(prayer_remains_queues.altarOffering, AUTOMATIC_ACTION_TICKS, request)
            }
        }
    }

    private fun offer(
        access: ProtectedAccess,
        altar: BoundLocInfo,
        altarType: PrayerAltarType,
        recipe: PrayerRemainsRecipe,
        slot: Int,
    ): Boolean {
        with(access) {
            if (player.inv[slot]?.id != recipe.obj.id) {
                return false
            }
            val chaosPreserves =
                altarType == PrayerAltarType.Chaos && random.randomBoolean(maxExclusive = 2)
            val preserved = chaosPreserves || access.zealotPreservesBone(recipe)
            if (!preserved) {
                val deleted = invDel(player.inv, recipe.obj, count = 1, slot = slot)
                if (deleted.failure) {
                    return false
                }
            }

            val burners = if (altarType is PrayerAltarType.Chapel) litBurners(altar) else 2
            val percent =
                when (altarType) {
                    PrayerAltarType.Chaos -> CHAOS_XP_PERCENT
                    is PrayerAltarType.Chapel -> altarType.baseXpPercent + burners * 50
                }
            faceLoc(altar)
            anim(seqs.human_bone_sacrifice)
            spotanim(spotanims.poh_bone_sacrifice)
            mes(offeringMessage(burners))
            if (chaosPreserves) {
                mes(SPARED_MESSAGE)
            }
            statAdvance(stats.prayer, recipe.xp * percent / 100.0)
            return true
        }
    }

    private fun ProtectedAccess.canOffer(
        recipe: PrayerRemainsRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (recipe.kind != PrayerRemainsKind.Bone) {
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to offer bones at this altar.")
        }
        if (player.basePrayerLvl < recipe.prayerLevel) {
            return reject("You need a Prayer level of ${recipe.prayerLevel} to use these bones.")
        }
        return true
    }

    private fun altarType(altar: BoundLocInfo): PrayerAltarType? {
        val tier =
            prayer_altar_locs.chapelByTier.indexOfFirst { altars ->
                altars.any { it.id == altar.id }
            }
        if (tier >= 0) {
            return PrayerAltarType.Chapel(CHAPEL_BASE_XP_PERCENT[tier])
        }
        return PrayerAltarType.Chaos.takeIf {
            altar.id == prayer_altar_locs.chaos.id &&
                ZoneKey.from(altar.coords) == DEEP_WILDERNESS_TEMPLE_ZONE
        }
    }

    private fun altarExists(altar: BoundLocInfo): Boolean =
        locRepo.findAll(altar.coords).any { it.entity == altar.entity }

    private fun litBurners(altar: BoundLocInfo): Int =
        locRepo
            .findAll(ZoneKey.from(altar.coords))
            .count { loc -> prayer_altar_locs.litBurners.any { it.id == loc.id } }
            .coerceAtMost(2)

    private fun Inventory.firstSlot(recipe: PrayerRemainsRecipe): Int? =
        indexOfFirst { it?.id == recipe.obj.id }.takeIf { it >= 0 }

    private fun offeringMessage(burners: Int): String =
        when (burners) {
            0 -> "The gods accept your offering."
            1 -> "The gods are pleased with your offering."
            else -> "The gods are very pleased with your offering."
        }

    private sealed interface PrayerAltarType {
        data object Chaos : PrayerAltarType

        data class Chapel(val baseXpPercent: Int) : PrayerAltarType
    }

    private companion object {
        val CHAPEL_BASE_XP_PERCENT = listOf(100, 110, 125, 150, 175, 200, 250)
        val DEEP_WILDERNESS_TEMPLE_ZONE = ZoneKey(368, 477, 0)

        const val CHAOS_XP_PERCENT = 350
        const val MANUAL_ACTION_TICKS = 1
        const val AUTOMATIC_ACTION_TICKS = 3
        const val WRATH_MESSAGE = "You fear the wrath of the gods!"
        const val SPARED_MESSAGE = "Your sacrifice is spared!"
    }
}

public data class PrayerAltarOfferingRequest(val altar: BoundLocInfo, val objId: Int)
