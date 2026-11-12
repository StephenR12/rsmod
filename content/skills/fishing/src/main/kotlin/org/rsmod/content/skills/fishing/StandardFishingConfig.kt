package org.rsmod.content.skills.fishing

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.varbit.VarBitType

public object SmallNetBaitFishingNpcs : NpcReferences() {
    val saltfish_43_51 = find("0_43_51_saltfish")
    val saltfish_44_52 = find("0_44_52_saltfish")
    val saltfish_44_53 = find("0_44_53_saltfish")
    val saltfish_45_49 = find("0_45_49_saltfish")
    val saltfish_46_49 = find("0_46_49_saltfish")
    val saltfish_47_57 = find("0_47_57_saltfish")
    val saltfish_48_50 = find("0_48_50_saltfish")
    val saltfish_51_49 = find("0_51_49_saltfish")
    val saltfish_50_49 = find("0_50_49_saltfish")
    val saltfish_39_55 = find("0_39_55_saltfish")
    val saltfish_41_57 = find("0_41_57_saltfish")
    val saltfish_28_56 = find("0_28_56_saltfish")
    val saltfish_23_53 = find("0_23_53_saltfish")
    val saltfish_24_53 = find("0_24_53_saltfish")
    val saltfish_26_54 = find("0_26_54_saltfish")
    val saltfish_27_59 = find("0_27_59_saltfish")
    val saltfish_39_44 = find("0_39_44_saltfish")
    val saltfish_24_51 = find("0_24_51_saltfish")
    val saltfish_21_46 = find("0_21_46_saltfish")
    val saltfish_20_46 = find("0_20_46_saltfish")
    val saltfish_20_47 = find("0_20_47_saltfish")

    val all: List<NpcType> =
        listOf(
            saltfish_43_51,
            saltfish_44_52,
            saltfish_44_53,
            saltfish_45_49,
            saltfish_46_49,
            saltfish_47_57,
            saltfish_48_50,
            saltfish_51_49,
            saltfish_50_49,
            saltfish_39_55,
            saltfish_41_57,
            saltfish_28_56,
            saltfish_23_53,
            saltfish_24_53,
            saltfish_26_54,
            saltfish_27_59,
            saltfish_39_44,
            saltfish_24_51,
            saltfish_21_46,
            saltfish_20_46,
            saltfish_20_47,
        )
}

public object LureBaitFishingNpcs : NpcReferences() {
    val freshfish_26_57 = find("0_26_57_freshfish")
    val freshfish_37_53 = find("0_37_53_freshfish")
    val freshfish_38_49 = find("0_38_49_freshfish")
    val freshfish_39_53 = find("0_39_53_freshfish")
    val freshfish_40_52 = find("0_40_52_freshfish")
    val freshfish_41_73 = find("0_41_73_freshfish")
    val freshfish_42_55 = find("0_42_55_freshfish")
    val freshfish_44_46 = find("0_44_46_freshfish")
    val freshfish_44_52 = find("0_44_52_freshfish")
    val freshfish_48_53 = find("0_48_53_freshfish")
    val freshfish_50_50 = find("0_50_50_freshfish")
    val freshfish_52_149 = find("0_52_149_freshfish")
    val freshfish_34_50 = find("0_34_50_freshfish")
    val freshfish_35_50 = find("0_35_50_freshfish")
    val freshfish_24_55 = find("0_24_55_freshfish")
    val freshfish_25_55 = find("0_25_55_freshfish")
    val freshfish_26_56 = find("0_26_56_freshfish")
    val freshfish_19_57 = find("0_19_57_freshfish")
    val freshfish_24_49 = find("0_24_49_freshfish")
    val freshfish_25_50 = find("0_25_50_freshfish")
    val freshfish_19_48 = find("0_19_48_freshfish")
    val freshfish_20_152 = find("0_20_152_freshfish")
    val freshfish_19_53 = find("0_19_53_freshfish")
    val freshfish_20_52 = find("0_20_52_freshfish")
    val freshfish_21_51 = find("0_21_51_freshfish")
    val freshfish_22_52 = find("0_22_52_freshfish")

