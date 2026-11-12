package org.rsmod.content.travel.fremennikseaboots

import jakarta.inject.Inject
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/** Cache-233.4 Fremennik sea boots daily Rellekka teleport behavior. */
public class FremennikSeaBootsTeleport @Inject constructor() {
    private var ProtectedAccess.teleportsUsed by intVarp(FremennikSeaBootsVarps.teleports_used)
    private var ProtectedAccess.lastTeleportDay by intVarp(FremennikSeaBootsVarps.last_teleport_day)

    public suspend fun teleport(
        access: ProtectedAccess,
        tier: FremennikSeaBootsTier,
        currentUtcDay: Int = currentUtcDay(),
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use these boots.")
                return
            }
            if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
                mes("A mysterious force prevents you from teleporting!")
                return
            }
            if (vars[varbits.teleblock_cycles] > TELEBLOCK_IMMUNITY_CYCLES) {
                mes("A magical force stops you from teleporting.")
                return
            }

            val usedToday = if (lastTeleportDay == currentUtcDay) teleportsUsed else 0
            val limit = tier.dailyLimit
            if (limit != null && usedToday >= limit) {
                mes("The boots have no teleports remaining today.")
                return
            }

            stopAction()
            anim(seqs.teleport_casting)
            spotanim(spotanims.teleport_casting)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)

            lastTeleportDay = currentUtcDay
            teleportsUsed = if (limit == null) 0 else usedToday + 1
            teleport(RELLEKKA_MARKET)
            publish(PlayerTeleportEvent.Start(player, RELLEKKA_MARKET))
        }
    }

    public fun usedToday(access: ProtectedAccess, currentUtcDay: Int = currentUtcDay()): Int =
        with(access) { if (lastTeleportDay == currentUtcDay) teleportsUsed else 0 }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    public companion object {
        public val RELLEKKA_MARKET: CoordGrid = CoordGrid(2643, 3675)
        public const val CAST_TICKS: Int = 4

        private const val MAX_WILDERNESS_LEVEL = 20
        private const val TELEBLOCK_IMMUNITY_CYCLES = 100
        private const val WILDERNESS_LEVEL_HEIGHT = 8
        private const val WILDERNESS_Z = 3520
        private const val WILDERNESS_UNDERGROUND_Z = 9920

        public fun currentUtcDay(): Int = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()
    }
}

public enum class FremennikSeaBootsTier(public val obj: ObjType, public val dailyLimit: Int?) {
    Easy(org.rsmod.api.config.refs.objs.diary_fremennik_boots_1, 1),
    Medium(org.rsmod.api.config.refs.objs.diary_fremennik_boots_2, 3),
    Hard(org.rsmod.api.config.refs.objs.diary_fremennik_boots_3, 5),
    Elite(org.rsmod.api.config.refs.objs.diary_fremennik_boots_4, null),
}
