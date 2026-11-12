package org.rsmod.content.bosses.kalphitequeen

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.death.NpcDropRolls.independent
import org.rsmod.api.death.NpcDropRolls.item
import org.rsmod.api.death.NpcDropRolls.sequence
import org.rsmod.api.death.NpcDropTable
import org.rsmod.api.death.NpcDropTableRegistry
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class KalphiteQueenDrops @Inject constructor(private val registry: NpcDropTableRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val table =
            NpcDropTable(
                sequence(
                    item(objs.shark, 2..2),
                    item(objs.coins, 15_000..30_000),
                    independent(denominator = 128, roll = item(KalphiteQueenObjs.dragonChainbody)),
                    independent(denominator = 256, roll = item(KalphiteQueenObjs.dragonTwoHander)),
                    independent(denominator = 400, roll = item(KalphiteQueenObjs.dragonPickaxe)),
                    independent(denominator = 128, roll = item(KalphiteQueenObjs.head)),
                    independent(denominator = 3_000, roll = item(KalphiteQueenObjs.pet)),
                )
            )
        registry.register(KalphiteQueenNpcs.ground, table)
        registry.register(KalphiteQueenNpcs.airborne, table)
    }
}