    val all: List<NpcType> =
        listOf(
            freshfish_26_57,
            freshfish_37_53,
            freshfish_38_49,
            freshfish_39_53,
            freshfish_40_52,
            freshfish_41_73,
            freshfish_42_55,
            freshfish_44_46,
            freshfish_44_52,
            freshfish_48_53,
            freshfish_50_50,
            freshfish_52_149,
            freshfish_34_50,
            freshfish_35_50,
            freshfish_24_55,
            freshfish_25_55,
            freshfish_26_56,
            freshfish_19_57,
            freshfish_24_49,
            freshfish_25_50,
            freshfish_19_48,
            freshfish_20_152,
            freshfish_19_53,
            freshfish_20_52,
            freshfish_21_51,
            freshfish_22_52,
        )
}

public object CageHarpoonFishingNpcs : NpcReferences() {
    val rarefish_40_53 = find("0_40_53_rarefish")
    val rarefish_44_53 = find("0_44_53_rarefish")
    val rarefish_45_49 = find("0_45_49_rarefish")
    val rarefish_52_59 = find("52_59_rarefish")
    val rarefish_40_60 = find("0_40_60_rarefish")
    val rarefish_41_57 = find("0_41_57_rarefish")
    val tuna_37_59 = find("0_37_59_tuna")
    val rarefish_28_56 = find("0_28_56_rarefish")
    val rarefish_23_53 = find("0_23_53_rarefish")
    val rarefish_26_54 = find("0_26_54_rarefish")
    val rarefish_27_59 = find("0_27_59_rarefish")
    val rarefish_38_45 = find("0_38_45_rarefish")
    val rarefish_33_52 = find("0_33_52_rarefish")
    val rarefish_34_53 = find("0_34_53_rarefish")
    val memberfish_35_44 = find("0_35_44_memberfish")
    val rarefish_35_46 = find("0_35_46_rarefish")
    val memberfish_27_46 = find("0_27_46_memberfish")
    val rarefish_24_46 = find("0_24_46_rarefish")
    val rarefish_21_44 = find("0_21_44_rarefish")

    val nonCageHarpoon: List<NpcType> = listOf(tuna_37_59, memberfish_35_44, memberfish_27_46)

    val all: List<NpcType> =
        listOf(
            rarefish_40_53,
            rarefish_44_53,
            rarefish_45_49,
            rarefish_52_59,
            rarefish_40_60,
            rarefish_41_57,
            rarefish_28_56,
            rarefish_23_53,
            rarefish_26_54,
            rarefish_27_59,
            rarefish_38_45,
            rarefish_33_52,
            rarefish_34_53,
            rarefish_35_46,
            rarefish_24_46,
            rarefish_21_44,
        )

    val tunaSwordfish: List<NpcType> =
        listOf(
            rarefish_40_53,
            rarefish_44_53,
            rarefish_45_49,
            rarefish_52_59,
            rarefish_40_60,
            rarefish_41_57,
            tuna_37_59,
            rarefish_28_56,
            rarefish_23_53,
            rarefish_26_54,
            rarefish_27_59,
            rarefish_38_45,
            rarefish_33_52,
            rarefish_34_53,
            rarefish_35_46,
            memberfish_27_46,
            rarefish_24_46,
            rarefish_21_44,
        )
}

public object BigNetHarpoonFishingNpcs : NpcReferences() {
    val memberfish_40_53 = find("0_40_53_memberfish")
    val memberfish_44_53 = find("0_44_53_memberfish")
    val memberfish_33_51 = find("0_33_51_memberfish")
    val memberfish_41_57 = find("0_41_57_memberfish")
    val memberfish_54_49 = find("0_54_49_memberfish")
    val memberfish_55_49 = find("0_55_49_memberfish")
    val memberfish_43_42 = find("0_43_42_memberfish")
    val memberfish_42_42 = find("0_42_42_memberfish")
    val lobster_37_59 = find("0_37_59_lobster")
    val memberfish_28_56 = find("0_28_56_memberfish")
    val memberfish_23_53 = find("0_23_53_memberfish")
    val memberfish_26_54 = find("0_26_54_memberfish")
    val memberfish_18_57 = find("0_18_57_memberfish")
    val memberfish_19_58 = find("0_19_58_memberfish")
    val memberfish_18_58 = find("0_18_58_memberfish")
    val memberfish_33_52 = find("0_33_52_memberfish")
    val memberfish_34_53 = find("0_34_53_memberfish")
    val memberfish_24_46 = find("0_24_46_memberfish")
    val memberfish_24_51 = find("0_24_51_memberfish")
    val memberfish_19_49 = find("0_19_49_memberfish")

