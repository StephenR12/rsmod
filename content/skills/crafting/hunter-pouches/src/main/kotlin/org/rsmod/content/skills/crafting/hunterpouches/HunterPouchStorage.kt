package org.rsmod.content.skills.crafting.hunterpouches

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType

public class HunterPouchStorage
@Inject
constructor(private val invTypes: InvTypeList, private val registry: HunterPouchStorageRegistry) {
    public fun fill(access: ProtectedAccess, descriptor: HunterPouchDescriptor): Int {
        val storage = storage(access.player, descriptor.family)
        var remaining = descriptor.capacity - storage.totalCount()
        if (remaining <= 0) {
            access.mes("Your pouch is full.")
            return 0
        }

        var moved = 0
        val accepted = descriptor.family.accepted.mapTo(HashSet()) { it.id }
        val slots = access.inv.mapSlots { _, obj -> obj != null && obj.id in accepted }
        for (slot in slots) {
            if (remaining <= 0) {
                break
            }
            val obj = access.inv[slot] ?: continue
            val count = minOf(obj.count, remaining)
            val transaction = access.invMoveFromSlot(access.inv, storage, slot, count)
            val completed = transaction.completed()
            moved += completed
            remaining -= completed
        }
        if (moved == 0) {
            access.mes(
                "You don't have any suitable ${descriptor.family.plural} to put in the pouch."
            )
        }
        return moved
    }

    public fun emptyToInventory(access: ProtectedAccess, descriptor: HunterPouchDescriptor): Int {
        access.mes("You can't empty your ${descriptor.family.singular} pouch here.")
        return 0
    }

    public fun emptyToBank(player: Player, descriptor: HunterPouchDescriptor): Int {
        val storage = storage(player, descriptor.family)
        val bank = player.invMap.getOrPut(invTypes[invs.bank])
        return player.invMoveAll(storage, bank).completed()
    }

    public fun check(access: ProtectedAccess, descriptor: HunterPouchDescriptor): Int {
        val count = count(access.player, descriptor.family)
        access.mes(
            "Your ${descriptor.family.singular} pouch contains $count " +
                if (count == 1) descriptor.family.singular else descriptor.family.plural
        )
        return count
    }

    public fun toggle(
        access: ProtectedAccess,
        descriptor: HunterPouchDescriptor,
        current: ObjType,
        slot: Int,
    ) {
        access.invReplaceSlot(access.inv, slot, count = 1, descriptor.counterpart(current))
    }

    public fun clear(player: Player, family: HunterPouchFamily) {
        player.invClear(storage(player, family))
    }

    /**
     * Entry point for Hunter catch scripts. A newly acquired resource is routed into an open pouch
     * when possible; callers should place the returned remainder in the normal inventory.
     */
    public fun storeAcquired(player: Player, type: ObjType, count: Int = 1): Int {
        val descriptor =
            registry.descriptors
                .asSequence()
                .filter { type in it.family.accepted && player.inv.contains(it.open) }
                .maxByOrNull { it.capacity } ?: return 0
        val storage = storage(player, descriptor.family)
        val amount = minOf(count, descriptor.capacity - storage.totalCount())
        if (amount <= 0) {
            return 0
        }
        return player.invAdd(storage, type, count = amount).completed()
    }

    public fun count(player: Player, family: HunterPouchFamily): Int =
        storage(player, family).totalCount()

    private fun storage(player: Player, family: HunterPouchFamily): Inventory =
        player.invMap.getOrPut(invTypes[family.storage])

    private fun Inventory.totalCount(): Int = sumOf { it?.count ?: 0 }
}
