package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

public data class FuneralPyreLog(
    val obj: ObjType,
    val logLoc: LocType,
    val remainsLoc: LocType,
    val level: Int,
    val firemakingXp: Double,
    val tier: Int,
)

public object FuneralPyreLogs {
    public val all: List<FuneralPyreLog> by lazy {
        listOf(
            FuneralPyreLog(
                objs.pyre_logs,
                firemaking_locs.funeralPyreLogs,
                firemaking_locs.funeralPyreRemainsLogs,
                level = 5,
                firemakingXp = 50.0,
                tier = 0,
            ),
            FuneralPyreLog(
                objs.oak_pyre_logs,
                firemaking_locs.funeralPyreOak,
                firemaking_locs.funeralPyreRemainsOak,
                level = 20,
                firemakingXp = 70.0,
                tier = 1,
            ),
            FuneralPyreLog(
                objs.willow_pyre_logs,
                firemaking_locs.funeralPyreWillow,
                firemaking_locs.funeralPyreRemainsWillow,
                level = 35,
                firemakingXp = 100.0,
                tier = 2,
            ),
            FuneralPyreLog(
                objs.teak_pyre_logs,
                firemaking_locs.funeralPyreTeak,
                firemaking_locs.funeralPyreRemainsTeak,
                level = 40,
                firemakingXp = 120.0,
                tier = 3,
            ),
            FuneralPyreLog(
                objs.arctic_pine_pyre_logs,
                firemaking_locs.funeralPyreArcticPine,
                firemaking_locs.funeralPyreRemainsArcticPine,
                level = 47,
                firemakingXp = 158.0,
                tier = 4,
            ),
            FuneralPyreLog(
                objs.maple_pyre_logs,
                firemaking_locs.funeralPyreMaple,
                firemaking_locs.funeralPyreRemainsMaple,
                level = 50,
                firemakingXp = 175.0,
                tier = 5,
            ),
            FuneralPyreLog(
                objs.mahogany_pyre_logs,
                firemaking_locs.funeralPyreMahogany,
                firemaking_locs.funeralPyreRemainsMahogany,
                level = 55,
                firemakingXp = 210.0,
                tier = 6,
            ),
            FuneralPyreLog(
                objs.yew_pyre_logs,
                firemaking_locs.funeralPyreYew,
                firemaking_locs.funeralPyreRemainsYew,
                level = 65,
                firemakingXp = 255.0,
                tier = 7,
            ),
            FuneralPyreLog(
                objs.magic_pyre_logs,
                firemaking_locs.funeralPyreMagic,
                firemaking_locs.funeralPyreRemainsMagic,
                level = 80,
                firemakingXp = 404.5,
                tier = 8,
            ),
            FuneralPyreLog(
                objs.redwood_pyre_logs,
                firemaking_locs.funeralPyreRedwood,
                firemaking_locs.funeralPyreRemainsRedwood,
                level = 95,
                firemakingXp = 500.0,
                tier = 9,
            ),
        )
    }

    public val byObj: Map<Int, FuneralPyreLog> by lazy { all.associateBy { it.obj.id } }
    public val byLogLoc: Map<Int, FuneralPyreLog> by lazy { all.associateBy { it.logLoc.id } }
    public val byRemainsLoc: Map<Int, FuneralPyreLog> by lazy {
        all.associateBy { it.remainsLoc.id }
    }
}

public data class WeightedShadeKey(val obj: ObjType, val weight: Int)

public data class ShadeRemain(
    val obj: ObjType,
    val minLogTier: Int,
    val prayerXpByLogTier: List<Double?>,
    val coinRange: IntRange,
    val keyWeights: List<WeightedShadeKey>,
) {
    public val totalKeyWeight: Int
        get() = keyWeights.sumOf { it.weight }

    public fun prayerXp(log: FuneralPyreLog): Double? = prayerXpByLogTier.getOrNull(log.tier)

    public fun keyAt(roll: Int): ObjType {
        require(roll in 0 until totalKeyWeight)
        var cursor = roll
        for ((key, weight) in keyWeights) {
            if (cursor < weight) {
                return key
            }
            cursor -= weight
        }
        throw IllegalStateException("Shade-key roll was not covered: $roll")
    }
}

