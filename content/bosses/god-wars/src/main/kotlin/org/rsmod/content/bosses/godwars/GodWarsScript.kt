package org.rsmod.content.bosses.godwars

import jakarta.inject.Inject
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GodWarsScript
@Inject
constructor(
    private val combat: GodWarsCombat,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        GodWarsDungeon.combatants.forEach { config ->
            onAiOpPlayer2(config.npc) { combat.attack(this, it.target, config) }
            onAiApPlayer2(config.npc) { combat.attack(this, it.target, config) }
        }
        onEvent<NpcStateEvents.Respawn> {
            GodWarsDungeon.byNpcId[npc.id]?.let { configure(npc, it) }
        }

        GodWarsDungeon.combatants.forEach { config ->
            val exists = npcRepo.findAll(config.spawn).any { it.id == config.npc.id }
            if (!exists) {
                val npc = Npc(npcTypes[config.npc], config.spawn)
                configure(npc, config)
                npcRepo.add(npc, Int.MAX_VALUE)
            }
        }
    }

    private fun configure(npc: Npc, config: GodWarsCombatant) {
        npc.apply {
            config.stats?.let { stats ->
                hitpoints = stats.hitpoints
                baseHitpointsLvl = stats.hitpoints
                attackLvl = stats.attack
                baseAttackLvl = stats.attack
                strengthLvl = stats.strength
                baseStrengthLvl = stats.strength
                defenceLvl = stats.defence
                baseDefenceLvl = stats.defence
                rangedLvl = stats.ranged
                baseRangedLvl = stats.ranged
                magicLvl = stats.magic
                baseMagicLvl = stats.magic
            }
            mode = NpcMode.Wander
            setHunt(HUNT_RANGE)
            setHuntMode(
                if (config.style == GodWarsStyle.Melee) huntmodes.constant_melee
                else huntmodes.constant_ranged
            )
        }
    }

    private companion object {
        const val HUNT_RANGE = 18
    }
}
