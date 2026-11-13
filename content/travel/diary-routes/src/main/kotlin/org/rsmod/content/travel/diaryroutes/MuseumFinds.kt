package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MuseumLocs : LocReferences() {
    val gate = find("vm_barrier_guard_gate")
    val rack = find("vm_digsite_tools")
    val rocks = listOf("rock_pile","specimen_pile_lft","specimen_pile_rht").map { find("vm_digsite_wheelbarrow_$it") }
    val tables = listOf(find("vm_specimen_table1"),find("vm_specimen_table2"))
    val cases = listOf("saradomin_symbol_old","saradomin_symbol_ancient","coin_saranthium","coin_senntisten","pottery").map { find("vm_digsite_finds_${it}_multi") }
    val crate = find("vm_crate")
}
internal object MuseumItems : ObjReferences() {
    val tools = listOf("rockpick","trowel","vm_specimen_brush","leather_gloves","leather_boots").map { find(it) }
    val find = find("vm_digsite_find_unclean")
    val necklace = find("necklace_of_digsite")
    val exhibit = listOf("vm_symbol_old_inv","vm_symbol_ancient_inv","vm_saranthium_inv","vm_senntisten_inv","vm_pottery_inv").map { find(it) }
    val common = listOf("vm_pottery_inv","vm_jewllery_inv","vm_vase_inv","vm_arrow_heads_inv").map { find(it) }
    val junk = listOf("digsitearrow","digsitearrow","digsiteglass","digsiteglass","iron_dagger","iron_dagger","uncut_jade","bones","coins").map { find(it) }
    data class Prize(val item: ObjType, val weight:Int, val amount:IntRange)
    private fun prize(key:String,weight:Int,amount:IntRange=1..1) = Prize(find(key),weight,amount)
    val crate = listOf(
        prize("coins",19,17..69), prize("bowl_empty",21), prize("pot_empty",21), prize("xbows_crossbow_bolts_iron",10,1..10),
        prize("iron_dagger",10), prize("xbows_crossbow_limbs_bronze",10), prize("xbows_crossbow_stock_wood",10),
        prize("tin_ore",10), prize("copper_ore",10), prize("iron_ore",7), prize("coal",10), prize("mithril_ore",5),
        prize("iron_knife",10), prize("iron_dart",10,1..50), prize("iron_arrowheads",10,1..49),
        prize("uncut_jade",5), prize("uncut_opal",5), prize("big_bones",10), prize("vm_reward_digsite",2)
    )
}
internal object ExhibitBits : VarBitReferences() {
    val display = listOf("symbol_old","symbol_anc","coin_sara","coin_senn","pottery").map { find("vm_digsite_$it") }
}
internal object MuseumNpcs : NpcReferences() {
    val archaeologists = (1..6).map { find("vm_archaeologist${it}_npc_dummy") }
}
internal object MuseumSeqs : SeqReferences() { val brush = find("vm_player_use_specimen_brush") }

