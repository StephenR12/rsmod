package org.rsmod.content.skills.herblore

import org.rsmod.api.player.diary.recordDiaryPotion
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
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

public class BarbarianMixing @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, recipe: BarbarianMixRecipe, egg: ObjType) {
        with(access) {
            if (!canMix(recipe, egg, sendFailure = true)) {
                return
            }
            val maximum =
                minOf(player.inv.count(objTypes[recipe.potion]), player.inv.count(objTypes[egg]))
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, egg, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: BarbarianMixRecipe,
        egg: ObjType,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Barbarian-mix quantity must be positive." }
        with(access) {
            if (!canMix(recipe, egg, sendFailure = true)) {
                return
            }
            clearHerbloreQueues()
            anim(seqs.human_herbing_vial)
            weakQueue(
                herblore_queues.barbarianMix,
                ACTION_TICKS,
                BarbarianMixRequest(recipe.output, egg, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: BarbarianMixRequest) {
        val recipe = BarbarianMixRecipes.byOutput[request.output.id] ?: return
        with(access) {
            if (!canMix(recipe, request.egg, sendFailure = false)) {
                resetAnim()
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.potion.id, 1)
                    delete(inventory, request.egg.id, 1)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) { "Barbarian-mix transaction failed after validation: $recipe" }
            mes(SUCCESS_MESSAGE)
            if (
                recipe.output == objs.attack_mix_2 &&
                    player.vars[varbits.brut_herb_potion] == TRAINING_TASKED
            ) {
                vars[varbits.brut_herb_potion] = TRAINING_MIX_MADE
                mesboxNp(TRAINING_MESSAGE)
            }
            statAdvance(stats.herblore, recipe.xp)
            player.recordDiaryPotion(recipe.output.id, player.coords)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMix(recipe, request.egg, sendFailure = false)) {
                resetAnim()
                return
            }
            weakQueue(
                herblore_queues.barbarianMix,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.canMix(
        recipe: BarbarianMixRecipe,
        egg: ObjType,
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
        val training = player.vars[varbits.brut_herb_potion]
        val isTrainingAttackMix = recipe.output == objs.attack_mix_2 && training >= TRAINING_TASKED
        if (!isTrainingAttackMix && training < TRAINING_COMPLETE) {
            return reject("You need to complete Otto's barbarian Herblore training first.")
        }
        if (player.stat(stats.herblore) < recipe.level) {
            return reject("You need a Herblore level of ${recipe.level} to make this potion.")
        }
        if (egg != objs.caviar && !(recipe.acceptsRoe && egg == objs.roe)) {
            return false
        }
        return player.inv.count(objTypes[recipe.potion]) > 0 && player.inv.count(objTypes[egg]) > 0
    }

    public companion object {
        public const val ACTION_TICKS: Int = 1
        public const val TRAINING_TASKED: Int = 1
        public const val TRAINING_MIX_MADE: Int = 2
        public const val TRAINING_COMPLETE: Int = 3
        public const val SUCCESS_MESSAGE: String = "You combine your potion with the fish eggs."
        public const val TRAINING_MESSAGE: String =
            "You feel you have learned more of barbarian ways. Otto might wish to talk to you more."
        private const val DRUIDIC_RITUAL_COMPLETE: Int = 4
        private const val RESCHEDULE_TICK_COMPENSATION: Int = 1
        private const val MAKE_ACTION_MODE: Int = 0
        private const val MENU_TITLE: String = "How many would you like to make?"
    }
}

public data class BarbarianMixRequest(
    public val output: ObjType,
    public val egg: ObjType,
    public val remaining: Int,
)
