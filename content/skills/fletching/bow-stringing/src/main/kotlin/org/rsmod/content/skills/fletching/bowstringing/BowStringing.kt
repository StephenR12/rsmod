package org.rsmod.content.skills.fletching.bowstringing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class BowStringing
@Inject
constructor(
    private val recipes: BowStringingRecipeRegistry,
    private val spool: BowStringSpool,
    private val objTypes: ObjTypeList,
) {
    public suspend fun select(
        access: ProtectedAccess,
        recipe: BowStringingRecipe,
        source: BowStringSource = BowStringSource.Loose,
    ) {
        with(access) {
            if (!canString(recipe, source, sendFailure = source == BowStringSource.Loose)) {
                return
            }
            val maximum = maximumActions(this, recipe, source).coerceAtLeast(1)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, recipe, input.subcomponent, source)
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: BowStringingRecipe,
        quantity: Int,
        source: BowStringSource = BowStringSource.Loose,
    ) {
        require(quantity >= 1) { "Bow-stringing quantity must be positive." }
        with(access) {
            if (!canString(recipe, source, sendFailure = source == BowStringSource.Loose)) {
                return
            }
            clearFletchingQueues()
            beginAction(recipe)
            weakQueue(
                fletching_queues.bowStringing,
                ACTION_TICKS,
                BowStringingRequest(recipe.unstrung, quantity, source),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: BowStringingRequest) {
        val recipe = recipes.find(request.unstrung) ?: return
        with(access) {
            if (!canString(recipe, request.source, sendFailure = false)) {
                resetAnim()
                return
            }
            string(recipe, request.source)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canString(recipe, request.source, sendFailure = false)) {
                resetAnim()
                return
            }
            beginAction(recipe)
            weakQueue(
                fletching_queues.bowStringing,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.beginAction(recipe: BowStringingRecipe) {
        anim(recipe.sequence)
    }

    private fun ProtectedAccess.string(recipe: BowStringingRecipe, source: BowStringSource) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.unstrung.id, 1)
                if (source == BowStringSource.Loose) {
                    delete(inventory, objs.bow_string.id, 1)
                }
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Bow-stringing transaction failed after validation: $recipe" }
        if (source == BowStringSource.Spool) {
            check(spool.consume(this)) { "Bow string spool became unavailable after validation." }
        }
        statAdvance(stats.fletching, recipe.xp)
    }

    private fun ProtectedAccess.canString(
        recipe: BowStringingRecipe,
        source: BowStringSource,
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
            return reject("You need a Fletching level of ${recipe.level} to string this bow.")
        }
        if (recipe.requiresCompOgreBowUnlock && player.vars[varbits.comp_ogre_bow_unlocked] == 0) {
            return reject("You're not sure how you would go about making that item.")
        }
        if (!player.inv.contains(recipe.unstrung)) {
            return reject("You do not have an unstrung bow to string.")
        }
        if (source == BowStringSource.Loose && !player.inv.contains(objs.bow_string)) {
            return reject("You need a bow string to string this bow.")
        }
        if (source == BowStringSource.Spool && !spool.canSupply(this)) {
            return false
        }
        return true
    }

    private fun maximumActions(
        access: ProtectedAccess,
        recipe: BowStringingRecipe,
        source: BowStringSource,
    ): Int =
        minOf(
            access.player.inv.count(objTypes[recipe.unstrung]),
            if (source == BowStringSource.Loose) {
                access.player.inv.count(objTypes[objs.bow_string])
            } else {
                spool.charges(access)
            },
        )

    private companion object {
        const val ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val MAKE_ACTION_MODE = 0
        const val MENU_TITLE = "How many would you like to make?"
    }
}

public enum class BowStringSource {
    Loose,
    Spool,
}

public data class BowStringingRequest(
    val unstrung: ObjType,
    val remaining: Int,
    val source: BowStringSource = BowStringSource.Loose,
)
