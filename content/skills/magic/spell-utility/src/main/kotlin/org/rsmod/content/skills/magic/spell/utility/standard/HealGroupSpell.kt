package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType

/** Cache-233 Lunar Heal Group behavior. */
public class HealGroupSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val utilityCooldowns: UtilitySpellCooldownRepository,
    private val healGroupCooldown: HealGroupStateRepository,
    private val players: PlayerList,
    private val realm: Realm,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (
                !utilityCooldowns.isReady(player, mapClock) ||
                    !healGroupCooldown.isReady(player) ||
                    !canAccess()
            ) {
                return
            }

            val current = player.hitpoints
            if (!LunarHealOther.hasRequiredHealth(current, player.baseHitpointsLvl)) {
                return
            }
            val recipients = recipients(player).take(LunarHealGroup.MAX_RECIPIENTS).toList()
            if (recipients.isEmpty()) {
                return
            }
            val sacrifice = LunarHealGroup.sacrifice(current)
            val healing =
                LunarHealGroup.healing(
                    sacrifice,
                    recipients.map { it.baseHitpointsLvl - it.hitpoints },
                )

            val spell = requireNotNull(spells.getObjSpell(objs.spell_heal_group))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.quest_lunar_spell_cast_spell_on_group)
            soundSynth(synths.lunar_heal_group)
            recipients.zip(healing).forEach { (recipient, amount) ->
                recipient.spotanim(
                    spotanims.quest_lunar_spellbook_heal_group_spot_anim,
                    height = GRAPHIC_HEIGHT,
                )
                recipient.soundSynth(synths.lunar_heal)
                recipient.statHeal(stats.hitpoints, constant = amount, percent = 0)
            }
            takeInstantHit(HitType.Typeless, sacrifice)
            addFoodStyleAttackDelay()
            utilityCooldowns.markCast(player, mapClock + ORDINARY_CAST_RATE)
            healGroupCooldown.begin(player)
        }
    }

    private fun recipients(caster: Player): Sequence<Player> =
        players
            .asSequence()
            .filter { it !== caster }
            .filter { it.coords.level == caster.coords.level }
            .filter { it.coords.chebyshevDistance(caster.coords) <= AREA_RADIUS }
            .filter { it.vars[varbits.option_acceptaid] != 0 }
            .filter { it.hitpoints in 1 until it.baseHitpointsLvl }
            .filter { canReceiveInPvpArea(caster, it) }

    private fun canReceiveInPvpArea(caster: Player, recipient: Player): Boolean {
        val casterSingleWilderness =
            caster.vars[varbits.inside_wilderness] == 1 &&
                caster.vars[varbits.multiway_indicator] == 0
        val recipientSingleWilderness =
            recipient.vars[varbits.inside_wilderness] == 1 &&
                recipient.vars[varbits.multiway_indicator] == 0
        if (casterSingleWilderness || recipientSingleWilderness) {
            return false
        }

        if (!realm.config.hasWorldFlag(WorldFlag.Pvp)) {
            return true
        }
        val casterInRecordedPvpCombat =
            caster.vars[varps.lastcombat_pvp] > 0 && caster.isInPvpCombat()
        val recipientInRecordedPvpCombat =
            recipient.vars[varps.lastcombat_pvp] > 0 && recipient.isInPvpCombat()
        val singleArea =
            caster.vars[varbits.multiway_indicator] == 0 ||
                recipient.vars[varbits.multiway_indicator] == 0
        return !singleArea || (!casterInRecordedPvpCombat && !recipientInRecordedPvpCombat)
    }

    private fun ProtectedAccess.addFoodStyleAttackDelay() {
        if (player.actionDelay > mapClock) {
            player.actionDelay += ATTACK_DELAY_CYCLES
        }
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        return true
    }

    private companion object {
        const val AREA_RADIUS = 1
        const val ATTACK_DELAY_CYCLES = 3
        const val GRAPHIC_HEIGHT = 96
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val ORDINARY_CAST_RATE = 5
    }
}
