package org.rsmod.content.skills.farming

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences

internal object SpiritPatchLocs : org.rsmod.api.type.refs.loc.LocReferences() {
    val grownPlots = (1..5).map { find("farming_spirit_tree_patch_$it") }
}

internal object FarmingObjs : ObjReferences() {
    val hesporiSeed = find("hespori_seed")
    val animaSeeds = listOf("attas", "iasor", "kronos").associateWith { find("${it}_seed") }
    val numulite = find("fossil_numulite")
    val grapeSeed = find("grape_seed")
    val grapes = find("grapes")
    val zamorakGrapes = find("zamorak_grapes")
    val bologaBlessing = find("grape_blessing")
    val saltpetre = find("hosidius_saltpetre")
    val rake = find("rake")
    val spade = find("spade")
    val dibber = find("dibber")
    val plantCure = find("plant_cure")
    val secateurs = find("secateurs")
    val magicSecateurs = find("fairy_enchanted_secateurs")
    val compost = find("bucket_compost")
    val supercompost = find("bucket_supercompost")
    val ultracompost = find("bucket_ultracompost")
    val bucket = find("bucket_empty")
    val weeds = find("weeds")
    val rottenTomato = find("rotten_tomato")
    val volcanicAsh = find("fossil_volcanic_ash")
    val compostPotions =
        listOf(
            find("supercompost_potion_1"),
            find("supercompost_potion_2"),
            find("supercompost_potion_3"),
            find("supercompost_potion_4"),
        )
    val vial = find("vial_empty")
    val plantPot = find("plantpot_empty")
    val bronzeSpear = find("bronze_spear")
    val haySack = find("scarecrow_torso")
    val scarecrowTorso = find("scarecrow_torso_spear")
    val scarecrow = find("scarecrow_complete")
    val wateringCan0 = find("watering_can_0")
    val wateringCans =
        listOf(
            wateringCan0,
            find("watering_can_1"),
            find("watering_can_2"),
            find("watering_can_3"),
            find("watering_can_4"),
            find("watering_can_5"),
            find("watering_can_6"),
            find("watering_can_7"),
            find("watering_can_8"),
        )

    val potatoSeed = find("potato_seed")
    val onionSeed = find("onion_seed")
    val cabbageSeed = find("cabbage_seed")
    val tomatoSeed = find("tomato_seed")
    val sweetcornSeed = find("sweetcorn_seed")
    val strawberrySeed = find("strawberry_seed")
    val watermelonSeed = find("watermelon_seed")
    val snapeGrassSeed = find("snape_grass_seed")
    val potato = find("potato")
    val onion = find("onion")
    val cabbage = find("cabbage")
    val tomato = find("tomato")
    val sweetcorn = find("sweetcorn")
    val strawberry = find("strawberry")
    val watermelon = find("watermelon")
    val snapeGrass = find("snape_grass")

    val marigoldSeed = find("marigold_seed")
    val rosemarySeed = find("rosemary_seed")
    val nasturtiumSeed = find("nasturtium_seed")
    val woadSeed = find("woad_seed")
    val limpwurtSeed = find("limpwurt_seed")
    val whiteLilySeed = find("white_lily_seed")
    val marigold = find("marigold")
    val rosemary = find("rosemary")
    val nasturtium = find("nasturtium")
    val woad = find("woadleaf")
    val limpwurt = find("limpwurt_root")
    val whiteLily = find("whitelilly")

    val barleySeed = find("barley_seed")
    val hammerstoneSeed = find("hammerstone_hop_seed")
    val asgarnianSeed = find("asgarnian_hop_seed")
    val juteSeed = find("jute_seed")
    val yanillianSeed = find("yanillian_hop_seed")
    val krandorianSeed = find("krandorian_hop_seed")
    val wildbloodSeed = find("wildblood_hop_seed")
    val barley = find("barley")
    val hammerstone = find("hammerstone_hops")
    val asgarnian = find("asgarnian_hops")
    val jute = find("jute_fibre")
    val yanillian = find("yanillian_hops")
    val krandorian = find("krandorian_hops")
    val wildblood = find("wildblood_hops")

    val redberrySeed = find("redberry_bush_seed")
    val cadavaberrySeed = find("cadavaberry_bush_seed")
    val dwellberrySeed = find("dwellberry_bush_seed")
    val jangerberrySeed = find("jangerberry_bush_seed")
    val whiteberrySeed = find("whiteberry_bush_seed")
    val poisonIvySeed = find("poisonivy_bush_seed")
    val redberries = find("redberries")
    val cadavaberries = find("cadavaberries")
    val dwellberries = find("dwellberries")
    val jangerberries = find("jangerberries")
    val whiteberries = find("white_berries")
    val poisonIvyBerries = find("poisonivy_berries")

