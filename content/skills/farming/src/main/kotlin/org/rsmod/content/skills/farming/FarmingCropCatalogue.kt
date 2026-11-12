package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.type.obj.ObjType

@Singleton
public class FarmingCropCatalogue @Inject constructor() {
    public val crops: List<FarmingCrop> = buildList {
        allotment("potato", "Potato", FarmingObjs.potatoSeed, FarmingObjs.potato, 1, 8.0, 9.0, 5)
        allotment("onion", "Onion", FarmingObjs.onionSeed, FarmingObjs.onion, 5, 9.5, 10.5, 5)
        allotment(
            "cabbage",
            "Cabbage",
            FarmingObjs.cabbageSeed,
            FarmingObjs.cabbage,
            7,
            10.0,
            11.5,
            5,
        )
        allotment("tomato", "Tomato", FarmingObjs.tomatoSeed, FarmingObjs.tomato, 12, 12.5, 14.0, 5)
        allotment(
            "sweetcorn",
            "Sweetcorn",
            FarmingObjs.sweetcornSeed,
            FarmingObjs.sweetcorn,
            20,
            17.0,
            19.0,
            7,
        )
        allotment(
            "strawberry",
            "Strawberry",
            FarmingObjs.strawberrySeed,
            FarmingObjs.strawberry,
            31,
            26.0,
            29.0,
            7,
        )
        allotment(
            "watermelon",
            "Watermelon",
            FarmingObjs.watermelonSeed,
            FarmingObjs.watermelon,
            47,
            48.5,
            54.5,
            9,
        )
        allotment(
            "snape_grass",
            "Snape grass",
            FarmingObjs.snapeGrassSeed,
            FarmingObjs.snapeGrass,
            61,
            82.0,
            82.0,
            8,
        )

        flower("marigold", "Marigold", FarmingObjs.marigoldSeed, FarmingObjs.marigold, 2, 8.5, 47.0)
        flower(
            "rosemary",
            "Rosemary",
            FarmingObjs.rosemarySeed,
            FarmingObjs.rosemary,
            11,
            12.0,
            66.5,
        )
        flower(
            "nasturtium",
            "Nasturtium",
            FarmingObjs.nasturtiumSeed,
            FarmingObjs.nasturtium,
            24,
            19.5,
            111.0,
        )
        flower("woad", "Woad", FarmingObjs.woadSeed, FarmingObjs.woad, 25, 20.5, 115.5)
        flower(
            "limpwurt",
            "Limpwurt",
            FarmingObjs.limpwurtSeed,
            FarmingObjs.limpwurt,
            26,
            21.5,
            120.0,
        )
        flower(
            "white_lily",
            "White lily",
            FarmingObjs.whiteLilySeed,
            FarmingObjs.whiteLily,
            58,
            42.0,
            250.0,
        )

        hops("barley", "Barley", FarmingObjs.barleySeed, FarmingObjs.barley, 3, 4, 8.5, 9.5, 5)
        hops(
            "hammerstone",
            "Hammerstone hops",
            FarmingObjs.hammerstoneSeed,
            FarmingObjs.hammerstone,
            4,
            4,
            9.0,
            10.0,
            5,
        )
        hops(
            "asgarnian",
            "Asgarnian hops",
            FarmingObjs.asgarnianSeed,
            FarmingObjs.asgarnian,
            8,
            4,
            10.9,
            12.0,
            6,
        )
        hops("jute", "Jute", FarmingObjs.juteSeed, FarmingObjs.jute, 13, 3, 13.0, 14.5, 6)
        hops(
            "yanillian",
            "Yanillian hops",
            FarmingObjs.yanillianSeed,
            FarmingObjs.yanillian,
            16,
            4,
            14.5,
            16.0,
            7,
        )
        hops(
            "krandorian",
            "Krandorian hops",
            FarmingObjs.krandorianSeed,
            FarmingObjs.krandorian,
            21,
            4,
            17.5,
            19.5,
            8,
        )
        hops(
            "wildblood",
            "Wildblood hops",
            FarmingObjs.wildbloodSeed,
            FarmingObjs.wildblood,
            28,
            4,
            23.0,
            26.0,
            9,
        )

        bush(
            "redberry",
            "Redberry",
            FarmingObjs.redberrySeed,
            FarmingObjs.redberries,
            10,
            11.5,
            64.0,
            4.5,
            6,
        )
        bush(
            "cadavaberry",
            "Cadavaberry",
            FarmingObjs.cadavaberrySeed,
            FarmingObjs.cadavaberries,
            22,
            18.0,
            102.5,
            7.0,
            7,
        )
        bush(
            "dwellberry",
            "Dwellberry",
            FarmingObjs.dwellberrySeed,
            FarmingObjs.dwellberries,
            36,
            31.5,
            177.5,
            12.0,
            8,
        )
        bush(
            "jangerberry",
            "Jangerberry",
            FarmingObjs.jangerberrySeed,
            FarmingObjs.jangerberries,
            48,
            50.5,
            284.5,
            19.0,
            9,
        )
        bush(
            "whiteberry",
            "Whiteberry",
            FarmingObjs.whiteberrySeed,
            FarmingObjs.whiteberries,
            59,
            78.0,
            437.5,
            29.0,
            9,
        )
        bush(
            "poisonivy",
            "Poison ivy",
            FarmingObjs.poisonIvySeed,
            FarmingObjs.poisonIvyBerries,
            70,
            120.0,
            675.0,
            45.0,
            9,
            true,
        )

        fruit(
            "apple",
            "Apple tree",
            FarmingObjs.appleSapling,
            FarmingObjs.apple,
            27,
            22.0,
            1199.5,
            8.5,
        )

        woodTree(
            "oak",
            "Oak tree",
            FarmingPatchType.Tree,
            FarmingObjs.oakSapling,
            FarmingObjs.oakLogs,
            FarmingObjs.oakRoots,
            15,
            14.0,
            467.3,
            5,
            1,
            37.5,
        )
        woodTree(
            "willow",
            "Willow tree",
            FarmingPatchType.Tree,
            FarmingObjs.willowSapling,
            FarmingObjs.willowLogs,
            FarmingObjs.willowRoots,
            30,
            25.0,
            1456.5,
            6,
            30,
            67.5,
        )
        woodTree(
            "maple",
            "Maple tree",
            FarmingPatchType.Tree,
            FarmingObjs.mapleSapling,
            FarmingObjs.mapleLogs,
            FarmingObjs.mapleRoots,
            45,
            45.0,
            3403.4,
            8,
            45,
            100.0,
        )
        woodTree(
            "yew",
            "Yew tree",
            FarmingPatchType.Tree,
            FarmingObjs.yewSapling,
            FarmingObjs.yewLogs,
            FarmingObjs.yewRoots,
            60,
            81.0,
            7069.9,
            10,
            60,
            175.0,
        )
        woodTree(
            "magic",
            "Magic tree",
            FarmingPatchType.Tree,
            FarmingObjs.magicSapling,
            FarmingObjs.magicLogs,
            FarmingObjs.magicRoots,
            75,
            145.5,
            13768.3,
            12,
            75,
            250.0,
        )
        woodTree(
            "teak",
            "Teak tree",
            FarmingPatchType.HardwoodTree,
            FarmingObjs.teakSapling,
            FarmingObjs.teakLogs,
            null,
            35,
            35.0,
            7290.0,
            9,
            35,
            85.0,
            640,
        )
        woodTree(
            "mahogany",
            "Mahogany tree",
            FarmingPatchType.HardwoodTree,
            FarmingObjs.mahoganySapling,
            FarmingObjs.mahoganyLogs,
            null,
            55,
            63.0,
            15720.0,
            9,
            50,
            125.0,
            640,
        )
        woodTree(
            "celastrus",
            "Celastrus tree",
            FarmingPatchType.Celastrus,
            FarmingObjs.celastrusSapling,
            FarmingObjs.celastrusBark,
            null,
            85,
            204.0,
            14130.0,
            6,
            1,
            23.5,
            800,
            baseHarvestLives = 3,
        )
        woodTree(
            "redwood",
            "Redwood tree",
            FarmingPatchType.Redwood,
            FarmingObjs.redwoodSeed,
            FarmingObjs.redwoodLogs,
            null,
            90,
            230.0,
            22450.0,
            11,
            90,
            380.0,
            640,
        )
        add(
            FarmingCrop(
                "crystal",
                "Crystal tree",
                FarmingPatchType.CrystalTree,
                FarmingObjs.crystalAcorn,
                null,
                listOf("crystal_tree"),
                74,
                1,
                480,
                7,
                0,
                plantXp = 126.0,
                healthXp = 13240.0,
                compostAffectsYield = false,
                diseaseImmune = true,
            )
        )
        add(
            FarmingCrop(
                "calquat",
                "Calquat tree",
                FarmingPatchType.Calquat,
                FarmingObjs.calquatSapling,
                FarmingObjs.calquatFruit,
                listOf("calquat_tree", "calquat"),
                72,
                1,
                160,
                9,
                6,
                regrowMinutes = 20,
                plantXp = 129.5,
                harvestXp = 48.5,
                healthXp = 12096.0,
                compostAffectsYield = false,
                protection = PROTECTION["calquat"],
            )
        )
        add(
            FarmingCrop(
                "spirit",
                "Spirit tree",
                FarmingPatchType.SpiritTree,
                FarmingObjs.spiritSapling,
                null,
                listOf("spirit_tree"),
                83,
                1,
                320,
                13,
                0,
                plantXp = 199.5,
                healthXp = 19301.8,
                compostAffectsYield = false,
                protection = PROTECTION["spirit"],
            )
        )
        fruit(
            "banana",
            "Banana tree",
            FarmingObjs.bananaSapling,
            FarmingObjs.banana,
            33,
            28.0,
            1750.5,
            10.5,
        )
        fruit(
            "orange",
            "Orange tree",
            FarmingObjs.orangeSapling,
            FarmingObjs.orange,
            39,
            35.5,
            2470.2,
            13.5,
        )
        fruit(
            "curry",
            "Curry tree",
            FarmingObjs.currySapling,
            FarmingObjs.curryLeaf,
            42,
            40.0,
            2906.9,
            15.0,
        )
        fruit(
            "pineapple",
            "Pineapple plant",
            FarmingObjs.pineappleSapling,
            FarmingObjs.pineapple,
            51,
            57.0,
            4605.7,
            21.5,
        )
        fruit(
            "papaya",
            "Papaya tree",
            FarmingObjs.papayaSapling,
            FarmingObjs.papaya,
            57,
            72.0,
            6146.4,
            27.0,
        )
        fruit(
            "palm",
            "Palm tree",
            FarmingObjs.palmSapling,
            FarmingObjs.coconut,
            68,
            110.5,
            10150.1,
            41.5,
        )
        fruit(
            "dragonfruit",
            "Dragonfruit tree",
            FarmingObjs.dragonfruitSapling,
            FarmingObjs.dragonfruit,
            81,
            140.0,
            17755.0,
            70.0,
        )

        special(
            "seaweed",
            "Giant seaweed",
            FarmingPatchType.Seaweed,
            FarmingObjs.seaweedSpore,
            FarmingObjs.giantSeaweed,
            23,
            2,
            10,
            5,
            3,
            19.0,
            21.0,
        )
        special(
            "bittercap",
            "Bittercap mushroom",
            FarmingPatchType.Mushroom,
            FarmingObjs.mushroomSpore,
            FarmingObjs.bittercapMushroom,
            53,
            1,
            40,
            7,
            6,
            61.5,
            57.7,
            compostAffectsYield = false,
        )
        special(
            "belladonna",
            "Belladonna",
            FarmingPatchType.Belladonna,
            FarmingObjs.belladonnaSeed,
            FarmingObjs.nightshade,
            63,
            1,
            80,
            5,
            1,
            91.0,
            512.0,
            compostAffectsYield = false,
        )
        special(
            "cactus",
            "Cactus",
            FarmingPatchType.Cactus,
            FarmingObjs.cactusSeed,
            FarmingObjs.cactusSpine,
            55,
            1,
            80,
            8,
            3,
            66.5,
            25.0,
            healthXp = 374.0,
            regrowMinutes = 20,
            compostAffectsYield = false,
        )
        special(
            "potato_cactus",
            "Potato cactus",
            FarmingPatchType.Cactus,
            FarmingObjs.potatoCactusSeed,
            FarmingObjs.potatoCactus,
            64,
            1,
            10,
            8,
            6,
            68.0,
            68.0,
            healthXp = 230.0,
            regrowMinutes = 5,
            compostAffectsYield = false,
        )

        herb("guam", "Guam", FarmingObjs.guamSeed, FarmingObjs.guam, 9, 11.0, 12.5)
        herb(
            "marrentill",
            "Marrentill",
            FarmingObjs.marrentillSeed,
            FarmingObjs.marrentill,
            14,
            13.5,
            15.0,
        )
        herb("tarromin", "Tarromin", FarmingObjs.tarrominSeed, FarmingObjs.tarromin, 19, 16.0, 18.0)
        herb(
            "harralander",
            "Harralander",
            FarmingObjs.harralanderSeed,
            FarmingObjs.harralander,
            26,
            21.5,
            24.0,
        )
        herb("ranarr", "Ranarr", FarmingObjs.ranarrSeed, FarmingObjs.ranarr, 32, 27.0, 30.5)
        herb("toadflax", "Toadflax", FarmingObjs.toadflaxSeed, FarmingObjs.toadflax, 38, 34.0, 38.5)
        herb("irit", "Irit", FarmingObjs.iritSeed, FarmingObjs.irit, 44, 43.0, 48.5)
        herb("avantoe", "Avantoe", FarmingObjs.avantoeSeed, FarmingObjs.avantoe, 50, 54.5, 61.5)
        herb("kwuarm", "Kwuarm", FarmingObjs.kwuarmSeed, FarmingObjs.kwuarm, 56, 69.0, 78.0)
        herb(
            "snapdragon",
            "Snapdragon",
            FarmingObjs.snapdragonSeed,
            FarmingObjs.snapdragon,
            62,
            87.5,
            98.5,
        )
        herb("huasca", "Huasca", FarmingObjs.huascaSeed, FarmingObjs.huasca, 65, 86.5, 110.0)
        herb(
            "cadantine",
            "Cadantine",
            FarmingObjs.cadantineSeed,
            FarmingObjs.cadantine,
            67,
            106.5,
            120.0,
        )
        herb(
            "lantadyme",
            "Lantadyme",
            FarmingObjs.lantadymeSeed,
            FarmingObjs.lantadyme,
            73,
            134.5,
            151.5,
        )
        herb(
            "dwarf_weed",
            "Dwarf weed",
            FarmingObjs.dwarfWeedSeed,
            FarmingObjs.dwarfWeed,
            79,
            170.5,
            192.0,
        )
        herb("torstol", "Torstol", FarmingObjs.torstolSeed, FarmingObjs.torstol, 85, 199.5, 224.5)
    }

