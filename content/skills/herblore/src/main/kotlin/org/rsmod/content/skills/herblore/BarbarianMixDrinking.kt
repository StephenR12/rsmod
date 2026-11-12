package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statHeal
import org.rsmod.content.skills.cooking.FoodConsumptionRepository

/** Cache-233 Barbarian-mix drinking, including the shared food and attack delay. */
public class BarbarianMixDrinking @Inject constructor(private val food: FoodConsumptionRepository) {
    public fun drink(access: ProtectedAccess, slot: Int, recipe: BarbarianMixRecipe, doses: Int) {
        with(access) {
            if (slot !in player.inv.indices || doses !in 1..2) {
                return
            }
            val expected = if (doses == 2) recipe.output else recipe.oneDose
            if (player.inv[slot]?.id != expected.id || !food.isReady(player)) {
                return
            }
            if (!player.members) {
                mes(MEMBERS_MESSAGE)
                return
            }

            val replacement =
                when {
                    doses == 2 -> recipe.oneDose
                    player.vars[varbits.auto_smash_vials] == 0 -> objs.empty_vial
                    else -> null
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, expected.id, count = 1, slot = slot)
                    replacement?.let { add(inventory, it.id, count = 1, slot = slot) }
                }
            check(result.success) { "Barbarian-mix replacement failed after validation." }

            stopAction()
            anim(seqs.human_eat)
            mes(DRINK_MESSAGE)
            if (doses == 2) {
                mes("You have 1 dose of potion left.")
            } else {
                mes(FINISHED_MESSAGE)
            }
            food.consume(player, eatDelay = FOOD_DELAY_CYCLES, attackDelay = ATTACK_DELAY_CYCLES)
            publish(PlayerConsumableEvent.Consumed(player, expected, recipe.potion))
            statHeal(stats.hitpoints, constant = recipe.heal, percent = 0)
        }
    }

    public companion object {
        public const val FOOD_DELAY_CYCLES: Int = 3
        public const val ATTACK_DELAY_CYCLES: Int = 3
        public const val DRINK_MESSAGE: String = "You drink the lumpy potion."
        public const val FINISHED_MESSAGE: String = "You have finished your potion."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use this item."
    }
}
