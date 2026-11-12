package org.rsmod.api.combat.manager

import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/** Events emitted after a spell's access checks and resource transaction succeed. */
public object MagicSpellEvents {
    public data class Cast(val player: Player, val spell: MagicSpell, val spellbook: Spellbook) :
        UnboundEvent
}
