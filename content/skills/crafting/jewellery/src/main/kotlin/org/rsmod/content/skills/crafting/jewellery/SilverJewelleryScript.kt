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

public class SilverJewelleryScript
@Inject
constructor(private val crafting: SilverJewelleryCrafting) : PluginScript() {
    override fun ScriptContext.startup() {
        for (furnace in furnace_locs.standard) {
            registerSilverBarUse(furnace, FurnaceKind.Standard)
        }
        registerSilverBarUse(furnace_locs.shilo_village, FurnaceKind.ShiloVillage)
        for (furnace in furnace_locs.lovakiteFurnaces) {
            registerSilverBarUse(furnace, FurnaceKind.Lovakite)
        }
        onPlayerQueueWithArgs<SilverCraftingRequest>(furnace_queues.silverJewellery) {
            crafting.process(this, it.args)
        }
    }

    private fun ScriptContext.registerSilverBarUse(furnace: LocType, kind: FurnaceKind) {
        onOpLocU(furnace, objs.silver_bar) { crafting.select(this, it.loc, kind) }
    }
}
