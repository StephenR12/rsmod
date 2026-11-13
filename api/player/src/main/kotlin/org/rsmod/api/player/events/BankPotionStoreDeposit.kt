package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/** Optional atomic potion-store interception before the ordinary bank transaction. */
public class BankPotionStoreDeposit(
    public val player: Player,
    public val inventory: Inventory,
    public val slot: Int,
    public val count: Int,
) : UnboundEvent {
    public var handled: Boolean = false
    public var deposited: Int = 0
}

/** Published after ordinary bank setup so optional inventory extensions can add their controls. */
public class BankOpened(public val player: Player) : UnboundEvent