public object ShadeRemains {
    public val all: List<ShadeRemain> by lazy {
        listOf(
            ShadeRemain(
                objs.loar_remains,
                minLogTier = 0,
                prayerXpByLogTier =
                    listOf(25.0, 33.0, 33.5, 33.7, 33.9, 34.0, 34.3, 34.5, 35.0, 35.5),
                coinRange = 200..300,
                keyWeights =
                    listOf(
                        WeightedShadeKey(objs.bronze_key_red, 35),
                        WeightedShadeKey(objs.bronze_key_brown, 30),
                        WeightedShadeKey(objs.bronze_key_crimson, 21),
                    ),
            ),
            ShadeRemain(
                objs.phrin_remains,
                minLogTier = 0,
                prayerXpByLogTier =
                    listOf(37.5, 45.5, 46.0, 46.2, 46.4, 46.5, 46.8, 47.0, 47.5, 48.0),
                coinRange = 400..500,
                keyWeights =
                    listOf(
                        WeightedShadeKey(objs.steel_key_red, 35),
                        WeightedShadeKey(objs.steel_key_brown, 30),
                        WeightedShadeKey(objs.steel_key_crimson, 20),
                        WeightedShadeKey(objs.bronze_key_black, 10),
                        WeightedShadeKey(objs.bronze_key_purple, 6),
                    ),
            ),
            ShadeRemain(
                objs.riyl_remains,
                minLogTier = 2,
                prayerXpByLogTier =
                    listOf(null, null, 61.0, 61.2, 61.4, 61.5, 61.8, 62.0, 62.5, 63.0),
                coinRange = 600..700,
                keyWeights =
                    listOf(
                        WeightedShadeKey(objs.black_key_red, 35),
                        WeightedShadeKey(objs.black_key_brown, 30),
                        WeightedShadeKey(objs.black_key_crimson, 20),
                        WeightedShadeKey(objs.steel_key_black, 10),
                        WeightedShadeKey(objs.steel_key_purple, 6),
                    ),
            ),
            ShadeRemain(
                objs.asyn_remains,
                minLogTier = 7,
                prayerXpByLogTier =
                    listOf(null, null, null, null, null, null, null, 79.5, 80.0, 80.5),
                coinRange = 800..900,
                keyWeights =
                    listOf(
                        WeightedShadeKey(objs.silver_key_red, 35),
                        WeightedShadeKey(objs.silver_key_brown, 30),
                        WeightedShadeKey(objs.black_key_crimson, 20),
                        WeightedShadeKey(objs.black_key_black, 10),
                        WeightedShadeKey(objs.black_key_purple, 6),
                    ),
            ),
            ShadeRemain(
                objs.fiyr_remains,
                minLogTier = 8,
                prayerXpByLogTier =
                    listOf(null, null, null, null, null, null, null, null, 100.0, 100.5),
                coinRange = 2000..4000,
                keyWeights =
                    listOf(
                        WeightedShadeKey(objs.gold_key_red, 20),
                        WeightedShadeKey(objs.silver_key_brown, 20),
                        WeightedShadeKey(objs.silver_key_crimson, 45),
                        WeightedShadeKey(objs.silver_key_black, 10),
                        WeightedShadeKey(objs.silver_key_purple, 6),
                    ),
            ),
            ShadeRemain(
                objs.urium_remains,
                minLogTier = 9,
                prayerXpByLogTier =
                    listOf(null, null, null, null, null, null, null, null, null, 120.5),
                coinRange = 2000..7000,
                keyWeights =
                    listOf(
                        WeightedShadeKey(objs.gold_key_crimson, 45),
                        WeightedShadeKey(objs.gold_key_red, 20),
                        WeightedShadeKey(objs.gold_key_brown, 20),
                        WeightedShadeKey(objs.gold_key_black, 10),
                        WeightedShadeKey(objs.gold_key_purple, 6),
                    ),
            ),
        )
    }

    public val byObj: Map<Int, ShadeRemain> by lazy { all.associateBy { it.obj.id } }
}
