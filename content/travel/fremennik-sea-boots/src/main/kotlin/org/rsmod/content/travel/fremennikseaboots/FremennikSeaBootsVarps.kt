package org.rsmod.content.travel.fremennikseaboots

import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences

public object FremennikSeaBootsVarps : VarpReferences() {
    public val teleports_used = find("fremennik_sea_boots_teleports_used")
    public val last_teleport_day = find("fremennik_sea_boots_last_teleport_day")
}

internal object FremennikSeaBootsVarpBuilder : VarpBuilder() {
    init {
        build("fremennik_sea_boots_teleports_used") { transmitNever = true }
        build("fremennik_sea_boots_last_teleport_day") { transmitNever = true }
    }
}
