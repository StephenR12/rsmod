package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
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
import org.rsmod.content.skills.crafting.tanning.TanningRecipe
import org.rsmod.content.skills.crafting.tanning.TanningRecipeRegistry
import org.rsmod.game.entity.Player

public enum class CowhideLeather {
    Soft,
    Hard,
}

@Singleton
public class TanLeatherSetupRepository @Inject constructor() {
    private val selections: MutableMap<Player, CowhideLeather> = WeakHashMap()

    public fun get(player: Player): CowhideLeather =
        selections.getOrDefault(player, CowhideLeather.Soft)

    public fun set(player: Player, selection: CowhideLeather) {
        selections[player] = selection
    }

    public fun clear(player: Player) {
        selections.remove(player)
    }
}

public class TanLeatherSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    recipes: TanningRecipeRegistry,
    private val setup: TanLeatherSetupRepository,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    private val softCowhide = recipes.recipes.single { it.output.id == objs.leather.id }
    private val hardCowhide = recipes.recipes.single { it.output.id == objs.hard_leather.id }
    private val nonCowhideRecipes =
        recipes.recipes.filter { it.input.id != objs.cow_hide.id }.associateBy { it.input.id }

    public fun cast(access: ProtectedAccess, allowQueue: Boolean = true) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock)) {
                if (allowQueue && tan_leather_queues.cast !in player.queueList) {
                    queue(tan_leather_queues.cast, cooldowns.cyclesUntilReady(player, mapClock))
                }
                return
            }
            if (!canAccess()) {
                return
            }

            val pending = pendingHides()
            if (pending.isEmpty()) {
                mes(NO_HIDES_MESSAGE)
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_tan_leather))
            val preflight = transform(pending, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            val committed = transform(pending, autoCommit = true)
            check(committed.success) { "Tan Leather transaction failed after validation." }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.tan_leather)
            spotanim(spotanims.lunar_tan_leather)
            soundSynth(synths.lunar_bake_pie)
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    public suspend fun configure(access: ProtectedAccess) {
        with(access) {
            val selection =
                menu("What type of leather do you wish to tan?", "Soft leather", "Hard leather")
            when (selection) {
                0 -> setup.set(player, CowhideLeather.Soft)
                1 -> setup.set(player, CowhideLeather.Hard)
            }
            ifClose()
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

    private fun ProtectedAccess.pendingHides(): List<PendingHide> =
        player.inv.indices
            .asSequence()
            .mapNotNull { slot ->
                val obj = player.inv[slot] ?: return@mapNotNull null
                val recipe = recipeFor(obj.id) ?: return@mapNotNull null
                PendingHide(slot, recipe)
            }
            .take(MAX_HIDES)
            .toList()

    private fun ProtectedAccess.recipeFor(input: Int): TanningRecipe? =
        when {
            input != objs.cow_hide.id -> nonCowhideRecipes[input]
            setup.get(player) == CowhideLeather.Hard -> hardCowhide
            else -> softCowhide
        }

    private fun ProtectedAccess.transform(pending: List<PendingHide>, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            for (hide in pending) {
                delete(inventory, hide.recipe.input.id, count = 1, slot = hide.slot)
                add(inventory, hide.recipe.output.id, count = 1, slot = hide.slot)
            }
        }

    private data class PendingHide(val slot: Int, val recipe: TanningRecipe)

    private companion object {
        const val MAX_HIDES = 5
        const val CAST_RATE = 3
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val NO_HIDES_MESSAGE = "You don't have any suitable hides to tan."
    }
}
