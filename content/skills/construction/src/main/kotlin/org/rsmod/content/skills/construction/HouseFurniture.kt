package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.game.type.stat.UnpackedStatType

internal data class HouseRecipe(
    val name: String,
    val requirements: List<Pair<UnpackedStatType, Int>>,
    val materials: List<Pair<Int, Int>>,
    val xp: Int,
    val location: String,
    val upgradeFrom: Int = 0,
    val plant: Boolean = false,
    val farmingXp: Int = 0,
    val returnedOnRemove: Int? = null,
) {
    val level: Int
        get() = requirements.first().second
}

@Singleton
internal class HouseFurniture @Inject constructor(rows: DbRowTypeList, stats: StatTypeList) {
    private val rowTypes = rows.values.associateBy { it.internalName }
    private val groups = mutableMapOf<Pair<HouseRoomType, Int>, List<HouseRecipe>>()
    val builtNames = mutableSetOf<String>()

    init {
        fun group(
            room: HouseRoomType,
            slot: Int,
            row: String,
            loc: String,
            xp: List<Int>,
            names: List<String> = emptyList(),
            upgradeFrom: Map<Int, Int> = emptyMap(),
            locations: List<String> = emptyList(),
            plant: Boolean = false,
            farmingXp: List<Int> = emptyList(),
            append: Boolean = false,
            returnMaterial: Boolean = false,
        ) {
            val existing = if (append) groups[room to slot].orEmpty() else emptyList()
            groups[room to slot] = existing +
                xp.mapIndexed { index, experience ->
                    val variant = index + 1
                    val name = names.getOrNull(index) ?: row.replace("#", variant.toString())
                    val entry = rowTypes.getValue("poh_$name")
                    val requirements =
                        entry.data.getValue(3).chunked(2).map {
                            stats[it[0] as Int]!! to it[1] as Int
                        }
                    val materials =
                        entry.data.getValue(2).chunked(2).map { it[0] as Int to it[1] as Int }
                    val location =
                        "poh_" +
                            (locations.getOrNull(index) ?: loc.replace("#", variant.toString()))
                    HouseRecipe(
                        entry.data.getValue(1).single() as String,
                        requirements,
                        materials,
                        experience,
                        location,
                        upgradeFrom[variant] ?: 0,
                        plant,
                        farmingXp.getOrElse(index) { 0 },
                        if (returnMaterial) materials.single().first else null,
                    )
                }
        }
        for (fixture in CostumeFurniture.entries) {
            val rows = fixture.materials.map { "cos_room_${fixture.rowPrefix}_$it" }
            val locations = rows.map { it.replace(fixture.rowPrefix, fixture.locPrefix).replace("teak_carved", "carved_teak") }
            group(HouseRoomType.CostumeRoom, fixture.slot, "", "", fixture.xp, names = rows, locations = locations,
                upgradeFrom = if (fixture == CostumeFurniture.Toy) emptyMap() else (2..rows.size).associateWith { it - 1 })
        }
        for (hall in HouseRoomType.entries.filter { it.stairHall }) {
            group(hall, 1, "", "", listOf(680,980,1040,3200,4400),
                names=listOf("stairs_oak_up","stairs_teak_up","spiralstairs_limestone","stairs_marble_up","spiralstairs_marble"),
                locations=listOf("stairs_3","stairs_4","spiralstairs","stairs_5","spiralstairs_2"))
        }
        val garden = HouseRoomType.Garden
        val treeXp = listOf(31, 44, 70, 100, 122, 141, 223)
        group(garden, 2, "tree_#", "big_tree#_4", treeXp, plant = true, farmingXp = treeXp)
        group(garden, 3, "tree_#", "small_tree#_5", treeXp, plant = true, farmingXp = treeXp)
        for ((index, plantName) in
            listOf("plantbig1", "plantbig2", "plantsmall1", "plantsmall2").withIndex()) {
            val xp = listOf(31, 70, 100)
            val rows = ('a'..'c').map { plantName + it }
            val locations = rows.map { it.replace("plantsmall", "plantbsmall") }
            group(
                garden,
                4 + index,
                "",
                "",
                xp,
                names = rows,
                locations = locations,
                plant = true,
                farmingXp = xp,
            )
        }
        group(
            HouseRoomType.SuperiorGarden,
            1,
            "",
            "",
            listOf(350, 535, 885),
            names =
                listOf(
                    "superior_garden_teleport_tree",
                    "superior_garden_teleport_ring",
                    "superior_garden_teleport_treering",
                ),
            locations = listOf("spirit_tree", "fairy_ring", "spirit_ring"),
            plant = true,
            farmingXp = listOf(350, 0, 885),
        )
        val poolTypes = listOf("restoration", "revitalisation", "rejuvenation", "recovery", "regeneration")
        group(
            HouseRoomType.SuperiorGarden, 3, "", "", listOf(706,850,900,1950,3107),
            names=poolTypes.map { "superior_garden_pool_$it" },
            locations=poolTypes.map { "pool_$it" },
            upgradeFrom=(2..5).associateWith { it-1 },
        )
        group(
            HouseRoomType.QuestHall,
            3,
            "landscape_#",
            "",
            listOf(314, 314, 314, 464, 464),
            locations =
                listOf(
                    "landscape_lumbridge_1",
                    "landscape_desert_1",
                    "landscape_morytania_1",
                    "landscape_karamja_1",
                    "landscape_istafar_1",
                ),
        )
        group(
            HouseRoomType.SkillHall,
            2,
            "",
            "",
            listOf(180, 280, 600),
            names =
                listOf(
                    "trophy_head_blank_teak",
                    "trophy_head_blank_mahogany",
                    "trophy_head_blank_gilded",
                ),
            locations =
                listOf(
                    "trophy_head_blank_teak",
                    "trophy_head_blank_mahogany",
                    "trophy_head_blank_gilded",
                ),
        )
        for (slot in 1..3) group(
            HouseRoomType.PortalChamber,
            slot,
            "portal_frame_#",
            "",
            listOf(270, 420, 1500),
            locations = listOf("portal_teak_empty", "portal_mag_empty", "portal_marble_empty"),
        )
        group(
            HouseRoomType.PortalChamber,
            7,
            "teleport_centrepiece_#",
            "",
            listOf(40, 500, 2000),
            locations = listOf("teleport_centrepiece", "teleport_centrepiece_grand", "scrying_pool"),
        )
        for(room in listOf(HouseRoomType.Menagerie,HouseRoomType.IndoorMenagerie)){
            group(room,1,"menagerie_pethouse_#","menagerie_pethouse_#",listOf(240,360,560,1560,160,158),
                upgradeFrom=(2..6).associateWith{it-1})
            group(room,2,"menagerie_scratchingpost_#","menagerie_scratchingpost_#",listOf(124,204,304))
            group(room,3,"menagerie_combatring_#","menagerie_combatring_#",listOf(139,199,299))
            group(room,4,"menagerie_petlist_#","menagerie_petlist_#",listOf(198))
            group(room,5,"menagerie_petfeeder_#","menagerie_petfeeder_#",listOf(182,272,862))
            if(room==HouseRoomType.Menagerie)group(room,6,"menagerie_habitat_#","menagerie_habitat_feature_#",listOf(37,51,181,271,46))
        }
        val chairs = listOf(58, 87, 87, 120, 180, 180, 280)
        val rugs = listOf(30, 60, 360)
        val fireplaces = listOf(30, 40, 500)
        val curtains = listOf(132, 225, 315)
        val bookcases = listOf(115, 180, 420)
        val parlour = HouseRoomType.Parlour
        for (slot in 1..3) group(parlour, slot, "armchair_#", "chair#", chairs)
        group(parlour, 4, "rug_#", "rug@#", rugs)
        group(parlour, 5, "bookcase_#", "bookcase#", bookcases)
        group(parlour, 6, "fireplace_#", "fireplace_#", fireplaces)
        group(parlour, 7, "curtains_#", "curtains_#", curtains)

        val kitchen = HouseRoomType.Kitchen
        group(kitchen, 1, "stove_#", "stove_#", listOf(40, 60, 80, 80, 100, 120, 160))
        group(
            kitchen,
            2,
            "kitchen_shelves_#",
            "kitchen_shelves_#",
            listOf(87, 147, 147, 240, 240, 330, 930),
        )
        group(kitchen, 3, "barrel_#", "barrel_#", listOf(87, 91, 184, 184, 224, 224))
        group(kitchen, 4, "cat_basket_#", "pet_#", listOf(15, 58, 58))
        group(kitchen, 5, "larder_#", "larder_#", listOf(228, 480, 750))
        group(kitchen, 6, "sink_#", "sink_#", listOf(100, 200, 300, 11144))
        group(kitchen, 7, "kitchentable_#", "kitchentable_#", listOf(87, 180, 270))

        val dining = HouseRoomType.DiningRoom
        group(
            dining,
            1,
            "dining_table_#",
            "diningtable_#",
            listOf(115, 240, 360, 360, 600, 840, 3100),
        )
        for (slot in 2..3) group(
            dining,
            slot,
            "dining_chairs_#",
            "diningchairs_#",
            listOf(115, 240, 240, 360, 360, 560, 1760),
        )
        group(dining, 4, "fireplace_#", "fireplace_#", fireplaces)
        group(dining, 5, "curtains_#", "curtains_#", curtains)
        group(dining, 6, "wall_crest_#", "wall_deco_#", listOf(120, 180, 1020))
        group(dining, 7, "bellpull_#", "bellpull_#", listOf(64, 120, 420))

        val bed = HouseRoomType.Bedroom
        group(bed, 1, "bed_#", "bed_#", listOf(117, 210, 330, 300, 480, 450, 1330))
        group(bed, 2, "wardrobe_#", "wardrobe_#", listOf(58, 120, 180, 180, 270, 420, 720))
        group(bed, 3, "mirror_#", "mirror_#", listOf(30, 61, 121, 181, 182, 281, 582))
        group(bed, 4, "curtains_#", "curtains_#", curtains)
        group(bed, 5, "rug_#", "rug@#", rugs)
        group(bed, 6, "fireplace_#", "fireplace_#", fireplaces)
        group(bed, 7, "clock_#", "clock_#", listOf(142, 202, 602))

        val study = HouseRoomType.Study
        group(study, 1, "lectern_#", "lectern_#", listOf(60, 120, 120, 180, 180, 580, 580, 1800))
        group(study, 2, "globe_#", "globe_#", listOf(180, 270, 570, 570, 960, 1320, 1420))
        group(study, 4, "crystalball_#", "crystalball_#", listOf(280, 580, 890))
        group(study, 5, "wallchart_#", "wallchart_#", listOf(30, 45, 60))
        group(study, 6, "telescope_#", "telescope_#", listOf(121, 181, 580))
        group(study, 7, "bookcase_#", "bookcase#", bookcases)

        val workshop = HouseRoomType.Workshop
        group(
            workshop,
            1,
            "workbench_#",
            "workbench_#",
            listOf(143, 300, 440, 140, 140),
            upgradeFrom = mapOf(4 to 3, 5 to 4),
        )
        group(
            workshop,
            2,
            "crafting_table_#",
            "clockmaking_#",
            listOf(240, 1, 2, 120),
            upgradeFrom = mapOf(2 to 1, 3 to 2, 4 to 3),
        )
        group(
            workshop,
            3,
            "tool_store_#",
            "tools#",
            listOf(120, 120, 120, 120, 120),
            upgradeFrom = mapOf(2 to 1, 3 to 2, 4 to 3, 5 to 4),
        )
        group(workshop, 4, "repair_bench_#", "repair_#", listOf(120, 260, 500))

        val chapel = HouseRoomType.Chapel
        group(
            chapel,
            2,
            "",
            "altar_saradomin_#",
            listOf(240, 360, 390, 590, 910, 1030, 2230),
            names =
                listOf(
                    "altar_oak",
                    "altar_teak",
                    "altar_teak_cloth",
                    "altar_mahogany",
                    "altar_limestone",
                    "altar_marble",
                    "altar_marble_gilt",
                ),
        )
        group(
            chapel,
            3,
            "",
            "torch_#",
            listOf(40, 58, 124, 46, 280, 600, 1600),
            names =
                listOf(
                    "torches_steel",
                    "torches_wooden",
                    "candlesticks_steel",
                    "candlesticks_gilt",
                    "incense_burner_oak",
                    "incense_burner_mahogany",
                    "incense_burner_marble",
                ),
        )
        group(chapel, 5, "rug_#", "rug@#", rugs)
        group(
            chapel,
            7,
            "",
            "musical_thing_#",
            listOf(323, 480, 680),
            names = listOf("windchimes", "bells", "organ"),
        )

        // These revision-233 furnishings are portable. Preserve existing saved variant numbers.
        for ((room,slot) in listOf(garden to 1,garden to 4,garden to 5,HouseRoomType.FormalGarden to 1)) {
            val rotation=when(slot){4->2;5->3;else->0}
            group(room,slot,"greenman_statue","greenman_statue_rot$rotation",listOf(0),append=true,returnMaterial=true)
        }
        for ((room,slot) in listOf(dining to 6,HouseRoomType.CombatRoom to 5,HouseRoomType.ThroneRoom to 8)) {
            group(room,slot,"greenman_wall_decoration","greenman_wall_decoration",listOf(0),append=true,returnMaterial=true)
        }
        for (recipes in groups.values) for (recipe in recipes) {
            if ('@' in recipe.location) {
                for (part in listOf("corner", "middle", "side")) builtNames +=
                    recipe.location.replace("@", part)
            } else {
                builtNames += recipe.location
                if ("kitchen_shelves" in recipe.location)
                    builtNames += recipe.location.replace("shelves", "crockery")
            }
        }
    }

    fun recipes(room: HouseRoomType, slot: Int): List<HouseRecipe> = groups[room to slot].orEmpty()

    fun built(recipe: HouseRecipe, hotspot: String, variant: Int): String? =
        when {
            hotspot=="poh_menagerie_combatring_mat_hotspot"->"poh_menagerie_combatring_mat"
            hotspot.startsWith("poh_menagerie_habitat_ground_")->"${hotspot}_$variant"
            hotspot.endsWith("_stool") -> if (variant >= 3) "poh_stool_2" else "poh_stool_1"
            hotspot.startsWith("poh_workshop_3") -> {
                val tier = hotspot.last() - 'a' + 1
                "poh_tools$tier".takeIf { tier <= variant }
            }
            hotspot.endsWith("_crockery") -> recipe.location.replace("shelves", "crockery")
            else -> recipe.location.replace("@", hotspot.substringAfterLast('_'))
        }
}
