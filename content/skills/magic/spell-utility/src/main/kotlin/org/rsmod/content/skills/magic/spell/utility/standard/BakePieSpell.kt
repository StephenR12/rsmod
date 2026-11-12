package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
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
import org.rsmod.api.player.stat.stat
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.cooking.StandardCookingRecipe
import org.rsmod.content.skills.cooking.StandardCookingRecipes

public class BakePieSpell
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            clearWeakQueue(bake_pie_queues.batch)
            if (!canAccess(sendFailure = true)) {
                return
            }
            val next = nextPie()
            if (next == null) {
                mes(NO_PIES_MESSAGE)
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_bake_pie))
            if (!runes.canCastSpell(player, spell)) {
                return
            }
            if (!hasCookingLevel(next.recipe, sendFailure = true)) {
                return
            }
            weakQueue(bake_pie_queues.batch, RESULT_TICKS)
        }
    }

    public fun process(access: ProtectedAccess) {
        with(access) {
            if (!canAccess(sendFailure = false)) {
                return
            }
            val next = nextPie() ?: return
            if (!hasCookingLevel(next.recipe, sendFailure = true)) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(objs.spell_bake_pie))
            val preflight = transform(next, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return
            }
            val committed = transform(next, autoCommit = true)
            check(committed.success) { "Bake Pie transaction failed after validation: $next" }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            statAdvance(stats.cooking, next.recipe.xp)
            anim(seqs.lunar_human_magic_summon2)
            spotanim(spotanims.quest_lunar_bakepie)

            if (nextPie() != null) {
                weakQueue(bake_pie_queues.batch, RESULT_TICKS + QUEUE_HANDLER_TICK_COMPENSATION)
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

    private fun ProtectedAccess.hasCookingLevel(
        recipe: StandardCookingRecipe,
        sendFailure: Boolean,
    ): Boolean {
        if (player.stat(stats.cooking) >= recipe.level) {
            return true
        }
        if (sendFailure) {
            mes("You need a Cooking level of ${recipe.level} to bake this pie.")
        }
        return false
    }

    private fun ProtectedAccess.nextPie(): PendingPie? =
        player.inv.indices.firstNotNullOfOrNull { slot ->
            val obj = player.inv[slot] ?: return@firstNotNullOfOrNull null
            val recipe = BakePieRecipes.byRaw[obj.id] ?: return@firstNotNullOfOrNull null
            PendingPie(slot, recipe)
        }

    private fun ProtectedAccess.transform(pie: PendingPie, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            delete(inventory, pie.recipe.raw.id, count = 1, slot = pie.slot)
            add(inventory, pie.recipe.cooked.id, count = 1, slot = pie.slot)
        }

    private data class PendingPie(val slot: Int, val recipe: StandardCookingRecipe)

    private companion object {
        const val RESULT_TICKS = 3
        const val QUEUE_HANDLER_TICK_COMPENSATION = 1
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val NO_PIES_MESSAGE = "You do not have any uncooked pies to bake."
    }
}

public object BakePieRecipes {
    public val all: List<StandardCookingRecipe> =
        listOf(
                objs.uncooked_redberry_pie,
                objs.uncooked_meat_pie,
                objs.uncooked_mud_pie,
                objs.uncooked_apple_pie,
                objs.uncooked_garden_pie,
                objs.uncooked_fish_pie,
                objs.uncooked_botanical_pie,
                objs.uncooked_mushroom_pie,
                objs.uncooked_admiral_pie,
                objs.uncooked_dragonfruit_pie,
                objs.uncooked_wild_pie,
                objs.uncooked_summer_pie,
            )
            .map { raw -> requireNotNull(StandardCookingRecipes.byRaw[raw.id]) }

    public val byRaw: Map<Int, StandardCookingRecipe> = all.associateBy { it.raw.id }
}
