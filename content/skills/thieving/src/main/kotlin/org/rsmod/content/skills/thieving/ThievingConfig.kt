package org.rsmod.content.skills.thieving

import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType

public object ThievingNpcs : NpcReferences() {
    val man = find("man")
    val man2 = find("man2")
    val man3 = find("man3")
    val man4 = find("man4")
    val manMusaPoint = find("man4_for_musa_point")
    val alKharidMan = find("al_kharid_man")
    val faladorMan2 = find("falador_man2")
    val faladorMan3 = find("falador_man3")
    val karamjaMan = find("karamja_man")
    val varrockMan = find("varrock_man1")
    val indoorMan = find("man_indoor")
    val deathIndoorMan = find("death_man_indoors1")
    val zeahMan = find("zeah_man")
    val zeahMan2 = find("zeah_man2")
    val zeahMan3 = find("zeah_man3")
    val prifddinasMan = find("sote_citizen_male_2")
    val shayzienMan1 = find("shayzien_man_1")
    val shayzienMan2 = find("shayzien_man_2")

    val woman = find("woman")
    val woman2 = find("woman2")
    val woman3 = find("woman3")
    val varrockWoman = find("varrock_woman1")
    val zeahWoman = find("zeah_woman")
    val zeahWoman2 = find("zeah_woman2")
    val zeahWoman3 = find("zeah_woman3")
    val zeahWomanOutside = find("zeah_woman_outside")
    val shayzienWoman1 = find("shayzien_woman_1")
    val shayzienWoman2 = find("shayzien_woman_2")

    val hamMembers: List<NpcType> =
        listOf("favour_male_ham_civilian", "favour_female_ham_civilian").map(::find)

    val hamStoreroomGuards: List<NpcType> =
        listOf(
                "dttd_ham_guard_1_postquest",
                "dttd_ham_guard_2_postquest",
                "dttd_ham_guard_3_postquest",
                "dttd_ham_guard_4_postquest",
                "dttd_ham_guard_5_postquest",
            )
            .map(::find)
    val hamStoreroomQuestPatrolGuards: List<NpcType> =
        listOf(
                "dttd_ham_guard_1",
                "dttd_ham_guard_2",
                "dttd_ham_guard_3",
                "dttd_ham_guard_4",
                "dttd_ham_guard_5",
            )
            .map(::find)
    val hamStoreroomQuestArrestGuard = find("dttd_ham_guard_extra")

    val farmer1 = find("farmer1")
    val farmer2 = find("farmer2")
    val farmer3 = find("farmer3")
    val farmer1F = find("farmer1_f")
    val farmer2F = find("farmer2_f")
    val farmer3F = find("farmer3_f")
    val farmer4 = find("farmer4")
    val varlamoreFarmerM1 = find("varlamore_farmer_m_1")
    val varlamoreFarmerM2 = find("varlamore_farmer_m_2")
    val varlamoreFarmerM3 = find("varlamore_farmer_m_3")
    val varlamoreFarmerM4 = find("varlamore_farmer_m_4")
    val varlamoreFarmerF1 = find("varlamore_farmer_f_1")
    val varlamoreFarmerF2 = find("varlamore_farmer_f_2")
    val varlamoreFarmerF3 = find("varlamore_farmer_f_3")
    val varlamoreFarmerF4 = find("varlamore_farmer_f_4")
    val kastoriFarmerM1 = find("kastori_farmer_m_1")
    val kastoriFarmerM2 = find("kastori_farmer_m_2")
    val kastoriFarmerF1 = find("kastori_farmer_f_1")
    val kastoriFarmerF2 = find("kastori_farmer_f_2")
    val talTeklanFarmer = find("tal_teklan_farmer")

    val warrior = find("warrior_woman")
    val warrior2 = find("warrior_woman_variant01")
    val warrior3 = find("warrior_woman_variant02")
    val warrior4 = find("warrior_man")
    val warrior5 = find("warrior_man_variant01")
    val warrior6 = find("warrior_man_variant02")
    val alKharidWarrior = find("al_kharid_warrior")

    val rogue = find("rogue")
    val wildernessRogue = find("wilderness_rogue")

    val knightOfArdougne = find("knight_of_ardougne")
    val knightOfArdougne2 = find("knight_of_ardougne2")
    val knightOfArdougneWest = find("knight_of_ardougne_west_vis")
    val knightOfArdougneFWest = find("knight_of_ardougne_f_west_vis")
    val knightOfArdougneF = find("knight_of_ardougne_f")

    val varlamoreKnightM1 = find("varlamore_knight_m_1")
    val varlamoreKnightM2 = find("varlamore_knight_m_2")
    val varlamoreKnightM3 = find("varlamore_knight_m_3")
    val varlamoreKnightF1 = find("varlamore_knight_f_1")
    val varlamoreKnightF2 = find("varlamore_knight_f_2")
    val varlamoreKnightF3 = find("varlamore_knight_f_3")

    val watchman = find("yanille_watchman")

