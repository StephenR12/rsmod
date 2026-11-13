package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** A PvP activity can deny entry to outsiders or allow multiway combat between participants. */
public data class PlayerCombatPermission(
    public val attacker: Player,
    public val target: Player,
    public var allowed: Boolean? = null,
) : UnboundEvent


/** Whether a secondary area hit may strike another player, including activity-specific PvP arenas. */
public fun Player.canHitNearbyPlayer(target: Player, bus: org.rsmod.events.EventBus): Boolean {
    if(this===target) return false
    val permission=PlayerCombatPermission(this,target)
    bus.publish(permission)
    permission.allowed?.let { return it }
    val wilderness=org.rsmod.api.config.refs.varbits.inside_wilderness
    if(vars[wilderness]==0 || target.vars[wilderness]==0)return false
    val base=if(coords.z>=9920)9920 else 3520
    val level=(minOf(coords.z,target.coords.z)-base)/8+1
    return kotlin.math.abs(combatLevel-target.combatLevel)<=level
}
