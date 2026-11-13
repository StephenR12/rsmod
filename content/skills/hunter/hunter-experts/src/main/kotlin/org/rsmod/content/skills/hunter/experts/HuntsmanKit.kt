package org.rsmod.content.skills.hunter.experts

import jakarta.inject.Inject
import org.rsmod.api.invtx.invClear
import org.rsmod.api.invtx.invSwap
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HuntsmanInv : InvReferences() {
    val items = find("huntsmans_kit")
}

internal object HuntsmanSave : InvEditor() {
    init {
        edit(HuntsmanInv.items) {
            scope = InvScope.Perm
            protect = false
            stack = InvStackType.Always
        }
    }
}

internal object HuntsmanUi : InterfaceReferences() {
    val main = find("huntsmans_kit")
    val side = find("huntsmans_kit_side")
}

internal object HuntsmanButton : ComponentReferences() {
    val stored = find("huntsmans_kit:items")
    val bag = find("huntsmans_kit_side:items")
    val fill = find("huntsmans_kit:fill")
    val empty = find("huntsmans_kit:empty")
    val quantities = listOf("1", "5", "all", "x").map { find("huntsmans_kit:$it") }
}

internal object HuntsmanBits : VarBitReferences() {
    val quantity = find("ii_elnock_storage_selectedquantity")
}

public class HuntsmanKit
@Inject
constructor(
    private val invs: InvTypeList,
    private val objects: ObjTypeList,
    private val enums: EnumTypeList,
) : PluginScript() {
    private val box
        get() = objects.getValue(29309)

    // Client script 4992 uses this enum to enable items in the side inventory.
    private val accepts by lazy {
        enums.getValue(5397).primitiveMap.values.filterIsInstance<Int>().toSet()
    }

    override fun ScriptContext.startup() {
        onOpHeld1(box) { open() }
        onOpHeld2(box) { fill(player) }
        onOpHeld3(box) { empty() }
        for (id in accepts) onOpHeldU(box, objects.getValue(id)) {
            deposit(player, it.secondSlot, Int.MAX_VALUE)
        }
        onOpBankside(box) { if (player.inv[slot]?.id == box.id) fill(player) }
        onDestroyHeld { if (type.id == box.id) player.invClear(storage(player)) }
        onIfClose(HuntsmanUi.main) {
            player.invMap[HuntsmanInv.items]?.let(player::stopInvTransmit)
        }
        onIfModalButton(HuntsmanButton.fill) { fill(player) }
        onIfModalButton(HuntsmanButton.empty) { empty() }
        for ((mode, button) in HuntsmanButton.quantities.withIndex()) onIfModalButton(button) {
            if (owns(player)) vars[HuntsmanBits.quantity] = mode
        }
        onIfModalButton(HuntsmanButton.bag) { transfer(it.comsub, it.obj?.id, it.op.slot, true) }
        onIfModalButton(HuntsmanButton.stored) {
            transfer(it.comsub, it.obj?.id, it.op.slot, false)
        }
        for (component in listOf(HuntsmanButton.bag, HuntsmanButton.stored)) onIfModalDrag(
            component
        ) { e ->
            if (!owns(player)) return@onIfModalDrag
            val from = e.selectedSlot ?: return@onIfModalDrag
            val to = e.targetSlot ?: return@onIfModalDrag
            val container = if (component == HuntsmanButton.bag) inv else storage(player)
            if (
                from !in container.indices ||
                    to !in container.indices ||
                    container[from]?.id != e.selectedObj ||
                    container[to]?.id != e.targetObj
            )
                return@onIfModalDrag
            player.invSwap(container, from, to, mergeStacks = false)
        }
    }

    private fun storage(p: Player) = p.invMap.getOrPut(invs[HuntsmanInv.items])

    private fun owns(p: Player) = p.members && box in p.inv

    private fun ProtectedAccess.open() {
        if (!owns(player)) return
        ifOpenMainSidePair(HuntsmanUi.main, HuntsmanUi.side)
        val stored = storage(player)
        invTransmit(stored)
        invTransmit(inv)
        for ((component, range) in
            listOf(
                HuntsmanButton.bag to inv.indices,
                HuntsmanButton.stored to stored.indices,
            )) ifSetEvents(
            component,
            range,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
            IfEvent.DragTarget,
            IfEvent.Depth1,
        )
        for (c in
            HuntsmanButton.quantities +
                listOf(HuntsmanButton.fill, HuntsmanButton.empty)) ifSetEvents(
            c,
            -1..0,
            IfEvent.Op1,
        )
    }

    private fun fill(p: Player) {
        if (!owns(p)) return
        for (slot in p.inv.indices) deposit(p, slot, Int.MAX_VALUE)
    }

    private fun deposit(p: Player, slot: Int, request: Int) {
        val item = p.inv[slot] ?: return
        if (!owns(p) || item.id !in accepts || request <= 0) return
        p.invTransfer(p.inv, slot, minOf(item.count, request), storage(p), strict = false)
    }

    private fun ProtectedAccess.empty() {
        if (!owns(player)) return
        val stored = storage(player)
        for (slot in stored.indices.reversed()) stored[slot]?.let {
            invMoveFromSlot(stored, inv, slot, it.count, strict = false)
        }
    }

    private suspend fun ProtectedAccess.transfer(slot: Int, id: Int?, op: Int, storing: Boolean) {
        if (!owns(player)) return
        val stored = storage(player)
        val from = if (storing) inv else stored
        val item = from[slot] ?: return
        if (item.id != id) return
        if (op == 10) {
            mes(objects[item].desc)
            return
        }
        // In either panel op 1 is the selected quantity. Fixed secondary options keep their slots.
        val mode =
            when (op) {
                1 -> vars[HuntsmanBits.quantity]
                2 -> 0
                3 -> 1
                4 -> 3
                5 -> 2
                else -> return
            }
        val count =
            when (mode) {
                0 -> 1
                1 -> 5
                2 -> item.count
                3 -> countDialog().coerceIn(0, item.count)
                else -> return
            }
        if (count <= 0 || !owns(player) || from[slot] != item) return
        if (storing) deposit(player, slot, count)
        else invMoveFromSlot(stored, inv, slot, minOf(count, item.count), strict = false)
    }
}
