package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.StandardShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object GraceActors : NpcReferences() {
    val grace = find("rooftops_grace")
}

internal object GraceSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<GraceSpawns>("grace.toml")
    }
}

internal object GraceCurrency : CurrencyReferences() {
    val marks = find("marks_of_grace")
}

internal object GraceInvs : InvReferences() {
    val shop = find("graceful_clothing")
}

internal object GraceItems : ObjReferences() {
    val pack = find("pack_amylase")
    val crystals = find("amylase")
    val shop =
        listOf(
            objs.graceful_hood_inv,
            objs.graceful_top_inv,
            objs.graceful_legs_inv,
            objs.graceful_gloves_inv,
            objs.graceful_boots_inv,
            objs.graceful_cape_inv,
            pack,
        )
}

internal object GraceShopStock : InvBuilder() {
    init {
        build("graceful_clothing") {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            autoSize = true
            for (item in GraceItems.shop) stock +=
                stock(item, count = if (item == GraceItems.pack) 1000 else 100, restockCycles = 1)
        }
    }
}

internal object GraceInterface : InterfaceReferences() {
    val scroll = find("journalscroll")
}

internal object GraceComponents : ComponentReferences() {
    val title = find("journalscroll:title")
    val rows = (1..AgilityLap.entries.size).map { find("journalscroll:qj$it") }
}

public class Grace
@Inject
constructor(
    private val shops: Shops,
    private val operations: ShopOperationMap,
    private val restock: ShopRestockProcess,
    private val types: ObjTypeList,
) : PluginScript(), StandardShopOperations {
    override fun ScriptContext.startup() {
        operations.register(GraceCurrency.marks, this@Grace)
        onOpNpc1(GraceActors.grace) {
            when (
                menu(
                    "Grace",
                    true,
                    listOf(
                        "Trade marks of grace.",
                        "View my lap counts.",
                        "Change lap announcements.",
                    ),
                )
            ) {
                0 -> shop()
                1 -> viewLaps()
                2 -> toggle()
            }
        }
        onOpNpc3(GraceActors.grace) { shop() }
        onOpNpc4(GraceActors.grace) { viewLaps() }
        onOpNpc5(GraceActors.grace) { toggle() }
        onOpHeld1(GraceItems.pack) {
            if (!player.members) return@onOpHeld1
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, GraceItems.pack.id, 1)
                    add(bag, GraceItems.crystals.id, 100)
                }
            if (result.failure) mes("You need more inventory space.")
        }
    }

    private fun ProtectedAccess.shop() {
        if (player.members)
            shops.open(
                player,
                "Grace's Graceful Clothing",
                GraceInvs.shop,
                0.8,
                1.0,
                0.0,
                GraceCurrency.marks,
            )
    }

    private fun ProtectedAccess.viewLaps() {
        ifOpenMainModal(GraceInterface.scroll)
        runClientScript(6844)
        ifSetText(GraceComponents.title, "Agility Course Lap Counts")
        for ((i, course) in AgilityLap.entries.withIndex()) ifSetText(
            GraceComponents.rows[i],
            "${course.label}: ${vars[LapVars.counts.getValue(course)]}",
        )
        runClientScript(6845, 1, AgilityLap.entries.size)
    }

    private fun ProtectedAccess.toggle() {
        val off = vars[LapVars.noticesOff] == 0
        vars[LapVars.noticesOff] = if (off) 1 else 0
        mes(
            if (off) "Course lap announcements are now off."
            else "Course lap announcements are now on."
        )
    }

    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val item = shop.inv[slot] ?: return
        player.mes("${types[item].name} costs ${types[item].cost} marks of grace.")
    }

    override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
        val item = sideInv[slot] ?: return
        if (GraceItems.shop.none { it.id == item.id }) player.mes("Grace doesn't buy that item.")
        else
            player.mes(
                "Grace will buy ${types[item].name.lowercase()} for ${types[item].cost*4/5} marks of grace."
            )
    }

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        inv[slot]?.let { player.mes(types[it].desc) }
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val item = shop.inv[slot] ?: return
        if (!player.members || request <= 0 || GraceItems.shop.none { it.id == item.id }) return
        val cost = types[item].cost
        val marks = sideInv.count(types[AgilityObjs.mark])
        val count = minOf(request, item.count, marks / cost)
        val free = sideInv.freeSpace()
        val n = if (count * cost == marks && count <= free + 1) count else minOf(count, free)
        if (n <= 0) {
            player.mes(
                if (item.count <= 0) "Grace is out of stock."
                else if (marks < cost) "You need more marks of grace."
                else "You need more inventory space."
            )
            return
        }
        val moved =
            player.invTransaction(sideInv) {
                val bag = select(sideInv)
                val stock = select(shop.inv)
                delete(bag, AgilityObjs.mark.id, n * cost)
                delete(stock, item.id, n)
                add(bag, item.id, n)
            }
        if (moved.success) {
            if (shop.inv[slot] == null) shop.inv[slot] = InvObj(types[item], 0)
            restock += shop.inv
        }
    }

    override fun invSell(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val item = sideInv[slot] ?: return
        if (!player.members || request <= 0) return
        if (GraceItems.shop.none { it.id == item.id }) {
            player.mes("Grace doesn't buy that item.")
            return
        }
        val type = types[item]
        var n = minOf(request, sideInv.count(type))
        val payment = type.cost * 4 / 5
        n = minOf(n, (Int.MAX_VALUE - sideInv.count(types[AgilityObjs.mark])) / payment)
        if (n <= 0) return
        val moved =
            player.invTransaction(sideInv) {
                val bag = select(sideInv)
                val stock = select(shop.inv)
                delete(bag, item.id, n)
                add(bag, AgilityObjs.mark.id, n * payment)
                add(stock, item.id, n)
            }
        if (moved.success) restock += shop.inv
    }
}
