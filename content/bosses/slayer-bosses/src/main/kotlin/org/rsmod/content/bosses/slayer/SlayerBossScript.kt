package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
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

public class SlayerBossScript
@Inject
constructor(
    private val controller: SlayerBossController,
    private val cerberus: CerberusCombat,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        SlayerBosses.definitions.forEach { definition ->
            onAiOpPlayer2(definition.npc) { attack(this, it.target, definition) }
            onAiApPlayer2(definition.npc) { attack(this, it.target, definition) }
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
            SlayerBosses.byId[npc.id]?.let { definition ->
                configure(npc, definition)
                controller.reset(npc)
            }
        }
        SlayerBosses.definitions.forEach { definition ->
            val spawns =
                if (definition.family == SlayerBossFamily.Cerberus)
                    CerberusRoom.entries.map { it.spawn }
                else listOf(definition.spawn)
            for (tile in spawns) {
                val boss =
                    npcs.findAll(tile).firstOrNull { it.id == definition.npc.id }
                        ?: Npc(npcTypes[definition.npc], tile).also { npcs.add(it, Int.MAX_VALUE) }
                configure(boss, definition)
                controller.reset(boss)
            }
        }
    }

    private fun attack(
        access: StandardNpcAccess,
        target: Player,
        definition: SlayerBossDefinition,
    ) {
        with(access) {
            if (actionDelay > mapClock) return
            if (definition.family == SlayerBossFamily.Cerberus) {
                cerberus.attack(npc, target)
                return
            }
            val speed =
                when (definition.family) {
                    SlayerBossFamily.Thermy -> 2
                    SlayerBossFamily.Araxxor,
                    SlayerBossFamily.Hydra -> 3
                    else -> 4
                }
            actionDelay = mapClock + speed
            val attack = controller.state(npc).consumeAttack()
            val style =
                when (definition.family) {
                    SlayerBossFamily.Cerberus -> error("Cerberus uses her own combat cycle.")
                    SlayerBossFamily.Kraken -> HitType.Magic
                    SlayerBossFamily.Thermy ->
                        if (isWithinDistance(target, 1)) HitType.Melee else HitType.Magic
                    SlayerBossFamily.Dusk -> HitType.Melee
                    SlayerBossFamily.Dawn -> if (attack % 2 == 0) HitType.Ranged else HitType.Magic
                    SlayerBossFamily.Sire ->
                        if (isWithinDistance(target, 1) && attack % 2 == 0) HitType.Melee
                        else HitType.Magic
                    SlayerBossFamily.Araxxor ->
                        if (isWithinDistance(target, 1) && attack % 3 != 0) HitType.Melee
                        else HitType.Ranged
                    SlayerBossFamily.Hydra ->
                        if ((attack / 3) % 2 == 0) HitType.Ranged else HitType.Magic
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            val special = attack % 6 == 0
            val hitType = if (special) HitType.Typeless else style
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                hitType,
                damage(target, style, if (special) definition.maxHit else definition.maxHit * 3 / 4),
            )
        }
    }

    private fun minionAttack(access: StandardNpcAccess, target: Player, type: NpcType) {
        with(access) {
            if (actionDelay > mapClock) return
            actionDelay = mapClock + 4
            val style =
                when (type.id) {
                    SlayerBossNpcs.magicSoul.id,
                    SlayerBossNpcs.krakenTentacle.id,
                    SlayerBossNpcs.acidicAraxyte.id -> HitType.Magic
                    SlayerBossNpcs.rangedSoul.id -> HitType.Ranged
                    else -> HitType.Melee
                }
            if (style == HitType.Melee && !isWithinDistance(target, 1)) return
            target.queueHit(
                npc,
                if (style == HitType.Melee) 1 else 2,
                style,
                damage(target, style, 15),
            )
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

    private fun configure(npc: Npc, definition: SlayerBossDefinition) {
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
            mode =
                if (definition.family == SlayerBossFamily.Cerberus) NpcMode.None else NpcMode.Wander
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
    }

    private companion object {
        private val minionTypes =
            listOf(
                SlayerBossNpcs.krakenTentacle,
                SlayerBossNpcs.sireSpawn,
                SlayerBossNpcs.sireScion,
                SlayerBossNpcs.mirrorback,
                SlayerBossNpcs.ruptura,
                SlayerBossNpcs.acidicAraxyte,
            )
    }
}
