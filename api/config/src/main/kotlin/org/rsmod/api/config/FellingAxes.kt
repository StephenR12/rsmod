package org.rsmod.api.config

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.refs.obj.ObjReferences

public object FellingAxes : ObjReferences() {
    val inactive = find("crystal_axe_2h_inactive")
    val tools =
        listOf(
            objs.bronze_felling_axe,
            objs.iron_felling_axe,
            objs.steel_felling_axe,
            objs.black_felling_axe,
            objs.mithril_felling_axe,
            objs.adamant_felling_axe,
            objs.rune_felling_axe,
            objs.dragon_felling_axe,
            objs.crystal_felling_axe,
            inactive,
            find("3a_axe_2h"),
        )
    val ordinary =
        listOf(
            objs.bronze_axe,
            objs.iron_axe,
            objs.steel_axe,
            objs.black_axe,
            objs.mithril_axe,
            objs.adamant_axe,
            objs.rune_axe,
            objs.dragon_axe,
            objs.crystal_axe,
            objs.crystal_axe_inactive,
            objs.third_age_axe,
        )
    val levels = listOf(1, 1, 6, 11, 21, 31, 41, 61, 71, 71, 61)

    // These revision IDs form a regular run with a certificate/alternate and placeholder between
    // each pair.
    fun isFelling(id: Int) = id in 28196..28226 && (id - 28196) % 3 == 0

    fun ordinaryId(id: Int) = if (isFelling(id)) ordinary[(id - 28196) / 3].id else id
}
