package org.rsmod.content.skills.mining.volcanic

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.death.DeathKeeps
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PetrifiedPete
@Inject
constructor(
    private val items: ObjTypeList,
    private val npcs: NpcTypeList,
    private val invs: InvTypeList,
    private val keeps: DeathKeeps,
    private val shops: Shops,
) : PluginScript() {
    // Native client enum 1779 includes notes, the capacity upgrade, and the four prospectors.
    private val stock =
        listOf(
            441,
            443,
            454,
            445,
            448,
            450,
            452,
            21543,
            21545,
            21622,
            21541,
            21539,
            21697,
            27693,
            29472,
            29474,
            29476,
            29478,
        )
    private val costs =
        listOf(
            30,
            55,
            60,
            150,
            150,
            300,
            855,
            70,
            70,
            40,
            200,
            10000,
            40000,
            4000,
            26000,
            39000,
            34000,
            21000,
        )

    private fun lost(p: Player) = p.invMap.getOrPut(invs[VolcanicInv.lost])

    internal fun holding(p: Player) = p.invMap[VolcanicInv.lost]?.isNotEmpty() == true

    internal fun eligible(p: Player) =
        p.members && p.miningLvl >= 50 && p.vars[VolcanicBits.quest] >= 50

    internal suspend fun pay(a: ProtectedAccess, amount: Int): Boolean =
        with(a) {
            if (!eligible(player)) {
                mes("You need level 50 Mining and access to Fossil Island.")
                return false
            }
            if (holding(player)) {
                mes("Retrieve your lost belongings first.")
                return false
            }
            if (
                vars[VolcanicVars.admission] == 2 ||
                    vars[VolcanicVars.admission] == 1 && amount == 30
            )
                return true
            if (!choice2("Pay $amount numulite.", true, "Keep my numulite.", false)) return false
            if (!invDel(inv, VolcanicItems.numulite, amount).success) {
                mes("You need $amount numulite.")
                return false
            }
            vars[VolcanicVars.admission] = if (amount == 3000) 2 else 1
            return true
        }

    override fun ScriptContext.startup() {
        val pete = npcs.getValue(7776)
        onOpNpc1(pete) {
            when (
                choice4(
                    "Pay for a visit.",
                    1,
                    "Buy permanent access.",
                    2,
                    "View the reward shop.",
                    3,
                    "Retrieve lost items.",
                    4,
                )
            ) {
                1 -> pay(this, 30)
                2 -> pay(this, 3000)
                3 -> shop()
                4 -> retrieve()
            }
        }
        onOpNpc3(pete) {
            pay(
                this,
                if (
                    choice2(
                        "Pay 30 numulite for one visit.",
                        true,
                        "Buy permanent access for 3,000.",
                        false,
                    )
                )
                    30
                else 3000,
            )
        }
        onOpNpc4(pete) { shop() }
        onOpNpc5(pete) { retrieve() }
        onIfModalButton(VolcanicButtons.stock) { e ->
            val index = e.comsub
            if (e.comsub < 0 || index !in stock.indices || !player.members) return@onIfModalButton
            val id = stock[index]
            val cost = costs[index]
            if (e.obj != null && e.obj?.id != id) return@onIfModalButton
            val qty =
                when (e.op.slot) {
                    1 -> {
                        mes("${items.getValue(id).name} costs $cost points.")
                        return@onIfModalButton
                    }
                    2 -> 1
                    3 -> 5
                    4 -> 10
                    5 -> 50
                    6 -> countDialog()
                    else -> 0
                }
            if (qty <= 0) return@onIfModalButton
            val single = index >= 11 && index != 13
            if (
                id == 21539 && vars[VolcanicBits.large] != 0 ||
                    id == 21697 && vars[VolcanicBits.soil] != 0
            ) {
                mes("You have already unlocked this reward.")
                return@onIfModalButton
            }
            val count = minOf(if (single) 1 else qty, vars[VolcanicVars.points] / cost)
            if (count < 1) {
                mes("You don't have enough Volcanic Mine points.")
                return@onIfModalButton
            }
            val delivered =
                if (id == 21539) {
                    vars[VolcanicBits.large] = 1
                    1
                } else invAdd(inv, items.getValue(id), count, strict = false).completed()
            if (delivered > 0) {
                vars[VolcanicVars.points] -= cost * delivered
                vars[varps.if1] = vars[VolcanicVars.points]
            } else mes("Make room for the reward.")
        }
        onOpHeld1(items.getValue(21697)) {
            if (
                player.members &&
                    vars[VolcanicBits.soil] == 0 &&
                    invDel(inv, items.getValue(21697), 1, it.slot).success
            ) {
                vars[VolcanicBits.soil] = 1
                mes("You learn how to use volcanic ash with Fertile Soil.")
            }
        }
        onOpHeld1(items.getValue(21541)) {
            if (
                player.members &&
                    vars[VolcanicBits.quest] >= 50 &&
                    !player.isTeleportBlocked() &&
                    invDel(inv, items.getValue(21541), 1, it.slot).success
            )
                teleport(entrance)
        }
        onOpHeld1(items.getValue(27693)) { openPack(it.slot) }
        val broken = items.getValue(27695)
        onOpHeld2(broken) { mes("Nurmof in the Dwarven Mine can repair this pickaxe.") }
        onOpNpcU(pete, broken) { mes("Nurmof in the Dwarven Mine repairs broken pickaxes.") }
        onOpNpcU(npcs.getValue(8686), broken) { repair(it.invSlot) }
        onOpNpc3(npcs.getValue(8686)) { pickaxes() }
        onOpNpc1(npcs.getValue(8686)) {
            val slot = inv.indexOfFirst { it?.id == broken.id }
            if (slot >= 0) repair(slot) else pickaxes()
        }
        onEvent<PlayerDeathEvent.Unsafe> {
            // A second unsafe death destroys the items retained from the previous one.
            val store = player.invMap[VolcanicInv.lost]
            if (store != null && store.isNotEmpty()) player.invClear(store)
            VarPlayerIntMapSetter.set(player, VolcanicVars.paid, 0)
            if (inMine(player.coords)) holdDeathItems(player)
        }
    }

    private fun ProtectedAccess.pickaxes() {
        shops.open(player, "Nurmof's Pickaxe Shop.", VolcanicInv.picks, 60.0, 100.0, 2.0)
    }

    private fun ProtectedAccess.shop() {
        if (!player.members) return
        vars[varps.if1] = vars[VolcanicVars.points]
        ifOpenMainModal(VolcanicUi.shop)
        ifSetEvents(
            VolcanicButtons.stock,
            stock.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op10,
        )
    }

    private suspend fun ProtectedAccess.repair(slot: Int) {
        if (
            !player.members ||
                !choice2("Pay 2,500,000 coins to repair the pickaxe.", true, "Leave.", false)
        )
            return
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, 27695, 1, slot)
                delete(bag, objs.coins.id, 2500000)
                add(bag, objs.dragon_pickaxe.id, 1)
            }
        if (!tx.success) mes("You need the broken pickaxe and 2,500,000 coins.")
    }

    private fun ProtectedAccess.openPack(slot: Int) {
        if (!player.members || inv[slot]?.id != 27693) return
        // The published pack observations bound the value and individual quantities.
        val draws = mutableMapOf<Int, Int>()
        var budget = 3300
        val limits = listOf(71, 10, 10, 8, 7, 6, 3)
        for (i in 6 downTo 1) {
            val max = minOf(limits[i], budget / costs[i])
            val count =
                if (max > 0 && random.of(100) < listOf(100, 90, 90, 90, 85, 55, 23)[i])
                    random.of(max + 1)
                else 0
            if (count > 0) {
                draws[stock[i]] = count
                budget -= count * costs[i]
            }
        }
        if (budget >= 30) draws[stock[0]] = minOf(71, maxOf(1, random.of(budget / 30 + 1)))
        if (random.of(100) == 0) draws[27695] = 1
        val tx =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, 27693, 1, slot)
                for ((id, count) in draws) add(bag, id, count)
            }
        if (!tx.success) mes("Make room for the contents of the ore pack.")
    }

    private fun holdDeathItems(p: Player) {
        val count =
            (if (p.skullIcon == null) 3 else 0) + (if (p.vars[varbits.protect_item] != 0) 1 else 0)
        val keep =
            keeps
                .kept(p, count)
                .groupBy { it.id to it.vars }
                .mapValues { e -> e.value.sumOf { it.count } }
                .toMutableMap()
        val store = lost(p)
        for (source in listOf(p.inv, p.worn)) for (slot in source.indices) {
            val item = source[slot] ?: continue
            if (items[item].param(params.bond_item)) continue
            val key = item.id to item.vars
            val saved = minOf(item.count, keep.getOrDefault(key, 0))
            keep[key] = keep.getOrDefault(key, 0) - saved
            if (item.count > saved) p.invTransfer(source, slot, item.count - saved, store)
        }
    }

    private fun ProtectedAccess.retrieve() {
        val store = lost(player)
        if (store.isEmpty()) {
            mes("Pete has no lost belongings for you.")
            return
        }
        if (vars[VolcanicVars.paid] == 0) {
            val fromInv = minOf(150, invTotal(inv, VolcanicItems.numulite))
            val tx =
                player.invTransaction(inv, store) {
                    val bag = select(inv)
                    val saved = select(store)
                    if (fromInv > 0) delete(bag, VolcanicItems.numulite.id, fromInv)
                    if (fromInv < 150) delete(saved, VolcanicItems.numulite.id, 150 - fromInv)
                }
            if (!tx.success) {
                mes("Pete charges 150 numulite to retrieve your belongings.")
                return
            }
            vars[VolcanicVars.paid] = 1
        }
        player.invMoveAll(store, inv)
        if (store.isNotEmpty()) mes("Make more room to collect the rest of your belongings.")
        else vars[VolcanicVars.paid] = 0
    }
}
