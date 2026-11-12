package org.rsmod.content.skills.crafting.battlestaves

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.inv.isType

public class BryophytasStaffCrafting @Inject constructor() {
    public fun craft(access: ProtectedAccess, battlestaffSlot: Int, essenceSlot: Int) {
        with(access) {
            val battlestaff = player.inv[battlestaffSlot]
            val essence = player.inv[essenceSlot]
            if (!battlestaff.isType(objs.battlestaff) || !essence.isType(objs.bryophytas_essence)) {
                return
            }
            if (!player.members) {
                mes("You need to be on a members' world to make this staff.")
                return
            }
            if (player.stat(stats.crafting) < CRAFTING_LEVEL) {
                mes("You need a Crafting level of $CRAFTING_LEVEL to make this item.")
                return
            }

            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.battlestaff.id, 1, slot = battlestaffSlot)
                    delete(inventory, objs.bryophytas_essence.id, 1, slot = essenceSlot)
                    add(inventory, objs.bryophytas_staff_uncharged.id, 1, slot = battlestaffSlot)
                }
            check(result.success) {
                "Bryophyta's staff transaction failed after validating both selected inputs."
            }
        }
    }

    public companion object {
        public const val CRAFTING_LEVEL: Int = 62
    }
}
