package org.rsmod.content.minigames.cox

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpHeld1
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

public class CoxScript
@Inject
constructor(
    private val controller: CoxController,
    private val combat: CoxCombat,
    private val npcRepo: NpcRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val raidSkills=listOf(stats.attack,stats.strength,stats.defence,stats.hitpoints,stats.ranged,stats.prayer,stats.magic,stats.mining,stats.woodcutting,stats.herblore,stats.farming,stats.hunter,stats.cooking,stats.fishing,stats.thieving,stats.firemaking,stats.agility)
        val skills=(raidSkills+listOf(stats.construction,stats.runecrafting,stats.smithing,stats.fletching,stats.slayer,stats.crafting)).sortedBy{it.id}
        onOpHeld1(CoxObjs.darkRelic){
            if(!player.members)return@onOpHeld1
            val skill=skills.getOrNull(menu("Choose a skill",true,skills.map{it.displayName}))?:return@onOpHeld1
            if(invDel(it.inventory,CoxObjs.darkRelic,slot=it.slot).success){
                val multiplier=if(skill in raidSkills)150 else 50
                statAdvance(skill,(player.statBase(skill)*multiplier).toDouble())
            }
        }

        onOpLoc1(CoxLocs.entrance) {
            startDialogue {
                val challenge =
                    choice2(
                        "Start a normal raid.",
                        false,
                        "Start Challenge Mode.",
                        true,
                        title = "Select a Chambers of Xeric raid.",
                    )
                controller.enter(this@onOpLoc1, challenge)
            }
        }
        onOpLoc1(CoxLocs.exit) {
            startDialogue {
                if (
                    choice2("Yes, leave.", true, "No, continue.", false, title = "Leave the raid?")
                ) {
                    controller.leave(this@onOpLoc1)
                }
            }
        }
        onOpLoc2(CoxLocs.exit) { controller.leave(this) }

        CoxNpcs.all.forEach { type ->
            onNpcHit(type) { controller.onHit(npc, hit) }
            onNpcQueue(type, queues.death) {
                noneMode()
                hideAllOps()
                delay(3)
                if (npc.slotId != INVALID_SLOT) npcRepo.del(npc, Int.MAX_VALUE)
            }
        }
        listOf(
                CoxNpcs.tekton,
                CoxNpcs.vasa,
                CoxNpcs.vanguardMelee,
                CoxNpcs.vanguardRanged,
                CoxNpcs.vanguardMagic,
                CoxNpcs.vespula,
                CoxNpcs.vespulaPortal,
                CoxNpcs.babyMuttadile,
                CoxNpcs.muttadile,
                CoxNpcs.shamanA,
                CoxNpcs.shamanB,
                CoxNpcs.mysticA,
                CoxNpcs.mysticB,
                CoxNpcs.mysticC,
                CoxNpcs.guardianLeft,
                CoxNpcs.guardianRight,
                CoxNpcs.iceDemon,
            )
            .forEach { type ->
                onAiOpPlayer2(type) { combat.attack(this, it.target) }
                onAiApPlayer2(type) { combat.attack(this, it.target) }
            }
        listOf(CoxNpcs.vasa, CoxNpcs.muttadile, CoxNpcs.olmLeft, CoxNpcs.olmHead).forEach { type ->
            onNpcTimer(type, timers.cox_pulse) {
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
