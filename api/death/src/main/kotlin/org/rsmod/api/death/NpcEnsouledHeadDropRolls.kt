package org.rsmod.api.death

import org.rsmod.game.inv.FreshDropOrigin
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

/** Exact fresh-drop provenance wrapper shared by NPC ensouled-head tertiary rolls. */
public object NpcEnsouledHeadDropRolls {
    /** One independent head roll whose origin remains attached across pickup and re-dropping. */
    public fun independent(type: ObjType, denominator: Int): NpcDropRoll =
        NpcDropRolls.independent(
            denominator = denominator,
            roll =
                NpcDropRoll { context ->
                    val obj = InvObj(type, vars = FreshDropOrigin.pack(context.coords))
                    val drop = NpcDrop(obj, context.coords, context.hero)
                    NpcDropRollResult(selected = true, drops = listOf(drop))
                },
        )
}
