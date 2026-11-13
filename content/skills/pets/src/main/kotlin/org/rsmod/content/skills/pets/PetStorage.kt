package org.rsmod.content.skills.pets

import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

internal object PetStorage : InvReferences() {
    val arena = find("poh_menagerie_arena_pets")
    val forms = find("skill_pet_menagerie_forms")
}

internal object MenagerieStorage : InvBuilder() {
    init {
        // The native 12-slot inventory holds extras. Unique pets use native species bits.
        // Keep the exact owned colour as well, including forms with no native saved colour.
        build("skill_pet_menagerie_forms") {
            scope = InvScope.Perm
            stack = InvStackType.Never
            protect = false
            size = 65
        }
    }
}
