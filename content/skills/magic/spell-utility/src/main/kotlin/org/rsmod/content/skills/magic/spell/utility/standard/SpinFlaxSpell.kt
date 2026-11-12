package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.min
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
import org.rsmod.content.skills.fletching.bowstringing.BowStringSpool

public class SpinFlaxSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val spool: BowStringSpool,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, allowQueue: Boolean = true) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock)) {
                if (allowQueue && spin_flax_queues.cast !in player.queueList) {
                    queue(spin_flax_queues.cast, cooldowns.cyclesUntilReady(player, mapClock))
                }
                return
            }
            if (!canAccess()) {
                return
            }

            val flaxSlots =
                player.inv.indices
                    .asSequence()
                    .filter { player.inv[it]?.id == objs.flax.id }
                    .take(MAX_FLAX)
                    .toList()
            if (flaxSlots.isEmpty()) {
                mes(NO_FLAX_MESSAGE)
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_spin_flax))
            val spoolCount = min(flaxSlots.size, spool.remainingCapacity(this))
            val looseCount = flaxSlots.size - spoolCount
            val preflight = transform(flaxSlots, looseCount, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            val committed = transform(flaxSlots, looseCount, autoCommit = true)
            check(committed.success) { "Spin Flax transaction failed after validation." }
            val stored = spool.storeProduced(this, spoolCount)
            check(stored == spoolCount) { "Bow string spool state changed during Spin Flax." }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            statAdvance(stats.crafting, flaxSlots.size * CRAFTING_XP_PER_FLAX)
            anim(seqs.lunar_human_magic_summon2)
            spotanim(spotanims.quest_lunar_magic_flax_strim)
            soundSynth(synths.lunar_hunter_kit)
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
        return true
    }

    private fun ProtectedAccess.transform(
        flaxSlots: List<Int>,
        looseCount: Int,
        autoCommit: Boolean,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            for (slot in flaxSlots) {
                delete(inventory, objs.flax.id, count = 1, slot = slot)
            }
            if (looseCount > 0) {
                add(inventory, objs.bow_string.id, count = looseCount)
            }
        }

    private companion object {
        const val MAX_FLAX = 5
        const val CAST_RATE = 5
        const val CRAFTING_XP_PER_FLAX = 15.0
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val NO_FLAX_MESSAGE = "You don't have any flax to spin."
    }
}