public class MuseumFinds : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(MuseumLocs.gate) {
            if (coords.z >= 3447 && !canClean()) return@onOpLoc1
            teleport(CoordGrid(3261,if (coords.z >= 3447) 3446 else 3447))
        }
        for (rocks in MuseumLocs.rocks) onOpLoc1(rocks) {
            if (it.loc.x !in 3263..3265 || it.loc.z != 3446 || !canClean()) return@onOpLoc1
            if (!invAdd(inv,MuseumItems.find).success) mes("You need room for the find.")
        }
        onOpLoc1(MuseumLocs.rack) {
            if (!canClean()) return@onOpLoc1
            for (tool in MuseumItems.tools) {
                if (tool !in inv && tool !in worn) invAdd(inv,tool)
            }
        }
        for (table in MuseumLocs.tables) {
            onOpLoc1(table) { clean() }
            onOpLocU(table,MuseumItems.find) { clean() }
        }
        for (archaeologist in MuseumNpcs.archaeologists) {
            onOpNpc1(archaeologist) { discussFind() }
            onOpNpcU(archaeologist,MuseumItems.necklace) { teachEnchantment() }
            onOpNpcU(archaeologist,MuseumItems.common[1]) {
                if (canClean()) exchange(it.invSlot,MuseumItems.common[1],objs.coins,5)
            }
        }
        for ((index,item) in MuseumItems.exhibit.withIndex()) onOpLocU(MuseumLocs.cases[index],item) {
            if (!canClean() || vars[ExhibitBits.display[index]] != 0) return@onOpLocU
            if (invDel(inv,item,invTotal(inv,item)).success) {
                val stored = invTotal(bank,item)
                if (stored > 0) invDel(bank,item,stored)
                vars[ExhibitBits.display[index]] = 1
                vars[MuseumBits.kudos] += 10
                mes("You place the artefact in its display case and earn 10 kudos.")
            }
        }
        onOpLoc1(MuseumLocs.crate) { depositFinds(null) }
        for (item in MuseumItems.common) onOpLocU(MuseumLocs.crate,item) { depositFinds(item) }
    }

    private fun ProtectedAccess.canClean():Boolean {
        if (player.members && vars[ArchaeologyVars.digsite] >= 9) return true
        mes("Complete The Dig Site to help clean specimens.")
        return false
    }
    private suspend fun ProtectedAccess.clean() {
        if (!canClean()) return
        if (MuseumItems.tools.take(3).any { it !in inv } || MuseumItems.tools.drop(3).any { it !in worn }) {
            mes("Wear leather boots and gloves and bring a rock pick, trowel and specimen brush.")
            return
        }
        while (true) {
            val slot = inv.indexOfFirst { it?.id == MuseumItems.find.id }
            if (slot < 0) break
            anim(MuseumSeqs.brush)
            delay(3)
            if (inv[slot]?.id != MuseumItems.find.id) break
            if (random.of(100) < 14) {
                invDel(inv,MuseumItems.find,1,slot)
                mes("The find crumbles to dust in your hands.")
            } else {
                val (item,count) = uncover()
                if (!exchange(slot,MuseumItems.find,item,count)) break
            }
        }
    }
    private fun ProtectedAccess.uncover():Pair<ObjType,Int> {
        val roll = random.of(51)
        if (roll >= 13) return MuseumItems.common[random.of(4)] to 1
        if (roll == 12) {
            if (vars[varbits.vm_necklace] == 0 && MuseumItems.necklace !in inv) return MuseumItems.necklace to 1
        } else {
            val index = roll / 3
            val item = MuseumItems.exhibit[index]
            if (vars[ExhibitBits.display[index]] == 0 && item !in inv) return item to 1
        }
        val item = MuseumItems.junk[random.of(MuseumItems.junk.size)]
        return item to if (item == objs.coins) 5 else 1
    }
    private fun ProtectedAccess.discussFind() {
        if (!canClean()) return
        if (MuseumItems.necklace in inv) teachEnchantment()
        else if (MuseumItems.exhibit.any { it in inv }) mes("Use your artefact on its matching display case.")
        else mes("Clean finds at a specimen table. Put common finds in the storage crate.")
    }
    private fun ProtectedAccess.teachEnchantment() {
        if (!canClean() || MuseumItems.necklace !in inv) return
        for (bag in listOf(inv,bank)) {
            val count = invTotal(bag,MuseumItems.necklace)
            if (count > 0) invDel(bag,MuseumItems.necklace,count)
        }
        vars[varbits.vm_necklace] = 1
        mes("The archaeologist teaches you how to enchant a ruby necklace into a digsite pendant.")
    }
    private fun ProtectedAccess.depositFinds(only:ObjType?) {
        if (!canClean()) return
        for (item in MuseumItems.common) {
            if (only != null && only != item) continue
            while (true) {
                val slot = inv.indexOfFirst { it?.id == item.id }
                if (slot < 0) break
                var roll = random.of(MuseumItems.crate.sumOf { it.weight })
                val prize = MuseumItems.crate.first { roll -= it.weight; roll < 0 }
                if (!exchange(slot,item,prize.item,random.of(prize.amount))) return
            }
        }
    }
    private fun ProtectedAccess.exchange(slot:Int,from:ObjType,to:ObjType,count:Int) =
        player.invTransaction(inv) {
            val bag = select(inv)
            delete(bag,from.id,1,slot)
            add(bag,to.id,count)
        }.success
}
