package org.rsmod.content.minigames.tob

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.npc.NpcType

internal object TobLocs : LocReferences() {
    val entrance = find("tob_surface_raid_entrance")
    val exit = find("tob_dungeon_verzik_entrance_door")
    val sotetsegPath = find("tob_sotetseg_lighttile")
    val xarpusAcid = find("tob_xarpus_acidpool")
    val midwayChest = find("tob_midway_chest_closed")
    val midwayChestOpen = find("tob_midway_chest_open")
}

internal object TobNpcs : NpcReferences() {
    val maiden = find("tob_maiden_100")
    val maidenEntry = find("tob_maiden_100_story")
    val maidenHard = find("tob_maiden_100_hard")
    val maidenHealer = find("maiden_elemental")
    val maidenHealerEntry = find("maiden_elemental_story")
    val maidenHealerHard = find("maiden_elemental_hard")
    val bloat = find("tob_bloat")
    val bloatEntry = find("tob_bloat_story")
    val bloatHard = find("tob_bloat_hard")
    val nyloMelee = find("tob_nylocas_fighting_melee")
    val nyloRanged = find("tob_nylocas_fighting_ranged")
    val nyloMagic = find("tob_nylocas_fighting_magic")
    val nyloMeleeEntry = find("tob_nylocas_fighting_melee_story")
    val nyloRangedEntry = find("tob_nylocas_fighting_ranged_story")
    val nyloMagicEntry = find("tob_nylocas_fighting_magic_story")
    val nyloMeleeHard = find("tob_nylocas_fighting_melee_hard")
    val nyloRangedHard = find("tob_nylocas_fighting_ranged_hard")
    val nyloMagicHard = find("tob_nylocas_fighting_magic_hard")
    val nyloBossMelee = find("nylocas_boss_melee")
    val nyloBossRanged = find("nylocas_boss_ranged")
    val nyloBossMagic = find("nylocas_boss_magic")
    val nyloBossMeleeEntry = find("nylocas_boss_melee_story")
    val nyloBossRangedEntry = find("nylocas_boss_ranged_story")
    val nyloBossMagicEntry = find("nylocas_boss_magic_story")
    val nyloBossMeleeHard = find("nylocas_boss_melee_hard")
    val nyloBossRangedHard = find("nylocas_boss_ranged_hard")
    val nyloBossMagicHard = find("nylocas_boss_magic_hard")
    val sotetseg = find("tob_sotetseg_combat")
    val sotetsegEntry = find("tob_sotetseg_combat_story")
    val sotetsegHard = find("tob_sotetseg_combat_hard")
    val xarpus = find("tob_xarpus_combat")
    val xarpusEntry = find("tob_xarpus_combat_story")
    val xarpusHard = find("tob_xarpus_combat_hard")
    val verzik1 = find("verzik_phase1")
    val verzik1Entry = find("verzik_phase1_story")
    val verzik1Hard = find("verzik_phase1_hard")
    val verzik2 = find("verzik_phase2")
    val verzik2Entry = find("verzik_phase2_story")
    val verzik2Hard = find("verzik_phase2_hard")
    val verzik3 = find("verzik_phase3")
    val verzik3Entry = find("verzik_phase3_story")
    val verzik3Hard = find("verzik_phase3_hard")
    val verzikNyloMelee = find("verzik_nylocas_melee")
    val verzikNyloRanged = find("verzik_nylocas_ranged")
    val verzikNyloMagic = find("verzik_nylocas_magic")
    val verzikNyloMeleeEntry = find("verzik_nylocas_melee_story")
    val verzikNyloRangedEntry = find("verzik_nylocas_ranged_story")
    val verzikNyloMagicEntry = find("verzik_nylocas_magic_story")
    val verzikNyloMeleeHard = find("verzik_nylocas_melee_hard")
    val verzikNyloRangedHard = find("verzik_nylocas_ranged_hard")
    val verzikNyloMagicHard = find("verzik_nylocas_magic_hard")
    val verzikArmouredNylo = find("tob_verzik_phase2_armourednylocas")
    val verzikBloodNylo = find("tob_verzik_phase2_bloodnylocas")
    val verzikArmouredNyloEntry = find("tob_verzik_phase2_armourednylocas_story")
    val verzikBloodNyloEntry = find("tob_verzik_phase2_bloodnylocas_story")
    val verzikArmouredNyloHard = find("tob_verzik_phase2_armourednylocas_hard")
    val verzikBloodNyloHard = find("tob_verzik_phase2_bloodnylocas_hard")

    val all =
        listOf(
            maiden,
            maidenEntry,
            maidenHard,
            maidenHealer,
            maidenHealerEntry,
            maidenHealerHard,
            bloat,
            bloatEntry,
            bloatHard,
            nyloMelee,
            nyloRanged,
            nyloMagic,
            nyloMeleeEntry,
            nyloRangedEntry,
            nyloMagicEntry,
            nyloMeleeHard,
            nyloRangedHard,
            nyloMagicHard,
            nyloBossMelee,
            nyloBossRanged,
            nyloBossMagic,
            nyloBossMeleeEntry,
            nyloBossRangedEntry,
            nyloBossMagicEntry,
            nyloBossMeleeHard,
            nyloBossRangedHard,
            nyloBossMagicHard,
            sotetseg,
            sotetsegEntry,
            sotetsegHard,
            xarpus,
            xarpusEntry,
            xarpusHard,
            verzik1,
            verzik1Entry,
            verzik1Hard,
            verzik2,
            verzik2Entry,
            verzik2Hard,
            verzik3,
            verzik3Entry,
            verzik3Hard,
            verzikNyloMelee,
            verzikNyloRanged,
            verzikNyloMagic,
            verzikNyloMeleeEntry,
            verzikNyloRangedEntry,
            verzikNyloMagicEntry,
            verzikNyloMeleeHard,
            verzikNyloRangedHard,
            verzikNyloMagicHard,
            verzikArmouredNylo,
            verzikBloodNylo,
            verzikArmouredNyloEntry,
            verzikBloodNyloEntry,
            verzikArmouredNyloHard,
            verzikBloodNyloHard,
        )

