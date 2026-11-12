package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.game.type.loc.LocType

public typealias prayer_altar_locs = PrayerAltarLocs

public object PrayerAltarLocs : LocReferences() {
    val chaos = find("chaosaltar")

    private val saradomin =
        listOf(
            find("poh_altar_saradomin_1"),
            find("poh_altar_saradomin_2"),
            find("poh_altar_saradomin_3"),
            find("poh_altar_saradomin_4"),
            find("poh_altar_saradomin_5"),
            find("poh_altar_saradomin_6"),
            find("poh_altar_saradomin_7"),
        )
    private val zamorak =
        listOf(
            find("poh_altar_zamorak_1"),
            find("poh_altar_zamorak_2"),
            find("poh_altar_zamorak_3"),
            find("poh_altar_zamorak_4"),
            find("poh_altar_zamorak_5"),
            find("poh_altar_zamorak_6"),
            find("poh_altar_zamorak_7"),
        )
    private val guthix =
        listOf(
            find("poh_altar_guthix_1"),
            find("poh_altar_guthix_2"),
            find("poh_altar_guthix_3"),
            find("poh_altar_guthix_4"),
            find("poh_altar_guthix_5"),
            find("poh_altar_guthix_6"),
            find("poh_altar_guthix_7"),
        )
    private val gnomeChild =
        listOf(
            find("poh_altar_gnomechild_1"),
            find("poh_altar_gnomechild_2"),
            find("poh_altar_gnomechild_3"),
            find("poh_altar_gnomechild_4"),
            find("poh_altar_gnomechild_5"),
            find("poh_altar_gnomechild_6"),
            find("poh_altar_gnomechild_7"),
        )

    val chapelByTier: List<List<LocType>> =
        List(7) { tier -> listOf(saradomin[tier], zamorak[tier], guthix[tier], gnomeChild[tier]) }
    val chapel: List<LocType> = chapelByTier.flatten()

    val litOakBurner = find("poh_torch_5_lit")
    val litMahoganyBurner = find("poh_torch_6_lit")
    val litMarbleBurner = find("poh_torch_7_lit")
    val litBurners = listOf(litOakBurner, litMahoganyBurner, litMarbleBurner)

    /**
     * Cache-233 altar locations whose Pray/Pray-at operation is an ordinary Prayer recharge. Access
     * requirements are enforced by the areas and quests that contain the altar.
     */
    val standardRestoration: List<LocType> =
        chapel +
            listOf(
                find("altar_talkasti01"),
                find("tox_xerician_altar"),
                find("altar"),
                find("contact_lesser_god_temple_altar_fixed"),
                find("regicide_voyage_temple_altar"),
                find("hosidius_altar"),
                find("clan_medieval_altar"),
                find("contact_high_priest_temple_altar"),
                find("castlewars_altar_zamorak"),
                find("altar_twilight_default01"),
                find("fai_varrock_church_altar"),
                find("slug2_altar_saradomin"),
                find("mm_gorillastatue1"),
                find("twilight_shrine"),
                find("hosdun_altar_normal"),
                find("castlewars_altar_saradomin"),
                find("bcs_tomb_altar_op"),
                find("wildy_hub_altar"),
                find("arceuus_altar"),
                find("wcguild_shrine"),
                find("elf_village_altar"),
                find("thrantaxaltar"),
                find("ranul_shrine"),
                find("guthix_altar"),
                find("slp_church_altar"),
                find("shayzien_church_altar"),
                chaos,
                find("gh_altar"),
                find("altar_twilight_default02"),
                find("altarv2"),
                find("lovakengj_altar"),
                find("ds2_guild_altar"),
                find("devious_altar_scorched"),
                find("devious_altar_pouch"),
                find("tbwt_tribal_statue"),
            )

    /** The two cache-233 altars documented to restore Prayer to two above the base level. */
    val boostedRestoration = listOf(find("monks_altar"), find("druidic_spirit_grotto_naturealtar"))

    val godWarsZamorak = find("godwars_dungeon_zamorak_altar01")
    val godWarsSaradomin = find("godwars_dungeon_saradomin_altar01")
    val godWarsArmadyl = find("godwars_dungeon_armadyl_altar01")
    val godWarsBandos = find("godwars_dungeon_bandos_altar01")
    val godWarsZaros = find("nex_zaros_altar")

    /** God Wars restoration is deliberately reserved for its equipment and cooldown rules. */
    val godWarsByGod: List<Pair<LocType, PrayerGodWarsGod>> =
        listOf(
            godWarsZamorak to PrayerGodWarsGod.Zamorak,
            godWarsSaradomin to PrayerGodWarsGod.Saradomin,
            godWarsArmadyl to PrayerGodWarsGod.Armadyl,
            godWarsBandos to PrayerGodWarsGod.Bandos,
            godWarsZaros to PrayerGodWarsGod.Zaros,
        )
    val godWarsRestoration: List<LocType> = godWarsByGod.map { (altar) -> altar }

    val cursedAltar = find("hosdun_altar_cursed")
    val darkmeyerStatue = find("darkm_altar")

    /** Pray operations that restore or drain Prayer while also dealing direct Hitpoints damage. */
    val hazardousRestoration = listOf(cursedAltar, darkmeyerStatue)

    val elidinisStatuette = find("elid_statuette_placed")

    /**
     * Full-player-state restoration whose quest and secondary effects are not generic altar use.
     */
    val utilityRestoration = listOf(elidinisStatuette)

    val astralAltar = find("astral_altar")
    val ancientPyramidAltar = find("dt_zaros_altar")

    /** Pray operations that switch between a themed spellbook and the standard spellbook. */
    val spellbookAltars = listOf(astralAltar, ancientPyramidAltar)

    val mageArenaSaradominStatue = find("magearena_statue_saradomin")
    val mageArenaGuthixStatue = find("magearena_statue_guthix")
    val mageArenaZamorakStatue = find("magearena_statue_zamorak")

    /** Mage Arena I statues whose Pray-at operation creates the corresponding god cape. */
    val mageArenaStatues =
        listOf(mageArenaSaradominStatue, mageArenaGuthixStatue, mageArenaZamorakStatue)

    val trappedChaosAltar = find("trappedchaosaltar")

    /** Pray-at trap that drops the player into the Yanille Agility Dungeon spider pit. */
    val transportAltars = listOf(trappedChaosAltar)

    val brokenSunAltar = find("hosdun_sun_altar_broken")

    /** Production altars that animate and respond but deliberately do not restore Prayer. */
    val nonRestoringAltars = listOf(brokenSunAltar)

    val unrestrictedWorldSpellbookAltar = find("clanwars_tournament_altar_spellbook")

    /** Cached Pray locations proven to be temporary or otherwise outside production-world scope. */
    val nonProductionAltars = listOf(unrestrictedWorldSpellbookAltar)

    /** The three interactive pieces of the Land of the Goblins temple's composite altar. */
    val goblinTempleAltars =
        listOf(
            find("lotg_temple_altar"),
            find("lotg_temple_altar_mid5"),
            find("lotg_temple_altar_front"),
        )

    val restorationCatalogue: List<LocType> =
        standardRestoration +
            boostedRestoration +
            godWarsRestoration +
            hazardousRestoration +
            utilityRestoration +
            spellbookAltars +
            mageArenaStatues +
            transportAltars +
            nonRestoringAltars +
            nonProductionAltars +
            goblinTempleAltars
}
