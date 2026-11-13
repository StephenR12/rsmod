package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.travelWithItem
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.diary.hasCompletedDiaryTask
import org.rsmod.api.player.diary.interruptDiaryKillSequence
import org.rsmod.api.player.diary.interruptDiaryProductionSequence
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object KourendRelicItems : ObjReferences() {
    val empty = find("xeric_talisman_empty")
    val charged = find("xeric_talisman")
    val fang = find("lizardman_fang")
    val tablet = find("raids_spellunlock_tablet")
    val memoirs = find("veos_kharedsts_memoirs")
    val pages = listOf("hos","pisc","shay","lova","arc").map { find("veos_memoirs_${it}_page") }
}
internal object KourendRelicBits : VarBitReferences() {
    val unlocked = listOf("hosidius","piscarilius","shayzien","lovakengj","arceuus").map { find("${it}_page") }
    val charges = find("veos_memoir_charges")
    val bankTarget = find("charges_xerics_talisman_quantity")
    val client = find("veos_progress")
    val divided = find("akd")
}
internal object KourendRelicVars : VarpReferences() { val cities = find("kourend_memoir_diary_cities"); val honour = find("xeric_honour_unlocked") }
internal object KourendRelicBuilds : VarpBuilder() {
    init { build("kourend_memoir_diary_cities") { transmitNever=true }; build("xeric_honour_unlocked") { transmitNever=true } }
}
internal object KourendRelicLocs : LocReferences() { val memorial = find("kourendwoodland_statue") }

public class KourendRelics : PluginScript() {
    private val bookTitles = listOf("Lunch by the Lancalliums","The Fisher's Flute","History and Hearsay","Jewellery of Jubilation","A Dark Disposition")
    private val cities = listOf(CoordGrid(1713,3612),CoordGrid(1804,3748),CoordGrid(1478,3576),CoordGrid(1545,3761),CoordGrid(1681,3747))
    private val sites = listOf("Lookout","Glade","Inferno","Heart","Honour")
    private val talismanDest = listOf(CoordGrid(1579,3530),CoordGrid(1752,3566),CoordGrid(1504,3815),CoordGrid(1644,3673),CoordGrid(1254,3560))

    override fun ScriptContext.startup() {
        with(KourendRelicItems) {
            for (talisman in listOf(empty,charged)) {
                onOpHeldU(talisman,fang) { chargeTalisman(it.firstSlot,it.secondSlot) }
                onOpHeldU(talisman,tablet) {
                    if (!player.members || vars[KourendRelicVars.honour] != 0) return@onOpHeldU
                    if (invDel(inv,tablet,1,it.secondSlot).success) {
                        vars[KourendRelicVars.honour] = 1
                        mes("Your talisman can now take you to Mount Quidamortem.")
                    }
                }
                onProtectedEvent<BankedObjEvents.ConfigureCharges>(talisman.id) {
                    if (player.members) vars[KourendRelicBits.bankTarget] = countDialog("How many charges should the talisman hold?").coerceIn(0,1000)
                }
                onProtectedEvent<BankedObjEvents.Autocharge>(talisman.id) {
                    val needed = vars[KourendRelicBits.bankTarget] - it.obj.vars
                    if (!player.members || needed <= 0) return@onProtectedEvent
                    val used=it.takeMaterial(fang,needed)
                    if (used > 0) inv[it.slot]=InvObj(charged,vars=it.obj.vars+used)
                    if (used<needed) mes("There aren't enough lizardman fangs in the bank.")
                }
            }
            onOpHeld3(empty) { mes("Your talisman is inert.") }
            onOpWorn2(empty) { mes("Your talisman is inert.") }
            onOpHeld4(empty) {
                if (!player.members || !choice2("Dismantle the inert talisman into 100 fangs.",true,"Keep it.",false)) return@onOpHeld4
                player.invTransaction(inv) {
                    val bag=select(inv)
                    delete(bag,empty.id,1,it.slot)
                    add(bag,fang.id,100)
                }
            }
            onOpHeld3(charged) {
                val dest=menu("Xeric's talisman",true,sites)
                if (dest in sites.indices) talismanTravel(inv,it.slot,dest)
            }
            onOpHeld4(charged) { mes("Your talisman has ${it.obj.vars} charges.") }
            onOpHeld5(charged) {
                val current = it.obj
                if (current.vars <= 0) return@onOpHeld5
                val result = player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag,charged.id,1,it.slot)
                    add(bag,empty.id,1,slot=it.slot)
                    add(bag,fang.id,current.vars)
                }
                if (!result.success) mes("You need space in your inventory for the fangs.")
            }
            onOpWorn2(charged) { talismanTravel(worn,it.slot,0) }
            onOpWorn3(charged) { talismanTravel(worn,it.slot,1) }
            onOpWorn4(charged) { talismanTravel(worn,it.slot,2) }
            onOpWorn5(charged) { talismanTravel(worn,it.slot,3) }
            onOpWorn6(charged) { talismanTravel(worn,it.slot,4) }
            onOpWorn7(charged) { mes("Your talisman has ${it.obj.vars} charges.") }

