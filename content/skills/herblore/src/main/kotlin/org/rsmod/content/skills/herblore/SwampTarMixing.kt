package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
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

/** Cache-233 three-tick, fifteen-at-a-time salamander-tar production. */
public class SwampTarMixing @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, recipe: SwampTarRecipe) {
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            val maximum = maximum(recipe)
            if (maximum == 1) {
                start(this, recipe, 1)
                return
            }
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

    public fun start(access: ProtectedAccess, recipe: SwampTarRecipe, quantity: Int) {
        require(quantity >= 1) { "Swamp-tar quantity must be positive." }
        with(access) {
            if (!canMix(recipe, sendFailure = true)) {
                return
            }
            clearHerbloreQueues()
            anim(seqs.human_salamander_tar_grind)
            weakQueue(
                herblore_queues.swampTar,
                ACTION_TICKS,
                SwampTarRequest(recipe.herb, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: SwampTarRequest) {
        val recipe = SwampTarRecipes.byHerb[request.herb.id] ?: return
        with(access) {
            if (!canMix(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.herb.id, 1)
                    delete(inventory, objs.swamp_tar.id, SWAMP_TAR_PER_ACTION)
                    add(inventory, recipe.output.id, SWAMP_TAR_PER_ACTION)
                }
            check(result.success) { "Swamp-tar transaction failed after validation: $recipe" }
            statAdvance(stats.herblore, recipe.xp)

            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMix(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            weakQueue(
                herblore_queues.swampTar,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.maximum(recipe: SwampTarRecipe): Int =
        minOf(
            player.inv.count(objTypes[recipe.herb]),
            player.inv.count(objTypes[objs.swamp_tar]) / SWAMP_TAR_PER_ACTION,
        )

    private fun ProtectedAccess.canMix(recipe: SwampTarRecipe, sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make tar.")
        }
        if (player.vars[varps.druidic_ritual] < DRUIDIC_RITUAL_COMPLETE) {
            return reject("You need to complete Druidic Ritual before training Herblore.")
        }
        if (player.stat(stats.herblore) < recipe.level) {
            return reject("You need a Herblore level of ${recipe.level} to make this tar.")
        }
        if (player.inv.count(objTypes[objs.pestle_and_mortar]) == 0) {
            return reject("You need a pestle and mortar to make herb tar.")
        }
        return maximum(recipe) > 0
    }

    public companion object {
        public const val SWAMP_TAR_PER_ACTION: Int = 15
        public const val ACTION_TICKS: Int = 3
        private const val DRUIDIC_RITUAL_COMPLETE: Int = 4
        private const val RESCHEDULE_TICK_COMPENSATION: Int = 1
        private const val MAKE_ACTION_MODE: Int = 0
        private const val MENU_TITLE: String = "How many would you like to make?"
    }
}

public data class SwampTarRequest(public val herb: ObjType, public val remaining: Int)
