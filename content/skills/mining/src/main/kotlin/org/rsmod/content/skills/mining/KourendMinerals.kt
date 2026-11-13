package org.rsmod.content.skills.mining

import org.rsmod.api.player.hat
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList

public object KourendMineralLocs : LocReferences() {
    val lovakite = listOf(find("lovakite_rock1"), find("lovakite_rock2"))
    val sulphur = (1..3).map { find("sulphur_rock_0$it") }
    val saltpetre = (1..4).map { find("hosidius_saltpetre_$it") }
}

public object KourendMineralObjs : ObjReferences() {
    val sulphur = find("lovakengj_sulphur")
    val lovakite = find("lovakite_ore")
    val saltpetre = find("hosidius_saltpetre")
    val gasMask = find("gasmask")
    val faceMask = find("slayer_facemask")
}

public object KourendMinerals {
    public fun sulphurFilter(player: Player, types: ObjTypeList): Boolean {
        val hat = player.hat ?: return false
        val name = types[hat].internalName ?: return false
        return hat.id == KourendMineralObjs.gasMask.id ||
            hat.id == KourendMineralObjs.faceMask.id ||
            name.contains("slayer_helm")
    }
}
