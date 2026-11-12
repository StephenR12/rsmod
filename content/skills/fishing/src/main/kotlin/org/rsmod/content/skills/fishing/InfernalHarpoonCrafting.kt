package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType

public class InfernalHarpoonCrafting @Inject constructor() {
    public val ornamentStyles: List<InfernalHarpoonStyle> =
        listOf(
            InfernalHarpoonStyle(
                objs.infernal_harpoon,
                objs.infernal_harpoon_or,
                objs.infernal_harpoon_or_reloaded,
            ),
            InfernalHarpoonStyle(
                objs.infernal_harpoon_uncharged,
                objs.infernal_harpoon_or_uncharged,
                objs.infernal_harpoon_or_reloaded_uncharged,
            ),
        )

    public val rechargePairs: List<InfernalHarpoonRecharge> =
        listOf(
            InfernalHarpoonRecharge(objs.infernal_harpoon_uncharged, objs.infernal_harpoon),
            InfernalHarpoonRecharge(objs.infernal_harpoon_or_uncharged, objs.infernal_harpoon_or),
            InfernalHarpoonRecharge(
                objs.infernal_harpoon_or_reloaded_uncharged,
                objs.infernal_harpoon_or_reloaded,
            ),
        )

    public val ornamentPairs: List<InfernalHarpoonOrnament> =
        ornamentStyles.map { InfernalHarpoonOrnament(it.plain, it.trailblazer) }

    public fun create(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val dragonSlot = selectedSlot(firstSlot, secondSlot, objs.dragon_harpoon.id) ?: return
            val stoneSlot = selectedSlot(firstSlot, secondSlot, objs.smouldering_stone.id) ?: return
            if (!canCreate(dragonSlot, stoneSlot, sendFailure = true)) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.dragon_harpoon.id, 1, slot = dragonSlot)
                    delete(inventory, objs.smouldering_stone.id, 1, slot = stoneSlot)
                    add(inventory, objs.infernal_harpoon.id, 1, slot = dragonSlot)
                }
            check(result.success) { "Infernal-harpoon creation failed after validation." }
            statAdvance(stats.fishing, FISHING_XP)
            statAdvance(stats.cooking, COOKING_XP)
        }
    }

    public fun recharge(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        recharge: InfernalHarpoonRecharge,
        material: ObjType,
    ) {
        with(access) {
            val harpoonSlot = selectedSlot(firstSlot, secondSlot, recharge.depleted.id) ?: return
            val materialSlot = selectedSlot(firstSlot, secondSlot, material.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to recharge an infernal harpoon.")
                return
            }
            if (
                player.inv[harpoonSlot]?.id != recharge.depleted.id ||
                    player.inv[materialSlot]?.id != material.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recharge.depleted.id, 1, slot = harpoonSlot)
                    delete(inventory, material.id, 1, slot = materialSlot)
                    add(inventory, recharge.charged.id, 1, slot = harpoonSlot)
                }
            check(result.success) { "Infernal-harpoon recharge failed after validation." }
        }
    }

    public fun applyOrnament(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        ornament: InfernalHarpoonOrnament,
    ) {
        with(access) {
            val harpoonSlot = selectedSlot(firstSlot, secondSlot, ornament.plain.id) ?: return
            val kitSlot =
                selectedSlot(firstSlot, secondSlot, objs.trailblazer_tool_ornament_kit.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to ornament an infernal harpoon.")
                return
            }
            val harpoon = player.inv[harpoonSlot] ?: return
            if (
                harpoon.id != ornament.plain.id ||
                    player.inv[kitSlot]?.id != objs.trailblazer_tool_ornament_kit.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, ornament.plain.id, 1, slot = harpoonSlot)
                    delete(inventory, objs.trailblazer_tool_ornament_kit.id, 1, slot = kitSlot)
                    add(
                        inventory,
                        ornament.ornamented.id,
                        1,
                        vars = harpoon.vars,
                        slot = harpoonSlot,
                    )
                }
            check(result.success) { "Infernal-harpoon ornamenting failed after validation." }
        }
    }

    public fun revertOrnament(
        access: ProtectedAccess,
        slot: Int,
        ornamented: ObjType,
        plain: ObjType,
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to revert an infernal harpoon.")
                return
            }
            val harpoon = player.inv[slot] ?: return
            if (harpoon.id != ornamented.id) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, ornamented.id, 1, slot = slot)
                    add(inventory, plain.id, 1, vars = harpoon.vars, slot = slot)
                    add(inventory, objs.trailblazer_tool_ornament_kit.id, 1)
                }
            if (!result.success) {
                mes("You do not have enough inventory space to remove the ornament kit.")
            }
        }
    }

    public fun changeStyle(
        access: ProtectedAccess,
        inventory: Inventory,
        slot: Int,
        style: InfernalHarpoonStyle,
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to change an infernal harpoon's style.")
                return
            }
            val harpoon = inventory[slot] ?: return
            val target =
                when (harpoon.id) {
                    style.trailblazer.id -> style.reloaded
                    style.reloaded.id -> style.trailblazer
                    else -> return
                }
            val result =
                player.invTransaction(inventory) {
                    val selected = select(inventory)
                    delete(selected, harpoon.id, 1, slot = slot)
                    add(selected, target.id, 1, vars = harpoon.vars, slot = slot)
                }
            check(result.success) { "Infernal-harpoon style change failed after validation." }
        }
    }

    public fun checkCharges(access: ProtectedAccess, harpoon: InvObj?) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to check an infernal harpoon.")
                return
            }
            if (harpoon == null || rechargePairs.none { it.charged.id == harpoon.id }) {
                return
            }
            val charges = InfernalHarpoonState.fromCharged(harpoon).charges
            mes("The infernal harpoon has $charges charges remaining.")
        }
    }

    public fun wieldOrnamented(access: ProtectedAccess, inventory: Inventory, slot: Int) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to wield an infernal harpoon.")
                return
            }
            val harpoon = inventory[slot] ?: return
            if (
                ornamentStyles.none {
                    harpoon.id == it.trailblazer.id || harpoon.id == it.reloaded.id
                }
            ) {
                return
            }
            val result = invEquip(slot, inventory)
            if (result is HeldEquipResult.Fail) {
                result.messages.forEach(::mes)
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

    private fun ProtectedAccess.canCreate(
        dragonSlot: Int,
        stoneSlot: Int,
        sendFailure: Boolean,
    ): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to create an infernal harpoon.")
            }
            return false
        }
        if (player.statBase(stats.fishing) < FISHING_LEVEL) {
            if (sendFailure) {
                mes("You need a Fishing level of $FISHING_LEVEL to create an infernal harpoon.")
            }
            return false
        }
        if (player.statBase(stats.cooking) < COOKING_LEVEL) {
            if (sendFailure) {
                mes("You need a Cooking level of $COOKING_LEVEL to create an infernal harpoon.")
            }
            return false
        }
        return player.inv[dragonSlot]?.id == objs.dragon_harpoon.id &&
            player.inv[stoneSlot]?.id == objs.smouldering_stone.id
    }

    public companion object {
        public const val FISHING_LEVEL: Int = 75
        public const val COOKING_LEVEL: Int = 85
        public const val FISHING_XP: Double = 200.0
        public const val COOKING_XP: Double = 350.0
    }
}

public data class InfernalHarpoonRecharge(val depleted: ObjType, val charged: ObjType)

public data class InfernalHarpoonOrnament(val plain: ObjType, val ornamented: ObjType)

public data class InfernalHarpoonStyle(
    val plain: ObjType,
    val trailblazer: ObjType,
    val reloaded: ObjType,
)
