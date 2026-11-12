package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DagannothKingsScript
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        DagannothKings.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
        }
        onEvent<NpcStateEvents.Respawn> { DagannothKings.byId[npc.id]?.let { configure(npc, it) } }
        DagannothKings.definitions.forEach { definition ->
            val existing = npcs.findAll(definition.spawn).firstOrNull { it.id == definition.npc.id }
            val king =
                existing
                    ?: Npc(npcTypes[definition.npc], definition.spawn).also {
                        npcs.add(it, Int.MAX_VALUE)
                    }
            configure(king, definition)
        }
    }

    private fun attack(
        access: StandardNpcAccess,
        target: Player,
        definition: DagannothKingDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            if (definition.style == HitType.Melee && !isWithinDistance(target, 1)) {
                return
            }
            actionDelay = mapClock + 4
            val accurate =
                when (definition.style) {
                    HitType.Melee ->
                        accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Slash, random)
                    HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                    HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                    HitType.Typeless -> true
                }
            val damage = if (accurate) random.of(0..definition.maxHit) else 0
            target.queueHit(
                npc,
                if (definition.style == HitType.Melee) 1 else 2,
                definition.style,
                damage,
            )
        }
    }

    private fun configure(npc: Npc, definition: DagannothKingDefinition) {
        npc.apply {
            hitpoints = 255
            baseHitpointsLvl = 255
            attackLvl = 255
            baseAttackLvl = 255
            strengthLvl = 255
            baseStrengthLvl = 255
            defenceLvl = 255
            baseDefenceLvl = 255
            magicLvl = 255
            baseMagicLvl = 255
            rangedLvl = 255
            baseRangedLvl = 255
            mode = NpcMode.Wander
            setHunt(18)
            setHuntMode(
                if (definition.style == HitType.Melee) huntmodes.constant_melee
                else huntmodes.constant_ranged
            )
        }
    }
}
