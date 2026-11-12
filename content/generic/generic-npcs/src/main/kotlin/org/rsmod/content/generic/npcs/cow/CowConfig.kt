package org.rsmod.content.generic.npcs.cow

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.npc.NpcType

internal typealias cow_npcs = CowNpcs

internal typealias cow_queues = CowQueues

internal typealias cow_locs = CowLocs

internal typealias cow_objs = CowObjs

internal typealias cow_varbits = CowVarBits

internal typealias cow_seqs = CowSeqs

object CowNpcs : NpcReferences() {
    val gillie_the_milkmaid = find("gillie_the_milkmaid", 8262063475332109549)
    val cow = find("cow")
    val cow2 = find("cow2")
    val cow3 = find("cow3")
    val cow_beef = find("cow_beef")
    val cow2_calf = find("cow2_calf")
    val cow3_calf = find("cow3_calf")
    val calf = find("calf")
    val fairy_cow = find("fairy_cow")
}

object CowQueues : QueueReferences() {
    val milk = find("milk_cow")
}

object CowSeqs : SeqReferences() {
    val cowbellPlay = find("peng_cowbell_ready")
}

object CowLocs : LocReferences() {
    val fat_cow = find("fat_cow")
}

object CowObjs : ObjReferences() {
    val cowbells = find("peng_cowbell")
}

object CowVarBits : VarBitReferences() {
    val coldWarProgress = find("peng_quest")
    val penguinTransmog = find("peng_transmog")
}

internal object CowLocEdits : LocEditor() {
    init {
        edit(cow_locs.fat_cow) { contentGroup = content.dairy_cow }
    }
}

internal object CowNpcEdits : NpcEditor() {
    init {
        cow(cow_npcs.cow)
        cow(cow_npcs.cow2)
        cow(cow_npcs.cow3)
        cow(cow_npcs.cow_beef)
        cow(cow_npcs.fairy_cow)
        calf(cow_npcs.cow2_calf)
        calf(cow_npcs.cow3_calf)
        calf(cow_npcs.calf)
    }

    private fun cow(type: NpcType) {
        edit(type) {
            contentGroup = content.cow
            timer = 1
            respawnRate = COW_RESPAWN_CYCLES
        }
    }

    private fun calf(type: NpcType) {
        edit(type) {
            contentGroup = content.cow_calf
            timer = 1
            respawnRate = COW_RESPAWN_CYCLES
        }
    }

    /** The dated Wiki's 45-tick (27-second) respawn on rsmod's 600 ms game clock. */
    private const val COW_RESPAWN_CYCLES = 45
}
