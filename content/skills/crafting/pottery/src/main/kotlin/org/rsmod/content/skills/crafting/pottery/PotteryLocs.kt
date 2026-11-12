package org.rsmod.content.skills.crafting.pottery

import org.rsmod.api.type.refs.loc.LocReferences

public typealias pottery_locs = PotteryLocs

public object PotteryLocs : LocReferences() {
    val vikingWheel = find("viking_potterywheel")
    val standardWheel = find("potterywheel")
    val contactWheel = find("contact_potterywheel")
    val darkmeyerWheel = find("darkm_poor_potterywheel")
    val soulWarsFixedWheel = find("sw_potterywheel_fixed")
    val soulWarsWheel = find("sw_potterywheel")
    val aldarinWheel = find("potterywheel_2")

    val wheels =
        listOf(
            vikingWheel,
            standardWheel,
            contactWheel,
            darkmeyerWheel,
            soulWarsFixedWheel,
            soulWarsWheel,
            aldarinWheel,
        )

    val vikingOven = find("viking_potteryoven")
    val barbarianOven = find("fai_barbarian_pottery_oven")
    val standardOven = find("potteryoven")
    val darkmeyerOven = find("darkm_poor_pottery_oven")

    val ovens = listOf(vikingOven, barbarianOven, standardOven, darkmeyerOven)
}
