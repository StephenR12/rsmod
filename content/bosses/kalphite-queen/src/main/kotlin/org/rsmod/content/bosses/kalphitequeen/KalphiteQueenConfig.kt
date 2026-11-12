package org.rsmod.content.bosses.kalphitequeen

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.map.CoordGrid

public object KalphiteQueenNpcs : NpcReferences() {
    val ground = find("kalphite_queen")
    val airborne = find("kalphite_flyingqueen")
}

public object KalphiteQueenObjs : ObjReferences() {
    val dragonChainbody = find("dragon_chainbody")
    val dragonTwoHander = find("dragon_2h_sword")
    val dragonPickaxe = find("dragon_pickaxe")
    val head = find("poh_trophydrop_kalphitequeen")
    val pet = find("kqpet_walking")
}

public object KalphiteQueenDefinition {
    public val spawn: CoordGrid = CoordGrid(3478, 9498, 0)
    public const val HITPOINTS: Int = 255
    public const val ATTACK: Int = 300
    public const val STRENGTH: Int = 300
    public const val DEFENCE: Int = 300
    public const val MAGIC: Int = 150
    public const val RANGED: Int = 300
    public const val MAX_HIT: Int = 31
}
