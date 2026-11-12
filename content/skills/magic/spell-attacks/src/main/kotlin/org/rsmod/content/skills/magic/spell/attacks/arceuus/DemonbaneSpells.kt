package org.rsmod.content.skills.magic.spell.attacks.arceuus

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.api.spells.attack.SpellAttackRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public class DemonbaneSpells @Inject constructor() : SpellAttackMap {
    override fun SpellAttackRepository.register(manager: SpellAttackManager) {
        for (definition in DemonbaneSpellDefinitions.all) {
            register(definition.spell, DemonbaneSpellAttack(manager, definition))
        }
    }
}

private class DemonbaneSpellAttack(
    private val manager: SpellAttackManager,
    private val definition: DemonbaneSpellDefinition,
) : SpellAttack {
    override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
        if (target.type.param(params.demon) == 0) {
            rejectTarget()
            return
        }
        cast(target, attack)
    }

    override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
        rejectTarget()
    }

    private fun ProtectedAccess.cast(target: Npc, attack: CombatAttack.Spell) {
        if (!canAccess()) {
            manager.stopCombat(this)
            return
        }

        val castResult = manager.attemptCast(this, attack)
        if (castResult.isFailure()) return

        anim(seqs.human_spellcast_demonbane)
        spotanim(definition.castSpot)

        val splash = manager.rollSplash(this, target, attack, castResult)
        if (splash) {
            manager.playSplashFx(
                source = this,
                target = target,
                clientDelay = CLIENT_DELAY,
                castSound = null,
                soundRadius = 0,
            )
            manager.queueSplashHit(
                this,
                target,
                definition.spell,
                clientDelay = CLIENT_DELAY,
                hitDelay = HIT_DELAY,
            )
            manager.continueCombatIfAutocast(this, target)
            return
        }

        val damage = manager.rollMaxHit(this, target, attack, castResult, definition.baseMaxHit)
        manager.playHitFx(
            source = this,
            target = target,
            clientDelay = CLIENT_DELAY,
            castSound = null,
            soundRadius = 0,
            hitSpot = definition.hitSpot,
            hitSpotHeight = 0,
            hitSound = null,
        )
        manager.giveCombatXp(this, target, attack, damage)
        manager.queueMagicHit(
            this,
            target,
            definition.spell,
            damage,
            clientDelay = CLIENT_DELAY,
            hitDelay = HIT_DELAY,
        )
        manager.continueCombatIfAutocast(this, target)
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (
            definition.requiresKingdomDivided &&
                vars[varbits.a_kingdom_divided] < A_KINGDOM_DIVIDED_COMPLETE
        ) {
            mes("You need to complete A Kingdom Divided to cast this spell.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.rejectTarget() {
        manager.stopCombat(this)
        mes("This spell can only be cast on demonic creatures.")
    }

    private companion object {
        const val A_KINGDOM_DIVIDED_COMPLETE = 150
        const val CLIENT_DELAY = 30
        const val HIT_DELAY = 2
    }
}
