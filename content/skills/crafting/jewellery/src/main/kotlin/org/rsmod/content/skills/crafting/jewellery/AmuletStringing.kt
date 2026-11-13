package org.rsmod.content.skills.crafting.jewellery

import org.rsmod.api.player.diary.recordDiaryCraft
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class AmuletStringing
@Inject
constructor(private val recipes: AmuletStringingRecipeRegistry, private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, unstrung: ObjType) {
        val recipe = recipes.forUnstrung(unstrung) ?: return
        with(access) {
            if (!canString(recipe, sendFailure = true)) {
                return
            }
            clearHandcraftQueues()
            weakQueue(
                handcraft_queues.amuletStringing,
                ACTION_TICKS,
                AmuletStringingRequest(unstrung),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: AmuletStringingRequest) {
        val recipe = recipes.forUnstrung(request.unstrung) ?: return
        with(access) {
            if (!canString(recipe, sendFailure = false)) {
                return
            }
            string(recipe)
            if (!canString(recipe, sendFailure = false)) {
                return
            }
            weakQueue(
                handcraft_queues.amuletStringing,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request,
            )
        }
    }

    private fun ProtectedAccess.string(recipe: AmuletStringingRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.unstrung.id, 1)
                delete(inventory, objs.ball_of_wool.id, 1)
                add(inventory, recipe.output.id, 1)
            }
        check(result.success) { "Amulet-stringing transaction failed after validation: $recipe" }
        player.recordDiaryCraft(recipe.output.id, player.coords)
        if (recipe.xp > 0.0) {
            statAdvance(stats.crafting, recipe.xp)
        }
    }

    private fun ProtectedAccess.canString(
        recipe: AmuletStringingRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to string this item.")
        }
        if (player.inv.count(objTypes[recipe.unstrung]) == 0) {
            return reject("You do not have any ${recipe.name.lowercase()} left to string.")
        }
        if (player.inv.count(objTypes[objs.ball_of_wool]) == 0) {
            return reject("You need a ball of wool to string this item.")
        }
        return true
    }

    private companion object {
        const val ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
    }
}
