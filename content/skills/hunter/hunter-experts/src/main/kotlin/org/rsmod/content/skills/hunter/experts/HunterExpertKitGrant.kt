package org.rsmod.content.skills.hunter.experts

import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.protect.ProtectedAccess

public class HunterExpertKitGrant {
    public fun hasClaimed(access: ProtectedAccess): Boolean =
        access.vars[HunterExpertVarps.receivedHunterKit] != 0

    public fun claim(access: ProtectedAccess): HunterExpertKitClaim {
        with(access) {
            if (hasClaimed(this)) {
                return HunterExpertKitClaim.AlreadyClaimed
            }
            val preflight = invAdd(player.inv, objs.hunter_kit, autoCommit = false)
            if (!preflight.success) {
                return HunterExpertKitClaim.NoInventorySpace
            }
            val committed = invAdd(player.inv, objs.hunter_kit)
            check(committed.success) { "Hunter expert kit transaction failed after validation." }
            vars[HunterExpertVarps.receivedHunterKit] = 1
            return HunterExpertKitClaim.Granted
        }
    }
}

public enum class HunterExpertKitClaim {
    Granted,
    AlreadyClaimed,
    NoInventorySpace,
}
