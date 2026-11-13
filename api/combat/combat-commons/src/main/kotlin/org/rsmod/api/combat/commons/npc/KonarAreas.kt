package org.rsmod.api.combat.commons.npc

import org.rsmod.map.CoordGrid

/** The Key Master's lobby and three Cerberus chambers occupy four map squares. */
public fun CoordGrid.inCerberusLair(): Boolean = level == 0 &&
    ((x in 1216..1407 && z in 1216..1279) || (x in 1280..1343 && z in 1280..1343))

/** Location ids from slayer_area (DB 115), in physical world coordinates. */
public fun inKonarArea(id: Int, tile: CoordGrid): Boolean = with(tile) {
    when (id) {
        // Catacombs of Kourend
        1 -> (level == 0 && x in 1584..1727 && z in 9952..10111) ||
            (level == 0 && x in 2260..2295 && z in 5652..5687)
        // Stronghold Slayer Dungeon
        2 -> (level == 0 && x in 2368..2495 && z in 9728..9855)
        // Karuulm Slayer Dungeon
        3 -> (level in 0..3 && x in 1216..1375 && z in 10112..10287)
        // Chasm of Fire
        4 -> (level in 0..2 && x in 1408..1535 && z in 10048..10111)
        // Brimhaven Dungeon
        5 -> (level == 2 && x in 2624..2647 && z in 9472..9511) ||
            (level == 2 && x in 2624..2647 && z in 9536..9591) ||
            (level == 0 && x in 2632..2743 && z in 9416..9599)
        // Taverley Dungeon
        6 -> inCerberusLair() || (level == 1 && x in 2768..2831 && z in 9792..9855) ||
            (level in 0..1 && x in 2816..2983 && z in 9664..9863)
        // Witchaven Dungeon
        7 -> (level == 0 && x in 2688..2703 && z in 9680..9695) ||
            (level == 0 && x in 2728..2751 && z in 9672..9703)
        // Waterfall Dungeon
        8 -> (level == 0 && x in 2552..2600 && z in 9872..9911)
        // Slayer Tower
        9 -> (level in 0..2 && x in 3392..3455 && z in 3520..3583) ||
            (level == 3 && x in 3392..3455 && z in 9920..9983)
        // God Wars Dungeon
        10 -> (level in 0..3 && x in 2816..2975 && z in 5184..5375)
        // Kalphite Lair
        11 -> (level in 0..2 && x in 3456..3519 && z in 9472..9535)
        // task-only Kalphite Cave
        12 -> (level == 0 && x in 3264..3343 && z in 9472..9543)
        // Kraken Cove
        13 -> (level == 0 && x in 2240..2303 && z in 9984..10047)
        // in the Lighthouse
        14 -> (level == 0 && x in 2504..2535 && z in 10008..10039)
        // Waterbirth Island
        15 -> (level in 0..3 && x in 1792..1951 && z in 4352..4479) ||
            (level == 0 && x in 2432..2559 && z in 10112..10175) ||
            (level == 0 && x in 2480..2559 && z in 3712..3783) ||
            (level == 0 && x in 2880..2943 && z in 4416..4479)
        // Lizardman Canyon
        16 -> (level == 0 && x in 1424..1543 && z in 3680..3735)
        // Molch
        17 -> (level == 0 && x in 1280..1335 && z in 3656..3695) ||
            (level == 0 && x in 1288..1335 && z in 10088..10103)
        // Lizardman Settlement
        18 -> (level == 0 && x in 1280..1335 && z in 9944..9975) ||
            (level == 0 && x in 1304..1343 && z in 3536..3567)
        // Smoke Devil Dungeon
        19 -> (level == 0 && x in 2344..2423 && z in 9416..9471)
        // Smoke Dungeon
        20 -> (level == 0 && x in 3176..3327 && z in 9344..9407)
        // Death Plateau
        21 -> (level == 0 && x in 2848..2879 && z in 3584..3607)
        // Troll Stronghold
        22 -> (level == 2 && x in 2816..2855 && z in 10048..10111) ||
            (level == 1 && x in 2824..2839 && z in 10072..10103) ||
            (level == 0 && x in 2840..2863 && z in 3672..3687)
        // Keldagrim
        23 -> (level == 0 && x in 2768..2791 && z in 10128..10151)
        // South of Mount Quidamortem
        24 -> (level == 0 && x in 1192..1247 && z in 3488..3599)
        // Fremennik Isles
        25 -> (level == 0 && x in 2312..2415 && z in 3824..3895) ||
            (level == 0 && x in 2376..2423 && z in 10208..10231)
        // Fremennik Slayer Dungeon
        26 -> (level == 0 && x in 2688..2727 && z in 9952..9983) ||
            (level == 0 && x in 2688..2815 && z in 9984..10047)
        // Myths' Guild Dungeon
        27 -> (level in 0..1 && x in 1888..1951 && z in 8960..9023)
        // Mourner Tunnels
        28 -> (level == 0 && x in 1984..2039 && z in 4632..4671)
        // Lithkren Vault
        29 -> (level == 0 && x in 1536..1591 && z in 5064..5087)
        // Ancient Cavern
        30 -> (level in 0..1 && x in 1600..1791 && z in 5312..5375)
        // Stronghold of Security
        31 -> (level == 0 && x in 2112..2175 && z in 5248..5311) ||
            (level == 0 && x in 2312..2367 && z in 5192..5247)
        // Fossil Island
        32 -> (level == 0 && x in 3592..3647 && z in 10184..10295) ||
            (level == 0 && x in 3672..3703 && z in 3816..3895) ||
            (level == 0 && x in 3800..3823 && z in 3752..3767)
        // Asgarnian Ice Dungeon
        33 -> (level == 0 && x in 2944..3087 && z in 9536..9599)
        // Ogre Enclave
        34 -> (level == 0 && x in 2560..2623 && z in 9416..9463)
        // Brine Rat Cavern
        35 -> (level == 0 && x in 2696..2751 && z in 10128..10175)
        // Zanaris
        36 -> (level == 0 && x in 2368..2431 && z in 4352..4543)
        // Evil Chicken's Lair
        37 -> (level == 0 && x in 2432..2463 && z in 4352..4399)
        // The Abyss
        38 -> (level == 0 && x in 2944..3135 && z in 4736..4927) ||
            (level == 1 && x in 2976..3071 && z in 4800..4863)
        // Kebos Swamp
        39 -> (level == 0 && x in 1280..1319 && z in 3608..3639)
        // The Battlefront
        40 -> (level == 0 && x in 1328..1399 && z in 3680..3727)
        // Forthos Dungeon
        41 -> (level == 0 && x in 1808..1831 && z in 9920..9943)
        // Iorwerth Dungeon
        42 -> (level == 0 && x in 3152..3263 && z in 12352..12479)
        // Jormungand's Prison
        43 -> (level == 0 && x in 2400..2487 && z in 10368..10463)
        // Darkmeyer
        44 -> (level == 0 && x in 3584..3667 && z in 3320..3420)
        // Slepe
        45 -> (level == 0 && x in 3692..3783 && z in 3296..3415) ||
            (level == 3 && x in 3712..3775 && z in 3264..3359)
        // Meiyerditch Laboratories
        46 -> (level == 0 && x in 3560..3623 && z in 9728..9783)
        // Isle of Souls
        47 -> (level == 0 && x in 2112..2303 && z in 2752..3023) ||
            (level == 0 && x in 2112..2175 && z in 9280..9343)
        // Giants' Den
        48 -> (level == 0 && x in 1416..1463 && z in 9856..9911)
        // Poison Waste Dungeon
        49 -> (level == 0 && x in 1352..1359 && z in 4232..4239) ||
            (level == 1 && x in 1360..1367 && z in 4232..4239) ||
            (level == 1 && x in 1472..1535 && z in 4224..4343) ||
            (level == 0 && x in 1472..1535 && z in 4224..4287)
        // Neypotzli
        50 -> (level == 0 && x in 1280..1407 && z in 9536..9727)
        // Tapoyauik
        51 -> (level in 0..2 && x in 1344..1407 && z in 4480..4543) ||
            (level in 0..2 && x in 1568..1727 && z in 9600..9663)
        // Crypt of Tonali
        52 -> (level in 0..3 && x in 1216..1343 && z in 9728..9855) ||
            (level in 0..3 && x in 1280..1407 && z in 9280..9407)
        else -> false
    }
}
