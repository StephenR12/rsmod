package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.currencies
import org.rsmod.api.config.refs.jingles
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.ObjTradeEvents
import org.rsmod.api.player.midiJingle
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.ShopOperations
import org.rsmod.api.shops.operation.StandardGpShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.skillcapes.SkillcapeSale
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AuburyNpcs : NpcReferences() {
    val variants = listOf(find("aubury_2op"), find("aubury_3op"))
}

internal object AuburyInventories : InvReferences() {
    val standard = find("runeshop")
    val untrimmed = find("runeshop_skillcape")
    val trimmed = find("runeshop_skillcape_trimmed")
}

internal object AuburyCurrency : CurrencyReferences() {
    val capeShop = find("aubury_cape_shop_gp")
}

internal object AuburyPacks : ObjReferences() {
    val items =
        listOf("fire", "water", "air", "earth", "mind", "chaos").map { find("pack_${it}rune") }
    val runes
        get() =
            listOf(
                objs.fire_rune,
                objs.water_rune,
                objs.air_rune,
                objs.earth_rune,
                objs.mind_rune,
                objs.chaos_rune,
            )
}

internal object AuburyStock : InvEditor() {
    init {
        val shelves =
            mapOf(
                AuburyInventories.standard to null,
                AuburyInventories.untrimmed to objs.runecrafting_skillcape,
                AuburyInventories.trimmed to objs.runecrafting_skillcape_t,
            )
        for ((shelf, cape) in shelves) edit(shelf) {
            stack = InvStackType.Always
            scope = InvScope.Shared
            autoSize = true
            for (rune in
                listOf(
                    objs.air_rune,
                    objs.fire_rune,
                    objs.water_rune,
                    objs.earth_rune,
                    objs.mind_rune,
                    objs.body_rune,
                )) stock += stock(rune, count = 5000, restockCycles = 10)
            stock += stock(objs.chaos_rune, count = 250, restockCycles = 10)
            stock += stock(objs.death_rune, count = 250, restockCycles = 15)
            for ((i, pack) in AuburyPacks.items.withIndex()) {
                val quantity = listOf(80, 80, 80, 80, 40, 35)[i]
                stock += stock(pack, count = quantity, restockCycles = 10)
            }
            if (cape != null) stock += stock(cape, count = 1, restockCycles = 1)
        }
    }
}

public class AuburyShop
@Inject
constructor(
    private val shops: Shops,
    private val operations: ShopOperationMap,
    private val gp: StandardGpShopOperations,
    private val restock: ShopRestockProcess,
    private val types: ObjTypeList,
    private val events: EventBus,
) : PluginScript(), ShopOperations by gp {
    override fun ScriptContext.startup() {
        operations.register(AuburyCurrency.capeShop, this@AuburyShop)
        for (npc in AuburyNpcs.variants) {
            onOpNpc3(npc) { openShop(player) }
            onOpNpc1(npc) {
                if (choice2("I'd like to buy some runes.", true, "No thanks.", false))
                    openShop(player)
            }
        }
        for ((index, pack) in AuburyPacks.items.withIndex()) {
            val rune = AuburyPacks.runes[index]
            onOpHeld1(pack) {
                val tx =
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, pack.id, 1, it.slot)
                        add(bag, rune.id, 100)
                    }
                if (!tx.success) mes("You don't have enough room for the runes.")
            }
        }
    }

    private fun openShop(player: Player) {
        val stock =
            when {
                !player.members || player.statBase(stats.runecrafting) < 99 ->
                    AuburyInventories.standard
                SkillcapeSale.isTrimmed(player) -> AuburyInventories.trimmed
                else -> AuburyInventories.untrimmed
            }
        shops.open(
            player,
            "Aubury's Rune Shop.",
            stock,
            60.0,
            100.0,
            0.1,
            if (stock == AuburyInventories.standard) currencies.standard_gp
            else AuburyCurrency.capeShop,
        )
    }

    override fun shopInvOp(
        player: Player,
        sideInv: Inventory,
        shop: Shop,
        slot: Int,
        op: IfButtonOp,
    ) {
        val item = shop.inv[slot] ?: return
        if (
            item.id != objs.runecrafting_skillcape.id && item.id != objs.runecrafting_skillcape_t.id
        ) {
            gp.shopInvOp(player, sideInv, shop, slot, op)
            return
        }
        if (op == IfButtonOp.Op1) {
            player.mes("The Runecraft cape and hood cost 99,000 coins.")
            return
        }
        if (op !in listOf(IfButtonOp.Op2, IfButtonOp.Op3, IfButtonOp.Op4, IfButtonOp.Op5)) {
            gp.shopInvOp(player, sideInv, shop, slot, op)
            return
        }
        if (!player.members || player.statBase(stats.runecrafting) < 99) return
        if (item.count == 0) {
            player.mes("That item is currently out of stock.")
            return
        }
        val tx =
            player.invTransaction(sideInv) {
                val bag = select(sideInv)
                val shelf = select(shop.inv)
                delete(bag, objs.coins.id, 99_000)
                delete(shelf, item.id, 1, slot)
                add(bag, item.id, 1)
                add(bag, objs.runecrafting_hood.id, 1)
            }
        if (!tx.success) {
            player.mes("You'll need 99,000 coins and room for the cape and hood.")
            return
        }
        if (shop.inv[slot] == null) shop.inv[slot] = InvObj(types[item], 0)
        restock += shop.inv
        player.midiJingle(jingles.cape_of_accomplishment)
        events.publish(ObjTradeEvents.Purchased(player, shop.inv.type, types[item], 1))
    }
}
