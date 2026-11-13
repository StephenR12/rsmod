package org.rsmod.content.bosses.modern

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object ModernBossNpcs : NpcReferences() {
    val amoxliatl = find("amoxliatl")
    val amoxliatlIce = find("amoxliatl_ice_block")
    val branda = find("rt_fire_queen")
    val eldric = find("rt_ice_king")
    val fireElemental = find("rt_summon_elemental_fire")
    val iceElemental = find("rt_summon_elemental_ice")
    val yama = find("yama")
    val yamaVoidflare = find("yama_voidflare")
    val yamaJudge = find("yama_judge_of_yama")
    val yamaMeteor = find("yama_meteor_npc")
    val doom = find("dom_boss")
    val doomShielded = find("dom_boss_shielded")
    val doomBurrowed = find("dom_boss_burrowed")
    val doomEnergyRanged = find("dom_demonic_energy_range")
    val doomEnergyMagic = find("dom_demonic_energy_mage")
    val doomEnergyMelee = find("dom_demonic_energy_melee")
    val doomEnergyGiantRanged = find("dom_demonic_energy_giant_range")
    val doomEnergyGiantMagic = find("dom_demonic_energy_giant_mage")
}

public object ModernBossObjs : ObjReferences() {
    val glacialTemotli = find("glacial_temotli")
    val pendantOfAtes = find("pendant_of_ates")
    val frozenTear = find("frozen_tear")
    val amoxliatlPet = find("amoxliatlpet")

    val deadeyeScroll = find("deadeye_prayer_scroll")
    val mysticVigourScroll = find("mystic_vigour_prayer_scroll")
    val twinflamePieceOne = find("twinflame_piece_1")
    val twinflamePieceTwo = find("twinflame_piece_2")
    val giantsoulAmulet = find("giantsoul_amulet_uncharged")
    val brandaPet = find("rtbrandapet")
    val eldricPet = find("rteldricpet")

    val soulflameHorn = find("soulflame_horn")
    val oathplateHelm = find("oathplate_helm")
    val oathplateChest = find("oathplate_chest")
    val oathplateLegs = find("oathplate_legs")
    val oathplateShards = find("oathplate_shards")
    val forgottenLockbox = find("forgotten_lockbox")
    val diabolicWorms = find("diabolic_worms")
    val yamaDossier = find("yama_dossier")
    val yamaPet = find("yamapet")

    val avernicTreads = find("avernic_treads")
    val eyeOfAyak = find("eye_of_ayak_uncharged")
    val mokhaiotlCloth = find("mokhaiotl_cloth")
    val demonTear = find("demon_tear")
    val doomPet = find("dompet")
}

public enum class ModernBossFamily {
    Amoxliatl,
    Branda,
    Eldric,
    Yama,
    Doom,
}

public data class ModernBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: ModernBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
)

public object ModernBosses {
    public val definitions: List<ModernBossDefinition> =
        listOf(
            ModernBossDefinition(
                ModernBossNpcs.amoxliatl,
                CoordGrid(1391, 10278, 0),
                ModernBossFamily.Amoxliatl,
                415,
                180,
                180,
                100,
                120,
                1,
                30,
            ),
            ModernBossDefinition(
                ModernBossNpcs.branda,
                CoordGrid(1743, 11552, 0),
                ModernBossFamily.Branda,
                500,
                180,
                180,
                150,
                200,
                1,
                36,
            ),
            ModernBossDefinition(
                ModernBossNpcs.eldric,
                CoordGrid(1761, 11552, 0),
                ModernBossFamily.Eldric,
                500,
                180,
                180,
                150,
                1,
                200,
                36,
            ),
            ModernBossDefinition(
                ModernBossNpcs.yama,
                CoordGrid(1439, 10074, 0),
                ModernBossFamily.Yama,
                2_500,
                300,
                300,
                200,
                300,
                250,
                75,
            ),
            ModernBossDefinition(
                ModernBossNpcs.doom,
                CoordGrid(1738, 9676, 0),
                ModernBossFamily.Doom,
                525,
                275,
                275,
                90,
                275,
                275,
                47,
            ),
        )

    public val byId: Map<Int, ModernBossDefinition> = definitions.associateBy { it.npc.id }
}