    val paladin = find("paladin")
    val paladin2 = find("paladin2")
    val paladinWest = find("paladin_west_vis")
    val paladinWestF = find("paladin_west_f_vis")
    val paladinVariant1 = find("paladin_variant01")
    val paladinVariant2 = find("paladin_variant02")
    val paladinF = find("paladin_f")
    val paladinFVariant1 = find("paladin_f_variant01")

    val desertBandit = find("fourdiamonds_sword_bandit_1")
    val desertBanditFree = find("fourdiamonds_sword_bandit_free")

    val menaphiteThugBeforeQuest = find("feud_egyptian_doorman_1")
    val menaphiteThugAfterQuest = find("feud_egyptian_doorman_2")
    val menaphiteThugs: List<NpcType> = listOf(menaphiteThugBeforeQuest, menaphiteThugAfterQuest)

    val pollnivneachBandits: List<NpcType> =
        listOf("feud_arabian_guard1_1", "feud_arabian_guard1_2").map(::find)
    val beardedPollnivneachBandits: List<NpcType> =
        listOf("feud_arabian_guard2_1", "feud_arabian_guard2_2").map(::find)

    val wealthyCitizens: List<NpcType> =
        listOf(
                "varlamore_wealthy_citizen_a",
                "varlamore_wealthy_citizen_b",
                "varlamore_wealthy_citizen_c",
                "varlamore_wealthy_citizen_d",
            )
            .map(::find)

    val hero = find("hero")
    val heroVariant1 = find("hero_variant01")
    val heroF = find("hero_f")

    val battlefieldGnome = find("battlefield_gnome_troop")
    val battlefieldDarkGnomeArcher = find("battlefield_darkskinned_gnome_archer")
    val battlefieldGnomeArcher = find("battlefield_gnome_archer")
    val gnome = find("gnome")
    val brownClothedGnome = find("browclothedgnome")
    val darkSkinnedGnome = find("darkskinned_gnome")

    val goreu = find("mourning_town_elf_1")
    val arvel = find("mourning_town_elf_3")
    val kelyn = find("sote_lady_ithell_normal")
    val kelynVisible = find("mourning_town_elf_5_vis")
    val mawrth = find("mourning_town_elf_4")
    val mawrthSongOfTheElves = find("sote_mawrth")
    val mawrthCutscene = find("sote_mawrth_cutscene")

    val prifddinasElves: List<NpcType> =
        listOf(
                "prif_citizen_miriel",
                "prif_citizen_curufin",
                "prif_citizen_enerdhil",
                "prif_citizen_tatie",
                "prif_citizen_finduilas",
                "prif_citizen_gelmir",
                "prif_citizen_mithrellas",
                "prif_citizen_erestor",
                "prif_citizen_lindir",
                "prif_citizen_idril",
                "prif_citizen_ingwion",
                "prif_citizen_thingol",
                "prif_citizen_elenwe",
                "prif_citizen_orophin",
                "prif_citizen_vaire",
                "prif_citizen_elladan",
                "prif_citizen_guilin",
                "prif_citizen_ingwe",
                "prif_citizen_cirdan",
                "prif_citizen_glorfindel",
                "prif_citizen_aredhel",
                "prif_citizen_celegorm",
                "prif_citizen_anaire",
                "prif_citizen_maeglin",
                "prif_citizen_edrahil",
                "prif_citizen_fingon",
                "prif_citizen_salgant",
                "prif_citizen_celebrian",
                "prif_citizen_imin",
                "prif_citizen_oropher",
                "prif_citizen_fingolfin",
                "prif_citizen_mahtan",
                "prif_citizen_indis",
                "prif_citizen_iminye",
                "prif_citizen_feanor",
                "prif_citizen_saeros",
                "prif_citizen_nellas",
                "prif_citizen_enelye",
                "prif_citizen_nerdanel",
                "prif_citizen_nimloth",
                "prif_citizen_findis",
                "prif_citizen_earwen",
                "prif_citizen_caranthir",
                "prif_citizen_enel",
                "prif_citizen_hendor",
                "prif_citizen_galathil",
                "prif_citizen_turgon",
                "prif_citizen_lenwe",
                "prif_citizen_aranwe",
            )
            .map(::find)

    val prifddinasElfExclusions: List<NpcType> =
        listOf(
                "prif_citizen_amrod",
                "prif_citizen_nimrodel",
                "prif_citizen_maedhros",
                "prif_citizen_finarfin",
                "prif_citizen_gwindor",
                "prif_citizen_eldalote",
            )
            .map(::find)

    val vyres: List<NpcType> =
        listOf(
                "valentin_rasputin",
                "von_van_von",
                "vlad_bechstein",
                "draconis_sanguine",
                "mort_nightshade",
                "vampyrus_diaemus",
                "carnivus_belamorta",
                "vormar_vakan",
                "misdrievus_shadum",
                "vlad_diaemus",
                "noctillion_lugosi",
                "alek_constantine",
                "grigor_rasputin",
                "haemas_lamescus",
                "remus_kaninus",
                "vallessia_dracyula",
                "violetta_sanguine",
                "diphylla_bechstein",
                "episcula_helsing",
                "vampyressa_van_von",
                "vallessia_von_pitt",
                "vonnetta_varnis",
                "natalidae_shadum",
                "mortina_daubenton",
                "lasenna_rasputin",
                "caninelle_draynar",
                "valentina_diaemus",
                "nakasa_jovkai",
                "crimsonette_van_marr",
                "pipistrelle_draynar",
            )
            .map(::find)

