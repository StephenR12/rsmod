package org.rsmod.content.minigames.tob

import jakarta.inject.Inject
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.timers
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TobScript
@Inject
constructor(
    private val controller: TobController,
    private val combat: TobCombat,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(TobLocs.entrance) {
            startDialogue {
                val mode =
                    choice3(
                        "Entry Mode.",
                        TobMode.Entry,
                        "Normal Mode.",
                        TobMode.Normal,
                        "Hard Mode.",
                        TobMode.Hard,
                        title = "Select a Theatre of Blood mode.",
                    )
                controller.enter(this@onOpLoc1, mode)
            }
        }
        onOpLoc1(TobLocs.exit) {
            startDialogue {
                if (
                    choice2(
                        "Yes, leave.",
                        true,
                        "No, continue.",
                        false,
                        title = "Abandon this Theatre attempt?",
                    )
                ) {
                    controller.leave(this@onOpLoc1)
                }
            }
        }
        onOpLoc2(TobLocs.exit) { controller.leave(this) }
        onOpLoc1(TobLocs.midwayChest) { controller.claimSupplies(this) }
        onOpLoc1(TobLocs.midwayChestOpen) { controller.claimSupplies(this) }

        TobNpcs.all.forEach { type ->
            onNpcHit(type) { controller.onHit(npc, hit) }
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(3)
                if (npc.slotId != INVALID_SLOT) npcRepo.del(npc, Int.MAX_VALUE)
            }
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
        }

        TobNpcs.all.forEach { type ->
            onNpcTimer(type, timers.tob_pulse) {
                controller.pulse(this)
                combat.pulse(this)
            }
        }

        onEvent<NpcKillEvents.Credited> { controller.onKill(player, npc) }
        onEvent<PlayerDeathEvent.Completed> { controller.onDeath(player) }
        onPlayerLogout { controller.suspendOnLogout(player) }
        onPlayerLogin { controller.resumeOnLogin(player) }
    }
}
