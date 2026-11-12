package org.rsmod.content.skills.crafting.glassmaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.smithing.smelting.clearFurnaceQueues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class GlassAssembly @Inject constructor(private val objTypes: ObjTypeList) {
    public fun makeOilLantern(access: ProtectedAccess) {
        assemble(
            access,
            AssemblyRecipe(
                level = 26,
                first = objs.empty_oil_lamp,
                second = objs.oil_lantern_frame,
                output = objs.empty_oil_lantern,
                xp = 50.0,
            ),
        )
    }

    public fun wireLightOrb(access: ProtectedAccess) {
        assemble(
            access,
            AssemblyRecipe(
                level = 87,
                first = objs.empty_light_orb,
                second = objs.cave_goblin_wire,
                output = objs.light_orb,
                xp = 104.0,
            ),
        )
    }

    private fun assemble(access: ProtectedAccess, recipe: AssemblyRecipe) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to make this item.")
                return
            }
            if (player.stat(stats.crafting) < recipe.level) {
                mes("You need a Crafting level of ${recipe.level} to make this item.")
                return
            }
            if (
                player.inv.count(objTypes[recipe.first]) == 0 ||
                    player.inv.count(objTypes[recipe.second]) == 0
            ) {
                return
            }
            clearCraftingQueues()
            clearFurnaceQueues()
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.first.id, 1)
                    delete(inventory, recipe.second.id, 1)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) { "Glass-assembly transaction failed after validation: $recipe" }
            statAdvance(stats.crafting, recipe.xp)
        }
    }

    private data class AssemblyRecipe(
        val level: Int,
        val first: ObjType,
        val second: ObjType,
        val output: ObjType,
        val xp: Double,
    )
}
