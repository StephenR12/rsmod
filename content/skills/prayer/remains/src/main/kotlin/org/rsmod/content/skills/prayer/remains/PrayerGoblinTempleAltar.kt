package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo

/** Cache-233 Land of the Goblins temple altar behavior. */
public class PrayerGoblinTempleAltar
@Inject
constructor(private val restoration: PrayerAltarRestoration) {
    public fun pray(access: ProtectedAccess, altar: BoundLocInfo) {
        if (access.vars[varbits.land_of_the_goblins] >= RESTORATION_UNLOCKED) {
            restoration.restore(access, altar)
            return
        }
        with(access) {
            faceLoc(altar)
            anim(seqs.human_pray)
            mes(NO_ANSWER)
        }
    }

    public companion object {
        /** The temple crypt is open and the high-priest encounters have begun. */
        public const val RESTORATION_UNLOCKED: Int = 36
        public const val QUEST_COMPLETE: Int = 56
        public const val NO_ANSWER: String = "The Big High War God does not answer your prayer"
    }
}
