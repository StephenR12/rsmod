package org.rsmod.content.skills.herblore.mixology

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.GroundItemTaken
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.interfaces.bank.BankProximity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object ReagentPouchInv : InvReferences() {
    val pouch = find("mixology_reagent_pouch")
}

internal object ReagentPouchSlots : InvBuilder() {
    init {
        build("mixology_reagent_pouch") {
            scope = InvScope.Perm
            size = 32
            stack = InvStackType.Always
            protect = false
        }
    }
}

public object ReagentPouchObjs : ObjReferences() {
    val closed = find("mm_secondary_pouch")
    val open = find("mm_secondary_pouch_open")
}

public class ReagentPouch
@Inject
constructor(
    private val invTypes: InvTypeList,
    private val items: ObjTypeList,
    private val banks: BankProximity,
) : PluginScript() {
    public val allowedIds =
        setOf(
            221,
            235,
            225,
            223,
            1975,
            239,
            2152,
            9736,
            231,
            2970,
            241,
            6049,
            245,
            3138,
            6051,
            6693,
            21975,
            592,
            9018,
            247,
            10111,
            27272,
            10937,
            6018,
            7650,
            29993,
            6016,
            11326,
            11324,
            30800,
        )

    public fun stored(player: Player): Inventory =
        player.invMap.getOrPut(invTypes[ReagentPouchInv.pouch])

    public fun owned(player: Player): Boolean =
        ReagentPouchObjs.closed in player.inv || ReagentPouchObjs.open in player.inv

    private fun eligible(player: Player) =
        player.members && player.statBase(stats.herblore) >= 81 && owned(player)

    public fun source(access: ProtectedAccess, item: ObjType): Inventory {
        val p = access.player
        return if (
            eligible(p) &&
                ReagentPouchObjs.open in p.inv &&
                item.id in allowedIds &&
                banks.nearBank(p.coords) &&
                item in stored(p)
        )
            stored(p)
        else p.inv
    }

    public fun quantity(access: ProtectedAccess, item: ObjType): Int {
        val own = access.inv.count(items[item])
        val source = source(access, item)
        return own + if (source === access.inv) 0 else source.count(items[item])
    }

    override fun ScriptContext.startup() {
        for (pouch in listOf(ReagentPouchObjs.closed, ReagentPouchObjs.open)) {
            onOpHeld1(pouch) { fill(this) }
            onOpHeld2(pouch) {
                if (eligible(player)) {
                    inv[it.slot] =
                        InvObj(
                            if (pouch == ReagentPouchObjs.closed) ReagentPouchObjs.open
                            else ReagentPouchObjs.closed
                        )
                    mes(
                        if (pouch == ReagentPouchObjs.closed) "You open the reagent pouch."
                        else "You close the reagent pouch."
                    )
                } else mes("You need level 81 Herblore to use the reagent pouch.")
            }
            onOpHeld3(pouch) { empty(this) }
            onOpBankside(pouch) {
                if (eligible(player)) {
                    if (player.inv.any { it?.id in allowedIds }) fillInventory(player)
                    else
                        player.invMoveAll(
                            stored(player),
                            player.invMap.getOrPut(invTypes[invs.bank]),
                        )
                }
            }
            onOpHeld4(pouch) {
                val contents = stored(player).filterNotNull()
                mes(
                    if (contents.isEmpty()) "Your reagent pouch is empty."
                    else contents.joinToString(", ") { "${items[it].name}: ${it.count}" }
                )
            }
            for (id in allowedIds) onOpHeldU(pouch, items.getValue(id)) {
                fill(this, it.secondSlot)
            }
        }
        onDestroyHeld {
            if (type.id == ReagentPouchObjs.closed.id || type.id == ReagentPouchObjs.open.id) {
                val pouch = stored(player)
                for (slot in pouch.indices) pouch[slot] = null
            }
        }
        onEvent<GroundItemTaken> {
            if (ReagentPouchObjs.open in player.inv && obj.type in allowedIds)
                storeAcquired(player, items.getValue(obj.type), obj.count)
        }
    }

    public fun storeAcquired(player: Player, item: ObjType, count: Int): Int {
        if (!eligible(player) || ReagentPouchObjs.open !in player.inv || item.id !in allowedIds)
            return 0
        val pouch = stored(player)
        var remaining = minOf(count, 26 - pouch.count(items[item]))
        var total = 0
        for (slot in player.inv.indices) {
            val obj = player.inv[slot] ?: continue
            if (obj.id != item.id || remaining <= 0) continue
            val amount = minOf(obj.count, remaining)
            val tx =
                player.invTransaction(player.inv, pouch) {
                    val from = select(player.inv)
                    val into = select(pouch)
                    delete(from, item.id, amount, slot = slot)
                    add(into, item.id, amount)
                }
            if (!tx.success) break
            remaining -= amount
            total += amount
        }
        return total
    }

    public fun fill(access: ProtectedAccess, selected: Int? = null): Int =
        with(access) {
            if (!eligible(player)) return 0
            val total = fillInventory(player, selected)
            if (total > 0) mes("You fill the reagent pouch.")
            total
        }

    private fun fillInventory(player: Player, selected: Int? = null): Int {
        val inv = player.inv
        val pouch = stored(player)
        var total = 0
        for (slot in inv.indices) {
            if (selected != null && slot != selected) continue
            val obj = inv[slot] ?: continue
            if (obj.id !in allowedIds) continue
            val count = minOf(obj.count, 26 - pouch.count(items[obj]))
            if (count <= 0) continue
            val tx =
                player.invTransaction(inv, pouch) {
                    val from = select(inv)
                    val into = select(pouch)
                    delete(from, obj.id, count, slot = slot)
                    add(into, obj.id, count)
                }
            if (tx.success) total += count
        }
        return total
    }

    public fun empty(access: ProtectedAccess): Int =
        with(access) {
            if (!eligible(player) || !banks.nearBank(coords)) {
                mes("You must be near a bank to empty the reagent pouch.")
                return 0
            }
            val pouch = stored(player)
            var total = 0
            for (slot in pouch.indices) {
                val item = pouch[slot] ?: continue
                val max =
                    if (items[item].stackable) item.count else minOf(item.count, inv.freeSpace())
                if (max <= 0) continue
                val tx =
                    player.invTransaction(inv, pouch) {
                        val from = select(pouch)
                        val into = select(inv)
                        delete(from, item.id, max, slot = slot)
                        add(into, item.id, max)
                    }
                if (tx.success) total += max
            }
            total
        }
}
