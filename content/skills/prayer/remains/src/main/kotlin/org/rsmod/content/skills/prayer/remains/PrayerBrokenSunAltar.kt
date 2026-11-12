package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo

/** Cache-233 Forthos Dungeon broken sun altar behavior. */
public class PrayerBrokenSunAltar {
    public fun pray(access: ProtectedAccess, altar: BoundLocInfo) {
        with(access) {
            faceLoc(altar)
            val sequence =
                if (vars[varbits.varlamore_sun_knight] == KNIGHT_AT_ALTAR) {
                    seqs.human_sun_prayer
                } else {
                    seqs.human_pray
                }
            anim(sequence)
            mes(NOTHING_HAPPENS)
        }
    }

    public companion object {
        public const val KNIGHT_AT_ALTAR: Int = 1
        public const val NOTHING_HAPPENS: String =
            "You pray at the altar, nothing interesting happens."
    }
}
