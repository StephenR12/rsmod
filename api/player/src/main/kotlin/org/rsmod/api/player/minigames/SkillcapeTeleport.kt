package org.rsmod.api.player.minigames

import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

/** Common restrictions and casting sequence for unlimited skillcape teleports. */
public suspend fun ProtectedAccess.skillcapeTeleport(destination: () -> CoordGrid?) {
    if (!player.members) return
    val base = if (coords.z >= 9920) 9920 else 3520
    if (
        player.isTeleportBlocked() ||
            (vars[varbits.inside_wilderness] != 0 && (coords.z - base) / 8 + 1 > 20)
    ) {
        mes("A magical force stops you from teleporting.")
        return
    }
    val tile = destination() ?: return
    stopAction()
    anim(seqs.teleport_casting)
    spotanim(spotanims.teleport_casting)
    soundSynth(synths.teleport_all)
    delay(4)
    teleport(tile)
    publish(PlayerTeleportEvent.Start(player, tile))
}
