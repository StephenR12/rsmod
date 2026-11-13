package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onApNpc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SawmillNpcs : NpcReferences() {
    val operators =
        listOf(
            find("poh_sawmill_opp"),
            find("prif_sawmill_operator"),
            find("auburn_sawmill_operator"),
        )
}

internal object SawmillInvs : InvReferences() {
    val supplies = find("poh_sawmill_shop")
}

internal object SawmillObjs : ObjReferences() {
    val cloth = find("cloth")
}

internal object SawmillStock : InvEditor() {
    init {
        edit(SawmillInvs.supplies) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(StashObjs.saw, count = 1000, restockCycles = 10)
            for (obj in
                listOf(SawmillObjs.cloth, objs.bronze_nails, objs.iron_nails, objs.steel_nails)) {
                stock += stock(obj, count = 1000, restockCycles = 100)
            }
        }
    }
}

public class Sawmill @Inject constructor(private val shops: Shops) : PluginScript() {
    private data class Product(val log: ObjType, val plank: ObjType, val cost: Int)

    private val products =
        listOf(
            Product(objs.logs, objs.plank, 100),
            Product(objs.oak_logs, objs.oak_plank, 250),
            Product(objs.teak_logs, objs.teak_plank, 500),
            Product(objs.mahogany_logs, objs.mahogany_plank, 1500),
        )

    override fun ScriptContext.startup() {
        for (operator in SawmillNpcs.operators) {
            // The Lumber Yard sawmill operator serves players across the counter.
            onApNpc3(operator) { if (isWithinApRange(it.npc, 2)) buyPlanks(it.npc) }
            onApNpc4(operator) { if (isWithinApRange(it.npc, 2)) supplies() }
            onOpNpc3(operator) { buyPlanks(it.npc) }
            onOpNpc4(operator) { supplies() }
            onOpNpc1(operator) {
                if (
                    choice2(
                        "Turn my logs into planks.",
                        true,
                        "Trade construction supplies.",
                        false,
                    )
                )
                    buyPlanks(it.npc)
                else supplies()
            }
        }
    }

    private fun ProtectedAccess.supplies() {
        if (player.members)
            shops.open(player, "Construction supplies", SawmillInvs.supplies, 50.0, 130.0, 0.0)
    }

    private suspend fun ProtectedAccess.buyPlanks(operator: Npc) {
        if (!player.members) return
        val input =
            skillMulti(
                "Choose planks to buy (100 / 250 / 500 / 1,500 coins each).",
                products.map { it.plank },
                28,
                28,
                0,
            )
        val buttons =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
            )
        val product = products[buttons.indexOfFirst { it.isType(input.component) }]
        val coins = invTotal(inv, objs.coins)
        val count = minOf(input.subcomponent, invTotal(inv, product.log), coins / product.cost)
        if (count == 0) {
            mes("You need a suitable log and ${product.cost} coins for each plank.")
            return
        }
        val fee = product.cost * count
        val vouchers = invTotal(inv, objs.sawmill_voucher)
        val remainingSlots = inv.freeSpace() + if (fee == coins) 1 else 0
        val bonus =
            minOf(vouchers, count).let {
                if (it == vouchers && it <= remainingSlots + 1) it else minOf(it, remainingSlots)
            }
        val result =
            player.invTransaction(inv) {
                val inventory = select(inv)
                delete(inventory, product.log.id, count)
                delete(inventory, objs.coins.id, fee)
                if (bonus > 0) delete(inventory, objs.sawmill_voucher.id, bonus)
                add(inventory, product.plank.id, count + bonus)
            }
        if (result.success) {
            mes("The sawmill operator turns your logs into planks.")
            if (operator.x in 3298..3308 && operator.z in 3489..3500) {
                if (product.plank == objs.plank) player.completeDiaryTask(DiaryTask.SawmillPlank)
                if (product.plank == objs.mahogany_plank && count >= 20)
                    player.completeDiaryTask(DiaryTask.Sawmill20Mahogany)
            }
        }
    }
}