    val guards: List<NpcType> =
        listOf(
                "hos_town_guard_01",
                "hos_town_guard_02",
                "hos_town_guard_03",
                "hos_town_guard_04",
                "jail_guard_1",
                "jail_guard_2",
                "jail_guard_3",
                "jail_guard_4",
                "jail_guard_5",
                "fai_varrock_guard",
                "fai_varrock_guard_captain",
                "guard1",
                "fai_falador_guard1",
                "fai_falador_guard2",
                "fai_falador_guard3",
                "fai_falador_guard4",
                "fai_falador_guard5",
                "fai_falador_guard6",
                "falador_doric_area_guard",
                "ardougne_guard",
                "kourend_guard_m1",
                "kourend_guard_m1_big",
                "kourend_guard_m2",
                "kourend_guard_m2_big",
                "kourend_guard_m3",
                "kourend_guard_m3_big",
                "kourend_guard_m4",
                "kourend_guard_m4_big",
                "kourend_guard_f1",
                "kourend_guard_f1_big",
                "kourend_guard_f2",
                "kourend_guard_f2_big",
                "kourend_guard_f3",
                "kourend_guard_f3_big",
                "kourend_guard_f4",
                "kourend_guard_f4_big",
                "fai_varrock_guard02",
                "fai_varrock_guard02_variant01",
                "fai_varrock_guard02_variant02",
                "fai_varrock_guard02_f",
                "fai_varrock_guard02_f_variant01",
                "fai_varrock_guard02_f_variant02",
                "fai_varrock_guard_captain02",
                "guard1_variant01",
                "guard1_f",
                "guard1_f_variant01",
                "ardougne_guard_variant01",
                "ardougne_guard_f",
                "ardougne_guard_f_variant01",
                "fai_falador_guard1_variant01",
                "fai_falador_guard1_f",
                "fai_falador_guard1_variant02",
                "fai_falador_guard2_f",
                "fai_falador_guard3_f",
                "fai_falador_guard4_f",
                "varlamore_guard_m_1",
                "varlamore_guard_m_2",
                "varlamore_guard_m_3",
                "varlamore_guard_m_4",
                "varlamore_guard_m_5",
                "varlamore_guard_f_1",
                "varlamore_guard_f_2",
                "varlamore_guard_f_3",
                "varlamore_guard_f_4",
                "varlamore_guard_f_5",
                "aldarin_guard_m_1",
                "aldarin_guard_m_2",
                "aldarin_guard_m_3",
                "aldarin_guard_m_4",
                "aldarin_guard_m_5",
                "aldarin_guard_f_1",
                "aldarin_guard_f_2",
                "aldarin_guard_f_3",
                "aldarin_guard_f_4",
                "aldarin_guard_f_5",
                "auburnvale_guard_m_1",
                "auburnvale_guard_m_2",
                "auburnvale_guard_m_3",
                "auburnvale_guard_m_4",
                "auburnvale_guard_f_1",
                "auburnvale_guard_f_2",
                "auburnvale_guard_f_3",
                "auburnvale_guard_f_4",
                "tlati_guard_m_1",
                "tlati_guard_m_2",
                "tlati_guard_m_3",
                "tlati_guard_m_4",
                "tlati_guard_f_1",
                "tlati_guard_f_2",
                "tlati_guard_f_3",
                "tlati_guard_f_4",
            )
            .map(::find)
}

public object ThievingLocs : LocReferences() {
    val hamLockedDoor = find("dttd_ham_door")
    val hamSteelChest = find("dttd_ham_chest_1")
    val hamBronzeChest = find("dttd_ham_chest_2")
    val hamSilverChest = find("dttd_ham_chest_3")
    val hamOpenChest = find("dttd_ham_chest_open")
    val hamIronChest = find("dttd_ham_chest_4")
}

public object ThievingVarbits : VarBitReferences() {
    val silentGlovesDamage = find("hunting_silent_gloves")
    val wealthyCitizenHouseKeyDryStreak = find("varlamore_thieving_house_key_drystreak")
}

