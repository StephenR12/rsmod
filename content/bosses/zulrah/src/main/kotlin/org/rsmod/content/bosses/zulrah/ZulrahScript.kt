package org.rsmod.content.bosses.zulrah

import jakarta.inject.Inject
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ZulrahScript
@Inject
constructor(private val controller: ZulrahController, private val combat: ZulrahCombat) :
    PluginScript() {
    override fun ScriptContext.startup() {
        ZulrahLocs.boats.forEach { boat ->
            onOpLoc1(boat) { controller.enter(this) }
            onOpLoc2(boat) { controller.enter(this) }
        }
        onOpLoc1(ZulrahLocs.exit) { controller.leave(this) }
        onOpLoc2(ZulrahLocs.exit) { controller.leave(this) }

        ZulrahNpcs.all.forEach { type ->
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                if (type in ZulrahNpcs.bosses) {
                    anim(ZulrahSeqs.death)
                    delay(3)
                } else {
                    delay(1)
                }
                controller.removeNpc(npc)
            }
        }
        ZulrahNpcs.bosses.forEach { type ->
            onNpcQueue(type, queues.generic_queue1) { combat.transition(this) }
        }

        onEvent<NpcKillEvents.Credited> { controller.onKill(player, npc) }
        onEvent<PlayerDeathEvent.Completed> { controller.failAfterDeath(player) }
        onPlayerLogout { controller.onLogout(player) }
        onPlayerLogin { controller.onLogin(player) }
    }
}
