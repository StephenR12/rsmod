package org.rsmod.content.travel.mausoleumbridge

import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences

public object MausoleumBridgeVarps : VarpReferences() {
    public val repairState = find("mausoleum_bridge_repair_state")
}

internal object MausoleumBridgeVarpBuilder : VarpBuilder() {
    init {
        build("mausoleum_bridge_repair_state") { transmitNever = true }
    }
}
