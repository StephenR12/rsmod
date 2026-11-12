package org.rsmod.content.skills.crafting.hydraleather

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class HydraLeatherScript @Inject constructor(private val crafting: HydraLeatherCrafting) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(objs.hydra_leather) { crafting.inspect(this) }
        onOpLoc1(hydra_leather_locs.machinery) { crafting.investigate(this, it.loc) }
        onOpLocU(hydra_leather_locs.machinery, objs.hydra_leather) {
            crafting.transform(this, it.loc, objs.hydra_leather)
        }
        onOpLocU(hydra_leather_locs.machinery, objs.ferocious_gloves) {
            crafting.transform(this, it.loc, objs.ferocious_gloves)
        }
    }
}