            onOpHeld1(memoirs) { reminisce(inv,it.slot) }
            onOpHeld1(objs.book_of_the_dead) { invEquip(it.slot) }
            onOpHeld2(objs.book_of_the_dead) { reminisce(inv,it.slot) }
            for (book in listOf(memoirs,objs.book_of_the_dead)) {
                for ((index,page) in pages.withIndex()) onOpHeldU(book,page) {
                    if (!player.members || vars[KourendRelicBits.unlocked[index]] != 0) return@onOpHeldU
                    if (invDel(inv,page,1,it.secondSlot).success) {
                        vars[KourendRelicBits.unlocked[index]]=1
                        vars[KourendRelicBits.charges]=minOf(capacity(book),vars[KourendRelicBits.charges]+20)
                        mes("You add the page to your book.")
                    }
                }
                onOpHeld4(book) { checkBook(book) }
                onOpWorn2(book) { bookTravel(worn,it.slot,0) }
                onOpWorn3(book) { bookTravel(worn,it.slot,1) }
                onOpWorn4(book) { bookTravel(worn,it.slot,2) }
                onOpWorn5(book) { bookTravel(worn,it.slot,3) }
                onOpWorn6(book) { bookTravel(worn,it.slot,4) }
                onOpWorn7(book) { checkBook(book) }
                onOpLocU(KourendRelicLocs.memorial,book) { rechargeBook(book) }
            }
            onOpLoc1(KourendRelicLocs.memorial) {
                val book = listOf(objs.book_of_the_dead,memoirs).firstOrNull { it in inv || it in worn }
                if (book != null) rechargeBook(book) else mes("The old memorial is covered in inscriptions.")
            }
            onEvent<HeldDropEvents.Destroy> {
                if (obj.id == objs.book_of_the_dead.id) VarPlayerIntMapSetter.set(player,KourendRelicBits.charges,0)
            }
        }
    }

    private fun ProtectedAccess.chargeTalisman(slot:Int,fangSlot:Int) {
        val current=inv[slot] ?: return
        val fangs=inv[fangSlot] ?: return
        if (!player.members || fangs.id != KourendRelicItems.fang.id) return
        val count=minOf(1000-current.vars,fangs.count)
        if (count > 0 && invDel(inv,KourendRelicItems.fang,count,fangSlot).success) {
            inv[slot]=InvObj(KourendRelicItems.charged,vars=current.vars+count)
            mes("You charge the talisman with $count fangs.")
        }
    }

    private suspend fun ProtectedAccess.talismanTravel(bag:Inventory,slot:Int,index:Int) {
        if (index==4 && vars[KourendRelicVars.honour]==0) { mes("You haven't unlocked Xeric's Honour."); return }
        val current=bag[slot] ?: return
        if (current.id != KourendRelicItems.charged.id || current.vars <= 0) return
        val success=travelWithItem(talismanDest[index]) {
            val live=bag[slot]
            if (live?.id != current.id || live.vars <= 0) false else {
                bag[slot]=if (live.vars == 1) InvObj(KourendRelicItems.empty) else live.copy(vars=live.vars-1)
                true
            }
        }
        if (success && index==3) player.completeDiaryTask(DiaryTask.TeleportXericsHeart)
    }

    private fun ProtectedAccess.capacity(book:ObjType) = KourendRelicBits.unlocked.count { vars[it] != 0 } * 20 + if (book == objs.book_of_the_dead) 150 else 0
    private fun ProtectedAccess.checkBook(book:ObjType) { mes("Your book holds ${vars[KourendRelicBits.charges]} of ${capacity(book)} charges.") }

    private suspend fun ProtectedAccess.rechargeBook(book:ObjType) {
        if (!player.members) return
        val needed=capacity(book)-vars[KourendRelicBits.charges]
        val runes=listOf(objs.law_rune,objs.body_rune,objs.mind_rune,objs.soul_rune)
        val available=minOf(needed,runes.minOf { invTotal(inv,it) })
        if (available <= 0) { mes("You need space for charges and one law, body, mind and soul rune per charge."); return }
        val count=countDialog("How many charges would you like to add?").coerceIn(0,available)
        if (count<=0 || book !in inv && book !in worn || count > capacity(book)-vars[KourendRelicBits.charges]) return
        val result=player.invTransaction(inv) { val bag=select(inv); for (rune in runes) delete(bag,rune.id,count) }
        if (result.success) { vars[KourendRelicBits.charges]+=count; statAdvance(stats.magic,count*10.0); checkBook(book) }
    }

    private suspend fun ProtectedAccess.reminisce(bag:Inventory,slot:Int) {
        val index = menu("Reminisce",true,bookTitles)
        if (index in cities.indices) bookTravel(bag,slot,index)
    }
    private suspend fun ProtectedAccess.bookTravel(bag:Inventory,slot:Int,index:Int) {
        val book = bag[slot] ?: return
        if (vars[KourendRelicBits.unlocked[index]] == 0 || vars[KourendRelicBits.charges] == 0) { mes("You need the corresponding page and a charged book."); return }
        val success=travelWithItem(cities[index]) {
            if (bag[slot]?.id!=book.id || vars[KourendRelicBits.charges]<=0) false else { vars[KourendRelicBits.charges]--; true }
        }
        if (!success || player.hasCompletedDiaryTask(DiaryTask.VisitFiveKourendCities)) return
        val old=vars[KourendRelicVars.cities]
        val visited=old or (1 shl index)
        if (old == visited) return
        player.interruptDiaryProductionSequence(DiaryTask.VisitFiveKourendCities)
        player.interruptDiaryKillSequence()
        vars[KourendRelicVars.cities]=visited
        if (visited == 31) player.completeDiaryTask(DiaryTask.VisitFiveKourendCities)
    }
}
