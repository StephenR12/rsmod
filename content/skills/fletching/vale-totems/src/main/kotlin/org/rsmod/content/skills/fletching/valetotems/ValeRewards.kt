package org.rsmod.content.skills.fletching.valetotems

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.onOmniShopButton
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ValeShopUi : InterfaceReferences() {
    val main = find("omnishop_main")
    val side = find("omnishop_side")
}

internal object ValeShopButtons : ComponentReferences() {
    val list = find("omnishop_main:list")
    val bag = find("omnishop_side:items")
    val buy = (1..4).map { find("omnishop_main:button_$it") }
    val sell = (1..4).map { find("omnishop_side:button_$it") }
}

internal object ValeShopVars : VarpReferences() {
    val last = find("omnishop_lastshop")
    val selected = find("omnishop_selected_id")
    val side = find("omnishop_selected_side_id")
    val redraw = find("omnishop_redraw")
}

internal object ValeShopBits : VarBitReferences() {
    val iron = find("ironman")
    val forester = find("forestry_forester_met")
}

public class ValeRewards
@Inject
constructor(
    private val items: ObjTypeList,
    private val ground: ObjRepository,
    private val clues: ClueInventory,
) : PluginScript() {
    private val stock = listOf(31052, 31043, 31034, 31032)
    private val cost = listOf(250, 350, 500, 20)
    private val quantities = listOf(1, 5, 10, 50)

    override fun ScriptContext.startup() {
        val offerings = items.getValue(31054)
        onOpHeld1(offerings) { rummage(1, it.slot) }
        onOpHeld2(offerings) { rummage(5, it.slot) }
        onOpHeld3(offerings) {
            mes(
                "Each 100 offerings can be exchanged for one research point and a reward. You have ${vars[ValeVars.points]} research points."
            )
        }
        for (row in listOf(5458, 5459)) {
            onOmniShopButton(row, ValeShopButtons.list) { e ->
                val i = e.comsub / 12
                if (e.comsub < 0 || i !in stock.indices) return@onOmniShopButton
                vars[ValeShopVars.selected] = i
                if (e.op.slot == 1) runClientScript(7146, row, -1, i, 0, 0, 0, -1, "", 1)
                else quantities.getOrNull(e.op.slot - 2)?.let { buy(i, it) }
            }
            onOmniShopButton(row, ValeShopButtons.bag) { e ->
                val obj = inv[e.comsub] ?: return@onOmniShopButton
                if (obj.id != e.obj?.id) return@onOmniShopButton
                vars[ValeShopVars.side] = e.comsub
                if (e.op.slot == 1)
                    mes("Ranulph will give you ${refund(obj.id)} research points for this item.")
                else quantities.getOrNull(e.op.slot - 2)?.let { sell(e.comsub, it) }
            }
            for (i in quantities.indices) {
                onOmniShopButton(row, ValeShopButtons.buy[i]) {
                    if (it.op.slot == 1) buy(vars[ValeShopVars.selected], quantities[i])
                }
                onOmniShopButton(row, ValeShopButtons.sell[i]) {
                    if (it.op.slot == 1) sell(vars[ValeShopVars.side], quantities[i])
                }
            }
        }
    }

    internal fun shop(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members || vars[ValeBits.intro] < 100) return
            val row = if (vars[ValeShopBits.iron] != 0) 5459 else 5458
            ifOpenMainSidePair(ValeShopUi.main, ValeShopUi.side)
            vars[ValeShopVars.last] = row
            vars[ValeShopVars.selected] = -1
            vars[ValeShopVars.side] = -1
            invTransmit(inv)
            runClientScript(7140, row, -1)
            runClientScript(7246, row)
            ifSetEvents(
                ValeShopButtons.list,
                0..47,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
            )
            ifSetEvents(
                ValeShopButtons.bag,
                inv.indices,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
            )
            for (button in ValeShopButtons.buy + ValeShopButtons.sell) ifSetEvents(
                button,
                -1..0,
                IfEvent.Op1,
            )
        }

    private fun ProtectedAccess.buy(index: Int, quantity: Int) {
        if (!player.members || vars[ValeBits.intro] < 100 || index !in stock.indices) return
        val price = cost[index]
        val item = items.getValue(stock[index])
        val count = minOf(quantity, vars[ValeVars.points] / price)
        if (count < 1) {
            mes("You don't have enough Vale Research Points.")
            return
        }
        val delivered = invAdd(inv, item, count, strict = false)
        if (delivered.success) {
            vars[ValeVars.points] -= (delivered.asOk(0)?.completed ?: 0) * price
            vars[ValeShopVars.redraw]++
        }
    }

    private fun ProtectedAccess.refund(id: Int): Int =
        when (id) {
            31052 -> 125
            31043 -> 175
            31034 -> if (vars[ValeShopBits.iron] != 0) 125 else 0
            else -> 0
        }

    private fun ProtectedAccess.sell(slot: Int, request: Int) {
        if (!player.members || vars[ValeBits.intro] < 100 || slot !in inv.indices) return
        val id = inv[slot]?.id ?: return
        val price = refund(id)
        if (price == 0) return
        if (id == 31052 && vars[org.rsmod.api.config.refs.varbits.bow_string_spool_charges] != 0) {
            mes("Empty your spool before selling it.")
            return
        }
        val count =
            minOf(
                request,
                inv.count(items.getValue(id)),
                (Int.MAX_VALUE - vars[ValeVars.points]) / price,
            )
        if (count > 0 && invDel(inv, items.getValue(id), count).success) {
            vars[ValeVars.points] += price * count
            vars[ValeShopVars.redraw]++
        }
    }

    private fun ProtectedAccess.rummage(max: Int, slot: Int) {
        if (!player.members || vars[ValeBits.intro] < 100) return
        val offerings = items.getValue(31054)
        val n = minOf(max, (inv[slot]?.count ?: 0) / 100, Int.MAX_VALUE - vars[ValeVars.points])
        if (n <= 0) {
            mes("You need at least 100 vale offerings to rummage.")
            return
        }
        if (invDel(inv, offerings, n * 100, slot = slot).failure) return
        vars[ValeVars.points] += n
        repeat(n) {
            val drop =
                ValeOfferings.roll(
                    random,
                    player.statBase(stats.fletching),
                    vars[ValeShopBits.forester] != 0,
                )
            invAddOrDrop(ground, items.getValue(drop.id), random.of(drop.low..drop.high))
            // Only relative rarities are published for tertiary Vale clue nests.
            when (random.of(512)) {
                in 0..3 -> giveClueNest(ClueTier.Beginner)
                in 4..7 -> giveClueNest(ClueTier.entries[random.of(1..4)])
            }
        }
        mes("You find some supplies and gain $n Vale Research Points.")
    }

    internal fun handInSeed(access: ProtectedAccess, slot: Int): Unit =
        with(access) {
            if (!player.members || vars[ValeBits.intro] < 100) return
            if (invDel(inv, items.getValue(31018), 1, slot = slot).failure) return
            val nest = random.of(100)
            if (nest < 5) {
                if (!giveClueNest(ClueTier.entries[nest]))
                    invAddOrDrop(ground, objs.empty_bird_nest, 1)
            } else {
                val ordinary =
                    when (nest) {
                        in 5..7 -> 5070 + nest - 5
                        in 8..37 -> 5074
                        else -> 5073
                    }
                invAddOrDrop(ground, items.getValue(ordinary), 1)
            }
            mes("The ent accepts the seed and gives you a bird nest.")
        }

    private fun ProtectedAccess.giveClueNest(tier: ClueTier): Boolean {
        val state = clues.state(player, inv, bank, tier)
        if (!state.canReceive) return false
        if (state.xMarksComplete) {
            invAddOrDrop(ground, tier.scrollBox, 1)
            invAddOrDrop(ground, objs.empty_bird_nest, 1)
        } else {
            val nests = listOf(23127, 19712, 19714, 19716, 19718)
            invAddOrDrop(ground, items.getValue(nests[tier.ordinal]), 1)
        }
        return true
    }
}
