package org.rsmod.content.skills.hunter.falconry

import jakarta.inject.Inject
import org.rsmod.api.player.clearInteractionRoute
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FalconryScript @Inject constructor(private val falconry: Falconry) : PluginScript() {
    override fun ScriptContext.startup() {
        for (matthias in listOf(FalconryNpcs.matthias, FalconryNpcs.matthiasNoBird)) {
            onOpNpc1(matthias) { falconry.talk(this) }
            onOpNpc3(matthias) { falconry.rent(this) }
        }
        for (species in FalconrySpeciesList.all) {
            onApNpc1(species.npc) {
                if (isWithinApRange(it.npc, Falconry.LAUNCH_RANGE)) {
                    if (lineOfSight(player.coords, it.npc.coords)) falconry.launch(this, it.npc)
                    player.clearInteractionRoute()
                    player.clearInteraction()
                }
            }
            onOpNpc1(species.npc) { falconry.launch(this, it.npc); player.clearInteraction() }
            onOpNpc1(species.caught) { falconry.retrieve(this, it.npc) }
        }
        onPlayerSoftTimer(FalconryTimers.lifecycle) { falconry.tick(player) }
        onPlayerLogout { falconry.logout(player) }
        onPlayerLogin { falconry.login(player) }
    }
}
