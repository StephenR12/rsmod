package org.rsmod.content.bosses.forgottenfour

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object ForgottenFourNpcs : NpcReferences() {
    val leviathan = find("leviathan")
    val leviathanTailOne = find("leviathan_tail_ring_1")
    val leviathanTailTwo = find("leviathan_tail_ring_2")
    val leviathanTailThree = find("leviathan_tail_ring_3")
    val leviathanPathfinder = find("leviathan_buff_npc")
    val whisperer = find("whisperer")
    val lostSoul = find("whisperer_soul_vulnerable")
    val vardorvis = find("vardorvis")
    val vardorvisHead = find("vardorvis_head_tentacle")
    val vardorvisAxe = find("vardorvis_axe")
    val duke = find("duke_sucellus_awake")
    val dukeVent = find("duke_vent_spotanim_npc")
}

public object ForgottenFourSeqs : SeqReferences() {
    val leviathanAttack = find("npc_leviathan_01_projectile_single_01")
    val leviathanStun = find("npc_leviathan_01_stun_01")
    val leviathanLightning = find("npc_leviathan_01_360_start")
    val leviathanSmoke = find("npc_leviathan_01_spit_start")
    val whispererRanged = find("npc_whisperer_01_attack_ranged_01")
    val whispererMagic = find("npc_whisperer_01_attack_magic_01")
    val whispererScreech = find("npc_whisperer_01_attack_screech_01_start")
    val whispererSouls = find("npc_whisperer_01_teleport_summon_souls_01_start")
    val vardorvisMelee = find("npc_vardorvis_01_melee_01")
    val vardorvisDash = find("npc_vardorvis_01_dash_01")
    val vardorvisStrangle = find("npc_vardorvis_01_entangle_start")
    val vardorvisHead = find("npc_vardorvis_01_head_01_scream_01")
    val dukeMelee = find("npc_duke_sucellus01_attack_melee_01")
    val dukeMagic = find("npc_duke_sucellus01_magic_attack_01")
    val dukeGaze = find("npc_duke_sucellus01_sight_attack_01")
}

public object ForgottenFourSpotanims : SpotanimReferences() {
    val leviathanRanged = find("vfx_leviathan_01_projectile_spotanim_ranged_01")
    val leviathanMelee = find("vfx_leviathan_01_projectile_spotanim_melee_01")
    val leviathanMagic = find("vfx_leviathan_01_projectile_spotanim_magic_01")
    val leviathanExplosion = find("spotanim_leviathan_explode")
    val whispererTentacle = find("spotanim_whisperer_tentacle_01_attack_02")
    val whispererScreech = find("vfx_whisperer_01_attack_screech_01_end")
    val vardorvisSpike = find("vardorvis_spike_impale")
    val vardorvisSpores = find("vfx_player_vardorvis_spores_spawn")
    val dukeMelee = find("spotanim_npc_duke_sucellus01_attack_melee_main_01")
    val dukeMagic = find("vfx_duke_sucellus_attack_magic_projectile_impact_01")
}

public object ForgottenFourObjs : ObjReferences() {
    val virtusMask = find("virtus_mask")
    val virtusTop = find("virtus_top")
    val virtusLegs = find("virtus_legs")
    val chromiumIngot = find("chromium_ingot")
    val venatorVestige = find("venator_vestige")
    val magusVestige = find("magus_vestige")
    val ultorVestige = find("ultor_vestige")
    val bellatorVestige = find("bellator_vestige")
    val smokeQuartz = find("smoke_quartz")
    val shadowQuartz = find("shadow_quartz")
    val bloodQuartz = find("blood_quartz")
    val iceQuartz = find("ice_quartz")
    val leviathansLure = find("soulreaper_axe_lure")
    val sirensStaff = find("soulreaper_axe_staff")
    val executionersAxeHead = find("soulreaper_axe_head")
    val eyeOfTheDuke = find("soulreaper_axe_eye")
    val lilviathan = find("leviathanpet")
    val wisp = find("whispererpet")
    val butch = find("vardorvispet")
    val baron = find("dukesucelluspet")
    val awakenersOrb = find("dt2_awakeners_orb")
    val leviathanTablet = find("leviathan_tablet")
    val whispererTablet = find("whisperer_tablet")
    val vardorvisTablet = find("vardorvis_tablet")
    val dukeTablet = find("duke_sucellus_tablet")
}

public enum class ForgottenBossFamily {
    Leviathan,
    Whisperer,
    Vardorvis,
    Duke,
}

public data class ForgottenBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: ForgottenBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
    val uniqueTableDenominator: Int,
)

public object ForgottenFour {
    public val definitions: List<ForgottenBossDefinition> =
        listOf(
            ForgottenBossDefinition(
                ForgottenFourNpcs.leviathan,
                CoordGrid(2066, 6434, 0),
                ForgottenBossFamily.Leviathan,
                900,
                1,
                1,
                180,
                250,
                250,
                36,
                96,
            ),
            ForgottenBossDefinition(
                ForgottenFourNpcs.whisperer,
                CoordGrid(2634, 6356, 0),
                ForgottenBossFamily.Whisperer,
                900,
                1,
                1,
                200,
                250,
                250,
                48,
                64,
            ),
            ForgottenBossDefinition(
                ForgottenFourNpcs.vardorvis,
                CoordGrid(1127, 3412, 0),
                ForgottenBossFamily.Vardorvis,
                700,
                250,
                250,
                215,
                1,
                1,
                38,
                136,
            ),
            ForgottenBossDefinition(
                ForgottenFourNpcs.duke,
                CoordGrid(3038, 6435, 0),
                ForgottenBossFamily.Duke,
                440,
                250,
                250,
                275,
                250,
                1,
                36,
                90,
            ),
        )

    public val byId: Map<Int, ForgottenBossDefinition> = definitions.associateBy { it.npc.id }
}
