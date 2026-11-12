package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.LocTDefaultEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.farming.FarmingCompost
import org.rsmod.content.skills.farming.FarmingCropStatus
import org.rsmod.content.skills.farming.FarmingService

/** Lunar and Arceuus crop spells backed by the cache's farming multi-loc varbits. */
public class FarmingPatchSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val random: GameRandom,
    private val farming: FarmingService,
) {
    public fun curePlant(access: ProtectedAccess, target: LocTDefaultEvents.Ap) {
        with(access) {
            if (!canUseLunarSpell()) return
            val state = farming.state(this, target.loc)
            if (state == null) {
                mes("You can only cast this spell on a farming patch.")
                return
            }
            if (state.status != FarmingCropStatus.Diseased) {
                mes(
                    if (state.status == FarmingCropStatus.Dead) {
                        "It is too late to cure this plant."
                    } else {
                        "This plant does not need curing."
                    }
                )
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_cure_plant))
            if (runes.attemptCast(player, spell).isFailure()) return

            farming.cure(this, target.loc)
            awardMagicXp(spell.castXp)
            statAdvance(stats.farming, CURE_FARMING_XP)
            anim(LunarUtilitySeqs.curePlant)
            spotanim(LunarUtilitySpotanims.curePlant)
            mes("The plant has been cured.")
        }
    }

    public fun fertileSoil(access: ProtectedAccess, target: LocTDefaultEvents.Ap) {
        with(access) {
            if (!canUseLunarSpell()) return
            val state = farming.state(this, target.loc)
            if (state == null) {
                mes("You can only cast this spell on a farming patch.")
                return
            }
            if (state.compost != FarmingCompost.None) {
                mes("This patch has already been treated with compost.")
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_treat_soil))
            val upgraded =
                vars[FarmingPatchVarbits.ultraFertileSoil] != 0 &&
                    invTotal(player.inv, objs.volcanic_ash) >= VOLCANIC_ASH_COST
            if (!runes.canCastSpell(player, spell)) return
            if (upgraded) {
                val result = player.invDel(player.inv, objs.volcanic_ash, VOLCANIC_ASH_COST)
                if (!result.success) return
            }
            if (runes.attemptCast(player, spell).isFailure()) return

            farming.fertilise(
                this,
                target.loc,
                if (upgraded) FarmingCompost.Ultracompost else FarmingCompost.Supercompost,
            )
            awardMagicXp(spell.castXp)
            statAdvance(stats.farming, FERTILE_SOIL_FARMING_XP)
            anim(LunarUtilitySeqs.fertileSoil)
            spotanim(LunarUtilitySpotanims.fertileSoil)
            mes(
                if (upgraded) "The patch has been treated with ultracompost."
                else "The patch has been treated with supercompost."
            )
        }
    }

    public fun resurrectCrops(access: ProtectedAccess, target: LocTDefaultEvents.Ap) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            val state = farming.state(this, target.loc)
            val crop = state?.let(farming::crop)
            if (
                state == null ||
                    state.status != FarmingCropStatus.Dead ||
                    crop == null ||
                    crop.patchType.name.lowercase() in RESURRECTION_EXCLUSIONS
            ) {
                mes("This spell can only be cast on a dead crop.")
                return
            }
            if (state.resurrectionAttempted) {
                mes("You have already tried to resurrect this crop.")
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_resurrect_crops))
            if (runes.attemptCast(player, spell).isFailure()) return

            awardMagicXp(spell.castXp)
            val chance = ResurrectCropsRules.successChance(stat(stats.magic))
            val success = random.of(100) < chance
            farming.resurrect(this, target.loc, success)
            if (success) {
                spotanim(FarmingPatchSpotanims.resurrectSuccess)
                mes("The crop springs back to life.")
            } else {
                spotanim(FarmingPatchSpotanims.resurrectFailure)
                mes("The crop crumbles to dust.")
            }
            anim(FarmingPatchSeqs.resurrect)
        }
    }

    private fun ProtectedAccess.canUseLunarSpell(): Boolean {
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

    private fun ProtectedAccess.awardMagicXp(xp: Double) {
        if (vars[varbits.fountain_of_rune_active] == 0) statAdvance(stats.magic, xp)
    }

    public companion object {
        private const val LUNAR_DIPLOMACY_COMPLETE = 190
        private const val CURE_FARMING_XP = 60.0
        private const val FERTILE_SOIL_FARMING_XP = 18.0
        private const val VOLCANIC_ASH_COST = 2
        private val RESURRECTION_EXCLUSIONS = setOf("hespori", "anima")
    }
}

internal object FarmingPatchVarbits : VarBitReferences() {
    val ultraFertileSoil = find("fossil_mine_unlocked_ultrafertilesoil")
}

internal object FarmingPatchComponents : ComponentReferences() {
    val resurrectCrops = find("magic_spellbook:resurrect_crops")
}

internal object FarmingPatchSeqs : SeqReferences() {
    val resurrect = find("arceuus_necromancy_playeranim")
}

internal object FarmingPatchSpotanims : SpotanimReferences() {
    val resurrectSuccess = find("arceuus_crop_res_spotanim")
    val resurrectFailure = find("arceuus_crop_res_fail_spotanim")
}
