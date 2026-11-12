package org.rsmod.content.skills.woodcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

public class CrystalAxeLifecycle @Inject constructor() {
    public suspend fun configureCharges(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to configure a crystal axe.")
                return
            }
            val requested = countDialog("How many charges should be added on withdrawal?")
            player.crystalToolAutochargeQuantity =
                requested.coerceIn(0, CrystalAxeState.MAX_CHARGES)
        }
    }

    public suspend fun charge(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val toolSlot =
                selectedSlot(firstSlot, secondSlot, objs.crystal_axe.id)
                    ?: selectedSlot(firstSlot, secondSlot, objs.crystal_axe_inactive.id)
                    ?: return
            val shardSlot = selectedSlot(firstSlot, secondSlot, objs.crystal_shard.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to charge a crystal axe.")
                return
            }
            val tool = player.inv[toolSlot] ?: return
            val shards = player.inv[shardSlot] ?: return
            val current =
                if (tool.id == objs.crystal_axe.id) {
                    CrystalAxeState.fromCharged(tool).charges
                } else {
                    0
                }
            if (current >= CrystalAxeState.MAX_CHARGES) {
                mes("The crystal axe is already fully charged.")
                return
            }
            val capacity = CrystalAxeState.MAX_CHARGES - current
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
                minOf(CrystalAxeState.MAX_CHARGES, current + requested * CHARGES_PER_SHARD)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, tool.id, 1, slot = toolSlot)
                    delete(inventory, objs.crystal_shard.id, requested, slot = shardSlot)
                    add(
                        inventory,
                        objs.crystal_axe.id,
                        1,
                        vars = CrystalAxeState(updatedCharges).pack(),
                        slot = toolSlot,
                    )
                }
            check(result.success) { "Crystal-axe charging failed after validation." }
        }
    }

    public fun checkCharges(access: ProtectedAccess, axe: InvObj?) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to check a crystal axe.")
                return
            }
            if (axe?.id != objs.crystal_axe.id) {
                return
            }
            val charges = CrystalAxeState.fromCharged(axe).charges
            mes("The crystal axe has $charges charges remaining.")
        }
    }

    public fun revert(access: ProtectedAccess, slot: Int, expected: ObjType) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to revert a crystal axe.")
                return
            }
            if (player.inv[slot]?.id != expected.id) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, expected.id, 1, slot = slot)
                    add(inventory, objs.dragon_axe.id, 1, slot = slot)
                    add(inventory, objs.crystal_tool_seed.id, 1)
                }
            if (!result.success) {
                mes("You do not have enough inventory space to revert the crystal axe.")
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

    public companion object {
        public const val CHARGES_PER_SHARD: Int = 100
    }
}

private var org.rsmod.game.entity.Player.crystalToolAutochargeQuantity by
    intVarBit(org.rsmod.api.config.refs.varbits.charges_crystal_tools_quantity)
