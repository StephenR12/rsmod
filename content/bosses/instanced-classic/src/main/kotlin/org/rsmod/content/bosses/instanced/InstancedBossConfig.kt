package org.rsmod.content.bosses.instanced

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object InstancedBossNpcs : NpcReferences() {
    val vorkath = find("vorkath")
    val zombifiedSpawn = find("vorkath_spawn")
    val muspah = find("muspah")
    val nightmare = find("nightmare_phase_01")
    val nightmareTotem = find("nightmare_totem_1_ready")
    val parasite = find("nightmare_parasite")
    val magicHusk = find("nightmare_husk_magic")
    val rangedHusk = find("nightmare_husk_ranged")
    val phosani = find("nightmare_challenge_phase_01")
    val sleepwalker = find("nightmare_challenge_sleepwalker")
    val phosaniParasite = find("nightmare_challenge_parasite")
}

public object InstancedBossObjs : ObjReferences() {
    val vorkathHead = find("vorkath_head")
    val skeletalVisage = find("skeletal_visage")
    val dragonboneNecklace = find("dragonbone_necklace")
    val jarOfDecay = find("jar_of_decay")
    val vorki = find("vorkathpet")
    val ancientIcon = find("ancient_icon")
    val venatorShard = find("venator_shard")
    val muphin = find("muspahpet")
    val inquisitorsMace = find("inquisitors_mace")
    val inquisitorsHelm = find("inquisitors_helm")
    val inquisitorsBody = find("inquisitors_body")
    val inquisitorsSkirt = find("inquisitors_skirt")
    val nightmareStaff = find("nightmare_staff")
    val harmonisedOrb = find("harmonised_orb")
    val volatileOrb = find("volatile_orb")
    val eldritchOrb = find("eldritch_orb")
    val jarOfDreams = find("jar_of_dreams")
    val littleNightmare = find("nightmarepet")
    val slepeyTablet = find("slepe_teleport_consumable")
    val parasiticEgg = find("nightmare_challenge_morph")
}

public enum class InstancedBossFamily {
    Vorkath,
    Muspah,
    Nightmare,
    Phosani,
}

public data class InstancedBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: InstancedBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
)

public object InstancedBosses {
    public val definitions: List<InstancedBossDefinition> =
        listOf(
            InstancedBossDefinition(
                InstancedBossNpcs.vorkath,
                CoordGrid(2272, 4055, 0),
                InstancedBossFamily.Vorkath,
                750,
                250,
                250,
                214,
                150,
                250,
                32,
            ),
            InstancedBossDefinition(
                InstancedBossNpcs.muspah,
                CoordGrid(2869, 6294, 0),
                InstancedBossFamily.Muspah,
                850,
                200,
                200,
                200,
                250,
                250,
                61,
            ),
            InstancedBossDefinition(
                InstancedBossNpcs.nightmare,
                CoordGrid(3871, 9947, 0),
                InstancedBossFamily.Nightmare,
                2_400,
                250,
                250,
                150,
                250,
                250,
                55,
            ),
            InstancedBossDefinition(
                InstancedBossNpcs.phosani,
                CoordGrid(3808, 9754, 0),
                InstancedBossFamily.Phosani,
                1_200,
                280,
                280,
                200,
                280,
                280,
                70,
            ),
        )
    public val byId: Map<Int, InstancedBossDefinition> = definitions.associateBy { it.npc.id }
}
