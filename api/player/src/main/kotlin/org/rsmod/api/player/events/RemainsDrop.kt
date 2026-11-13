package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/** Lets equipment process monster or Hunter remains when the source successfully delivers its loot. */
public class RemainsDrop(public val player: Player, public val obj: InvObj) : UnboundEvent {
    public var remaining: Int = obj.count
        private set
    private var onCommit: (() -> Unit)? = null

    /** The caller must commit synchronously after all other rewards have been delivered. */
    public fun convert(count: Int, action: () -> Unit) {
        check(onCommit == null)
        require(count in 1..remaining)
        remaining -= count
        onCommit = action
    }

    public fun commit() {
        val action = onCommit ?: return
        onCommit = null
        action()
    }
}
