package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

public class FiremakingXp {
    public fun advance(access: ProtectedAccess, baseXp: Double) {
        with(access) { statAdvance(stats.firemaking, baseXp * multiplier(player)) }
    }

    public fun multiplier(player: Player): Double {
        var basisPoints = 0
        var pieces = 0
        for ((piece, bonus) in PIECES) {
            if (player.worn.contains(piece)) {
                basisPoints += bonus
                pieces++
            }
        }
        if (pieces == PIECES.size) {
            basisPoints += FULL_SET_BONUS
        }
        return 1.0 + basisPoints / 1_000.0
    }

    private companion object {
        // Cache xpmod_percent units are tenths of one percent.
        val PIECES: Map<ObjType, Int> =
            linkedMapOf(
                objs.pyromancer_hood to 4,
                objs.pyromancer_garb to 8,
                objs.pyromancer_robe to 6,
                objs.pyromancer_boots to 2,
            )
        const val FULL_SET_BONUS = 5
    }
}
