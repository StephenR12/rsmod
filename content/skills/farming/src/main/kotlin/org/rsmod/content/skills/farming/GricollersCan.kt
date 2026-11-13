package org.rsmod.content.skills.farming

import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TitheRewardVars : VarpReferences() {
    val water = find("gricollers_can_water")
    val blessing = find("tithe_bologa_unlocked")
    val autoWeed = find("tithe_auto_weed")
}

internal object TitheRewardVarBuilder : VarpBuilder() {
    init {
        build("gricollers_can_water") { transmitNever = true }
        build("tithe_bologa_unlocked") { transmitNever = true }
        build("tithe_auto_weed") { transmitNever = true }
    }
}

public class GricollersCan : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld4(objs.gricollers_can) {
            mes("Your watering can holds ${vars[TitheRewardVars.water]} of 1,000 doses of water.")
        }
    }
}

internal fun ProtectedAccess.hasWater(slot: Int): Boolean {
    val can = inv[slot] ?: return false
    return if (can.id == objs.gricollers_can.id) vars[TitheRewardVars.water] > 0
    else FarmingObjs.wateringCans.drop(1).any { it.id == can.id }
}

internal fun ProtectedAccess.useWater(slot: Int): Boolean {
    val can = inv[slot] ?: return false
    if (can.id == objs.gricollers_can.id) {
        if (vars[TitheRewardVars.water] <= 0) return false
        vars[TitheRewardVars.water]--
        return true
    }
    val dose = FarmingObjs.wateringCans.indexOfFirst { it.id == can.id }
    return dose > 0 && invReplaceSlot(inv, slot, 1, FarmingObjs.wateringCans[dose - 1]).success
}
