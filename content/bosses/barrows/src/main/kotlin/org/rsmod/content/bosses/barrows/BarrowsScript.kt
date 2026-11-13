package org.rsmod.content.bosses.barrows

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcKillEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.HitType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BarrowsScript
@Inject
constructor(
    private val controller: BarrowsController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val tunnels: BarrowsTunnels,
) : PluginScript() {
    override fun ScriptContext.startup() {
        BarrowsBrothers.all.forEach { brother ->
            onAiOpPlayer2(brother.npc) { attack(this, it.target, brother) }
            onAiApPlayer2(brother.npc) { attack(this, it.target, brother) }
        }
        onEvent<NpcKillEvents.Credited> {
            if (tunnels.credited(player, npc)) controller.onKill(player, npc)
        }
        onOpLoc1(BarrowsLocs.rewardChest) { tunnels.chest(this) }
    }

    private fun attack(access: StandardNpcAccess, target: Player, brother: BarrowsBrother) {
        with(access) {
            if (
                actionDelay > mapClock ||
                    (brother.style == HitType.Melee && !isWithinDistance(target, 1))
            ) {
                return
            }
            actionDelay = mapClock + 5
            val style =
                if (npc.id == BarrowsNpcs.verac.id && random.of(4) == 0) HitType.Typeless
                else brother.style
            val accurate =
                when (style) {
                    HitType.Melee ->
                        accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Slash, random)
                    HitType.Ranged -> accuracy.rollRangedAccuracy(npc, target, random)
                    HitType.Magic -> accuracy.rollMagicAccuracy(npc, target, random)
                    HitType.Typeless -> true
                }
            var maxHit = brother.maxHit
            if (npc.id == BarrowsNpcs.dharok.id) {
                val missing = npc.baseHitpointsLvl - npc.hitpoints
                maxHit += missing * brother.maxHit / npc.baseHitpointsLvl
            }
            val damage = if (accurate) random.of(0..maxHit) else 0
            target.queueHit(npc, if (style == HitType.Melee) 1 else 2, style, damage)
            when {
                npc.id == BarrowsNpcs.guthan.id && random.of(4) == 0 ->
                    npc.hitpoints = min(npc.baseHitpointsLvl, npc.hitpoints + damage)
                npc.id == BarrowsNpcs.torag.id && random.of(4) == 0 ->
                    target.runEnergy = (target.runEnergy - 200).coerceAtLeast(0)
                npc.id == BarrowsNpcs.ahrim.id && random.of(4) == 0 ->
                    target.statSub(stats.strength, constant = 5, percent = 0)
                npc.id == BarrowsNpcs.karil.id && random.of(4) == 0 ->
                    target.statSub(stats.magic, constant = 5, percent = 0)
            }
        }
    }
}

internal fun configureBrother(npc: Npc, brother: BarrowsBrother) {
    npc.apply {
        hitpoints = 100
        baseHitpointsLvl = 100
        attackLvl = 115
        baseAttackLvl = 115
        strengthLvl = 115
        baseStrengthLvl = 115
        defenceLvl = 100
        baseDefenceLvl = 100
        magicLvl = if (brother.style == HitType.Magic) 120 else 1
        baseMagicLvl = magicLvl
        rangedLvl = if (brother.style == HitType.Ranged) 120 else 1
        baseRangedLvl = rangedLvl
        mode = NpcMode.None
    }
}
