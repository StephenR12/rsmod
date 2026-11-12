package org.rsmod.content.skills.fletching.ammunition

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjTypeList

public class PoisonedAmmunition @Inject constructor(private val objTypes: ObjTypeList) {
    public fun poison(access: ProtectedAccess, recipe: PoisonedAmmunitionRecipe) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to poison ammunition.")
                return
            }
            val available = player.inv.count(objTypes[recipe.ammunition])
            if (available <= 0 || !player.inv.contains(recipe.poison)) {
                return
            }
            clearFletchingQueues()
            val amount = available.coerceAtMost(POISONED_PER_VIAL)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.ammunition.id, amount)
                    delete(inventory, recipe.poison.id, 1)
                    add(inventory, recipe.output.id, amount)
                    add(inventory, objs.empty_vial.id, 1)
                }
            if (!result.success) {
                mes("You don't have enough inventory space.")
                return
            }
        }
    }

    private companion object {
        const val POISONED_PER_VIAL = 5
    }
}
