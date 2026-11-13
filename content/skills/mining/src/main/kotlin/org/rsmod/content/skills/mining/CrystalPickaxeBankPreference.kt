package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit

public class CrystalPickaxeBankPreference @Inject constructor() {
    public suspend fun configureCharges(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to configure a crystal pickaxe.")
                return
            }
            val requested = countDialog("How many charges should the tool hold on withdrawal?")
            player.crystalToolAutochargeQuantity = requested.coerceIn(0, MAX_CHARGES)
        }
    }

    public companion object {
        public const val MAX_CHARGES: Int = 20_000
    }
}

private var org.rsmod.game.entity.Player.crystalToolAutochargeQuantity by
    intVarBit(varbits.charges_crystal_tools_quantity)
