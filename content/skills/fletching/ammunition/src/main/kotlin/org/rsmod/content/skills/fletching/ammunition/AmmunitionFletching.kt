package org.rsmod.content.skills.fletching.ammunition

import org.rsmod.api.player.diary.recordDiaryFletch
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
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

public class AmmunitionFletching
@Inject
constructor(
    private val recipes: AmmunitionFletchingRecipeRegistry,
    private val objTypes: ObjTypeList,
) {
    public suspend fun select(access: ProtectedAccess, recipe: AmmunitionFletchingRecipe) {
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            val available = availableItems(this, recipe)
            if (!usesMakeX(this, recipe) || available < recipe.family.batchSize) {
                start(this, recipe, quantity = 1)
                return
            }
            val maximum =
                ((available + recipe.family.batchSize - 1) / recipe.family.batchSize).coerceAtMost(
                    MAX_MAKE_X_SETS
                )
            val input =
                skillMulti(
                    title = menuTitle(recipe),
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, recipe: AmmunitionFletchingRecipe, quantity: Int) {
        require(quantity in 1..MAX_MAKE_X_SETS) {
            "Ammunition Fletching quantity must be between 1 and $MAX_MAKE_X_SETS."
        }
        with(access) {
            if (!canMake(recipe, sendFailure = true)) {
                return
            }
            clearFletchingQueues()
            beginAction(recipe)
            if (
                !usesMakeX(this, recipe) &&
                    recipe.family != AmmunitionFamily.TippedBolt &&
                    recipe.family != AmmunitionFamily.FlightedOgreArrow &&
                    recipe.family != AmmunitionFamily.OgreArrow &&
                    recipe.family != AmmunitionFamily.BrutalArrow
            ) {
                makeSet(recipe)
                return
            }
            weakQueue(
                fletching_queues.ammunition,
                initialDelay(recipe),
                AmmunitionFletchingRequest(recipe.output, quantity, completed = 0),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: AmmunitionFletchingRequest) {
        val recipe = recipes.find(request.output) ?: return
        with(access) {
            if (!canMake(recipe, sendFailure = false)) {
                endAction(recipe)
                return
            }
            makeSet(recipe)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMake(recipe, sendFailure = false)) {
                endAction(recipe)
                return
            }
            beginAction(recipe)
            weakQueue(
                fletching_queues.ammunition,
                repeatDelay(recipe, request.completed + 1),
                request.copy(remaining = remaining, completed = request.completed + 1),
            )
        }
    }

    private fun ProtectedAccess.beginAction(recipe: AmmunitionFletchingRecipe) {
        recipe.sequence?.let(::anim)
    }

    private fun ProtectedAccess.endAction(recipe: AmmunitionFletchingRecipe) {
        if (recipe.sequence != null) {
            resetAnim()
        }
    }

    private fun ProtectedAccess.makeSet(recipe: AmmunitionFletchingRecipe) {
        val amount = availableItems(this, recipe).coerceAtMost(recipe.family.batchSize)
        check(amount > 0) { "Ammunition Fletching reached mutation without materials: $recipe" }
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.primary.id, amount)
                delete(inventory, recipe.secondary.id, amount * recipe.secondaryPerItem)
                add(inventory, recipe.output.id, amount)
            }
        check(result.success) {
            "Ammunition Fletching transaction failed after validation: $recipe"
        }
        statAdvance(stats.fletching, recipe.xpPerItem * amount)
        player.recordDiaryFletch(recipe.output.id, amount, player.coords)
    }

    private fun ProtectedAccess.canMake(
        recipe: AmmunitionFletchingRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to train Fletching.")
        }
        if (player.stat(stats.fletching) < recipe.level) {
            return reject(
                "You need a Fletching level of ${recipe.level} to make ${recipe.name.lowercase()}."
            )
        }
        if (recipe.requiresBroaderFletching && player.vars[varbits.slayer_ammo_unlocked] == 0) {
            return reject("You need to unlock Broader Fletching from a Slayer master first.")
        }
        if (
            recipe.requiresBigChompyStarted &&
                player.vars[varps.big_chompy_bird_hunting] == BIG_CHOMPY_UNSTARTED_STATE
        ) {
            return reject("You need to start Big Chompy Bird Hunting first.")
        }
        if (
            recipe.requiresBrutalArrowUnlock &&
                player.vars[varbits.zogre_brutal_arrow_unlocked] == 0
        ) {
            return reject("You need to learn how to make brutal arrows first.")
        }
        if (recipe.requiresHammer && !player.inv.contains(objs.hammer)) {
            return reject("You need a hammer to make brutal arrows.")
        }
        if (!player.inv.contains(recipe.primary)) {
            return reject(
                "You do not have the required ${objTypes[recipe.primary].name.lowercase()}."
            )
        }
        if (player.inv.count(objTypes[recipe.secondary]) < recipe.secondaryPerItem) {
            return reject(
                "You do not have the required ${objTypes[recipe.secondary].name.lowercase()}."
            )
        }
        return true
    }

    private fun usesMakeX(access: ProtectedAccess, recipe: AmmunitionFletchingRecipe): Boolean =
        recipe.family.alwaysMakeX ||
            (recipe.family.usesOptionalMakeX &&
                access.player.vars[varbits.option_fletch_dart_or_bolt_makex] != 0)

    private fun availableItems(access: ProtectedAccess, recipe: AmmunitionFletchingRecipe): Int =
        minOf(
            access.player.inv.count(objTypes[recipe.primary]),
            access.player.inv.count(objTypes[recipe.secondary]) / recipe.secondaryPerItem,
        )

    private fun menuTitle(recipe: AmmunitionFletchingRecipe): String =
        "How many sets of ${recipe.family.batchSize} would you like to make?"

    private fun initialDelay(recipe: AmmunitionFletchingRecipe): Int =
        if (recipe.family == AmmunitionFamily.Javelin) JAVELIN_INITIAL_TICKS else ACTION_TICKS

    private fun repeatDelay(recipe: AmmunitionFletchingRecipe, completed: Int): Int =
        if (recipe.family == AmmunitionFamily.Javelin && completed == 1) {
            JAVELIN_RAPID_REPEAT_TICKS + RESCHEDULE_TICK_COMPENSATION
        } else {
            ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION
        }

    private companion object {
        const val ACTION_TICKS = 2
        const val JAVELIN_INITIAL_TICKS = 1
        const val JAVELIN_RAPID_REPEAT_TICKS = 1
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAX_MAKE_X_SETS = 10
        const val MAKE_ACTION_MODE = 0
        const val BIG_CHOMPY_UNSTARTED_STATE = 0
    }
}

public data class AmmunitionFletchingRequest(
    val output: ObjType,
    val remaining: Int,
    val completed: Int = 0,
)
