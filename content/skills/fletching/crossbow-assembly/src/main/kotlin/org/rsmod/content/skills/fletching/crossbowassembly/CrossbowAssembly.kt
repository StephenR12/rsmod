package org.rsmod.content.skills.fletching.crossbowassembly

import jakarta.inject.Inject
import org.rsmod.api.player.diary.recordDiaryFletch
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class CrossbowAssembly
@Inject
constructor(
    private val recipes: CrossbowAssemblyRecipeRegistry,
    private val objTypes: ObjTypeList,
) {
    public suspend fun select(
        access: ProtectedAccess,
        recipe: CrossbowAssemblyRecipe,
        stage: CrossbowAssemblyStage,
    ) {
        with(access) {
            if (!canMake(recipe, stage, sendFailure = true)) {
                return
            }
            val maximum = maximumActions(this, recipe, stage).coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(output(recipe, stage)),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, stage, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: CrossbowAssemblyRecipe,
        stage: CrossbowAssemblyStage,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Crossbow-assembly quantity must be positive." }
        with(access) {
            if (!canMake(recipe, stage, sendFailure = true)) {
                return
            }
            clearFletchingQueues()
            beginAction(recipe, stage)
            weakQueue(
                fletching_queues.crossbowAssembly,
                ACTION_TICKS,
                CrossbowAssemblyRequest(recipe.unstrung, stage, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: CrossbowAssemblyRequest) {
        val recipe = recipes.find(request.recipe) ?: return
        with(access) {
            if (!canMake(recipe, request.stage, sendFailure = false)) {
                resetAnim()
                return
            }
            make(recipe, request.stage)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMake(recipe, request.stage, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction(recipe, request.stage)
            weakQueue(
                fletching_queues.crossbowAssembly,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction(
        recipe: CrossbowAssemblyRecipe,
        stage: CrossbowAssemblyStage,
    ) {
        anim(
            when (stage) {
                CrossbowAssemblyStage.Limbs -> recipe.limbSequence
                CrossbowAssemblyStage.String -> recipe.stringSequence
            }
        )
    }

    private fun ProtectedAccess.make(recipe: CrossbowAssemblyRecipe, stage: CrossbowAssemblyStage) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                when (stage) {
                    CrossbowAssemblyStage.Limbs -> {
                        delete(inventory, recipe.stock.id, 1)
                        delete(inventory, recipe.limbs.id, 1)
                        add(inventory, recipe.unstrung.id, 1)
                    }
                    CrossbowAssemblyStage.String -> {
                        delete(inventory, recipe.unstrung.id, 1)
                        delete(inventory, objs.crossbow_string.id, 1)
                        add(inventory, recipe.output.id, 1)
                    }
                }
            }
        check(result.success) { "Crossbow-assembly transaction failed after validation: $recipe" }
        player.recordDiaryFletch(if(stage==CrossbowAssemblyStage.Limbs)recipe.unstrung.id else recipe.output.id,1,player.coords)
        statAdvance(
            stats.fletching,
            when (stage) {
                CrossbowAssemblyStage.Limbs -> recipe.limbXp
                CrossbowAssemblyStage.String -> recipe.stringXp
            },
        )
        mes(
            when (stage) {
                CrossbowAssemblyStage.Limbs ->
                    "You attach the stock to the limbs and create an unstrung crossbow."
                CrossbowAssemblyStage.String -> "You add a string to the crossbow."
            }
        )
    }

    private fun ProtectedAccess.canMake(
        recipe: CrossbowAssemblyRecipe,
        stage: CrossbowAssemblyStage,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make this crossbow.")
        }
        if (player.stat(stats.fletching) < recipe.level) {
            return reject("You need a Fletching level of ${recipe.level} to make this crossbow.")
        }
        return when (stage) {
            CrossbowAssemblyStage.Limbs -> {
                if (!hasHammer()) {
                    reject("You need a hammer to make this crossbow.")
                } else if (!player.inv.contains(recipe.stock)) {
                    reject("You do not have the correct crossbow stock.")
                } else if (!player.inv.contains(recipe.limbs)) {
                    reject("You do not have the correct crossbow limbs.")
                } else {
                    true
                }
            }
            CrossbowAssemblyStage.String -> {
                if (!player.inv.contains(recipe.unstrung)) {
                    reject("You do not have an unstrung crossbow to string.")
                } else if (!player.inv.contains(objs.crossbow_string)) {
                    reject("You need a crossbow string to string this crossbow.")
                } else {
                    true
                }
            }
        }
    }

    private fun ProtectedAccess.hasHammer(): Boolean {
        val valid = setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
        return player.inv.any { it?.id in valid } || player.worn.any { it?.id in valid }
    }

    private fun maximumActions(
        access: ProtectedAccess,
        recipe: CrossbowAssemblyRecipe,
        stage: CrossbowAssemblyStage,
    ): Int =
        when (stage) {
            CrossbowAssemblyStage.Limbs ->
                minOf(
                    access.player.inv.count(objTypes[recipe.stock]),
                    access.player.inv.count(objTypes[recipe.limbs]),
                )
            CrossbowAssemblyStage.String ->
                minOf(
                    access.player.inv.count(objTypes[recipe.unstrung]),
                    access.player.inv.count(objTypes[objs.crossbow_string]),
                )
        }

    private fun output(recipe: CrossbowAssemblyRecipe, stage: CrossbowAssemblyStage): ObjType =
        when (stage) {
            CrossbowAssemblyStage.Limbs -> recipe.unstrung
            CrossbowAssemblyStage.String -> recipe.output
        }

    private companion object {
        const val ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "How many would you like to make?"
    }
}

public data class CrossbowAssemblyRequest(
    val recipe: ObjType,
    val stage: CrossbowAssemblyStage,
    val remaining: Int,
)
