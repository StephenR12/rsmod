package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

public data class BarbarianPyreLog(
    val log: ObjType,
    val level: Int,
    val craftingXp: Double,
    val firemakingXp: Double,
    val prayerCharges: Int,
)

public object BarbarianPyreLogs {
    public val all: List<BarbarianPyreLog> by lazy {
        listOf(
            recipe(objs.logs, 11, 10.0, 40.0, 1),
            recipe(objs.achey_tree_logs, 11, 10.0, 40.0, 1),
            recipe(objs.oak_logs, 25, 15.0, 60.0, 2),
            recipe(objs.willow_logs, 40, 22.5, 90.0, 2),
            recipe(objs.teak_logs, 45, 26.2, 105.0, 3),
            recipe(objs.arctic_pine_logs, 52, 31.2, 125.0, 3),
            recipe(objs.maple_logs, 55, 33.7, 135.0, 3),
            recipe(objs.mahogany_logs, 60, 39.3, 157.5, 4),
            recipe(objs.yew_logs, 70, 50.6, 202.5, 4),
            recipe(objs.magic_logs, 85, 75.9, 303.8, 5),
            recipe(objs.redwood_logs, 95, 87.0, 350.0, 5),
        )
    }

    public val byLog: Map<Int, BarbarianPyreLog> by lazy { all.associateBy { it.log.id } }

    private fun recipe(
        log: ObjType,
        level: Int,
        craftingXp: Double,
        firemakingXp: Double,
        prayerCharges: Int,
    ): BarbarianPyreLog = BarbarianPyreLog(log, level, craftingXp, firemakingXp, prayerCharges)
}

public data class BarbarianPyreAxe(val obj: ObjType, val carveSequence: SeqType)

public object BarbarianPyreAxes {
    public val all: List<BarbarianPyreAxe> by lazy {
        listOf(
            axe(objs.crystal_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.crystal_felling_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.third_age_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.league_trailblazer_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.infernal_axe_or, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.infernal_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.infernal_axe_or_uncharged, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.infernal_axe_uncharged, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.dragon_axe_or, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.dragon_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.dragon_felling_axe, seqs.human_canoeing_carve_dragon_axe),
            axe(objs.rune_axe, seqs.human_canoeing_carve_rune_axe),
            axe(objs.rune_felling_axe, seqs.human_canoeing_carve_rune_axe),
            axe(objs.gilded_axe, seqs.human_canoeing_carve_rune_axe),
            axe(objs.adamant_axe, seqs.human_canoeing_carve_adamant_axe),
            axe(objs.adamant_felling_axe, seqs.human_canoeing_carve_adamant_axe),
            axe(objs.mithril_axe, seqs.human_canoeing_carve_mithril_axe),
            axe(objs.mithril_felling_axe, seqs.human_canoeing_carve_mithril_axe),
            axe(objs.black_axe, seqs.human_canoeing_carve_black_axe),
            axe(objs.black_felling_axe, seqs.human_canoeing_carve_black_axe),
            axe(objs.steel_axe, seqs.human_canoeing_carve_steel_axe),
            axe(objs.steel_felling_axe, seqs.human_canoeing_carve_steel_axe),
            axe(objs.iron_axe, seqs.human_canoeing_carve_iron_axe),
            axe(objs.iron_felling_axe, seqs.human_canoeing_carve_iron_axe),
            axe(objs.bronze_axe, seqs.human_canoeing_carve_bronze_axe),
            axe(objs.bronze_felling_axe, seqs.human_canoeing_carve_bronze_axe),
        )
    }

    public val byObj: Map<Int, BarbarianPyreAxe> by lazy { all.associateBy { it.obj.id } }

    private fun axe(obj: ObjType, sequence: SeqType): BarbarianPyreAxe =
        BarbarianPyreAxe(obj, sequence)
}

public data class BarbarianPyreReward(val obj: ObjType, val count: IntRange, val weight: Int)

public object BarbarianPyreRewards {
    public const val MAIN_ROLL_WEIGHT: Int = 128

    public val main: List<BarbarianPyreReward> by lazy {
        listOf(
            reward(objs.noted_ranarr_potion_unf, 2, 8),
            reward(objs.antipoison_supermix_2, 1, 4),
            reward(objs.antifire_mix_1, 1, 4),
            reward(objs.antifire_mix_2, 1, 4),
            reward(objs.fishing_mix_2, 1, 4),
            reward(objs.prayer_mix_1, 1, 4),
            reward(objs.prayer_mix_2, 1, 4),
            reward(objs.superattack_mix_1, 1, 4),
            reward(objs.superattack_mix_2, 1, 4),
            reward(objs.super_strength_mix_1, 1, 4),
            reward(objs.super_strength_mix_2, 1, 4),
            reward(objs.super_defence_mix_1, 1, 4),
            reward(objs.super_defence_mix_2, 1, 4),
            reward(objs.death_rune, 8..15, 18),
            reward(objs.blood_rune, 4..7, 10),
            reward(objs.silver_bolts, 5, 6),
            reward(objs.rune_arrow, 10, 6),
            reward(objs.runite_bolts, 10, 6),
            reward(objs.noted_diamond, 2, 8),
            reward(objs.mith_grapple, 2, 6),
            reward(objs.adamant_dart_p, 20, 6),
            reward(objs.adamant_throwing_knife, 20, 6),
        )
    }

    public val totalWeight: Int by lazy { main.sumOf { it.weight } }

    public fun at(roll: Int): BarbarianPyreReward {
        require(roll in 0 until totalWeight)
        var remaining = roll
        for (reward in main) {
            if (remaining < reward.weight) {
                return reward
            }
            remaining -= reward.weight
        }
        error("Unreachable reward roll: $roll")
    }

    private fun reward(obj: ObjType, count: Int, weight: Int): BarbarianPyreReward =
        reward(obj, count..count, weight)

    private fun reward(obj: ObjType, count: IntRange, weight: Int): BarbarianPyreReward =
        BarbarianPyreReward(obj, count, weight)
}
