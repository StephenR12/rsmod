package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class CrystalPickaxeLifecycle @Inject constructor() {
    public suspend fun charge(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val toolSlot =
                selectedSlot(firstSlot, secondSlot, MiningObjs.crystalPickaxe.id)
                    ?: selectedSlot(firstSlot, secondSlot, MiningObjs.crystalPickaxeInactive.id)
                    ?: return
            val shardSlot = selectedSlot(firstSlot, secondSlot, objs.crystal_shard.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to charge a crystal pickaxe.")
                return
            }
            val tool = player.inv[toolSlot] ?: return
            val shards = player.inv[shardSlot] ?: return
            val current =
                if (tool.id == MiningObjs.crystalPickaxe.id) {
                    CrystalPickaxeState.fromCharged(tool).charges
                } else {
                    0
                }
            if (current >= CrystalPickaxeState.MAX_CHARGES) {
                mes("The crystal pickaxe is already fully charged.")
                return
            }
            val capacity = CrystalPickaxeState.MAX_CHARGES - current
            val capacityInShards = (capacity + CHARGES_PER_SHARD - 1) / CHARGES_PER_SHARD
            val maximum = minOf(capacityInShards, shards.count)
            val requested =
                minOf(
                    countDialog("How many crystal shards would you like to add? (Up to $maximum)"),
                    maximum,
                )
            if (requested == 0 || player.inv[toolSlot] != tool || player.inv[shardSlot] != shards) {
                return
            }
            val updatedCharges =
                minOf(CrystalPickaxeState.MAX_CHARGES, current + requested * CHARGES_PER_SHARD)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, tool.id, 1, slot = toolSlot)
                    delete(inventory, objs.crystal_shard.id, requested, slot = shardSlot)
                    add(
                        inventory,
                        MiningObjs.crystalPickaxe.id,
                        1,
                        vars = CrystalPickaxeState(updatedCharges).pack(),
                        slot = toolSlot,
                    )
                }
            check(result.success) { "Crystal-axe charging failed after validation." }
        }
    }

    public fun checkCharges(access: ProtectedAccess, axe: InvObj?) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to check a crystal pickaxe.")
                return
            }
            if (axe?.id != MiningObjs.crystalPickaxe.id) {
                return
            }
            val charges = CrystalPickaxeState.fromCharged(axe).charges
            mes("The crystal pickaxe has $charges charges remaining.")
        }
    }

    public fun revert(access: ProtectedAccess, slot: Int, expected: ObjType) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to revert a crystal pickaxe.")
                return
            }
            if (player.inv[slot]?.id != expected.id) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, expected.id, 1, slot = slot)
                    add(inventory, MiningObjs.dragonPickaxe.id, 1, slot = slot)
                    add(inventory, objs.crystal_tool_seed.id, 1)
                }
            if (!result.success) {
                mes("You do not have enough inventory space to revert the crystal pickaxe.")
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

    /**
     * Consumes one successful-resource charge from the actual selected worn/carried pickaxe.
     * Returns false for another tool or a tool which is no longer owned. Call only after delivery;
     * dense essence, Motherlode rockfalls and the other period exclusions must not call this.
     */
    public fun consumeCharge(access: ProtectedAccess, tool: InvObj): Boolean =
        with(access) {
            if (tool.id != MiningObjs.crystalPickaxe.id) return false
            val worn = player.righthand == tool
            val slot = if (worn) -1 else inv.indexOfFirst { it == tool }
            if (!worn && slot < 0) return false
            val signet =
                listOf(objs.elven_signet, objs.celestial_signet, objs.celestial_signet_charged)
                    .any(player.worn::contains)
            val penny = objs.ghommals_lucky_penny in player.worn
            // Both published savings roll independently even when the first one succeeds.
            val signetSaved = signet && random.randomBoolean(10)
            val pennySaved = penny && random.randomBoolean(20)
            if (signetSaved || pennySaved) return true
            val replacement =
                CrystalPickaxeState.fromCharged(tool)
                    .consume(tool, MiningObjs.crystalPickaxeInactive)
            if (worn) player.righthand = replacement else inv[slot] = replacement
            if (replacement.id == MiningObjs.crystalPickaxeInactive.id)
                mes("Your crystal pickaxe has run out of charges.")
            true
        }

    public companion object {
        public const val CHARGES_PER_SHARD: Int = 100
    }
}
