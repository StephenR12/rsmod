package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo

public class PrayerAltarRestoration {
    public fun restore(access: ProtectedAccess, altar: BoundLocInfo, bonus: Int = 0) {
        with(access) {
            faceLoc(altar)
            anim(seqs.human_pray)

            // Since 5 April 2023, praying at an altar resets accumulated Prayer drain progress.
            vars[varbits.prayer_drain_counter] = 0

            val target = (statBase(stats.prayer) + bonus).coerceAtMost(MAX_LEVEL)
            val delta = target - stat(stats.prayer)
            when {
                delta > 0 -> statAdd(stats.prayer, constant = delta, percent = 0)
                delta < 0 -> statSub(stats.prayer, constant = -delta, percent = 0)
            }
            mes("You recharge your Prayer points.")
        }
    }

    private companion object {
        const val MAX_LEVEL = 255
    }
}
