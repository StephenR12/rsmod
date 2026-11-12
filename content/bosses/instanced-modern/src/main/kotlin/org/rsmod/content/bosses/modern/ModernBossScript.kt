package org.rsmod.content.bosses.modern

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
import org.rsmod.api.script.onNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ModernBossScript
@Inject
constructor(
    private val controller: ModernBossController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        ModernBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
            onNpcHit(definition.npc) {
                if (hit.isFromPlayer) controller.onBossDamaged(npc, definition)
            }
        }
        onEvent<NpcStateEvents.Respawn> {
            ModernBosses.byId[npc.id]?.let { configure(npc, it).also { controller.reset(npc) } }
        }
        ModernBosses.definitions.forEach { definition ->
            val boss =
                npcs.findAll(definition.spawn).firstOrNull { it.id == definition.npc.id }
                    ?: Npc(npcTypes[definition.npc], definition.spawn).also {
                        npcs.add(it, Int.MAX_VALUE)
                    }
            configure(boss, definition)
            controller.reset(boss)
        }
    }

    private fun attack(
        access: StandardNpcAccess,
        target: Player,
        definition: ModernBossDefinition,
    ) =
        with(access) {
            if (actionDelay > mapClock) return@with
            val count = controller.state(npc).consumeAttack()
            actionDelay = mapClock + if (definition.family == ModernBossFamily.Doom) 4 else 5
            val style = style(definition.family, count)
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return@with
            val special = count % 7 == 0
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                if (special) HitType.Typeless else style,
                damage(target, style, definition.maxHit),
            )
        }

    private fun style(family: ModernBossFamily, count: Int): HitType =
        when (family) {
            ModernBossFamily.Amoxliatl -> HitType.Melee
            ModernBossFamily.Branda -> if (count % 3 == 0) HitType.Melee else HitType.Magic
            ModernBossFamily.Eldric -> if (count % 3 == 0) HitType.Melee else HitType.Ranged
            ModernBossFamily.Yama -> if (count % 3 == 0) HitType.Melee else HitType.Magic
            ModernBossFamily.Doom ->
                when (count % 3) {
                    0 -> HitType.Melee
                    1 -> HitType.Magic
                    else -> HitType.Ranged
                }
        }

    private fun StandardNpcAccess.damage(target: Player, style: HitType, maximum: Int): Int {
        val accurate =
            when (style) {
                HitType.Melee ->
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                HitType.Typeless -> true
            }
        return if (accurate) random.of(0..maximum) else 0
    }

    private fun configure(npc: Npc, definition: ModernBossDefinition) =
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
