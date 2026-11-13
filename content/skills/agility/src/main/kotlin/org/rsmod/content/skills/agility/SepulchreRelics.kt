package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RelicObjs : ObjReferences() {
    val tome = find("hallowed_tome")
    val dye = find("dark_dye")
    val outfit =
        listOf("hood", "cape", "top", "legs", "gloves", "boots").map {
            find("graceful_$it") to find("graceful_${it}_hallowed")
        }
}

internal object RelicLocs : LocReferences() {
    val skeleton = find("hallowed_lobby_skeleton")
}

internal object RelicBits : VarBitReferences() {
    val found = find("hallowed_tome")
}

internal object RelicUI : InterfaceReferences() {
    val book = find("book")
}

internal object RelicBook : ComponentReferences() {
    val title = find("book:title")
    val pages =
        listOf("left", "right").map { side -> (1..15).map { find("book:page_${side}_text_$it") } }
    val numbers = listOf("left", "right").map { find("book:page_${it}_number") }
    val prev = find("book:page_left_button")
    val next = find("book:page_right_button")
}

public class SepulchreRelics @Inject constructor() : PluginScript() {
    private val reading = WeakHashMap<Player, Int>()
    // The blacksmith's account is reconstructed from the six recovered chapters.
    private val chapters =
        listOf(
            listOf(
                "The Hallowvale blacksmith",
                "writes of an icyene warrior.",
                "The queen crossed that same",
                "divide when she married a",
                "human. Tonight, he will ask",
                "the warrior to marry him.",
            ),
            listOf(
                "She accepts. Their families",
                "are still coming to terms",
                "with the match, but they",
                "give the couple their support.",
            ),
            listOf(
                "They marry below the",
                "Acropolis. That evening a",
                "performance delights the",
                "blacksmith, whose attention",
                "returns to his bride.",
            ),
            listOf(
                "The armies of Lord Drakan",
                "have reached Hallowvale.",
                "His neighbours trust the",
                "city's warriors. His wife",
                "is one of those warriors.",
            ),
            listOf(
                "Her orders arrive. She has",
                "three days before she must",
                "leave. He asks the priests",
                "to let them rest together",
                "in the Sepulchre when the",
                "time comes.",
            ),
            listOf("After their farewell, he", "prays to Saradomin for", "her safe return."),
        )

    override fun ScriptContext.startup() {
        onOpLoc1(RelicLocs.skeleton) {
            if (
                RelicObjs.tome in inv || player.invMap[invs.bank]?.contains(RelicObjs.tome) == true
            ) {
                mes("You have already recovered the tome.")
                return@onOpLoc1
            }
            if (invAdd(inv, RelicObjs.tome).success) {
                vars[RelicBits.found] = 1
                mes("You recover a strange tome from the skeleton.")
            } else mes("You need space to carry the tome.")
        }
        for ((i, page) in TreasureItems.pages.withIndex()) onOpHeldU(RelicObjs.tome, page) {
            val bit = TreasureBits.pages[i]
            if (vars[bit] != 0) {
                mes("This page is already in the tome.")
                return@onOpHeldU
            }
            if (invDel(inv, page).success) {
                vars[bit] = 1
                mes("You add the page to the strange hallowed tome.")
            }
        }
        onOpHeld1(RelicObjs.tome) {
            ifOpenMainModal(RelicUI.book)
            ifSetEvents(RelicBook.prev, -1..0, IfEvent.Op1)
            ifSetEvents(RelicBook.next, -1..0, IfEvent.Op1)
            display(0)
        }
        onIfModalButton(RelicBook.prev) { reading[player]?.let { if (it > 0) display(it - 2) } }
        onIfModalButton(RelicBook.next) { reading[player]?.let { if (it < 4) display(it + 2) } }
        onIfClose(RelicUI.book) { reading.remove(player) }
        for ((base, dyed) in RelicObjs.outfit) onOpHeldU(base, RelicObjs.dye) { e ->
            val slot = if (inv[e.firstSlot]?.id == base.id) e.firstSlot else e.secondSlot
            if (
                player
                    .invTransaction(inv) {
                        val bag = select(inv)
                        delete(bag, RelicObjs.dye.id, 1)
                        delete(bag, base.id, 1, slot)
                        add(bag, dyed.id, 1, slot = slot)
                    }
                    .success
            )
                mes("You dye the graceful clothing black and red.")
        }
    }

    private fun ProtectedAccess.display(first: Int) {
        reading[player] = first
        ifSetText(RelicBook.title, "Strange hallowed tome")
        for (side in 0..1) {
            val number = first + side
            val text =
                if (number == 0 || vars[TreasureBits.pages[number - 1]] != 0) chapters[number]
                else listOf("This page is missing.")
            ifSetText(RelicBook.numbers[side], (number + 1).toString())
            for ((i, line) in RelicBook.pages[side].withIndex()) ifSetText(
                line,
                text.getOrNull(i).orEmpty(),
            )
        }
        ifSetHide(RelicBook.prev, first == 0)
        ifSetHide(RelicBook.next, first == 4)
    }
}
