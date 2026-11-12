package org.rsmod.content.minigames.colosseum

import jakarta.inject.Inject
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ColosseumScript
@Inject
constructor(
    private val controller: ColosseumController,
    private val combat: ColosseumCombat,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(ColosseumLocs.outsideEntrance) { controller.enter(this) }
        onOpLoc1(ColosseumLocs.arenaEntrance) { controller.enter(this) }
        listOf(ColosseumLocs.arenaExit, ColosseumLocs.lobbyExit).forEach { exit ->
            onOpLoc1(exit) {
                startDialogue {
                    if (
                        choice2(
                            "Yes, leave.",
                            true,
                            "No, keep fighting.",
                            false,
                            title = "Leave the Fortis Colosseum?",
                        )
                    )
                        controller.leave(this@onOpLoc1)
                }
            }
            onOpLoc2(exit) { controller.leave(this) }
        }

        ColosseumNpcs.monsters.forEach { type ->
            onAiOpPlayer2(type) { combat.attack(this, it.target) }
            onAiApPlayer2(type) { combat.attack(this, it.target) }
            onNpcHit(type) { controller.onNpcHit(npc) }
        }
        ColosseumNpcs.all.forEach { type ->
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(3)
                if (npc.slotId != INVALID_SLOT) npcRepo.del(npc, Int.MAX_VALUE)
            }
        }
        onEvent<NpcKillEvents.Credited> { controller.onKill(player, npc) }
        onEvent<PlayerHitEvents.Impact> { controller.onPlayerHit(player, damage) }
        onEvent<PlayerDeathEvent.Completed> { controller.onDeath(player) }
        onPlayerLogout { controller.onLogout(player) }
        onPlayerLogin { controller.onLogin(player) }
    }
}
