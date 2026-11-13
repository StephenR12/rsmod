package org.rsmod.content.bosses.wilderness

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object WildernessBossNpcs : NpcReferences() {
    val callisto = find("callisto")
    val artio = find("callisto_singles")
    val venenatis = find("venenatis")
    val spindel = find("venenatis_singles")
    val vetion = find("vetion")
    val vetionEnraged = find("vetion_2")
    val calvarion = find("vetion_single")
    val calvarionEnraged = find("vetion_2_single")
    val juniorHound = find("vetion_hellhound_jnr")
    val seniorHound = find("vetion_hellhound_snr")
    val juniorHoundSingle = find("vetion_hellhound_jnr_singles")
    val seniorHoundSingle = find("vetion_hellhound_snr_singles")
    val venenatisSpiderling = find("venenatis_spiderling")
    val spindelSpiderling = find("spindel_spiderling")
    val scorpia = find("scorpia")
    val scorpiaGuardian = find("scorpia_guardian")
    val chaosElemental = find("chaoselemental")
    val chaosFanatic = find("chaos_fanatic")
    val crazyArchaeologist = find("crazy_archaeologist")
    val kingBlackDragon = find("king_dragon")
}

public object WildernessBossLocs : LocReferences() {
    val callistoEntrance = find("wild_callisto_entrance01")
    val artioEntrance = find("wild_callisto_singles_entrance01")
    val venenatisEntrance = find("wild_venanatis_entrance01")
    val spindelEntrance = find("wild_venenatis_singles_entrance01")
    val vetionEntrance = find("wild_vetion_entrance01")
    val calvarionEntrance = find("wild_vetion_singles_entrance01")
}

public object WildernessBossVarBits : VarBitReferences() {
    val entranceFeePaid = find("wbr_entrance_fee_paid")
}

public object WildernessBossObjs : ObjReferences() {
    val bigBones = find("big_bones")
    val dragonBones = find("dragon_bones")
    val blackDragonhide = find("dragonhide_black")
    val dragonPickaxe = find("dragon_pickaxe")
    val dragonTwoHander = find("dragon_2h_sword")
    val callistoClaws = find("wbr_callisto_claws")
    val venenatisFang = find("wbr_venenatis_fang")
    val vetionSkull = find("wbr_vetion_skull")
    val voidwakerHilt = find("wbr_voidwaker_hilt")
    val voidwakerBlade = find("wbr_voidwaker_blade")
    val voidwakerGem = find("wbr_voidwaker_gem")
    val ringOfGods = find("rotg")
    val tyrannicalRing = find("heavy_ring")
    val treasonousRing = find("sharp_ring")
    val odiumShard1 = find("odium_shard1")
    val odiumShard2 = find("odium_shard2")
    val odiumShard3 = find("odium_shard3")
    val maledictionShard1 = find("malediction_shard1")
    val maledictionShard2 = find("malediction_shard2")
    val maledictionShard3 = find("malediction_shard3")
    val dragonfireVisage = find("dragonfire_visage")
    val antiDragonShield = find("antidragonbreathshield")
    val dragonfireShield = find("dragonfire_shield")
    val dragonfireShieldUncharged = find("dragonfire_shield_uncharged")
    val dragonfireWard = find("dragonfire_ward")
    val callistoPet = find("callisto_pet")
    val venenatisPet = find("venenatis_pet")
    val vetionPet = find("vetion_pet")
    val scorpiaPet = find("scorpia_pet")
    val chaosElementalPet = find("chaoselepet")
    val kbdPet = find("kbdpet")
}

public object WildernessBossSeqs : SeqReferences() {
    val bearMelee = find("npc_callisto_attack_melee01")
    val bearRanged = find("npc_callisto_attack_ranged01")
    val bearMagic = find("npc_callisto_attack_magic01")
    val spiderMelee = find("npc_venenatis_melee_01")
    val spiderRanged = find("npc_venenatis_ranged_01")
    val spiderMagic = find("npc_venenatis_magic_01")
    val vetionMagic = find("npc_vetion_attack_magic_01")
    val vetionMelee = find("npc_vetion_attack_melee_01")
    val scorpion = find("scorpion_update_attack_tail")
    val chaosElemental = find("chaoselemental_attack")
    val dragonMelee = find("dragon_attack")
    val dragonfire = find("dragon_firebreath_all_attack")
}

