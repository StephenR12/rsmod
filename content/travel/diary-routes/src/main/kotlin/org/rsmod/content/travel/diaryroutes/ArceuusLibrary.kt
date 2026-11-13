package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LibraryNpcs:NpcReferences() {
    val customers = (2..4).map { find("arceuus_library_customer_$it") }
    val biblia = find("arceuus_library_assistant")
}
internal object LibraryStairs : LocReferences() {
    val up = listOf("", "_left", "_right").map { find("archeuus_stairs_lower$it") }
    val down = listOf("", "_left", "_right").map { find("archeuus_stairs_upper$it") }
}
internal object LibraryVars:VarpReferences() { val request = find("arceuus_library_request") }
internal object LibraryVarBuilds:VarpBuilder() { init { build("arceuus_library_request") { transmitNever=true } } }
internal object LibrarySequences:SeqReferences() { val search = find("human_pickuptable") }

public class ArceuusLibrary @Inject constructor(random:GameRandom,private val locs:LocTypeList,private val items:ObjTypeList):PluginScript() {
    private val layout = LibraryLayout(random)
    override fun ScriptContext.startup() {
        for (stairs in LibraryStairs.up) onOpLoc1(stairs) { climb(it.loc.coords,it.loc.angleId,true) }
        for (stairs in LibraryStairs.down) onOpLoc1(stairs) { climb(it.loc.coords,it.loc.angleId,false) }
        for (type in locs.values.filter { it.internalName?.startsWith("archeuus_library_bookcase_") == true && it.id in 27991..28350 }) {
            onOpLoc1(type) { search(it.loc.coords) }
        }
        for ((index,npc) in LibraryNpcs.customers.withIndex()) onOpNpc1(npc) { assist(index) }
        onOpNpc1(LibraryNpcs.biblia) {
            val request = vars[LibraryVars.request]
            if (request == 0) { mes("Ask a customer in the library which book they would like."); return@onOpNpc1 }
            val book = (request-1) % LibraryBooks.texts.size
            val shelf = layout.shelfFor(book,epochMinute())
            val north = shelf.z > if (shelf.level == 0) 3813 else 3815
            val west = shelf.x < if (shelf.level == 0) 1627 else 1625
            val bay = when { north && west -> "north-western bay"; north -> "north-eastern bay"; west -> "south-western bay"; else -> "central area" }
            val floor = listOf("bottom","middle","top")[shelf.level]
            mes("Biblia suggests looking on the $floor floor, in the $bay.")
        }
        onOpHeld1(LibraryBooks.reward) {
            if (!player.members) return@onOpHeld1
            val skill = menu("Choose a skill",true,listOf("Magic","Runecraft","Cancel"))
            val stat = when(skill) { 0 -> stats.magic; 1 -> stats.runecrafting; else -> return@onOpHeld1 }
            if (invDel(inv,LibraryBooks.reward,1,it.slot).success) {
                statAdvance(stat,(statBase(stat) * if (skill==0) 15 else 5).toDouble())
            }
        }
    }

    private fun ProtectedAccess.climb(origin:CoordGrid,angle:Int,up:Boolean) {
        val destination = when (angle) {
            0 -> origin.translate(0,if (up) 6 else -3)
            1 -> origin.translate(if (up) 6 else -3,0)
            2 -> origin.translate(0,if (up) -4 else 6)
            else -> origin.translate(if (up) -4 else 6,0)
        }
        teleport(CoordGrid(destination.x,destination.z,origin.level + if (up) 1 else -1))
    }

    private fun ProtectedAccess.assist(customer:Int) {
        if (!player.members) return
        val total = LibraryBooks.texts.size
        val request = vars[LibraryVars.request] - 1
        if (request >= 0) {
            val book = LibraryBooks.texts[request%total]
            if (request/total != customer) {
                mes("You are still looking for ${items[book].name} for another customer.")
                return
            }
            if (book !in inv) { mes("I'm looking for ${items[book].name}."); return }
            if (invReplace(inv,book,1,LibraryBooks.reward).success) {
                vars[LibraryVars.request] = 0
                mes("The customer thanks you and gives you a book of arcane knowledge.")
                player.completeDiaryTask(DiaryTask.DeliverLibraryBook)
            }
            return
        }
        val index = random.of(total)
        vars[LibraryVars.request] = 1 + customer*total + index
        mes("Please bring me ${items[LibraryBooks.texts[index]].name}.")
    }

    private suspend fun ProtectedAccess.search(shelf:CoordGrid) {
        if (!player.members) return
        anim(LibrarySequences.search)
        delay(1)
        val books = layout.booksAt(shelf,epochMinute())
        val book = books.map { LibraryBooks.texts[it] }.firstOrNull { it !in inv }
        if (book == null) { mes("You don't find anything useful here."); return }
        if (invAdd(inv,book).success) mes("You find ${items[book].name}.")
    }
}
