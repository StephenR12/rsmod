package org.rsmod.content.skills.magic.tomeofwater

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.TomeOfWater
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varobjs
import org.rsmod.api.player.events.interact.BankedObjEvents
import org.rsmod.api.player.lefthand
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.utils.bits.getBits
import org.rsmod.utils.bits.withBits

internal enum class Book(
    val full: ObjType,
    val empty: ObjType,
    val page: ObjType,
    val target: VarBitType,
    val title: String,
    val pageName: String,
) {
    Water(
        objs.tome_of_water,
        objs.tome_of_water_uncharged,
        objs.soaked_page,
        varbits.charges_tome_of_water_quantity,
        "water",
        "soaked",
    ),
    Earth(
        objs.tome_of_earth,
        objs.tome_of_earth_uncharged,
        objs.soiled_page,
        varbits.charges_tome_of_earth_quantity,
        "earth",
        "soiled",
    ),
    Fire(
        objs.tome_of_fire,
        objs.tome_of_fire_uncharged,
        objs.burnt_page,
        varbits.charges_tome_of_fire_quantity,
        "fire",
        "burnt",
    );

    fun accepts(type: Int): Boolean =
        page.id == type || this == Fire && type == objs.searing_page.id

    fun loadedPage(item: InvObj): ObjType =
        if (this == Fire && item.vars and TomeOfWater.SEARING_PAGES != 0) objs.searing_page
        else page

    fun pageName(type: ObjType): String =
        if (type.id == objs.searing_page.id) "searing" else pageName

    companion object {
        fun from(item: InvObj): Book? =
            entries.firstOrNull { item.isType(it.full) || item.isType(it.empty) }
    }
}

public class TomeOfWaterLifecycle @Inject constructor() {
    public fun charges(tome: InvObj?): Int =
        tome?.vars?.getBits(varobjs.tome_of_water_charges.bits) ?: 0

    internal suspend fun configure(access: ProtectedAccess, book: Book) {
        access.vars[book.target] =
            access
                .countDialog("How many charges should the tome hold on withdrawal?")
                .coerceIn(0, TomeOfWater.MAX_CHARGES)
    }

    internal fun autocharge(
        access: ProtectedAccess,
        event: BankedObjEvents.Autocharge,
        book: Book,
    ) {
        val item = access.inv[event.slot] ?: return
        if (item != event.obj || Book.from(item) != book) return
        val current = charges(item)
        val needed = access.vars[book.target].coerceIn(0, TomeOfWater.MAX_CHARGES) - current
        if (needed <= 0) return
        val page = book.loadedPage(item)
        val requested = pagesFor(needed)
        val used = event.takeMaterial(page, requested)
        if (used > 0)
            store(
                access,
                access.inv,
                event.slot,
                item,
                book,
                page,
                current + used * TomeOfWater.CHARGES_PER_PAGE,
            )
        if (used < requested)
            access.mes(
                "There aren't enough ${book.pageName(page)} pages in your bank to charge the tome fully."
            )
    }

    public fun charge(access: ProtectedAccess, tomeSlot: Int, pageSlot: Int) {
        addPages(access, access.inv, tomeSlot, pageSlot)
    }

    private fun addPages(
        access: ProtectedAccess,
        inventory: Inventory,
        tomeSlot: Int,
        pageSlot: Int,
    ) {
        val item = inventory[tomeSlot] ?: return
        val book = Book.from(item) ?: return
        val stack = access.inv[pageSlot] ?: return
        if (!book.accepts(stack.id)) return
        val page = if (stack.isType(objs.searing_page)) objs.searing_page else book.page
        val current = charges(item)
        if (current > 0 && book.loadedPage(item).id != page.id) {
            access.mes("Remove the existing pages before adding a different type.")
            return
        }
        val capacity = TomeOfWater.MAX_CHARGES - current
        if (capacity <= 0) {
            access.mes("Your tome of ${book.title} is already fully charged.")
            return
        }
        val used = minOf(stack.count, pagesFor(capacity))
        if (used <= 0 || access.invDel(access.inv, page, used, pageSlot).failure) return
        store(
            access,
            inventory,
            tomeSlot,
            item,
            book,
            page,
            current + used * TomeOfWater.CHARGES_PER_PAGE,
        )
        access.mes(
            "You add $used ${book.pageName(page)} ${if (used == 1) "page" else "pages"} to the tome."
        )
    }

    internal suspend fun pages(access: ProtectedAccess, inventory: Inventory, slot: Int) {
        val item = inventory[slot] ?: return
        val book = Book.from(item) ?: return
        val current = charges(item)
        val remove = current > 0 && access.choice2("Add pages.", false, "Remove pages.", true)
        if (inventory[slot] != item) return
        if (remove) {
            val count = current / TomeOfWater.CHARGES_PER_PAGE
            val page = book.loadedPage(item)
            if (count > 0 && access.invAdd(access.inv, page, count).failure) return
            store(access, inventory, slot, item, book, page, 0)
            access.mes("You empty the tome of ${book.title}.")
            return
        }
        val slotWithPage =
            access.inv.indexOfFirst {
                it != null &&
                    book.accepts(it.id) &&
                    (current == 0 || it.id == book.loadedPage(item).id)
            }
        if (slotWithPage < 0) {
            access.mes("You have no suitable pages to add.")
            return
        }
        addPages(access, inventory, slot, slotWithPage)
    }

    public fun check(access: ProtectedAccess, tome: InvObj?) {
        if (tome == null) return
        val book = Book.from(tome) ?: return
        val detail =
            if (book == Book.Fire) " (${book.pageName(book.loadedPage(tome))} pages)" else ""
        access.mes("Your tome of ${book.title} has ${charges(tome)} charges remaining$detail.")
    }

    private fun store(
        access: ProtectedAccess,
        inventory: Inventory,
        slot: Int,
        item: InvObj,
        book: Book,
        page: ObjType,
        charges: Int,
    ) {
        val base = item.vars and TomeOfWater.SEARING_PAGES.inv()
        val vars =
            base.withBits(
                varobjs.tome_of_water_charges.bits,
                charges.coerceAtMost(TomeOfWater.MAX_CHARGES),
            )
        val searing =
            if (charges > 0 && page.id == objs.searing_page.id) TomeOfWater.SEARING_PAGES else 0
        val updated = InvObj(if (charges > 0) book.full else book.empty, vars = vars or searing)
        if (inventory === access.player.worn && slot == Wearpos.LeftHand.slot)
            access.player.lefthand = updated
        else inventory[slot] = updated
    }

    private fun pagesFor(charges: Int): Int =
        (charges + TomeOfWater.CHARGES_PER_PAGE - 1) / TomeOfWater.CHARGES_PER_PAGE

    public companion object {
        public const val FULL_MESSAGE: String = "Your tome of water is already fully charged."
    }
}
