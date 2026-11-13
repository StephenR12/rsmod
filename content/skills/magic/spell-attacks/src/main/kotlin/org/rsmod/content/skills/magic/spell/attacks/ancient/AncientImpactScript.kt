package org.rsmod.content.skills.magic.spell.attacks.ancient

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.stat.reduceIncomingHealing
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.api.script.onNpcQueueWithArgs
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.magic.spell.attacks.ancient.config.ancient_queues
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStateRepository
import org.rsmod.content.skills.magic.spell.attacks.standard.BindingStatus
import org.rsmod.content.skills.magic.spell.utility.standard.PoisonProtectionRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class AncientImpactScript
@Inject
constructor(
    private val bindings: BindingStateRepository,
    private val playerPoison: PoisonProtectionRepository,
    private val npcPoison: AncientNpcPoisonRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs<AncientImpact>(ancient_queues.impact) { apply(player, it.args) }
        onNpcQueueWithArgs<AncientImpact>(ancient_queues.impact) { apply(npc, it.args) }
        onEvent<NpcStateEvents.Respawn> { npcPoison.clear(npc) }
        onNpcQueueWithArgs<NpcPoisonTick>(ancient_queues.poison) { npcPoison.tick(npc) }
    }

    public fun apply(target: PathingEntity, impact: AncientImpact) {
        when (impact.family) {
            AncientSpellFamily.Smoke -> applySmoke(target, impact)
            AncientSpellFamily.Shadow -> applyShadow(target, impact)
            AncientSpellFamily.Blood -> applyBlood(impact)
            AncientSpellFamily.Ice -> applyIce(target, impact)
        }
    }

    private fun applySmoke(target: PathingEntity, impact: AncientImpact) {
        if (impact.poisonProc) {
            val severity =
                if (impact.ancientSceptre) impact.effectStrength * 11 / 10
                else impact.effectStrength
            when (target) {
                is Player -> playerPoison.inflictPoison(target, severity)
                is Npc -> npcPoison.inflict(target, severity)
            }
        }

        if (impact.familySceptre && impact.damage > 0 && target is Player) {
            if (target.vars[varps.poison] > 0) {
                target.reduceIncomingHealing(percent = 20, duration = 10)
            }
        }
    }

    private fun applyShadow(target: PathingEntity, impact: AncientImpact) {
        val permille =
            if (impact.ancientSceptre) impact.effectStrength * 11 else impact.effectStrength * 10
        drainIfRestored(target, stats.attack, permille)
        if (impact.familySceptre) {
            drainIfRestored(target, stats.strength, permille)
            drainIfRestored(target, stats.defence, permille)
        }
    }

    private fun applyBlood(impact: AncientImpact) {
        var healPermille = 250 + (impact.bloodbarkPieces * 20)
        if (impact.ancientSceptre) healPermille = healPermille * 11 / 10
        val heal = impact.damage * healPermille / 1000
        if (heal <= 0) return

        val source = impact.source
        val base = source.statBase(stats.hitpoints)
        val cap = if (impact.familySceptre) base + (base / 10) else base
        val actual = min(heal, max(0, cap - source.stat(stats.hitpoints)))
        if (actual > 0) source.statAdd(stats.hitpoints, constant = actual, percent = 0)
    }

    private fun applyIce(target: PathingEntity, impact: AncientImpact) {
        var duration =
            if (impact.ancientSceptre) impact.effectStrength * 11 / 10 else impact.effectStrength
        if (target is Npc) {
            val resistance = target.visType.param(params.freeze_resistance).coerceIn(0, 100)
            duration = duration * (100 - resistance) / 100
        }
        if (duration <= 0) return
        if (bindings.status(target) == BindingStatus.Available) {
            bindings.bind(target, impact.source, duration)
        }
    }

    private fun drainIfRestored(
        target: PathingEntity,
        stat: org.rsmod.game.type.stat.StatType,
        permille: Int,
    ) {
        when (target) {
            is Player -> {
                val base = target.statBase(stat)
                if (target.stat(stat) >= base) {
                    target.statSub(stat, constant = base * permille / 1000, percent = 0)
                }
            }
            is Npc -> {
                val base = target.baseLevel(stat)
                val current = target.currentLevel(stat)
                if (current >= base) target.setCurrentLevel(stat, base - (base * permille / 1000))
            }
        }
    }

    private fun Npc.baseLevel(stat: org.rsmod.game.type.stat.StatType): Int =
        when {
            stat.isType(stats.attack) -> baseAttackLvl
            stat.isType(stats.strength) -> baseStrengthLvl
            stat.isType(stats.defence) -> baseDefenceLvl
            else -> error("Unsupported Ancient shadow stat: $stat")
        }

    private fun Npc.currentLevel(stat: org.rsmod.game.type.stat.StatType): Int =
        when {
            stat.isType(stats.attack) -> attackLvl
            stat.isType(stats.strength) -> strengthLvl
            stat.isType(stats.defence) -> defenceLvl
            else -> error("Unsupported Ancient shadow stat: $stat")
        }

    private fun Npc.setCurrentLevel(stat: org.rsmod.game.type.stat.StatType, value: Int) {
        when {
            stat.isType(stats.attack) -> attackLvl = value
            stat.isType(stats.strength) -> strengthLvl = value
            stat.isType(stats.defence) -> defenceLvl = value
            else -> error("Unsupported Ancient shadow stat: $stat")
        }
    }
}
