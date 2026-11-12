package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.refs.seqs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

/** Cache-233 Yanille Agility Dungeon Chaos altar trap. */
public class PrayerTrappedChaosAltar {
    public fun pray(access: ProtectedAccess, altar: BoundLocInfo) {
        with(access) {
            faceLoc(altar)
            anim(seqs.human_pray)
            takeInstantHit(type = HitType.Typeless, damage = FALL_DAMAGE)
            telejump(SPIDER_PIT)
        }
    }

    public companion object {
        public const val FALL_DAMAGE: Int = 15
        public val SPIDER_PIT: CoordGrid = CoordGrid(0, 40, 149, 18, 42)
    }
}
