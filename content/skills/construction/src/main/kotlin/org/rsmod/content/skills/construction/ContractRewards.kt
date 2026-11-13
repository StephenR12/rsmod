package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.config.refs.invs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object ContractShop : InterfaceReferences() {
    val shop = find("construction_contract_shop")
}

internal object ContractShopButtons : ComponentReferences() {
    val confirm = find("construction_contract_shop:confirm")
}

internal object ContractItems : ObjReferences() {
    val crate = find("construction_supply_crate")
    val saw = find("wearable_saw")
    val leftSaw = find("wearable_saw_offhand")
    val sack = find("plank_sack")
    val plans = find("hosidius_blueprints")
    val outfit = listOf("hat", "shirt", "trousers", "boots").map { find("construction_$it") }
    val stock = listOf(crate, saw, sack, plans) + outfit
    val prices = listOf(25, 500, 350, 2000, 400, 800, 600, 200)
    val notedSupplies =
        listOf(
                "cloth",
                "limestonebrick",
                "plank_mahogany",
                "plank_oak",
                "softclay",
                "steel_bar",
                "plank_teak",
            )
            .map { find("cert_$it") }
}

private var Player.plankSackContents by intVarp(ContractVars.sack)

public class ContractRewards
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val invTypes: InvTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc5(HomeContractors.introduced.first()) {
            if (!player.members) return@onOpNpc5
            vars[varps.if1] = vars[ContractVars.points]
            ifOpenMainModal(ContractShop.shop)
            ifSetEvents(ContractShopButtons.confirm, 0..7, IfEvent.Op1)
        }
        onIfModalButton(ContractShopButtons.confirm) {
            if (it.op.slot == 1 && player.members) {
                val index =
                    listOf(4, 5, 6, 7, 2, 0, 1, 3).getOrNull(it.comsub) ?: return@onIfModalButton
                purchase(index)
                vars[varps.if1] = vars[ContractVars.points]
            }
        }
        onOpHeld3(ContractItems.saw) {
            invReplace(inv, ContractItems.saw, 1, ContractItems.leftSaw)
        }
        onOpHeld3(ContractItems.leftSaw) {
            invReplace(inv, ContractItems.leftSaw, 1, ContractItems.saw)
        }
        onOpBankside(ContractItems.sack) {
            if (player.members && ContractItems.sack in player.inv) {
                if (planks.any { it in player.inv }) fill(player, null) else emptyToBank(player)
            }
        }
        onOpHeld1(ContractItems.sack) { fill(player,null) }
        onOpHeld3(ContractItems.sack) {
            val amounts =
                planks.mapIndexed { n, p ->
                    "${sackCount(this,p)} ${listOf("regular","oak","teak","mahogany")[n]}"
                }
            mes("Your sack contains ${amounts.joinToString()} planks.")
        }
        onOpHeld4(ContractItems.sack) { empty() }
        for (p in planks) onOpHeldU(ContractItems.sack, p) { fill(player,p) }
        onOpHeld1(ContractItems.crate) { openCrate() }
        onOpHeld3(ContractItems.crate) {
            while (ContractItems.crate in inv && openCrate()) delay(1)
        }
        onOpHeld1(ContractItems.plans) {
            mes(
                "Give these blueprints to an estate agent to redecorate your house in the Hosidius style."
            )
        }
    }

    private fun ProtectedAccess.purchase(i: Int) {
        val obj = ContractItems.stock.getOrNull(i) ?: return
        val cost = ContractItems.prices[i]
        if (vars[ContractVars.points] < cost) {
            mes("You need $cost carpenter points for that reward.")
            return
        }
        if (obj == ContractItems.sack && (obj in inv || obj in bank)) {
            mes("You already have a plank sack.")
            return
        }
        if (invAdd(inv, obj).failure) {
            mes("Make some room in your inventory.")
            return
        }
        vars[ContractVars.points] -= cost
        mes("You purchase ${objTypes[obj].name.lowercase()}.")
    }

    private fun fill(player:Player, only: ObjType?) {
        if (!player.members) return
        val inv=player.inv
        var free = 28 - planks.sumOf { sackCount(player, it) }
        val items = linkedMapOf<ObjType, Int>()
        for (p in planks) {
            if (only != null && only != p) continue
            val amount = minOf(inv.count(objTypes[p]), free)
            if (amount > 0) {
                items[p] = amount
                free -= amount
            }
        }
        if (items.isEmpty()) {
            player.mes(
                if (free <= 0) "Your plank sack is full."
                else "You have no planks to put in the sack."
            )
            return
        }
        val paid =
            player.invTransaction(inv) {
                val bag = select(inv)
                for ((p, amount) in items) delete(bag, p.id, amount)
            }
        if (paid.success) {
            for ((p, amount) in items) adjustSack(player, p, amount)
            player.mes("You put the planks in your sack.")
        }
    }

    private fun emptyToBank(player:Player) {
        val bank=player.invMap.getOrPut(invTypes[invs.bank])
        val amounts=planks.associateWith { sackCount(player,it) }.filterValues { it>0 }
        if(amounts.isEmpty()){player.mes("Your plank sack is empty.");return}
        val reward=player.invTransaction(bank){
            val dst=select(bank)
            for((plank,amount) in amounts) add(dst,plank.id,amount)
        }
        if(reward.success) for ((plank,amount) in amounts)adjustSack(player,plank,-amount)
        else player.mes("You need more room in your bank.")
    }

    private fun ProtectedAccess.empty() {
        if (!player.members) return
        var spaces = inv.freeSpace()
        val items = linkedMapOf<ObjType, Int>()
        for (p in planks) {
            val amount = minOf(spaces, sackCount(this, p))
            if (amount > 0) {
                items[p] = amount
                spaces -= amount
            }
        }
        if (items.isEmpty()) {
            mes(
                if (spaces == 0) "Make some room in your inventory."
                else "Your plank sack is empty."
            )
            return
        }
        val reward =
            player.invTransaction(inv) {
                val bag = select(inv)
                for ((p, amount) in items) add(bag, p.id, amount)
            }
        if (reward.success) {
            for ((p, amount) in items) adjustSack(this, p, -amount)
            mes("You take the planks out of the sack.")
        }
    }

    private fun ProtectedAccess.openCrate(): Boolean {
        if (!player.members) return false
        var roll = random.of(144)
        for ((i, amounts) in crateWeights.withIndex()) {
            for ((n, weight) in amounts.second.withIndex()) {
                if (roll < weight) {
                    val item = ContractItems.notedSupplies[i]
                    val count = amounts.first + n
                    val reward =
                        player.invTransaction(inv) {
                            val bag = select(inv)
                            delete(bag, ContractItems.crate.id, 1)
                            add(bag, item.id, count)
                        }
                    if (reward.success)
                        mes("You find $count ${objTypes[item].name.lowercase()} in the crate.")
                    return reward.success
                }
                roll -= weight
            }
        }
        error("Supply crate weights must total 144.")
    }

    internal companion object {
        val planks = listOf(objs.plank, objs.oak_plank, objs.teak_plank, objs.mahogany_plank)
        private val crateWeights =
            listOf(
                9 to listOf(5, 1),
                9 to listOf(5, 1),
                6 to listOf(15, 3),
                28 to listOf(10, 15, 5),
                45 to listOf(5, 10, 10, 5),
                23 to listOf(5, 20, 5),
                15 to listOf(20, 4),
            )

        fun sackCount(access:ProtectedAccess,item:ObjType): Int = sackCount(access.player,item)

        private fun sackCount(player:Player, item: ObjType): Int {
            val i = planks.indexOfFirst { it.id == item.id }
            if (i < 0 || ContractItems.sack !in player.inv) return 0
            return (player.plankSackContents ushr (5 * i)) and 31
        }

        fun adjustSack(access: ProtectedAccess, item: ObjType, count:Int) { adjustSack(access.player,item,count) }

        private fun adjustSack(player:Player, item: ObjType, count: Int) {
            val shift = 5 * planks.indexOfFirst { it.id == item.id }
            val next = sackCount(player, item) + count
            require(next in 0..28)
            player.plankSackContents =
                (player.plankSackContents and (31 shl shift).inv()) or (next shl shift)
        }

        fun spend(access: ProtectedAccess, item: ObjType, count: Int): Boolean =
            with(access) {
                val loose = minOf(count, invTotal(inv, item))
                val fromSack = count - loose
                if (fromSack > sackCount(this, item)) return false
                if (loose > 0 && invDel(inv, item, loose).failure) return false
                if (fromSack > 0) adjustSack(this, item, -fromSack)
                true
            }

        fun outfitModifier(access: ProtectedAccess): Double {
            val weights = listOf(0.004, 0.008, 0.006, 0.002)
            val equipped = ContractItems.outfit.map { it in access.worn }
            return if (equipped.all { it }) 1.025
            else 1.0 + equipped.indices.sumOf { if (equipped[it]) weights[it] else 0.0 }
        }
    }
}
