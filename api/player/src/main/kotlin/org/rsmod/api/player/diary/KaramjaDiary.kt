package org.rsmod.api.player.diary

internal val karamjaTasks =
    mapOf(
        DiaryDifficulty.Easy to
            listOf(
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_banana,
                    5,
                    "Pick 5 bananas from the plantation located east of the volcano.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_swing,
                    1,
                    "Use the rope swing to travel to the Moss Giant Island north-west of Karamja.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_gold,
                    1,
                    "Mine some gold from the rocks on the north-west peninsula of Karamja.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_boat_sarim,
                    1,
                    "Travel to Port Sarim via the dock, east of Musa Point.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_boat_ardy,
                    1,
                    "Travel to Ardougne via the port near Brimhaven.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_cairn,
                    1,
                    "Explore Cairn Isle to the west of Karamja.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_fishing,
                    1,
                    "Use the fishing spots north of the banana plantation (any one of them).",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_seaweed,
                    5,
                    "Collect 5 seaweed from anywhere on Karamja.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_tzhaar,
                    1,
                    "Attempt the TzHaar Fight Pits or Fight Cave.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_easy_jogre,
                    1,
                    "Kill a jogre in the Pothole Dungeon.",
                ),
            ),
        DiaryDifficulty.Medium to
            listOf(
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_agility,
                    1,
                    "Claim a ticket from the Agility arena in Brimhaven.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_volcano,
                    1,
                    "Discover hidden wall in the dungeon below the volcano.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_crandor,
                    1,
                    "Visit the Isle of Crandor via the dungeon below the volcano.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_cart,
                    1,
                    "Use Vigroy and Hajedy's cart service.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_cleanup,
                    1,
                    "Earn 100% favour in the Tai Bwo Wannai Cleanup minigame.",
                ),
                DiaryEntry.Counter(DiaryBits.atjun_med_spider, 1, "Cook a Spider on stick."),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_topaz,
                    1,
                    "Charter the Lady of the Waves from south of Cairn Isle to Port Khazard",
                ),
                DiaryEntry.Counter(DiaryBits.atjun_med_teak, 1, "Cut a log from a teak tree."),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_mahogany,
                    1,
                    "Cut a log from a mahogany tree.",
                ),
                DiaryEntry.Counter(DiaryBits.atjun_med_karambwan, 1, "Catch a karambwan."),
                DiaryEntry.Counter(DiaryBits.atjun_med_machette, 1, "Exchange gems for a machete."),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_glider,
                    1,
                    "Use the gnome glider to travel to Karamja.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_farming,
                    1,
                    "Grow a healthy fruit tree in the patch near Brimhaven.",
                ),
                DiaryEntry.Counter(DiaryBits.atjun_med_graahk, 1, "Trap a Horned Graahk."),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_shilo_vines,
                    1,
                    "Chop the vines to gain deeper access to Brimhaven Dungeon (immediately inside the northern entrance).",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_shilo_lava,
                    1,
                    "Cross the lava using the stepping stones within Brimhaven Dungeon (south-west of pipe squeeze with moss giants).",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_shilo_stairs,
                    1,
                    "Climb the stairs within Brimhaven Dungeon.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_khazard,
                    1,
                    "Charter a ship from the shipyard in the far east of Karamja.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_med_charter,
                    1,
                    "Mine a red topaz from a gem rock.",
                ),
            ),
        DiaryDifficulty.Hard to
            listOf(
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_fightpits,
                    1,
                    "Become the champion of the Fight Pit.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_fightcave,
                    1,
                    "Kill a Ket-Zek in the Fight Caves.",
                ),
                DiaryEntry.Counter(DiaryBits.atjun_hard_oomlie, 1, "Eat an oomlie wrap."),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_nature,
                    1,
                    "Craft some nature runes from Essence.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_karambwan,
                    1,
                    "Cook a raw karambwan thoroughly.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_deathwing,
                    1,
                    "Kill a death wing in the dungeon under the Kharazi Jungle.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_xbow,
                    1,
                    "Use the crossbow shortcut south of the volcano.",
                ),
                DiaryEntry.Counter(DiaryBits.atjun_hard_palm, 5, "Collect 5 palm leaves."),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_duradel,
                    1,
                    "Be assigned a Slayer task by the Slayer Master in Shilo Village.",
                ),
                DiaryEntry.Counter(
                    DiaryBits.atjun_hard_dragon,
                    1,
                    "Kill a metal dragon in Brimhaven Dungeon.",
                ),
            ),
        DiaryDifficulty.Elite to
            listOf(
                DiaryEntry.Flag(
                    DiaryPages.atjun_tasks_4,
                    1,
                    "Craft 56 nature runes simultaneously from Essence without the use of Extracts.",
                ),
                DiaryEntry.Flag(
                    DiaryPages.atjun_tasks_4,
                    2,
                    "Equip a fire cape or infernal cape in the TzHaar City.",
                ),
                DiaryEntry.Flag(
                    DiaryPages.atjun_tasks_4,
                    3,
                    "Check the health of a palm tree in Brimhaven.",
                ),
                DiaryEntry.Flag(
                    DiaryPages.atjun_tasks_4,
                    4,
                    "Create an antivenom potion whilst standing in the horse shoe mine.",
                ),
                DiaryEntry.Flag(
                    DiaryPages.atjun_tasks_4,
                    5,
                    "Check the health of your Calquat tree patch.",
                ),
            ),
    )
