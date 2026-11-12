package org.rsmod.content.skills.crafting.spinning

import org.rsmod.api.type.refs.loc.LocReferences

public typealias spinning_locs = SpinningLocs

public object SpinningLocs : LocReferences() {
    val viking = find("viking_spinningwheel")
    val elf_village = find("elf_village_spinning_wheel")
    val standard = find("spinningwheel")
    val contact = find("contact_spinning_wheel")
    val neitiznot = find("iznot_spinning_wheel")
    val kourend = find("kr_spinningwheel")
    val fossil_island = find("fossil_spinning_wheel")
    val soul_wars_fixed = find("sw_spinningwheel_fixed")
    val soul_wars = find("sw_spinningwheel")
    val quetzacali = find("spinningwheel_quetzacali")
    val aldarin = find("spinningwheel_2")

    val all =
        listOf(
            viking,
            elf_village,
            standard,
            contact,
            neitiznot,
            kourend,
            fossil_island,
            soul_wars_fixed,
            soul_wars,
            quetzacali,
            aldarin,
        )
}
