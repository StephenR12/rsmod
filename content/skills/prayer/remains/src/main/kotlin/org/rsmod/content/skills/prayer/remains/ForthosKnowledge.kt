package org.rsmod.content.skills.prayer.remains

import jakarta.inject.Inject
import org.rsmod.api.config.refs.*
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object KnowledgeBits : VarBitReferences() {
    val healed = find("hosdun_aimeri_status")
    val stage = find("hosdun_knowledge_search")
    val pages = listOf("sun", "moon", "temple").map { find("hosdun_${it}_pages") }
    val submitted = listOf("sun", "moon", "temple").map { find("hosdun_${it}_tome_returned") }
    val noPages = find("hosdun_page_prevention")
}

internal object KnowledgeItems : ObjReferences() {
    val pages = listOf("sun", "moon", "temple").map { find("hosdun_${it}_page") }
    val books = listOf("sun", "moon", "temple").map { find("hosdun_${it}_tome") }
    val lamp = find("hosdun_miniquest_reward")
}

internal object KnowledgeLocs : LocReferences() {
    val shelves = listOf("sun", "moon", "temple").map { find("hosdun_${it}_bookcase") }
}

internal object KnowledgePeople : NpcReferences() {
    val logosia = find("arceuus_library_librarian")
}

public class ForthosKnowledge @Inject constructor(private val skills: StatTypeList) :
    PluginScript() {
    private val foods by lazy {
        listOf(
            objs.shrimps,
            objs.anchovies,
            objs.sardine,
            objs.herring,
            objs.trout,
            objs.salmon,
            objs.tuna,
            objs.lobster,
            objs.swordfish,
            objs.shark,
            objs.monkfish,
            objs.cooked_chicken,
            objs.cooked_meat,
            objs.cabbage,
            objs.cooked_karambwan,
        )
    }

    override fun ScriptContext.startup() {
        for (p in listOf(CryptPeople.injured, CryptPeople.healed)) {
            onOpNpc1(p) {
                if (!player.members) return@onOpNpc1
                if (
                    vars[CryptBits.faith] >= 200 &&
                        CryptItems.key !in inv &&
                        CryptItems.key !in bank &&
                        choice2("Ask about the temple key.", true, "Discuss the temple.", false)
                ) {
                    invAdd(inv, CryptItems.key)
                    return@onOpNpc1
                }
                if (vars[KnowledgeBits.healed] < 5) {
                    val food = foods.firstOrNull { it in inv }
                    if (food == null) mes("Brother Aimeri needs fish, meat or vegetables.")
                    else feed(food)
                } else if (
                    vars[KnowledgeBits.stage] == 0 &&
                        choice2("I'll keep my eyes open.", true, "Goodbye.", false)
                ) {
                    vars[KnowledgeBits.stage] = 1
                    mes("Bring the lost teachings of the temple to the Arceuus Library.")
                } else mes("There may be forgotten books in the temple library.")
            }
            for (food in foods) onOpNpcU(p, food) { feed(food) }
        }
        for ((i, book) in KnowledgeItems.books.withIndex()) {
            onOpLoc1(KnowledgeLocs.shelves[i]) {
                if (
                    player.members &&
                        vars[KnowledgeBits.stage] > 0 &&
                        vars[KnowledgeBits.submitted[i]] == 0 &&
                        book !in inv &&
                        book !in bank
                )
                    invAdd(inv, book)
            }
            onOpHeldU(book, KnowledgeItems.pages[i]) {
                if (!player.members || vars[KnowledgeBits.pages[i]] >= 4) return@onOpHeldU
                val page = KnowledgeItems.pages[i]
                val count =
                    minOf(
                        inv.sumOf { if (it?.id == page.id) it.count else 0 },
                        4 - vars[KnowledgeBits.pages[i]],
                    )
                if (count > 0 && invDel(inv, page, count).success) {
                    vars[KnowledgeBits.pages[i]] += count
                    mes("You place the missing pages into the tome.")
                }
            }
            onOpHeld2(book) {
                mes("The tome contains ${vars[KnowledgeBits.pages[i]]} of its four missing pages.")
            }
            onOpNpcU(KnowledgePeople.logosia, book) { submit(i, it.invSlot) }
            onOpNpcU(KnowledgePeople.logosia, KnowledgeItems.pages[i]) {
                val page = KnowledgeItems.pages[i]
                if (!player.members || vars[KnowledgeBits.submitted[i]] == 0) return@onOpNpcU
                val count = inv.sumOf { obj -> if (obj?.id == page.id) obj.count else 0 }
                if (count > 0)
                    player.invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, page.id, count)
                        add(bag, objs.coins.id, count * 1000)
                    }
            }
        }
        onOpNpc1(KnowledgePeople.logosia) {
            if (vars[KnowledgeBits.stage] >= 2) {
                if (choice2("Change whether I receive temple pages.", true, "Goodbye.", false))
                    vars[KnowledgeBits.noPages] = 1 - vars[KnowledgeBits.noPages]
            } else {
                val i =
                    KnowledgeItems.books.indices.firstOrNull {
                        KnowledgeItems.books[it] in inv && vars[KnowledgeBits.pages[it]] == 4
                    }
                if (i != null) submit(i, inv.indexOfFirst { it?.id == KnowledgeItems.books[i].id })
                else mes("Logosia is interested in complete tomes from the Forthos Dungeon.")
            }
        }
        onOpHeld1(KnowledgeItems.lamp) {
            if (!player.members) return@onOpHeld1
            val eligible = (0 until 23).map { skills.getValue(it) }.filter { statBase(it) >= 40 }
            if (eligible.isEmpty()) {
                mes("You need a skill of at least level 40.")
                return@onOpHeld1
            }
            val chosen =
                eligible.getOrNull(menu("Choose a skill", true, eligible.map { it.displayName }))
                    ?: return@onOpHeld1
            if (invDel(inv, KnowledgeItems.lamp, slot = it.slot).success)
                statAdvance(chosen, 10000.0)
        }
    }

    private fun ProtectedAccess.feed(food: ObjType) {
        if (!player.members || vars[KnowledgeBits.healed] >= 5) return
        if (invDel(inv, food).success) {
            vars[KnowledgeBits.healed]++
            mes("Brother Aimeri eats the food.")
        }
    }

    private fun ProtectedAccess.submit(i: Int, slot: Int) {
        if (
            !player.members ||
                vars[KnowledgeBits.stage] != 1 ||
                vars[KnowledgeBits.submitted[i]] != 0 ||
                vars[KnowledgeBits.pages[i]] != 4
        )
            return
        val last = KnowledgeBits.submitted.count { vars[it] != 0 } == 2
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                delete(bag, KnowledgeItems.books[i].id, 1, slot)
                if (last) add(bag, KnowledgeItems.lamp.id, 1)
            }
        if (result.success) {
            vars[KnowledgeBits.submitted[i]] = 1
            if (last) {
                vars[KnowledgeBits.stage] = 2
                mes("Logosia gives you a lamp of knowledge.")
            }
        }
    }
}
