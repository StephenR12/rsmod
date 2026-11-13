package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HouseActivityNpcs : NpcReferences() {
    val taxidermist = find("poh_taxidermist")
    val rock = find("poh_rock")
}

internal object HouseActivityItems : ObjReferences() {
    val rock = find("vt_useless_rock")
    val heads = listOf(find("poh_trophydrop_kalphitequeen"), find("poh_pitydrop_kalphitequeen"))
    val stuffed =
        listOf(
            find("poh_trophydrop_kalphitequeen_stuffed"),
            find("poh_pitydrop_kalphitequeen_stuffed"),
        )
}

internal object HouseActivityVars : VarpReferences() {
    val kq = find("poh_kq_trophy_owned")
}

internal object HouseActivityVarBuilder : VarpBuilder() {
    init {
        build("poh_kq_trophy_owned") { transmitNever = true }
    }
}

@Singleton
internal class HousePetRocks
@Inject
constructor(private val npcs: NpcRepository, private val types: NpcTypeList) {
    private val rocks = WeakHashMap<Player, MutableMap<CoordGrid, Npc>>()

    fun spawn(player: Player, tile: CoordGrid) {
        val pet = Npc(types[HouseActivityNpcs.rock], tile)
        pet.mode = NpcMode.None
        npcs.add(pet, Int.MAX_VALUE)
        rocks.getOrPut(player) { mutableMapOf() }[tile] = pet
    }

    fun restore(player: Player, room: HouseRoom, origin: CoordGrid) {
        var mask = room.petRocks
        while (mask != 0L) {
            val bit = mask.countTrailingZeroBits()
            spawn(player, origin.translate(bit and 7, bit shr 3))
            mask = mask and (mask - 1)
        }
    }

    fun contains(player: Player, pet: Npc): Boolean = rocks[player]?.get(pet.coords) === pet

    fun remove(player: Player, pet: Npc) {
        rocks[player]?.remove(pet.coords)
        npcs.del(pet, Int.MAX_VALUE)
    }

    fun clear(player: Player) {
        for (pet in rocks.remove(player)?.values.orEmpty()) if (pet.slotId != -1)
            npcs.del(pet, Int.MAX_VALUE)
    }
}

internal object HousePortals {
    data class Destination(
        val name: String,
        val suffix: String,
        val level: Int,
        val landing: CoordGrid,
        val runes: List<Pair<ObjType, Int>>,
    )

    val destinations =
        listOf(
            Destination(
                "Varrock",
                "varrock",
                25,
                CoordGrid(3212, 3424),
                listOf(objs.air_rune to 300, objs.fire_rune to 100, objs.law_rune to 100),
            ),
            Destination(
                "Lumbridge",
                "lumbridge",
                31,
                CoordGrid(3222, 3218),
                listOf(objs.air_rune to 300, objs.earth_rune to 100, objs.law_rune to 100),
            ),
            Destination(
                "Falador",
                "falador",
                37,
                CoordGrid(2964, 3378),
                listOf(objs.air_rune to 300, objs.water_rune to 100, objs.law_rune to 100),
            ),
            Destination(
                "Camelot",
                "camelot",
                45,
                CoordGrid(2757, 3477),
                listOf(objs.air_rune to 500, objs.law_rune to 100),
            ),
            Destination(
                "Kharyrll",
                "kharyrll",
                66,
                CoordGrid(3493, 3474),
                listOf(objs.blood_rune to 100, objs.law_rune to 200),
            ),
        )
}

