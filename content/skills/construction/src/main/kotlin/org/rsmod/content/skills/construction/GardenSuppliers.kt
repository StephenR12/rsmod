package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GardenStoreInvs : InvReferences() {
    val centres = listOf(find("poh_garden_centre"), find("kebos_poh_garden_centre"))
    val enchantments = find("fairy_fixit_enchantments")
}

internal object GardenStoreNpcs : NpcReferences() {
    val gardeners = listOf(find("poh_garden_supplier"), find("kebos_poh_garden_supplier"))
    val fixitLocked = find("fairy2_repair_1op")
    val fixit = find("fairy2_repair_2ops")
}

internal object GardenGoods : ObjReferences() {
    val enchantment = find("poh_fairy_enchantment")
    val plants =
        (1..7).map { find("poh_sapling_tree_$it") } +
            (1..3).map { find("poh_sapling_plant_$it") } +
            (1..7).map { find("poh_sapling_hedge$it") } +
            (1..3).map { find("poh_sapling_flowerb$it") } +
            (1..3).map { find("poh_sapling_flowera$it") }
}

internal object GardenCentreStock : InvEditor() {
    init {
        for (centre in GardenStoreInvs.centres) edit(centre) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            for (item in GardenGoods.plants) stock += stock(item, count = 20, restockCycles = 100)
        }
    }
}

internal object FairyFixitStock : InvBuilder() {
    init {
        build("fairy_fixit_enchantments") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(GardenGoods.enchantment, count = 4, restockCycles = 1)
        }
    }
}

public class GardenSuppliers @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((index, gardener) in GardenStoreNpcs.gardeners.withIndex()) onOpNpc3(gardener) {
            if (player.members)
                shops.open(
                    player,
                    "Garden Centre",
                    GardenStoreInvs.centres[index],
                    40.0,
                    100.0,
                    3.0,
                )
        }
        // The cache transforms Fairy Fixit on fairy2_rewardgiven after Fairytale II.
        onOpNpc1(GardenStoreNpcs.fixitLocked) {
            mes("Fairy Fixit is busy tending to the fairy ring.")
        }
        onOpNpc3(GardenStoreNpcs.fixit) {
            if (player.members)
                shops.open(
                    player,
                    "Fairy Fixit's Fairy Enchantment",
                    GardenStoreInvs.enchantments,
                    60.0,
                    100.0,
                    1.0,
                )
        }
    }
}
