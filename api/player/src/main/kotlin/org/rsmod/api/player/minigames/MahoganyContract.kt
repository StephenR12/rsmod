package org.rsmod.api.player.minigames

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.SuspendEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType

public object MahoganyContract : VarpReferences() {
    public val client: VarpType = find("mahogany_homes_client")
    public const val ROSS: Int = 7
}

internal object MahoganyContractClientSave : VarpBuilder() {
    init {
        build("mahogany_homes_client") { transmitNever = true }
    }
}

public fun Player.hasRossContract(): Boolean =
    vars[MahoganyContract.client] == MahoganyContract.ROSS

public object ContactAmy : SuspendEvent<ProtectedAccess> {
    override val id: Long = 0
}
