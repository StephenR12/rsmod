package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

@Singleton
public class FarmingPatchRepository
@Inject
constructor(private val crops: FarmingCropCatalogue, private val random: GameRandom) {
    private val states = WeakHashMap<Player, MutableMap<String, FarmingPatchState>>()

    public fun getOrCreate(
        player: Player,
        patchKey: String,
        patchType: FarmingPatchType,
    ): FarmingPatchState =
        synchronized(states) {
            states
                .getOrPut(player) { mutableMapOf() }
                .getOrPut(patchKey) { FarmingPatchState(patchKey, patchType) }
        }

    public fun find(player: Player, patchKey: String): FarmingPatchState? =
        synchronized(states) { states[player]?.get(patchKey) }

    public fun replace(player: Player, loaded: Collection<FarmingPatchState>) {
        synchronized(states) {
            states[player] = loaded.associateByTo(mutableMapOf()) { it.patchKey }
        }
    }

    public fun snapshot(player: Player): List<FarmingPatchState> =
        synchronized(states) { states[player]?.values?.map(FarmingPatchState::copy).orEmpty() }

    public fun advance(
        player: Player,
        state: FarmingPatchState,
        now: Long = System.currentTimeMillis(),
    ) {
        if (state.patchType == FarmingPatchType.Compost) {
            if (
                state.compostBinStatus == CompostBinStatus.Rotting && state.compostReadyAt in 1..now
            ) {
                state.compostBinStatus = CompostBinStatus.Ready
                state.compostReadyAt = 0L
            }
            return
        }
        if (state.scarecrow && state.cropKey == null) {
            while (state.nextGrowthAt in 1..now && state.weeds < 3) {
                state.weeds++
                state.nextGrowthAt += 5 * MILLIS_PER_MINUTE
            }
            return
        }
        val crop = state.cropKey?.let(crops::get) ?: return
        while (state.nextGrowthAt in 1..now && state.status != FarmingCropStatus.Dead) {
            when (state.status) {
                FarmingCropStatus.Diseased -> {
                    state.status = FarmingCropStatus.Dead
                    state.nextGrowthAt = 0L
                }
                FarmingCropStatus.Growing -> advanceHealthy(player, state, crop)
                FarmingCropStatus.Harvestable -> {
                    if (crop.regrowMinutes <= 0 || state.harvestLives > 0) return
                    state.harvestLives = FarmingRules.minimumHarvestLives(crop, state.compost)
                    // A depleted perennial has finished its single regrowth cycle. It stays
                    // harvestable until the player picks the newly-grown produce.
                    state.nextGrowthAt = 0L
                }
                FarmingCropStatus.Stump -> {
                    state.status = FarmingCropStatus.Harvestable
                    state.nextGrowthAt = 0L
                }
                FarmingCropStatus.Dead -> return
            }
        }
    }

    private fun advanceHealthy(player: Player, state: FarmingPatchState, crop: FarmingCrop) {
        val diseaseChance = crop.diseaseChanceOutOf128
        val canDisease =
            state.stage > 0 &&
                state.stage < crop.growthCycles &&
                !crop.diseaseImmune &&
                !isProtected(player, state, crop)
        if (
            canDisease &&
                diseaseChance != null &&
                FarmingRules.diseaseRollSucceeds(diseaseChance, state.compost, random.of(128))
        ) {
            state.status = FarmingCropStatus.Diseased
            state.watered = false
            state.nextGrowthAt += crop.growthMinutes * MILLIS_PER_MINUTE
            return
        }

        state.stage++
        state.watered = false
        if (state.stage >= crop.growthCycles) {
            state.stage = crop.growthCycles
            state.status = FarmingCropStatus.Harvestable
            state.harvestLives = FarmingRules.minimumHarvestLives(crop, state.compost)
            state.nextGrowthAt = 0L
        } else {
            state.nextGrowthAt += crop.growthMinutes * MILLIS_PER_MINUTE
        }
    }

    private fun isProtected(player: Player, state: FarmingPatchState, crop: FarmingCrop): Boolean {
        if (state.protected) return true
        if (state.patchType != FarmingPatchType.Allotment) return false
        val square = state.patchKey.substringBefore(':')
        val nearby = synchronized(states) { states[player]?.values?.toList().orEmpty() }
        val flowers =
            nearby.filter {
                it.patchKey.startsWith("$square:") && it.patchType == FarmingPatchType.Flower
            }
        if (flowers.any { it.scarecrow } && crop.key == "sweetcorn") return true
        val grownFlowers =
            flowers.mapNotNull { patch ->
                if (patch.status == FarmingCropStatus.Harvestable) patch.cropKey else null
            }
        if ("white_lily" in grownFlowers) return true
        val protector =
            when (crop.key) {
                "potato",
                "onion" -> "marigold"
                "cabbage" -> "rosemary"
                "tomato" -> "marigold"
                "watermelon" -> "nasturtium"
                else -> null
            }
        return protector != null && protector in grownFlowers
    }

    public fun firstGrowthTick(player: Player, minutes: Int, now: Long): Long {
        val interval = minutes * MILLIS_PER_MINUTE
        val maximumOffset = minOf(30, minutes) * MILLIS_PER_MINUTE
        val offset = Math.floorMod(player.displayName.hashCode().toLong(), maximumOffset)
        return Math.floorDiv(now + offset, interval) * interval + interval - offset
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
