package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.zone.header.UpdateZonePartialFollows
import org.rsmod.api.registry.zone.ZoneUpdateTransformer
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey

/** Stores and, when visible, immediately sends a location override seen only by this player. */
public fun Player.privateLocChange(loc: LocInfo) {
    val zone = ZoneKey.from(loc.coords)
    val grid = ZoneGrid.from(loc.coords)
    privateLocs[zone, LocZoneKey(grid, loc.layer)] = loc.entity
    if (!visibleZoneKeys.contains(zone.packed)) {
        return
    }
    val zoneBase = zone.toCoords()
    client.write(
        UpdateZonePartialFollows(zoneBase.x - buildArea.x, zoneBase.z - buildArea.z, zoneBase.level)
    )
    client.write(ZoneUpdateTransformer.toLocAddChangeProt(loc))
}