    val all: List<NpcType> =
        listOf(
            memberfish_40_53,
            memberfish_44_53,
            memberfish_33_51,
            memberfish_41_57,
            memberfish_54_49,
            memberfish_55_49,
            memberfish_43_42,
            memberfish_42_42,
            lobster_37_59,
            memberfish_28_56,
            memberfish_23_53,
            memberfish_26_54,
            memberfish_18_57,
            memberfish_19_58,
            memberfish_18_58,
            memberfish_33_52,
            memberfish_34_53,
            memberfish_24_46,
            memberfish_24_51,
            memberfish_19_49,
        )

    val bigNet: List<NpcType> =
        listOf(
            memberfish_40_53,
            memberfish_44_53,
            memberfish_33_51,
            memberfish_41_57,
            memberfish_54_49,
            memberfish_55_49,
            memberfish_43_42,
            memberfish_42_42,
            memberfish_28_56,
            memberfish_23_53,
            memberfish_26_54,
            memberfish_18_57,
            memberfish_19_58,
            memberfish_18_58,
            memberfish_33_52,
            memberfish_34_53,
            memberfish_24_46,
            memberfish_24_51,
            memberfish_19_49,
        )
}

public object BarbarianFishingNpcs : NpcReferences() {
    val ottosGrotto = find("0_39_54_brut_fishing_spot")
    val mountQuidamortem = find("0_19_55_brut_fishing_spot")

    val all: List<NpcType> = listOf(ottosGrotto, mountQuidamortem)
}

public object KarambwanjiFishingNpcs : NpcReferences() {
    val holyLake = find("0_43_47_karambwanji")

    val all: List<NpcType> = listOf(holyLake)
}

public object KarambwanFishingNpcs : NpcReferences() {
    val fixed = find("0_45_48_karambwan")
    val general = find("general_karambwan")
    val lubufuQuest = find("lubufu_karambwan")

    val postQuest: List<NpcType> = listOf(fixed, general)
}

public object CaveEelFishingNpcs : NpcReferences() {
    val lumbridgeEast = find("0_49_149_cavefish")
    val lumbridgeWest = find("0_50_149_cavefish")
    val dorgeshKaanEast = find("0_42_81_cavefish_east")
    val dorgeshKaanWest = find("0_42_81_cavefish_west")
    val camdozaal = find("0_45_90_cavefish")

    val all: List<NpcType> =
        listOf(lumbridgeEast, lumbridgeWest, dorgeshKaanEast, dorgeshKaanWest, camdozaal)
}

public object SwampEelFishingNpcs : NpcReferences() {
    val mortMyreNorth = find("0_54_53_slimey_eel")
    val mortMyreSouth = find("0_53_53_slimey_eel")
    val mortMyreWest = find("0_53_51_slimey_eel")

    val all: List<NpcType> = listOf(mortMyreNorth, mortMyreSouth, mortMyreWest)
}

public object LavaEelFishingNpcs : NpcReferences() {
    val taverleyDungeon = find("0_45_152_lavafish")
    val lavaMaze = find("0_47_59_lavafish")

    val all: List<NpcType> = listOf(taverleyDungeon, lavaMaze)
}

public object SpecialEelFishingNpcs : NpcReferences() {
    val infernal = find("morulrek_eels_fishingspot")
    val sacred = find("snakeboss_fishingspot")

    val all: List<NpcType> = listOf(infernal, sacred)
}

public object FishingGuildFishingNpcs {
    val all: List<NpcType> =
        listOf(CageHarpoonFishingNpcs.rarefish_40_53, BigNetHarpoonFishingNpcs.memberfish_40_53)
}

