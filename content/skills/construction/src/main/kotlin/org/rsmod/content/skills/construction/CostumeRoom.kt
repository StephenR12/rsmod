package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.invtx.*
import org.rsmod.api.player.CostumeRoomInventory
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal enum class CostumeFurniture(
    val slot: Int,
    val rowPrefix: String,
    val materials: List<String>,
    val xp: List<Int>,
    val catalogues: List<Int>,
    val locPrefix: String = rowPrefix,
) {
    Rack(
        1,
        "cape_rack",
        listOf("oak", "teak", "mahogany", "mahogany_gilded", "marble", "magic_stone"),
        listOf(240, 360, 560, 860, 500, 1000),
        listOf(3292),
    ),
    Wardrobe(
        2,
        "magic_wardrobe",
        listOf("oak", "carved_oak", "teak", "teak_carved", "mahogany", "mahogany_gilded", "marble"),
        listOf(240, 360, 360, 540, 560, 860, 500),
        listOf(3289),
    ),
    Toy(3, "toy_box", listOf("oak", "teak", "mahogany"), listOf(120, 180, 280), listOf(3299)),
    Treasure(
        4,
        "treasure_chest",
        listOf("oak", "teak", "mahogany"),
        listOf(120, 180, 280),
        listOf(3293, 3294, 3295, 3296, 3297, 3298),
        "tresure_chest",
    ),
    Fancy(
        5,
        "fancy_dress_box",
        listOf("oak", "teak", "mahogany"),
        listOf(120, 180, 280),
        listOf(3291),
    ),
    Armour(
        6,
        "armour_case",
        listOf("oak", "teak", "mahogany"),
        listOf(180, 270, 420),
        listOf(3290),
    );

    fun pages(tier: Int): List<Int> =
        if (this == Treasure) catalogues.take(listOf(2, 3, 6)[tier - 1]) else catalogues

    fun setLimit(tier: Int): Int =
        when (this) {
            Wardrobe -> if (tier == 7) Int.MAX_VALUE else tier * 7
            Armour -> listOf(25, 50, Int.MAX_VALUE)[tier - 1]
            Fancy -> listOf(2, 4, Int.MAX_VALUE)[tier - 1]
            Rack -> listOf(0, 1, 5, 10, Int.MAX_VALUE, Int.MAX_VALUE)[tier - 1]
            else -> Int.MAX_VALUE
        }
}

internal object CostumeInterfaces : InterfaceReferences() {
    val main = find("poh_costumes")
    val side = find("poh_costumes_side")
}

internal object CostumeButtons : ComponentReferences() {
    val items = find("poh_costumes:items")
    val depositAll = find("poh_costumes:depositall")
    val bag = find("poh_costumes_side:items")
}

internal object CostumeBits : VarBitReferences() {
    val iron = find("ironman")
}

internal object CostumeInvEdit : InvEditor() {
    init {
        edit(CostumeRoomInventory.storage) {
            scope = InvScope.Perm
            protect = false
            stack = InvStackType.Always
        }
    }
}

/** Native catalogue entries group set pieces, and each piece may have several item variants. */
@jakarta.inject.Singleton
internal class CostumeCatalogue @Inject constructor(enums: EnumTypeList) {
    data class Set(val head: Int, val pieces: List<kotlin.collections.Set<Int>>) {
        val items: kotlin.collections.Set<Int> = pieces.flatten().toSet()

        fun present(storage: Inventory): Boolean = storage.any { it?.id in items }

        fun complete(storage: Inventory): Boolean =
            pieces.all { piece -> storage.any { it?.id in piece } }
    }

    private fun enumMap(enums: EnumTypeList, id: Int) = enums.getValue(id).primitiveMap

    private fun values(enums: EnumTypeList, id: Int) =
        enumMap(enums, id).entries.sortedBy { it.key as Int }.map { it.value as Int }

    val pages: Map<Int, List<Set>> =
        CostumeFurniture.entries
            .flatMap { it.catalogues }
            .associateWith { page ->
                values(enums, page).map { head ->
                    val memberEnum = enumMap(enums, 3077)[head] as? Int
                    val required = memberEnum?.let { values(enums, it) } ?: listOf(head)
                    val pieces =
                        required.map { member ->
                            buildSet {
                                add(member)
                                (enumMap(enums, 3303)[member] as? Int)?.let(::add)
                                (enumMap(enums, 3304)[member] as? Int)?.let {
                                    addAll(values(enums, it))
                                }
                            }
                        }
                    Set(head, pieces)
                }
            }

    fun hasStored(player: Player, slot: Int? = null): Boolean {
        val storage = player.invMap[CostumeRoomInventory.storage] ?: return false
        if (slot == null) return storage.any { it != null }
        val fixture = CostumeFurniture.entries.firstOrNull { it.slot == slot } ?: return false
        return fixture.catalogues.any { page -> pages.getValue(page).any { it.present(storage) } }
    }
}

