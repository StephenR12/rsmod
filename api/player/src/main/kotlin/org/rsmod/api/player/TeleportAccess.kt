package org.rsmod.api.player

import org.rsmod.api.config.refs.varbits
import org.rsmod.game.entity.Player

/** The last 100 Tele Block cycles are immunity after the block expires. */
public fun Player.isTeleportBlocked(): Boolean = activityBlocksTeleport || vars[varbits.teleblock_cycles] > 100
