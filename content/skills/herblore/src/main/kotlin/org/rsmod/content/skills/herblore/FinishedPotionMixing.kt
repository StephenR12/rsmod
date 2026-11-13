package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.recordDiaryPotion
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class FinishedPotionMixing
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val modifiers: PotionProductionModifiers,
    private val pouch: org.rsmod.content.skills.herblore.mixology.ReagentPouch,
) {
    public suspend fun select(access: ProtectedAccess, recipe: FinishedPotionRecipe) {
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            val maximum =
                minOf(
                    player.inv.count(objTypes[recipe.unfinished]),
                    pouch.quantity(this, recipe.secondary),
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

    public fun start(access: ProtectedAccess, recipe: FinishedPotionRecipe, quantity: Int) {
        require(quantity >= 1) { "Finished-potion quantity must be positive." }
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            clearHerbloreQueues()
            anim(seqs.human_herbing_vial)
            weakQueue(
                herblore_queues.finishedPotions,
                FIRST_ACTION_TICKS,
                FinishedPotionRequest(recipe.output, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: FinishedPotionRequest) {
        val recipe = FinishedPotionRecipes.byOutput[request.output.id] ?: return
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
                herblore_queues.finishedPotions,
                REPEAT_ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.mix(recipe: FinishedPotionRecipe) {
        val roll = modifiers.roll(this, recipe)
        val secondarySource = pouch.source(this, recipe.secondary)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.unfinished.id, 1)
                if (!roll.preserveSecondary) {
                    val secondary =
                        if (secondarySource === player.inv) inventory else select(secondarySource)
                    delete(secondary, recipe.secondary.id, 1)
                }
                add(inventory, roll.output.id, 1)
            }
        check(result.success) { "Finished-potion transaction failed after validation: $recipe" }
        val commit = modifiers.commit(this, roll)
        statAdvance(stats.herblore, recipe.xp)
        player.recordDiaryPotion(recipe.output.id, player.coords)
        val chemistryCharges = commit.chemistryChargesRemaining
        if (chemistryCharges != null) {
            if (chemistryCharges == 0) {
                mes(
                    "Your amulet of chemistry helps you create a 4-dose potion. " +
                        "It then crumbles to dust."
                )
            } else {
                val noun = if (chemistryCharges == 1) "charge" else "charges"
                val count = if (chemistryCharges == 1) "one" else chemistryCharges.toString()
                mes(
                    "Your amulet of chemistry helps you create a 4-dose potion. " +
                        "It has $count $noun left."
                )
            }
        } else {
            mes("You mix the ${objTypes[recipe.secondary].name.lowercase()} into your potion.")
        }
    }

    private fun ProtectedAccess.canMix(
        recipe: FinishedPotionRecipe,
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
        if (player.inv.count(objTypes[recipe.unfinished]) == 0) {
            return false
        }
        if (pouch.quantity(this, recipe.secondary) == 0) {
            return false
        }
        return true
    }

    private companion object {
        const val DRUIDIC_RITUAL_COMPLETE = 4
        const val FIRST_ACTION_TICKS = 2
        const val REPEAT_ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "How many would you like to make?"
    }
}

public data class FinishedPotionRequest(val output: ObjType, val remaining: Int)
