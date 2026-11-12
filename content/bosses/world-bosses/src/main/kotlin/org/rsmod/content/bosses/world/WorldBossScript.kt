package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import kotlin.math.max
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
import org.rsmod.api.script.onNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class WorldBossScript
@Inject
constructor(
    private val controller: WorldBossController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        WorldBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
            onNpcHit(definition.npc) { controller.onHit(npc, hit, definition) }
        }
        listOf(
                WorldBossNpcs.sarachnisMeleeSpawn,
                WorldBossNpcs.sarachnisMagicSpawn,
                WorldBossNpcs.giantRat,
            )
            .forEach { type ->
                onAiOpPlayer2(type) { minionAttack(this, it.target) }
                onAiApPlayer2(type) { minionAttack(this, it.target) }
            }
        onEvent<NpcStateEvents.Respawn> {
            WorldBosses.byId[npc.id]?.let { configure(npc, it).also { controller.reset(npc) } }
        }
        WorldBosses.definitions.forEach { definition ->
            val existing = npcs.findAll(definition.spawn).firstOrNull { it.id == definition.npc.id }
            val boss =
                existing
                    ?: Npc(npcTypes[definition.npc], definition.spawn).also {
                        npcs.add(it, Int.MAX_VALUE)
                    }
            configure(boss, definition)
            controller.reset(boss)
        }
    }

    private fun attack(access: StandardNpcAccess, target: Player, definition: WorldBossDefinition) {
        with(access) {
            if (actionDelay > mapClock) return
            actionDelay = mapClock + 4
            val state = controller.state(npc)
            state.attacks++
            val style =
                when (definition.family) {
                    WorldBossFamily.Archaeologist -> HitType.Magic
                    WorldBossFamily.Sarachnis ->
                        if (isWithinDistance(target, 1)) HitType.Melee else HitType.Magic
                    WorldBossFamily.Scurrius ->
                        when (state.attacks % 3) {
                            0 -> HitType.Melee
                            1 -> HitType.Ranged
                            else -> HitType.Magic
                        }
                    WorldBossFamily.Maledictus ->
                        if (state.attacks % 2 == 0) HitType.Ranged else HitType.Magic
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            val special = state.attacks % 5 == 0
            val hitType =
                if (special && definition.family != WorldBossFamily.Sarachnis) HitType.Typeless
                else style
            val maxHit = if (special) definition.maxHit else (definition.maxHit * 3 / 4)
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                hitType,
                damage(target, style, maxHit),
            )
            if (special && definition.family == WorldBossFamily.Sarachnis) {
                target.delay = max(target.delay, mapClock + 3)
            }
        }
    }

    private fun minionAttack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock || !isWithinDistance(target, 1)) return
            actionDelay = mapClock + 4
            target.queueHit(npc, 1, HitType.Melee, random.of(0..8))
        }
    }

    private fun StandardNpcAccess.damage(target: Player, style: HitType, maxHit: Int): Int {
        val accurate =
            when (style) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        return if (accurate) random.of(0..maxHit) else 0
    }

    private fun configure(npc: Npc, definition: WorldBossDefinition) {
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
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
    }
}