internal object SmallNetBaitFishingNpcEditor : NpcEditor() {
    init {
        SmallNetBaitFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object LureBaitFishingNpcEditor : NpcEditor() {
    init {
        LureBaitFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object CageHarpoonFishingNpcEditor : NpcEditor() {
    init {
        CageHarpoonFishingNpcs.tunaSwordfish.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object BigNetHarpoonFishingNpcEditor : NpcEditor() {
    init {
        BigNetHarpoonFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object BarbarianFishingNpcEditor : NpcEditor() {
    init {
        BarbarianFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object KarambwanjiFishingNpcEditor : NpcEditor() {
    init {
        KarambwanjiFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object KarambwanFishingNpcEditor : NpcEditor() {
    init {
        KarambwanFishingNpcs.postQuest.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object CaveEelFishingNpcEditor : NpcEditor() {
    init {
        CaveEelFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object SwampEelFishingNpcEditor : NpcEditor() {
    init {
        SwampEelFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object LavaEelFishingNpcEditor : NpcEditor() {
    init {
        LavaEelFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

internal object SpecialEelFishingNpcEditor : NpcEditor() {
    init {
        SpecialEelFishingNpcs.all.forEach(::freeze)
    }

    private fun freeze(type: NpcType) {
        edit(type) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}

public data class FishingCatch(
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val message: String,
    val infernalCookingXp: Double? = null,
    val rareReplacement: FishingRareReplacement? = null,
    val barehandedStrengthLevel: Int? = null,
    val barehandedStrengthXp: Double = 0.0,
    val baseStrengthLevel: Int? = null,
    val baseAgilityLevel: Int? = null,
    val strengthXp: Double = 0.0,
    val agilityXp: Double = 0.0,
)

public data class FishingRareReplacement(
    val output: ObjType,
    val chance: Int,
    val message: String,
    val membersOnly: Boolean = false,
)

public data class FishingMethod(
    val tools: List<FishingTool>,
    val bait: ObjType? = null,
    val baits: List<ObjType> = emptyList(),
    val missingBaitMessage: String = "You do not have any fishing bait.",
    val catches: List<FishingCatch>,
    val membersOnly: Boolean = false,
    val requiredWorn: List<ObjType> = emptyList(),
    val missingRequiredWornMessage: String = "You are not wearing the required equipment.",
    val varbitRequirement: FishingVarbitRequirement? = null,
    val extraRollDelayChance: Int? = null,
    val allowsBarehanded: Boolean = false,
    val requiresBarbarianRodTraining: Boolean = false,
)

public data class FishingVarbitRequirement(
    val type: VarBitType,
    val minimumValue: Int,
    val failureMessage: String,
)

public data class FishingTool(
    val type: ObjType,
    val animation: SeqType,
    val wearable: Boolean = false,
    val membersOnly: Boolean = false,
    val level: Int = 1,
    val catchRateMultiplier: Double = 1.0,
    val infernalDepletedType: ObjType? = null,
    val crystalDepletedType: ObjType? = null,
)

public data class RadasBlessing(val type: ObjType, val extraCatchPercent: Int)

public data class BigNetFishingRoll(
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val low: Int,
    val high: Int,
    val message: String,
    val baseLevelRequired: Boolean = false,
    val casketChanceBonus: Int = 0,
    val rareReplacement: FishingRareReplacement? = null,
)

public object StandardFishingMethods {
    val radasBlessings: List<RadasBlessing> =
        listOf(
            RadasBlessing(objs.diary_radas_blessing_4, 8),
            RadasBlessing(objs.diary_radas_blessing_3, 6),
            RadasBlessing(objs.diary_radas_blessing_2, 4),
            RadasBlessing(objs.diary_radas_blessing_1, 2),
        )

    val harpoonTools: List<FishingTool> =
        listOf(
            FishingTool(
                objs.crystal_harpoon,
                seqs.human_harpoon_crystal,
                wearable = true,
                membersOnly = true,
                level = 71,
                catchRateMultiplier = 1.35,
                crystalDepletedType = objs.crystal_harpoon_inactive,
            ),
            FishingTool(
                objs.infernal_harpoon,
                seqs.human_harpoon_infernal,
                wearable = true,
                membersOnly = true,
                level = 75,
                catchRateMultiplier = 1.2,
                infernalDepletedType = objs.infernal_harpoon_uncharged,
            ),
            FishingTool(
                objs.infernal_harpoon_or,
                seqs.human_harpoon_trailblazer,
                wearable = true,
                membersOnly = true,
                level = 75,
                catchRateMultiplier = 1.2,
                infernalDepletedType = objs.infernal_harpoon_or_uncharged,
            ),
            FishingTool(
                objs.infernal_harpoon_or_reloaded,
                seqs.human_harpoon_trailblazer_reloaded,
                wearable = true,
                membersOnly = true,
                level = 75,
                catchRateMultiplier = 1.2,
                infernalDepletedType = objs.infernal_harpoon_or_reloaded_uncharged,
            ),
            FishingTool(
                objs.dragon_harpoon,
                seqs.human_harpoon_dragon,
                wearable = true,
                membersOnly = true,
                level = 61,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.dragon_harpoon_or,
                seqs.human_harpoon_dragon_or,
                wearable = true,
                membersOnly = true,
                level = 61,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.dragon_harpoon_or_reloaded,
                seqs.human_harpoon_dragon_or_reloaded,
                wearable = true,
                membersOnly = true,
                level = 61,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.infernal_harpoon_uncharged,
                seqs.human_harpoon_infernal,
                wearable = true,
                membersOnly = true,
                level = 75,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.infernal_harpoon_or_uncharged,
                seqs.human_harpoon_trailblazer,
                wearable = true,
                membersOnly = true,
                level = 75,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.infernal_harpoon_or_reloaded_uncharged,
                seqs.human_harpoon_trailblazer_reloaded,
                wearable = true,
                membersOnly = true,
                level = 75,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.crystal_harpoon_inactive,
                seqs.human_harpoon_dragon,
                wearable = true,
                membersOnly = true,
                level = 71,
                catchRateMultiplier = 1.2,
            ),
            FishingTool(
                objs.barb_tail_harpoon,
                seqs.human_harpoon_barbed,
                wearable = true,
                membersOnly = true,
                level = 33,
            ),
            FishingTool(objs.harpoon, seqs.human_harpoon, level = 35),
        )

    val chargedSkillsNecklaces: List<ObjType> =
        listOf(
            objs.skills_necklace_1,
            objs.skills_necklace_2,
            objs.skills_necklace_3,
            objs.skills_necklace_4,
            objs.skills_necklace_5,
            objs.skills_necklace_6,
        )

    val bigNetRolls: List<BigNetFishingRoll> =
        listOf(
            BigNetFishingRoll(
                output = objs.leather_boots,
                level = 16,
                xp = 1.0,
                low = 10,
                high = 10,
                message = "You catch some leather boots.",
            ),
            BigNetFishingRoll(
                output = objs.seaweed,
                level = 16,
                xp = 1.0,
                low = 10,
                high = 10,
                message = "You catch some seaweed.",
            ),
            BigNetFishingRoll(
                output = objs.leather_gloves,
                level = 16,
                xp = 1.0,
                low = 10,
                high = 10,
                message = "You catch some leather gloves.",
            ),
            BigNetFishingRoll(
                output = objs.raw_mackerel,
                level = 16,
                xp = 20.0,
                low = 5,
                high = 65,
                message = "You catch a raw mackerel.",
            ),
            BigNetFishingRoll(
                output = objs.raw_mackerel,
                level = 16,
                xp = 20.0,
                low = 10,
                high = 10,
                message = "You catch a raw mackerel.",
            ),
            BigNetFishingRoll(
                output = objs.oyster,
                level = 16,
                xp = 10.0,
                low = 3,
                high = 7,
                message = "You catch an oyster.",
            ),
            BigNetFishingRoll(
                output = objs.casket,
                level = 16,
                xp = 10.0,
                low = 1,
                high = 2,
                message = "You catch a casket.",
                casketChanceBonus = 1,
            ),
            BigNetFishingRoll(
                output = objs.raw_cod,
                level = 23,
                xp = 45.0,
                low = 4,
                high = 55,
                message = "You catch a raw cod.",
            ),
            BigNetFishingRoll(
                output = objs.raw_bass,
                level = 46,
                xp = 100.0,
                low = 3,
                high = 40,
                message = "You catch a raw bass.",
                baseLevelRequired = true,
                rareReplacement =
                    FishingRareReplacement(
                        output = objs.big_bass,
                        chance = 1000,
                        message = "You catch an enormous bass!",
                        membersOnly = true,
                    ),
            ),
        )

    val smallNet =
        FishingMethod(
            tools = listOf(FishingTool(objs.small_fishing_net, seqs.human_smallnet)),
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_anchovies,
                        level = 15,
                        xp = 40.0,
                        low = 24,
                        high = 128,
                        message = "You catch some raw anchovies.",
                    ),
                    FishingCatch(
                        output = objs.raw_shrimps,
                        level = 1,
                        xp = 10.0,
                        low = 48,
                        high = 256,
                        message = "You catch some raw shrimps.",
                    ),
                ),
        )

    val bait =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_herring,
                        level = 10,
                        xp = 30.0,
                        low = 24,
                        high = 128,
                        message = "You catch a raw herring.",
                    ),
                    FishingCatch(
                        output = objs.raw_sardine,
                        level = 5,
                        xp = 20.0,
                        low = 32,
                        high = 192,
                        message = "You catch a raw sardine.",
                    ),
                ),
        )

    val caveEels =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_cave_eel,
                        level = 38,
                        xp = 80.0,
                        low = 10,
                        high = 80,
                        message = "You catch a raw cave eel.",
                    ),
                    FishingCatch(
                        output = objs.raw_slimy_eel,
                        level = 28,
                        xp = 80.0,
                        low = 10,
                        high = 80,
                        message = "You catch a raw slimy eel.",
                    ),
                ),
            membersOnly = true,
        )

    val frogspawnNet =
        FishingMethod(
            tools = listOf(FishingTool(objs.small_fishing_net, seqs.human_smallnet)),
            catches =
                listOf(
                    FishingCatch(
                        output = objs.frog_spawn,
                        level = 33,
                        xp = 75.0,
                        low = 16,
                        high = 96,
                        message = "You catch some frog spawn.",
                    ),
                    FishingCatch(
                        output = objs.swamp_weed,
                        level = 33,
                        xp = 1.0,
                        low = 10,
                        high = 10,
                        message = "You catch some swamp weed.",
                    ),
                ),
            membersOnly = true,
        )

    val swampSlimyEel =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_slimy_eel,
                        level = 28,
                        xp = 65.0,
                        low = 10,
                        high = 80,
                        message = "You catch a raw slimy eel.",
                    )
                ),
            membersOnly = true,
        )

    val lavaEel =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.oily_fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.oily_pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_lava_eel,
                        level = 53,
                        xp = 60.0,
                        low = 16,
                        high = 96,
                        message = "You catch a lava eel.",
                    )
                ),
        )

    val infernalEel =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.oily_fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.oily_pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.infernal_eel,
                        level = 80,
                        xp = 95.0,
                        low = 30,
                        high = 93,
                        message = "You catch an infernal eel.",
                    )
                ),
            membersOnly = true,
            requiredWorn = listOf(objs.ice_gloves, objs.smiths_gloves_i),
            missingRequiredWornMessage = "You need to be wearing ice gloves to fish here.",
        )

    val sacredEel =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.sacred_eel,
                        level = 87,
                        xp = 105.0,
                        low = 0,
                        high = 60,
                        message = "You catch a sacred eel.",
                    )
                ),
            membersOnly = true,
            varbitRequirement =
                FishingVarbitRequirement(
                    type = varbits.snakeboss_info,
                    minimumValue = 1,
                    failureMessage =
                        "You know nothing of the sacred contract between our tribe and the mighty " +
                            "Zulrah! Ask the High Priestess to educate you about Zulrah before you " +
                            "meddle further.",
                ),
        )

    val fly =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fly_fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fly_fishing_rod,
                        seqs.human_fishing_casting_pearl_fly,
                        wearable = true,
                    ),
                ),
            bait = objs.feather,
            missingBaitMessage = "You do not have any feathers.",
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_salmon,
                        level = 30,
                        xp = 70.0,
                        low = 16,
                        high = 96,
                        message = "You catch a raw salmon",
                    ),
                    FishingCatch(
                        output = objs.raw_trout,
                        level = 20,
                        xp = 50.0,
                        low = 32,
                        high = 192,
                        message = "You catch a raw trout",
                    ),
                ),
        )

    val pike =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fishing_rod,
                        seqs.human_fishing_casting_pearl,
                        wearable = true,
                    ),
                ),
            bait = objs.fishing_bait,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_pike,
                        level = 25,
                        xp = 60.0,
                        low = 16,
                        high = 96,
                        message = "You catch a pike.",
                    )
                ),
            extraRollDelayChance = 5,
        )

    val rainbow =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.fly_fishing_rod, seqs.human_fishing_casting),
                    FishingTool(
                        objs.pearl_fly_fishing_rod,
                        seqs.human_fishing_casting_pearl_fly,
                        wearable = true,
                    ),
                ),
            bait = objs.stripy_feather,
            missingBaitMessage = "You do not have any stripy feathers.",
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_rainbow_fish,
                        level = 38,
                        xp = 80.0,
                        low = 8,
                        high = 64,
                        message = "You catch a rainbow fish.",
                    )
                ),
            membersOnly = true,
        )

    val barbarianRod =
        FishingMethod(
            tools =
                listOf(
                    FishingTool(objs.barbarian_rod, seqs.human_fishing_onspot_barbarian),
                    FishingTool(
                        objs.pearl_barbarian_rod,
                        seqs.human_fishing_casting_pearl_barbarian,
                        wearable = true,
                    ),
                ),
            baits =
                listOf(objs.fish_offcuts, objs.fishing_bait, objs.feather, objs.roe, objs.caviar),
            missingBaitMessage = "You do not have any suitable bait.",
            catches =
                listOf(
                    FishingCatch(
                        output = objs.leaping_sturgeon,
                        level = 70,
                        xp = 80.0,
                        low = 8,
                        high = 64,
                        message = "You catch a leaping sturgeon.",
                        baseStrengthLevel = 45,
                        baseAgilityLevel = 45,
                        strengthXp = 7.0,
                        agilityXp = 7.0,
                    ),
                    FishingCatch(
                        output = objs.leaping_salmon,
                        level = 58,
                        xp = 70.0,
                        low = 16,
                        high = 96,
                        message = "You catch a leaping salmon.",
                        baseStrengthLevel = 30,
                        baseAgilityLevel = 30,
                        strengthXp = 6.0,
                        agilityXp = 6.0,
                    ),
                    FishingCatch(
                        output = objs.leaping_trout,
                        level = 48,
                        xp = 50.0,
                        low = 32,
                        high = 192,
                        message = "You catch a leaping trout.",
                        baseStrengthLevel = 15,
                        baseAgilityLevel = 15,
                        strengthXp = 5.0,
                        agilityXp = 5.0,
                    ),
                ),
            membersOnly = true,
            requiresBarbarianRodTraining = true,
        )

    val lobster =
        FishingMethod(
            tools = listOf(FishingTool(objs.lobster_pot, seqs.human_lobster)),
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_lobster,
                        level = 40,
                        xp = 90.0,
                        low = 6,
                        high = 95,
                        message = "You catch a lobster.",
                    )
                ),
        )

    val harpoon =
        FishingMethod(
            tools = harpoonTools,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_swordfish,
                        level = 50,
                        xp = 100.0,
                        low = 4,
                        high = 48,
                        message = "You catch a raw swordfish.",
                        infernalCookingXp = 70.0,
                        barehandedStrengthLevel = 50,
                        barehandedStrengthXp = 10.0,
                        rareReplacement =
                            FishingRareReplacement(
                                output = objs.big_swordfish,
                                chance = 2500,
                                message = "You catch an enormous swordfish!",
                                membersOnly = true,
                            ),
                    ),
                    FishingCatch(
                        output = objs.raw_tuna,
                        level = 35,
                        xp = 80.0,
                        low = 8,
                        high = 64,
                        message = "You catch a raw tuna.",
                        infernalCookingXp = 50.0,
                        barehandedStrengthLevel = 35,
                        barehandedStrengthXp = 8.0,
                    ),
                ),
            allowsBarehanded = true,
        )

    val shark =
        FishingMethod(
            tools = harpoonTools,
            catches =
                listOf(
                    FishingCatch(
                        output = objs.raw_shark,
                        level = 76,
                        xp = 110.0,
                        low = 3,
                        high = 40,
                        message = "You catch a shark!",
                        infernalCookingXp = 105.0,
                        barehandedStrengthLevel = 76,
                        barehandedStrengthXp = 11.0,
                        rareReplacement =
                            FishingRareReplacement(
                                output = objs.big_shark,
                                chance = 5000,
                                message = "You catch an enormous shark!",
                                membersOnly = true,
                            ),
                    )
                ),
            membersOnly = true,
            allowsBarehanded = true,
        )
}
