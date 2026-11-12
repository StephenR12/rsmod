package org.rsmod.content.bosses.varlamore

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

public class VarlamoreBossScript
@Inject
constructor(
    private val controller: VarlamoreBossController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        VarlamoreBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
            onNpcHit(definition.npc) {
                if (hit.isFromPlayer) controller.onBossDamaged(npc, definition)
            }
        }
        onEvent<NpcStateEvents.Respawn> {
            VarlamoreBosses.byId[npc.id]?.let { configure(npc, it).also { controller.reset(npc) } }
        }
        VarlamoreBosses.definitions.forEach { definition ->
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
        definition: VarlamoreBossDefinition,
    ) {
        if (definition.family == VarlamoreBossFamily.GemstoneCrab) return
        with(access) {
            if (actionDelay > mapClock) return
            actionDelay = mapClock + 4
            val count = controller.state(npc).consumeAttack()
            val style =
                when (definition.family) {
                    VarlamoreBossFamily.BloodMoon -> HitType.Melee
                    VarlamoreBossFamily.BlueMoon -> HitType.Magic
                    VarlamoreBossFamily.EclipseMoon -> HitType.Ranged
                    VarlamoreBossFamily.Huey ->
                        if (count % 2 == 0) HitType.Ranged else HitType.Magic
                    VarlamoreBossFamily.GemstoneCrab -> HitType.Typeless
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            val special = count % 6 == 0
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                if (special) HitType.Typeless else style,
                damage(target, style, definition.maxHit),
            )
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

    private fun configure(npc: Npc, definition: VarlamoreBossDefinition) {
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
