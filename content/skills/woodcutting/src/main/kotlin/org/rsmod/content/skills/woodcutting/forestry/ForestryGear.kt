package org.rsmod.content.skills.woodcutting.forestry

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfItemSubOp
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.interfaces.bank.deliverToBank
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GearInv : InvReferences() {
    val kit = find("forestry_kit")
    val logs = find("forestry_log_basket_storage")
    val forester = find("forestry_shop_log_storage")
}

internal object GearSlots : InvBuilder() {
    init {
        build("forestry_log_basket_storage") {
            scope = InvScope.Perm
            size = 28
            stack = InvStackType.Never
            protect = false
        }
    }
}

internal object GearSave : InvEditor() {
    init {
        for (id in listOf(GearInv.kit, GearInv.forester)) edit(id) {
            scope = InvScope.Perm
            stack = InvStackType.Always
            protect = false
        }
    }
}

internal object GearVars : VarpReferences() {
    val pouches = find("forestry_kit_pouches")
}

internal object GearUpgrades : VarpBuilder() {
    init {
        build("forestry_kit_pouches") { permanent = true }
    }
}

internal object ForestryBits : VarBitReferences() {
    val luck = find("gathering_event_woodcutting_leprechaun_total_rainbows")
    val house = find("poh_house_location")
    val quantity = find("forestry_kit_selectedquantity")
    val chosen = find("forestry_kit_selected_item")
    val iron = find("ironman")
    val met = find("forestry_forester_met")
}

internal object GearUi : InterfaceReferences() {
    val main = find("forestry_kit_main")
    val side = find("forestry_kit_side")
}

internal object GearButtons : ComponentReferences() {
    val list = find("forestry_kit_main:list")
    val bag = find("forestry_kit_side:items")
    val fill = find("forestry_kit_main:fill")
    val empty = find("forestry_kit_main:empty")
    val counts = listOf("1", "5", "x", "all").map { find("forestry_kit_main:$it") }
}

