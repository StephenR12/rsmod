package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.ceil
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.chatMesColorTag
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public data class SurgePotionDose(
    public val potion: ObjType,
    public val replacement: ObjType?,
    public val dosesRemaining: Int,
)

public object SurgePotionDoses {
    public val all: List<SurgePotionDose> =
        listOf(
            SurgePotionDose(objs.surge_potion_4, objs.surge_potion_3, 3),
            SurgePotionDose(objs.surge_potion_3, objs.surge_potion_2, 2),
            SurgePotionDose(objs.surge_potion_2, objs.surge_potion_1, 1),
            SurgePotionDose(objs.surge_potion_1, objs.empty_vial, 0),
        )
}

public data class SurgePotionRestore(public val origin: CoordGrid)

/** Cache-233 direct consumption, cooldown and delayed PvP restoration for Surge potions. */
public class SurgePotionDrinking
@Inject
constructor(
    private val potions: PotionConsumptionRepository,
    private val cooldown: SurgePotionCooldownRepository,
    private val specialEnergy: SpecialAttackEnergy,
    private val realm: Realm,
) {
    public fun drink(access: ProtectedAccess, slot: Int, dose: SurgePotionDose) {
        with(access) {
            if (slot !in player.inv.indices) {
                return
            }
            val selected = player.inv[slot]
            if (selected?.id != dose.potion.id || !potions.isReady(player)) {
                return
            }
            if (!player.members) {
                mes(MEMBERS_MESSAGE)
                return
            }
            if (cooldown.isActive(player)) {
                mes(cooldown.rejectionMessage(player))
                return
            }
            if (specialEnergy.hasSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)) {
                mes(NO_EFFECT_MESSAGE)
                return
            }

            val replacement =
                if (dose.dosesRemaining == 0 && player.vars[varbits.auto_smash_vials] != 0) {
                    null
                } else {
                    dose.replacement
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, dose.potion.id, count = 1, slot = slot)
                    replacement?.let { add(inventory, it.id, count = 1, slot = slot) }
                }
            check(result.success) { "Surge-potion replacement failed after validation." }

            stopAction()
            anim(seqs.human_eat)
            mes(DRINK_MESSAGE)
            if (dose.dosesRemaining == 0) {
                mes(FINISHED_MESSAGE)
            } else {
                val doseWord = if (dose.dosesRemaining == 1) "dose" else "doses"
                mes("You have ${dose.dosesRemaining} $doseWord of potion left.")
            }
            potions.consume(player, POTION_DELAY_CYCLES)
            applyStoredEffect(this, dose.potion)
        }
    }

    /** Shared direct and chugging-barrel eligibility; potion delay is managed by the caller. */
    public fun canConsumeStored(player: Player): Boolean =
        player.members &&
            !cooldown.isActive(player) &&
            !specialEnergy.hasSpecialEnergy(player, SpecialAttackEnergy.MAX_ENERGY)

    public fun applyStoredEffect(access: ProtectedAccess, potion: ObjType) {
        with(access) {
            if (!canConsumeStored(player)) return
            cooldown.start(player)
            if (isPvpArea(player)) {
                softQueue(
                    herblore_queues.surgePotionRestore,
                    PVP_RESTORE_DELAY_CYCLES,
                    SurgePotionRestore(coords),
                )
            } else restore(player)
            publish(PlayerConsumableEvent.Consumed(player, potion))
        }
    }

    public fun restoreAfterPvpDelay(player: Player, request: SurgePotionRestore) {
        if (player.coords.chebyshevDistance(request.origin) > PVP_MAX_DISTANCE) {
            return
        }
        restore(player)
    }

    public fun cancelPendingRestore(player: Player) {
        player.queueList.removeAll(herblore_queues.surgePotionRestore)
    }

    private fun restore(player: Player) {
        specialEnergy.restoreSpecialEnergy(player, SPECIAL_ENERGY_RESTORE)
        player.spotanim(spotanims.surge_potion)
    }

    private fun isPvpArea(player: Player): Boolean =
        player.vars[varbits.inside_wilderness] == 1 ||
            player.vars[varbits.pvp_area_client] == 1 ||
            realm.config.hasWorldFlag(WorldFlag.Pvp)

    public companion object {
        public const val POTION_DELAY_CYCLES: Int = 3
        public const val PVP_RESTORE_DELAY_CYCLES: Int = 6
        public const val PVP_MAX_DISTANCE: Int = 15
        public const val SPECIAL_ENERGY_RESTORE: Int = 250
        public const val DRINK_MESSAGE: String = "You drink some of your surge potion."
        public const val FINISHED_MESSAGE: String = "You have finished your potion."
        public const val NO_EFFECT_MESSAGE: String = "Drinking this would have no effect."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use this item."
    }
}

