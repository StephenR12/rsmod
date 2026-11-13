package org.rsmod.content.skills.slayer

import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

object SlayerShops : InvReferences() {
    val regular = find("slayershop_pre_porcine")
    val afterPorcine = find("slayershop")
}

internal object SlayerEquipment : ObjReferences() {
    val enchantedGem = find("slayer_gem")
    val mirrorShield = find("slayer_mirror_shield")
    val leafBladedSpear = find("slayer_leafbladed_spear")
    val broadArrows = find("slayer_broad_arrows")
    val bagOfSalt = find("slayer_bag_of_salt")
    val rockHammer = find("slayer_rock_hammer")
    val facemask = find("slayer_facemask")
    val earmuffs = find("slayer_earmuffs")
    val nosePeg = find("slayer_nosepeg")
    val slayerSStaff = find("slayer_staff")
    val spinyHelmet = find("wallbeast_spike_helmet")
    val fishingExplosive = find("fishing_explosive")
    val iceCooler = find("slayer_icy_water")
    val slayerGloves = find("deal_slayer_gloves")
    val unlitBugLantern = find("slayer_buglan_off")
    val insulatedBoots = find("slayer_boots")
    val fungicideSpray = find("slayer_spray_pump_10")
    val fungicide = find("slayer_fungicide")
    val witchwoodIcon = find("witchwood_icon")
    val slayerBell = find("molanisk_bell")
    val broadArrowheads = find("slayer_broad_arrowhead")
    val broadArrowheadPack = find("pack_slayer_broad_arrowhead")
    val unfinishedBroadBolts = find("slayer_broad_bolt_unfinished")
    val unfinishedBroadBoltPack = find("pack_slayer_broad_bolt_unfinished")
    val rockThrownhammer = find("slayer_rock_thrownhammer")
    val bootsOfStone = find("boots_of_stone")
    val reinforcedGoggles = find("slayer_reinforced_goggles")
}

internal object SlayerShopStock : InvEditor() {
    init {
        for (inventory in listOf(SlayerShops.regular, SlayerShops.afterPorcine)) {
            edit(inventory) {
                scope = InvScope.Shared
                stack = InvStackType.Always
                autoSize = true
                restock = true
                stock += stock(SlayerEquipment.enchantedGem, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.mirrorShield, count = 100, restockCycles = 100)
                stock += stock(SlayerEquipment.leafBladedSpear, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.broadArrows, count = 50000, restockCycles = 5)
                stock += stock(SlayerEquipment.bagOfSalt, count = 5000, restockCycles = 5)
                stock += stock(SlayerEquipment.rockHammer, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.facemask, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.earmuffs, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.nosePeg, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.slayerSStaff, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.spinyHelmet, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.fishingExplosive, count = 5000, restockCycles = 50)
                stock += stock(SlayerEquipment.iceCooler, count = 5000, restockCycles = 5)
                stock += stock(SlayerEquipment.slayerGloves, count = 50, restockCycles = 16)
                stock += stock(SlayerEquipment.unlitBugLantern, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.insulatedBoots, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.fungicideSpray, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.fungicide, count = 5000, restockCycles = 5)
                stock += stock(SlayerEquipment.witchwoodIcon, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.slayerBell, count = 50, restockCycles = 100)
                stock += stock(SlayerEquipment.broadArrowheads, count = 3000, restockCycles = 1)
                stock += stock(SlayerEquipment.broadArrowheadPack, count = 800, restockCycles = 1)
                stock +=
                    stock(SlayerEquipment.unfinishedBroadBolts, count = 5000, restockCycles = 1)
                stock +=
                    stock(SlayerEquipment.unfinishedBroadBoltPack, count = 1000, restockCycles = 1)
                stock += stock(SlayerEquipment.rockThrownhammer, count = 5000, restockCycles = 5)
                stock += stock(SlayerEquipment.bootsOfStone, count = 50, restockCycles = 1)
                if (inventory === SlayerShops.afterPorcine) {
                    stock += stock(SlayerEquipment.reinforcedGoggles, count = 50, restockCycles = 1)
                }
            }
        }
    }
}
