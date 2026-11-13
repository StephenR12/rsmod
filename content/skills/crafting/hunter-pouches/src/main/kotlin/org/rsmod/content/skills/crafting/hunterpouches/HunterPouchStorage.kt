package org.rsmod.content.skills.crafting.hunterpouches

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.zone.ZoneKey

public class HunterPouchStorage
@Inject
constructor(
    private val invTypes: InvTypeList,
    private val registry: HunterPouchStorageRegistry,
    private val locs: LocRepository,
    private val npcs: NpcRepository,
    private val locTypes: LocTypeList,
) {
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
        val c = access.coords
        // Native world-map tzhaar_area source sections cover both outer and inner city.
        val tzhaar =
            c.x in 2368..2559 && c.z in 5056..5183 || c.x in 2240..2303 && c.z in 5312..5375
        val guild = c.level == 2 && c.x in 1550..1572 && c.z in 3040..3068
        if (
            !access.player.members ||
                descriptor.family != HunterPouchFamily.Meat ||
                tzhaar ||
                (!guild && !nearBank(access.player))
        ) {
            access.mes("You can't empty your ${descriptor.family.singular} pouch here.")
            return 0
        }
        return access.player
            .invMoveAll(storage(access.player, descriptor.family), access.inv)
            .completed()
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

    /**
     * Plans normal catch delivery and open-pouch routing together, then commits all inventories
     * atomically. A full ordinary inventory must never partially fill a pouch or duplicate a catch.
     */
    public fun deliverCaught(player: Player, loot: List<InvObj>): Boolean {
        data class Delivery(val inventory: Inventory, val item: InvObj)
        val remaining = mutableMapOf<HunterPouchFamily, Int>()
        val deliveries = buildList {
            for (item in loot) {
                if (item.count <= 0) continue
                val descriptor =
                    registry.descriptors
                        .filter { d ->
                            d.family.accepted.any { it.id == item.id } && d.open in player.inv
                        }
                        .maxByOrNull { it.capacity }
                val amount =
                    if (descriptor == null) 0
                    else
                        minOf(
                            item.count,
                            remaining.getOrPut(descriptor.family) {
                                (descriptor.capacity -
                                        storage(player, descriptor.family).totalCount())
                                    .coerceAtLeast(0)
                            },
                        )
                if (descriptor != null && amount > 0) {
                    add(Delivery(storage(player, descriptor.family), item.copy(count = amount)))
                    remaining[descriptor.family] = remaining.getValue(descriptor.family) - amount
                }
                if (amount < item.count)
                    add(Delivery(player.inv, item.copy(count = item.count - amount)))
            }
        }
        return player
            .invTransaction(player.inv) {
                val targets =
                    deliveries.map { it.inventory }.distinct().associateWith { select(it) }
                for (delivery in deliveries) add(
                    targets.getValue(delivery.inventory),
                    delivery.item.id,
                    delivery.item.count,
                    vars = delivery.item.vars,
                )
            }
            .success
    }

    private fun nearBank(player: Player): Boolean {
        if (interfaces.bank_main in player.ui) return true
        val c = player.coords
        for (dx in -4..4) for (dz in -4..4) for (loc in locs.findAll(c.translate(dx, dz))) {
            val type = locTypes.getValue(loc.id)
            if (
                type.op.any { it.equals("Bank", true) } ||
                    type.name.contains("bank chest", true) && type.op.any { it.equals("Use", true) }
            )
                return true
        }
        return npcs.findAll(ZoneKey.from(c), 1).any {
            it.coords.chebyshevDistance(c) <= 4 && it.type.op.any { op -> op.equals("Bank", true) }
        }
    }

    public fun count(player: Player, family: HunterPouchFamily): Int =
        storage(player, family).totalCount()

    private fun storage(player: Player, family: HunterPouchFamily): Inventory =
        player.invMap.getOrPut(invTypes[family.storage])

    private fun Inventory.totalCount(): Int = sumOf { it?.count ?: 0 }
}
