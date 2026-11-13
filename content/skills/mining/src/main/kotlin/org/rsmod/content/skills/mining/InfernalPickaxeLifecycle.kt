package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.worn.HeldEquipResult
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.mining.configs.MiningObjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType

public class InfernalPickaxeLifecycle @Inject constructor(private val xpMods: XpModifiers) {
    public val ornamentStyles: List<InfernalPickaxeStyle> =
        listOf(
            InfernalPickaxeStyle(
                MiningObjs.infernalPickaxe,
                MiningObjs.infernalPickaxeTrailblazer,
                MiningObjs.infernalPickaxeTrailblazerReloaded,
            ),
            InfernalPickaxeStyle(
                MiningObjs.infernalPickaxeUncharged,
                MiningObjs.infernalPickaxeTrailblazerUncharged,
                MiningObjs.infernalPickaxeTrailblazerReloadedUncharged,
            ),
        )

    public val rechargePairs: List<InfernalPickaxeRecharge> =
        listOf(
            InfernalPickaxeRecharge(
                MiningObjs.infernalPickaxeUncharged,
                MiningObjs.infernalPickaxe,
            ),
            InfernalPickaxeRecharge(
                MiningObjs.infernalPickaxeTrailblazerUncharged,
                MiningObjs.infernalPickaxeTrailblazer,
            ),
            InfernalPickaxeRecharge(
                MiningObjs.infernalPickaxeTrailblazerReloadedUncharged,
                MiningObjs.infernalPickaxeTrailblazerReloaded,
            ),
        )

    public fun checkCharges(access: ProtectedAccess, pickaxe: InvObj?) =
        with(access) {
            if (
                !player.members ||
                    pickaxe == null ||
                    rechargePairs.none { it.charged.id == pickaxe.id }
            )
                return
            val remaining = InfernalPickaxeState.fromCharged(pickaxe).charges
            mes(
                "The infernal pickaxe has ${remaining * 100 / InfernalPickaxeState.MAX_CHARGES}% of its charges remaining."
            )
        }

