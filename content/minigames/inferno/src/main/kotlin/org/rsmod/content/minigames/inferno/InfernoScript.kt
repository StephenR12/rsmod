package org.rsmod.content.minigames.inferno

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
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class InfernoScript
@Inject
constructor(
    private val controller: InfernoController,
    private val combat: InfernoCombat,
    private val npcRepo: NpcRepository,
    private val players: PlayerList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(InfernoLocs.entrance) {
            if (controller.hasUnlocked(player)) {
                controller.enter(this)
            } else {
                startDialogue {
                    val sacrifice =
                        choice2(
                            "Sacrifice a fire cape.",
                            true,
                            "No, not yet.",
                            false,
                            title = "A fire cape must be sacrificed permanently to enter.",
                        )
                    if (sacrifice && controller.sacrificeCape(this@onOpLoc1)) {
                        controller.enter(this@onOpLoc1)
                    }
                }
            }
        }
        onOpLoc1(InfernoLocs.exit) {
            startDialogue {
                if (
                    choice2(
                        "Yes, leave.",
                        true,
                        "No, keep fighting.",
                        false,
                        title = "End this Inferno attempt?",
                    )
                ) {
                    controller.leave(this@onOpLoc1)
                }
            }
        }
        onOpLoc2(InfernoLocs.exit) { controller.leave(this) }

        InfernoNpcs.combatants.forEach { type ->
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
        }
        InfernoNpcs.all.forEach { type ->
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(3)
                if (npc.slotId != INVALID_SLOT) npcRepo.del(npc, Int.MAX_VALUE)
            }
        }
        listOf(InfernoNpcs.jadHealer, InfernoNpcs.finalJadHealer).forEach { type ->
            onNpcTimer(type, timers.inferno_healer_heal) { controller.healerPulse(this) }
            onNpcHit(type) {
                controller.aggroHealer(npc, hit.resolvePlayerSource(players) ?: return@onNpcHit)
            }
        }
        listOf(
                InfernoNpcs.nibbler,
                InfernoNpcs.mager,
                InfernoNpcs.shield,
                InfernoNpcs.zuk,
                InfernoNpcs.zukHealer,
                InfernoNpcs.finalRanger,
                InfernoNpcs.finalMager,
            )
            .forEach { type ->
                onNpcTimer(type, timers.inferno_pulse) {
                    controller.pulse(this)
                    combat.pulse(this)
                }
            }
        listOf(
                InfernoNpcs.jad,
                InfernoNpcs.finalJad,
                InfernoNpcs.zuk,
                InfernoNpcs.zukHealer,
                InfernoNpcs.finalRanger,
                InfernoNpcs.finalMager,
            )
            .forEach { type -> onNpcHit(type) { controller.onNpcHit(npc) } }

        onEvent<NpcKillEvents.Credited> { controller.onKill(player, npc) }
        onEvent<PlayerDeathEvent.Completed> { controller.failAfterDeath(player) }
        onPlayerLogout { controller.suspendOnLogout(player) }
        onPlayerLogin { controller.resumeOnLogin(player) }
    }
}
