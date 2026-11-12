package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType

public class OilyFishingRodMaking @Inject constructor() {
    public fun start(
        access: ProtectedAccess,
        oilSlot: Int,
        rodSlot: Int,
        recipe: OilyFishingRodRecipe,
    ) {
        with(access) {
            clearHerbloreQueues()
            if (!canMake(oilSlot, rodSlot, recipe, sendFailure = true)) {
                return
            }
            if (recipe.ticks == 0) {
                make(oilSlot, rodSlot, recipe)
            } else {
                weakQueue(
                    herblore_queues.oilyFishingRod,
                    recipe.ticks,
                    OilyFishingRodRequest(oilSlot, rodSlot, recipe.output),
                )
            }
        }
    }

    public fun process(access: ProtectedAccess, request: OilyFishingRodRequest) {
        val recipe = OilyFishingRodRecipes.byOutput[request.output.id] ?: return
        with(access) {
            if (!canMake(request.oilSlot, request.rodSlot, recipe, sendFailure = false)) {
                return
            }
            make(request.oilSlot, request.rodSlot, recipe)
        }
    }

    private fun ProtectedAccess.make(oilSlot: Int, rodSlot: Int, recipe: OilyFishingRodRecipe) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.blamish_oil.id, 1, slot = oilSlot)
                delete(inventory, recipe.rod.id, 1, slot = rodSlot)
                add(inventory, recipe.output.id, 1, slot = rodSlot)
            }
        check(result.success) { "Oily-fishing-rod transaction failed after validation: $recipe" }
    }

    private fun ProtectedAccess.canMake(
        oilSlot: Int,
        rodSlot: Int,
        recipe: OilyFishingRodRecipe,
        sendFailure: Boolean,
    ): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to use this item.")
            }
            return false
        }
        return player.inv[oilSlot]?.id == objs.blamish_oil.id &&
            player.inv[rodSlot]?.id == recipe.rod.id
    }
}

public data class OilyFishingRodRecipe(val rod: ObjType, val output: ObjType, val ticks: Int)

public object OilyFishingRodRecipes {
    public val all: List<OilyFishingRodRecipe> =
        listOf(
            OilyFishingRodRecipe(objs.fishing_rod, objs.oily_fishing_rod, ticks = 0),
            OilyFishingRodRecipe(objs.pearl_fishing_rod, objs.oily_pearl_fishing_rod, ticks = 1),
        )

    public val byOutput: Map<Int, OilyFishingRodRecipe> = all.associateBy { it.output.id }
}

public data class OilyFishingRodRequest(val oilSlot: Int, val rodSlot: Int, val output: ObjType)
