package org.rsmod.content.skills.farming

import org.rsmod.api.config.refs.stats
import org.rsmod.api.script.onArea
import org.rsmod.api.type.builders.area.AreaBuilder
import org.rsmod.api.type.builders.map.area.MapAreaBuilder
import org.rsmod.api.type.refs.area.AreaReferences
import org.rsmod.game.area.polygon.VertexCoord
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GuildWingAreas : AreaReferences() {
    val west = find("farming_guild_west_barrier")
    val north = find("farming_guild_north_barrier")
}

internal object GuildWingAreaBuilder : AreaBuilder() {
    init {
        build("farming_guild_west_barrier")
        build("farming_guild_north_barrier")
    }
}

internal object FarmingGuildBarriers : MapAreaBuilder() {
    override fun onPackMapTask() {
        // The cache's inner gates stay open. Crossing the two-tile threshold checks the skill.
        area(GuildWingAreas.west) {
            polygon {
                for ((x, z) in
                    listOf(1241 to 3729, 1243 to 3729, 1243 to 3731, 1241 to 3731)) vertex(
                    VertexCoord.from(CoordGrid(x, z))
                )
            }
        }
        area(GuildWingAreas.north) {
            polygon {
                for ((x, z) in
                    listOf(1248 to 3745, 1250 to 3745, 1250 to 3747, 1248 to 3747)) vertex(
                    VertexCoord.from(CoordGrid(x, z))
                )
            }
        }
    }
}

public class FarmingGuildWings : PluginScript() {
    override fun ScriptContext.startup() {
        onArea(GuildWingAreas.west) {
            if (!player.members || stat(stats.farming) < 65) {
                telejump(CoordGrid(1244, coords.z.coerceIn(3729, 3730)))
                mes("You need 65 Farming to enter the western wing.")
            }
        }
        onArea(GuildWingAreas.north) {
            if (!player.members || stat(stats.farming) < 85) {
                telejump(CoordGrid(coords.x.coerceIn(1248, 1249), 3743))
                mes("You need 85 Farming to enter the northern wing.")
            }
        }
    }
}
