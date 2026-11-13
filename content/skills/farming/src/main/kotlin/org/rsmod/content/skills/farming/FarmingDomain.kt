package org.rsmod.content.skills.farming

import org.rsmod.game.type.obj.ObjType

public enum class FarmingPatchType {
    Allotment,
    Flower,
    Herb,
    Hops,
    Bush,
    Tree,
    FruitTree,
    Cactus,
    HardwoodTree,
    Seaweed,
    Mushroom,
    Belladonna,
    Calquat,
    SpiritTree,
    Celastrus,
    Redwood,
    CrystalTree,
    Anima,
    Hespori,
    Compost,
    Grapes,
}

public enum class FarmingCropStatus {
    Growing,
    Diseased,
    Dead,
    Harvestable,
    Stump,
}

public enum class CompostBinKind {
    Empty,
    Compost,
    Supercompost,
    RottenTomatoes,
    Ultracompost,
}

public enum class CompostBinStatus {
    Open,
    Rotting,
    Ready,
}

public enum class FarmingCompost(public val diseaseDenominator: Int, public val extraLives: Int) {
    None(1, 0),
    Compost(2, 1),
    Supercompost(5, 2),
    Ultracompost(10, 3),
    Saltpetre(1, 0),
}

public data class FarmingCrop(
    val key: String,
    val displayName: String,
    val patchType: FarmingPatchType,
    val seed: ObjType,
    val produce: ObjType?,
    val visualPrefixes: List<String>,
    val level: Int,
    val seedCount: Int,
    val growthMinutes: Int,
    val growthStages: Int,
    val baseHarvestLives: Int,
    val regrowMinutes: Int = 0,
    val plantXp: Double,
    val harvestXp: Double = 0.0,
    val healthXp: Double = 0.0,
    val compostAffectsYield: Boolean = true,
    /** Null when Jagex's per-stage chance is not publicly known; never substituted with a guess. */
    val diseaseChanceOutOf128: Int? = null,
    val diseaseImmune: Boolean = false,
    /** Exact cache/community-confirmed crop save constants. Null means the rate is unknown. */
    val harvestSaveLow: Int? = null,
    val harvestSaveHigh: Int? = null,
    val woodcuttingLevel: Int = 0,
    val woodcuttingXp: Double = 0.0,
    val logs: ObjType? = null,
    val roots: ObjType? = null,
    val protection: FarmingProtection? = null,
) {
    val growthCycles: Int
        get() = growthStages - 1
}

public data class FarmingProtection(val item: ObjType, val quantity: Int)

public data class FarmingPatchState(
    val patchKey: String,
    val patchType: FarmingPatchType,
    var weeds: Int = 3,
    var cropKey: String? = null,
    var stage: Int = 0,
    var status: FarmingCropStatus = FarmingCropStatus.Growing,
    var compost: FarmingCompost = FarmingCompost.None,
    var watered: Boolean = false,
    var harvestLives: Int = 0,
    var nextGrowthAt: Long = 0L,
    var resurrectionAttempted: Boolean = false,
    var healthChecked: Boolean = false,
    var protected: Boolean = false,
    var scarecrow: Boolean = false,
    var compostBinKind: CompostBinKind = CompostBinKind.Empty,
    var compostBinStatus: CompostBinStatus = CompostBinStatus.Open,
    var compostBinCount: Int = 0,
    var compostReadyAt: Long = 0L,
) {
    public val isEmpty: Boolean
        get() = cropKey == null
}

public object FarmingRules {
    /** Jagex rolls a crop-specific disease chance on each eligible growth cycle. */
    public fun diseaseRollSucceeds(
        baseChanceOutOf128: Int,
        compost: FarmingCompost,
        roll: Int,
    ): Boolean {
        require(baseChanceOutOf128 in 0..128)
        require(roll in 0..<128)
        if (baseChanceOutOf128 == 0) return false
        val reducedChance =
            (baseChanceOutOf128 + compost.diseaseDenominator - 1) / compost.diseaseDenominator
        return roll < reducedChance
    }

    /** Minimum harvest lives are 3, increased by one for each compost tier. */
    public fun minimumHarvestLives(crop: FarmingCrop, compost: FarmingCompost): Int =
        crop.baseHarvestLives + if (crop.compostAffectsYield) compost.extraLives else 0

    /** Returns the numerator of the Wiki's chance-to-save formula (denominator 256). */
    public fun harvestSaveChance(level: Int, low: Int, high: Int, itemBonusPercent: Int = 0): Int {
        require(level in 1..99)
        val boostedLow = low * (100 + itemBonusPercent) / 100
        val boostedHigh = high * (100 + itemBonusPercent) / 100
        val interpolated =
            boostedLow * (99 - level).toDouble() / 98.0 +
                boostedHigh * (level - 1).toDouble() / 98.0
        return 1 + kotlin.math.floor(interpolated + 0.5).toInt()
    }
}
