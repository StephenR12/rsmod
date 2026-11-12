package org.rsmod.content.minigames.fightcaves

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

public class FightCaveScript
@Inject
constructor(
    private val controller: FightCaveController,
    private val combat: FightCaveCombat,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(FightCaveLocs.entrance) { controller.enter(this) }
        onOpLoc1(FightCaveLocs.exit) {
            startDialogue {
                val leave =
                    choice2(
                        "Yes, leave the Fight Cave.",
                        true,
                        "No, keep fighting.",
                        false,
                        title = "If you leave, you will have to start again. Are you sure?",
                    )
                if (leave) {
                    controller.leave(this@onOpLoc1)
                }
            }
        }
        onOpLoc2(FightCaveLocs.exit) { controller.leave(this) }

        FightCaveNpcs.all.forEach { type ->
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(3)
                if (npc.slotId != INVALID_SLOT) {
                    npcRepo.del(npc, Int.MAX_VALUE)
                }
            }
        }

        onEvent<NpcKillEvents.Credited> { controller.onKill(player, npc) }
        onNpcHit(FightCaveNpcs.jad) { controller.onJadHit(npc) }
        onNpcHit(FightCaveNpcs.tzKekA) { combat.recoil(npc, hit) }
        onNpcHit(FightCaveNpcs.tzKekB) { combat.recoil(npc, hit) }
        onNpcHit(FightCaveNpcs.healer) { combat.aggroHealer(npc, hit) }
        onNpcTimer(FightCaveNpcs.healer, timers.fight_cave_healer_heal) { combat.healerPulse(this) }
        onEvent<PlayerDeathEvent.Completed> { controller.failAfterDeath(player) }
        onPlayerLogout { controller.suspendOnLogout(player) }
        onPlayerLogin { controller.resumeOnLogin(player) }
    }
}
