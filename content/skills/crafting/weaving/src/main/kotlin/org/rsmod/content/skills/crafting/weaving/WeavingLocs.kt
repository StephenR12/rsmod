package org.rsmod.content.skills.crafting.weaving

import org.rsmod.api.type.refs.loc.LocReferences

public typealias weaving_locs = WeavingLocs

public object WeavingLocs : LocReferences() {
    val iorwerth = find("regicide_loom")
    val standard = find("loom")
    val fossilIsland = find("fossil_loom")

    val all = listOf(iorwerth, standard, fossilIsland)
}
