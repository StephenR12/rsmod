package org.rsmod.content.other.gembag

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class GemBagStorage
@Inject
constructor(private val invTypes: InvTypeList, private val objTypes: ObjTypeList) {
    public fun fill(access: ProtectedAccess): Int {
        if (!access.requireMembers()) {
            return 0
        }
        val suitable = access.inv.any { it?.id in acceptedIds }
        if (!suitable) {
            access.mes(NOTHING_SUITABLE_MESSAGE)
            return 0
        }

        var moved = 0
        for (type in accepted) {
            var remaining = remainingCapacity(access.player, type)
            if (remaining <= 0) {
                continue
            }
            val slots = access.inv.mapSlots { _, obj -> obj?.id == type.id }
            for (slot in slots) {
                if (remaining <= 0) {
                    break
                }
                val obj = access.inv[slot] ?: continue
                val count = minOf(obj.count, remaining)
                val completed =
                    access
                        .invMoveFromSlot(access.inv, storage(access.player), slot, count)
                        .completed()
                moved += completed
                remaining -= completed
            }
        }

        if (moved == 0) {
            access.mes(BAG_FULL_MESSAGE)
            return 0
        }
        access.mes(if (moved == 1) ADD_ONE_MESSAGE else ADD_MANY_MESSAGE)
        if (access.inv.any { it?.id in acceptedIds }) {
            access.mes(BAG_FULL_MESSAGE)
        }
        return moved
    }

    public fun storeHeld(access: ProtectedAccess, type: ObjType, gemSlot: Int): Int {
        if (!access.requireMembers()) {
            return 0
        }
        if (type.id !in acceptedIds) {
            access.mes(UNCUT_ONLY_MESSAGE)
            return 0
        }
        if (remainingCapacity(access.player, type) <= 0) {
            access.mes(BAG_FULL_MESSAGE)
            return 0
        }
        val moved =
            access
                .invMoveFromSlot(access.inv, storage(access.player), gemSlot, count = 1)
                .completed()
        if (moved > 0) {
            access.mes(ADD_ONE_MESSAGE)
        }
        return moved
    }

    public fun rejectCutGem(access: ProtectedAccess) {
        if (access.requireMembers()) {
            access.mes(UNCUT_ONLY_MESSAGE)
        }
    }

    public fun toggle(access: ProtectedAccess, current: ObjType, slot: Int) {
        if (!access.requireMembers()) {
            return
        }
        val counterpart =
            when (current) {
                objs.gem_bag -> objs.gem_bag_open
                objs.gem_bag_open -> objs.gem_bag
                else -> error("$current is not a gem bag variant")
            }
        access.invReplaceSlot(access.inv, slot, count = 1, counterpart)
        access.mes(if (counterpart == objs.gem_bag_open) OPEN_MESSAGE else CLOSE_MESSAGE)
    }

    public fun check(access: ProtectedAccess) {
        if (!access.requireMembers()) {
            return
        }
        access.mes(
            "Sapphires: ${count(access.player, objs.uncut_sapphire)} / " +
                "Emeralds: ${count(access.player, objs.uncut_emerald)} / " +
                "Rubies: ${count(access.player, objs.uncut_ruby)}"
        )
        access.mes(
            "Diamonds: ${count(access.player, objs.uncut_diamond)} / " +
                "Dragonstones: ${count(access.player, objs.uncut_dragonstone)}"
        )
    }

    /** Transfers in the cutoff's documented sapphire-to-dragonstone order. */
    public fun emptyToInventory(access: ProtectedAccess): Int {
        if (!access.requireMembers()) {
            return 0
        }
        val storage = storage(access.player)
        var moved = 0
        for (type in accepted) {
            while (access.inv.hasFreeSpace()) {
                val slot = storage.indexOfFirst { it?.id == type.id }
                if (slot < 0) {
                    break
                }
                val completed =
                    access.invMoveFromSlot(storage, access.inv, slot, count = 1).completed()
                if (completed == 0) {
                    return moved
                }
                moved += completed
            }
            if (access.inv.isFull()) {
                break
            }
        }
        return moved
    }

    public fun emptyToBank(player: Player): Int {
        if (!player.members) {
            return 0
        }
        val bank = player.invMap.getOrPut(invTypes[invs.bank])
        return player.invMoveAll(storage(player), bank).completed()
    }

    public fun clear(player: Player) {
        player.invClear(storage(player))
    }

    /** Shared acquisition hook for Mining, Thieving and ground-item pickup implementations. */
    public fun storeAcquired(player: Player, type: ObjType, count: Int = 1): Int {
        if (
            !player.members ||
                count <= 0 ||
                type.id !in acceptedIds ||
                !player.inv.contains(objs.gem_bag_open)
        ) {
            return 0
        }
        val amount = minOf(count, remainingCapacity(player, type))
        if (amount <= 0) {
            return 0
        }
        return player.invAdd(storage(player), type, count = amount).completed()
    }

    /** Ground Take operations redirect only when the complete ground stack fits in the bag. */
    public fun acquisitionInventory(player: Player, type: ObjType, count: Int): Inventory? {
        if (
            !player.members ||
                count <= 0 ||
                type.id !in acceptedIds ||
                !player.inv.contains(objs.gem_bag_open) ||
                count > remainingCapacity(player, type)
        ) {
            return null
        }
        return storage(player)
    }

    public fun remainingCapacity(player: Player, type: ObjType): Int =
        (PER_GEM_CAPACITY - count(player, type)).coerceAtLeast(0)

    public fun count(player: Player, type: ObjType): Int = storage(player).count(objTypes[type])

    private fun storage(player: Player): Inventory =
        player.invMap.getOrPut(invTypes[invs.gem_bag_storage])

    private fun ProtectedAccess.requireMembers(): Boolean {
        if (player.members) {
            return true
        }
        mes(MEMBERS_MESSAGE)
        return false
    }

    public companion object {
        public const val PER_GEM_CAPACITY: Int = 60
        public const val NOTHING_SUITABLE_MESSAGE: String =
            "You have nothing suitable for storing in the bag."
        public const val BAG_FULL_MESSAGE: String = "Your bag is too full to hold all your gems."
        public const val UNCUT_ONLY_MESSAGE: String = "Only uncut gems can be stored in the bag."
        public const val ADD_ONE_MESSAGE: String = "You add the gem to your bag."
        public const val ADD_MANY_MESSAGE: String = "You add the gems to your bag."
        public const val OPEN_MESSAGE: String = "You open your gem bag, ready to fill it."
        public const val CLOSE_MESSAGE: String = "You close your gem bag."
        public const val AUTO_STORE_MESSAGE: String = "You put it straight into your open gem bag."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use a gem bag."

        public val accepted: List<ObjType> =
            listOf(
                objs.uncut_sapphire,
                objs.uncut_emerald,
                objs.uncut_ruby,
                objs.uncut_diamond,
                objs.uncut_dragonstone,
            )

        public val cut: List<ObjType> =
            listOf(objs.sapphire, objs.emerald, objs.ruby, objs.diamond, objs.dragonstone)

        private val acceptedIds: Set<Int> = accepted.mapTo(HashSet()) { it.id }
    }
}
