package org.rsmod.content.minigames.toa

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal object ToaLocs : LocReferences() {
    val entrance = find("toa_entrance")
    val exit = find("toa_door_exit")
    val scabarasDoor = find("toa_nexus_scabaras_door")
    val apmekenDoor = find("toa_nexus_apmeken_door")
    val crondisDoor = find("toa_nexus_crondis_door")
    val hetDoor = find("toa_nexus_het_door")
    val wardensDoor = find("toa_nexus_wardens_door")
    val wardensDoorOpen = find("toa_nexus_wardens_door_open")
    val scabarasButtons =
        listOf(
            find("toa_scabaras_memorygame_button1"),
            find("toa_scabaras_memorygame_button2"),
            find("toa_scabaras_memorygame_button3"),
            find("toa_scabaras_memorygame_button4"),
            find("toa_scabaras_memorygame_button5"),
            find("toa_scabaras_memorygame_button6"),
            find("toa_scabaras_memorygame_button7"),
            find("toa_scabaras_memorygame_button8"),
            find("toa_scabaras_memorygame_button9"),
        )
    val crondisWater = find("toa_crondis_water_source")
    val hetGoal = find("toa_het_goal")
    val poisonTile = find("toa_crondis_poison_tile")
    val kephriDung = find("toa_kephri_dung01")
}

internal object ToaNpcs : NpcReferences() {
    val kephriShielded = find("toa_kephri_boss_shielded")
    val kephriWeak = find("toa_kephri_boss_weak")
    val kephriEnrage = find("toa_kephri_boss_enrage")
    val kephriMelee = find("toa_kephri_guardian_melee")
    val kephriRanged = find("toa_kephri_guardian_ranged")
    val kephriMage = find("toa_kephri_guardian_mage")
    val zebak = find("toa_zebak")
    val zebakEnraged = find("toa_zebak_enraged")
    val baba = find("toa_baba")
    val babaBaboon = find("toa_baba_baboon")
    val babaBoulder = find("toa_baba_boulder")
    val babaBoulderWeak = find("toa_baba_boulder_weak")
    val akkhaMelee = find("akkha_melee")
    val akkhaRanged = find("akkha_range")
    val akkhaMage = find("akkha_mage")
    val akkhaEnrage = find("akkha_enrage")
    val akkhaShadow = find("akkha_shadow")
    val apmekenMelee = find("toa_path_apmeken_baboon_melee_1")
    val apmekenRanged = find("toa_path_apmeken_baboon_ranged_1")
    val apmekenMagic = find("toa_path_apmeken_baboon_magic_1")
    val apmekenShaman = find("toa_path_apmeken_baboon_shaman")
    val wardenP1 = find("toa_warden_tumeken_phase1")
    val wardenP2Mage = find("toa_warden_tumeken_phase2_mage")
    val wardenP2Range = find("toa_warden_tumeken_phase2_range")
    val wardenP3 = find("toa_warden_tumeken_phase3")
    val wardenP3Charging = find("toa_warden_tumeken_phase3_charging")

    val all =
        listOf(
            kephriShielded,
            kephriWeak,
            kephriEnrage,
            kephriMelee,
            kephriRanged,
            kephriMage,
            zebak,
            zebakEnraged,
            baba,
            babaBaboon,
            babaBoulder,
            babaBoulderWeak,
            akkhaMelee,
            akkhaRanged,
            akkhaMage,
            akkhaEnrage,
            akkhaShadow,
            apmekenMelee,
            apmekenRanged,
            apmekenMagic,
            apmekenShaman,
            wardenP1,
            wardenP2Mage,
            wardenP2Range,
            wardenP3,
            wardenP3Charging,
        )
}

internal object ToaSeqs : SeqReferences() {
    val kephriAttack = find("npc_kephri_attack_ranged")
    val babaAttack = find("npc_baba_attack_melee")
    val akkhaMelee = find("npc_akkha_attack_melee_spear")
    val akkhaRanged = find("npc_akkha_attack_ranged_spear")
    val akkhaMagic = find("npc_akkha_attack_magic_spear")
    val wardenMelee = find("npc_wardens_melee01")
    val wardenRanged = find("npc_wardens_ranged01")
    val wardenMagic = find("npc_wardens_magic01")
}

internal object ToaObjs : ObjReferences() {
    val honeyLocust = find("toa_honey_locust")
    val nectar = find("toa_supply_heal_4")
    val tears = find("toa_supply_prayer_4")
    val ambrosia = find("toa_supply_panicheal_2")
    val salts = find("toa_supply_stats_2")
    val fang = find("osmumtens_fang")
    val lightbearer = find("lightbearer")
    val ward = find("elidinis_ward")
    val masoriMask = find("masori_mask")
    val masoriBody = find("masori_body")
    val masoriChaps = find("masori_chaps")
    val shadow = find("tumekens_shadow_uncharged")
}

internal object ToaVarps : VarpReferences() {
    val entryCompletions = find("total_completed_tombsofamascut_entry")
    val normalCompletions = find("total_completed_tombsofamascut")
    val expertCompletions = find("total_completed_tombsofamascut_expert")
}

internal object ToaVarBits : VarBitReferences() {
    val raidLevel = find("toa_client_raid_level")
    val damage = find("toa_damage_done")
}
