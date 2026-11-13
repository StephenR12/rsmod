package org.rsmod.content.skills.herblore

import org.rsmod.api.player.diary.recordDiaryPotion
import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
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

public class UnfinishedPotionMixing
@Inject
constructor(private val objTypes: ObjTypeList, private val modifiers: PotionProductionModifiers) {
    public suspend fun select(access: ProtectedAccess, recipe: UnfinishedPotionRecipe) {
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            val maximum =
                minOf(
                    player.inv.count(objTypes[recipe.herb]),
                    player.inv.count(objTypes[recipe.base]),
                )
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: UnfinishedPotionRecipe, quantity: Int) {
        require(quantity >= 1) { "Unfinished-potion quantity must be positive." }
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            clearHerbloreQueues()
            anim(seqs.human_herbing_vial)
            weakQueue(
                herblore_queues.unfinishedPotions,
                recipe.ticks,
                UnfinishedPotionRequest(recipe.base, recipe.herb, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: UnfinishedPotionRequest) {
        val recipe =
            UnfinishedPotionRecipes.byInputs[Pair(request.base.id, request.herb.id)] ?: return
        with(access) {
            if (!canMix(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            mix(recipe)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMix(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            weakQueue(
                herblore_queues.unfinishedPotions,
                recipe.ticks + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.mix(recipe: UnfinishedPotionRecipe) {
        val preserveHerb =
            recipe.preserveHerbWithGoggles && modifiers.rollSecondaryPreservation(this)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                if (!preserveHerb) {
                    delete(inventory, recipe.herb.id, 1)
                }
                delete(inventory, recipe.base.id, 1)
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Unfinished-potion transaction failed after validation: $recipe" }
        player.recordDiaryPotion(recipe.output.id, player.coords)
        mes(
            "You put the ${objTypes[recipe.herb].name} into the " +
                "${objTypes[recipe.base].name.lowercase()}."
        )
    }

    private fun ProtectedAccess.canMix(
        recipe: UnfinishedPotionRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make potions.")
        }
        if (player.vars[varps.druidic_ritual] < DRUIDIC_RITUAL_COMPLETE) {
            return reject("You need to complete Druidic Ritual before training Herblore.")
        }
        if (player.stat(stats.herblore) < recipe.level) {
            return reject("You need a Herblore level of ${recipe.level} to make this potion.")
        }
        if (player.inv.count(objTypes[recipe.herb]) == 0) {
            return false
        }
        if (player.inv.count(objTypes[recipe.base]) == 0) {
            return false
        }
        return true
    }

    private companion object {
        const val DRUIDIC_RITUAL_COMPLETE = 4
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "How many would you like to make?"
    }
}

public data class UnfinishedPotionRequest(val base: ObjType, val herb: ObjType, val remaining: Int)