    val appleSapling = find("plantpot_apple_sapling")
    val bananaSapling = find("plantpot_banana_sapling")
    val orangeSapling = find("plantpot_orange_sapling")
    val currySapling = find("plantpot_curry_sapling")
    val pineappleSapling = find("plantpot_pineapple_sapling")
    val papayaSapling = find("plantpot_papaya_sapling")
    val palmSapling = find("plantpot_palm_sapling")
    val dragonfruitSapling = find("plantpot_dragonfruit_sapling")
    val apple = find("cooking_apple")
    val banana = find("banana")
    val orange = find("orange")
    val curryLeaf = find("curry_leaf")
    val pineapple = find("pineapple")
    val papaya = find("papaya")
    val coconut = find("coconut")
    val dragonfruit = find("dragonfruit")

    val seaweedSpore = find("seaweed_seed")
    val giantSeaweed = find("giant_seaweed")
    val mushroomSpore = find("mushroom_seed")
    val bittercapMushroom = find("bittercap_mushroom")
    val belladonnaSeed = find("belladonna_seed")
    val nightshade = find("nightshade")
    val cactusSeed = find("cactus_seed")
    val cactusSpine = find("cactus_spine")
    val potatoCactusSeed = find("potato_cactus_seed")
    val potatoCactus = find("cactus_potato")
    val calquatFruit = find("calquat_fruit")
    val monkeyNuts = find("mm_monkey_nuts")
    val monkeyBar = find("mm_monkey_bar")
    val groundTooth = find("lunar_groundtooth")

    val oakSapling = find("plantpot_oak_sapling")
    val willowSapling = find("plantpot_willow_sapling")
    val mapleSapling = find("plantpot_maple_sapling")
    val yewSapling = find("plantpot_yew_sapling")
    val magicSapling = find("plantpot_magic_tree_sapling")
    val teakSapling = find("plantpot_teak_sapling")
    val mahoganySapling = find("plantpot_mahogany_sapling")
    val calquatSapling = find("plantpot_calquat_sapling")
    val spiritSapling = find("plantpot_spirit_tree_sapling")
    val celastrusSapling = find("plantpot_celastrus_tree_sapling")
    val redwoodSapling = find("plantpot_redwood_tree_sapling")
    val crystalSapling = find("plantpot_crystal_tree_sapling")
    val logs = find("logs")
    val oakLogs = find("oak_logs")
    val willowLogs = find("willow_logs")
    val mapleLogs = find("maple_logs")
    val yewLogs = find("yew_logs")
    val magicLogs = find("magic_logs")
    val teakLogs = find("teak_logs")
    val mahoganyLogs = find("mahogany_logs")
    val redwoodLogs = find("redwood_logs")
    val celastrusBark = find("celastrus_wood")
    val oakRoots = find("oak_roots")
    val willowRoots = find("willow_roots")
    val mapleRoots = find("maple_roots")
    val yewRoots = find("yew_roots")
    val magicRoots = find("magic_roots")
    val spiritRoots = find("spirit_roots")

    val basketTomatoes = find("basket_tomato_5")
    val basketApples = find("basket_apple_5")
    val basketOranges = find("basket_orange_5")
    val basketStrawberries = find("basket_strawberry_5")
    val basketBananas = find("basket_banana_5")
    val sackPotatoes = find("sack_potato_10")
    val sackOnions = find("sack_onion_10")
    val sackCabbages = find("sack_cabbage_10")

    val guamSeed = find("guam_seed")
    val marrentillSeed = find("marrentill_seed")
    val tarrominSeed = find("tarromin_seed")
    val harralanderSeed = find("harralander_seed")
    val ranarrSeed = find("ranarr_seed")
    val toadflaxSeed = find("toadflax_seed")
    val iritSeed = find("irit_seed")
    val avantoeSeed = find("avantoe_seed")
    val kwuarmSeed = find("kwuarm_seed")
    val snapdragonSeed = find("snapdragon_seed")
    val huascaSeed = find("huasca_seed")
    val cadantineSeed = find("cadantine_seed")
    val lantadymeSeed = find("lantadyme_seed")
    val dwarfWeedSeed = find("dwarf_weed_seed")
    val torstolSeed = find("torstol_seed")
    val guam = find("guam_leaf")
    val marrentill = find("marentill")
    val tarromin = find("tarromin")
    val harralander = find("harralander")
    val ranarr = find("ranarr_weed")
    val toadflax = find("toadflax")
    val irit = find("irit_leaf")
    val avantoe = find("avantoe")
    val kwuarm = find("kwuarm")
    val snapdragon = find("snapdragon")
    val huasca = find("huasca")
    val cadantine = find("cadantine")
    val lantadyme = find("lantadyme")
    val dwarfWeed = find("dwarf_weed")
    val torstol = find("torstol")
}

internal object FarmingSeqs : SeqReferences() {
    val rake = find("farming_raking")
    val plant = find("farming_seed_dibbing")
    val cure = find("farming_plant_cure")
    val harvest = find("human_farming")
    val water = find("farming_watering")
}

internal object FarmingTimers : TimerReferences() {
    val sync = find("farming_patch_sync")
}

internal object BarbarianFarmingBits : org.rsmod.api.type.refs.varbit.VarBitReferences() {
    val planting = find("brut_farming_planting")
    val smashing = find("brut_farming_smashing")
    val automatic = find("brut_smash_pots_automatically")
    val failures = find("brut_dibber_failed_attempts")
}
