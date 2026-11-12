package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

public class GnomishFirelighter @Inject constructor() {
    public fun charge(
        access: ProtectedAccess,
        firelighterSlot: Int,
        holderSlot: Int,
        colour: ColouredLogRecipe,
    ) {
        with(access) {
            val firelighters = player.inv[firelighterSlot]
            val holder = player.inv[holderSlot]
            if (firelighters?.id != colour.firelighter.id || holder == null) {
                return
            }
            if (
                holder.id != objs.gnomish_firelighter.id &&
                    holder.id != objs.gnomish_firelighter_charged.id
            ) {
                return
            }
            if (!player.members) {
                mes("You need to be on a members' world to use this item.")
                return
            }

            val current = GnomishFirelighterState.from(holder)
            if (holder.id == objs.gnomish_firelighter_charged.id && current == null) {
                return
            }
            if (current != null && current.colour != colour) {
                mes("You must uncharge the gnomish firelighter before changing its colour.")
                return
            }
            val currentCharges = current?.charges ?: 0
            val added =
                min(firelighters.count, GnomishFirelighterState.MAX_CHARGES - currentCharges)
            if (added == 0) {
                mes("Your gnomish firelighter cannot hold any more charges.")
                return
            }
            val updated = GnomishFirelighterState(colour, currentCharges + added)
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, holder.id, count = 1, slot = holderSlot)
                    delete(inventory, colour.firelighter.id, count = added, slot = firelighterSlot)
                    add(
                        inventory,
                        objs.gnomish_firelighter_charged.id,
                        count = 1,
                        vars = updated.toInvObj().vars,
                        slot = holderSlot,
                    )
                }
            if (transaction.success) {
                val noun = if (added == 1) "firelighter" else "firelighters"
                mes("You add $added ${colour.colour} $noun to the gnomish firelighter.")
            }
        }
    }

    public fun check(access: ProtectedAccess, slot: Int) {
        with(access) {
            val obj = player.inv[slot] ?: return
            if (obj.id == objs.gnomish_firelighter.id) {
                mes("The gnomish firelighter is empty.")
                return
            }
            val state = GnomishFirelighterState.from(obj) ?: return
            val noun = if (state.charges == 1) "firelighter" else "firelighters"
            mes(
                "Your gnomish firelighter contains ${state.charges} " +
                    "${state.colour.colour} $noun."
            )
        }
    }

    public fun uncharge(access: ProtectedAccess, slot: Int) {
        with(access) {
            val state = GnomishFirelighterState.from(player.inv[slot]) ?: return
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.gnomish_firelighter_charged.id, count = 1, slot = slot)
                    add(inventory, objs.gnomish_firelighter.id, count = 1, slot = slot)
                    add(inventory, state.colour.firelighter.id, count = state.charges)
                }
            if (transaction.failure) {
                mes("You don't have enough inventory space to uncharge the gnomish firelighter.")
                return
            }
            mes("You remove the firelighters from the gnomish firelighter.")
        }
    }

    public fun findChargedSlot(inventory: Inventory): Int =
        inventory.indexOfFirst { GnomishFirelighterState.from(it) != null }

    public fun state(inventory: Inventory, slot: Int): GnomishFirelighterState? =
        if (slot in inventory.indices) {
            GnomishFirelighterState.from(inventory[slot])
        } else {
            null
        }

    public fun consume(
        inventory: Inventory,
        slot: Int,
        expected: GnomishFirelighterState,
    ): Boolean {
        val current = state(inventory, slot) ?: return false
        if (current.colour != expected.colour || current.charges != expected.charges) {
            return false
        }
        inventory[slot] =
            if (current.charges == 1) {
                InvObj(objs.gnomish_firelighter)
            } else {
                current.copy(charges = current.charges - 1).toInvObj()
            }
        return true
    }
}
