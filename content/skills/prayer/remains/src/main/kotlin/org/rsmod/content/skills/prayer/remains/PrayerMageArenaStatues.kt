package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType

/** Cache-233 Mage Arena I statue rewards. */
public class PrayerMageArenaStatues @Inject constructor(private val objRepo: ObjRepository) {
    public fun pray(access: ProtectedAccess, statue: BoundLocInfo, cape: ObjType) {
        with(access) {
            if (vars[varps.magearena] < MAGE_ARENA_COMPLETE) {
                return
            }
            if (vars[varps.magearena] == MAGE_ARENA_COMPLETE) vars[varps.magearena] = 7
            faceLoc(statue)
            anim(seqs.human_pray)
            objRepo.add(
                type = cape,
                coords = coords,
                duration = player.lootDropDuration ?: constants.lootdrop_duration,
                receiver = player,
            )
        }
    }

    public companion object {
        public const val MAGE_ARENA_COMPLETE: Int = 6
    }
}
