package org.rsmod.content.other.degradable

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

public class DegradableEquipmentLifecycle @Inject constructor(private val objTypes: ObjTypeList) {
    private val lastEngagement = WeakHashMap<Player, Int>()

    public fun engage(player: Player) {
        val now = player.currentMapClock
        val previous = lastEngagement[player]
        val wasActive = previous != null && previous + constants.combat_activecombat_delay >= now
        lastEngagement[player] = now
        if (wasActive) {
            return
        }
        degradeWorn(player)
        player.softTimer(degradation_timers.combat, COMBAT_DEGRADE_INTERVAL)
    }

    public fun combatTimer(player: Player) {
        val last = lastEngagement[player]
        if (last == null || last + constants.combat_activecombat_delay < player.currentMapClock) {
            clear(player)
            return
        }
        degradeWorn(player)
    }

    public fun clear(player: Player) {
        lastEngagement.remove(player)
        player.clearSoftTimer(degradation_timers.combat)
    }

    public fun prepareDrop(obj: InvObj): InvObj {
        val definition = DegradableEquipmentCatalog.byType[obj.id] ?: return obj
        return if (definition.remainingPoints(obj) == 0) obj else definition.breakForDrop(obj)
    }

    public fun check(access: ProtectedAccess, obj: InvObj?) {
        obj ?: return
        val definition = DegradableEquipmentCatalog.byType[obj.id] ?: return
        val remaining = definition.remainingPoints(obj)
        val repairedName = objTypes[definition.repaired].name
        with(access) {
            when (definition.family) {
                DegradableEquipmentFamily.Barrows -> {
                    val tenths = remaining
                    mes("Your $repairedName has ${tenths / 10}.${tenths % 10}% charge left.")
                }
                DegradableEquipmentFamily.Moon ->
                    mes("Your $repairedName has $remaining charges remaining.")
            }
        }
    }

    private fun degradeWorn(player: Player) {
        var changed = false
        for (slot in player.worn.indices) {
            val obj = player.worn[slot] ?: continue
            val definition = DegradableEquipmentCatalog.byType[obj.id] ?: continue
            val remaining = definition.remainingPoints(obj)
            if (remaining == 0) {
                continue
            }
            val degraded = definition.degrade(obj)
            player.worn[slot] = degraded
            changed = true
            if (degraded.id == definition.broken.id) {
                player.mes("Your ${objTypes[definition.repaired].name} has completely degraded.")
            }
        }
        if (changed) {
            player.rebuildAppearance()
        }
    }

    public companion object {
        public const val COMBAT_DEGRADE_INTERVAL: Int = 90
    }
}