public object ThievingObjs : ObjReferences() {
    val citizenPouch = find("pickpocket_coin_pouch_citizen")
    val farmerPouch = find("pickpocket_coin_pouch_farmer")
    val hamPouch = find("pickpocket_coin_pouch_ham")
    val hamSteelKey = find("dttd_key_1")
    val hamBronzeKey = find("dttd_key_2")
    val hamSilverKey = find("dttd_key_3")
    val hamIronKey = find("dttd_key_4")
    val coins = find("coins")
    val sapphire = find("sapphire")
    val emerald = find("emerald")
    val ruby = find("ruby")
    val goldRing = find("gold_ring")
    val sapphireRing = find("sapphire_ring")
    val emeraldRing = find("emerald_ring")
    val rubyRing = find("ruby_ring")
    val diamondRing = find("diamond_ring")
    val goldNecklace = find("gold_necklace")
    val sapphireNecklace = find("sapphire_necklace")
    val emeraldNecklace = find("emerald_necklace")
    val rubyNecklace = find("ruby_necklace")
    val diamondNecklace = find("diamond_necklace")
    val goldAmulet = find("strung_gold_amulet")
    val sapphireAmulet = find("strung_sapphire_amulet")
    val emeraldAmulet = find("strung_emerald_amulet")
    val rubyAmulet = find("strung_ruby_amulet")
    val diamondAmulet = find("strung_diamond_amulet")
    val warriorPouch = find("pickpocket_coin_pouch_warrior")
    val roguePouch = find("pickpocket_coin_pouch_rogue")
    val caveGoblinPouch = find("pickpocket_coin_pouch_cavegoblin")
    val guardPouch = find("pickpocket_coin_pouch_guard")
    val fremennikPouch = find("pickpocket_coin_pouch_fremennik")
    val bandit2Pouch = find("pickpocket_coin_pouch_bandit2")
    val desertBanditPouch = find("pickpocket_coin_pouch_desertbandit")
    val knightPouch = find("pickpocket_coin_pouch_knight")
    val banditPouch = find("pickpocket_coin_pouch_bandit")
    val watchmanPouch = find("pickpocket_coin_pouch_watchman")
    val menaphitePouch = find("pickpocket_coin_pouch_menaphite")
    val paladinPouch = find("pickpocket_coin_pouch_paladin")
    val gnomePouch = find("pickpocket_coin_pouch_gnome")
    val heroPouch = find("pickpocket_coin_pouch_hero")
    val elfPouch = find("pickpocket_coin_pouch_elf")
    val vyrePouch = find("pickpocket_coin_pouch_vyre")
    val wealthyCitizenPouch = find("pickpocket_coin_pouch_varlamore_wealthy")
    val wealthyCitizenHouseKey = find("varlamore_thieving_house_key")
    val crystalShard = find("prif_crystal_shard")
    val enhancedCrystalTeleportSeed = find("prif_teleport_seed")
    val potatoSeed = find("potato_seed")
    val airRune = find("airrune")
    val jugWine = find("jug_wine")
    val lockpick = find("lockpick")
    val ironDaggerP = find("iron_dagger_p")
    val bread = find("bread")
    val chaosRune = find("chaosrune")
    val antipoison1 = find("1doseantipoison")
    val deathRune = find("deathrune")
    val natureRune = find("naturerune")
    val bloodRune = find("bloodrune")
    val diamond = find("diamond")
    val fireOrb = find("fire_orb")
    val goldOre = find("gold_ore")
    val arrowShaft = find("arrow_shaft")
    val swampToad = find("swamp_toad")
    val earthRune = find("earthrune")
    val kingWorm = find("king_worm")
    val bloodPint = find("blood_pint")
    val uncutRuby = find("uncut_ruby")
    val cookedMysteryMeat = find("cooked_mystery_meat")
    val bloodShard = find("blood_shard")

    val hamShirt = find("ham_shirt")
    val hamRobe = find("ham_robe")
    val hamHood = find("ham_hood")
    val hamCloak = find("ham_cloak")
    val hamLogo = find("ham_badge")
    val hamGloves = find("ham_gloves")
    val hamBoots = find("ham_boots")
    val bronzeArrow = find("bronze_arrow")
    val bronzeAxe = find("bronze_axe")
    val bronzeDagger = find("bronze_dagger")
    val bronzePickaxe = find("bronze_pickaxe")
    val ironAxe = find("iron_axe")
    val ironDagger = find("iron_dagger")
    val ironPickaxe = find("iron_pickaxe")
    val leatherBody = find("leather_armour")
    val steelArrow = find("steel_arrow")
    val steelAxe = find("steel_axe")
    val steelDagger = find("steel_dagger")
    val steelPickaxe = find("steel_pickaxe")
    val buttons = find("digsitebuttons")
    val feather = find("feather")
    val knife = find("knife")
    val logs = find("logs")
    val needle = find("needle")
    val rawAnchovies = find("raw_anchovies")
    val rawChicken = find("raw_chicken")
    val thread = find("thread")
    val tinderbox = find("tinderbox")
    val uncutOpal = find("opal")
    val coal = find("coal")
    val cowhide = find("cow_hide")
    val damagedArmour = find("digsitearmour1")
    val grimyGuamLeaf = find("unidentified_guam")
    val grimyMarrentill = find("unidentified_marentill")
    val grimyTarromin = find("unidentified_tarromin")
    val ironOre = find("iron_ore")
    val rustySword = find("digsitesword")
    val uncutJade = find("jade")

    val rogueTop = find("roguesden_body")
    val rogueMask = find("roguesden_helm")
    val rogueTrousers = find("roguesden_legs")
    val rogueGloves = find("roguesden_gloves")
    val rogueBoots = find("roguesden_boots")

