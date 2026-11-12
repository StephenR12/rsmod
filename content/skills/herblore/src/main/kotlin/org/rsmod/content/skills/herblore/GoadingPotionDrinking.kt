package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType

public data class GoadingPotionDose(
    public val potion: ObjType,
    public val replacement: ObjType?,
    public val dosesRemaining: Int,
)

public object GoadingPotionDoses {
    public val all: List<GoadingPotionDose> =
        listOf(
            GoadingPotionDose(objs.goading_potion_4, objs.goading_potion_3, 3),
            GoadingPotionDose(objs.goading_potion_3, objs.goading_potion_2, 2),
            GoadingPotionDose(objs.goading_potion_2, objs.goading_potion_1, 1),
            GoadingPotionDose(objs.goading_potion_1, objs.empty_vial, 0),
        )
}

/** Cache-233 direct consumption of Goading potion doses. */
public class GoadingPotionDrinking
@Inject
constructor(private val potions: PotionConsumptionRepository) {
    public fun drink(access: ProtectedAccess, slot: Int, dose: GoadingPotionDose) {
        with(access) {
            if (slot !in player.inv.indices) {
                return
            }
            val selected = player.inv[slot]
            if (selected?.id != dose.potion.id || !potions.isReady(player)) {
                return
            }
            if (!player.members) {
                mes(MEMBERS_MESSAGE)
                return
            }

            val replacement =
                if (dose.dosesRemaining == 0 && player.vars[varbits.auto_smash_vials] != 0) {
                    null
                } else {
                    dose.replacement
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, dose.potion.id, count = 1, slot = slot)
                    replacement?.let { add(inventory, it.id, count = 1, slot = slot) }
                }
            check(result.success) { "Goading-potion replacement failed after validation." }

            stopAction()
            anim(seqs.human_eat)
            mes(DRINK_MESSAGE)
            if (dose.dosesRemaining == 0) {
                mes(FINISHED_MESSAGE)
            } else {
                val doseWord = if (dose.dosesRemaining == 1) "dose" else "doses"
                mes("You have ${dose.dosesRemaining} $doseWord of potion left.")
            }
            potions.consume(player, POTION_DELAY_CYCLES)
            publish(PlayerConsumableEvent.Consumed(player, dose.potion))
        }
    }

    public companion object {
        public const val POTION_DELAY_CYCLES: Int = 3
        public const val DRINK_MESSAGE: String = "You drink some of your goading potion."
        public const val FINISHED_MESSAGE: String = "You have finished your potion."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use this item."
    }
}
