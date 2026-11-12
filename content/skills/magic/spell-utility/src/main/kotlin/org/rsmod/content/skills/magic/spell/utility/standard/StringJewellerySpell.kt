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
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.crafting.jewellery.AmuletStringingRecipe

public class StringJewellerySpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val recipes: StringJewelleryRecipeRegistry,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            clearWeakQueue(string_jewellery_queues.batch)
            if (!canAccess(sendFailure = true)) {
                return
            }
            if (nextJewellery() == null) {
                mes(NO_JEWELLERY_MESSAGE)
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_string_jewelry))
            if (!runes.canCastSpell(player, spell)) {
                return
            }
            weakQueue(string_jewellery_queues.batch, RESULT_TICKS)
        }
    }

    public fun process(access: ProtectedAccess) {
        with(access) {
            if (!canAccess(sendFailure = false)) {
                return
            }
            val next = nextJewellery() ?: return
            val spell = requireNotNull(spells.getObjSpell(objs.spell_string_jewelry))
            val preflight = transform(next, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            val committed = transform(next, autoCommit = true)
            check(committed.success) {
                "String Jewellery transaction failed after validation: $next"
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            statAdvance(stats.crafting, CRAFTING_XP_PER_ITEM)
            anim(seqs.lunar_human_magic_summon1)
            spotanim(spotanims.quest_lunar_spellbook_string_jewelery_spot_anim)
            soundSynth(synths.lunar_string_amulet2)

            if (nextJewellery() != null) {
                weakQueue(
                    string_jewellery_queues.batch,
                    RESULT_TICKS + QUEUE_HANDLER_TICK_COMPENSATION,
                )
            }
        }
    }

    private fun ProtectedAccess.canAccess(sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to cast this spell.")
            }
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            if (sendFailure) {
                mes("You need to complete Lunar Diplomacy to cast this spell.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.nextJewellery(): PendingJewellery? =
        player.inv.indices.firstNotNullOfOrNull { slot ->
            val obj = player.inv[slot] ?: return@firstNotNullOfOrNull null
            val recipe = recipes.forUnstrung(obj.id) ?: return@firstNotNullOfOrNull null
            PendingJewellery(slot, recipe)
        }

    private fun ProtectedAccess.transform(jewellery: PendingJewellery, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            delete(inventory, jewellery.recipe.unstrung.id, count = 1, slot = jewellery.slot)
            add(inventory, jewellery.recipe.output.id, count = 1, slot = jewellery.slot)
        }

    private data class PendingJewellery(val slot: Int, val recipe: AmuletStringingRecipe)

    private companion object {
        const val RESULT_TICKS = 3
        const val QUEUE_HANDLER_TICK_COMPENSATION = 1
        const val CRAFTING_XP_PER_ITEM = 4.0
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val NO_JEWELLERY_MESSAGE = "You don't have any jewellery to string."
    }
}
