package org.rsmod.content.bosses.varlamore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class VarlamoreBossDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        registry.register(
            VarlamoreBossNpcs.huey,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 50_000..150_000),
                    independent(denominator = 105, roll = item(VarlamoreBossObjs.dragonHunterWand)),
                    independent(denominator = 30, roll = item(VarlamoreBossObjs.hueyHide, 3..3)),
                    independent(denominator = 90, roll = item(VarlamoreBossObjs.tomeOfEarth)),
                    independent(denominator = 400, roll = item(VarlamoreBossObjs.huberte)),
                )
            ),
        )
        registry.register(
            VarlamoreBossNpcs.gemstoneCrab,
            NpcDropTable(
                sequence(
                    item(VarlamoreBossObjs.uncutSapphire, 5..15),
                    item(VarlamoreBossObjs.uncutEmerald, 3..10),
                    independent(denominator = 4, roll = item(VarlamoreBossObjs.uncutRuby, 2..8)),
                    independent(denominator = 8, roll = item(VarlamoreBossObjs.uncutDiamond, 1..5)),
                )
            ),
        )
        registerMoon(
            VarlamoreBossNpcs.bloodMoon,
            listOf(
                VarlamoreBossObjs.dualMacuahuitl,
                VarlamoreBossObjs.bloodHelm,
                VarlamoreBossObjs.bloodBody,
                VarlamoreBossObjs.bloodLegs,
            ),
        )
        registerMoon(
            VarlamoreBossNpcs.blueMoon,
            listOf(
                VarlamoreBossObjs.blueSpear,
                VarlamoreBossObjs.blueHelm,
                VarlamoreBossObjs.blueBody,
                VarlamoreBossObjs.blueLegs,
            ),
        )
        registerMoon(
            VarlamoreBossNpcs.eclipseMoon,
            listOf(
                VarlamoreBossObjs.eclipseAtlatl,
                VarlamoreBossObjs.eclipseHelm,
                VarlamoreBossObjs.eclipseBody,
                VarlamoreBossObjs.eclipseLegs,
            ),
        )
    }

    private fun registerMoon(npc: NpcType, uniques: List<ObjType>) {
        registry.register(
            npc,
            NpcDropTable(
                sequence(
                    item(objs.big_bones),
                    item(objs.coins, 10_000..40_000),
                    *uniques.map { independent(denominator = 224, roll = item(it)) }.toTypedArray(),
                )
            ),
        )
    }
}
