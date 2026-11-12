package org.rsmod.content.skills.firemaking

import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.game.inv.InvObj

public class HandheldLightSources {
    public fun light(
        access: ProtectedAccess,
        sourceSlot: Int,
        recipe: LightSourceRecipe,
        ignitionSlot: Int,
        ignitionId: Int,
    ) {
        with(access) {
            if (
                player.inv[sourceSlot]?.id != recipe.unlit.id ||
                    player.inv[ignitionSlot]?.id != ignitionId
            ) {
                return
            }
            if (ignitionId != objs.tinderbox.id) {
                return
            }
            if (!checkRequirements(recipe.level, "light this")) {
                return
            }
            replaceInventory(sourceSlot, recipe.unlit.id, recipe.lit.id)
        }
    }

    public fun rubDrySticks(access: ProtectedAccess, sticksSlot: Int) {
        with(access) {
            if (player.inv[sticksSlot]?.id != objs.dry_sticks.id) {
                return
            }
            val torchSlot = player.inv.indexOfFirst { it?.id == objs.unlit_torch.id }
            if (torchSlot < 0 || !checkRequirements(DRY_STICKS_LEVEL, "light this")) {
                return
            }
            mes("You rub together the dry sticks and the sticks catch alight.")
            replaceInventory(torchSlot, objs.unlit_torch.id, objs.lit_torch.id)
            mes("You place the smouldering twigs to your torch.")
            mes("Your torch lights.")
        }
    }

    public fun extinguish(access: ProtectedAccess, slot: Int, recipe: LightSourceRecipe) {
        with(access) {
            if (player.inv[slot]?.id != recipe.lit.id) {
                return
            }
            replaceInventory(slot, recipe.lit.id, recipe.unlit.id)
            if (recipe.lit == objs.lit_sapphire_lantern) {
                mes("You extinguish the lantern.")
            }
        }
    }

    public fun extinguishWorn(access: ProtectedAccess, slot: Int, recipe: LightSourceRecipe) {
        with(access) {
            if (!recipe.wearable) {
                return
            }
            val current = player.worn[slot] ?: return
            if (current.id != recipe.lit.id) {
                return
            }
            player.worn[slot] = InvObj(recipe.unlit, current.count, current.vars)
        }
    }

    public fun assembleCandleLantern(
        access: ProtectedAccess,
        lanternSlot: Int,
        candleSlot: Int,
        recipe: CandleLanternRecipe,
    ) {
        with(access) {
            if (
                player.inv[lanternSlot]?.id != objs.empty_candle_lantern.id ||
                    player.inv[candleSlot]?.id != recipe.candle.id ||
                    !checkRequirements(recipe.level, "make this")
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.empty_candle_lantern.id, 1, slot = lanternSlot)
                    delete(inventory, recipe.candle.id, 1, slot = candleSlot)
                    add(inventory, recipe.output.id, 1, slot = lanternSlot)
                }
            check(result.success) { "Candle-lantern transaction failed: $recipe" }
        }
    }

    private fun ProtectedAccess.checkRequirements(level: Int, action: String): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to use this item.")
            return false
        }
        if (player.firemakingLvl < level) {
            mes("You need a Firemaking level of at least $level to $action.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.replaceInventory(slot: Int, from: Int, into: Int) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, from, 1, slot = slot)
                add(inventory, into, 1, slot = slot)
            }
        check(result.success) { "Light-source replacement failed: from=$from, into=$into" }
    }

    public companion object {
        public const val DRY_STICKS_LEVEL: Int = 30
    }
}
