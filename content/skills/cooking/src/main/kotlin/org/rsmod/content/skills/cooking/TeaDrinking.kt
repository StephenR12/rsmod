package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal

/** Cache-233 ordinary cup-of-tea consumption. */
public class TeaDrinking @Inject constructor(private val food: FoodConsumptionRepository) {
    public fun drink(access: ProtectedAccess, slot: Int) {
        with(access) {
            if (slot !in player.inv.indices) {
                return
            }
            val selected = player.inv[slot]
            if (selected?.id != objs.cup_of_tea.id || !food.isReady(player)) {
                return
            }
            if (!player.members) {
                mes(MEMBERS_MESSAGE)
                return
            }

            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.cup_of_tea.id, count = 1, slot = slot)
                    add(inventory, objs.empty_cup.id, count = 1, slot = slot)
                }
            check(result.success) { "Cup-of-tea replacement failed after validation." }

            stopAction()
            anim(seqs.human_drink_from_teacup_clay)
            say(DRINK_SAY)
            statHeal(stats.hitpoints, constant = HEAL, percent = 0)
            statBoost(stats.attack, constant = ATTACK_CONSTANT, percent = ATTACK_PERCENT)
            food.consume(player, eatDelay = FOOD_DELAY_CYCLES, attackDelay = ATTACK_DELAY_CYCLES)
            publish(PlayerConsumableEvent.Consumed(player, objs.cup_of_tea, foodHealing = 2))
        }
    }

    public companion object {
        public const val HEAL: Int = 3
        public const val ATTACK_CONSTANT: Int = 2
        public const val ATTACK_PERCENT: Int = 2
        public const val FOOD_DELAY_CYCLES: Int = 3
        public const val ATTACK_DELAY_CYCLES: Int = 3
        public const val DRINK_SAY: String = "Aaah, nothing like a nice cuppa tea!"
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use this item."
    }
}
