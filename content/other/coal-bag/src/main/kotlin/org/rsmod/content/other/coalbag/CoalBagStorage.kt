package org.rsmod.content.other.coalbag

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

/** Permanent coal-bag state and operations for the two cache-233 object variants. */
public class CoalBagStorage
@Inject
constructor(private val invTypes: InvTypeList, private val objTypes: ObjTypeList) {
    public fun fillFromInventory(access: ProtectedAccess): Int {
        if (!access.requireMembers()) {
            return 0
        }
        val remaining = remainingCapacity(access.player)
        if (remaining <= 0) {
            access.mes(FULL_MESSAGE)
            return 0
        }
        val available = access.inv.count(objTypes[objs.coal])
        if (available <= 0) {
            access.mes(NO_COAL_MESSAGE)
            return 0
        }
        val requested = minOf(available, remaining)
        val moved = access.invDel(access.inv, objs.coal, count = requested).completed()
        if (moved > 0) {
            setCount(access.player, count(access.player) + moved)
        }
        return moved
    }

    public fun toggle(access: ProtectedAccess, current: ObjType, slot: Int) {
        if (!access.requireMembers()) {
            return
        }
        val counterpart =
            when (current) {
                objs.coal_bag -> objs.coal_bag_open
                objs.coal_bag_open -> objs.coal_bag
                else -> error("$current is not a coal bag variant")
            }
        access.invReplaceSlot(access.inv, slot, count = 1, counterpart)
    }

    public fun check(access: ProtectedAccess) {
        if (!access.requireMembers()) {
            return
        }
        val count = count(access.player)
        when {
            count == 0 -> access.mes(EMPTY_MESSAGE)
            count == 1 -> access.mes("Your coal bag contains 1 piece of coal.")
            else -> access.mes("Your coal bag contains $count pieces of coal.")
        }
    }

    public fun emptyToInventory(access: ProtectedAccess): Int {
        if (!access.requireMembers()) {
            return 0
        }
        val stored = count(access.player)
        if (stored <= 0) {
            access.mes(EMPTY_MESSAGE)
            return 0
        }
        val moved = access.invAdd(access.inv, objs.coal, count = stored, strict = false).completed()
        if (moved > 0) {
            setCount(access.player, stored - moved)
        }
        return moved
    }

    /** Bank op 9 is client-flipped: empty bags fill from bank and non-empty bags empty to bank. */
    public fun bankside(player: Player): Int {
        if (!player.members) {
            return 0
        }
        val stored = count(player)
        val bank = player.invMap.getOrPut(invTypes[invs.bank])
        if (stored > 0) {
            val moved = player.invAdd(bank, objs.coal, count = stored, strict = false).completed()
            if (moved > 0) {
                setCount(player, stored - moved)
            }
            return moved
        }

        val available = bank.count(objTypes[objs.coal])
        val requested = minOf(available, remainingCapacity(player))
        if (requested <= 0) {
            return 0
        }
        val moved =
            player
                .invDel(bank, objs.coal, count = requested, placehold = true, strict = true)
                .completed()
        if (moved > 0) {
            setCount(player, moved)
        }
        return moved
    }

    public suspend fun destroy(access: ProtectedAccess, type: ObjType, slot: Int): Boolean {
        if (!access.requireMembers()) {
            return false
        }
        if (count(access.player) > 0) {
            access.mes(NONEMPTY_DESTROY_MESSAGE)
            return false
        }
        val obj = objTypes[type]
        val confirmed =
            access.confirmDestroy(
                type,
                count = 1,
                header = obj.param(params.destroy_note_title),
                text = obj.param(params.destroy_note_desc),
            )
        if (!confirmed) {
            return false
        }
        return access.invDel(access.inv, type, count = 1, slot = slot).completed() == 1
    }

    /** Redirects coal produced by Mining only while the open bag has effective capacity. */
    public fun storeMinedCoal(player: Player, type: ObjType, amount: Int = 1): Int {
        if (
            !player.members ||
                type.id != objs.coal.id ||
                amount <= 0 ||
                !player.inv.contains(objs.coal_bag_open)
        ) {
            return 0
        }
        val moved = minOf(amount, remainingCapacity(player))
        if (moved > 0) {
            setCount(player, count(player) + moved)
        }
        return moved
    }

    public fun canStoreMinedCoal(player: Player, type: ObjType): Boolean =
        player.members &&
            type.id == objs.coal.id &&
            player.inv.contains(objs.coal_bag_open) &&
            remainingCapacity(player) > 0

    public fun capacity(player: Player): Int =
        if (
            player.back?.id in
                setOf(
                    objs.smithing_skillcape.id,
                    objs.smithing_skillcape_t.id,
                    objs.max_cape_worn.id,
                )
        ) {
            SMITHING_CAPE_CAPACITY
        } else {
            BASE_CAPACITY
        }

    public fun remainingCapacity(player: Player): Int =
        (capacity(player) - count(player)).coerceAtLeast(0)

    public fun count(player: Player): Int = player.coalBagCount

    /** Clears contents when the bag itself is irreversibly removed, including by alchemy. */
    public fun clear(player: Player) {
        setCount(player, 0)
    }

    private fun setCount(player: Player, value: Int) {
        player.coalBagCount = value.coerceIn(0, SMITHING_CAPE_CAPACITY)
    }

    private fun ProtectedAccess.requireMembers(): Boolean {
        if (player.members) {
            return true
        }
        mes(MEMBERS_MESSAGE)
        return false
    }

    public companion object {
        public const val BASE_CAPACITY: Int = 27
        public const val SMITHING_CAPE_CAPACITY: Int = 36
        public const val EMPTY_MESSAGE: String = "Your coal bag is empty."
        public const val FULL_MESSAGE: String = "Your coal bag is already full."
        public const val NO_COAL_MESSAGE: String = "You don't have any coal to put in the bag."
        public const val NONEMPTY_DESTROY_MESSAGE: String =
            "You need to empty the coal bag before you can destroy it."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use a coal bag."

        public val variants: List<ObjType> = listOf(objs.coal_bag, objs.coal_bag_open)
    }
}

private var Player.coalBagCount by intVarBit(varbits.coal_bag_storage_count)
