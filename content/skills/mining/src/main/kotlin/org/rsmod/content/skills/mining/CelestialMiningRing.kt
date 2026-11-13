package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ring
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal object StarItems : ObjReferences() {
    val dust = find("star_dust")
    val ring = find("celestial_ring")
    val charged = find("celestial_ring_charged")
}
internal object StarBits : VarBitReferences() { val withdraw = find("charges_celestial_ring_quantity") }

public class CelestialMiningRing @Inject constructor() : PluginScript() {
    private val rings get() = listOf(StarItems.ring to StarItems.charged, objs.celestial_signet to objs.celestial_signet_charged)
    override fun ScriptContext.startup() {
        for ((empty, full) in rings) {
            onOpHeld1(full) { invEquip(it.slot) }
            onOpHeld2(full) { checkCharges(inv[it.slot]) }
            onOpHeld5(full) { uncharge(it.slot,empty) }
            onOpWorn2(full) { checkCharges(player.ring) }
            for (type in listOf(empty,full)) {
                onOpHeld3(type) { charge(it.slot,full) }
                onOpHeldU(type,StarItems.dust) {
                    val slot = if (inv[it.firstSlot]?.id == type.id) it.firstSlot else it.secondSlot
                    charge(slot,full)
                }
                onProtectedEvent<BankedObjEvents.ConfigureCharges>(type.id) {
                    if (player.members) {
                        val n = countDialog("How many charges should the ring hold on withdrawal?")
                        if (n >= 0) vars[StarBits.withdraw] = n.coerceAtMost(MAX)
                    }
                }
                onProtectedEvent<BankedObjEvents.Autocharge>(type.id) { event ->
                    val item = inv[event.slot] ?: return@onProtectedEvent
                    if (!player.members || item != event.obj) return@onProtectedEvent
                    val charges = charges(item)
                    val request = vars[StarBits.withdraw].coerceAtMost(MAX) - charges
                    if (request <= 0) return@onProtectedEvent
                    val paid = event.takeMaterial(StarItems.dust,request)
                    if (paid > 0) inv[event.slot] = charged(full, item, charges + paid)
                    if (paid < request) mes("Your bank has insufficient stardust to fully charge this item.")
                }
            }
        }
    }

    private fun charges(item: InvObj): Int =
        if (rings.none { it.second.id == item.id }) 0
        else item.vars.getBits(varobjs.charges_16383.bits).coerceIn(1,MAX)

    private fun charged(type: ObjType, item: InvObj, n: Int): InvObj =
        InvObj(type,item.count,item.vars.withBits(varobjs.charges_16383.bits,n))

    private fun ProtectedAccess.checkCharges(item: InvObj?) {
        if (item != null) mes("Your ring has ${charges(item)} charges remaining.")
    }

    private suspend fun ProtectedAccess.charge(slot: Int, full: ObjType) {
        if (!player.members) return
        val item = inv[slot] ?: return
        if (rings.none { it.second == full && item.id in listOf(it.first.id,it.second.id) }) return
        val old = charges(item)
        val limit = minOf(MAX-old,invTotal(inv,StarItems.dust))
        if (limit <= 0) {
            mes(if (old == MAX) "Your ring is fully charged." else "You need stardust to charge your ring.")
            return
        }
        val count = if (limit == 1) 1 else countDialog("How much stardust would you like to add?").coerceAtMost(limit)
        if (count<=0 || inv[slot]!=item) return
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,StarItems.dust.id,count)
            delete(bag,item.id,1,slot)
            add(bag,full.id,1,vars=charged(full,item,old+count).vars,slot=slot)
        }
        if (result.success) checkCharges(inv[slot])
    }

    private suspend fun ProtectedAccess.uncharge(slot: Int, empty: ObjType) {
        val item = inv[slot] ?: return
        if (!player.members || !choice2("Remove the stardust.",true,"Keep the charges.",false) || inv[slot]!=item) return
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,item.id,1,slot)
            add(bag,empty.id,1,slot=slot)
            add(bag,StarItems.dust.id,charges(item))
        }
        if (!result.success) mes("You need space for the stardust.")
    }

    /** The invisible four levels affect eligible success rolls, even when the ring is uncharged. */
    public fun invisibleBoost(player: Player): Int =
        if (player.members && rings.any { player.ring?.id == it.first.id || player.ring?.id == it.second.id }) 4 else 0

    /** Call once after obtaining a resource from an eligible node. The charge is spent on every roll. */
    public fun rollExtra(access: ProtectedAccess): Boolean = with(access) {
        val item = player.ring ?: return false
        val (empty,full) = rings.firstOrNull { item.id == it.second.id } ?: return false
        if (!player.members) return false
        val remaining = charges(item) - 1
        player.ring = if (remaining == 0) InvObj(empty) else charged(full,item,remaining)
        if (remaining == 0) {
            rebuildAppearance()
            mes("Your ring has run out of stardust.")
        }
        return random.of(10) == 0
    }
    private companion object { const val MAX = 10_000 }
}
