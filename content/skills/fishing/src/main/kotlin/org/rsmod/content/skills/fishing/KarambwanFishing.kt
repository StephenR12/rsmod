package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueVessels
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.obj.ObjTypeList

public class KarambwanFishing
@Inject
constructor(private val vessels: ClueVessels, private val petRates: SkillPetRates, private val objTypes: ObjTypeList, private val fishBarrel: FishBarrelStorage) {
    public fun loadVessel(access: ProtectedAccess, vesselSlot: Int, baitSlot: Int) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use a karambwan vessel.")
                return
            }
            if (
                vesselSlot !in player.inv.indices ||
                    baitSlot !in player.inv.indices ||
                    player.inv[vesselSlot]?.id != objs.karambwan_vessel.id ||
                    player.inv[baitSlot]?.id != objs.raw_karambwanji.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.karambwan_vessel.id, 1, slot = vesselSlot)
                    delete(inventory, objs.raw_karambwanji.id, 1, slot = baitSlot)
                    add(inventory, objs.baited_karambwan_vessel.id, 1)
                }
            check(result.success) { "Karambwan vessel loading failed after validation." }
        }
    }

    public suspend fun fish(access: ProtectedAccess, npc: Npc) {
        with(access) {
            if (!canFish(sendFailure = true)) {
                return
            }
            faceEntitySquare(npc)
            mes(LOWER_VESSEL_MESSAGE)
            while (true) {
                anim(seqs.human_karambwan)
                gatheringDelay(ROLL_INTERVAL)
                if (!isWithinDistance(npc, distance = 1) || !canFish(sendFailure = false)) {
                    resetAnim()
                    return
                }

                val success = statRandom(stats.fishing, CATCH_LOW, CATCH_HIGH, invisibleBoost = 0)
                val multiplier = if (success) catchMultiplier() else CatchMultiplier.Single
                val occupied = petRates.occupiedBy(player,objs.raw_karambwan.id)
                if (!resolveAttempt(success, multiplier)) {
                    resetAnim()
                    return
                }
                if (success) {
                    petRates.roll(player,stats.fishing,13320,170_874,petRates.newResourceSlot(player,objs.raw_karambwan.id,occupied))
                    vessels.bottles(player,player.stat(stats.fishing),170_874)
                    FishingXp.advance(this, CATCH_XP)
                    player.completeDiaryTask(DiaryTask.KARAMJA_MEDIUM_CATCH_KARAMBWAN)
                    mes(CATCH_MESSAGE)
                    if (multiplier.barrelCount > 0 && fishBarrel.remainingCapacity(player) == 0) {
                        mes(FishBarrelStorage.BARREL_FULL_MESSAGE)
                    }
                } else {
                    mes(BAIT_STOLEN_MESSAGE)
                }
                if (!player.inv.contains(objs.baited_karambwan_vessel)) {
                    resetAnim()
                    return
                }
            }
        }
    }

    private fun ProtectedAccess.resolveAttempt(
        success: Boolean,
        multiplier: CatchMultiplier,
    ): Boolean {
        val vesselSlot = player.inv.indexOfFirst { it?.id == objs.baited_karambwan_vessel.id }
        if (vesselSlot == -1) {
            return false
        }
        val baitSlot = player.inv.indexOfFirst { it?.id == objs.raw_karambwanji.id }
        val replacement =
            if (baitSlot == -1) objs.karambwan_vessel else objs.baited_karambwan_vessel
        val inventorySuccess =
            player
                .invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.baited_karambwan_vessel.id, 1, slot = vesselSlot)
                    if (baitSlot != -1) {
                        delete(inventory, objs.raw_karambwanji.id, 1, slot = baitSlot)
                    }
                    if (multiplier.consumeSpiritFlake) {
                        delete(inventory, objs.spirit_flakes.id, 1)
                    }
                    add(inventory, replacement.id, 1)
                    if (success && multiplier.inventoryCount > 0) {
                        add(inventory, objs.raw_karambwan.id, multiplier.inventoryCount)
                    }
                }
                .success
        if (!inventorySuccess) {
            return false
        }
        if (success) {
            check(
                fishBarrel.storeCaught(player, objs.raw_karambwan, multiplier.barrelCount) ==
                    multiplier.barrelCount
            )
        }
        return true
    }

    private fun ProtectedAccess.catchMultiplier(): CatchMultiplier {
        val available = availableOutputSlots()
        if (available < 1) {
            return CatchMultiplier.Unavailable
        }
        val blessing =
            StandardFishingMethods.radasBlessings.firstOrNull { player.worn.contains(it.type) }
        val canMultiply = available >= 2
        val consumeSpiritFlake = canMultiply && player.inv.contains(objs.spirit_flakes)
        val chance =
            (if (consumeSpiritFlake) SPIRIT_FLAKE_EXTRA_CATCH_PERCENT else 0) +
                (if (canMultiply) blessing?.extraCatchPercent ?: 0 else 0)
        val outputCount = if (chance > 0 && random.of(100) < chance) 2 else 1
        val barrelCount =
            if (fishBarrel.canStoreCaught(player, objs.raw_karambwan)) {
                minOf(outputCount, fishBarrel.remainingCapacity(player))
            } else {
                0
            }
        return CatchMultiplier(outputCount - barrelCount, barrelCount, consumeSpiritFlake)
    }

    private fun ProtectedAccess.availableOutputSlots(): Int {
        val baitFreesSlot =
            player.inv.count(objTypes[objs.raw_karambwanji]) == 1 &&
                player.inv.contains(objs.raw_karambwanji)
        val barrelSpace =
            if (fishBarrel.canStoreCaught(player, objs.raw_karambwan)) {
                fishBarrel.remainingCapacity(player)
            } else {
                0
            }
        return player.inv.freeSpace() + (if (baitFreesSlot) 1 else 0) + barrelSpace
    }

    private fun ProtectedAccess.canFish(sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to fish here.")
            }
            return false
        }
        if (player.vars[varps.tbwt_main] < QUEST_COMPLETE) {
            if (sendFailure) {
                mes("You need to complete Tai Bwo Wannai Trio before fishing here.")
            }
            return false
        }
        if (player.stat(stats.fishing) < LEVEL_REQUIRED) {
            if (sendFailure) {
                mes("You need a Fishing level of $LEVEL_REQUIRED to fish here.")
            }
            return false
        }
        if (!player.inv.contains(objs.baited_karambwan_vessel)) {
            if (sendFailure) {
                mes("You need a baited karambwan vessel to fish here.")
            }
            return false
        }
        if (availableOutputSlots() < 1) {
            if (sendFailure) {
                mes("You do not have enough inventory space.")
            }
            return false
        }
        return true
    }

    private data class CatchMultiplier(
        val inventoryCount: Int,
        val barrelCount: Int,
        val consumeSpiritFlake: Boolean,
    ) {
        companion object {
            val Unavailable: CatchMultiplier = CatchMultiplier(0, 0, false)
            val Single: CatchMultiplier = CatchMultiplier(1, 0, false)
        }
    }

    public companion object {
        public const val LEVEL_REQUIRED: Int = 65
        public const val QUEST_COMPLETE: Int = 6
        public const val CATCH_XP: Double = 50.0
        public const val CATCH_LOW: Int = 5
        public const val CATCH_HIGH: Int = 160
        public const val ROLL_INTERVAL: Int = 4
        public const val SPIRIT_FLAKE_EXTRA_CATCH_PERCENT: Int = 50
        public const val LOWER_VESSEL_MESSAGE: String = "You lower the Karambwan vessel."
        public const val CATCH_MESSAGE: String = "You catch a Karambwan!"
        public const val BAIT_STOLEN_MESSAGE: String =
            "A Karambwan deftly snatches the Karambwanji from your vessel!"
    }
}
