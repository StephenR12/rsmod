package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FishingScript
@Inject
constructor(
    private val fishing: StandardFishing,
    private val karambwanjiFishing: KarambwanjiFishing,
    private val karambwanFishing: KarambwanFishing,
    private val infernalHarpoonCrafting: InfernalHarpoonCrafting,
    private val crystalHarpoonLifecycle: CrystalHarpoonLifecycle,
    private val leapingFishGutting: LeapingFishGutting,
    private val specialEelProcessing: SpecialEelProcessing,
    private val fishBarrel: FishBarrelStorage,
    private val fishSackBarrel: FishSackBarrelLifecycle,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (barrel in listOf(objs.fish_barrel_closed, objs.fish_barrel_open)) {
            onOpHeld1(barrel) { fishBarrel.fill(this) }
            onOpHeld2(barrel) { fishBarrel.check(this) }
            onOpHeld3(barrel) { fishBarrel.toggle(this, barrel, it.slot) }
            onOpBankside(barrel) { fishBarrel.emptyToBank(player) }
        }
        for (barrel in listOf(objs.fish_sack_barrel_closed, objs.fish_sack_barrel_open)) {
            onOpHeld1(barrel) { fishBarrel.fill(this) }
            onOpHeld3(barrel) { fishSackBarrel.configureDismantle(this, it.slot, barrel) }
            onOpHeld4(barrel) { fishBarrel.toggle(this, barrel, it.slot) }
            onOpWorn2(barrel) { fishBarrel.check(this) }
            onOpWorn3(barrel) { fishBarrel.toggleWorn(this, barrel, it.slot) }
            onOpBankside(barrel) { fishBarrel.emptyToBank(player) }
        }
        for (barrel in listOf(objs.fish_barrel_closed, objs.fish_barrel_open)) {
            onOpHeldU(objs.fish_sack, barrel) {
                fishSackBarrel.assemble(this, it.firstSlot, it.secondSlot, barrel)
            }
        }
        onDestroyHeld {
            if (
                type.id == objs.fish_barrel_closed.id ||
                    type.id == objs.fish_barrel_open.id ||
                    type.id == objs.fish_sack_barrel_closed.id ||
                    type.id == objs.fish_sack_barrel_open.id
            ) {
                fishBarrel.clear(player)
            }
        }
        for (recipe in LeapingFishGuttingRecipes.all) {
            onOpHeldU(objs.knife, recipe.input) {
                leapingFishGutting.start(this, it.firstSlot, it.secondSlot)
            }
        }
        onOpHeldU(objs.karambwan_vessel, objs.raw_karambwanji) {
            karambwanFishing.loadVessel(this, it.firstSlot, it.secondSlot)
        }
        onPlayerQueueWithArgs<LeapingFishGuttingRequest>(fishing_queues.leapingFishGutting) {
            leapingFishGutting.process(this, it.args)
        }
        for (hammer in listOf(objs.hammer, objs.imcando_hammer, objs.imcando_hammer_offhand)) {
            onOpHeldU(hammer, objs.infernal_eel) {
                specialEelProcessing.smashInfernal(this, it.firstSlot, it.secondSlot, hammer)
            }
        }
        onOpHeldU(objs.knife, objs.sacred_eel) {
            specialEelProcessing.dissectSacred(this, it.firstSlot, it.secondSlot)
        }
        onPlayerQueueWithArgs<SacredEelDissectionRequest>(fishing_queues.sacredEelDissection) {
            specialEelProcessing.continueSacred(this)
        }
        onOpHeld4(objs.infernal_eel) { specialEelProcessing.rejectInfernalEating(this) }
        onOpHeld4(objs.sacred_eel) { specialEelProcessing.rejectSacredEating(this) }
        onOpHeldU(objs.crystal_harpoon, objs.crystal_shard) {
            crystalHarpoonLifecycle.charge(this, it.firstSlot, it.secondSlot)
        }
        onOpHeldU(objs.crystal_harpoon_inactive, objs.crystal_shard) {
            crystalHarpoonLifecycle.charge(this, it.firstSlot, it.secondSlot)
        }
        onOpHeldU(objs.dragon_harpoon, objs.smouldering_stone) {
            infernalHarpoonCrafting.create(this, it.firstSlot, it.secondSlot)
        }
        for (recharge in infernalHarpoonCrafting.rechargePairs) {
            onOpHeldU(recharge.depleted, objs.smouldering_stone) {
                infernalHarpoonCrafting.recharge(
                    this,
                    it.firstSlot,
                    it.secondSlot,
                    recharge,
                    objs.smouldering_stone,
                )
            }
            onOpHeldU(recharge.depleted, objs.dragon_harpoon) {
                infernalHarpoonCrafting.recharge(
                    this,
                    it.firstSlot,
                    it.secondSlot,
                    recharge,
                    objs.dragon_harpoon,
                )
            }
            onOpWorn2(recharge.charged) {
                infernalHarpoonCrafting.checkCharges(this, player.worn[it.slot])
            }
        }
        onOpHeld3(objs.infernal_harpoon) {
            infernalHarpoonCrafting.checkCharges(this, it.inventory[it.slot])
        }
        onOpHeld2(objs.infernal_harpoon_or) {
            infernalHarpoonCrafting.checkCharges(this, it.inventory[it.slot])
        }
        onOpHeld2(objs.infernal_harpoon_or_reloaded) {
            infernalHarpoonCrafting.checkCharges(this, it.inventory[it.slot])
        }
        onOpHeld3(objs.crystal_harpoon) {
            crystalHarpoonLifecycle.checkCharges(this, it.inventory[it.slot])
        }
        onOpWorn2(objs.crystal_harpoon) {
            crystalHarpoonLifecycle.checkCharges(this, player.worn[it.slot])
        }
        onOpHeld4(objs.crystal_harpoon) {
            crystalHarpoonLifecycle.revert(this, it.slot, objs.crystal_harpoon)
        }
        onOpHeld4(objs.crystal_harpoon_inactive) {
            crystalHarpoonLifecycle.revert(this, it.slot, objs.crystal_harpoon_inactive)
        }
        onProtectedEvent<BankedObjEvents.ConfigureCharges>(objs.crystal_harpoon.id) {
            crystalHarpoonLifecycle.configureCharges(this)
        }
        onProtectedEvent<BankedObjEvents.ConfigureCharges>(objs.crystal_harpoon_inactive.id) {
            crystalHarpoonLifecycle.configureCharges(this)
        }
        for (ornament in infernalHarpoonCrafting.ornamentPairs) {
            onOpHeldU(ornament.plain, objs.trailblazer_tool_ornament_kit) {
                infernalHarpoonCrafting.applyOrnament(this, it.firstSlot, it.secondSlot, ornament)
            }
        }
        for (style in infernalHarpoonCrafting.ornamentStyles) {
            for (ornamented in listOf(style.trailblazer, style.reloaded)) {
                onOpHeld1(ornamented) {
                    infernalHarpoonCrafting.wieldOrnamented(this, it.inventory, it.slot)
                }
                onOpHeld3(ornamented) {
                    infernalHarpoonCrafting.revertOrnament(this, it.slot, ornamented, style.plain)
                }
                onOpHeld4(ornamented) {
                    infernalHarpoonCrafting.changeStyle(this, it.inventory, it.slot, style)
                }
                onOpWorn3(ornamented) {
                    infernalHarpoonCrafting.changeStyle(this, player.worn, it.slot, style)
                }
            }
        }
        for (spot in SmallNetBaitFishingNpcs.all) {
            onOpNpc1(spot) { fishing.fish(this, it.npc, StandardFishingMethods.smallNet) }
            onOpNpc3(spot) { fishing.fish(this, it.npc, StandardFishingMethods.bait) }
        }
        for (spot in LureBaitFishingNpcs.all) {
            onOpNpc1(spot) { fishing.lure(this, it.npc) }
            onOpNpc3(spot) { fishing.fish(this, it.npc, StandardFishingMethods.pike) }
        }
        for (spot in CageHarpoonFishingNpcs.all) {
            onOpNpc1(spot) { fishing.fish(this, it.npc, StandardFishingMethods.lobster) }
        }
        for (spot in CageHarpoonFishingNpcs.tunaSwordfish) {
            onOpNpc3(spot) { fishing.fish(this, it.npc, StandardFishingMethods.harpoon) }
        }
        for (spot in BigNetHarpoonFishingNpcs.all) {
            onOpNpc3(spot) { fishing.fish(this, it.npc, StandardFishingMethods.shark) }
        }
        for (spot in BigNetHarpoonFishingNpcs.bigNet) {
            onOpNpc1(spot) { fishing.bigNet(this, it.npc) }
        }
        for (spot in BarbarianFishingNpcs.all) {
            onOpNpc1(spot) { fishing.fish(this, it.npc, StandardFishingMethods.barbarianRod) }
        }
        for (spot in KarambwanjiFishingNpcs.all) {
            onOpNpc1(spot) { karambwanjiFishing.fish(this, it.npc) }
        }
        for (spot in KarambwanFishingNpcs.postQuest) {
            onOpNpc1(spot) { karambwanFishing.fish(this, it.npc) }
        }
        for (spot in CaveEelFishingNpcs.all) {
            onOpNpc1(spot) { fishing.fish(this, it.npc, StandardFishingMethods.frogspawnNet) }
            onOpNpc3(spot) { fishing.fish(this, it.npc, StandardFishingMethods.caveEels) }
        }
        for (spot in SwampEelFishingNpcs.all) {
            onOpNpc1(spot) { fishing.fish(this, it.npc, StandardFishingMethods.swampSlimyEel) }
        }
        for (spot in LavaEelFishingNpcs.all) {
            onOpNpc1(spot) { fishing.fish(this, it.npc, StandardFishingMethods.lavaEel) }
        }
        onOpNpc1(SpecialEelFishingNpcs.infernal) {
            fishing.fish(this, it.npc, StandardFishingMethods.infernalEel)
        }
        onOpNpc1(SpecialEelFishingNpcs.sacred) {
            fishing.fish(this, it.npc, StandardFishingMethods.sacredEel)
        }
    }
}