    public fun recharge(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        recharge: InfernalPickaxeRecharge,
        material: ObjType,
    ): Unit =
        with(access) {
            if (
                !player.members ||
                    material !in listOf(objs.smouldering_stone, MiningObjs.dragonPickaxe)
            )
                return
            val pickSlot = selectedSlot(firstSlot, secondSlot, recharge.depleted.id) ?: return
            val materialSlot = selectedSlot(firstSlot, secondSlot, material.id) ?: return
            if (pickSlot == materialSlot) return
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete(inv, recharge.depleted.id, 1, slot = pickSlot)
                    delete(inv, material.id, 1, slot = materialSlot)
                    add(inv, recharge.charged.id, 1, slot = pickSlot)
                }
            if (result.success) {
                statAdvance(stats.mining, MINING_XP)
                statAdvance(stats.smithing, SMITHING_XP)
                mes("You recharge the infernal pickaxe.")
            }
        }

    /** One-third ore destruction; ordinary production must call this before delivering each ore. */
    public fun combust(
        access: ProtectedAccess,
        tool: InvObj,
        product: ObjType,
        requiredLevel: Int,
    ): Boolean =
        with(access) {
            if (!player.members) return false
            val depleted =
                rechargePairs.firstOrNull { it.charged.id == tool.id }?.depleted ?: return false
            val worn = player.righthand == tool
            val slot = if (worn) -1 else player.inv.indexOfFirst { it == tool }
            if (!worn && slot < 0) return false
            // No procs at gem rocks, amethyst, dense essence, Motherlode or other separate
            // activities.
            if (product.id in excludedProducts) return false
            if (!random.randomBoolean(3)) return false
            val saved = objs.ghommals_lucky_penny in player.worn && random.randomBoolean(20)
            if (!saved) {
                val updated = InfernalPickaxeState.fromCharged(tool).consume(tool, depleted)
                if (worn) player.righthand = updated else player.inv[slot] = updated
                if (updated.id == depleted.id) mes("Your infernal pickaxe has run out of charges.")
            }
            statAdvance(
                stats.smithing,
                smithingXp(product, requiredLevel) * xpMods.get(player, stats.smithing),
            )
            mes("Your infernal pickaxe consumes the resource.")
            true
        }

    private val excludedProducts: Set<Int> by lazy {
        setOf(
            objs.uncut_opal.id,
            objs.uncut_jade.id,
            objs.uncut_red_topaz.id,
            objs.uncut_sapphire.id,
            objs.uncut_emerald.id,
            objs.uncut_ruby.id,
            objs.uncut_diamond.id,
            MiningObjs.runeEssence.id,
            MiningObjs.pureEssence.id,
            MiningObjs.denseEssenceBlock.id,
            21347, // Amethyst.
        )
    }

    /** Dated Infernal pickaxe table; unspecified non-smeltable XP is explicitly modelled. */
    public fun smithingXp(product: ObjType, requiredLevel: Int): Double =
        when (product.id) {
            objs.copper_ore.id,
            objs.tin_ore.id -> 3.6
            objs.blurite_ore.id -> 4.0
            objs.iron_ore.id -> 6.25
            objs.silver_ore.id -> 7.8
            objs.gold_ore.id -> 9.0
            objs.mithril_ore.id -> 12.0
            objs.adamantite_ore.id -> 18.75
            objs.runite_ore.id -> 25.0
            QuarryObjs.granite[0].id -> 7.2
            QuarryObjs.granite[1].id -> 13.2
            QuarryObjs.granite[2].id -> 22.2
            QuarryObjs.sandstone[0].id -> 6.0
            QuarryObjs.sandstone[1].id -> 9.0
            QuarryObjs.sandstone[2].id -> 15.0
            QuarryObjs.sandstone[3].id -> 21.0
            // Published prose only specifies level-based XP for other nonsmeltable rocks.
            else -> requiredLevel * 0.2
        }

    public val ornamentPairs: List<InfernalPickaxeOrnament> =
        ornamentStyles.map { InfernalPickaxeOrnament(it.plain, it.trailblazer) }

    public val cosmeticDragonPickaxes: List<ObjType> =
        listOf(
            MiningObjs.dragonPickaxeUpgraded,
            MiningObjs.dragonPickaxeZalcano,
            MiningObjs.dragonPickaxeTrailblazer,
            MiningObjs.dragonPickaxeTrailblazerReloaded,
        )

    public suspend fun create(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val dragonSlot =
                selectedSlot(firstSlot, secondSlot, MiningObjs.dragonPickaxe.id) ?: return
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
                        delete(inventory, MiningObjs.dragonPickaxe.id, 1, slot = dragonSlot)
                        delete(inventory, objs.smouldering_stone.id, 1, slot = stoneSlot)
                        add(inventory, MiningObjs.infernalPickaxe.id, 1, slot = dragonSlot)
                    }
                check(result.success) { "Infernal-pickaxe creation failed after validation." }
                statAdvance(stats.mining, MINING_XP)
                statAdvance(stats.smithing, SMITHING_XP)
            }
        }
    }

    public fun rejectCosmeticConversion(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val hasStone = selectedSlot(firstSlot, secondSlot, objs.smouldering_stone.id) != null
            val hasCosmetic =
                cosmeticDragonPickaxes.any { selectedSlot(firstSlot, secondSlot, it.id) != null }
            if (hasStone && hasCosmetic) {
                mes(COSMETIC_CONVERSION_REJECTION)
            }
        }
    }

    public fun applyOrnament(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        ornament: InfernalPickaxeOrnament,
    ) {
        with(access) {
            val pickaxeSlot = selectedSlot(firstSlot, secondSlot, ornament.plain.id) ?: return
            val kitSlot =
                selectedSlot(firstSlot, secondSlot, objs.trailblazer_tool_ornament_kit.id) ?: return
            if (!player.members) {
                mes("You need to be on a members' world to ornament an infernal pickaxe.")
                return
            }
            val pickaxe = player.inv[pickaxeSlot] ?: return
            if (
                pickaxe.id != ornament.plain.id ||
                    player.inv[kitSlot]?.id != objs.trailblazer_tool_ornament_kit.id
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, ornament.plain.id, 1, slot = pickaxeSlot)
                    delete(inventory, objs.trailblazer_tool_ornament_kit.id, 1, slot = kitSlot)
                    add(
                        inventory,
                        ornament.ornamented.id,
                        1,
                        vars = pickaxe.vars,
                        slot = pickaxeSlot,
                    )
                }
            check(result.success) { "Infernal-pickaxe ornamenting failed after validation." }
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
                mes("You need to be on a members' world to revert an infernal pickaxe.")
                return
            }
            val pickaxe = player.inv[slot] ?: return
            if (pickaxe.id != ornamented.id) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, ornamented.id, 1, slot = slot)
                    add(inventory, plain.id, 1, vars = pickaxe.vars, slot = slot)
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
        style: InfernalPickaxeStyle,
    ) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to change an infernal pickaxe's style.")
                return
            }
            val pickaxe = inventory[slot] ?: return
            val target =
                when (pickaxe.id) {
                    style.trailblazer.id -> style.reloaded
                    style.reloaded.id -> style.trailblazer
                    else -> return
                }
            val result =
                player.invTransaction(inventory) {
                    val selected = select(inventory)
                    delete(selected, pickaxe.id, 1, slot = slot)
                    add(selected, target.id, 1, vars = pickaxe.vars, slot = slot)
                }
            check(result.success) { "Infernal-pickaxe style change failed after validation." }
        }
    }

    public fun wieldOrnamented(access: ProtectedAccess, inventory: Inventory, slot: Int) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to wield an infernal pickaxe.")
                return
            }
            val pickaxe = inventory[slot] ?: return
            if (
                ornamentStyles.none {
                    pickaxe.id == it.trailblazer.id || pickaxe.id == it.reloaded.id
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
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to create an infernal pickaxe.")
        }
        if (player.statBase(stats.mining) < MINING_LEVEL) {
            return reject("You need a Mining level of $MINING_LEVEL to create an infernal pickaxe.")
        }
        if (player.statBase(stats.smithing) < SMITHING_LEVEL) {
            return reject(
                "You need a Smithing level of $SMITHING_LEVEL to create an infernal pickaxe."
            )
        }
        return player.inv[dragonSlot]?.id == MiningObjs.dragonPickaxe.id &&
            player.inv[stoneSlot]?.id == objs.smouldering_stone.id
    }

    public companion object {
        public const val MINING_LEVEL: Int = 61
        public const val SMITHING_LEVEL: Int = 85
        public const val MINING_XP: Double = 200.0
        public const val SMITHING_XP: Double = 350.0
        public const val CREATION_TICKS: Int = 3
        public const val COSMETIC_CONVERSION_REJECTION: String =
            "The cosmetically upgraded pickaxe is too beautiful to convert into an infernal pickaxe."
    }
}

public data class InfernalPickaxeOrnament(val plain: ObjType, val ornamented: ObjType)

public data class InfernalPickaxeStyle(
    val plain: ObjType,
    val trailblazer: ObjType,
    val reloaded: ObjType,
)

public data class InfernalPickaxeRecharge(val depleted: ObjType, val charged: ObjType)
