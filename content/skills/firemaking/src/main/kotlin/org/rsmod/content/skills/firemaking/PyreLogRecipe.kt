package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

public data class SacredOil(val obj: ObjType, val doses: Int)

public object SacredOils {
    public val all: List<SacredOil> by lazy {
        listOf(
            SacredOil(objs.sacred_oil_4, 4),
            SacredOil(objs.sacred_oil_3, 3),
            SacredOil(objs.sacred_oil_2, 2),
            SacredOil(objs.sacred_oil_1, 1),
        )
    }

    public val byObj: Map<Int, SacredOil> by lazy { all.associateBy { it.obj.id } }

    public fun withDoses(doses: Int): SacredOil? = all.firstOrNull { it.doses == doses }
}

public data class PyreLogRecipe(val input: ObjType, val output: ObjType, val oilDoses: Int) {
    public val xp: Double
        get() = oilDoses * XP_PER_DOSE

    private companion object {
        const val XP_PER_DOSE = 5.0
    }
}

public object PyreLogRecipes {
    public val all: List<PyreLogRecipe> by lazy {
        listOf(
            PyreLogRecipe(objs.logs, objs.pyre_logs, oilDoses = 2),
            PyreLogRecipe(objs.achey_tree_logs, objs.pyre_logs, oilDoses = 2),
            PyreLogRecipe(objs.oak_logs, objs.oak_pyre_logs, oilDoses = 2),
            PyreLogRecipe(objs.arctic_pine_logs, objs.arctic_pine_pyre_logs, oilDoses = 2),
            PyreLogRecipe(objs.willow_logs, objs.willow_pyre_logs, oilDoses = 3),
            PyreLogRecipe(objs.teak_logs, objs.teak_pyre_logs, oilDoses = 3),
            PyreLogRecipe(objs.maple_logs, objs.maple_pyre_logs, oilDoses = 3),
            PyreLogRecipe(objs.mahogany_logs, objs.mahogany_pyre_logs, oilDoses = 3),
            PyreLogRecipe(objs.yew_logs, objs.yew_pyre_logs, oilDoses = 4),
            PyreLogRecipe(objs.magic_logs, objs.magic_pyre_logs, oilDoses = 4),
            PyreLogRecipe(objs.redwood_logs, objs.redwood_pyre_logs, oilDoses = 4),
        )
    }

    public val byInput: Map<Int, PyreLogRecipe> by lazy { all.associateBy { it.input.id } }
}
