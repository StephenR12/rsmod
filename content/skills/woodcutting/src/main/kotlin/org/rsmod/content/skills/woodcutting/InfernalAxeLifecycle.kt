package org.rsmod.content.skills.woodcutting

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

public class InfernalAxeLifecycle @Inject constructor() {
    public val ornamentStyles: List<InfernalAxeStyle> =
        listOf(
            InfernalAxeStyle(
                objs.infernal_axe,
                objs.infernal_axe_or,
                objs.infernal_axe_or_reloaded,
            ),
            InfernalAxeStyle(
                objs.infernal_axe_uncharged,
                objs.infernal_axe_or_uncharged,
                objs.infernal_axe_or_reloaded_uncharged,
            ),
        )

    public val rechargePairs: List<InfernalAxeRecharge> =
        listOf(
            InfernalAxeRecharge(objs.infernal_axe_uncharged, objs.infernal_axe),
            InfernalAxeRecharge(objs.infernal_axe_or_uncharged, objs.infernal_axe_or),
            InfernalAxeRecharge(
                objs.infernal_axe_or_reloaded_uncharged,
                objs.infernal_axe_or_reloaded,
            ),
        )

    public val ornamentPairs: List<InfernalAxeOrnament> =
        ornamentStyles.map { InfernalAxeOrnament(it.plain, it.trailblazer) }

    public suspend fun create(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val dragonSlot = selectedSlot(firstSlot, secondSlot, objs.dragon_axe.id) ?: return
            val stoneSlot = selectedSlot(firstSlot, secondSlot, objs.smouldering_stone.id) ?: return
            if (!canCreate(dragonSlot, stoneSlot, sendFailure = true)) {
                return
            }
            startDialogue {
                delay(CREATION_TICKS)
                if (!canCreate(dragonSlot, stoneSlot, sendFailure = false)) {
                    return@startDialogue
                }
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, objs.dragon_axe.id, 1, slot = dragonSlot)
                        delete(inventory, objs.smouldering_stone.id, 1, slot = stoneSlot)
                        add(inventory, objs.infernal_axe.id, 1, slot = dragonSlot)
                    }
                check(result.success) { "Infernal-axe creation failed after validation." }
                awardExperience()
            }
        }
    }

    public fun recharge(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        recharge: InfernalAxeRecharge,
        material: ObjType,
    ) {
        with(access) {
            val axeSlot = selectedSlot(firstSlot, secondSlot, recharge.depleted.id) ?: return
            val materialSlot = selectedSlot(firstSlot, secondSlot, material.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to recharge an infernal axe.")
                return
            }
            if (
                player.inv[axeSlot]?.id != recharge.depleted.id ||
                    player.inv[materialSlot]?.id != material.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recharge.depleted.id, 1, slot = axeSlot)
                    delete(inventory, material.id, 1, slot = materialSlot)
                    add(inventory, recharge.charged.id, 1, slot = axeSlot)
                }
            check(result.success) { "Infernal-axe recharge failed after validation." }
            awardExperience()
        }
    }

    public fun applyOrnament(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        ornament: InfernalAxeOrnament,
    ) {
        with(access) {
            val axeSlot = selectedSlot(firstSlot, secondSlot, ornament.plain.id) ?: return
            val kitSlot =
                selectedSlot(firstSlot, secondSlot, objs.trailblazer_tool_ornament_kit.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to ornament an infernal axe.")
                return
            }
            val axe = player.inv[axeSlot] ?: return
            if (
                axe.id != ornament.plain.id ||
                    player.inv[kitSlot]?.id != objs.trailblazer_tool_ornament_kit.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, ornament.plain.id, 1, slot = axeSlot)
                    delete(inventory, objs.trailblazer_tool_ornament_kit.id, 1, slot = kitSlot)
                    add(inventory, ornament.ornamented.id, 1, vars = axe.vars, slot = axeSlot)
                }
            check(result.success) { "Infernal-axe ornamenting failed after validation." }
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
                mes("You need to be on a members' world to revert an infernal axe.")
                return
            }
            val axe = player.inv[slot] ?: return
            if (axe.id != ornamented.id) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, ornamented.id, 1, slot = slot)
                    add(inventory, plain.id, 1, vars = axe.vars, slot = slot)
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
        style: InfernalAxeStyle,
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to change an infernal axe's style.")
                return
            }
            val axe = inventory[slot] ?: return
            val target =
                when (axe.id) {
                    style.trailblazer.id -> style.reloaded
                    style.reloaded.id -> style.trailblazer
                    else -> return
                }
            val result =
                player.invTransaction(inventory) {
                    val selected = select(inventory)
                    delete(selected, axe.id, 1, slot = slot)
                    add(selected, target.id, 1, vars = axe.vars, slot = slot)
                }
            check(result.success) { "Infernal-axe style change failed after validation." }
        }
    }

    public fun checkCharges(access: ProtectedAccess, axe: InvObj?) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to check an infernal axe.")
                return
            }
            if (axe == null || rechargePairs.none { it.charged.id == axe.id }) {
                return
            }
            val charges = InfernalAxeState.fromCharged(axe).charges
            val percentage = charges * 100 / InfernalAxeState.MAX_CHARGES
            mes("Your infernal axe is $percentage% charged.")
        }
    }

    public fun wieldOrnamented(access: ProtectedAccess, inventory: Inventory, slot: Int) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to wield an infernal axe.")
                return
            }
            val axe = inventory[slot] ?: return
            if (ornamentStyles.none { axe.id == it.trailblazer.id || axe.id == it.reloaded.id }) {
                return
            }
            val result = invEquip(slot, inventory)
            if (result is HeldEquipResult.Fail) {
                result.messages.forEach(::mes)
            }
        }
    }

    private fun ProtectedAccess.awardExperience() {
        statAdvance(stats.woodcutting, WOODCUTTING_XP)
        statAdvance(stats.firemaking, FIREMAKING_XP)
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
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to create an infernal axe.")
        }
        if (player.statBase(stats.woodcutting) < WOODCUTTING_LEVEL) {
            return reject(
                "You need a Woodcutting level of $WOODCUTTING_LEVEL to create an infernal axe."
            )
        }
        if (player.statBase(stats.firemaking) < FIREMAKING_LEVEL) {
            return reject(
                "You need a Firemaking level of $FIREMAKING_LEVEL to create an infernal axe."
            )
        }
        return player.inv[dragonSlot]?.id == objs.dragon_axe.id &&
            player.inv[stoneSlot]?.id == objs.smouldering_stone.id
    }

    public companion object {
        public const val WOODCUTTING_LEVEL: Int = 61
        public const val FIREMAKING_LEVEL: Int = 85
        public const val WOODCUTTING_XP: Double = 200.0
        public const val FIREMAKING_XP: Double = 350.0
        public const val CREATION_TICKS: Int = 3
    }
}

public data class InfernalAxeRecharge(val depleted: ObjType, val charged: ObjType)

public data class InfernalAxeOrnament(val plain: ObjType, val ornamented: ObjType)

public data class InfernalAxeStyle(
    val plain: ObjType,
    val trailblazer: ObjType,
    val reloaded: ObjType,
)
