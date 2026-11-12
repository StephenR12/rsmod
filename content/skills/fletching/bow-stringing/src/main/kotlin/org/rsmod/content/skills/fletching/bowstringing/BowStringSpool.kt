package org.rsmod.content.skills.fletching.bowstringing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class BowStringSpool @Inject constructor(private val objTypes: ObjTypeList) {
    public fun fill(access: ProtectedAccess): Int = deposit(access, objs.bow_string)

    public fun deposit(access: ProtectedAccess, type: ObjType): Int {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use the bow string spool.")
                return 0
            }
            val remaining = capacity(this) - charges
            val amount = minOf(inv.count(objTypes[type]), remaining)
            if (amount <= 0) {
                mes(
                    if (remaining <= 0) {
                        "Your bow string spool is full."
                    } else {
                        "You don't have any bow strings to add to the spool."
                    }
                )
                return 0
            }
            val result =
                player.invTransaction(player.inv) { delete(select(player.inv), type.id, amount) }
            if (!result.success) {
                return 0
            }
            charges += amount
            return amount
        }
    }

    public fun combine(access: ProtectedAccess): Boolean {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use the bow string spool.")
                return false
            }
            if (size >= MAX_SIZE) {
                mes("Your bow string spool is already at its maximum capacity.")
                return false
            }
            if (inv.count(objTypes[objs.bow_string_spool]) < 2) {
                return false
            }
            val result =
                player.invTransaction(player.inv) {
                    delete(select(player.inv), objs.bow_string_spool.id, 1)
                }
            if (!result.success) {
                return false
            }
            size++
            return true
        }
    }

    public fun check(access: ProtectedAccess): Int {
        val count = charges(access)
        access.mes(
            "Your bow string spool contains $count bow strings and can hold ${capacity(access)}."
        )
        return count
    }

    public fun canSupply(access: ProtectedAccess): Boolean =
        access.inv.contains(objs.bow_string_spool) && charges(access) > 0

    public fun consume(access: ProtectedAccess): Boolean {
        if (!canSupply(access)) {
            return false
        }
        access.charges--
        return true
    }

    /** Stores bow strings produced by spinning without first placing them in the inventory. */
    public fun storeProduced(access: ProtectedAccess, count: Int): Int {
        require(count >= 0) { "Produced bow-string count must not be negative." }
        if (count == 0 || !access.inv.contains(objs.bow_string_spool)) {
            return 0
        }
        val stored = minOf(count, capacity(access) - access.charges)
        if (stored > 0) {
            access.charges += stored
        }
        return stored
    }

    /** Returns how many newly produced strings can be accepted without mutating spool state. */
    public fun remainingCapacity(access: ProtectedAccess): Int {
        if (!access.inv.contains(objs.bow_string_spool)) {
            return 0
        }
        return capacity(access) - access.charges
    }

    public fun charges(access: ProtectedAccess): Int = access.charges

    public fun size(access: ProtectedAccess): Int = access.size

    public fun capacity(access: ProtectedAccess): Int =
        CAPACITIES[access.size.coerceIn(0, MAX_SIZE)]

    private var ProtectedAccess.charges by intVarBit(varbits.bow_string_spool_charges)

    private var ProtectedAccess.size by intVarBit(varbits.bow_string_spool_size)

    private companion object {
        const val MAX_SIZE = 5
        val CAPACITIES = intArrayOf(250, 500, 2_000, 5_000, 10_000, 100_000)
    }
}
