package org.rsmod.content.bosses.barrows

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

public object BarrowsNpcs : NpcReferences() {
    val ahrim = find("barrows_ahrim")
    val dharok = find("barrows_dharok")
    val guthan = find("barrows_guthan")
    val karil = find("barrows_karil")
    val torag = find("barrows_torag")
    val verac = find("barrows_verac")
}

public object BarrowsLocs : LocReferences() {
    val rewardChest = find("barrows_stone_chest")
}

public object BarrowsObjs : ObjReferences() {
    val dharokHead = find("barrows_dharok_head")
    val dharokBody = find("barrows_dharok_body")
    val dharokLegs = find("barrows_dharok_legs")
    val dharokWeapon = find("barrows_dharok_weapon")
    val toragHead = find("barrows_torag_head")
    val toragBody = find("barrows_torag_body")
    val toragLegs = find("barrows_torag_legs")
    val toragWeapon = find("barrows_torag_weapon")
    val veracHead = find("barrows_verac_head")
    val veracBody = find("barrows_verac_body")
    val veracLegs = find("barrows_verac_legs")
    val veracWeapon = find("barrows_verac_weapon")
    val ahrimHead = find("barrows_ahrim_head")
    val ahrimBody = find("barrows_ahrim_body")
    val ahrimLegs = find("barrows_ahrim_legs")
    val ahrimWeapon = find("barrows_ahrim_weapon")
    val guthanHead = find("barrows_guthan_head")
    val guthanBody = find("barrows_guthan_body")
    val guthanLegs = find("barrows_guthan_legs")
    val guthanWeapon = find("barrows_guthan_weapon")
    val karilHead = find("barrows_karil_head")
    val karilBody = find("barrows_karil_body")
    val karilLegs = find("barrows_karil_legs")
    val karilWeapon = find("barrows_karil_weapon")
}

public data class BarrowsBrother(
    val npc: NpcType,
    val spawn: CoordGrid,
    val style: HitType,
    val maxHit: Int,
    val equipment: List<ObjType>,
)

public object BarrowsBrothers {
    public val all: List<BarrowsBrother> =
        listOf(
            brother(
                BarrowsNpcs.ahrim,
                3556,
                9700,
                HitType.Magic,
                20,
                BarrowsObjs.ahrimHead,
                BarrowsObjs.ahrimBody,
                BarrowsObjs.ahrimLegs,
                BarrowsObjs.ahrimWeapon,
            ),
            brother(
                BarrowsNpcs.dharok,
                3554,
                9718,
                HitType.Melee,
                29,
                BarrowsObjs.dharokHead,
                BarrowsObjs.dharokBody,
                BarrowsObjs.dharokLegs,
                BarrowsObjs.dharokWeapon,
            ),
            brother(
                BarrowsNpcs.guthan,
                3534,
                9704,
                HitType.Melee,
                24,
                BarrowsObjs.guthanHead,
                BarrowsObjs.guthanBody,
                BarrowsObjs.guthanLegs,
                BarrowsObjs.guthanWeapon,
            ),
            brother(
                BarrowsNpcs.karil,
                3546,
                9684,
                HitType.Ranged,
                20,
                BarrowsObjs.karilHead,
                BarrowsObjs.karilBody,
                BarrowsObjs.karilLegs,
                BarrowsObjs.karilWeapon,
            ),
            brother(
                BarrowsNpcs.torag,
                3568,
                9686,
                HitType.Melee,
                23,
                BarrowsObjs.toragHead,
                BarrowsObjs.toragBody,
                BarrowsObjs.toragLegs,
                BarrowsObjs.toragWeapon,
            ),
            brother(
                BarrowsNpcs.verac,
                3576,
                9706,
                HitType.Melee,
                24,
                BarrowsObjs.veracHead,
                BarrowsObjs.veracBody,
                BarrowsObjs.veracLegs,
                BarrowsObjs.veracWeapon,
            ),
        )

    public val byId: Map<Int, BarrowsBrother> = all.associateBy { it.npc.id }

    private fun brother(
        npc: NpcType,
        x: Int,
        z: Int,
        style: HitType,
        maxHit: Int,
        vararg equipment: ObjType,
    ): BarrowsBrother = BarrowsBrother(npc, CoordGrid(x, z, 0), style, maxHit, equipment.toList())
}
