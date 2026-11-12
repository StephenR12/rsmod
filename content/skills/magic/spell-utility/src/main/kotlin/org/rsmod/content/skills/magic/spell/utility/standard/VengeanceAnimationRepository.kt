package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

private var Player.vengeanceAnimationToggles by intVarp(varps.vengeance_anim_toggles)
private var Player.vengeanceAnimationOverride by intVarBit(varbits.vengeance_animation_override)

/** Owns cache-233's Vengeance animation unlock, selection and picker bitmasks. */
public class VengeanceAnimationRepository @Inject constructor() {
    public fun allowedMask(player: Player): Int {
        var mask = DEFAULT_BIT
        if (player.vars[varbits.league_4_vengeance_animation_unlocked] != 0) {
            mask = mask or LEAGUE_IV_BIT
        }
        return mask
    }

    public fun normalizedEnabledMask(player: Player): Int {
        val allowed = allowedMask(player)
        val enabled = player.vengeanceAnimationToggles and allowed
        val normalized = if (enabled == 0) DEFAULT_BIT else enabled
        if (player.vengeanceAnimationToggles != normalized) {
            player.vengeanceAnimationToggles = normalized
        }
        return normalized
    }

    /** Toggles one unlocked entry while preserving the picker's at-least-one-enabled invariant. */
    public fun toggle(player: Player, index: Int): Boolean {
        if (index !in DEFAULT_INDEX..LEAGUE_IV_INDEX) {
            return false
        }
        val bit = 1 shl index
        if (allowedMask(player) and bit == 0) {
            return false
        }
        val current = normalizedEnabledMask(player)
        val next = current xor bit
        if (next == 0) {
            return false
        }
        player.vengeanceAnimationToggles = next
        return true
    }

    public fun choose(player: Player, random: GameRandom): Int {
        val enabled = normalizedEnabledMask(player)
        val choices = buildList {
            if (enabled and DEFAULT_BIT != 0) add(DEFAULT_INDEX)
            if (enabled and LEAGUE_IV_BIT != 0) add(LEAGUE_IV_INDEX)
        }
        val selected = random.pick(choices)
        player.vengeanceAnimationOverride = selected
        return selected
    }

    public companion object {
        public const val DEFAULT_INDEX: Int = 0
        public const val LEAGUE_IV_INDEX: Int = 1
        public const val DEFAULT_BIT: Int = 1 shl DEFAULT_INDEX
        public const val LEAGUE_IV_BIT: Int = 1 shl LEAGUE_IV_INDEX
    }
}
