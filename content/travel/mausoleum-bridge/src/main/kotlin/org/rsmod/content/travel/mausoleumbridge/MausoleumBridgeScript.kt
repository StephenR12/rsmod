package org.rsmod.content.travel.mausoleumbridge

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MausoleumBridgeScript
@Inject
constructor(private val bridge: MausoleumBridge, private val baxtorianRopes: BaxtorianFallsRopes) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            bridge.sync(player)
            baxtorianRopes.sync(player)
        }
        onOpLoc1(BaxtorianFallsRopeLocs.rockOp) { baxtorianRopes.take(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.islandBuild) { bridge.build(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.mainlandBuild) { bridge.build(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.surface) { bridge.cross(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.islandUnrepaired) { bridge.cross(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.mainlandUnrepaired) { bridge.cross(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.islandRepaired) { bridge.cross(this, it.loc) }
        onOpLoc1(MausoleumBridgeLocs.mainlandRepaired) { bridge.cross(this, it.loc) }
    }
}
