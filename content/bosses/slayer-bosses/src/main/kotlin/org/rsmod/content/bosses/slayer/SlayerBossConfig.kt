package org.rsmod.content.bosses.slayer

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.map.CoordGrid

public object SlayerBossNpcs : NpcReferences() {
    val cerberus = find("cerberus_attacking")
    val rangedSoul = find("cerberus_spectre_ranged")
    val magicSoul = find("cerberus_spectre_magic")
    val meleeSoul = find("cerberus_spectre_melee")
    val kraken = find("slayer_kraken_boss")
    val krakenTentacle = find("slayer_kraken_boss_tentacle")
    val thermy = find("smoke_devil_boss")
    val dusk = find("gargboss_dusk_phase1_defensive")
    val dawn = find("gargboss_dawn_phase1")
    val sire = find("abyssalsire_sire_stasis_awake")
    val sireLung = find("abyssalsire_lung")
    val sireSpawn = find("abyssalsire_spawn")
    val sireScion = find("abyssalsire_scion")
    val araxxor = find("araxxor")
    val mirrorback = find("araxxor_minion_mirrorback")
    val ruptura = find("araxxor_minion_explode")
    val acidicAraxyte = find("araxxor_minion_venom")
    val hydra = find("hydraboss")
}

public object SlayerBossObjs : ObjReferences() {
    val primordialCrystal = find("primordial_crystal")
    val pegasianCrystal = find("pegasian_crystal")
    val eternalCrystal = find("eternal_crystal")
    val smoulderingStone = find("smouldering_stone")
    val jarOfSouls = find("jar_of_souls")
    val hellpuppy = find("hell_pet")
    val trident = find("tots")
    val krakenTentacle = find("kraken_tentacle")
    val jarOfDirt = find("jar_of_dirt")
    val krakenPet = find("krakenpet")
    val occultNecklace = find("occult_necklace")
    val smokeBattlestaff = find("smoke_battlestaff")
    val jarOfSmoke = find("jar_of_smoke")
    val smokeDevilPet = find("smokepet")
    val tourmalineCore = find("tourmaline_core")
    val graniteGloves = find("granite_gloves")
    val graniteRing = find("granite_ring")
    val graniteHammer = find("granite_hammer")
    val jarOfStone = find("jar_of_stone")
    val noon = find("dawnpet")
    val unsired = find("abyssalsire_unsired")
    val abyssalDagger = find("abyssal_dagger")
    val abyssalBludgeon = find("abyssal_bludgeon")
    val jarOfMiasma = find("jar_of_miasma")
    val abyssalOrphan = find("abyssalsire_pet")
    val araxyteFang = find("araxyte_fang")
    val noxiousPoint = find("noxious_halberd_part_1")
    val noxiousBlade = find("noxious_halberd_part_2")
    val noxiousPommel = find("noxious_halberd_part_3")
    val jarOfVenom = find("jar_of_venom")
    val nid = find("araxxorpet")
    val hydraClaw = find("hydra_claw")
    val hydraLeather = find("hydra_leather")
    val hydraTail = find("hydra_tail")
    val hydraEye = find("hydra_eye")
    val hydraHeart = find("hydra_heart")
    val hydraFang = find("hydra_fang")
    val jarOfChemicals = find("jar_of_chemicals")
    val hydraPet = find("hydrapet")
}

public enum class SlayerBossFamily {
    Cerberus,
    Kraken,
    Thermy,
    Dusk,
    Dawn,
    Sire,
    Araxxor,
    Hydra,
}

public data class SlayerBossDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val family: SlayerBossFamily,
    val hitpoints: Int,
    val attack: Int,
    val strength: Int,
    val defence: Int,
    val magic: Int,
    val ranged: Int,
    val maxHit: Int,
)

public object SlayerBosses {
    public val definitions: List<SlayerBossDefinition> =
        listOf(
            SlayerBossDefinition(
                SlayerBossNpcs.cerberus,
                CoordGrid(1238, 1250, 0),
                SlayerBossFamily.Cerberus,
                600,
                220,
                220,
                100,
                220,
                220,
                23,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.kraken,
                CoordGrid(2280, 10034, 0),
                SlayerBossFamily.Kraken,
                255,
                1,
                1,
                130,
                150,
                1,
                28,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.thermy,
                CoordGrid(2404, 9419, 0),
                SlayerBossFamily.Thermy,
                240,
                200,
                200,
                360,
                200,
                1,
                16,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.dusk,
                CoordGrid(1695, 4575, 1),
                SlayerBossFamily.Dusk,
                450,
                220,
                220,
                200,
                1,
                1,
                38,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.dawn,
                CoordGrid(1701, 4575, 1),
                SlayerBossFamily.Dawn,
                450,
                1,
                1,
                150,
                220,
                220,
                20,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.sire,
                CoordGrid(3036, 4782, 0),
                SlayerBossFamily.Sire,
                400,
                180,
                180,
                250,
                200,
                1,
                36,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.araxxor,
                CoordGrid(3654, 3418, 0),
                SlayerBossFamily.Araxxor,
                1_000,
                260,
                260,
                180,
                220,
                220,
                55,
            ),
            SlayerBossDefinition(
                SlayerBossNpcs.hydra,
                CoordGrid(1364, 10265, 0),
                SlayerBossFamily.Hydra,
                1_100,
                1,
                1,
                100,
                260,
                260,
                55,
            ),
        )

    public val byId: Map<Int, SlayerBossDefinition> = definitions.associateBy { it.npc.id }
}


internal object SlayerBossAttackRange : org.rsmod.api.type.editors.npc.NpcEditor() {
    init {
        with(SlayerBossNpcs) {
            for (type in listOf(rangedSoul, magicSoul, kraken, krakenTentacle, dawn, sire, acidicAraxyte, araxxor, hydra))
                edit(type) { attackRange = 10 }
            edit(cerberus) { attackRange = 14 }
            edit(thermy) { attackRange = 2 }
        }
    }
}
