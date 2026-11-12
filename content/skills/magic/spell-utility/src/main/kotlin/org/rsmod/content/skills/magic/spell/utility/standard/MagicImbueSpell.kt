package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player

public class MagicImbueSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val states: MagicImbueStateRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!canAccess() || states.isActive(player)) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_magic_imbue))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_cast_selfimbue)
            soundSynth(synths.lunar_embue_runes)
            states.activate(player)
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        return true
    }

    private companion object {
        const val LUNAR_DIPLOMACY_COMPLETE = 190
    }
}

private var Player.magicImbueDisplay by intVarBit(varbits.magic_imbue_active)

@Singleton
public class MagicImbueStateRepository {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player) {
        expiresAt[player] = player.currentMapClock + DURATION_CYCLES
        syncClient(player, DURATION_CYCLES)
        player.softTimer(timers.magic_imbue_active, 1)
    }

    public fun isActive(player: Player): Boolean = remainingCycles(player) > 0

    public fun remainingCycles(player: Player): Int {
        val expiry = expiresAt[player]
        if (expiry == null) {
            player.magicImbueDisplay = 0
            return 0
        }
        val remaining = expiry - player.currentMapClock
        if (remaining <= 0) {
            clear(player)
            return 0
        }
        return remaining
    }

    public fun tick(player: Player) {
        val remaining = remainingCycles(player)
        if (remaining > 0) {
            syncClient(player, remaining)
        }
    }

    public fun clear(player: Player) {
        expiresAt.remove(player)
        player.magicImbueDisplay = 0
        player.clearSoftTimer(timers.magic_imbue_active)
    }

    private fun syncClient(player: Player, remaining: Int) {
        // Cache varbit 5438 is only two bits wide. RuneLite defines each non-zero unit as ten
        // remaining game ticks, while the effect itself lasts the Wiki's exact 21-tick period.
        player.magicImbueDisplay = minOf(MAX_DISPLAY_VALUE, (remaining + 9) / 10)
    }

    public companion object {
        public const val DURATION_CYCLES: Int = 21
        public const val MAX_DISPLAY_VALUE: Int = 2
    }
}
