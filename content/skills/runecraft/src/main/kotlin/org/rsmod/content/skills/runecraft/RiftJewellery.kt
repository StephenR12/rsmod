package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.travelWithItem
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.interfaces.bank.BankProximity
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RingBits : VarBitReferences() {
    val charges = find("ring_of_the_elements_charges")
    val last = find("ring_of_elements_last_destination")
}

internal object RiftHeirlooms : VarBitReferences() {
    val inspected = find("gotr_locket_opened")
    val amulet = find("gotr_amulet_of_the_eye_obtained")
    val bagColour = find("gotr_bag_colour")
    val amuletColour = find("gotr_amulet_colour")
}

internal object RingItems : ObjReferences() {
    val charged = find("ring_of_elements_charged")
}

internal object ColouredRiftItems : ObjReferences() {
    val bags = listOf("", "_red", "_green", "_blue").map { find("gotr_lost_bag$it") }
    val amulets = listOf("", "_red", "_green", "_blue").map { find("gotr_amulet_of_the_eye$it") }
}

internal object RiftGuides : NpcReferences() {
    val guide = find("lumbridge_guide")
}

public class RiftJewellery
@Inject
constructor(
    private val banks: BankProximity,
    private val types: ObjTypeList,
    private val events: EventBus,
) : PluginScript() {
    private val runes
        get() =
            listOf(objs.air_rune, objs.water_rune, objs.earth_rune, objs.fire_rune, objs.law_rune)

    private val destinations =
        listOf(
            CoordGrid(2981, 3276),
            CoordGrid(3170, 3155),
            CoordGrid(3288, 3468),
            CoordGrid(3314, 3279),
        )
    private val names = listOf("Air Altar", "Water Altar", "Earth Altar", "Fire Altar")

    override fun ScriptContext.startup() {
        for (ring in listOf(RiftLoot.ring, RingItems.charged)) {
            onOpHeld3(ring) { rub(inv, it.slot) }
            for (rune in runes) onOpHeldU(ring, rune) { charge() }
        }
        val ring = RingItems.charged
        onOpHeld4(ring) { go(inv, it.slot, vars[RingBits.last] - 1) }
        onOpHeld5(ring) { uncharge() }
        onOpWorn2(ring) { go(worn, it.slot, vars[RingBits.last] - 1) }
        onOpWorn3(ring) { go(worn, it.slot, 0) }
        onOpWorn4(ring) { go(worn, it.slot, 1) }
        onOpWorn5(ring) { go(worn, it.slot, 2) }
        onOpWorn6(ring) { go(worn, it.slot, 3) }
        registerHeirlooms()
    }

    private suspend fun ProtectedAccess.rub(bag: Inventory, slot: Int) {
        if (vars[RingBits.charges] <= 0) {
            mes("The ring has no charges. Use elemental runes and law runes to charge it.")
            return
        }
        val choice = menu("Ring of the elements (" + vars[RingBits.charges] + ")", true, names)
        if (choice in destinations.indices) go(bag, slot, choice)
    }

    private suspend fun ProtectedAccess.go(bag: Inventory, slot: Int, dest: Int) {
        if (dest !in destinations.indices) {
            rub(bag, slot)
            return
        }
        val id = bag[slot]?.id ?: return
        if (vars[RingBits.charges] <= 0) {
            mes("The ring has no charges.")
            return
        }
        travelWithItem(destinations[dest]) {
            val charges = vars[RingBits.charges]
            if (bag[slot]?.id != id || charges <= 0) return@travelWithItem false
            if (charges == 1 && !changeRings(RingItems.charged, RiftLoot.ring, 0))
                return@travelWithItem false
            vars[RingBits.charges] = charges - 1
            vars[RingBits.last] = dest + 1
            true
        }
    }

    private suspend fun ProtectedAccess.charge() {
        if (!player.members) return
        val current = vars[RingBits.charges]
        val max = minOf(10000 - current, runes.minOf { invTotal(inv, it) })
        if (max <= 0) {
            mes(
                if (current >= 10000) "The ring is fully charged."
                else "You need one of each elemental rune and a law rune per charge."
            )
            return
        }
        val count =
            if (max == 1) 1 else countDialog("Add how many charges? (1–$max)").coerceAtMost(max)
        if (
            count <= 0 ||
                vars[RingBits.charges] != current ||
                (RiftLoot.ring !in inv && RingItems.charged !in inv)
        )
            return
        if (changeRings(RiftLoot.ring, RingItems.charged, -count)) {
            vars[RingBits.charges] = current + count
            mes("Your ring now has " + vars[RingBits.charges] + " charges.")
        }
    }

    private fun ProtectedAccess.uncharge() {
        val count = vars[RingBits.charges]
        if (!player.members || count <= 0) return
        if (coords.x in 3120..3155 && coords.z in 3615..3645 || !banks.nearBank(coords)) {
            mes("You need to be at a bank to uncharge the ring.")
            return
        }
        if (changeRings(RingItems.charged, RiftLoot.ring, count)) {
            vars[RingBits.charges] = 0
            mes("You recover the runes from your ring.")
        }
    }

    // The charge pool belongs to the account. Apply the payment/refund and all stacked bank
    // conversions together before changing the equipped models or the saved charge count.
    private fun ProtectedAccess.changeRings(
        from: ObjType,
        to: ObjType,
        runesPerKind: Int,
    ): Boolean {
        val tx =
            player.invTransaction(inv, bank) {
                val bag = select(inv)
                val vault = select(bank)
                if (runesPerKind != 0)
                    for (r in runes) if (runesPerKind < 0) delete(bag, r.id, -runesPerKind)
                    else add(bag, r.id, runesPerKind)
                for (slot in bank.indices) {
                    val obj = bank[slot] ?: continue
                    if (obj.id == from.id) {
                        delete(vault, from.id, obj.count, slot)
                        add(vault, to.id, obj.count, vars = obj.vars)
                    }
                }
            }
        if (!tx.success) {
            mes("You need enough room for the runes and rings.")
            return false
        }
        for (storage in listOf(inv, worn)) for (slot in storage.indices) {
            val obj = storage[slot] ?: continue
            if (obj.id != from.id) continue
            storage[slot] = InvObj(to, obj.count, obj.vars)
            if (storage === worn) {
                events.publish(HeldEquipEvents.WearposChange(player, Wearpos.Ring, types[to]))
                rebuildAppearance()
            }
        }
        return true
    }

    private fun ScriptContext.registerHeirlooms() {
        val outfits =
            listOf(
                RunecraftObjs.raimentsHats,
                RunecraftObjs.raimentsTorsos,
                RunecraftObjs.raimentsLegs,
                ColouredRiftItems.bags,
                ColouredRiftItems.amulets,
            )
        for (set in outfits) {
            for ((i, dye) in RiftLoot.dyes.withIndex()) {
                val shade = i + 1
                onOpHeldU(set[0], dye) {
                    if (recolour(set[0], set[shade], dye)) saveColour(set, shade)
                }
                val dyed = set[shade]
                val revert: suspend ProtectedAccess.() -> Unit = {
                    if (
                        choice2(
                            "Remove the dye and return the item to its original colour.",
                            true,
                            "Keep it as it is.",
                            false,
                        )
                    ) {
                        if (recolour(dyed, set[0], null)) saveColour(set, 0)
                    }
                }
                if (set == ColouredRiftItems.bags || set == ColouredRiftItems.amulets)
                    onOpHeld4(dyed) { revert() }
                else onOpHeld3(dyed) { revert() }
            }
        }
        for (amulet in ColouredRiftItems.amulets) {
            onOpHeld3(amulet) { travelWithItem(CoordGrid(3615, 9460)) { amulet in inv } }
            onOpWorn2(amulet) { travelWithItem(CoordGrid(3615, 9460)) { amulet in worn } }
        }
        for (bag in ColouredRiftItems.bags) onOpHeld3(bag) {
            mes("The bag bears the markings of an old expedition.")
        }
        onOpHeld1(RiftLoot.locket) {
            vars[RiftHeirlooms.inspected] = 1
            mes("Inside the locket is a note from Phileas to his beloved Thenia.")
        }
        onOpNpcU(RiftGuides.guide, RiftLoot.locket) { talkToGuide() }
        onOpNpc1(RiftGuides.guide) { talkToGuide() }
    }

    private fun ProtectedAccess.recolour(from: ObjType, to: ObjType, dye: ObjType?): Boolean {
        if (!player.members) return false
        val tx =
            player.invTransaction(inv) {
                val b = select(inv)
                delete(b, from.id, 1)
                if (dye != null) delete(b, dye.id, 1)
                add(b, to.id, 1)
            }
        return tx.success
    }

    private fun ProtectedAccess.saveColour(set: List<ObjType>, shade: Int) {
        when (set) {
            ColouredRiftItems.amulets -> vars[RiftHeirlooms.amuletColour] = shade
            ColouredRiftItems.bags -> vars[RiftHeirlooms.bagColour] = shade
        }
    }

    private fun ProtectedAccess.talkToGuide() {
        if (!player.members) {
            mes("Welcome to Lumbridge.")
            return
        }
        if (RiftLoot.locket in inv) {
            if (vars[RiftHeirlooms.inspected] == 0) {
                mes("You should inspect the locket first.")
                return
            }
            if (recolour(RiftLoot.locket, RiftLoot.amulet, null)) {
                vars[RiftHeirlooms.amulet] = 1
                mes("Phileas recognises his family's locket and gives you the restored amulet.")
            }
        } else if (
            vars[RiftHeirlooms.amulet] != 0 &&
                ColouredRiftItems.amulets.none { it in inv || it in bank || it in worn }
        ) {
            invAdd(inv, ColouredRiftItems.amulets[vars[RiftHeirlooms.amuletColour].coerceIn(0, 3)])
        } else mes("Welcome to Lumbridge.")
    }
}
