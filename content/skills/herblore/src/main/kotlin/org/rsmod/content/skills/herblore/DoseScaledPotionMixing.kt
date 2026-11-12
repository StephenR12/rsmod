package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class DoseScaledPotionMixing
@Inject
constructor(private val objTypes: ObjTypeList, private val modifiers: PotionProductionModifiers) {
    public suspend fun select(access: ProtectedAccess, recipe: DoseScaledPotionRecipe) {
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            val maximum =
                minOf(
                    player.inv.count(objTypes[recipe.potion]),
                    player.inv.count(objTypes[recipe.secondary]) / recipe.secondaryQuantity,
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

    public fun start(access: ProtectedAccess, recipe: DoseScaledPotionRecipe, quantity: Int) {
        require(quantity >= 1) { "Dose-scaled potion quantity must be positive." }
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            clearHerbloreQueues()
            anim(seqs.human_herbing_vial)
            weakQueue(
                herblore_queues.doseScaledPotions,
                recipe.ticks,
                DoseScaledPotionRequest(recipe.potion, recipe.secondary, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: DoseScaledPotionRequest) {
        val recipe =
            DoseScaledPotionRecipes.byInputs[request.potion.id to request.secondary.id] ?: return
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
                herblore_queues.doseScaledPotions,
                recipe.ticks + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.mix(recipe: DoseScaledPotionRecipe) {
        val roll = modifiers.roll(this, recipe.output, recipe.extraDoseOutput)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.potion.id, 1)
                if (!roll.preserveSecondary) {
                    delete(inventory, recipe.secondary.id, recipe.secondaryQuantity)
                }
                add(inventory, roll.output.id, 1)
            }
        check(result.success) { "Dose-scaled potion transaction failed after validation: $recipe" }
        val commit = modifiers.commit(this, roll)
        statAdvance(stats.herblore, recipe.xp)
        val chemistryCharges = commit.chemistryChargesRemaining
        if (chemistryCharges != null) {
            val resultingDoses = recipe.doses + 1
            if (chemistryCharges == 0) {
                mes(
                    "Your amulet of chemistry helps you create a $resultingDoses-dose potion. " +
                        "It then crumbles to dust."
                )
            } else {
                val noun = if (chemistryCharges == 1) "charge" else "charges"
                val count = if (chemistryCharges == 1) "one" else chemistryCharges.toString()
                mes(
                    "Your amulet of chemistry helps you create a $resultingDoses-dose potion. " +
                        "It has $count $noun left."
                )
            }
        } else {
            mes("You mix the ${objTypes[recipe.secondary].name.lowercase()} into your potion.")
        }
    }

    private fun ProtectedAccess.canMix(
        recipe: DoseScaledPotionRecipe,
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
        if (
            recipe.requiresSuperAntifireUnlock && player.vars[varbits.super_antifire_unlocked] == 0
        ) {
            return reject("You need to speak to Primula before you can make this potion.")
        }
        if (player.stat(stats.herblore) < recipe.level) {
            return reject("You need a Herblore level of ${recipe.level} to make this potion.")
        }
        if (player.inv.count(objTypes[recipe.potion]) == 0) {
            return false
        }
        if (player.inv.count(objTypes[recipe.secondary]) < recipe.secondaryQuantity) {
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

public data class DoseScaledPotionRequest(
    public val potion: ObjType,
    public val secondary: ObjType,
    public val remaining: Int,
)
