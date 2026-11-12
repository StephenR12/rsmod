package org.rsmod.content.skills.woodcutting

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences

public object CrystalSingingLocs : LocReferences() {
    public val bowl = find("prif_singing_bowl")
}

public object CrystalSingingNpcs : NpcReferences() {
    public val reese = find("prif_singer1_learnt")
    public val conwenna = find("prif_singer2_learnt")
    public val assistants = listOf(reese, conwenna)
}
