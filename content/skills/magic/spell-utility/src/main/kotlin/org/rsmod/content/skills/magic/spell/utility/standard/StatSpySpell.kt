package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.stat.StatType

/** Cache-233 Lunar Stat Spy behavior and native side-panel population. */
public class StatSpySpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, target: Player) {
        with(access) {
            if (player === target || !cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_stat_spy))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            stopAction()
            anim(seqs.dream_player_monsterexam_statspy)
            spotanim(spotanims.dream_spell_playerexam_statspy_spotanim)
            // The original player-impact asset is shared with Cure Other and Heal Other.
            target.spotanim(spotanims.quest_lunar_spellbook_energy_heal_spot_anim)
            soundSynth(synths.lunar_stat_spy)
            openStats(target)
            target.mes("${player.displayName} is spying on your stats!")
            cooldowns.markCast(player, mapClock + CAST_RATE)
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
        if (vars[varbits.dream_mentor] < DREAM_MENTOR_COMPLETE) {
            mes("You need to complete Dream Mentor to cast this spell.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.openStats(target: Player) {
        ifOpenSide(interfaces.dream_player_stats)
        for (field in fields) {
            ifSetText(field.current, target.stat(field.stat).toString())
            ifSetText(field.base, target.statBase(field.stat).toString())
        }
        ifSetText(components.dream_player_stats_name, "Player ~ ${target.displayName}")
    }

    private data class StatField(
        val stat: StatType,
        val current: ComponentType,
        val base: ComponentType,
    )

    private companion object {
        // Stat Spy is cache-classified as a combat spell; legacy spell timing records its
        // non-damaging recast interval as the standard five-tick combat-spell interval.
        const val CAST_RATE = 5
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val DREAM_MENTOR_COMPLETE = 28

        val fields =
            listOf(
                StatField(
                    stats.attack,
                    components.dream_player_stats_attack,
                    components.dream_player_stats_attack_base,
                ),
                StatField(
                    stats.hitpoints,
                    components.dream_player_stats_hitpoints,
                    components.dream_player_stats_hitpoints_base,
                ),
                StatField(
                    stats.mining,
                    components.dream_player_stats_mining,
                    components.dream_player_stats_mining_base,
                ),
                StatField(
                    stats.strength,
                    components.dream_player_stats_strength,
                    components.dream_player_stats_strength_base,
                ),
                StatField(
                    stats.agility,
                    components.dream_player_stats_agility,
                    components.dream_player_stats_agility_base,
                ),
                StatField(
                    stats.smithing,
                    components.dream_player_stats_smithing,
                    components.dream_player_stats_smithing_base,
                ),
                StatField(
                    stats.defence,
                    components.dream_player_stats_defence,
                    components.dream_player_stats_defence_base,
                ),
                StatField(
                    stats.herblore,
                    components.dream_player_stats_herblore,
                    components.dream_player_stats_herblore_base,
                ),
                StatField(
                    stats.fishing,
                    components.dream_player_stats_fishing,
                    components.dream_player_stats_fishing_base,
                ),
                StatField(
                    stats.ranged,
                    components.dream_player_stats_ranged,
                    components.dream_player_stats_ranged_base,
                ),
                StatField(
                    stats.thieving,
                    components.dream_player_stats_thieving,
                    components.dream_player_stats_thieving_base,
                ),
                StatField(
                    stats.cooking,
                    components.dream_player_stats_cooking,
                    components.dream_player_stats_cooking_base,
                ),
                StatField(
                    stats.prayer,
                    components.dream_player_stats_prayer,
                    components.dream_player_stats_prayer_base,
                ),
                StatField(
                    stats.crafting,
                    components.dream_player_stats_crafting,
                    components.dream_player_stats_crafting_base,
                ),
                StatField(
                    stats.firemaking,
                    components.dream_player_stats_firemaking,
                    components.dream_player_stats_firemaking_base,
                ),
                StatField(
                    stats.magic,
                    components.dream_player_stats_magic,
                    components.dream_player_stats_magic_base,
                ),
                StatField(
                    stats.fletching,
                    components.dream_player_stats_fletching,
                    components.dream_player_stats_fletching_base,
                ),
                StatField(
                    stats.woodcutting,
                    components.dream_player_stats_woodcutting,
                    components.dream_player_stats_woodcutting_base,
                ),
                StatField(
                    stats.runecrafting,
                    components.dream_player_stats_runecrafting,
                    components.dream_player_stats_runecrafting_base,
                ),
                StatField(
                    stats.slayer,
                    components.dream_player_stats_slayer,
                    components.dream_player_stats_slayer_base,
                ),
                StatField(
                    stats.farming,
                    components.dream_player_stats_farming,
                    components.dream_player_stats_farming_base,
                ),
                StatField(
                    stats.construction,
                    components.dream_player_stats_construction,
                    components.dream_player_stats_construction_base,
                ),
                StatField(
                    stats.hunter,
                    components.dream_player_stats_hunter,
                    components.dream_player_stats_hunter_base,
                ),
            )
    }
}
