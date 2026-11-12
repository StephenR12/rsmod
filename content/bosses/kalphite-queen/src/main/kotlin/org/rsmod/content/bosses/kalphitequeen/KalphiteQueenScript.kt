package org.rsmod.content.bosses.kalphitequeen

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
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
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class KalphiteQueenScript
@Inject
constructor(
    private val controller: KalphiteQueenController,
    private val accuracy: AccuracyFormulae,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        listOf(KalphiteQueenNpcs.ground, KalphiteQueenNpcs.airborne).forEach { type ->
            onAiOpPlayer2(type) { attack(this, it.target) }
            onAiApPlayer2(type) { attack(this, it.target) }
            onModifyNpcHit(type) { controller.modifyHit(npc, hit) }
            onNpcHit(type) { controller.onHit(npc, hit) }
        }
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id == KalphiteQueenNpcs.ground.id) {
                configure(npc)
                controller.reset(npc)
            }
        }
        val existing =
            npcs.findAll(KalphiteQueenDefinition.spawn).firstOrNull {
                it.id == KalphiteQueenNpcs.ground.id
            }
        val queen =
            existing
                ?: Npc(npcTypes[KalphiteQueenNpcs.ground], KalphiteQueenDefinition.spawn).also {
                    npcs.add(it, Int.MAX_VALUE)
                }
        configure(queen)
        controller.reset(queen)
    }

    private fun attack(access: StandardNpcAccess, target: Player) {
        with(access) {
            if (actionDelay > mapClock) {
                return
            }
            actionDelay = mapClock + 4
            val state = controller.state(npc)
            state.attacks++
            if (isWithinDistance(target, 1) && state.attacks % 3 == 0) {
                val accurate =
                    accuracy.rollMeleeAccuracy(npc, target, MeleeAttackType.Crush, random)
                val damage = if (accurate) random.of(0..KalphiteQueenDefinition.MAX_HIT) else 0
                target.queueHit(npc, 1, HitType.Melee, damage)
                return
            }
            val ranged = state.attacks % 2 == 0
            val type = if (ranged) HitType.Ranged else HitType.Magic
            target.queueHit(npc, 2, type, random.of(0..KalphiteQueenDefinition.MAX_HIT))
            if (ranged) {
                target.statSub(stats.prayer, constant = 1, percent = 0)
            }
        }
    }

    private fun configure(npc: Npc) {
        npc.apply {
            resetTransmog()
            hitpoints = KalphiteQueenDefinition.HITPOINTS
            baseHitpointsLvl = KalphiteQueenDefinition.HITPOINTS
            attackLvl = KalphiteQueenDefinition.ATTACK
            baseAttackLvl = KalphiteQueenDefinition.ATTACK
            strengthLvl = KalphiteQueenDefinition.STRENGTH
            baseStrengthLvl = KalphiteQueenDefinition.STRENGTH
            defenceLvl = KalphiteQueenDefinition.DEFENCE
            baseDefenceLvl = KalphiteQueenDefinition.DEFENCE
            magicLvl = KalphiteQueenDefinition.MAGIC
            baseMagicLvl = KalphiteQueenDefinition.MAGIC
            rangedLvl = KalphiteQueenDefinition.RANGED
            baseRangedLvl = KalphiteQueenDefinition.RANGED
            mode = NpcMode.Wander
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
    }
}