    val allCoinPouches: List<ObjType> =
        listOf(
            citizenPouch,
            farmerPouch,
            hamPouch,
            warriorPouch,
            roguePouch,
            caveGoblinPouch,
            guardPouch,
            fremennikPouch,
            bandit2Pouch,
            desertBanditPouch,
            knightPouch,
            banditPouch,
            watchmanPouch,
            menaphitePouch,
            paladinPouch,
            gnomePouch,
            heroPouch,
            elfPouch,
            vyrePouch,
            wealthyCitizenPouch,
        )
}

public data class CoinPouchDefinition(
    val obj: ObjType,
    val coins: IntRange,
    val rogueBonusCoins: IntRange = coins,
) {
    public constructor(obj: ObjType, coins: Int) : this(obj, coins..coins)

    init {
        require(!coins.isEmpty() && coins.first > 0) { "Coin-pouch values must be positive." }
        require(!rogueBonusCoins.isEmpty() && rogueBonusCoins.first > 0) {
            "Rogue coin-pouch bonuses must be positive."
        }
    }

    public fun roll(random: GameRandom, count: Int = 1): Int {
        require(count > 0) { "Coin-pouch roll count must be positive." }
        return if (coins.first == coins.last) {
            coins.first * count
        } else {
            List(count) { random.of(coins) }.sum()
        }
    }

    public fun rollRogueBonus(random: GameRandom, count: Int = 1): Int {
        require(count > 0) { "Coin-pouch roll count must be positive." }
        return if (rogueBonusCoins.first == rogueBonusCoins.last) {
            rogueBonusCoins.first * count
        } else {
            List(count) { random.of(rogueBonusCoins) }.sum()
        }
    }
}

public object CoinPouchDefinitions {
    val citizen = CoinPouchDefinition(ThievingObjs.citizenPouch, coins = 3)
    val farmer = CoinPouchDefinition(ThievingObjs.farmerPouch, coins = 9)
    val warrior = CoinPouchDefinition(ThievingObjs.warriorPouch, coins = 18)
    val rogue = CoinPouchDefinition(ThievingObjs.roguePouch, coins = 25..40)
    val guard = CoinPouchDefinition(ThievingObjs.guardPouch, coins = 30)
    val menaphite = CoinPouchDefinition(ThievingObjs.menaphitePouch, coins = 60)
    val knight = CoinPouchDefinition(ThievingObjs.knightPouch, coins = 50)
    val watchman = CoinPouchDefinition(ThievingObjs.watchmanPouch, coins = 60)
    val paladin = CoinPouchDefinition(ThievingObjs.paladinPouch, coins = 80)
    val desertBandit = CoinPouchDefinition(ThievingObjs.desertBanditPouch, coins = 30)
    val gnome = CoinPouchDefinition(ThievingObjs.gnomePouch, coins = 300)
    val hero =
        CoinPouchDefinition(ThievingObjs.heroPouch, coins = 200..300, rogueBonusCoins = 200..200)
    val elf = CoinPouchDefinition(ThievingObjs.elfPouch, coins = 280..350)
    val vyre =
        CoinPouchDefinition(ThievingObjs.vyrePouch, coins = 230..315, rogueBonusCoins = 230..230)

    val active: List<CoinPouchDefinition> =
        listOf(
            citizen,
            farmer,
            warrior,
            rogue,
            guard,
            menaphite,
            knight,
            watchman,
            paladin,
            desertBandit,
            gnome,
            hero,
            elf,
            vyre,
        )
}

public data class PickpocketReward(
    val obj: ObjType,
    val count: Int = 1,
    val weight: Int = 1,
    val coinPouch: CoinPouchDefinition? = null,
    val countRange: IntRange = count..count,
) {
    init {
        require(count > 0) { "Pickpocket reward counts must be positive." }
        require(!countRange.isEmpty() && countRange.first > 0) {
            "Pickpocket reward count ranges must be positive."
        }
    }

    public fun rollCount(random: GameRandom): Int =
        if (countRange.first == countRange.last) countRange.first else random.of(countRange)
}

public data class PickpocketTertiaryDrop(
    val obj: ObjType,
    val denominator: Int,
    val rogueDoubles: Boolean = true,
) {
    init {
        require(denominator > 0) { "Pickpocket tertiary denominators must be positive." }
    }
}

public data class PickpocketDefinition(
    val name: String,
    val npcs: List<NpcType>,
    val level: Int,
    val xp: Double,
    val chanceLow: Int,
    val chanceHigh: Int,
    val rewards: List<PickpocketReward>,
    val damage: Int,
    val pickpocketBlockCycles: Int,
    val movementBlockCycles: Int,
    val mediumArdougneDiaryApplies: Boolean = false,
    val guaranteedRewards: List<PickpocketReward> = emptyList(),
    val clueDrop: PickpocketClueDrop? = null,
    val tertiaryDrop: PickpocketTertiaryDrop? = null,
    val requiresFreeInventorySlot: Boolean = false,
    val rewardSelector: ((ProtectedAccess) -> PickpocketReward)? = null,
) {
    public val totalRewardWeight: Int = rewards.sumOf { it.weight }
    public val allRewards: List<PickpocketReward> = rewards + guaranteedRewards

    init {
        require(rewards.isNotEmpty()) { "Pickpocket rewards must not be empty." }
        require(allRewards.all { it.count > 0 && it.weight > 0 }) {
            "Pickpocket reward counts and weights must be positive."
        }
    }

    public fun rewardAt(roll: Int): PickpocketReward {
        require(roll in 0 until totalRewardWeight) {
            "Reward roll $roll must be in 0 until $totalRewardWeight."
        }
        var cursor = 0
        for (reward in rewards) {
            cursor += reward.weight
            if (roll < cursor) {
                return reward
            }
        }
        error("Unreachable reward roll: $roll")
    }
}

