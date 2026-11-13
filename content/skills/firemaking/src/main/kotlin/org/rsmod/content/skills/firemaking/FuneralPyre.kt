package org.rsmod.content.skills.firemaking

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public class FuneralPyre
@Inject
constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
    private val random: GameRandom,
) {
    private val remainsByPyre = mutableMapOf<Int, ShadeRemain>()

    public fun buildBest(access: ProtectedAccess, pyre: BoundLocInfo): Boolean {
        with(access) {
            interruptFiremakingQueues()
            if (!canUse(sendFailure = true) || !isCurrent(pyre)) {
                return false
            }
            val owned = FuneralPyreLogs.all.asReversed().filter { player.inv.contains(it.obj) }
            if (owned.isEmpty()) {
                mes("You do not have any pyre logs to build this funeral pyre.")
                return false
            }
            val paired =
                owned.filter { log ->
                    (player.vars[varps.morttonquest] >= QUEST_COMPLETE_STATE ||
                        log.obj == objs.pyre_logs) &&
                        ShadeRemains.all.any { remains ->
                            player.inv.contains(remains.obj) &&
                                remains.prayerXp(log) != null &&
                                (player.vars[varps.morttonquest] >= QUEST_COMPLETE_STATE ||
                                    remains.obj == objs.loar_remains)
                        }
                }
            if (paired.isEmpty()) {
                mes("You do not have the appropriate shade remains for your pyre logs.")
                return false
            }
            val recipe = paired.firstOrNull { player.firemakingLvl >= it.level }
            if (recipe == null) {
                val required = paired.minOf { it.level }
                mes("You need a Firemaking level of $required to use these pyre logs.")
                return false
            }
            val slot = player.inv.firstSlot(recipe.obj) ?: return false
            return build(this, pyre, slot, recipe)
        }
    }

    public fun build(
        access: ProtectedAccess,
        pyre: BoundLocInfo,
        selectedSlot: Int,
        recipe: FuneralPyreLog,
    ): Boolean {
        with(access) {
            interruptFiremakingQueues()
            if (!canUse(sendFailure = true) || !isCurrent(pyre)) {
                return false
            }
            if (player.firemakingLvl < recipe.level) {
                mes("You need a Firemaking level of ${recipe.level} to use these pyre logs.")
                return false
            }
            if (
                player.vars[varps.morttonquest] < QUEST_COMPLETE_STATE &&
                    recipe.obj != objs.pyre_logs
            ) {
                mes("You need to complete Shades of Mort'ton before using these pyre logs.")
                return false
            }
            if (player.inv[selectedSlot]?.id != recipe.obj.id) {
                return false
            }
            if (!replace(pyre, recipe.logLoc, STAGE_DURATION_TICKS)) {
                return false
            }
            val deleted = invDel(player.inv, recipe.obj, count = 1, slot = selectedSlot)
            if (deleted.failure) {
                replace(pyre, firemaking_locs.funeralPyre, Int.MAX_VALUE)
                return false
            }
            remainsByPyre.remove(pyre.coords.packed)
            faceLoc(pyre)
            anim(seqs.temple_pyre_nowood)
            mes("You place the pyre logs on the funeral pyre.")
            return true
        }
    }

    public fun addBestRemains(
        access: ProtectedAccess,
        pyre: BoundLocInfo,
        log: FuneralPyreLog,
    ): Boolean {
        with(access) {
            interruptFiremakingQueues()
            if (!canUse(sendFailure = true) || !isCurrent(pyre)) {
                return false
            }
            val remains =
                ShadeRemains.all.asReversed().firstOrNull {
                    player.inv.contains(it.obj) &&
                        it.prayerXp(log) != null &&
                        (player.vars[varps.morttonquest] >= QUEST_COMPLETE_STATE ||
                            it.obj == objs.loar_remains)
                }
            if (remains == null) {
                mes("You do not have any shade remains that can be burned on this pyre.")
                return false
            }
            val slot = player.inv.firstSlot(remains.obj) ?: return false
            return addRemains(this, pyre, selectedSlot = slot, log, remains)
        }
    }

    public fun addRemains(
        access: ProtectedAccess,
        pyre: BoundLocInfo,
        selectedSlot: Int,
        log: FuneralPyreLog,
        remains: ShadeRemain,
    ): Boolean {
        with(access) {
            interruptFiremakingQueues()
            if (!canUse(sendFailure = true) || !isCurrent(pyre)) {
                return false
            }
            if (remains.prayerXp(log) == null) {
                mes("These remains need a higher tier of pyre logs.")
                return false
            }
            if (
                player.vars[varps.morttonquest] < QUEST_COMPLETE_STATE &&
                    (log.obj != objs.pyre_logs || remains.obj != objs.loar_remains)
            ) {
                mes("You need to complete Shades of Mort'ton before cremating these remains.")
                return false
            }
            if (player.inv[selectedSlot]?.id != remains.obj.id) {
                return false
            }
            if (!replace(pyre, log.remainsLoc, STAGE_DURATION_TICKS)) {
                return false
            }
            val deleted = invDel(player.inv, remains.obj, count = 1, slot = selectedSlot)
            if (deleted.failure) {
                replace(pyre, log.logLoc, STAGE_DURATION_TICKS)
                return false
            }
            remainsByPyre[pyre.coords.packed] = remains
            faceLoc(pyre)
            anim(seqs.temple_pyre_nobones)
            mes("You place the shade remains on the funeral pyre.")
            return true
        }
    }

    public fun light(access: ProtectedAccess, pyre: BoundLocInfo, log: FuneralPyreLog): Boolean {
        with(access) {
            interruptFiremakingQueues()
            if (!canUse(sendFailure = true) || !isCurrent(pyre)) {
                return false
            }
            if (!player.inv.contains(objs.tinderbox)) {
                mes("You need a tinderbox to light the funeral pyre.")
                return false
            }
            val remains = remainsByPyre[pyre.coords.packed]
            val prayerXp = remains?.prayerXp(log)
            if (remains == null || prayerXp == null) {
                mes("The funeral pyre is not ready to be lit.")
                return false
            }
            if (!replace(pyre, firemaking_locs.funeralPyre, Int.MAX_VALUE)) {
                return false
            }
            remainsByPyre.remove(pyre.coords.packed)
            faceLoc(pyre)
            anim(seqs.temple_pyre_fire)
            spotanimMap(worldRepo, spotanims.temple_pyre_fire, pyre.adjustedCentre)

            val firemakingMultiplier =
                if (player.vars[varbits.morytania_diary_elite_complete] != 0) 1.5 else 1.0
            val prayerMultiplier =
                if (player.vars[varbits.morytania_diary_hard_complete] != 0) 1.5 else 1.0
            statAdvance(stats.firemaking, log.firemakingXp * firemakingMultiplier)
            statAdvance(stats.prayer, prayerXp * prayerMultiplier)
            if (log.obj == objs.magic_pyre_logs || log.obj == objs.redwood_pyre_logs) player.completeDiaryTask(DiaryTask.CremateMagicShade)

            val reward = rollReward(remains)
            objRepo.add(
                reward.obj,
                rewardCoords(pyre, player.coords),
                duration = REWARD_DURATION_TICKS,
                receiver = player,
                count = reward.count,
                reveal = REWARD_DURATION_TICKS,
            )
            mes("You cremate the shade remains and free the spirit.")
            return true
        }
    }

    public fun rollReward(remains: ShadeRemain): ShadeReward {
        if (random.of(COIN_ROLL_DENOMINATOR) < COIN_ROLL_SUCCESS_COUNT) {
            return ShadeReward(objs.coins, random.of(remains.coinRange))
        }
        return ShadeReward(remains.keyAt(random.of(remains.totalKeyWeight)), count = 1)
    }

    private fun ProtectedAccess.canUse(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (!player.members) {
            return reject("You need to be on a members' world to use a funeral pyre.")
        }
        if (player.vars[varps.morttonquest] < MIN_QUEST_STATE) {
            return reject(
                "You need to progress further in Shades of Mort'ton to use a funeral pyre."
            )
        }
        return true
    }

    private fun ProtectedAccess.interruptFiremakingQueues() {
        clearWeakQueue(firemaking_queues.attempt)
        clearWeakQueue(firemaking_queues.pyreLogPreparation)
    }

    private fun isCurrent(pyre: BoundLocInfo): Boolean =
        locRepo.findAll(pyre.coords).any { it.entity == pyre.entity }

    private fun replace(pyre: BoundLocInfo, into: LocType, duration: Int): Boolean {
        if (!isCurrent(pyre)) {
            return false
        }
        val replacement =
            LocInfo(pyre.layer, pyre.coords, LocEntity(into.id, pyre.shapeId, pyre.angleId))
        return locRepo.add(replacement, duration)
    }

    private fun rewardCoords(pyre: BoundLocInfo, playerCoords: CoordGrid): CoordGrid {
        val centre = pyre.adjustedCentre
        val candidates =
            listOf(
                centre.translateZ(REWARD_STAND_OFFSET),
                centre.translateX(REWARD_STAND_OFFSET),
                centre.translateZ(-REWARD_STAND_OFFSET),
                centre.translateX(-REWARD_STAND_OFFSET),
            )
        val mapped = candidates.filter { locRepo.findExact(it, firemaking_locs.stoneStand) != null }
        return (mapped.ifEmpty { candidates }).minBy { squaredDistance(playerCoords, it) }
    }

    private fun squaredDistance(playerCoords: CoordGrid, target: CoordGrid): Int {
        val x = playerCoords.x - target.x
        val z = playerCoords.z - target.z
        return x * x + z * z
    }

    private fun Inventory.firstSlot(type: ObjType): Int? =
        indexOfFirst { it?.id == type.id }.takeIf { it >= 0 }

    public companion object {
        public const val MIN_QUEST_STATE: Int = 70
        public const val QUEST_COMPLETE_STATE: Int = 85
        public const val STAGE_DURATION_TICKS: Int = 50
        public const val REWARD_DURATION_TICKS: Int = 500
        public const val COIN_ROLL_DENOMINATOR: Int = 100
        public const val COIN_ROLL_SUCCESS_COUNT: Int = 21
        public const val REWARD_STAND_OFFSET: Int = 4
    }
}

public data class ShadeReward(val obj: ObjType, val count: Int)
