package org.rsmod.content.skills.fishing

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

public class FishBarrelStorage
@Inject
constructor(private val invTypes: InvTypeList, private val objTypes: ObjTypeList) {
    public fun fill(access: ProtectedAccess): Int {
        if (!access.player.members) {
            access.mes("You need to be on a members' world to use a fish barrel.")
            return 0
        }
        val storage = storage(access.player)
        var remaining = CAPACITY - storage.totalCount()
        if (remaining <= 0) {
            access.mes(BARREL_FULL_MESSAGE)
            return 0
        }

        var moved = 0
        val slots = access.inv.mapSlots { _, obj -> obj?.id in acceptedIds }
        for (slot in slots) {
            if (remaining <= 0) {
                break
            }
            val obj = access.inv[slot] ?: continue
            val count = minOf(obj.count, remaining)
            val completed = access.invMoveFromSlot(access.inv, storage, slot, count).completed()
            moved += completed
            remaining -= completed
        }
        if (moved == 0) {
            access.mes("You don't have any suitable fish to put in the barrel.")
        } else if (remaining == 0) {
            access.mes(BARREL_FULL_MESSAGE)
        }
        return moved
    }

    public suspend fun check(access: ProtectedAccess): Int {
        if (!access.player.members) {
            access.mes("You need to be on a members' world to use a fish barrel.")
            return 0
        }
        val storage = storage(access.player)
        val count = storage.totalCount()
        if (count == 0) {
            access.mesbox("The barrel is empty.")
            return 0
        }
        val contents =
            storage
                .filterNotNull()
                .groupingBy { it.id }
                .fold(0) { total, obj -> total + obj.count }
                .entries
                .joinToString(", ") { (id, amount) ->
                    "$amount x ${objTypes.types.getValue(id).name}"
                }
        access.mesbox("The barrel contains: $contents")
        return count
    }

    public fun toggle(access: ProtectedAccess, current: ObjType, slot: Int) {
        toggle(access, current, access.inv, slot)
    }

    public fun toggleWorn(access: ProtectedAccess, current: ObjType, slot: Int) {
        toggle(access, current, access.player.worn, slot)
    }

    private fun toggle(access: ProtectedAccess, current: ObjType, inventory: Inventory, slot: Int) {
        if (!access.player.members) {
            access.mes("You need to be on a members' world to use a fish barrel.")
            return
        }
        val counterpart =
            when (current) {
                objs.fish_barrel_closed -> objs.fish_barrel_open
                objs.fish_barrel_open -> objs.fish_barrel_closed
                objs.fish_sack_barrel_closed -> objs.fish_sack_barrel_open
                objs.fish_sack_barrel_open -> objs.fish_sack_barrel_closed
                else -> error("$current is not a fish barrel variant")
            }
        access.invReplaceSlot(inventory, slot, count = 1, counterpart)
    }

    public fun emptyToBank(player: Player): Int {
        val storage = storage(player)
        val bank = player.invMap.getOrPut(invTypes[invs.bank])
        return player.invMoveAll(storage, bank).completed()
    }

    public fun clear(player: Player) {
        player.invClear(storage(player))
    }

    public fun canStoreCaught(player: Player, type: ObjType): Boolean =
        type.id in acceptedIds && hasOpenCapacity(player)

    public fun hasOpenCapacity(player: Player): Boolean =
        player.members &&
            (player.inv.contains(objs.fish_barrel_open) ||
                player.inv.contains(objs.fish_sack_barrel_open) ||
                player.worn.contains(objs.fish_sack_barrel_open)) &&
            remainingCapacity(player) > 0

    public fun storeCaught(player: Player, type: ObjType, count: Int): Int {
        if (!canStoreCaught(player, type) || count <= 0) {
            return 0
        }
        val amount = minOf(count, remainingCapacity(player))
        return player.invAdd(storage(player), type, amount).completed()
    }

    public fun remainingCapacity(player: Player): Int =
        (CAPACITY - storage(player).totalCount()).coerceAtLeast(0)

    public fun count(player: Player): Int = storage(player).totalCount()

    public fun count(player: Player, type: ObjType): Int = storage(player).count(objTypes[type])

    private fun storage(player: Player): Inventory =
        player.invMap.getOrPut(invTypes[invs.fish_barrel_storage])

    private fun Inventory.totalCount(): Int = sumOf { it?.count ?: 0 }

    public companion object {
        public const val CAPACITY: Int = 28
        public const val BARREL_FULL_MESSAGE: String =
            "The barrel is full. It may be emptied at a bank."

        public val accepted: Set<ObjType> =
            linkedSetOf(
                objs.raw_shrimps,
                objs.raw_sardine,
                objs.raw_herring,
                objs.raw_anchovies,
                objs.raw_mackerel,
                objs.raw_trout,
                objs.raw_cod,
                objs.raw_pike,
                objs.raw_slimy_eel,
                objs.raw_salmon,
                objs.raw_tuna,
                objs.raw_rainbow_fish,
                objs.raw_cave_eel,
                objs.raw_lobster,
                objs.raw_bass,
                objs.leaping_trout,
                objs.raw_swordfish,
                objs.raw_lava_eel,
                objs.leaping_salmon,
                objs.raw_monkfish,
                objs.raw_karambwan,
                objs.leaping_sturgeon,
                objs.raw_shark,
                objs.infernal_eel,
                objs.raw_anglerfish,
                objs.raw_dark_crab,
                objs.sacred_eel,
                objs.raw_bluegill,
                objs.raw_common_tench,
                objs.raw_mottled_eel,
                objs.raw_greater_siren,
                objs.raw_sea_turtle,
                objs.raw_manta_ray,
            )

        private val acceptedIds: Set<Int> = accepted.mapTo(HashSet()) { it.id }
    }
}
