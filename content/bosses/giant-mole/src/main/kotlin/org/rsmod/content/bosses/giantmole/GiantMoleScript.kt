package org.rsmod.content.bosses.giantmole

import jakarta.inject.Inject
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.queues
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GiantMoleScript
@Inject
constructor(
    private val combat: GiantMoleCombat,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onAiOpPlayer2(GiantMoleNpcs.boss) { combat.attack(this, it.target) }
        onAiApPlayer2(GiantMoleNpcs.boss) { combat.attack(this, it.target) }
        onNpcHit(GiantMoleNpcs.boss) { combat.considerBurrow(npc, hit) }
        onNpcQueue(GiantMoleNpcs.boss, queues.generic_queue1) { combat.burrow(this) }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id == GiantMoleNpcs.boss.id) {
                configure(npc)
            }
        }

        val alreadySpawned =
            npcRepo.findAll(GiantMoleLair.spawn).any { it.id == GiantMoleNpcs.boss.id }
        if (!alreadySpawned) {
            val mole = Npc(npcTypes[GiantMoleNpcs.boss], GiantMoleLair.spawn)
            configure(mole)
            npcRepo.add(mole, Int.MAX_VALUE)
        }
    }

    private fun configure(npc: Npc) {
        npc.apply {
            attackLvl = COMBAT_LEVEL
            baseAttackLvl = COMBAT_LEVEL
            strengthLvl = COMBAT_LEVEL
            baseStrengthLvl = COMBAT_LEVEL
            defenceLvl = COMBAT_LEVEL
            baseDefenceLvl = COMBAT_LEVEL
            hitpoints = HITPOINTS
            baseHitpointsLvl = HITPOINTS
            rangedLvl = 1
            baseRangedLvl = 1
            magicLvl = 1
            baseMagicLvl = 1
            mode = NpcMode.Wander
            setHunt(HUNT_RANGE)
            setHuntMode(huntmodes.constant_melee)
        }
    }

    public companion object {
        public const val COMBAT_LEVEL: Int = 200
        public const val HITPOINTS: Int = 200
        public const val HUNT_RANGE: Int = 12
    }
}