public object StandardPickpockets {
    val man =
        PickpocketDefinition(
            name = "man",
            npcs =
                listOf(
                    ThievingNpcs.man,
                    ThievingNpcs.indoorMan,
                    ThievingNpcs.zeahMan,
                    ThievingNpcs.man2,
                    ThievingNpcs.zeahMan2,
                    ThievingNpcs.man3,
                    ThievingNpcs.zeahMan3,
                    ThievingNpcs.man4,
                    ThievingNpcs.deathIndoorMan,
                    ThievingNpcs.manMusaPoint,
                    ThievingNpcs.alKharidMan,
                    ThievingNpcs.faladorMan2,
                    ThievingNpcs.faladorMan3,
                    ThievingNpcs.karamjaMan,
                    ThievingNpcs.varrockMan,
                    ThievingNpcs.shayzienMan1,
                    ThievingNpcs.shayzienMan2,
                ),
            level = 1,
            xp = 8.0,
            chanceLow = 180,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.citizenPouch,
                        coinPouch = CoinPouchDefinitions.citizen,
                    )
                ),
            damage = 1,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val farmer =
        PickpocketDefinition(
            name = "farmer",
            npcs =
                listOf(
                    ThievingNpcs.farmer1,
                    ThievingNpcs.farmer2,
                    ThievingNpcs.farmer3,
                    ThievingNpcs.farmer1F,
                    ThievingNpcs.farmer2F,
                    ThievingNpcs.farmer3F,
                    ThievingNpcs.farmer4,
                    ThievingNpcs.varlamoreFarmerM1,
                    ThievingNpcs.varlamoreFarmerM2,
                    ThievingNpcs.varlamoreFarmerM3,
                    ThievingNpcs.varlamoreFarmerM4,
                    ThievingNpcs.varlamoreFarmerF1,
                    ThievingNpcs.varlamoreFarmerF2,
                    ThievingNpcs.varlamoreFarmerF3,
                    ThievingNpcs.varlamoreFarmerF4,
                    ThievingNpcs.kastoriFarmerM1,
                    ThievingNpcs.kastoriFarmerM2,
                    ThievingNpcs.kastoriFarmerF1,
                    ThievingNpcs.kastoriFarmerF2,
                    ThievingNpcs.talTeklanFarmer,
                ),
            level = 10,
            xp = 14.5,
            chanceLow = 150,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.farmerPouch,
                        weight = 123,
                        coinPouch = CoinPouchDefinitions.farmer,
                    ),
                    PickpocketReward(obj = ThievingObjs.potatoSeed, weight = 5),
                ),
            damage = 1,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val woman =
        man.copy(
            name = "woman",
            npcs =
                listOf(
                    ThievingNpcs.woman,
                    ThievingNpcs.zeahWoman,
                    ThievingNpcs.zeahWomanOutside,
                    ThievingNpcs.woman2,
                    ThievingNpcs.zeahWoman2,
                    ThievingNpcs.woman3,
                    ThievingNpcs.zeahWoman3,
                    ThievingNpcs.varrockWoman,
                    ThievingNpcs.shayzienWoman1,
                    ThievingNpcs.shayzienWoman2,
                ),
        )

    val warrior =
        PickpocketDefinition(
            name = "warrior",
            npcs =
                listOf(
                    ThievingNpcs.warrior,
                    ThievingNpcs.warrior2,
                    ThievingNpcs.warrior3,
                    ThievingNpcs.warrior4,
                    ThievingNpcs.warrior5,
                    ThievingNpcs.warrior6,
                    ThievingNpcs.alKharidWarrior,
                ),
            level = 25,
            xp = 26.0,
            chanceLow = 100,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.warriorPouch,
                        coinPouch = CoinPouchDefinitions.warrior,
                    )
                ),
            damage = 2,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val guard =
        PickpocketDefinition(
            name = "guard",
            npcs = ThievingNpcs.guards,
            level = 40,
            xp = 46.8,
            chanceLow = 50,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.guardPouch,
                        coinPouch = CoinPouchDefinitions.guard,
                    )
                ),
            damage = 2,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val rogue =
        PickpocketDefinition(
            name = "rogue",
            npcs = listOf(ThievingNpcs.rogue),
            level = 32,
            xp = 36.5,
            chanceLow = 75,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.roguePouch,
                        weight = 123,
                        coinPouch = CoinPouchDefinitions.rogue,
                    ),
                    PickpocketReward(obj = ThievingObjs.airRune, count = 8, weight = 9),
                    PickpocketReward(obj = ThievingObjs.jugWine, weight = 6),
                    PickpocketReward(obj = ThievingObjs.lockpick, weight = 5),
                    PickpocketReward(obj = ThievingObjs.ironDaggerP, weight = 1),
                ),
            damage = 2,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val knightOfArdougne =
        PickpocketDefinition(
            name = "knight",
            npcs =
                listOf(
                    ThievingNpcs.knightOfArdougne,
                    ThievingNpcs.knightOfArdougne2,
                    ThievingNpcs.knightOfArdougneWest,
                    ThievingNpcs.knightOfArdougneFWest,
                    ThievingNpcs.knightOfArdougneF,
                ),
            level = 55,
            xp = 84.3,
            chanceLow = 50,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.knightPouch,
                        coinPouch = CoinPouchDefinitions.knight,
                    )
                ),
            damage = 3,
            pickpocketBlockCycles = 10,
            movementBlockCycles = 11,
            mediumArdougneDiaryApplies = true,
        )

    val knightOfVarlamore =
        knightOfArdougne.copy(
            npcs =
                listOf(
                    ThievingNpcs.varlamoreKnightM1,
                    ThievingNpcs.varlamoreKnightM2,
                    ThievingNpcs.varlamoreKnightM3,
                    ThievingNpcs.varlamoreKnightF1,
                    ThievingNpcs.varlamoreKnightF2,
                    ThievingNpcs.varlamoreKnightF3,
                ),
            mediumArdougneDiaryApplies = false,
        )

    val watchman =
        PickpocketDefinition(
            name = "watchman",
            npcs = listOf(ThievingNpcs.watchman),
            level = 65,
            xp = 137.5,
            chanceLow = 15,
            chanceHigh = 160,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.watchmanPouch,
                        coinPouch = CoinPouchDefinitions.watchman,
                    )
                ),
            damage = 3,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
            guaranteedRewards = listOf(PickpocketReward(obj = ThievingObjs.bread)),
        )

    val menaphiteThug =
        PickpocketDefinition(
            name = "Menaphite",
            npcs = listOf(ThievingNpcs.menaphiteThugAfterQuest),
            level = 65,
            xp = 137.5,
            chanceLow = 50,
            chanceHigh = 160,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.menaphitePouch,
                        coinPouch = CoinPouchDefinitions.menaphite,
                    )
                ),
            damage = 5,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val paladin =
        PickpocketDefinition(
            name = "paladin",
            npcs =
                listOf(
                    ThievingNpcs.paladin,
                    ThievingNpcs.paladin2,
                    ThievingNpcs.paladinWest,
                    ThievingNpcs.paladinWestF,
                    ThievingNpcs.paladinVariant1,
                    ThievingNpcs.paladinVariant2,
                    ThievingNpcs.paladinF,
                    ThievingNpcs.paladinFVariant1,
                ),
            level = 70,
            xp = 131.8,
            chanceLow = 40,
            chanceHigh = 170,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.paladinPouch,
                        coinPouch = CoinPouchDefinitions.paladin,
                    )
                ),
            damage = 3,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
            mediumArdougneDiaryApplies = true,
            guaranteedRewards = listOf(PickpocketReward(obj = ThievingObjs.chaosRune, count = 2)),
            clueDrop =
                PickpocketClueDrop(
                    tier = org.rsmod.api.clue.ClueTier.Hard,
                    denominator = 1_000,
                    combatAchievementTierStatus = varbits.ca_tier_status_hard,
                ),
        )

    val desertBandit =
        PickpocketDefinition(
            name = "bandit",
            npcs = listOf(ThievingNpcs.desertBandit, ThievingNpcs.desertBanditFree),
            level = 53,
            xp = 79.4,
            chanceLow = 50,
            chanceHigh = 240,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.desertBanditPouch,
                        weight = 5,
                        coinPouch = CoinPouchDefinitions.desertBandit,
                    ),
                    PickpocketReward(obj = ThievingObjs.antipoison1),
                    PickpocketReward(obj = ThievingObjs.lockpick),
                ),
            damage = 3,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
        )

    val hero =
        PickpocketDefinition(
            name = "hero",
            npcs = listOf(ThievingNpcs.hero, ThievingNpcs.heroVariant1, ThievingNpcs.heroF),
            level = 80,
            xp = 163.3,
            chanceLow = 39,
            chanceHigh = 160,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.heroPouch,
                        weight = 105,
                        coinPouch = CoinPouchDefinitions.hero,
                    ),
                    PickpocketReward(obj = ThievingObjs.deathRune, count = 2, weight = 8),
                    PickpocketReward(obj = ThievingObjs.jugWine, weight = 6),
                    PickpocketReward(obj = ThievingObjs.bloodRune, weight = 5),
                    PickpocketReward(obj = ThievingObjs.diamond),
                    PickpocketReward(obj = ThievingObjs.fireOrb, weight = 2),
                    PickpocketReward(obj = ThievingObjs.goldOre),
                ),
            damage = 3,
            pickpocketBlockCycles = 10,
            movementBlockCycles = 11,
            mediumArdougneDiaryApplies = true,
            clueDrop =
                PickpocketClueDrop(
                    tier = org.rsmod.api.clue.ClueTier.Elite,
                    denominator = 1_400,
                    combatAchievementTierStatus = varbits.ca_tier_status_elite,
                ),
        )

    val gnome =
        PickpocketDefinition(
            name = "gnome",
            npcs =
                listOf(
                    ThievingNpcs.gnome,
                    ThievingNpcs.brownClothedGnome,
                    ThievingNpcs.darkSkinnedGnome,
                ),
            level = 75,
            xp = 133.5,
            chanceLow = 43,
            chanceHigh = 175,
            rewards =
                listOf(
                    PickpocketReward(obj = ThievingObjs.arrowShaft, weight = 56, countRange = 2..4),
                    PickpocketReward(
                        obj = ThievingObjs.gnomePouch,
                        weight = 30,
                        coinPouch = CoinPouchDefinitions.gnome,
                    ),
                    PickpocketReward(obj = ThievingObjs.swampToad, weight = 24),
                    PickpocketReward(obj = ThievingObjs.goldOre, weight = 8),
                    PickpocketReward(obj = ThievingObjs.earthRune, weight = 5),
                    PickpocketReward(obj = ThievingObjs.kingWorm, weight = 3),
                    PickpocketReward(obj = ThievingObjs.fireOrb, weight = 2),
                ),
            damage = 1,
            pickpocketBlockCycles = 8,
            movementBlockCycles = 9,
            clueDrop =
                PickpocketClueDrop(
                    tier = org.rsmod.api.clue.ClueTier.Medium,
                    denominator = 150,
                    combatAchievementTierStatus = varbits.ca_tier_status_medium,
                ),
        )

    val lletyaElf =
        PickpocketDefinition(
            name = "elf",
            npcs =
                listOf(
                    ThievingNpcs.goreu,
                    ThievingNpcs.arvel,
                    ThievingNpcs.kelynVisible,
                    ThievingNpcs.mawrth,
                ),
            level = 85,
            xp = 353.3,
            chanceLow = 6,
            chanceHigh = 100,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.elfPouch,
                        weight = 105,
                        coinPouch = CoinPouchDefinitions.elf,
                    ),
                    PickpocketReward(obj = ThievingObjs.deathRune, count = 2, weight = 8),
                    PickpocketReward(obj = ThievingObjs.natureRune, count = 3, weight = 5),
                    PickpocketReward(obj = ThievingObjs.jugWine, weight = 6),
                    PickpocketReward(obj = ThievingObjs.diamond),
                    PickpocketReward(obj = ThievingObjs.fireOrb, weight = 2),
                    PickpocketReward(obj = ThievingObjs.goldOre),
                ),
            damage = 5,
            pickpocketBlockCycles = 10,
            movementBlockCycles = 11,
        )

    val vyre =
        PickpocketDefinition(
            name = "vyre",
            npcs = ThievingNpcs.vyres,
            level = 82,
            xp = 306.9,
            chanceLow = 8,
            chanceHigh = 128,
            rewards =
                listOf(
                    PickpocketReward(
                        obj = ThievingObjs.vyrePouch,
                        weight = 109,
                        coinPouch = CoinPouchDefinitions.vyre,
                    ),
                    PickpocketReward(obj = ThievingObjs.deathRune, count = 2, weight = 8),
                    PickpocketReward(obj = ThievingObjs.bloodRune, count = 4, weight = 2),
                    PickpocketReward(obj = ThievingObjs.bloodPint, weight = 6),
                    PickpocketReward(obj = ThievingObjs.uncutRuby, weight = 5),
                    PickpocketReward(obj = ThievingObjs.diamond),
                    PickpocketReward(obj = ThievingObjs.cookedMysteryMeat),
                ),
            damage = 5,
            pickpocketBlockCycles = 10,
            movementBlockCycles = 11,
            tertiaryDrop =
                PickpocketTertiaryDrop(obj = ThievingObjs.bloodShard, denominator = 5_000),
        )

    val all: List<PickpocketDefinition> =
        listOf(
            man,
            woman,
            farmer,
            warrior,
            rogue,
            guard,
            knightOfArdougne,
            knightOfVarlamore,
            watchman,
            menaphiteThug,
            paladin,
            desertBandit,
            gnome,
            hero,
            lletyaElf,
            vyre,
        )

    val allNpcs: Set<NpcType> = all.flatMapTo(linkedSetOf()) { it.npcs }
}

internal fun Iterable<ObjType>.countIn(inventory: org.rsmod.game.inv.Inventory): Int {
    val ids = mapTo(hashSetOf()) { it.id }
    return inventory.filterNotNull { it.id in ids }.sumOf { it.count }
}

internal fun org.rsmod.game.inv.Inventory.has(type: ObjType): Boolean = any { it?.id == type.id }
