package org.rsmod.content.minigames.toa

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

public class ToaScript
@Inject
constructor(
    private val controller: ToaController,
    private val combat: ToaCombat,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ToaLocs.entrance) {
            startDialogue {
                val mode =
                    choice3(
                        "Entry Mode (raid level 0).",
                        ToaMode.Entry,
                        "Normal Mode (raid level 150).",
                        ToaMode.Normal,
                        "Expert Mode (raid level 300).",
                        ToaMode.Expert,
                        title = "Select a solo Tombs of Amascut preset.",
                    )
                controller.enter(this@onOpLoc1, mode)
            }
        }
        onOpLoc1(ToaLocs.exit) {
            startDialogue {
                if (
                    choice2(
                        "Yes, leave.",
                        true,
                        "No, continue.",
                        false,
                        title = "Abandon this Tombs attempt?",
                    )
                ) {
                    controller.leave(this@onOpLoc1)
                }
            }
        }
        onOpLoc2(ToaLocs.exit) { controller.leave(this) }

        onOpLoc1(ToaLocs.scabarasDoor) { controller.beginPath(player, ToaPath.Scabaras) }
        onOpLoc1(ToaLocs.apmekenDoor) { controller.beginPath(player, ToaPath.Apmeken) }
        onOpLoc1(ToaLocs.crondisDoor) { controller.beginPath(player, ToaPath.Crondis) }
        onOpLoc1(ToaLocs.hetDoor) { controller.beginPath(player, ToaPath.Het) }
        onOpLoc1(ToaLocs.wardensDoor) { controller.beginWardens(player) }
        onOpLoc1(ToaLocs.wardensDoorOpen) { controller.beginWardens(player) }

        ToaLocs.scabarasButtons.forEachIndexed { index, type ->
            onOpLoc1(type) { controller.pressScabaras(player, index) }
        }
        onOpLoc1(ToaLocs.crondisWater) { controller.progressCrondis(player) }
        onOpLoc2(ToaLocs.crondisWater) { controller.progressCrondis(player) }
        onOpLoc1(ToaLocs.hetGoal) { controller.progressHet(player) }
        onOpLoc2(ToaLocs.hetGoal) { controller.progressHet(player) }

        ToaNpcs.all.forEach { type ->
            onNpcHit(type) { controller.onHit(npc, hit) }
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(3)
                if (npc.slotId != INVALID_SLOT) npcRepo.del(npc, Int.MAX_VALUE)
            }
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
            onNpcTimer(type, timers.toa_pulse) { controller.pulse(this) }
        }

        onEvent<NpcKillEvents.Credited> { controller.onKill(player, npc) }
        onEvent<PlayerDeathEvent.Completed> { controller.onDeath(player) }
        onPlayerLogout { controller.suspendOnLogout(player) }
        onPlayerLogin { controller.resumeOnLogin(player) }
    }
}
