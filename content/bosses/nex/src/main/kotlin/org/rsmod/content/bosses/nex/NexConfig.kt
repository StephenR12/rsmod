package org.rsmod.content.bosses.nex

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.map.CoordGrid

public object NexLocs : LocReferences() {
    val frozenDoorOuter1 = find("nex_frozen_door_outer_1")
    val frozenDoorOuter2 = find("nex_frozen_door_outer_2")
    val frozenDoorInner1 = find("nex_frozen_door_inner_1")
    val frozenDoorInner2 = find("nex_frozen_door_inner_2")
    val outerPrisonDoor = find("nex_outer_prison_door")
    val innerPrisonDoor = find("nex_inner_prison_door")
    val gravestoneChest = find("nex_gravestone_chest")
    val gravestoneChestNoop = find("nex_gravestone_chest_noop")
    val fightBarrierOuter = find("nex_fight_barrier_outer")
    val zarosAltar = find("nex_zaros_altar")

    val infrastructure =
        listOf(
            frozenDoorOuter1,
            frozenDoorOuter2,
            frozenDoorInner1,
            frozenDoorInner2,
            outerPrisonDoor,
            innerPrisonDoor,
            gravestoneChest,
            gravestoneChestNoop,
            fightBarrierOuter,
            zarosAltar,
        )
}

public object NexNpcs : NpcReferences() {
    val boss = find("nex")
    val spawning = find("nex_spawning")
    val soulSplit = find("nex_soulsplit")
    val deflect = find("nex_deflect")
    val dying = find("nex_dying")
    val fumus = find("nex_smokemage")
    val umbra = find("nex_shadowmage")
    val cruor = find("nex_bloodmage")
    val glacies = find("nex_icemage")
    val bloodReaver = find("nex_prison_blood_reaver_boss")
    val prisonWarrior = find("nex_prison_warrior")
    val prisonRanger = find("nex_prison_ranger")
    val prisonMage = find("nex_prison_mage")
    val prisonBloodReaver = find("nex_prison_blood_reaver")

    val forms: List<NpcType> = listOf(boss, spawning, soulSplit, deflect, dying)
    val mages: List<NpcType> = listOf(fumus, umbra, cruor, glacies)
    val prisonFollowers: List<NpcType> =
        listOf(prisonWarrior, prisonRanger, prisonMage, prisonBloodReaver)
    val combatants: List<NpcType> = forms + mages + bloodReaver + prisonFollowers
}

public object NexGodWarsNpcs : NpcReferences() {
    val zilyana = find("godwars_saradomin_avatar")
    val starlight = find("godwars_saradomin_unicorn")
    val growler = find("godwars_saradomin_lion")
    val bree = find("godwars_saradomin_centaur")
    val graardor = find("godwars_bandos_avatar")
    val strongstack = find("godwars_sergeant_goblin1")
    val steelwill = find("godwars_sergeant_goblin2")
    val grimspike = find("godwars_sergeant_goblin3")
    val kril = find("godwars_zamorak_avatar")
    val tstanon = find("godwars_ancient_greater_demon")
    val balfrug = find("godwars_ancient_lesser_demon")
    val zakln = find("godwars_ancient_black_demon")
    val kree = find("godwars_armadyl_avatar")
    val skree = find("godwars_armadyl_bodyguard_skree")
    val geerin = find("godwars_armadyl_bodyguard_geerin")
    val kilisa = find("godwars_armadyl_bodyguard_kilisa")
}

public object NexSeqs : SeqReferences() {
    val attack = find("nex_attack")
    val cast = find("nex_cast_attack")
    val alternateCast = find("nex_alternate_cast_attack")
    val dash = find("nex_dash_attack")
    val smash = find("nex_smash_attack")
    val siphon = find("nex_blood_siphon")
    val turmoil = find("nex_turmoil")
    val death = find("nex_death")
    val reaverAttack = find("blood_reaver_attack")
}

public object NexSpotanims : SpotanimReferences() {
    val smoke = find("nex_smoke_attack_impact")
    val shadow = find("nex_shadow_attack_proj")
    val blood = find("nex_blood_attack_impact")
    val bloodSiphon = find("nex_blood_siphon")
    val ice = find("nex_ice_attack_impact")
    val icePrison = find("nex_ice_prison_proj")
    val finale = find("nex_finale_attack_impact")
    val soulSplit = find("nex_soulsplit_projanim")
    val turmoil = find("nex_turmoil")
    val summon = find("nex_summon")
}

