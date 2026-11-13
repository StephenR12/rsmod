package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

public class PrayerGodWarsAltarTeleport @Inject constructor() {
    public fun teleport(access: ProtectedAccess, god: PrayerGodWarsGod) {
        access.telejump(destination(god))
    }

    public companion object {
        public val ZAMORAK_EXIT: CoordGrid = CoordGrid(2925, 5333, 2)
        public val SARADOMIN_EXIT: CoordGrid = CoordGrid(2909, 5265, 0)
        public val ARMADYL_EXIT: CoordGrid = CoordGrid(2839, 5294, 2)
        public val BANDOS_EXIT: CoordGrid = CoordGrid(2862, 5354, 2)
        public val ZAROS_EXIT: CoordGrid = CoordGrid(2900, 5204, 0)

        public fun destination(god: PrayerGodWarsGod): CoordGrid =
            when (god) {
                PrayerGodWarsGod.Zamorak -> ZAMORAK_EXIT
                PrayerGodWarsGod.Saradomin -> SARADOMIN_EXIT
                PrayerGodWarsGod.Armadyl -> ARMADYL_EXIT
                PrayerGodWarsGod.Bandos -> BANDOS_EXIT
                PrayerGodWarsGod.Zaros -> ZAROS_EXIT
            }
    }
}
