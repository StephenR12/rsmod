package org.rsmod.content.skills.cooking

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.timer.TimerReferences

public typealias cooking_locs = CookingLocs

public object CookingLocs : LocReferences() {
    public val fire = find("fire")
    public val campfires = listOf("forestry_fire", "forestry_fire_red", "forestry_fire_green", "forestry_fire_blue", "forestry_fire_white", "forestry_fire_purple").map { find(it) }
    public val permanentCookFire = find("fire_cook")
    public val ogreSpitRoast = find("multi_chompybird_spitroast_entity")
    public val ogreSpitRoastEmpty = find("chompybird_spitroast_empty")
    public val ogreSpitRoastRawChompy = find("chompybird_spitroast")
    public val ogreSpitRoastCookedChompy = find("chompybird_spitroast_cooked")
    public val ogreSpitRoastRuinedChompy = find("chompybird_spitroast_ruined")
    public val ogreSpitRoastRawChicken = find("chompybird_spitroast_chicken")
    public val ogreSpitRoastCookedChicken = find("chompybird_spitroast_chicken_c")
    public val ogreSpitRoastRawJubbly = find("100_jubbly_spitroast")
    public val ogreSpitRoastCookedJubbly = find("100_jubbly_spitroast_cooked")
    public val ogreSpitRoastBurntJubbly = find("100_jubbly_spitroast_ruined")
    public val range = find("range")
    public val lumbridgeRange = find("cooksquestrange")
    public val hosidiusRange = find("hos_cooking_range")
    public val hosidiusRange2 = find("hos_cooking_range_02")
    public val dairyChurn = find("dairy_churn")
    public val dairyChurnMetal = find("dairy_churn_metal")
    public val sinclairDairyChurn = find("kr_sin_dairy_churn")
    public val prifddinasDairyChurn = find("kr_sin_dairy_churn_north")

    public val dairyChurns =
        listOf(dairyChurn, dairyChurnMetal, sinclairDairyChurn, prifddinasDairyChurn)

    /** Every ordinary cache-233 location whose first operation is `Cook`. */
    public val standardRangesOp1 =
        listOf(
            find("fai_varrock_range"),
            find("fai_varrock_range_with_pipe"),
            find("fai_varrock_posh_stove"),
            find("elf_village_range"),
            find("rimmington_poor_range"),
            find("newbierange"),
            find("elid_clay_oven"),
            find("fairy_range"),
            find("100_dave_stove"),
            find("swan_stove"),
            find("ahoy_range"),
            find("lunar_moonclan_cooker_small"),
            find("lunar_pirate_cooking_range"),
            find("aluft_gnome_cooker"),
            find("iznot_clay_range"),
            find("sorceress_range"),
            find("dorgesh_cooking_range1"),
            find("dorgesh_cooking_range2"),
            find("kr_sin_range"),
            range,
            find("piscarilius_range"),
            find("fossil_range_built"),
            find("ds2_guild_cooking_range"),
            find("slp_poor_stove"),
            find("slp_stove"),
            find("gauntlet_range_hm"),
            find("gauntlet_range"),
            find("tut2_range"),
            find("darkm_poor_range"),
            find("darkm_middle_range"),
            find("darkm_rich_range"),
            find("con_contract_falador_south_range_fixed"),
            find("range_lassar01_default01_shadow"),
            find("range_lassar01_default01"),
            find("mah3_base_range"),
            find("pmoon_range"),
            find("cam_torum_stove"),
            find("fortis_stove"),
            find("luc2_mov_kitchen_range"),
            find("furnace_clay01_twilight"),
            find("stove_clay01_talkasti01"),
        )

    /** Player-owned-house fancy ranges expose `Cook` as their second operation. */
    public val standardRangesOp2 =
        listOf(find("poh_stove_7"), find("poh_stove_7_kettle"), find("poh_stove_7_pot"))

    /** Early house stoves expose Remove only; they accept food-on-location instead of Cook. */
    public val houseFireStoves = listOf(find("poh_stove_1"), find("poh_stove_2"), find("poh_stove_2_kettle"), find("poh_stove_3"), find("poh_stove_3_kettle"))
    public val houseRangeStoves = listOf(find("poh_stove_4"), find("poh_stove_4_pot"), find("poh_stove_4_kettle"), find("poh_stove_5"), find("poh_stove_5_pot"), find("poh_stove_5_kettle"), find("poh_stove_6"), find("poh_stove_6_kettle"), find("poh_stove_6_pot"))

    public val allCookOperationSources =
        standardRangesOp1 +
            standardRangesOp2 +
            listOf(lumbridgeRange, hosidiusRange, hosidiusRange2, permanentCookFire)

    public val allFoodUseSources = allCookOperationSources + fire + campfires + houseFireStoves + houseRangeStoves
}

public typealias cooking_queues = CookingQueues

public object CookingQueues : QueueReferences() {
    public val standardFood = find("cooking_standard_food")
    public val chompyFire = find("cooking_chompy_fire")
    public val ogreSpitRoast = find("cooking_ogre_spit_roast")
    public val foodPreparation = find("cooking_food_preparation")
    public val dairyChurning = find("cooking_dairy_churning")
    public val wineMaking = find("cooking_wine_making")
    public val karambwan = find("cooking_karambwan")
    public val sinew = find("cooking_sinew")
    public val ironSpit = find("cooking_iron_spit")
}

public typealias cooking_timers = CookingTimers

public object CookingTimers : TimerReferences() {
    public val ogreSpitRoastState = find("cooking_ogre_spit_roast_state")
    public val wineFermentation = find("cooking_wine_fermentation")
}
