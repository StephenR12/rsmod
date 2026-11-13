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
        if (state.patchType == FarmingPatchType.Grapes) state.weeds = 0
        if (state.isEmpty) {
            if (state.patchType == FarmingPatchType.Grapes || state.weeds >= 3) return
            if (player.vars[TitheRewardVars.autoWeed] == 1) {
                state.nextGrowthAt = 0L
                return
            }
            if (state.nextGrowthAt == 0L) state.nextGrowthAt = firstGrowthTick(player, 5, now)
            while (state.nextGrowthAt <= now && state.weeds < 3) {
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
                    if (crop.regrowMinutes <= 0) return
                    val capacity = FarmingRules.minimumHarvestLives(crop, state.compost)
                    state.harvestLives =
                        (state.harvestLives +
                                growthStages(player, state.nextGrowthAt, crop.regrowMinutes))
                            .coerceAtMost(capacity)
                    state.nextGrowthAt =
                        if (state.harvestLives == capacity) 0L
                        else state.nextGrowthAt + crop.regrowMinutes * MILLIS_PER_MINUTE
                }
                FarmingCropStatus.Stump -> {
                    state.status = FarmingCropStatus.Harvestable
                    state.nextGrowthAt = 0L
                }
                FarmingCropStatus.Dead -> return
            }
        }
    }

    internal fun advanceExisting(player: Player) {
        val existing = synchronized(states) { states[player]?.values?.toList().orEmpty() }
        for (state in existing) advance(player, state)
    }

    internal fun activeAnima(player: Player, at: Long = System.currentTimeMillis()): String? {
        val anima =
            synchronized(states) {
                states[player]?.values?.firstOrNull { it.patchType == FarmingPatchType.Anima }
            } ?: return null
        val interval = 640 * MILLIS_PER_MINUTE
        if (anima.isEmpty || anima.nextGrowthAt == 0L) return null
        val expiry = anima.nextGrowthAt + (7 - anima.stage).coerceAtLeast(0) * interval
        return anima.cropKey.takeIf { at in (expiry - 8 * interval) until expiry }
    }

    private fun growthStages(player: Player, at: Long, minutes: Int): Int {
        if (activeAnima(player, at) != "kronos") return 1
        // All crops in the same period share the skip roll, including when advanced after logout.
        // The original per-stage success probability is unpublished.
        val roll =
            kotlin.random.Random(at xor minutes.toLong() xor player.displayName.hashCode().toLong())
        var steps = 1
        while (steps < 16 && roll.nextInt(10) == 0) steps++
        return steps
    }

    private fun advanceHealthy(player: Player, state: FarmingPatchState, crop: FarmingCrop) {
        if (crop.patchType == FarmingPatchType.Anima) {
            state.stage++
            if (state.stage >= crop.growthCycles) state.status = FarmingCropStatus.Dead
            else state.nextGrowthAt += crop.growthMinutes * MILLIS_PER_MINUTE
            return
        }
        val diseaseChance = crop.diseaseChanceOutOf128
        val canDisease =
            state.stage > 0 &&
                state.stage < crop.growthCycles &&
                !crop.diseaseImmune &&
                !isProtected(player, state, crop)
        if (
            canDisease &&
                diseaseChance != null &&
                FarmingRules.diseaseRollSucceeds(diseaseChance, state.compost, random.of(128)) &&
                (activeAnima(player, state.nextGrowthAt) != "iasor" || random.of(5) == 0)
        ) {
            state.status = FarmingCropStatus.Diseased
            state.watered = false
            state.nextGrowthAt += crop.growthMinutes * MILLIS_PER_MINUTE
            return
        }

        state.stage += growthStages(player, state.nextGrowthAt, crop.growthMinutes)
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
