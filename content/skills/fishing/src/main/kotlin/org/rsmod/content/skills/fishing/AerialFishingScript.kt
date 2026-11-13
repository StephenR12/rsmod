package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AerialFishingScript @Inject constructor(private val fishing: AerialFishing) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(AerialNpcs.alry) { fishing.talk(this) }
        onOpNpc3(AerialNpcs.alry) { fishing.shop(this) }
        onOpNpc4(AerialNpcs.alry) { fishing.borrow(this) }
        onApNpc1(AerialNpcs.pool) {
            if (isWithinApRange(it.npc, AerialFishing.LAUNCH_RANGE)) {
                if (lineOfSight(player.coords, it.npc.coords)) fishing.launch(this, it.npc)
                player.clearInteractionRoute()
            }
        }
        onOpNpc1(AerialNpcs.pool) { fishing.launch(this, it.npc); player.clearInteractionRoute() }
        for (fish in AerialFishTable.all) onOpHeldU(objs.knife, fish.obj) { fishing.cut(this, fish) }
        onPlayerSoftTimer(AerialTimers.lifecycle) { fishing.tick(player) }
        onPlayerLogin { fishing.login(player) }
        onPlayerLogout { fishing.logout(player) }
        onAiTimer(AerialNpcs.pool) { fishing.movePool(npc) }
        onOpLoc1(AerialLocs.worms) { if (player.members) invAdd(inv, AerialObjs.worm, 1) }
        onOpLoc1(AerialLocs.boat) {
            if (!player.members) return@onOpLoc1
            val destination = when (menu("Where would you like to row?", "Molch Island", "Molch", "Battlefront", "Shayzien")) {
                1 -> CoordGrid(1368, 3638)
                2 -> CoordGrid(1342, 3645)
                3 -> CoordGrid(1383, 3665)
                4 -> CoordGrid(1408, 3612)
                else -> return@onOpLoc1
            }
            if (!AerialFishing.inside(destination)) fishing.leave(player)
            telejump(destination)
        }
    }
}
