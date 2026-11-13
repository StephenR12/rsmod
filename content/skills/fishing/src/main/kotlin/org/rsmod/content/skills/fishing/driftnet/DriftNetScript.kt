package org.rsmod.content.skills.fishing.driftnet

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueInventory
import org.rsmod.api.clue.ClueTier
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invClear
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.content.interfaces.bank.deliverToBank
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DriftButtons : ComponentReferences() {
    val catch = find("fossil_driftnet:inv_layer")
    val take = find("fossil_driftnet:inv")
    val showBin = find("fossil_driftnet:bin")
    val showBank = find("fossil_driftnet:bank")
    val discard = find("fossil_driftnet:confirm_destroy")
    val bank = find("fossil_driftnet:confirm_bank")
    val bag = find("fossil_driftnet_sidestore:items")
    val stock = find("fossil_driftnet_store:store")
}

public class DriftNetScript
@Inject
internal constructor(
    private val cave: DriftNetCave,
    private val locs: LocTypeList,
    private val npcs: NpcTypeList,
    private val objects: ObjTypeList,
    private val clues: ClueInventory,
) : PluginScript() {
    private val nets
        get() = objects.getValue(21652)

    private val fee
        get() = objects.getValue(21555)

    override fun ScriptContext.startup() {
        onPlayerSoftTimer(DriftTimer.life) { cave.tick(player) }
        onPlayerLogin { cave.login(player) }
        onPlayerLogout { cave.logout(player) }
        onEvent<PlayerTeleportEvent.Start> {
            if (cave.room(player) != null) cave.leave(player, null)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (cave.room(player) != null) {
                cave.leave(player, null)
                safeDestination = DriftNetCave.ISLAND
            }
        }
        onOpNpc1(npcs.getValue(7783)) { access(false) }
        onOpNpc3(npcs.getValue(7783)) { access(false) }
        for (id in listOf(30958, 31844, 31845)) {
            onOpLoc1(locs.getValue(id)) { access(true) }
            onOpLoc2(locs.getValue(id)) { access(false) }
        }
        onOpLoc1(locs.getValue(30961)) {
            if (cave.room(player) != null) cave.leave(player, DriftNetCave.ENTRANCE)
        }
        fun port(p: org.rsmod.game.entity.Player) = p.coords.x < 3500
        for (id in listOf(7767, 8012, 8013)) {
            onOpNpc4(npcs.getValue(id)) {
                if (cave.questComplete(player))
                    telejump(if (port(player)) CAMP_LANDING else DIGSITE)
            }
            onOpNpc3(npcs.getValue(id)) {
                if (cave.questComplete(player)) telejump(if (port(player)) DECK else ISLAND_DECK)
            }
        }
        onOpNpc3(npcs.getValue(7768)) {
            if (cave.questComplete(player)) telejump(if (port(player)) DIGSITE else CAMP_LANDING)
        }
        for (id in listOf(7764, 7820)) onOpNpc3(npcs.getValue(id)) {
            if (cave.questComplete(player)) telejump(if (port(player)) CAMP_LANDING else DIGSITE)
        }
        for (id in listOf(30914, 30915, 30919)) onOpLoc1(locs.getValue(id)) { row(id) }
        onOpLoc2(locs.getValue(30919)) { cave.dive(player) }
        onOpLoc1(locs.getValue(30948)) {
            if (cave.questComplete(player)) cave.leave(player, DriftNetCave.ISLAND)
        }
        onOpHeld1(objects.getValue(21655)) {
            if (cave.room(player) != null || DriftNetCave.inWater(player.coords)) {
                if (invDel(inv, objects.getValue(21655)).success)
                    vars[DriftBits.oxygen] = (vars[DriftBits.oxygen] + 150).coerceAtMost(1000)
            }
        }
        onOpNpc1(npcs.getValue(7782)) { cave.chase(player, it.npc) }
        for (id in listOf(31433, 31434, 30952, 30953, 30954, 30955)) {
            onOpLoc1(locs.getValue(id)) { anchor(it.loc.coords, false) }
            onOpLoc2(locs.getValue(id)) { anchor(it.loc.coords, true) }
            onOpLocU(locs.getValue(id), nets) { anchor(it.loc.coords, false, setupOnly = true) }
        }
        onOpLoc1(locs.getValue(31843)) { openStore() }
        onOpLoc3(locs.getValue(31843)) { openStore() }
        onIfModalButton(DriftButtons.bag) { store(it.comsub, it.op.slot, it.obj?.id) }
        onIfModalButton(DriftButtons.stock) { withdraw(it.op.slot) }
        onIfModalButton(DriftButtons.catch) { e ->
            if (e.op.slot == 1 && cave.room(player) != null) take(e.comsub, e.obj?.id)
        }
        onIfModalButton(DriftButtons.take) { takeAll() }
        onIfModalButton(DriftButtons.discard) {
            if (cave.room(player) != null) {
                player.invClear(inv(DriftInv.harvest))
                ifClose()
            }
        }
        onIfModalButton(DriftButtons.bank) {
            if (cave.room(player) == null) return@onIfModalButton
            val stored = inv(DriftInv.harvest)
            if (stored.isEmpty()) return@onIfModalButton
            if (player.deliverToBank(bank, stored.filterNotNull(), fee, 5)) {
                stored.fillNulls()
                ifClose()
            } else mes("You need five numulites and room in your bank for the catch.")
        }
        onIfClose(DriftUi.harvest) { player.invMap[DriftInv.harvest]?.let(player::stopInvTransmit) }
    }

    private suspend fun ProtectedAccess.access(enter: Boolean) {
        if (!cave.eligible(player)) {
            mes("You need to complete Bone Voyage and have 47 Fishing and 44 Hunter.")
            return
        }
        if (!cave.permitted(player)) {
            val choice =
                menu(
                    "Access to the fishing cave",
                    false,
                    listOf("Today (200 numulites)", "Permanently (20,000 numulites)", "Leave"),
                )
            val cost =
                when (choice) {
                    0 -> 200
                    1 -> 20000
                    else -> return
                }
            if (!invDel(inv, fee, cost).success) {
                mes("You don't have enough numulites.")
                return
            }
            if (choice == 1) vars[DriftBits.life] = 1 else vars[DriftVars.day] = cave.today()
        }
        if (enter) cave.enter(player) else mes("You can now enter the drift net fishing cave.")
    }

    private suspend fun ProtectedAccess.row(id: Int) {
        if (!cave.questComplete(player)) return
        val routes = buildList {
            if (id != 30914) add("Museum Camp" to DriftNetCave.CAMP)
            if (id != 30915) add("North of the island" to DriftNetCave.NORTH)
            if (id != 30919 && (id == 30915 || vars[DriftVars.route] != 0))
                add("Row out to sea" to DriftNetCave.ISLAND)
        }
        val dest =
            routes
                .getOrNull(menu("Row to where?", false, routes.map { it.first } + "Cancel"))
                ?.second ?: return
        if (dest == DriftNetCave.ISLAND) vars[DriftVars.route] = 1
        telejump(dest)
    }

    private fun ProtectedAccess.openStore() {
        if (cave.room(player) == null) return
        ifOpenMainSidePair(DriftUi.store, DriftUi.side)
        invTransmit(inv)
        ifSetEvents(
            DriftButtons.stock,
            0..0,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op9,
            IfEvent.Op10,
        )
        ifSetEvents(
            DriftButtons.bag,
            inv.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op10,
        )
    }

    private suspend fun ProtectedAccess.count(op: Int, max: Int): Int =
        when (op) {
            1 -> 1
            2 -> 5
            3 -> countDialog()
            4,
            9 -> max
            else -> 0
        }.coerceIn(0, max)

    private suspend fun ProtectedAccess.store(slot: Int, op: Int, id: Int?) {
        if (cave.room(player) == null) return
        val item = inv[slot] ?: return
        if (item.id != id) return
        if (op == 10) {
            mes(objects[item].desc)
            return
        }
        if (item.id !in 21652..21653) {
            mes("Annette only stores drift nets.")
            return
        }
        val type = objects[item]
        val n =
            count(op, minOf(2000 - vars[DriftBits.stored], invTotal(inv, type)).coerceAtLeast(0))
        if (n > 0 && cave.room(player) != null && inv[slot] == item && invDel(inv, type, n).success)
            vars[DriftBits.stored] += n
    }

    private suspend fun ProtectedAccess.withdraw(op: Int) {
        if (cave.room(player) == null) return
        if (op == 10) {
            mes(objects[nets].desc)
            return
        }
        val n = count(op, vars[DriftBits.stored])
        if (n > 0 && cave.room(player) != null) {
            val moved =
                invAdd(inv, if (op == 9) objects.cert(nets) else nets, n, strict = false)
                    .completed()
            vars[DriftBits.stored] -= moved
        }
    }

    private fun ProtectedAccess.anchor(
        pos: CoordGrid,
        inspect: Boolean,
        setupOnly: Boolean = false,
    ) {
        val r = cave.room(player) ?: return
        val i = DriftBits.nets.indices.firstOrNull { r.anchor(it) == pos } ?: return
        val state = vars[DriftBits.nets[i]]
        val n = vars[DriftBits.fish[i]]
        if (inspect) {
            mes("You can see $n shoals in the net.")
            return
        }
        val pending = inv(DriftInv.harvest)
        if (!setupOnly && pending.isNotEmpty()) {
            showHarvest()
            return
        }
        when (state) {
            0 -> {
                if (invDel(inv, nets).success) {
                    vars[DriftBits.nets[i]] = 1
                    vars[DriftBits.fish[i]] = 0
                    cave.start(player)
                } else mes("You need a drift net to set up the anchors.")
            }
            1 -> if (!setupOnly && invAdd(inv, nets).success) vars[DriftBits.nets[i]] = 0
            else ->
                if (!setupOnly) {
                    var clueCount = 0
                    repeat(n.coerceIn(0, 10)) {
                        val reward = loot(clueCount)
                        if (reward != null) {
                            check(invAdd(pending, objects[reward], reward.count).success)
                            if (reward.id == 13649 || reward.id == ClueTier.Medium.scrollBox.id) clueCount++
                        }
                    }
                    vars[DriftBits.nets[i]] = 0
                    vars[DriftBits.fish[i]] = 0
                    showHarvest()
                }
        }
    }

    private fun ProtectedAccess.showHarvest() {
        val pending = inv(DriftInv.harvest)
        ifOpenMainModal(DriftUi.harvest)
        invTransmit(pending)
        interfaceInvInit(pending, DriftButtons.catch, 5, 2, op1 = "Take")
        ifSetEvents(DriftButtons.catch, pending.indices, IfEvent.Op1)
        for (c in
            listOf(
                DriftButtons.showBin,
                DriftButtons.showBank,
                DriftButtons.take,
                DriftButtons.discard,
                DriftButtons.bank,
            )) ifSetEvents(c, -1..0, IfEvent.Op1)
    }

    private fun ProtectedAccess.take(slot: Int, id: Int?) {
        val stored = inv(DriftInv.harvest)
        val item = stored[slot] ?: return
        if (id == item.id) invMoveFromSlot(stored, inv, slot, item.count, strict = false)
    }

    private fun ProtectedAccess.takeAll() {
        if (cave.room(player) == null) return
        val stored = inv(DriftInv.harvest)
        for (slot in stored.indices) take(slot, stored[slot]?.id)
        if (stored.isEmpty()) ifClose()
    }

    private fun ProtectedAccess.loot(clueCount: Int): InvObj? {
        if (random.of(25) == 0)
            return when (random.of(3500)) {
                in 0..539 -> InvObj(fee, random.of(5..14))
                in 540..569 -> InvObj(objects.getValue(21562))
                in 570..584 -> InvObj(objects.getValue(21564))
                in 585..596 -> InvObj(objects.getValue(21566))
                in 597..599 -> InvObj(objects.getValue(21568))
                else -> null
            }
        if (random.of(600) == 0) {
            val state = clues.state(player, ClueTier.Medium)
            val eligible = vars[org.rsmod.api.config.refs.varbits.vesseled_clues_disabled] == 0 &&
                clueCount + state.owned < state.capacity
            val item = when {
                !eligible -> objects.getValue(21655)
                state.xMarksComplete -> objects[ClueTier.Medium.scrollBox]
                else -> objects.getValue(13649)
            }
            return InvObj(item)
        }
        val max = 5 + (stat(stats.fishing) / 10 - 4).coerceIn(0, 5)
        return InvObj(objects.getValue(FISH[random.of(max)]))
    }

    private companion object {
        val DIGSITE = CoordGrid(3362, 3447)
        val CAMP_LANDING = CoordGrid(3720, 3800)
        val DECK = CoordGrid(3363, 3452, 1)
        val ISLAND_DECK = CoordGrid(3722, 3784, 1)
        val FISH = intArrayOf(407, 21655, 321, 327, 359, 377, 371, 383, 395, 389)
    }
}
