package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

/** Completes a spell teleport and publishes its gameplay-semantic cancellation event. */
public fun ProtectedAccess.spellTeleport(destination: CoordGrid) {
    resetAnim()
    resetSpotanim()
    teleport(destination)
    publish(PlayerTeleportEvent.Start(player, destination))
}
