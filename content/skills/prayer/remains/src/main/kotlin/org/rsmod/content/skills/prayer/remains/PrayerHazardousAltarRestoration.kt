package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo

public class PrayerHazardousAltarRestoration
@Inject
constructor(private val random: GameRandom, private val altarRestoration: PrayerAltarRestoration) {
    /**
     * Drains all remaining Prayer and deals equal damage, bounded to 5..30. The minimum applies
     * even when the player has no Prayer points, and the damage is allowed to be lethal.
     */
    public fun curse(access: ProtectedAccess, altar: BoundLocInfo) {
        with(access) {
            faceLoc(altar)
            anim(seqs.human_pray)

            val prayer = stat(stats.prayer)
            vars[varbits.prayer_drain_counter] = 0
            if (prayer > 0) {
                statSub(stats.prayer, constant = prayer, percent = 0)
            }
            takeInstantHit(type = HitType.Typeless, damage = prayer.coerceIn(5, 30))
        }
    }

    /** Restores Prayer normally, then exacts the Darkmeyer statue's random 1..3 Hitpoints cost. */
    public fun darkmeyer(access: ProtectedAccess, altar: BoundLocInfo) {
        altarRestoration.restore(access, altar)
        access.takeInstantHit(type = HitType.Typeless, damage = random.of(1, 3))
    }
}
