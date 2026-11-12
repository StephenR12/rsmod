package org.rsmod.content.other.combatachievements

import jakarta.inject.Inject
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.events.CombatAchievementEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CombatAchievementScript
@Inject
constructor(private val service: CombatAchievementService) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { service.refresh(player) }
        onEvent<CombatAchievementEvents.TaskSatisfied> { service.complete(player, taskId) }
        onEvent<CombatAchievementEvents.EncounterCompleted> { service.process(this) }
        onEvent<NpcKillEvents.Credited> {
            service.process(
                CombatAchievementEvents.EncounterCompleted(
                    player = player,
                    encounter = npc.visType.name,
                )
            )
        }
        onCommand("ca") {
            desc = "Show Combat Achievement progress or inspect a task"
            cheat(::command)
        }
    }

    private fun command(cheat: Cheat) =
        with(cheat) {
            if (args.isEmpty() || args.first().equals("progress", ignoreCase = true)) {
                val progress = service.refresh(player)
                player.mes(
                    "Combat Achievements: ${progress.completed}/${progress.total} tasks, ${progress.points} points."
                )
                for (tier in CombatAchievementTier.entries) {
                    val complete = progress.byTier.getValue(tier)
                    val total = service.catalogue.tasks.count { it.tier == tier }
                    val threshold = service.catalogue.threshold(tier)
                    player.mes(
                        "${tier.name}: $complete/$total tasks; tier unlocks at $threshold points."
                    )
                }
                return@with
            }
            val query = args.joinToString(" ").removePrefix("task ")
            val task = service.catalogue.find(query)
            if (task == null) {
                player.mes("No Combat Achievement matches '$query'.")
                return@with
            }
            val state = if (service.isCompleted(player, task)) "Complete" else "Incomplete"
            player.mes("[${task.id}] ${task.name} (${task.tier.name}) - $state")
            player.mes(task.description)
        }
}