public class ForestryGear
@Inject
constructor(private val inventories: InvTypeList, private val types: ObjTypeList) : PluginScript() {
    private val kitIds = setOf(28136, 28143, 28145)
    private val basketIds = setOf(28140, 28142, 28143, 28145)
    private val openIds = setOf(28142, 28145)
    private val kitContents =
        listOf(
            28134,
            28152,
            28157,
            28161,
            6020,
            6022,
            6024,
            6028,
            6026,
            6030,
            10939,
            10940,
            10941,
            10933,
            28173,
            28169,
            28171,
            28175,
            9807,
            9808,
        )
    internal val logs =
        setOf(1511, 1521, 1519, 6333, 1517, 6332, 10810, 1515, 1513, 19669, 2862, 24691, 8934)

    public fun kit(p: Player) = p.invMap.getOrPut(inventories[GearInv.kit])

    public fun logStore(p: Player) = p.invMap.getOrPut(inventories[GearInv.logs])

    internal fun shopLogs(p: Player) = p.invMap.getOrPut(inventories[GearInv.forester])

    internal fun hasKit(p: Player) =
        p.inv.any { it?.id in kitIds } || p.worn.any { it?.id in kitIds }

    private fun hasBasket(p: Player, ids: Set<Int>) =
        p.members && (p.inv.any { it?.id in ids } || p.worn.any { it?.id in ids })

    internal fun hasBasket(p: Player) = hasBasket(p, basketIds)

    internal fun autoStores(p: Player, id: Int) =
        id in logs && hasBasket(p, openIds) && !logStore(p).isFull()

    internal fun storeLog(p: Player, type: ObjType) =
        autoStores(p, type.id) && p.invAdd(logStore(p), type, 1).success

    internal fun reward(p: Player, id: Int, count: Int): Boolean {
        if (count <= 0) return false
        val item = types.getValue(id)
        return hasKit(p) && allowed(p, id) && p.invAdd(kit(p), item, count).success ||
            p.invAdd(p.inv, item, count).success
    }

    internal fun consume(p: Player, id: Int): Boolean {
        if (!p.members) return false
        val item = types.getValue(id)
        val source = if (item in p.inv) p.inv else if (hasKit(p)) kit(p) else return false
        return item in source && p.invDel(source, item).success
    }

    internal fun capeEquipped(p: Player) =
        p.members &&
            p.vars[GearVars.pouches] and 2 != 0 &&
            p.worn.any { it?.id in kitIds } &&
            kit(p).any { it?.id == 9807 || it?.id == 9808 }

    private fun allowed(p: Player, id: Int): Boolean {
        val index = kitContents.indexOf(id)
        return when (index) {
            in 0..9 -> true
            in 10..17 -> p.vars[GearVars.pouches] and 1 != 0
            in 18..19 -> p.vars[GearVars.pouches] and 2 != 0
            else -> false
        }
    }

    override fun ScriptContext.startup() {
        for (id in kitIds + basketIds) {
            val obj = types.getValue(id)
            onOpHeld1(obj) {
                when (id) {
                    28136 -> view()
                    28143,
                    28145 -> chooseFill()
                    else -> fill(false)
                }
            }
            onOpHeld3(obj) {
                when (id) {
                    28136 -> fill(true)
                    28143,
                    28145 -> chooseView()
                    else -> checkLogs()
                }
            }
            if (id in basketIds) onProtectedEvent<IfItemSubOp>(id) { submenu(it) }
            if (id in basketIds) onOpHeld4(obj) { configure(id, it.slot, inv) }
            for (item in
                (if (id in kitIds) kitContents.toSet() else emptySet()) +
                    (if (id in basketIds) logs else emptySet())) {
                onOpHeldU(obj, types.getValue(item)) {
                    deposit(player, it.secondSlot, item !in logs, Int.MAX_VALUE)
                }
            }
            onOpBankside(obj) {
                if (player.inv[slot]?.id != id) return@onOpBankside
                if (id in basketIds && player.members && logStore(player).isNotEmpty())
                    bankLogs(player)
                else if (id in kitIds) player.invMoveAll(kit(player), player.inv)
            }
            val wearOps = listOf(params.wear_op1, params.wear_op2, params.wear_op3, params.wear_op4)
            for ((i, param) in wearOps.withIndex()) {
                val op = obj.paramOrNull(param) ?: continue
                when (i) {
                    0 -> onOpWorn2(obj) { wornAction(op, id, it.slot) }
                    1 -> onOpWorn3(obj) { wornAction(op, id, it.slot) }
                    2 -> onOpWorn4(obj) { wornAction(op, id, it.slot) }
                    3 -> onOpWorn5(obj) { wornAction(op, id, it.slot) }
                }
            }
        }
        onDestroyHeld {
            val bank = player.invMap[invs.bank]
            fun owns(ids: Set<Int>) =
                player.inv.any { it?.id in ids } ||
                    player.worn.any { it?.id in ids } ||
                    bank?.any { it?.id in ids } == true
            if (type.id in kitIds && !owns(kitIds)) {
                player.invClear(kit(player))
                VarPlayerIntMapSetter.set(player, GearVars.pouches, 0)
            }
            if (type.id in basketIds && !owns(basketIds)) player.invClear(logStore(player))
        }
        onIfClose(GearUi.main) { player.invMap[GearInv.kit]?.let(player::stopInvTransmit) }
        onIfModalButton(GearButtons.fill) { fill(true) }
        onIfModalButton(GearButtons.empty) {
            if (hasKit(player)) player.invMoveAll(kit(player), inv)
        }
        for ((mode, button) in GearButtons.counts.withIndex()) onIfModalButton(button) {
            vars[ForestryBits.quantity] = mode
        }
        onIfModalButton(GearButtons.bag) { e ->
            if (!hasKit(player)) return@onIfModalButton
            val item = inv[e.comsub] ?: return@onIfModalButton
            val count = quantity(item.count)
            if (item.id == e.obj?.id && inv[e.comsub] == item)
                deposit(player, e.comsub, true, count)
        }
        onIfModalButton(GearButtons.list) { e ->
            if (!hasKit(player)) return@onIfModalButton
            val stored = kit(player)
            val index = e.comsub / 12
            val id = kitContents.getOrNull(index) ?: return@onIfModalButton
            if (e.op.slot == 1) {
                vars[ForestryBits.chosen] = index + 1
                return@onIfModalButton
            }
            val slot = stored.indexOfFirst { it?.id == id }
            if (slot < 0) return@onIfModalButton
            val item = stored[slot] ?: return@onIfModalButton
            val count =
                when (e.op.slot) {
                    2 -> quantity(item.count)
                    3 -> 1
                    4 -> minOf(5, item.count)
                    5 -> countDialog().coerceIn(0, item.count)
                    6 -> item.count
                    else -> return@onIfModalButton
                }
            if (stored[slot] == item && count > 0)
                invMoveFromSlot(stored, inv, slot, count, strict = false)
        }
        for (id in kitIds) for ((bit, item) in listOf(1 to 28163, 2 to 28613)) onOpHeldU(
            types.getValue(id),
            types.getValue(item),
        ) {
            if (
                !player.members ||
                    vars[GearVars.pouches] and bit != 0 ||
                    bit == 2 && player.statBase(org.rsmod.api.config.refs.stats.woodcutting) < 99
            )
                return@onOpHeldU
            if (invDel(inv, types.getValue(item), 1, it.secondSlot).success)
                vars[GearVars.pouches] = vars[GearVars.pouches] or bit
        }
        for (id in listOf(28140, 28142)) for ((first, second) in
            listOf(id to 28136, id to 28149, 28136 to 28149).filter {
                it.first != 28136 || id == 28140
            }) {
            onOpHeldU(types.getValue(first), types.getValue(second)) {
                val b =
                    if (first in basketIds) first
                    else if (types.getValue(28140) in inv) 28140 else 28142
                if (!player.members) return@onOpHeldU
                val tx =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        for (material in listOf(28136, 28149, b)) delete(bag, material, 1)
                        add(bag, if (b == 28140) 28143 else 28145, 1)
                    }
                if (!tx.success) mes("You need a Forestry kit, log basket and sturdy harness.")
            }
        }
    }

    private fun ProtectedAccess.submenu(event: IfItemSubOp) {
        if (!player.members) return
        val combined = event.obj in kitIds
        val sub = event.subop
        when {
            combined && event.op == IfButtonOp.Op2 && sub in 1..2 -> fill(sub == 1)
            combined &&
                event.op == (if (event.worn) IfButtonOp.Op3 else IfButtonOp.Op4) &&
                sub in 1..2 -> if (sub == 1) view() else emptyLogs()
            combined && !event.worn && event.op == IfButtonOp.Op6 && sub in 1..2 ->
                changeBasket(event.obj, event.slot, inv, dismantle = sub == 2)
            !combined &&
                event.op == (if (event.worn) IfButtonOp.Op3 else IfButtonOp.Op4) &&
                sub == 1 -> emptyLogs()
        }
    }

    private suspend fun ProtectedAccess.wornAction(op: String, id: Int, slot: Int) {
        when (op.lowercase()) {
            "fill" ->
                if (id == 28136) fill(true) else if (id in kitIds) chooseFill() else fill(false)
            "check" -> checkLogs()
            "empty",
            "empty basket" -> emptyLogs()
            "view" -> if (id == 28136) view() else chooseView()
            "configure" -> configure(id, slot, player.worn)
            "open",
            "close" -> if (player.members) changeBasket(id, slot, player.worn, false)
        }
    }

    private suspend fun ProtectedAccess.chooseFill() {
        fill(choice2("Fill Forestry kit.", true, "Fill log basket.", false))
    }

    private suspend fun ProtectedAccess.chooseView() {
        if (choice2("View Forestry kit.", true, "Empty log basket.", false)) view() else emptyLogs()
    }

    private suspend fun ProtectedAccess.configure(id: Int, slot: Int, container: Inventory) {
        if (!player.members || container[slot]?.id != id) return
        val dismantle =
            id in kitIds &&
                choice2("Open or close the basket.", false, "Dismantle the basket.", true)
        changeBasket(id, slot, container, dismantle)
    }

    private fun ProtectedAccess.changeBasket(
        id: Int,
        slot: Int,
        container: Inventory,
        dismantle: Boolean,
    ) {
        if (container[slot]?.id != id) return
        if (dismantle) {
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(if (container === inv) bag else select(container), id, 1, slot)
                    add(bag, 28136, 1)
                    add(bag, 28149, 1)
                    add(bag, if (id == 28143) 28140 else 28142, 1)
                }
            if (!tx.success) mes("You need room for the kit, basket and harness.")
        } else
            invReplaceSlot(
                container,
                slot,
                1,
                types.getValue(
                    when (id) {
                        28140 -> 28142
                        28142 -> 28140
                        28143 -> 28145
                        else -> 28143
                    }
                ),
            )
    }

    private fun ProtectedAccess.fill(gear: Boolean) {
        if (gear && !hasKit(player) || !gear && !hasBasket(player, basketIds)) return
        for (slot in inv.indices) deposit(player, slot, gear, Int.MAX_VALUE)
    }

    private fun deposit(p: Player, slot: Int, gear: Boolean, count: Int) {
        if (count <= 0) return
        val item = p.inv[slot] ?: return
        if (
            gear && (!hasKit(p) || !allowed(p, item.id)) ||
                !gear && (!hasBasket(p, basketIds) || item.id !in logs)
        )
            return
        p.invTransfer(
            p.inv,
            slot,
            minOf(count, item.count),
            if (gear) kit(p) else logStore(p),
            strict = false,
        )
    }

    public fun bankLogs(player: Player) {
        val stored = logStore(player)
        val bank = player.invMap.getOrPut(inventories[invs.bank])
        for (slot in stored.indices) {
            val log = stored[slot] ?: continue
            if (player.deliverToBank(bank, types[log], log.count)) stored[slot] = null
        }
    }

    private suspend fun ProtectedAccess.checkLogs() {
        val store = logStore(player)
        if (store.isEmpty()) {
            mes("The log basket is empty.")
            return
        }
        val contents =
            store
                .filterNotNull()
                .groupingBy { types[it].name }
                .eachCount()
                .entries
                .joinToString(", ") { "${it.value} × ${it.key}" }
        mes("Your basket contains $contents.")
        if (choice2("Empty it into my inventory.", true, "Leave the logs in the basket.", false))
            emptyLogs()
    }

    private fun ProtectedAccess.emptyLogs() {
        if (hasBasket(player, basketIds)) player.invMoveAll(logStore(player), inv)
    }

    private suspend fun ProtectedAccess.quantity(max: Int) =
        when (vars[ForestryBits.quantity]) {
            1 -> minOf(5, max)
            2 -> countDialog().coerceIn(0, max)
            3 -> max
            else -> 1
        }

    private fun ProtectedAccess.view() {
        if (!hasKit(player)) return
        ifOpenMainSidePair(GearUi.main, GearUi.side)
        invTransmit(kit(player))
        invTransmit(inv)
        ifSetEvents(
            GearButtons.list,
            0 until 12 * kitContents.size,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
        )
        ifSetEvents(GearButtons.bag, inv.indices, IfEvent.Op1)
        for (c in GearButtons.counts + listOf(GearButtons.empty, GearButtons.fill)) ifSetEvents(
            c,
            -1..0,
            IfEvent.Op1,
        )
    }
}