public object WildernessBossSpots : SpotanimReferences() {
    val callistoRanged = find("fx_callisto_ranged_impact")
    val callistoTrap = find("fx_callisto_trap")
    val venenatisRanged = find("fx_venenatis_ranged_impact")
    val venenatisMagic = find("fx_venenatis_magic_impact")
    val venenatisWeb = find("fx_venenatis_web_impact")
    val vetionMagic = find("fx_vetion_attack_magic_impact_01")
    val chaosImpact = find("chaoselemental_spotanim_madness_impact")
    val book = find("crazy_archaeologist_book")
    val bookSpecial = find("crazy_archaeologist_book_special")
}

public enum class WildernessBossFamily {
    Callisto,
    Venenatis,
    Vetion,
    Scorpia,
    ChaosElemental,
    ChaosFanatic,
    CrazyArchaeologist,
    KingBlackDragon,
}

public data class WildernessBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: WildernessBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
    val attackRate: Int,
    val lesser: Boolean = false,
)

public object WildernessBosses {
    val definitions: List<WildernessBossDefinition> =
        listOf(
            boss(
                WildernessBossNpcs.callisto,
                3291,
                3849,
                WildernessBossFamily.Callisto,
                1000,
                350,
                300,
                225,
                140,
                200,
                55,
                4,
            ),
            boss(
                WildernessBossNpcs.artio,
                3116,
                3677,
                WildernessBossFamily.Callisto,
                450,
                250,
                270,
                150,
                90,
                120,
                35,
                5,
                true,
            ),
            boss(
                WildernessBossNpcs.venenatis,
                3315,
                3745,
                WildernessBossFamily.Venenatis,
                850,
                300,
                200,
                321,
                300,
                350,
                35,
                4,
            ),
            boss(
                WildernessBossNpcs.spindel,
                3045,
                10343,
                WildernessBossFamily.Venenatis,
                515,
                200,
                130,
                225,
                235,
                286,
                28,
                4,
                true,
            ),
            boss(
                WildernessBossNpcs.vetion,
                3278,
                3845,
                WildernessBossFamily.Vetion,
                255,
                430,
                390,
                395,
                300,
                1,
                46,
                5,
            ),
            boss(
                WildernessBossNpcs.calvarion,
                1881,
                11590,
                WildernessBossFamily.Vetion,
                150,
                220,
                220,
                220,
                200,
                1,
                35,
                5,
                true,
            ),
            boss(
                WildernessBossNpcs.scorpia,
                3233,
                10341,
                WildernessBossFamily.Scorpia,
                200,
                250,
                150,
                180,
                1,
                1,
                16,
                4,
            ),
            boss(
                WildernessBossNpcs.chaosElemental,
                3263,
                3922,
                WildernessBossFamily.ChaosElemental,
                250,
                270,
                270,
                270,
                270,
                270,
                28,
                5,
            ),
            boss(
                WildernessBossNpcs.chaosFanatic,
                2981,
                3848,
                WildernessBossFamily.ChaosFanatic,
                225,
                1,
                1,
                260,
                270,
                1,
                31,
                4,
            ),
            boss(
                WildernessBossNpcs.crazyArchaeologist,
                2980,
                3708,
                WildernessBossFamily.CrazyArchaeologist,
                225,
                1,
                1,
                160,
                240,
                1,
                25,
                4,
            ),
            boss(
                WildernessBossNpcs.kingBlackDragon,
                2271,
                4698,
                WildernessBossFamily.KingBlackDragon,
                240,
                240,
                240,
                240,
                240,
                1,
                65,
                4,
            ),
        )

    val byId: Map<Int, WildernessBossDefinition> = definitions.associateBy { it.npc.id }

    private fun boss(
        npc: NpcType,
        x: Int,
        z: Int,
        family: WildernessBossFamily,
        hp: Int,
        attack: Int,
        strength: Int,
        defence: Int,
        magic: Int,
        ranged: Int,
        maxHit: Int,
        rate: Int,
        lesser: Boolean = false,
    ) =
        WildernessBossDefinition(
            npc,
            CoordGrid(x, z, 0),
            family,
            hp,
            attack,
            strength,
            defence,
            magic,
            ranged,
            maxHit,
            rate,
            lesser,
        )
}

internal object WildernessBossAttackRanges : NpcEditor() {
    init {
        with(WildernessBossNpcs) {
            for (type in listOf(callisto, artio, venenatis, spindel, vetion, vetionEnraged, calvarion, calvarionEnraged)) {
                edit(type) { attackRange = 18 }
            }
            for (type in listOf(chaosElemental, chaosFanatic, crazyArchaeologist, kingBlackDragon)) {
                edit(type) { attackRange = 10 }
            }
        }
    }
}
