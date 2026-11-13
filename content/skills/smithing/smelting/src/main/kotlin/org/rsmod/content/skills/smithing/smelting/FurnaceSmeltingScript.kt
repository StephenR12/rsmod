package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FurnaceSmeltingScript
@Inject
constructor(private val smelting: FurnaceSmelting, private val cannonballs: CannonballSmithing) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (furnace in furnace_locs.standard) {
            val kind =
                if (furnace.id == furnace_locs.edgeville.id) FurnaceKind.Edgeville
                else FurnaceKind.Standard
            registerOp2(furnace, kind)
        }
        registerOp2(furnace_locs.shilo_village, FurnaceKind.ShiloVillage)
        registerOp1(furnace_locs.lovakite_large, FurnaceKind.Lovakite)
        registerOp2(furnace_locs.lovakite, FurnaceKind.Lovakite)

        onPlayerQueueWithArgs<FurnaceSmeltingRequest>(furnace_queues.smelting) {
            smelting.process(this, it.args)
        }
        onPlayerQueueWithArgs<CannonballSmithingRequest>(furnace_queues.cannonballs) {
            cannonballs.process(this, it.args)
        }
        onOpHeld2(objs.ring_of_forging) { smelting.breakRing(this, it.slot) }
        onOpWorn2(objs.ring_of_forging) { smelting.checkRing(this) }
    }

    private fun ScriptContext.registerOp1(furnace: LocType, kind: FurnaceKind) {
        onOpLoc1(furnace) { smelting.select(this, it.loc, kind) }
        registerOreUse(furnace, kind)
    }

    private fun ScriptContext.registerOp2(furnace: LocType, kind: FurnaceKind) {
        onOpLoc2(furnace) { smelting.select(this, it.loc, kind) }
        registerOreUse(furnace, kind)
    }

    private fun ScriptContext.registerOreUse(furnace: LocType, kind: FurnaceKind) {
        for (ore in ORES) {
            onOpLocU(furnace, ore) { smelting.select(this, it.loc, kind, ore) }
        }
        for (obj in CANNONBALL_INPUTS) {
            onOpLocU(furnace, obj) { cannonballs.select(this, it.loc, kind, obj) }
        }
    }

    private companion object {
        val ORES: List<ObjType> =
            listOf(
                objs.copper_ore,
                objs.tin_ore,
                objs.iron_ore,
                objs.silver_ore,
                objs.gold_ore,
                objs.lovakite_ore,
                objs.blurite_ore,
                objs.lunar_ore,
                objs.mithril_ore,
                objs.adamantite_ore,
                objs.runite_ore,
            )
        val CANNONBALL_INPUTS: List<ObjType> =
            listOf(objs.steel_bar, objs.ammo_mould, objs.double_ammo_mould)
    }
}
