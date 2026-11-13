package org.rsmod.content.skills.magic.tomeofwater

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.lefthand
import org.rsmod.api.script.*
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TomeOfWaterScript @Inject constructor(private val lifecycle: TomeOfWaterLifecycle) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.burnt_page, objs.sunfire_rune) {
            if (!player.members) return@onOpHeldU
            val result =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.burnt_page.id, 1, it.firstSlot)
                    delete(bag, objs.sunfire_rune.id, 100, it.secondSlot)
                    add(bag, objs.searing_page.id, 1)
                }
            if (result.success) mes("You infuse the burnt page with sunfire runes.")
        }
        for (book in Book.entries) {
            for (tome in listOf(book.empty, book.full)) {
                val pages =
                    if (book == Book.Fire) listOf(book.page, objs.searing_page)
                    else listOf(book.page)
                for (page in pages) onOpHeldU(tome, page) {
                    lifecycle.charge(this, it.firstSlot, it.secondSlot)
                }
                onOpHeld4(tome) { lifecycle.pages(this, inv, it.slot) }
                onProtectedEvent<BankedObjEvents.Autocharge>(tome.id) {
                    lifecycle.autocharge(this, it, book)
                }
            }
            onProtectedEvent<BankedObjEvents.ConfigureCharges>(book.full.id) {
                lifecycle.configure(this, book)
            }
            onOpHeld3(book.full) { lifecycle.check(this, it.inventory[it.slot]) }
            onOpWorn2(book.full) { lifecycle.check(this, player.lefthand) }
            onOpWorn2(book.empty) { lifecycle.pages(this, player.worn, Wearpos.LeftHand.slot) }
            onOpWorn3(book.full) { lifecycle.pages(this, player.worn, Wearpos.LeftHand.slot) }
        }
    }
}
