package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

/** Cache-233 Lunar Boost Potion Share behavior for ordinary stat-boosting potions. */
public class BoostPotionShareSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
    private val players: PlayerList,
    private val poison: PoisonProtectionRepository,
    private val disease: DiseaseImmunityRepository,
    private val antifire: AntifireProtectionRepository,
    private val goading: GoadingPotionRepository,
) {
    public fun cast(access: ProtectedAccess, targetSlot: Int) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }

            val target = player.inv[targetSlot] ?: return
            val family = BoostPotionFamily.forObj(target.id) ?: return
            val dose = family.doseFor(target.id)
            val recipients = recipients(player).take(minOf(dose, MAX_RECIPIENTS)).toList()
            if (recipients.isEmpty()) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_statboost_pot_share))
            check(spell.type == MagicSpellType.Combat) { "Expected combat spell: $spell" }
            val remainder = family.remainder(dose, recipients.size)
            val preflight = transformPotion(targetSlot, target.id, remainder.id, autoCommit = false)
            if (!preflight || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            check(transformPotion(targetSlot, target.id, remainder.id, autoCommit = true)) {
                "Potion-share transaction failed after validation: $target"
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.lunar_human_magic_summon2)
            for (recipient in sequenceOf(player) + recipients.asSequence()) {
                BoostPotionEffects.apply(recipient, family, poison, disease, antifire, goading)
                recipient.spotanim(
                    spotanims.quest_lunar_spellbook_cure_poison_spot_anim,
                    height = GRAPHIC_HEIGHT,
                )
            }
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    private fun ProtectedAccess.transformPotion(
        targetSlot: Int,
        targetId: Int,
        remainderId: Int,
        autoCommit: Boolean,
    ): Boolean =
        player
            .invTransaction(player.inv, autoCommit = autoCommit) {
                val inventory = select(player.inv)
                delete(inventory, targetId, count = 1, slot = targetSlot)
                add(inventory, remainderId, count = 1)
            }
            .success

    private fun recipients(caster: Player): Sequence<Player> =
        players
            .asSequence()
            .filter { it !== caster }
            .filter { it.coords.level == caster.coords.level }
            .filter { it.coords.chebyshevDistance(caster.coords) <= AREA_RADIUS }
            .filter { it.vars[varbits.option_acceptaid] != 0 }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        if (
            player.coords.level == CASTLE_WARS_LEVEL &&
                player.coords.x in CASTLE_WARS_X &&
                player.coords.z in CASTLE_WARS_Z
        ) {
            mes("You don't want to help your enemies")
            return false
        }
        return true
    }

    private companion object {
        const val AREA_RADIUS = 2
        const val CAST_RATE = 3
        const val GRAPHIC_HEIGHT = 96
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val MAX_RECIPIENTS = 4
        const val CASTLE_WARS_LEVEL = 0
        val CASTLE_WARS_X = 2368..2431
        val CASTLE_WARS_Z = 3072..3135
    }
}
