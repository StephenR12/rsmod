package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.type.refs.loc.LocReferences

public typealias orb_charging_locs = OrbChargingLocs

public object OrbChargingLocs : LocReferences() {
    val earth = find("obelisk_earth")
    val water = find("obelisk_water")
    val air = find("obelisk_air")
    val fire = find("obelisk_fire")
}
