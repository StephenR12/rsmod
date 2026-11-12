package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.synths
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType

public class BloodEssenceLifecycle @Inject constructor() {
    public fun activate(access: ProtectedAccess, slot: Int) {
        val inactive = access.inv[slot]
        if (!inactive.isType(RunecraftObjs.bloodEssenceInactive)) {
            return
        }
        if (!access.player.members) {
            access.mes("You need to be on a members' world to use this item.")
            return
        }
        if (access.hasActiveBloodEssence()) {
            access.mes(ONLY_ONE_ACTIVE_MESSAGE)
            return
        }

        val transaction =
            access.player.invTransaction(access.inv) {
                val inventory = select(access.inv)
                delete(inventory, RunecraftObjs.bloodEssenceInactive.id, 1, slot = slot)
                add(
                    inventory,
                    RunecraftObjs.bloodEssenceActive.id,
                    1,
                    vars = BloodEssenceState(BloodEssenceState.MAX_CHARGES).pack(),
                )
            }
        if (transaction.failure) {
            access.mes(constants.dm_invspace)
            return
        }
        access.soundSynth(synths.enchant_ruby_ring)
    }

    public fun check(access: ProtectedAccess, essence: InvObj?) {
        if (!essence.isType(RunecraftObjs.bloodEssenceActive)) {
            return
        }
        access.mes(
            "Your blood essence has ${BloodEssenceState.fromActive(essence).charges} charges remaining."
        )
    }

    public fun planBonus(
        inventory: Inventory,
        eligibleEssence: Int,
        random: GameRandom,
    ): BloodEssenceBonusPlan? {
        if (eligibleEssence <= 0) {
            return null
        }
        val slot =
            inventory.indices.firstOrNull { inventory[it].isType(RunecraftObjs.bloodEssenceActive) }
                ?: return null
        val active = inventory.getValue(slot)
        val charges = BloodEssenceState.fromActive(active).charges
        var bonus = 0
        repeat(eligibleEssence) {
            if (bonus < charges && random.of(2) == 0) {
                bonus++
            }
        }
        return BloodEssenceBonusPlan(slot, bonus, charges - bonus)
    }

    private fun ProtectedAccess.hasActiveBloodEssence(): Boolean =
        player.inv.hasType(RunecraftObjs.bloodEssenceActive) ||
            bank.hasType(RunecraftObjs.bloodEssenceActive)

    private fun Inventory.hasType(type: org.rsmod.game.type.obj.ObjType): Boolean = any {
        it.isType(type)
    }

    public companion object {
        public const val ONLY_ONE_ACTIVE_MESSAGE: String =
            "You can only have one active blood essence at a time."
    }
}

public data class BloodEssenceBonusPlan(
    val slot: Int,
    val bonusRunes: Int,
    val remainingCharges: Int,
)
