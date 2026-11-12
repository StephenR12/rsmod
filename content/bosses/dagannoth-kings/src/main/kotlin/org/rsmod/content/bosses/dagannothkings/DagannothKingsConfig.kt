package org.rsmod.content.bosses.dagannothkings

import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object DagannothKingNpcs : NpcReferences() {
    val rex = find("dagcave_melee_boss")
    val prime = find("dagcave_magic_boss")
    val supreme = find("dagcave_ranged_boss")
}

public object DagannothKingObjs : ObjReferences() {
    val bones = find("dagannoth_king_bones")
    val dragonAxe = find("dragon_axe")
    val warriorRing = find("warrior_ring")
    val berserkerRing = find("berzerker_ring")
    val seersRing = find("seer_ring")
    val archersRing = find("ranger_ring")
    val mudBattlestaff = find("mud_battlestaff")
    val seercull = find("daganoth_cave_magic_shortbow")
    val rexPet = find("rexpet")
    val primePet = find("primepet")
    val supremePet = find("supremepet")
}

public data class DagannothKingDefinition(
    val npc: NpcType,
    val spawn: CoordGrid,
    val style: HitType,
    val maxHit: Int,
    val uniques: List<ObjType>,
    val pet: ObjType,
)

public object DagannothKings {
    public val definitions: List<DagannothKingDefinition> =
        listOf(
            DagannothKingDefinition(
                DagannothKingNpcs.rex,
                CoordGrid(2926, 4445, 0),
                HitType.Melee,
                26,
                listOf(DagannothKingObjs.warriorRing, DagannothKingObjs.berserkerRing),
                DagannothKingObjs.rexPet,
            ),
            DagannothKingDefinition(
                DagannothKingNpcs.prime,
                CoordGrid(2922, 4454, 0),
                HitType.Magic,
                50,
                listOf(DagannothKingObjs.seersRing, DagannothKingObjs.mudBattlestaff),
                DagannothKingObjs.primePet,
            ),
            DagannothKingDefinition(
                DagannothKingNpcs.supreme,
                CoordGrid(2914, 4455, 0),
                HitType.Ranged,
                30,
                listOf(DagannothKingObjs.archersRing, DagannothKingObjs.seercull),
                DagannothKingObjs.supremePet,
            ),
        )

    public val byId: Map<Int, DagannothKingDefinition> = definitions.associateBy { it.npc.id }
}
