package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.events.RemainsDrop
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onProtectedEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private enum class Device {
    Crusher, Sanctifier;

    val material get() = if (this == Crusher) EctoItems.tokens else objs.death_rune
    val units get() = if (this == Crusher) 25 else 10
    val max get() = if (this == Crusher) Int.MAX_VALUE else 2_147_483_622
    val bankTarget get() = if (this == Crusher) EctoBits.crusherBankCharges else EctoBits.sanctifierBankCharges
    val region get() = if (this == Crusher) DiaryRegion.Morytania else DiaryRegion.Kourend
    val mask get() = 1 shl ordinal
    fun accepts(item: InvObj?) = if (this == Crusher) item.isAnyType(EctoItems.crusher,EctoItems.crusherNecklace) else item.isType(DarkAltarGoods.sanctifier)
    fun enabled(player: Player) = player.vars[EctoVars.devicesDisabled] and mask == 0
}

public class PrayerDevices @Inject constructor(private val ground: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        val necklace = EctoItems.crusherNecklace
        for ((item,device) in listOf(EctoItems.crusher to Device.Crusher, necklace to Device.Crusher, DarkAltarGoods.sanctifier to Device.Sanctifier)) {
            onOpHeldU(item,device.material) { charge(device,item,it.firstSlot,it.secondSlot) }
            onOpHeld3(item) { toggle(device) }
            if (item == necklace) {
                onOpHeld4(item) {
                    checkCharges(it.obj)
                    if (choice2("Uncharge the necklace.",true,"Leave it charged.",false)) uncharge(device,inv,it.slot)
                }
            } else {
                onOpHeld2(item) { checkCharges(it.obj) }
                onOpHeld4(item) { uncharge(device,inv,it.slot) }
            }
            onProtectedEvent<BankedObjEvents.ConfigureCharges>(item.id) {
                if (player.members) vars[device.bankTarget] = countDialog("How many charges should this item hold on withdrawal?").coerceIn(0, device.max)
            }
            onProtectedEvent<BankedObjEvents.Autocharge>(item.id) {
                if (!player.members) return@onProtectedEvent
                val missing = vars[device.bankTarget] - it.obj.vars
                if (missing <= 0) return@onProtectedEvent
                val needed = ((missing.toLong() + device.units - 1) / device.units).toInt()
                val available = (device.max - it.obj.vars) / device.units
                val used = it.takeMaterial(device.material, minOf(needed,available))
                if (used > 0) inv[it.slot] = it.obj.copy(vars = it.obj.vars + used * device.units)
                if (used < minOf(needed,available)) mes("Your bank has insufficient materials to fully charge this item.")
            }
        }
        onOpWorn2(necklace) { checkCharges(it.obj) }
        onOpWorn3(necklace) { toggle(Device.Crusher) }
        onOpWorn4(necklace) { uncharge(Device.Crusher,worn,it.slot) }
        onOpHeld5(necklace) {
            if (choice2("Dismantle the bonecrusher necklace.",true,"Keep it.",false)) dismantle(it.slot)
        }
        val parts = listOf(EctoItems.crusher,EctoItems.dragonNecklace,EctoItems.hydraTail)
        for ((i, first) in parts.withIndex()) for (second in parts.drop(i+1)) {
            onOpHeldU(first,second) {
                if (parts.any { part -> part !in inv }) {
                    mes("You need a bonecrusher, a dragonbone necklace and a hydra tail.")
                } else if (choice2("Combine all three items into a bonecrusher necklace.", true, "Leave them separate.", false)) assemble()
            }
        }
        onEvent<HeldDropEvents.Destroy> {
            val device = Device.entries.firstOrNull { it.accepts(obj) } ?: return@onEvent
            val materials = obj.vars / device.units
            if (materials > 0) ground.add(device.material, player.coords, constants.lootdrop_duration, player, materials)
        }
        onEvent<RemainsDrop> { prepareRemains() }
    }

    private fun ProtectedAccess.checkCharges(obj: InvObj) { mes("Charges remaining: ${obj.vars}.") }

    private fun ProtectedAccess.toggle(device: Device) {
        if (!player.members) return
        vars[EctoVars.devicesDisabled] = vars[EctoVars.devicesDisabled] xor device.mask
        mes(if (device.enabled(player)) "Automatic processing enabled." else "Automatic processing disabled.")
    }

    private suspend fun ProtectedAccess.charge(device: Device,item: ObjType,slot: Int,materialSlot: Int) {
        if (!player.members) return
        val material = inv[materialSlot] ?: return
        if (!material.isType(device.material)) return
        val requested = if (material.count == 1 || device == Device.Crusher) material.count else countDialog("How many death runes would you like to use?")
        val held = inv[slot] ?: return
        if (!held.isType(item) || !inv[materialSlot].isType(device.material)) return
        val count = minOf(requested, inv.getValue(materialSlot).count, (device.max - held.vars) / device.units)
        if (count <= 0) return
        if (invDel(inv,device.material,count,materialSlot).success) {
            inv[slot] = held.copy(vars = held.vars + count * device.units)
            checkCharges(inv.getValue(slot))
        }
    }

    private fun ProtectedAccess.uncharge(device: Device,bag: Inventory,slot: Int) {
        val held = bag[slot] ?: return
        if (!player.members || !device.accepts(held)) return
        val count = held.vars / device.units
        if (count <= 0) {
            mes("There are too few charges to recover any materials.")
            return
        }
        if (invAdd(inv,device.material,count).success) {
            bag[slot] = held.copy(vars = held.vars % device.units)
            checkCharges(bag.getValue(slot))
        } else mes("You don't have enough inventory space.")
    }

    private fun ProtectedAccess.assemble() {
        val slot = inv.indexOfFirst { it.isType(EctoItems.crusher) }
        val crusher = inv[slot] ?: return
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,EctoItems.crusher.id,1,slot)
            delete(bag,EctoItems.dragonNecklace.id,1)
            delete(bag,EctoItems.hydraTail.id,1)
            add(bag,EctoItems.crusherNecklace.id,1,crusher.vars,slot)
        }
        if (result.success) mes("You attach the bonecrusher to the necklace.")
    }

    private fun ProtectedAccess.dismantle(slot: Int) {
        val necklace = inv[slot] ?: return
        if (!necklace.isType(EctoItems.crusherNecklace)) return
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,EctoItems.crusherNecklace.id,1,slot)
            add(bag,EctoItems.crusher.id,1,necklace.vars,slot)
            add(bag,EctoItems.dragonNecklace.id,1)
            add(bag,EctoItems.hydraTail.id,1)
        }
        mes(if (result.success) "You separate the three pieces." else "You need two free inventory spaces.")
    }

    private fun RemainsDrop.prepareRemains() {
        if (!player.members || remaining == 0) return
        val recipe = PrayerRemainsRecipes.byObj[obj.id] ?: return
        if (recipe.confirmBurial) return
        val device = if (recipe.kind == PrayerRemainsKind.Bone) Device.Crusher else Device.Sanctifier
        if (!device.enabled(player)) return
        val neck = Wearpos.Front.slot
        val equipped = device == Device.Crusher && player.worn[neck].isType(EctoItems.crusherNecklace)
        val bag = if (equipped) player.worn else player.inv
        val slot = if (equipped) neck else bag.indexOfFirst(device::accepts)
        val held = bag[slot] ?: return
        val count = minOf(remaining,held.vars)
        if (count <= 0) return
        convert(count) {
            bag[slot] = held.copy(vars = held.vars - count)
            val fullXp = player.vars[device.region.tier(DiaryDifficulty.Elite).reward] != 0
            val boosted = player.consumeAncestralBurials(recipe,count)
            player.statAdvance(stats.prayer,recipe.xp * (count + 2.0 * boosted) / if (fullXp) 1 else 2)
            if (device == Device.Crusher) player.restoreBurialPrayer(recipe.obj,count)
        }
    }
}

internal fun Player.restoreBurialPrayer(bone: ObjType,count: Int = 1) {
    val necklace = worn[Wearpos.Front.slot].isAnyType(EctoItems.dragonNecklace,EctoItems.crusherNecklace)
    val catacombs = coords.level == 0 && coords.x in 1600..1727 && coords.z in 9984..10111
    if (!necklace && !catacombs) return
    val units = when (bone) {
        objs.bones -> 1
        objs.big_bones -> 2
        objs.dragon_bones -> 4
        objs.jogre_bones,objs.small_ninja_monkey_bones -> if (necklace) 2 else 0
        objs.babydragon_bones,objs.wyrm_bones -> if (necklace) 3 else 0
        objs.wyvern_bones,objs.drake_bones,objs.dagannoth_bones,objs.hydra_bones -> if (necklace) 4 else 0
        objs.superior_dragon_bones -> if (necklace) 5 else 0
        else -> 0
    }
    if (units > 0) statHeal(stats.prayer, minOf(units.toLong() * count,255).toInt(),0)
}
