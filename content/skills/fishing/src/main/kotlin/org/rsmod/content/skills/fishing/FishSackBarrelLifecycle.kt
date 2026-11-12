package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjType

public class FishSackBarrelLifecycle @Inject constructor() {
    public fun assemble(access: ProtectedAccess, firstSlot: Int, secondSlot: Int, barrel: ObjType) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to create a fish sack barrel.")
                return
            }
            val barrelSlot = selectedSlot(firstSlot, secondSlot, barrel.id) ?: return
            val sackSlot = selectedSlot(firstSlot, secondSlot, objs.fish_sack.id) ?: return
            val output =
                when (barrel) {
                    objs.fish_barrel_closed -> objs.fish_sack_barrel_closed
                    objs.fish_barrel_open -> objs.fish_sack_barrel_open
                    else -> error("$barrel is not a fish barrel variant")
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, barrel.id, 1, slot = barrelSlot)
                    delete(inventory, objs.fish_sack.id, 1, slot = sackSlot)
                    add(inventory, output.id, 1, slot = barrelSlot)
                }
            check(result.success) { "Fish-sack-barrel assembly failed after validation." }
        }
    }

    public suspend fun configureDismantle(access: ProtectedAccess, slot: Int, sackBarrel: ObjType) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to configure a fish sack barrel.")
                return
            }
            val barrel = player.inv[slot] ?: return
            if (barrel.id != sackBarrel.id) {
                return
            }
            val confirmed = choice2("Dismantle.", true, "Cancel.", false)
            if (!confirmed || player.inv[slot] != barrel) {
                return
            }
            val output =
                when (sackBarrel) {
                    objs.fish_sack_barrel_closed -> objs.fish_barrel_closed
                    objs.fish_sack_barrel_open -> objs.fish_barrel_open
                    else -> error("$sackBarrel is not a fish sack barrel variant")
                }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, sackBarrel.id, 1, slot = slot)
                    add(inventory, output.id, 1, slot = slot)
                    add(inventory, objs.fish_sack.id, 1)
                }
            if (!result.success) {
                mes("You do not have enough inventory space to dismantle the fish sack barrel.")
            }
        }
    }

    private fun ProtectedAccess.selectedSlot(
        firstSlot: Int,
        secondSlot: Int,
        expectedId: Int,
    ): Int? =
        when {
            player.inv[firstSlot]?.id == expectedId -> firstSlot
            player.inv[secondSlot]?.id == expectedId -> secondSlot
            else -> null
        }
}
