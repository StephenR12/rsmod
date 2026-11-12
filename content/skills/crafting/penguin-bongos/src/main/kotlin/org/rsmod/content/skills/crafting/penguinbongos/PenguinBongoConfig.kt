package org.rsmod.content.skills.crafting.penguinbongos

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences

internal typealias penguin_bongo_objs = PenguinBongoObjs

internal typealias penguin_bongo_seqs = PenguinBongoSeqs

internal typealias penguin_bongo_varbits = PenguinBongoVarBits

public object PenguinBongoObjs : ObjReferences() {
    public val leather = find("leather")
    public val mahoganyPlank = find("plank_mahogany")
    public val bongos = find("peng_bongos")
}

public object PenguinBongoSeqs : SeqReferences() {
    /** The cache names this short action sequence for the player, unlike the longer ready loop. */
    public val play = find("peng_player_bongo")
    public val ready = find("peng_bongo_ready")
}

public object PenguinBongoVarBits : VarBitReferences() {
    public val coldWarProgress = find("peng_quest")
    public val penguinTransmog = find("peng_transmog")
}