public class CostumeRoom
@Inject
internal constructor(
    private val home: StarterHouse,
    private val locs: LocRepository,
    private val types: LocTypeList,
    private val objs: ObjTypeList,
    private val catalogue: CostumeCatalogue,
) : PluginScript() {

    private data class View(
        val tile: CoordGrid,
        val fixture: CostumeFurniture,
        val tier: Int,
        val page: Int,
    )

    private val views = WeakHashMap<Player, View>()

    override fun ScriptContext.startup() {
        val names = types.values.associateBy { it.internalName }
        for (fixture in CostumeFurniture.entries) for ((index, material) in
            fixture.materials.withIndex()) {
            val prefix = "poh_cos_room_${fixture.locPrefix}_"
            val suffix = material.replace("teak_carved", "carved_teak")
            val closed = names.getValue(prefix + suffix)
            val open = names[prefix + "open_" + suffix]
            val tier = index + 1
            onOpLoc1(closed) {
                if (owner(it.loc.coords, fixture, tier)) {
                    if (open != null) locs.change(it.loc, open, Int.MAX_VALUE)
                    show(it.loc.coords, fixture, tier)
                }
            }
            if (open != null) {
                onOpLoc1(open) {
                    if (owner(it.loc.coords, fixture, tier)) show(it.loc.coords, fixture, tier)
                }
                onOpLoc2(open) {
                    if (owner(it.loc.coords, fixture, tier))
                        locs.change(it.loc, closed, Int.MAX_VALUE)
                }
            }
            for (loc in listOfNotNull(closed, open)) onOpLocU(loc) { e ->
                if (owner(e.loc.coords, fixture, tier))
                    deposit(e.invSlot, fixture, tier, fixture.pages(tier))
            }
        }
        onIfModalButton(CostumeButtons.bag) { e ->
            val view = current() ?: return@onIfModalButton
            if (e.op.slot == 1 && inv[e.comsub]?.id == e.obj?.id)
                deposit(e.comsub, view.fixture, view.tier, listOf(view.page))
        }
        onIfModalButton(CostumeButtons.depositAll) {
            val view = current() ?: return@onIfModalButton
            if (it.op.slot == 1)
                for (slot in inv.indices) deposit(
                    slot,
                    view.fixture,
                    view.tier,
                    listOf(view.page),
                    false,
                )
        }
        onIfModalButton(CostumeButtons.items) { e ->
            val view = current() ?: return@onIfModalButton
            val id = e.obj?.id ?: return@onIfModalButton
            val set =
                catalogue.pages.getValue(view.page).firstOrNull { id in it.items }
                    ?: return@onIfModalButton
            if (e.op.slot != 1) return@onIfModalButton
            val storage = inv(CostumeRoomInventory.storage)
            if (vars[CostumeBits.iron] == 2 && !set.complete(storage)) {
                mes("Store the complete set before withdrawing an item.")
                return@onIfModalButton
            }
            val slot = storage.indexOfFirst { it?.id == id }
            if (slot < 0) return@onIfModalButton
            val slots = if (vars[CostumeBits.iron] == 2)
                set.pieces.map { piece -> storage.indexOfFirst { it?.id in piece } }
            else listOf(slot)
            val result = player.invTransaction(storage, inv) {
                val source = select(storage)
                val bag = select(inv)
                for (storedSlot in slots) transfer(from = source, fromSlot = storedSlot, count = 1, into = bag)
            }
            if (result.failure) mes("You need more inventory space to withdraw that.")

        }
        onIfClose(CostumeInterfaces.main) {
            views.remove(player)
            player.invMap[CostumeRoomInventory.storage]?.let(player::stopInvTransmit)
        }
    }

    private fun ProtectedAccess.owner(
        tile: CoordGrid,
        fixture: CostumeFurniture,
        tier: Int,
    ): Boolean {
        val room = home.roomAt(player, tile) ?: return false
        return player.members &&
            room.type == HouseRoomType.CostumeRoom &&
            room.variant(fixture.slot) == tier
    }

    private fun ProtectedAccess.current(): View? =
        views[player]?.takeIf { owner(it.tile, it.fixture, it.tier) }

    private suspend fun ProtectedAccess.show(
        tile: CoordGrid,
        fixture: CostumeFurniture,
        tier: Int,
    ) {
        val pages = fixture.pages(tier)
        val page =
            if (pages.size == 1) pages.first()
            else
                pages.getOrNull(
                    menu(
                        "Treasure trails",
                        true,
                        listOf("Beginner", "Easy", "Medium", "Hard", "Elite", "Master")
                            .take(pages.size),
                    )
                ) ?: return
        ifOpenMainSidePair(CostumeInterfaces.main, CostumeInterfaces.side)
        views[player] = View(tile, fixture, tier, page)
        invTransmit(inv(CostumeRoomInventory.storage))
        interfaceInvInit(inv, CostumeButtons.bag, 4, 7, op1 = "Store")
        runClientScript(3532, page, 1, 1)
        ifSetEvents(CostumeButtons.bag, inv.indices, IfEvent.Op1)
        ifSetEvents(CostumeButtons.items, 0..4095, IfEvent.Op1, IfEvent.Op10)
        ifSetEvents(CostumeButtons.depositAll, -1..0, IfEvent.Op1)
    }

    private fun ProtectedAccess.deposit(
        slot: Int,
        fixture: CostumeFurniture,
        tier: Int,
        pages: List<Int>,
        message: Boolean = true,
    ) {
        val obj = inv[slot] ?: return
        val sets = pages.flatMap { catalogue.pages.getValue(it) }
        val set = sets.firstOrNull { obj.id in it.items }
        if (set == null) {
            if (message) mes("That item doesn't belong in this furniture.")
            return
        }
        val storage = inv(CostumeRoomInventory.storage)
        if (vars[CostumeBits.iron] == 2 && storage.any { it?.id == obj.id }) {
            if (message) mes("You already have one of those stored.")
            return
        }
        val limited =
            if (fixture == CostumeFurniture.Rack)
                sets.filter { objs[it.head]?.internalName?.startsWith("skillcape_") == true }
            else sets
        if (
            set in limited &&
                !set.present(storage) &&
                limited.count { it.present(storage) } >= fixture.setLimit(tier)
        ) {
            if (message) mes("You need to upgrade this furniture to store more sets.")
            return
        }
        player.invTransaction(inv, storage) {
            transfer(from = select(inv), fromSlot = slot, count = 1, into = select(storage))
        }
    }
}
