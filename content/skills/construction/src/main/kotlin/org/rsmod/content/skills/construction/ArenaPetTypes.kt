package org.rsmod.content.skills.construction

import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

internal object ArenaPetTypes : InvBuilder() {
    init {
        build("poh_menagerie_arena_pets") {
            scope = InvScope.Perm
            stack = InvStackType.Never
            protect = false
            size = 2
        }
    }
}
internal object ArenaPetTimers : TimerReferences() { val fight = find("poh_menagerie_arena_tick") }
internal object MenagerieSequences : SeqReferences() { val swipe = find("cat_attack") }
