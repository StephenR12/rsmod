package org.rsmod.content.skills.fletching.valetotems

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MaskVars : VarpReferences() {
    val styles = find("greenman_mask")
}

public class ValeRewardItems
@Inject
constructor(private val items: ObjTypeList, private val invTypes: InvTypeList) : PluginScript() {
    private val masks = listOf(31034, 31037, 31038, 31039, 31040, 31041, 31042)
    private val leaves = listOf(6020, 6022, 6024, 6028, 6026, 6030)

    override fun ScriptContext.startup() {
        for (mask in masks) {
            val type = items.getValue(mask)
            for ((log, decoration) in listOf(objs.maple_logs to 31027, objs.yew_logs to 31024)) {
                onOpHeldU(type, log) { carveFurniture(mask, log.id, decoration) }
            }
            onOpHeldU(type, objs.ent_branch) {
                val selection =
                    choice2(
                        "Carve a greenman statue from maple logs.",
                        true,
                        "Carve a greenman wall decoration from yew logs.",
                        false,
                    )
                if (selection) carveFurniture(mask, objs.maple_logs.id, 31027)
                else carveFurniture(mask, objs.yew_logs.id, 31024)
            }
        }
        val antler = items.getValue(31086)
        onOpHeldU(objs.chisel, antler) {
            if (!player.members || player.stat(stats.fletching) < 74) {
                mes("You need 74 Fletching to cut atlatl dart tips.")
                return@onOpHeldU
            }
            val available = inv.count(antler)
            repeat(available.coerceAtMost(28)) {
                if (objs.chisel !in inv || antler !in inv || player.stat(stats.fletching) < 74)
                    return@onOpHeldU
                anim(org.rsmod.api.config.refs.seqs.human_opalcutting)
                delay(4)
                if (objs.chisel !in inv || player.stat(stats.fletching) < 74) return@onOpHeldU
                val tx =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, antler.id, 1)
                        add(bag, objs.atlatl_dart_tips.id, 100)
                    }
                if (tx.failure) return@onOpHeldU
                statAdvance(stats.fletching, 10.0)
            }
        }
        val bale = items.getValue(31045)
        onOpHeld1(bale) {
            if (!player.members) return@onOpHeld1
            if (inv.freeSpace() < 25) {
                mes("You need at least 25 empty spaces to unpack the flax.")
                return@onOpHeld1
            }
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, bale.id, 1, it.slot)
                    add(bag, objs.flax.id, 25)
                }
            if (tx.success) mes("You unpack a bale of flax.")
        }
        onOpBankside(bale) {
            if (!player.members || player.inv[slot]?.id != bale.id) return@onOpBankside
            val bank = player.invMap.getOrPut(invTypes[invs.bank])
            val capacity = (Int.MAX_VALUE - bank.count(items[objs.flax])) / 25
            val n = minOf(player.inv.count(bale), capacity)
            if (n == 0) return@onOpBankside
            val tx =
                player.invTransaction(player.inv, bank) {
                    delete(select(player.inv), bale.id, n)
                    add(select(bank), objs.flax.id, 25 * n)
                }
            if (tx.success) player.mes("You unpack the bales into your bank.")
            else player.mes("Make room for the flax in your bank.")
        }
        onOpHeld1(items.getValue(31047)) {
            if (!player.members) return@onOpHeld1
            val level = player.stat(stats.fletching)
            val weights =
                when {
                    level >= 90 -> listOf(1, 60, 80, 45, 23)
                    level >= 80 -> listOf(17, 119, 154, 86, 44)
                    level >= 65 -> listOf(65, 65, 67, 36, 18)
                    level >= 50 -> listOf(138, 53, 28, 12, 5)
                    else -> listOf(69, 20, 5, 1, 0)
                }
            val tips = listOf(41, 42, 43, 44, 11237)
            var cleaned = 0
            // Stackable arrowtips are cleaned in small handfuls, preserving inventory capacity on
            // each exchange.
            val n = minOf(inv[it.slot]?.count ?: 0, 28)
            for (index in 0 until n) {
                var ticket = random.of(weights.sum())
                val output =
                    tips[
                        weights.indexOfFirst { weight ->
                            ticket -= weight
                            ticket < 0
                        }]
                val tx =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, 31047, 1)
                        add(bag, output, 1)
                    }
                if (tx.failure) break
                cleaned++
            }
            if (cleaned > 0) mes("You clean $cleaned arrowtips.")
            else mes("Make room for the clean arrowtips.")
        }
        for (mask in masks) {
            val type = items.getValue(mask)
            onOpHeld3(type) { style(inv, it.slot) }
            onOpWorn2(type) { style(worn, it.slot) }
            for ((i, id) in leaves.withIndex()) {
                onOpHeldU(type, items.getValue(id)) {
                    if (!player.members) return@onOpHeldU
                    val bit = 1 shl i
                    if (vars[MaskVars.styles] and bit != 0) {
                        mes("You have already learnt this style.")
                        return@onOpHeldU
                    }
                    val tx =
                        player.invTransaction(inv) {
                            val bag = select(inv)
                            delete(bag, id, 300)
                            delete(bag, mask, 1, it.firstSlot)
                            add(bag, masks[i + 1], 1, slot = it.firstSlot)
                        }
                    if (tx.success) {
                        vars[MaskVars.styles] = vars[MaskVars.styles] or bit
                        statAdvance(stats.fletching, 20.0)
                        mes("You unlock a new style for the Greenman mask.")
                    } else mes("You need 300 matching leaves to unlock this style.")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.carveFurniture(mask: Int, log: Int, product: Int) {
        val level = if (product == 31027) 53 else 79
        if (!player.members || player.stat(stats.fletching) < level) {
            mes("You need $level Fletching to carve this.")
            return
        }
        fun hasKnife() =
            objs.knife in inv || objs.fletching_knife in inv || objs.fletching_knife in worn
        if (!hasKnife() || inv.count(items[objs.ent_branch]) < 5 || items.getValue(log) !in inv) {
            mes("Bring a knife, five ent branches and the correct log.")
            return
        }
        anim(org.rsmod.api.config.refs.seqs.human_fletching)
        delay(4)
        if (!hasKnife() || player.stat(stats.fletching) < level) return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, mask, 1)
                delete(bag, log, 1)
                delete(bag, objs.ent_branch.id, 5)
                add(bag, product, 1)
            }
        if (tx.success) statAdvance(stats.fletching, if (product == 31027) 55.0 else 70.0)
    }

    private suspend fun ProtectedAccess.style(bag: Inventory, slot: Int) {
        if (!player.members) return
        val before = bag[slot] ?: return
        val allowed =
            masks.filterIndexed { index, _ ->
                index == 0 || vars[MaskVars.styles] and (1 shl (index - 1)) != 0
            }
        if (allowed.size == 1) {
            mes("Use 300 leaves on the mask to unlock a style.")
            return
        }
        val target =
            allowed.getOrNull(
                menu("Choose a Greenman mask style", true, allowed.map { items.getValue(it).name })
            ) ?: return
        if (bag[slot] != before || target == before.id) return
        player.invTransaction(bag) {
            val source = select(bag)
            delete(source, before.id, 1, slot)
            add(source, target, 1, slot = slot)
        }
    }
}
