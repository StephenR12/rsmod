package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

private var Player.deathChargeCooldown by intVarBit(varbits.arceuus_death_charge_cooldown)
private var Player.deathChargeDisplay by intVarBit(varbits.arceuus_death_charge_active)

public class DeathChargeSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: DeathChargeStateRepository,
    private val specialEnergy: SpecialAttackEnergy,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || player.deathChargeCooldown != 0 || states.isActive(player)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_death_charge))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            val upgraded = vars[varbits.death_charge_scroll_used] != 0
            anim(seqs.death_charge)
            spotanim(
                if (upgraded) spotanims.death_charge_upgrade_cast else spotanims.death_charge_cast
            )
            states.activate(player, upgraded)
            player.deathChargeCooldown = 1
            player.softTimer(timers.magic_arceuus_death_charge_cooldown, DURATION_CYCLES)
            player.softTimer(timers.magic_arceuus_death_charge_expiry, DURATION_CYCLES)
        }
    }

    public fun onNpcKill(player: Player, npc: Npc) {
        if (!states.isActive(player) || !DeathChargePolicy.canTrigger(player, npc)) {
            return
        }
        val consumption = states.consume(player) ?: return
        specialEnergy.restoreSpecialEnergy(player, RESTORE_ENERGY)
        npc.anim(seqs.death_charge_npc_hit)
        npc.spotanim(
            if (consumption.upgraded) {
                spotanims.death_charge_upgrade_end
            } else {
                spotanims.death_charge_end
            }
        )
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.a_kingdom_divided] < A_KINGDOM_DIVIDED_COMPLETE) {
            mes("You need to complete A Kingdom Divided to cast this spell.")
            return false
        }
        return true
    }

    public companion object {
        public const val DURATION_CYCLES: Int = 100
        public const val RESTORE_ENERGY: Int = 150
        private const val A_KINGDOM_DIVIDED_COMPLETE: Int = 150
    }
}

public data class DeathChargeConsumption(
    public val upgraded: Boolean,
    public val remainingCharges: Int,
)

@Singleton
public class DeathChargeStateRepository {
    private val states = WeakHashMap<Player, State>()

    public fun activate(player: Player, upgraded: Boolean) {
        val charges = if (upgraded) 2 else 1
        states[player] =
            State(player.currentMapClock + DeathChargeSpell.DURATION_CYCLES, charges, upgraded)
        player.deathChargeDisplay = charges
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    public fun remainingCycles(player: Player): Int {
        val state = states[player] ?: return 0
        val remaining = state.expiresAt - player.currentMapClock
        if (remaining <= 0) {
            clear(player)
            return 0
        }
        return remaining
    }

    public fun remainingCharges(player: Player): Int {
        if (!isActive(player)) {
            return 0
        }
        return states.getValue(player).charges
    }

    public fun consume(player: Player): DeathChargeConsumption? {
        if (!isActive(player)) {
            return null
        }
        val state = states.getValue(player)
        val remaining = state.charges - 1
        if (remaining == 0) {
            clear(player)
        } else {
            states[player] = state.copy(charges = remaining)
            player.deathChargeDisplay = remaining
        }
        return DeathChargeConsumption(state.upgraded, remaining)
    }

    public fun clear(player: Player) {
        states.remove(player)
        player.deathChargeDisplay = 0
        player.clearSoftTimer(timers.magic_arceuus_death_charge_expiry)
    }

    private data class State(val expiresAt: Int, val charges: Int, val upgraded: Boolean)
}

internal object DeathChargePolicy {
    private val excludedNpcIds =
        setOf(
            5914,
            5915, // Abyssal Sire respiratory systems.
            7550,
            7552,
            7553,
            7555,
            7556,
            7557, // Great Olm's hands and transitions.
            7568, // Vasa Nistirio's glowing crystal.
            9434,
            9435,
            9436,
            9437,
            9438,
            9439,
            9440,
            9441,
            9442,
            9443,
            9444,
            9445, // Nightmare and Phosani's Nightmare totems.
            11278,
            11279,
            11280,
            11281,
            11282, // Nex requires encounter-specific most-damage attribution.
            11750,
            11751,
            11752,
            11769, // Wardens obelisk and energy siphons.
            13011,
            13012,
            13013,
            13016,
            13017,
            13018,
            13020, // Moons of Peril visible/combat forms.
            13688, // Amoxliatl unstable ice block.
        )

    fun canTrigger(player: Player, npc: Npc): Boolean {
        if (npc.visType.id in excludedNpcIds || player.distanceTo(npc) > MAX_DISTANCE) {
            return false
        }
        val lastPvp = player.vars[varps.lastcombat_pvp]
        return lastPvp <= 0 || player.currentMapClock - lastPvp > PVP_SUPPRESSION_CYCLES
    }

    const val MAX_DISTANCE: Int = 16
    const val PVP_SUPPRESSION_CYCLES: Int = 10
}
