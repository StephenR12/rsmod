package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class HerbCleaning @Inject constructor(private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, slot: Int, recipe: HerbCleaningRecipe) {
        with(access) {
            clearHerbloreQueues()
            if (!canClean(slot, recipe, sendFailure = true)) {
                return
            }
            clean(slot, recipe)
            scheduleNext(recipe, INITIAL_AUTO_CLEAN_TICKS)
        }
    }

    public fun process(access: ProtectedAccess, request: HerbCleaningRequest) {
        val recipe = HerbCleaningRecipes.byGrimy[request.grimy.id] ?: return
        with(access) {
            val slot = player.inv.indexOfFirst { it?.id == recipe.grimy.id }
            if (slot < 0 || !canClean(slot, recipe, sendFailure = false)) {
                return
            }
            clean(slot, recipe)
            scheduleNext(recipe, REPEAT_AUTO_CLEAN_TICKS)
        }
    }

    private fun ProtectedAccess.clean(slot: Int, recipe: HerbCleaningRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.grimy.id, 1, slot = slot)
                add(inventory, recipe.clean.id, 1, slot = slot)
            }
        check(result.success) { "Herb-cleaning transaction failed after validation: $recipe" }
        statAdvance(stats.herblore, recipe.xp)
        mes("You clean the dirt from the ${objTypes[recipe.clean].name.lowercase()}.")
    }

    private fun ProtectedAccess.scheduleNext(recipe: HerbCleaningRecipe, delay: Int) {
        if (player.inv.any { it?.id == recipe.grimy.id }) {
            weakQueue(herblore_queues.herbCleaning, delay, HerbCleaningRequest(recipe.grimy))
        }
    }

    private fun ProtectedAccess.canClean(
        slot: Int,
        recipe: HerbCleaningRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to clean herbs.")
        }
        if (player.vars[varps.druidic_ritual] < DRUIDIC_RITUAL_COMPLETE) {
            return reject("You need to complete Druidic Ritual before training Herblore.")
        }
        if (player.inv[slot]?.id != recipe.grimy.id) {
            return false
        }
        if (player.stat(stats.herblore) < recipe.level) {
            return reject("You do not have a high enough Herblore level to clean this herb.")
        }
        return true
    }

    private companion object {
        const val DRUIDIC_RITUAL_COMPLETE = 4
        const val INITIAL_AUTO_CLEAN_TICKS = 2
        // Queue handlers consume one cycle while running, so repeat work needs compensation.
        const val REPEAT_AUTO_CLEAN_TICKS = 3
    }
}

public data class HerbCleaningRequest(val grimy: ObjType)
