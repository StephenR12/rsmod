package org.rsmod.api.combat

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.combat.npc.attackingPlayer
import org.rsmod.api.combat.npc.lastAttack
import org.rsmod.api.combat.player.aggressiveNpc
import org.rsmod.api.combat.player.lastCombat
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcMeleeAlternative
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.player.events.PlayerCombatEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.soundSynth
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList

public class NvPCombat
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
    private val objTypes: ObjTypeList,
    private val eventBus: EventBus,
) {
    public fun attack(access: StandardNpcAccess, target: Player, attack: CombatAttack.NpcMelee) {
        access.attackMelee(target, attack)
    }

    private fun StandardNpcAccess.attackMelee(target: Player, attack: CombatAttack.NpcMelee) {
        if (!canAttack(target)) {
            resetMode()
            return
        }

        // Note: We do not need to explicitly call `opplayer2` because npcs will automatically
        // repeat their last interaction until it is canceled (e.g., by changing their `npcmode`).
        if (actionDelay > mapClock) {
            return
        }

        if (!npc.isInCombat()) {
            resetMode()
            return
        }

        val attackRate = npc.attackRate()
        actionDelay = mapClock + attackRate

        val hook = eventBus.keyed[NpcMeleeAlternative::class.java, npc.visType.id]
        val alternative =
            hook?.let {
                val event = NpcMeleeAlternative(npc, target)
                it(event)
                event.attack
            }
        val attackAnim = alternative?.animation ?: npc.visType.param(params.attack_anim)
        anim(attackAnim)
        if (alternative == null)
            npc.visType.paramOrNull(params.attack_sound)?.let(target::soundSynth)
        val damage =
            alternative?.damage
                ?: if (accuracy.rollMeleeAccuracy(npc, target, attack.type, random)) {
                    random.of(0..maxHits.getMeleeMaxHit(npc, target, attack.type))
                } else 0

        setAttackVars(target)
        eventBus.publish(PlayerCombatEvents.Engage(target))

        // Note: Retaliation must be queued _before_ the hit. If queued after, every hit would
        // trigger the "speed-up" death mechanic, since the hit queues would no longer be the
        // last entries in the queue list at the time of processing.
        target.queueCombatRetaliate(npc)

        target.queueHit(npc, 1, alternative?.type ?: HitType.Melee, damage)
        target.combatPlayDefendAnim(objTypes)
    }

    private fun canAttack(target: Player): Boolean {
        return target.isValidTarget()
    }

    private fun StandardNpcAccess.setAttackVars(target: Player) {
        npc.lastAttack = mapClock
        npc.attackingPlayer = target.uid
        target.lastCombat = mapClock
        target.aggressiveNpc = npc.uid
    }
}
