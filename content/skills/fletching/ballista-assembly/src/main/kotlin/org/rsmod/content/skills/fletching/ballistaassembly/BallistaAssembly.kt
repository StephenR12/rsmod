package org.rsmod.content.skills.fletching.ballistaassembly

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat

public class BallistaAssembly @Inject constructor() {
    public fun assemble(access: ProtectedAccess, recipe: BallistaAssemblyRecipe) {
        with(access) {
            if (!canAssemble(recipe)) {
                return
            }
            clearFletchingQueues()
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.primary.id, 1)
                    delete(inventory, recipe.secondary.id, 1)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) {
                "Ballista-assembly transaction failed after validation: $recipe"
            }
            statAdvance(stats.fletching, recipe.xp)
        }
    }

    private fun ProtectedAccess.canAssemble(recipe: BallistaAssemblyRecipe): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to make this ballista.")
            return false
        }
        if (player.stat(stats.fletching) < recipe.level) {
            mes("You need a Fletching level of ${recipe.level} to make this ballista.")
            return false
        }
        if (!player.inv.contains(recipe.primary) || !player.inv.contains(recipe.secondary)) {
            mes("You do not have the components needed to make this ballista.")
            return false
        }
        return true
    }
}
