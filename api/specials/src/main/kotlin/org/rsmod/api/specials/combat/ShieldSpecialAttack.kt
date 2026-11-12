package org.rsmod.api.specials.combat

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/** A target-based special attack supplied by an equipped off-hand shield. */
public interface ShieldSpecialAttack {
    public suspend fun ProtectedAccess.attack(target: Npc): Boolean

    public suspend fun ProtectedAccess.attack(target: Player): Boolean
}
