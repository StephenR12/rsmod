package org.rsmod.content.travel.fremennikseaboots

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FremennikSeaBootsScript
@Inject
constructor(private val teleport: FremennikSeaBootsTeleport) : PluginScript() {
    override fun ScriptContext.startup() {
        for (tier in FremennikSeaBootsTier.entries) {
            onOpHeld3(tier.obj) { teleport.teleport(this, tier) }
            onOpWorn2(tier.obj) { teleport.teleport(this, tier) }
        }
    }
}
