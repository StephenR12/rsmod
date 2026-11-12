package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType

public data class ForgottenBrewUpgradeRecipe(
    public val ancientBrew: ObjType,
    public val forgottenBrew: ObjType,
    public val essence: Int,
    public val xp: Double,
)

public object ForgottenBrewUpgradeRecipes {
    public val all: List<ForgottenBrewUpgradeRecipe> =
        listOf(
            ForgottenBrewUpgradeRecipe(objs.ancient_brew_1, objs.forgotten_brew_1, 20, 36.0),
            ForgottenBrewUpgradeRecipe(objs.ancient_brew_2, objs.forgotten_brew_2, 40, 72.0),
            ForgottenBrewUpgradeRecipe(objs.ancient_brew_3, objs.forgotten_brew_3, 60, 108.0),
            ForgottenBrewUpgradeRecipe(objs.ancient_brew_4, objs.forgotten_brew_4, 80, 145.0),
        )
}

public data class ForgottenBrewUpgradeRequest(
    public val brewSlot: Int,
    public val essenceSlot: Int,
    public val output: ObjType,
)

/** Exact cache-233 resource, level, duration and XP core for upgrading Ancient brews. */
public class ForgottenBrewUpgrading @Inject constructor() {
    public fun start(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        recipe: ForgottenBrewUpgradeRecipe,
    ) {
        with(access) {
            clearHerbloreQueues()
            val brewSlot = slotOf(firstSlot, secondSlot, recipe.ancientBrew) ?: return
            val essenceSlot = slotOf(firstSlot, secondSlot, objs.ancient_essence) ?: return
            if (!canUpgrade(brewSlot, essenceSlot, recipe, sendFailure = true)) {
                return
            }
            weakQueue(
                herblore_queues.forgottenBrewUpgrade,
                ACTION_TICKS,
                ForgottenBrewUpgradeRequest(brewSlot, essenceSlot, recipe.forgottenBrew),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: ForgottenBrewUpgradeRequest) {
        val recipe =
            ForgottenBrewUpgradeRecipes.all.firstOrNull { it.forgottenBrew == request.output }
                ?: return
        with(access) {
            if (!canUpgrade(request.brewSlot, request.essenceSlot, recipe, sendFailure = false)) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.ancientBrew.id, 1, slot = request.brewSlot)
                    delete(
                        inventory,
                        objs.ancient_essence.id,
                        recipe.essence,
                        slot = request.essenceSlot,
                    )
                    add(inventory, recipe.forgottenBrew.id, 1, slot = request.brewSlot)
                }
            check(result.success) { "Forgotten-brew upgrade failed after validation: $recipe" }
            statAdvance(stats.herblore, recipe.xp)
        }
    }

    private fun ProtectedAccess.slotOf(firstSlot: Int, secondSlot: Int, obj: ObjType): Int? =
        when {
            firstSlot in player.inv.indices && player.inv[firstSlot]?.id == obj.id -> firstSlot
            secondSlot in player.inv.indices && player.inv[secondSlot]?.id == obj.id -> secondSlot
            else -> null
        }

    private fun ProtectedAccess.canUpgrade(
        brewSlot: Int,
        essenceSlot: Int,
        recipe: ForgottenBrewUpgradeRecipe,
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
        if (player.stat(stats.herblore) < REQUIRED_LEVEL) {
            return reject("You need a Herblore level of $REQUIRED_LEVEL to make this potion.")
        }
        if (brewSlot !in player.inv.indices || essenceSlot !in player.inv.indices) {
            return false
        }
        return player.inv[brewSlot]?.id == recipe.ancientBrew.id &&
            player.inv[essenceSlot]?.let {
                it.id == objs.ancient_essence.id && it.count >= recipe.essence
            } == true
    }

    public companion object {
        public const val REQUIRED_LEVEL: Int = 91
        public const val ACTION_TICKS: Int = 3
        private const val DRUIDIC_RITUAL_COMPLETE: Int = 4
    }
}