public object NexObjs : ObjReferences() {
    val coins = find("coins")
    val ecumenicalKey = find("ecumenical_key")
    val frozenKey = find("nex_frozen_key")
    val armadylKeyPiece = find("nex_frozen_key_armadyl")
    val bandosKeyPiece = find("nex_frozen_key_bandos")
    val zamorakKeyPiece = find("nex_frozen_key_zamorak")
    val saradominKeyPiece = find("nex_frozen_key_saradomin")
    val bigBones = find("big_bones")
    val nihilShard = find("nihil_shard")
    val zaryteVambraces = find("zaryte_vambraces")
    val nihilHorn = find("nihil_horn")
    val torvaHelmDamaged = find("broken_torva_helm")
    val torvaPlatebodyDamaged = find("broken_torva_chest")
    val torvaPlatelegsDamaged = find("broken_torva_legs")
    val ancientHilt = find("godwars_godsword_hilt_ancient")
    val nexling = find("nexpet")
    val bloodEssence = find("blood_essence_inactive")
    val dragonBoltsUnfinished = find("dragon_bolts_unfeathered")
    val runiteOreNoted = find("cert_runite_ore")
    val coalNoted = find("cert_coal")
    val rawSharkNoted = find("cert_raw_shark")
    val rawMantaRayNoted = find("cert_raw_mantaray")
    val uncutRubyNoted = find("cert_uncut_ruby")
    val uncutDiamondNoted = find("cert_uncut_diamond")
    val superRestore4 = find("4dose2restore")
    val saradominBrew4 = find("4dosepotionofsaradomin")
}

public object NexVarps : VarpReferences() {
    val permanentAccess = find("nex_perm")
    val totalKills = find("total_nex_kills")
}

public object NexVarBits : VarBitReferences() {
    val ancientEssence = find("godwars_counter_zaros")
    val frozenDoor = find("frozen_door")
    val armadylPiece = find("frozen_door_armadyl_piece")
    val bandosPiece = find("frozen_door_bandos_piece")
    val zamorakPiece = find("frozen_door_zamorak_piece")
    val saradominPiece = find("frozen_door_saradomin_piece")
    val barrier = find("nex_barrier")
}

public enum class NexPhase(
    public val threshold: Int,
    public val mage: NpcType?,
    public val attackSpot: SpotanimType,
) {
    Smoke(2_720, NexNpcs.fumus, NexSpotanims.smoke),
    Shadow(2_040, NexNpcs.umbra, NexSpotanims.shadow),
    Blood(1_360, NexNpcs.cruor, NexSpotanims.blood),
    Ice(680, NexNpcs.glacies, NexSpotanims.ice),
    Zaros(0, null, NexSpotanims.finale),
}

public enum class NexCurse {
    SoulSplit,
    DeflectMelee,
    None,
}

public data class NexState(
    var phase: NexPhase = NexPhase.Smoke,
    var waitingForMage: NpcType? = null,
    var attacks: Int = 0,
    var specialToggle: Boolean = false,
    var siphonUntil: Int = Int.MIN_VALUE,
) {
    public val curse: NexCurse
        get() =
            if (phase != NexPhase.Zaros) {
                NexCurse.None
            } else {
                when ((attacks / 4) % 3) {
                    0 -> NexCurse.SoulSplit
                    1 -> NexCurse.DeflectMelee
                    else -> NexCurse.None
                }
            }
}

public object NexArena {
    val bossSpawn: CoordGrid = CoordGrid(2924, 5202, 0)
    val mageSpawns: Map<NpcType, CoordGrid> =
        mapOf(
            NexNpcs.fumus to CoordGrid(2915, 5214, 0),
            NexNpcs.umbra to CoordGrid(2937, 5214, 0),
            NexNpcs.cruor to CoordGrid(2937, 5190, 0),
            NexNpcs.glacies to CoordGrid(2915, 5190, 0),
        )
    const val MIN_X: Int = 2909
    const val MAX_X: Int = 2940
    const val MIN_Z: Int = 5187
    const val MAX_Z: Int = 5217

    fun contains(coords: CoordGrid): Boolean =
        coords.level == bossSpawn.level && coords.x in MIN_X..MAX_X && coords.z in MIN_Z..MAX_Z
}

public object NexAccessMap {
    val godWarsSide: CoordGrid = CoordGrid(2889, 5276, 0)
    val ancientPrison: CoordGrid = CoordGrid(2855, 5227, 0)
    val prisonDoorSide: CoordGrid = CoordGrid(2889, 5203, 0)
    val safeRoom: CoordGrid = CoordGrid(2904, 5203, 0)
    val chamberEntry: CoordGrid = CoordGrid(2912, 5202, 0)
    val prisonFollowers: List<Pair<NpcType, CoordGrid>> =
        listOf(
            NexNpcs.prisonWarrior to CoordGrid(2864, 5210, 0),
            NexNpcs.prisonWarrior to CoordGrid(2880, 5213, 0),
            NexNpcs.prisonRanger to CoordGrid(2869, 5202, 0),
            NexNpcs.prisonRanger to CoordGrid(2887, 5202, 0),
            NexNpcs.prisonMage to CoordGrid(2876, 5221, 0),
            NexNpcs.prisonBloodReaver to CoordGrid(2877, 5208, 0),
        )
}