/** Persistent remaining-cycle state. Logout intentionally pauses the cache-233 cooldown. */
@Singleton
public class SurgePotionCooldownRepository {
    public fun start(player: Player) {
        player.surgePotionCooldownCycles = DURATION_CYCLES
        syncClient(player)
        player.softTimer(timers.surge_potion_cooldown, TICK_CYCLES)
    }

    public fun tick(player: Player) {
        val remaining = player.surgePotionCooldownCycles
        if (remaining <= TICK_CYCLES) {
            clear(player)
            val colour = player.chatMesColorTag(opaque = "e00a19", transparent = "ff3045")
            player.mes("$colour$EXPIRED_MESSAGE</col>")
            return
        }
        player.surgePotionCooldownCycles = remaining - TICK_CYCLES
        syncClient(player)
    }

    public fun pause(player: Player) {
        player.clearSoftTimer(timers.surge_potion_cooldown)
    }

    public fun resume(player: Player) {
        if (isActive(player)) {
            syncClient(player)
            player.softTimer(timers.surge_potion_cooldown, TICK_CYCLES)
        } else {
            clear(player)
        }
    }

    public fun clear(player: Player) {
        player.surgePotionCooldownCycles = 0
        player.surgePotionDisplay = 0
        player.clearSoftTimer(timers.surge_potion_cooldown)
    }

    public fun isActive(player: Player): Boolean = player.surgePotionCooldownCycles > 0

    public fun remainingCycles(player: Player): Int = player.surgePotionCooldownCycles

    public fun rejectionMessage(player: Player): String {
        val seconds = ceil(player.surgePotionCooldownCycles * SECONDS_PER_CYCLE).toInt()
        val minutes = seconds / SECONDS_PER_MINUTE
        val remainder = seconds % SECONDS_PER_MINUTE
        val duration =
            when {
                minutes == 0 -> "$remainder ${unit(remainder, "second")}"
                remainder == 0 -> "$minutes ${unit(minutes, "minute")}"
                else ->
                    "$minutes ${unit(minutes, "minute")} and " +
                        "$remainder ${unit(remainder, "second")}"
            }
        return "You are still full of adrenaline. You'll be able to drink in $duration."
    }

    private fun syncClient(player: Player) {
        val remaining = player.surgePotionCooldownCycles
        player.surgePotionDisplay = (remaining + DISPLAY_UNIT_CYCLES - 1) / DISPLAY_UNIT_CYCLES
    }

    private fun unit(value: Int, singular: String): String =
        if (value == 1) singular else "${singular}s"

    public companion object {
        public const val DURATION_CYCLES: Int = 500
        public const val DISPLAY_UNIT_CYCLES: Int = 10
        public const val TICK_CYCLES: Int = 1
        public const val EXPIRED_MESSAGE: String =
            "You now feel capable of drinking another dose of surge potion."
        private const val SECONDS_PER_CYCLE: Double = 0.6
        private const val SECONDS_PER_MINUTE: Int = 60
    }
}

private var Player.surgePotionCooldownCycles by intVarp(varps.surge_potion_cooldown_cycles)
private var Player.surgePotionDisplay by intVarBit(varbits.surge_potion_timer)
