package org.rsmod.content.bosses.giantmole

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.map.CoordGrid

public object GiantMoleNpcs : NpcReferences() {
    val boss = find("mole_giant")
}

public object GiantMoleObjs : ObjReferences() {
    val claw = find("mole_claw")
    val skin = find("mole_skin")
    val bigBones = find("big_bones")
}

public object GiantMoleSeqs : SeqReferences() {
    val attack = find("mole_attack")
    val burrowDown = find("mole_burrow_down")
    val burrowUp = find("mole_burrow_up")
}

public object GiantMoleLair {
    val spawn = CoordGrid(1761, 5187, 0)

    val burrowDestinations =
        listOf(
            CoordGrid(1737, 5226, 0),
            CoordGrid(1751, 5233, 0),
            CoordGrid(1778, 5237, 0),
            CoordGrid(1780, 5201, 0),
            CoordGrid(1759, 5190, 0),
            CoordGrid(1743, 5170, 0),
            CoordGrid(1775, 5154, 0),
            CoordGrid(1738, 5155, 0),
        )
}