    private val byKey = crops.associateBy(FarmingCrop::key)
    private val bySeed = crops.associateBy { it.seed.id }

    public operator fun get(key: String): FarmingCrop? = byKey[key]

    public fun forSeed(id: Int): FarmingCrop? = bySeed[id]

    private fun MutableList<FarmingCrop>.allotment(
        key: String,
        name: String,
        seed: ObjType,
        produce: ObjType,
        level: Int,
        plantXp: Double,
        harvestXp: Double,
        stages: Int,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                FarmingPatchType.Allotment,
                seed,
                produce,
                listOf(key),
                level,
                3,
                10,
                stages,
                3,
                plantXp = plantXp,
                harvestXp = harvestXp,
                harvestSaveLow = YIELD_CONSTANTS[key]?.first,
                harvestSaveHigh = YIELD_CONSTANTS[key]?.second,
                protection = PROTECTION[key],
            )
        )

    private fun MutableList<FarmingCrop>.flower(
        key: String,
        name: String,
        seed: ObjType,
        produce: ObjType,
        level: Int,
        plantXp: Double,
        harvestXp: Double,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                FarmingPatchType.Flower,
                seed,
                produce,
                listOf(key),
                level,
                1,
                5,
                5,
                1,
                plantXp = plantXp,
                harvestXp = harvestXp,
            )
        )

    private fun MutableList<FarmingCrop>.herb(
        key: String,
        name: String,
        seed: ObjType,
        produce: ObjType,
        level: Int,
        plantXp: Double,
        harvestXp: Double,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                FarmingPatchType.Herb,
                seed,
                produce,
                listOf(key),
                level,
                1,
                20,
                5,
                3,
                plantXp = plantXp,
                harvestXp = harvestXp,
                diseaseChanceOutOf128 = 27,
                harvestSaveLow = YIELD_CONSTANTS[key]?.first,
                harvestSaveHigh = YIELD_CONSTANTS[key]?.second,
            )
        )

    private fun MutableList<FarmingCrop>.hops(
        key: String,
        name: String,
        seed: ObjType,
        produce: ObjType,
        level: Int,
        seedCount: Int,
        plantXp: Double,
        harvestXp: Double,
        stages: Int,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                FarmingPatchType.Hops,
                seed,
                produce,
                listOf(key),
                level,
                seedCount,
                10,
                stages,
                3,
                plantXp = plantXp,
                harvestXp = harvestXp,
                harvestSaveLow = YIELD_CONSTANTS[key]?.first,
                harvestSaveHigh = YIELD_CONSTANTS[key]?.second,
                protection = PROTECTION[key],
            )
        )

    private fun MutableList<FarmingCrop>.bush(
        key: String,
        name: String,
        seed: ObjType,
        produce: ObjType,
        level: Int,
        plantXp: Double,
        healthXp: Double,
        harvestXp: Double,
        stages: Int,
        immune: Boolean = false,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                FarmingPatchType.Bush,
                seed,
                produce,
                listOf(key),
                level,
                1,
                20,
                stages,
                4,
                regrowMinutes = 20,
                plantXp = plantXp,
                harvestXp = harvestXp,
                healthXp = healthXp,
                compostAffectsYield = false,
                diseaseImmune = immune,
                protection = PROTECTION[key],
            )
        )

    private fun MutableList<FarmingCrop>.fruit(
        key: String,
        name: String,
        sapling: ObjType,
        produce: ObjType,
        level: Int,
        plantXp: Double,
        healthXp: Double,
        harvestXp: Double,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                FarmingPatchType.FruitTree,
                sapling,
                produce,
                listOf(key),
                level,
                1,
                160,
                7,
                6,
                regrowMinutes = 45,
                plantXp = plantXp,
                harvestXp = harvestXp,
                healthXp = healthXp,
                compostAffectsYield = false,
                diseaseChanceOutOf128 = 18,
                protection = PROTECTION[key],
            )
        )

    private fun MutableList<FarmingCrop>.special(
        key: String,
        name: String,
        patchType: FarmingPatchType,
        seed: ObjType,
        produce: ObjType,
        level: Int,
        seedCount: Int,
        growthMinutes: Int,
        stages: Int,
        harvestLives: Int,
        plantXp: Double,
        harvestXp: Double,
        healthXp: Double = 0.0,
        regrowMinutes: Int = 0,
        compostAffectsYield: Boolean = true,
    ) =
        add(
            FarmingCrop(
                key = key,
                displayName = name,
                patchType = patchType,
                seed = seed,
                produce = produce,
                visualPrefixes = listOf(key),
                level = level,
                seedCount = seedCount,
                growthMinutes = growthMinutes,
                growthStages = stages,
                baseHarvestLives = harvestLives,
                regrowMinutes = regrowMinutes,
                plantXp = plantXp,
                harvestXp = harvestXp,
                healthXp = healthXp,
                compostAffectsYield = compostAffectsYield,
                harvestSaveLow = YIELD_CONSTANTS[key]?.first,
                harvestSaveHigh = YIELD_CONSTANTS[key]?.second,
                protection = PROTECTION[key],
            )
        )

    private fun MutableList<FarmingCrop>.woodTree(
        key: String,
        name: String,
        patchType: FarmingPatchType,
        sapling: ObjType,
        logs: ObjType,
        roots: ObjType?,
        level: Int,
        plantXp: Double,
        healthXp: Double,
        stages: Int,
        woodcuttingLevel: Int,
        woodcuttingXp: Double,
        growthMinutes: Int = 40,
        baseHarvestLives: Int = 0,
    ) =
        add(
            FarmingCrop(
                key,
                name,
                patchType,
                sapling,
                if (baseHarvestLives > 0) logs else null,
                listOf("${key}_tree", key),
                level,
                1,
                growthMinutes,
                stages,
                baseHarvestLives,
                plantXp = plantXp,
                harvestXp = if (baseHarvestLives > 0) woodcuttingXp else 0.0,
                healthXp = healthXp,
                compostAffectsYield = baseHarvestLives > 0,
                woodcuttingLevel = woodcuttingLevel,
                woodcuttingXp = woodcuttingXp,
                logs = logs,
                roots = roots,
                protection = PROTECTION[key],
                harvestSaveLow = YIELD_CONSTANTS[key]?.first,
                harvestSaveHigh = YIELD_CONSTANTS[key]?.second,
            )
        )

    private companion object {
        val YIELD_CONSTANTS =
            mapOf(
                "potato" to (101 to 180),
                "onion" to (105 to 180),
                "cabbage" to (107 to 180),
                "tomato" to (112 to 180),
                "sweetcorn" to (88 to 180),
                "strawberry" to (103 to 180),
                "watermelon" to (126 to 180),
                "snape_grass" to (148 to 195),
                "barley" to (103 to 180),
                "hammerstone" to (104 to 180),
                "asgarnian" to (108 to 180),
                "jute" to (113 to 180),
                "yanillian" to (116 to 180),
                "krandorian" to (121 to 180),
                "wildblood" to (128 to 180),
                "seaweed" to (150 to 210),
                "guam" to (25 to 80),
                "marrentill" to (28 to 80),
                "tarromin" to (31 to 80),
                "harralander" to (36 to 80),
                "ranarr" to (39 to 80),
                "toadflax" to (43 to 80),
                "irit" to (46 to 80),
                "avantoe" to (50 to 80),
                "kwuarm" to (54 to 80),
                "snapdragon" to (57 to 80),
                "cadantine" to (60 to 80),
                "lantadyme" to (64 to 80),
                "dwarf_weed" to (67 to 80),
                "torstol" to (71 to 80),
            )

        val PROTECTION =
            mapOf(
                "potato" to FarmingProtection(FarmingObjs.compost, 2),
                "onion" to FarmingProtection(FarmingObjs.sackPotatoes, 1),
                "cabbage" to FarmingProtection(FarmingObjs.sackOnions, 1),
                "tomato" to FarmingProtection(FarmingObjs.sackCabbages, 2),
                "sweetcorn" to FarmingProtection(FarmingObjs.jute, 10),
                "strawberry" to FarmingProtection(FarmingObjs.basketApples, 1),
                "watermelon" to FarmingProtection(FarmingObjs.curryLeaf, 10),
                "snape_grass" to FarmingProtection(FarmingObjs.jangerberries, 5),
                "oak" to FarmingProtection(FarmingObjs.basketTomatoes, 1),
                "willow" to FarmingProtection(FarmingObjs.basketApples, 1),
                "maple" to FarmingProtection(FarmingObjs.basketOranges, 1),
                "yew" to FarmingProtection(FarmingObjs.cactusSpine, 10),
                "magic" to FarmingProtection(FarmingObjs.coconut, 25),
                "apple" to FarmingProtection(FarmingObjs.sweetcorn, 9),
                "banana" to FarmingProtection(FarmingObjs.basketApples, 4),
                "orange" to FarmingProtection(FarmingObjs.basketStrawberries, 3),
                "curry" to FarmingProtection(FarmingObjs.basketBananas, 5),
                "pineapple" to FarmingProtection(FarmingObjs.watermelon, 10),
                "papaya" to FarmingProtection(FarmingObjs.pineapple, 10),
                "palm" to FarmingProtection(FarmingObjs.papaya, 15),
                "dragonfruit" to FarmingProtection(FarmingObjs.coconut, 15),
                "teak" to FarmingProtection(FarmingObjs.limpwurt, 15),
                "mahogany" to FarmingProtection(FarmingObjs.yanillian, 25),
                "celastrus" to FarmingProtection(FarmingObjs.potatoCactus, 8),
                "redwood" to FarmingProtection(FarmingObjs.dragonfruit, 6),
                "cactus" to FarmingProtection(FarmingObjs.cadavaberries, 8),
                "potato_cactus" to FarmingProtection(FarmingObjs.snapeGrass, 8),
                "calquat" to FarmingProtection(FarmingObjs.poisonIvyBerries, 8),
                "spirit" to FarmingProtection(FarmingObjs.monkeyNuts, 5),
            )
    }
}
