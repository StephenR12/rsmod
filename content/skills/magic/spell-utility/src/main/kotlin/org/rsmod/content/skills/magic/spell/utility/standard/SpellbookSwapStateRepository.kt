package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player

private var Player.activeSpellbook by intVarBit(varbits.spellbook)
private var Player.spellbookSwapBook by intVarBit(varbits.spellbook_swap_book)
private var Player.spellbookSwapActive by intVarBit(varbits.lunar_spellbook_change)

/** Owns the cache-233 Spellbook Swap state and exact 200-tick expiry. */
@Singleton
public class SpellbookSwapStateRepository @Inject constructor() {
    private val expiresAt = WeakHashMap<Player, Int>()

    public fun activate(player: Player, spellbook: Spellbook) {
        require(spellbook != Spellbook.Lunars) { "Spellbook Swap cannot select Lunar spells." }
        expiresAt[player] = player.currentMapClock + ACTIVE_CYCLES
        player.activeSpellbook = spellbook.varValue
        player.spellbookSwapBook = spellbook.varValue
        player.spellbookSwapActive = 1
        player.softTimer(timers.magic_spellbook_swap_timeout, 1)
    }

    public fun consume(player: Player) {
        if (expiresAt.containsKey(player)) {
            revert(player)
        }
    }

    public fun tick(player: Player) {
        val expiry = expiresAt[player]
        if (
            expiry == null ||
                player.currentMapClock >= expiry ||
                player.vars[varbits.in_lms_game] == 1
        ) {
            revert(player)
        }
    }

    public fun syncOnLogin(player: Player) {
        val expiry = expiresAt[player]
        if (
            expiry == null ||
                player.currentMapClock >= expiry ||
                player.vars[varbits.in_lms_game] == 1
        ) {
            if (player.spellbookSwapActive != 0) {
                revert(player)
            }
            return
        }
        player.spellbookSwapActive = 1
        player.softTimer(timers.magic_spellbook_swap_timeout, 1)
    }

    public fun isActive(player: Player): Boolean = expiresAt.containsKey(player)

    public fun revert(player: Player) {
        expiresAt.remove(player)
        player.activeSpellbook = Spellbook.Lunars.varValue
        player.spellbookSwapBook = 0
        player.spellbookSwapActive = 0
        player.clearSoftTimer(timers.magic_spellbook_swap_timeout)
    }

    public companion object {
        public const val ACTIVE_CYCLES: Int = 200
    }
}
