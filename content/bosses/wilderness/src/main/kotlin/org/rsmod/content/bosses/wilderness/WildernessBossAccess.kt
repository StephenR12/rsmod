package org.rsmod.content.bosses.wilderness

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player

@Singleton
public class WildernessBossAccess @Inject constructor() {
    public suspend fun enter(access: ProtectedAccess, definition: WildernessBossDefinition) {
        with(access) {
            if (!entranceFeePaid) {
                if (!invTakeFee(ENTRANCE_FEE)) {
                    mes("You need 50,000 coins in your inventory to enter this boss lair.")
                    return
                }
                entranceFeePaid = true
                mes("You pay 50,000 coins. The fee covers all Wilderness boss lairs.")
            }
            telejump(definition.spawn)
            delay(TELEPORT_DELAY)
        }
    }

    public fun hasPaid(player: Player): Boolean = player.entranceFeePaid

    public companion object {
        public const val ENTRANCE_FEE: Int = 50_000
        public const val TELEPORT_DELAY: Int = 3
    }
}

private var ProtectedAccess.entranceFeePaid by boolVarBit(WildernessBossVarBits.entranceFeePaid)
private val Player.entranceFeePaid by boolVarBit(WildernessBossVarBits.entranceFeePaid)
