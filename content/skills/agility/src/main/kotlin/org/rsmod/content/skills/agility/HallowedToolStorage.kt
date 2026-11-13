package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private val toolNames = listOf("grapple", "focus", "symbol", "hammer", "ring", "token")

internal object HallowedTools : ObjReferences() {
    val items = toolNames.map { find("hallowed_$it") }
}

internal object HallowedToolBits : VarBitReferences() {
    val stock = toolNames.map { find("hallowed_storage_$it") }
    val quantity = find("farming_tools_selectedquantity")
}

internal object HallowedToolInterfaces : InterfaceReferences() {
    val main = find("hallowed_tools")
    val side = find("hallowed_tool_side")
}

internal object HallowedToolButtons : ComponentReferences() {
    val stored = toolNames.map { find("hallowed_tools:$it") }
    val carried = toolNames.map { find("hallowed_tool_side:$it") }
    val withdrawal = find("hallowed_tools:withdrawal_all")
    val deposit = find("hallowed_tool_side:store_all")
    val quantity = listOf("1", "5", "x", "all").map { find("hallowed_tools:quantity_$it") }
}

public class HallowedToolStorage @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc5(SepulchreActors.stranger) { open(this) }
        for (i in toolNames.indices) {
            onIfModalButton(HallowedToolButtons.stored[i]) { transfer(i, false, it.op.slot) }
            onIfModalButton(HallowedToolButtons.carried[i]) { transfer(i, true, it.op.slot) }
        }
        onIfModalButton(HallowedToolButtons.withdrawal) {
            for (i in toolNames.indices) move(i, false, 1000)
        }
        onIfModalButton(HallowedToolButtons.deposit) {
            for (i in toolNames.indices) move(i, true, 1000)
        }
        for ((mode, button) in HallowedToolButtons.quantity.withIndex()) onIfModalButton(button) {
            vars[HallowedToolBits.quantity] = listOf(0, 1, 3, 2)[mode]
        }
    }

    internal fun open(a: ProtectedAccess) =
        with(a) {
            if (!player.members || vars[SepulchreBits.intro] == 0) return@with
            ifOpenMainSidePair(HallowedToolInterfaces.main, HallowedToolInterfaces.side)
            for (button in HallowedToolButtons.stored + HallowedToolButtons.carried) ifSetEvents(
                button,
                -1..4,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
            )
            for (button in
                HallowedToolButtons.quantity +
                    listOf(
                        HallowedToolButtons.withdrawal,
                        HallowedToolButtons.deposit,
                    )) ifSetEvents(button, -1..0, IfEvent.Op1)
        }

    private suspend fun ProtectedAccess.transfer(i: Int, deposit: Boolean, op: Int) {
        val selected =
            when (vars[HallowedToolBits.quantity]) {
                1 -> 5
                2 -> 1000
                3 -> -1
                else -> 1
            }
        val n =
            if (i < 5) 1
            else
                (listOf(selected) + listOf(1, 5, -1, 1000).filter { it != selected }).getOrNull(
                    op - 1
                ) ?: return
        move(i, deposit, if (n == -1) countDialog("How many hallowed tokens?") else n)
    }

    private fun ProtectedAccess.move(i: Int, deposit: Boolean, requested: Int) {
        val item = HallowedTools.items[i]
        val bit = HallowedToolBits.stock[i]
        val old = vars[bit]
        val maximum = if (i == 5) 1000 else 1
        val count =
            minOf(requested, if (deposit) minOf(maximum - old, invTotal(inv, item)) else old)
        if (count <= 0) return
        if (deposit) {
            if (invDel(inv, item, count).success) vars[bit] = old + count
        } else {
            val placed = invAdd(inv, item, count, strict = false).completed()
            if (placed > 0) vars[bit] = old - placed
        }
    }
}
