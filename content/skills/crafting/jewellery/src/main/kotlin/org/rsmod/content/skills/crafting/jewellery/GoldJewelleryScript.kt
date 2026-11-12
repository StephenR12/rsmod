package org.rsmod.content.skills.crafting.jewellery

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.content.skills.smithing.smelting.FurnaceKind
import org.rsmod.content.skills.smithing.smelting.furnace_locs
import org.rsmod.content.skills.smithing.smelting.furnace_queues
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class GoldJewelleryScript @Inject constructor(private val crafting: GoldJewelleryCrafting) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (furnace in furnace_locs.standard) {
            registerGoldBarUse(furnace, FurnaceKind.Standard)
        }
        registerGoldBarUse(furnace_locs.shilo_village, FurnaceKind.ShiloVillage)
        for (furnace in furnace_locs.lovakiteFurnaces) {
            registerGoldBarUse(furnace, FurnaceKind.Lovakite)
        }
        onPlayerQueueWithArgs<GoldJewelleryRequest>(furnace_queues.goldJewellery) {
            crafting.process(this, it.args)
        }
    }

    private fun ScriptContext.registerGoldBarUse(furnace: LocType, kind: FurnaceKind) {
        onOpLocU(furnace, objs.gold_bar) { crafting.select(this, it.loc, kind) }
    }
}
