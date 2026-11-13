package org.rsmod.api.player

import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

public fun ProtectedAccess.canTravelWithItem(maxWilderness:Int = 20, membersOnly:Boolean = true):Boolean {
    val level = (coords.z - if (coords.z >= 9920) 9920 else 3520) / 8 + 1
    if ((membersOnly && !player.members) || player.isTeleportBlocked() || (vars[varbits.inside_wilderness] != 0 && level > maxWilderness)) {
        mes("A magical force prevents the teleport.")
        return false
    }
    return true
}

public suspend fun ProtectedAccess.travelWithItem(destination:CoordGrid, maxWilderness:Int = 20, membersOnly:Boolean = true, pay:() -> Boolean): Boolean {
    if (!canTravelWithItem(maxWilderness,membersOnly)) return false
    stopAction()
    anim(seqs.teleport_casting)
    spotanim(spotanims.teleport_casting)
    soundSynth(synths.teleport_all)
    delay(4)
    if (!pay()) return false
    teleport(destination)
    publish(PlayerTeleportEvent.Start(player,destination))
    return true
}
