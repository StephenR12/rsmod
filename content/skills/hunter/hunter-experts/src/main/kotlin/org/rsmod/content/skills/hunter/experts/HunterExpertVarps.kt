package org.rsmod.content.skills.hunter.experts

import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences

/** Permanent, server-only state shared by both Poll 85 hunting experts. */
public object HunterExpertVarps : VarpReferences() {
    public val receivedHunterKit = find("hunter_expert_received_hunter_kit")
}

internal object HunterExpertVarpBuilder : VarpBuilder() {
    init {
        build("hunter_expert_received_hunter_kit") { transmitNever = true }
    }
}
