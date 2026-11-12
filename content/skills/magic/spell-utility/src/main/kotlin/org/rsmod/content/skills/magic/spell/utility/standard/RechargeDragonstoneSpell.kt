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
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class RechargeDragonstoneSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }

            val legendsComplete = vars[varps.legends_quest] >= LEGENDS_QUEST_COMPLETE
            val conversions = pendingConversions(legendsComplete)
            if (conversions.isEmpty()) {
                val hasLockedJewellery =
                    !legendsComplete &&
                        player.inv.any { RechargeDragonstoneRecipes.isLegendsLocked(it?.id) }
                mes(if (hasLockedJewellery) LEGENDS_QUEST_MESSAGE else NO_JEWELLERY_MESSAGE)
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_recharge_dragonstone))
            val preflight = transform(conversions, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val committed = transform(conversions, autoCommit = true)
            check(committed.success) {
                "Recharge Dragonstone transaction failed after validation: $conversions"
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.quest_lunar_spell_cast_spell_on_group)
            spotanim(spotanims.lunar_enchant_dragonstone)
            soundSynth(synths.enchant_dragon_amulet)
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
        if (vars[varbits.fremennik_diary_hard_complete] == 0) {
            mes("You need to complete the hard Fremennik Diary to cast this spell.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.pendingConversions(legendsComplete: Boolean): List<Conversion> =
        player.inv.indices.mapNotNull { slot ->
            val input = player.inv[slot] ?: return@mapNotNull null
            val recipe = RechargeDragonstoneRecipes.byInput[input.id] ?: return@mapNotNull null
            if (recipe.requiresLegendsQuest && !legendsComplete) {
                return@mapNotNull null
            }
            Conversion(slot, input, recipe.output)
        }

    private fun ProtectedAccess.transform(conversions: List<Conversion>, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            for (conversion in conversions) {
                delete(inventory, conversion.input.id, conversion.input.count, conversion.slot)
                add(inventory, conversion.output.id, conversion.input.count, slot = conversion.slot)
            }
        }

    private data class Conversion(val slot: Int, val input: InvObj, val output: ObjType)

    private companion object {
        const val CAST_RATE = 3
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val LEGENDS_QUEST_COMPLETE = 75
        const val NO_JEWELLERY_MESSAGE = "You don't have any dragonstone jewellery to recharge."
        const val LEGENDS_QUEST_MESSAGE =
            "You need to complete Legends' Quest to recharge this jewellery."
    }
}

public data class RechargeDragonstoneRecipe(
    val input: ObjType,
    val output: ObjType,
    val requiresLegendsQuest: Boolean,
)

public object RechargeDragonstoneRecipes {
    private val glory =
        listOf(
            objs.amulet_of_glory_uncharged,
            objs.amulet_of_glory_1,
            objs.amulet_of_glory_2,
            objs.amulet_of_glory_3,
        )
    private val trimmedGlory =
        listOf(
            objs.amulet_of_glory_t_uncharged,
            objs.amulet_of_glory_t_1,
            objs.amulet_of_glory_t_2,
            objs.amulet_of_glory_t_3,
        )
    private val skillsNecklace =
        listOf(
            objs.skills_necklace_uncharged,
            objs.skills_necklace_1,
            objs.skills_necklace_2,
            objs.skills_necklace_3,
        )
    private val combatBracelet =
        listOf(
            objs.combat_bracelet_uncharged,
            objs.combat_bracelet_1,
            objs.combat_bracelet_2,
            objs.combat_bracelet_3,
        )

    public val all: List<RechargeDragonstoneRecipe> = buildList {
        glory.forEach { add(RechargeDragonstoneRecipe(it, objs.amulet_of_glory_4, false)) }
        trimmedGlory.forEach { add(RechargeDragonstoneRecipe(it, objs.amulet_of_glory_t_4, false)) }
        skillsNecklace.forEach { add(RechargeDragonstoneRecipe(it, objs.skills_necklace_4, true)) }
        combatBracelet.forEach { add(RechargeDragonstoneRecipe(it, objs.combat_bracelet_4, true)) }
    }

    public val byInput: Map<Int, RechargeDragonstoneRecipe> = all.associateBy { it.input.id }

    public fun isLegendsLocked(input: Int?): Boolean =
        input != null && byInput[input]?.requiresLegendsQuest == true
}
