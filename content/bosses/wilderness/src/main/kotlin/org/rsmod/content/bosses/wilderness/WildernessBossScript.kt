package org.rsmod.content.bosses.wilderness

import jakarta.inject.Inject
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WildernessBossScript
@Inject
constructor(
    private val combat: WildernessBossCombat,
    private val controller: WildernessBossController,
    private val access: WildernessBossAccess,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        WildernessBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { combat.attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { combat.attack(this, it.target, definition) }
            onModifyNpcHit(definition.npc) { controller.modifyHit(npc, hit, definition) }
            onNpcHit(definition.npc) { controller.onHit(npc, hit, definition) }
        }
        MINIONS.forEach { type ->
            onAiOpPlayer2(type) { combat.minionAttack(this, it.target) }
            onAiApPlayer2(type) { combat.minionAttack(this, it.target) }
        }
        ENTRANCES.forEach { (loc, boss) -> onOpLoc1(loc) { access.enter(this, boss) } }
        onEvent<NpcStateEvents.Respawn> {
            WildernessBosses.byId[npc.id]?.let { definition ->
                configure(npc, definition)
                controller.reset(npc)
            }
        }
        WildernessBosses.definitions.forEach { definition ->
            spawnPermanent(definition.npc, definition.spawn) { configure(it, definition) }
                .also(controller::reset)
        }
    }

    private fun spawnPermanent(type: NpcType, coords: CoordGrid, configure: (Npc) -> Unit): Npc {
        val existing = npcs.findAll(coords).firstOrNull { it.id == type.id }
        if (existing != null) {
            configure(existing)
            return existing
        }
        return Npc(npcTypes[type], coords).also { npc ->
            configure(npc)
            npcs.add(npc, Int.MAX_VALUE)
        }
    }

    private fun configure(npc: Npc, definition: WildernessBossDefinition) {
        npc.apply {
            hitpoints = definition.hitpoints
            baseHitpointsLvl = definition.hitpoints
            attackLvl = definition.attack
            baseAttackLvl = definition.attack
            strengthLvl = definition.strength
            baseStrengthLvl = definition.strength
            defenceLvl = definition.defence
            baseDefenceLvl = definition.defence
            magicLvl = definition.magic
            baseMagicLvl = definition.magic
            rangedLvl = definition.ranged
            baseRangedLvl = definition.ranged
            mode = NpcMode.Wander
            setHunt(HUNT_RANGE)
            setHuntMode(huntmodes.constant_ranged)
        }
    }

    public companion object {
        public const val HUNT_RANGE: Int = 18
        public val MINIONS: List<NpcType> =
            listOf(
                WildernessBossNpcs.juniorHound,
                WildernessBossNpcs.seniorHound,
                WildernessBossNpcs.juniorHoundSingle,
                WildernessBossNpcs.seniorHoundSingle,
                WildernessBossNpcs.venenatisSpiderling,
                WildernessBossNpcs.spindelSpiderling,
                WildernessBossNpcs.scorpiaGuardian,
            )
        public val ENTRANCES =
            mapOf(
                WildernessBossLocs.callistoEntrance to
                    WildernessBosses.byId.getValue(WildernessBossNpcs.callisto.id),
                WildernessBossLocs.artioEntrance to
                    WildernessBosses.byId.getValue(WildernessBossNpcs.artio.id),
                WildernessBossLocs.venenatisEntrance to
                    WildernessBosses.byId.getValue(WildernessBossNpcs.venenatis.id),
                WildernessBossLocs.spindelEntrance to
                    WildernessBosses.byId.getValue(WildernessBossNpcs.spindel.id),
                WildernessBossLocs.vetionEntrance to
                    WildernessBosses.byId.getValue(WildernessBossNpcs.vetion.id),
                WildernessBossLocs.calvarionEntrance to
                    WildernessBosses.byId.getValue(WildernessBossNpcs.calvarion.id),
            )
    }
}
