package org.rsmod.content.skills.hunter.birdhouses

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BirdhouseTrapScript @Inject constructor(private val traps: BirdhouseTrap) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(BirdhouseTrapLocs.notBuilt) { traps.buildBest(this, it.loc) }
        onOpLocU(BirdhouseTrapLocs.notBuilt) {
            val tier = BirdhouseTiers.byObjId[it.objType.id] ?: return@onOpLocU
            traps.place(this, it.loc, tier, it.invSlot)
        }
        for (tier in BirdhouseTiers.all) {
            for (loc in listOf(tier.builtLoc, tier.seededLoc)) {
                onOpLoc2(loc) { traps.fillFirstSeed(this, it.loc) }
                onOpLocU(loc) { traps.fill(this, it.loc, it.invSlot) }
            }
            onOpLoc3(tier.seededLoc) { traps.dismantlePartial(this, it.loc) }
            onOpLoc1(tier.activeLoc) { traps.harvest(this, it.loc) }
            onOpLoc3(tier.activeLoc) { traps.empty(this, it.loc) }
            onOpLoc4(tier.activeLoc) { traps.reset(this, it.loc) }
        }
    }
}