    fun maiden(mode: TobMode): NpcType = select(mode, maidenEntry, maiden, maidenHard)

    fun maidenHealer(mode: TobMode): NpcType =
        select(mode, maidenHealerEntry, maidenHealer, maidenHealerHard)

    fun bloat(mode: TobMode): NpcType = select(mode, bloatEntry, bloat, bloatHard)

    fun sotetseg(mode: TobMode): NpcType = select(mode, sotetsegEntry, sotetseg, sotetsegHard)

    fun xarpus(mode: TobMode): NpcType = select(mode, xarpusEntry, xarpus, xarpusHard)

    fun verzik1(mode: TobMode): NpcType = select(mode, verzik1Entry, verzik1, verzik1Hard)

    fun verzik2(mode: TobMode): NpcType = select(mode, verzik2Entry, verzik2, verzik2Hard)

    fun verzik3(mode: TobMode): NpcType = select(mode, verzik3Entry, verzik3, verzik3Hard)

    fun nyloMelee(mode: TobMode): NpcType = select(mode, nyloMeleeEntry, nyloMelee, nyloMeleeHard)

    fun nyloRanged(mode: TobMode): NpcType =
        select(mode, nyloRangedEntry, nyloRanged, nyloRangedHard)

    fun nyloMagic(mode: TobMode): NpcType = select(mode, nyloMagicEntry, nyloMagic, nyloMagicHard)

    fun nyloBossMelee(mode: TobMode): NpcType =
        select(mode, nyloBossMeleeEntry, nyloBossMelee, nyloBossMeleeHard)

    fun nyloBossRanged(mode: TobMode): NpcType =
        select(mode, nyloBossRangedEntry, nyloBossRanged, nyloBossRangedHard)

    fun nyloBossMagic(mode: TobMode): NpcType =
        select(mode, nyloBossMagicEntry, nyloBossMagic, nyloBossMagicHard)

    fun verzikNyloMelee(mode: TobMode): NpcType =
        select(mode, verzikNyloMeleeEntry, verzikNyloMelee, verzikNyloMeleeHard)

    fun verzikNyloRanged(mode: TobMode): NpcType =
        select(mode, verzikNyloRangedEntry, verzikNyloRanged, verzikNyloRangedHard)

    fun verzikNyloMagic(mode: TobMode): NpcType =
        select(mode, verzikNyloMagicEntry, verzikNyloMagic, verzikNyloMagicHard)

    fun verzikArmouredNylo(mode: TobMode): NpcType =
        select(mode, verzikArmouredNyloEntry, verzikArmouredNylo, verzikArmouredNyloHard)

    fun verzikBloodNylo(mode: TobMode): NpcType =
        select(mode, verzikBloodNyloEntry, verzikBloodNylo, verzikBloodNyloHard)

    private fun select(mode: TobMode, entry: NpcType, normal: NpcType, hard: NpcType): NpcType =
        when (mode) {
            TobMode.Entry -> entry
            TobMode.Normal -> normal
            TobMode.Hard -> hard
        }
}

internal object TobSeqs : SeqReferences() {
    val maidenAttack = find("maiden_attack_blood")
    val maidenSpecial = find("maiden_attack_special")
    val bloatWalk = find("tob_bloat_walk")
    val bloatSleep = find("tob_bloat_sleep")
    val bloatStomp = find("tob_bloat_stunned_short")
    val sotetsegMelee = find("tob_sotetseg_attack_melee")
    val sotetsegRanged = find("tob_sotetseg_attack_ranged")
    val xarpusAttack = find("tob_xarpus_attack_ranged")
    val xarpusAbsorb = find("tob_xarpus_absorb")
    val verzik1Attack = find("verzik_phase1_attack_magic")
    val verzik2Attack = find("verzik_phase2_attack_magic")
    val verzik2Melee = find("verzik_phase2_attack_melee")
    val verzik3Melee = find("verzik_phase3_attack_melee")
    val verzik3Magic = find("verzik_phase3_attack_magic")
    val verzik3Ranged = find("verzik_phase3_attack_ranged")
    val verzik3Power = find("verzik_phase3_attack_powerblast")
    val verzik3Web = find("verzik_phase3_attack_webspin")
}

internal object TobObjs : ObjReferences() {
    val bandage = find("tob_bandages")
    val dawnbringer = find("verzik_special_weapon")
    val avernicHilt = find("infernal_defender_hilt")
    val rapier = find("ghrazi_rapier")
    val sanguinesti = find("sanguinesti_staff_uncharged")
    val scythe = find("scythe_of_vitur_uncharged")
    val justiciarHelm = find("justiciar_faceguard")
    val justiciarBody = find("justiciar_chestguard")
    val justiciarLegs = find("justiciar_leg_guards")
}

internal object TobVarBits : VarBitReferences() {
    val progress = find("tob_progress")
    val waveType = find("tob_client_waveprogress_type")
    val waveValue = find("tob_client_waveprogress_val")
    val waveMax = find("tob_client_waveprogress_max")
    val damageTaken = find("tob_damage_taken")
}

internal object TobVarps : VarpReferences() {
    val normalCompletions = find("total_completed_theatreofblood")
    val hardCompletions = find("total_completed_theatreofblood_hard")
    val entryCompletions = find("total_completed_theatreofblood_story")
}