public class HouseActivities
@Inject
internal constructor(
    private val homes: StarterHouse,
    private val pets: HousePetRocks,
    private val residents:MenagerieResidents,
    private val types: LocTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val byName = types.values.associateBy { it.internalName }
        for (name in
            listOf(
                "poh_teleport_centrepiece",
                "poh_teleport_centrepiece_grand",
                "poh_scrying_pool",
            )) onOpLoc1(byName.getValue(name)) { direct(it.loc) }
        for (dest in HousePortals.destinations) for (material in listOf("teak", "mag", "marble")) {
            val portal = byName.getValue("poh_portal_${material}_${dest.suffix}")
            onOpLoc1(portal) {
                if (homes.publicRoom(player, it.loc.coords)?.type != HouseRoomType.PortalChamber)
                    return@onOpLoc1
                if (player.isTeleportBlocked()) {
                    mes("A magical force stops you from teleporting.")
                    return@onOpLoc1
                }
                homes.exitTo(this, dest.landing)
                if (dest.suffix == "kharyrll")
                    player.completeDiaryTask(DiaryTask.EnterKharyrllHousePortal)
            }
            onOpLoc5(portal) { homes.operate(this, it.loc) }
        }
        val blank = byName.getValue("poh_trophy_head_blank_gilded")
        val kq = byName.getValue("poh_trophy_kalphitequeen_gilded")
        onOpLoc5(kq) { homes.operate(this, it.loc) }
        for (loc in listOf(blank, kq)) {
            for (stuffed in HouseActivityItems.stuffed) onOpLocU(loc, stuffed) {
                mount(it.loc, stuffed)
            }
            onOpLoc2(loc) {
                val room = homes.roomAt(player, it.loc.coords, building = true) ?: return@onOpLoc2
                if (room.type != HouseRoomType.SkillHall || room.variant(2) != 3) return@onOpLoc2
                if (vars[HouseActivityVars.kq] == 0) {
                    mes("Use a stuffed head on the gilded display to add it to your collection.")
                    return@onOpLoc2
                }
                val show =
                    choice2(
                        "Display the Kalphite Queen head.",
                        true,
                        "Leave the display empty.",
                        false,
                    )
                homes.update(player, room, room.furnish(12, if (show) 1 else 0), 2)
            }
        }
        onOpLoc1(kq) {
            if (homes.roomAt(player, it.loc.coords)?.type != HouseRoomType.SkillHall)
                return@onOpLoc1
            mes("You speak to your mounted Kalphite Queen head.")
            player.completeDiaryTask(DiaryTask.SpeakToMountedKq)
        }
        onOpNpc1(HouseActivityNpcs.taxidermist) {
            stuff(HouseActivityItems.heads.firstOrNull { it in inv })
        }
        for (head in HouseActivityItems.heads) onOpNpcU(HouseActivityNpcs.taxidermist, head) {
            stuff(head)
        }
        onOpHeld5(HouseActivityItems.rock) {
            val room = homes.roomAt(player, coords)
            if (room == null) {
                invDrop(it.slot, it.inventory)
                return@onOpHeld5
            }
            val bit = 1L shl ((coords.x and 7) + (coords.z and 7) * 8)
            if (room.petRocks and bit != 0L) {
                mes("A pet rock is already sitting here.")
                return@onOpHeld5
            }
            if (invDel(inv, HouseActivityItems.rock).failure) return@onOpHeld5
            homes.update(player, room, room.copy(petRocks = room.petRocks or bit))
            pets.spawn(player, coords)
        }
        onOpNpc3(HouseActivityNpcs.rock) {
            val pet = it.npc
            val room = homes.roomAt(player, pet.coords) ?: return@onOpNpc3
            if (!pets.contains(player, pet)){residents.pickUp(player,pet);return@onOpNpc3}
            if (invAdd(inv, HouseActivityItems.rock).failure) {
                mes("You need some free inventory space.")
                return@onOpNpc3
            }
            val bit = 1L shl ((pet.coords.x and 7) + (pet.coords.z and 7) * 8)
            homes.update(player, room, room.copy(petRocks = room.petRocks and bit.inv()))
            pets.remove(player, pet)
            if (homes.roomAt(player, coords)?.type?.isMenagerie == true)
                player.completeDiaryTask(DiaryTask.RetrieveMenageriePetRock)
        }
    }

    private suspend fun ProtectedAccess.direct(loc: BoundLocInfo) {
        val room = homes.roomAt(player, loc.coords) ?: return
        if (room.type != HouseRoomType.PortalChamber || room.variant(7) == 0) return
        val slots = (1..3).filter { room.variant(it) != 0 }
        if (slots.isEmpty()) {
            mes("Build a portal frame before directing a portal.")
            return
        }
        val slot =
            slots.getOrNull(
                menu(
                    "Direct a portal",
                    true,
                    slots.map { listOf("Left", "Centre", "Right")[it - 1] },
                )
            ) ?: return
        val options = HousePortals.destinations
        val i = menu("Choose a destination", true, options.map { it.name })
        val dest = options.getOrNull(i) ?: return
        if (stat(stats.magic) < dest.level) {
            mes("You need level ${dest.level} Magic to direct this portal.")
            return
        }
        if (dest.suffix == "kharyrll" && vars[varbits.desert_treasure_main] < 15) {
            mes("Complete Desert Treasure I to direct a portal to Kharyrll.")
            return
        }
        if (room.portal(slot) == i + 1) {
            mes("This portal is already directed there.")
            return
        }
        if (!choice2("Spend the runes for 100 teleports to ${dest.name}.", true, "Cancel.", false))
            return
        if (homes.roomAt(player, loc.coords) != room) return
        val paid =
            player.invTransaction(inv) {
                val inventory = select(inv)
                for ((rune, count) in dest.runes) delete(inventory, rune.id, count)
            }
        if (paid.failure) {
            mes("You do not have the required runes in your inventory.")
            return
        }
        homes.update(player, room, room.withPortal(slot, i + 1), slot)
        mes("You direct the portal to ${dest.name}.")
    }

    private suspend fun ProtectedAccess.stuff(head: ObjType?) {
        if (!player.members || head == null) {
            mes("Bring me a Kalphite Queen head and 50,000 coins to have it stuffed.")
            return
        }
        val stuffed = HouseActivityItems.stuffed[HouseActivityItems.heads.indexOf(head)]
        if (!choice2("Pay 50,000 coins to stuff the head.", true, "Cancel.", false)) return
        val result =
            player.invTransaction(inv) {
                val inventory = select(inv)
                delete(inventory, head.id, 1)
                delete(inventory, objs.coins.id, 50000)
                add(inventory, stuffed.id, 1)
            }
        if (result.failure) mes("You need the head and 50,000 coins.")
    }

    private fun ProtectedAccess.mount(loc: BoundLocInfo, stuffed: ObjType) {
        val room = homes.roomAt(player, loc.coords, building = true) ?: return
        if (
            room.type != HouseRoomType.SkillHall ||
                room.variant(2) != 3 ||
                invDel(inv, stuffed).failure
        )
            return
        vars[HouseActivityVars.kq] = 1
        homes.update(player, room, room.furnish(12, 1), 2)
        statAdvance(stats.construction, 223.0)
        for (stat in
            listOf(
                stats.attack,
                stats.strength,
                stats.defence,
                stats.ranged,
                stats.magic,
            )) statAdvance(stat, 200.0)
        mes("You mount the Kalphite Queen head.")
    }
}
