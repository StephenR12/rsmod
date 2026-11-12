package org.rsmod.content.bosses.sporadic

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcHit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class SporadicBossScript
@Inject
constructor(
    private val controller: SporadicBossController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        SporadicBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
            onModifyNpcHit(definition.npc) { controller.modifyHit(npc, hit, definition) }
            onNpcHit(definition.npc) {
                if (hit.isFromPlayer) controller.onBossDamaged(npc, definition)
            }
        }
        minionTypes.forEach { type ->
            onAiOpPlayer2(type) { minionAttack(this, it.target, type) }
            onAiApPlayer2(type) { minionAttack(this, it.target, type) }
        }
        onEvent<NpcKillEvents.Credited> { controller.onMinionKilled(npc) }
        onEvent<NpcStateEvents.Respawn> {
            SporadicBosses.byId[npc.id]?.let { definition ->
                configure(npc, definition)
                controller.reset(npc)
            }
        }
        SporadicBosses.definitions.forEach { definition ->
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

    private fun attack(
        access: StandardNpcAccess,
        target: Player,
        definition: SporadicBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) return
            actionDelay = mapClock + 4
            val attack = controller.state(npc).consumeAttack()
            val style = style(definition, target, attack)
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            val special = attack % SPECIAL_INTERVAL == 0
            val hitType = if (special) HitType.Typeless else style
            val maxHit = if (special) definition.maxHit else definition.maxHit * 3 / 4
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                hitType,
                damage(target, style, maxHit),
            )
            if (special && definition.family in immobilisingBosses) {
                target.delay = max(target.delay, mapClock + 3)
            }
        }
    }

    private fun StandardNpcAccess.style(
        definition: SporadicBossDefinition,
        target: Player,
        attack: Int,
    ): HitType =
        when (definition.family) {
            SporadicBossFamily.Obor ->
                if (isWithinDistance(target, 1)) HitType.Melee else HitType.Ranged
            SporadicBossFamily.Bryophyta ->
                if (isWithinDistance(target, 1) && attack % 2 == 0) HitType.Melee else HitType.Magic
            SporadicBossFamily.Mimic ->
                when (attack % 3) {
                    0 -> HitType.Melee
                    1 -> HitType.Ranged
                    else -> HitType.Magic
                }
            SporadicBossFamily.Hespori -> if (attack % 2 == 0) HitType.Ranged else HitType.Magic
            SporadicBossFamily.Skotizo ->
                if (isWithinDistance(target, 1) && attack % 2 == 0) HitType.Melee else HitType.Magic
        }

    private fun minionAttack(access: StandardNpcAccess, target: Player, type: NpcType) {
        with(access) {
            if (actionDelay > mapClock) return
            actionDelay = mapClock + 4
            val style =
                when (type.id) {
                    SporadicBossNpcs.thirdAgeRanger.id -> HitType.Ranged
                    SporadicBossNpcs.thirdAgeMage.id,
                    SporadicBossNpcs.hesporiFlower.id -> HitType.Magic
                    else -> HitType.Melee
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                style,
                damage(target, style, 10),
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

    private fun configure(npc: Npc, definition: SporadicBossDefinition) {
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

    public companion object {
        public const val SPECIAL_INTERVAL: Int = 5
        private val immobilisingBosses =
            setOf(SporadicBossFamily.Obor, SporadicBossFamily.Bryophyta, SporadicBossFamily.Hespori)
        private val minionTypes =
            listOf(
                SporadicBossNpcs.growthling,
                SporadicBossNpcs.thirdAgeWarrior,
                SporadicBossNpcs.thirdAgeRanger,
                SporadicBossNpcs.thirdAgeMage,
                SporadicBossNpcs.hesporiFlower,
                SporadicBossNpcs.skotizoDemon,
                SporadicBossNpcs.northAltar,
                SporadicBossNpcs.eastAltar,
                SporadicBossNpcs.southAltar,
                SporadicBossNpcs.westAltar,
            )
    }
}
