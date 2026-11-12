package org.rsmod.content.bosses.nex

import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType

/** Nex's attacks retain half damage through the matching protection prayer. */
public object NexPlayerHitModifier : PlayerHitModifier {
    override fun HitBuilder.modify(target: Player) {
        val protected =
            when (type) {
                HitType.Melee -> target.vars[varbits.protect_from_melee] == 1
                HitType.Ranged -> target.vars[varbits.protect_from_missiles] == 1
                HitType.Magic -> target.vars[varbits.protect_from_magic] == 1
                HitType.Typeless -> false
            }
        if (protected) {
            damage /= 2
        }
    }
}
