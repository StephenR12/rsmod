package org.rsmod.api.config.builders

import org.rsmod.api.type.builders.area.AreaBuilder

internal object AreaBuilds : AreaBuilder() {
    init {
        build("woodcutting_guild")
        build("lumbridge")
        build("singles_plus")
        build("multiway")
    }
}
