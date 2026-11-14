package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** DB table 76 supplies the destination IDs. Fares are directional and indexed by ID. */
internal enum class CharterPort(
    val portId: Int,
    val label: String,
    val button: String,
    val dock: CoordGrid,
    private val fares: IntArray,
) {
    Brimhaven(
        1,
        "Brimhaven",
        "brimhaven",
        CoordGrid(2761, 3238),
        intArrayOf(
            0,
            0,
            480,
            3900,
            480,
            400,
            2900,
            1600,
            400,
            3200,
            680,
            3450,
            2000,
            2200,
            2400,
            2500,
            2500,
        ),
    ),
    Catherby(
        2,
        "Catherby",
        "catherby",
        CoordGrid(2792, 3415),
        intArrayOf(
            0,
            480,
            0,
            3500,
            480,
            1600,
            3500,
            1000,
            1600,
            3200,
            1000,
            3560,
            2000,
            2200,
            2400,
            2500,
            2500,
        ),
    ),
    MosLeHarmless(
        3,
        "Mos Le'Harmless",
        "harmless",
        CoordGrid(3670, 2931),
        intArrayOf(
            0,
            2900,
            2500,
            0,
            4100,
            1100,
            0,
            1300,
            1100,
            3200,
            4080,
            4950,
            4200,
            4400,
            4600,
            4700,
            4700,
        ),
    ),
    MusaPoint(
        4,
        "Musa Point",
        "musapoint",
        CoordGrid(2955, 3158),
        intArrayOf(
            0,
            200,
            480,
            1100,
            0,
            400,
            1100,
            0,
            200,
            3200,
            800,
            4400,
            2500,
            2700,
            2900,
            3000,
            3000,
        ),
    ),
    PortKhazard(
        5,
        "Port Khazard",
        "khazard",
        CoordGrid(2674, 3143),
        intArrayOf(
            0,
            1600,
            1600,
            4100,
            1600,
            0,
            4100,
            1280,
            1600,
            3200,
            600,
            2800,
            1800,
            2000,
            2200,
            2300,
            2300,
        ),
    ),
    PortPhasmatys(
        6,
        "Port Phasmatys",
        "phasmatys",
        CoordGrid(3703, 3503),
        intArrayOf(
            0,
            2900,
            3500,
            0,
            1100,
            4100,
            0,
            1300,
            3200,
            3200,
            4040,
            5200,
            4000,
            4200,
            4400,
            4500,
            4500,
        ),
    ),
    PortSarim(
        7,
        "Port Sarim",
        "sarim",
        CoordGrid(3038, 3191),
        intArrayOf(
            0,
            1600,
            1000,
            1300,
            0,
            1280,
            1300,
            0,
            400,
            3200,
            1200,
            4800,
            0,
            0,
            3000,
            3100,
            3100,
        ),
    ),
    ShipYard(
        8,
        "Ship Yard",
        "shipyard",
        CoordGrid(3000, 3032),
        intArrayOf(
            0,
            400,
            1600,
            1100,
            200,
            720,
            1100,
            400,
            0,
            3200,
            800,
            4000,
            2600,
            2800,
            3000,
            3100,
            3100,
        ),
    ),
    PortTyras(
        9,
        "Port Tyras",
        "tyras",
        CoordGrid(2142, 3123),
        intArrayOf(
            0,
            3200,
            3200,
            3200,
            3200,
            3200,
            3200,
            3200,
            3200,
            0,
            3200,
            3200,
            3200,
            3200,
            3200,
            3200,
            3200,
        ),
    ),
    CorsairCove(
        10,
        "Corsair Cove",
        "corsaircove",
        CoordGrid(2590, 2851),
        intArrayOf(
            0,
            680,
            1000,
            4080,
            800,
            600,
            4040,
            1200,
            800,
            3200,
            0,
            1420,
            1500,
            1800,
            2000,
            2100,
            2100,
        ),
    ),
    Prifddinas(
        11,
        "Prifddinas",
        "prifddinas",
        CoordGrid(2157, 3331),
        intArrayOf(
            0,
            3450,
            3560,
            4950,
            4400,
            2800,
            5200,
            4800,
            4000,
            3200,
            1420,
            0,
            1200,
            1500,
            1700,
            1800,
            3600,
        ),
    ),
    PortPiscarilius(
        12,
        "Port Piscarilius",
        "piscarilius",
        CoordGrid(1809, 3679),
        intArrayOf(
            0,
            2000,
            2000,
            4200,
            2500,
            1800,
            4000,
            0,
            2600,
            3200,
            1500,
            1200,
            0,
            0,
            1000,
            1100,
            1100,
        ),
    ),
    LandsEnd(
        13,
        "Land's End",
        "landsend",
        CoordGrid(1495, 3403),
        intArrayOf(
            0,
            2200,
            2200,
            4400,
            2700,
            2000,
            4200,
            0,
            2800,
            3200,
            1800,
            1500,
            0,
            0,
            800,
            900,
            900,
        ),
    ),
    CivitasIllaFortis(
        14,
        "Civitas illa Fortis",
        "fortis",
        CoordGrid(1743, 3136),
        intArrayOf(
            0,
            2400,
            2400,
            4600,
            2900,
            2200,
            4400,
            3000,
            3000,
            3200,
            2000,
            1700,
            1000,
            800,
            0,
            600,
            500,
        ),
    ),
    Aldarin(
        15,
        "Aldarin",
        "aldarin",
        CoordGrid(1455, 2968),
        intArrayOf(
            0,
            2500,
            2500,
            4700,
            3000,
            2300,
            4500,
            3100,
            3100,
            3200,
            2100,
            1800,
            1100,
            900,
            600,
            0,
            0,
        ),
    ),
    SunsetCoast(
        16,
        "Sunset Coast",
        "sunsetcoast",
        CoordGrid(1514, 2970),
        intArrayOf(
            0,
            2500,
            2500,
            4700,
            3000,
            2300,
            4500,
            3100,
            3100,
            3200,
            2100,
            3600,
            1100,
            900,
            500,
            0,
            0,
        ),
    );

    fun fareTo(other: CharterPort): Int = fares[other.portId]

    companion object {
        fun near(tile: CoordGrid): CharterPort? =
            entries.firstOrNull { tile.level == 0 && it.dock.chebyshevDistance(tile) <= 12 }
    }
}

internal object CharterMap : InterfaceReferences() {
    val main = find("sailing_menu")
}

internal object CharterButtons : ComponentReferences() {
    // The revision 238 client builds the map's clickable children from chartering_destinations
    // (DB table 206). Each row creates a map image followed by its clickable marker.
    val content = find("sailing_menu:content")
    private val destinationsInClientOrder =
        listOf(
            CharterPort.PortSarim,
            CharterPort.Brimhaven,
            CharterPort.Catherby,
            CharterPort.MosLeHarmless,
            CharterPort.MusaPoint,
            CharterPort.PortKhazard,
            CharterPort.PortPhasmatys,
            CharterPort.ShipYard,
            CharterPort.PortTyras,
            CharterPort.CorsairCove,
            CharterPort.Prifddinas,
            CharterPort.PortPiscarilius,
            CharterPort.LandsEnd,
            CharterPort.CivitasIllaFortis,
            CharterPort.Aldarin,
            CharterPort.SunsetCoast,
        )
    val childToDestination = destinationsInClientOrder.mapIndexed { index, port ->
        (index * 2 + 1) to port
    }.toMap()
    val destinationToChild = childToDestination.entries.associate { (child, port) -> port to child }
    // Tempestus occupies the next DB row. Crandor follows it and has no charter route.
    const val crandorChild = 18 * 2 + 1
}

internal object CharterVars : VarpReferences() {
    val fever = find("fever_quest")
    val priest = find("priestperil")
    val regicide = find("regicide_quest")
    val monkey = find("mm_main")
    val grandTree = find("grandtree")
    val dragon = find("dragonquest")
}

internal object CharterBits : VarBitReferences() {
    val picked = find("chartering_previous_destination")
    val skipConfirmation = find("settings_warning_confirmation_charter_ships")
    val elves = find("sote")
    val zeah = find("zeah_playerhasvisited")
    val varlamore = find("varlamore_visited")
}

internal object CharterLocs : LocReferences() {
    val gates = listOf(find("grandtree_fencegate_l"), find("grandtree_fencegate_r"))
}

internal object CharterInvs : InvReferences() {
    val trade = find("trader_stan_shop")
}

internal object CharterItems : ObjReferences() {
    data class Goods(val item: ObjType, val amount: Int, val restock: Int)

    val excluded = listOf(find("karamja_rum"), find("ahoy_bedsheet"), find("ahoy_bedsheetgreen"))
    val seal = find("mm_gnome_royal_seal")
    val goods =
        listOf(
            Goods(find("pot_empty"), 5, 10),
            Goods(find("jug_empty"), 2, 100),
            Goods(find("pack_jug_empty"), 6, 15),
            Goods(find("shears"), 2, 100),
            Goods(find("bucket_empty"), 3, 10),
            Goods(find("pack_bucket"), 10, 10),
            Goods(find("bowl_empty"), 2, 50),
            Goods(find("cake_tin"), 2, 50),
            Goods(find("tinderbox"), 2, 100),
            Goods(find("chisel"), 2, 100),
            Goods(find("hammer"), 5, 100),
            Goods(find("newcomer_map"), 5, 100),
            Goods(find("sos_security_book"), 5, 100),
            Goods(find("rope"), 2, 300),
            Goods(find("knife"), 2, 100),
            Goods(find("pineapple"), 15, 100),
            Goods(find("banana"), 15, 100),
            Goods(find("orange"), 10, 100),
            Goods(find("bucket_ectoplasm"), 10, 100),
            Goods(find("glassblowingpipe"), 15, 100),
            Goods(find("bucket_sand"), 10, 100),
            Goods(find("seaweed"), 20, 100),
            Goods(find("soda_ash"), 10, 100),
            Goods(find("lobster_pot"), 20, 100),
            Goods(find("fishing_rod"), 20, 100),
            Goods(find("swamppaste"), 30, 100),
            Goods(find("sail_tyras_helm"), 25, 200),
            Goods(find("raw_rabbit"), 20, 100),
            Goods(find("eye_patch"), 5, 100),
            Goods(find("bronze_cannonball"), 250, 100),
        )
}

internal object CharterStore : InvEditor() {
    init {
        edit(CharterInvs.trade) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            restock = true
            for (g in CharterItems.goods) stock +=
                stock(g.item, count = g.amount, restockCycles = g.restock)
        }
    }
}

public class CharterTravel
@Inject
constructor(private val types: NpcTypeList, private val shops: Shops) : PluginScript() {
    private val browsing = WeakHashMap<Player, CharterPort>()

    override fun ScriptContext.startup() {
        // The cache morphs the base crew at every port using the player's previous destination.
        for (npc in types.values) {
            if (
                !npc.internalName.orEmpty().startsWith("sailing_transport_trader_stan") ||
                    npc.op[3] != "Charter"
            )
                continue
            onOpNpc1(npc) {
                val from = CharterPort.near(it.npc.coords) ?: return@onOpNpc1
                if (!player.members) return@onOpNpc1
                when (
                    menu(
                        "Trader Stan's fleet",
                        true,
                        listOf("Charter a ship.", "Browse the trading post.", "Leave."),
                    )
                ) {
                    0 -> open(from)
                    1 -> trade()
                }
            }
            onOpNpc3(npc) { if (player.members) trade() }
            onApNpc3(npc) { if (player.members && isWithinApRange(it.npc, 3)) trade() }
            onOpNpc4(npc) { CharterPort.near(it.npc.coords)?.let { from -> open(from) } }
            onApNpc4(npc) {
                if (isWithinApRange(it.npc, 3))
                    CharterPort.near(it.npc.coords)?.let { from -> open(from) }
            }
            if (npc.op[4]?.startsWith("Charter-to ") == true)
                onOpNpc5(npc) {
                    val from = CharterPort.near(it.npc.coords) ?: return@onOpNpc5
                    val dest =
                        CharterPort.entries.firstOrNull { port ->
                            port.portId == vars[CharterBits.picked]
                        } ?: return@onOpNpc5
                    sail(from, dest)
                }
            if (npc.op[4]?.startsWith("Charter-to ") == true)
                onApNpc5(npc) {
                    if (!isWithinApRange(it.npc, 3)) return@onApNpc5
                    val from = CharterPort.near(it.npc.coords) ?: return@onApNpc5
                    val dest =
                        CharterPort.entries.firstOrNull { port ->
                            port.portId == vars[CharterBits.picked]
                        } ?: return@onApNpc5
                    sail(from, dest)
                }
        }
        onIfModalButton(CharterButtons.content) {
            val from = browsing[player] ?: return@onIfModalButton
            val port = CharterButtons.childToDestination[it.comsub]
            if (port != null) sail(from, port)
            else if (it.comsub == CharterButtons.crandorChild && vars[CharterVars.dragon] in 2..9)
                mes("No charter captain will sail to Crandor.")
        }
        onIfClose(CharterMap.main) { browsing.remove(player) }
        onPlayerLogout { browsing.remove(player) }
        for (gate in CharterLocs.gates) onOpLoc1(gate) {
            if (it.loc.x != 2945) return@onOpLoc1
            val entering = coords.x < 2945
            if (
                !player.members ||
                    entering &&
                        (vars[CharterVars.grandTree] < 80 ||
                            vars[CharterVars.grandTree] < 160 && CharterItems.seal !in inv)
            ) {
                mes("The workers refuse to let you enter the shipyard.")
                return@onOpLoc1
            }
            telejump(CoordGrid(if (entering) 2945 else 2944, it.loc.z))
        }
    }

    private fun ProtectedAccess.trade() =
        shops.open(player, "Trader Stan's Trading Post", CharterInvs.trade, 25.0, 150.0, 2.0)

    // Client script 7333 uses 61 for having crossed into Morytania, beyond the 60-point quest
    // finish.
    private fun ProtectedAccess.available(port: CharterPort): Boolean =
        when (port) {
            CharterPort.MosLeHarmless ->
                vars[CharterVars.fever] >= 140 && vars[CharterVars.priest] >= 61
            CharterPort.PortPhasmatys -> vars[CharterVars.priest] >= 61
            CharterPort.ShipYard ->
                vars[CharterVars.grandTree] >= 160 && vars[CharterVars.monkey] >= 2
            CharterPort.PortTyras -> vars[CharterVars.regicide] >= 15
            CharterPort.Prifddinas -> vars[CharterBits.elves] >= 200
            CharterPort.PortPiscarilius,
            CharterPort.LandsEnd -> vars[CharterBits.zeah] != 0
            CharterPort.Aldarin,
            CharterPort.SunsetCoast,
            CharterPort.CivitasIllaFortis -> vars[CharterBits.varlamore] != 0
            else -> true
        }

    private fun ProtectedAccess.canDepart(from: CharterPort): Boolean {
        if (
            !player.members ||
                !available(from) ||
                coords.level != 0 ||
                from.dock.chebyshevDistance(coords) > 14
        ) {
            mes("The crew cannot offer you passage here.")
            return false
        }
        if (CharterItems.excluded.any { it in inv }) {
            mes("The crew will not carry Karamjan rum or ghostly bedsheets.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.open(from: CharterPort) {
        if (!canDepart(from)) return
        ifOpenMainModal(CharterMap.main)
        browsing[player] = from
        for ((dest, child) in CharterButtons.destinationToChild) {
            val usable = available(dest) && from.fareTo(dest) > 0
            if (usable) ifSetEvents(CharterButtons.content, child..child, IfEvent.Op1)
        }
        if (vars[CharterVars.dragon] in 2..9)
            ifSetEvents(
                CharterButtons.content,
                CharterButtons.crandorChild..CharterButtons.crandorChild,
                IfEvent.Op1,
            )
    }

    private suspend fun ProtectedAccess.sail(from: CharterPort, to: CharterPort) {
        if (!canDepart(from) || !available(to)) return
        val base = from.fareTo(to)
        if (base <= 0) return
        var fare = base
        if (vars[CharterVars.fever] >= 140) fare /= 2
        if (QuestRouteItems.charos in worn) fare /= 2
        ifClose()
        if (
            vars[CharterBits.skipConfirmation] == 0 &&
                !choice2("Charter to ${to.label} for $fare coins.", true, "Stay here.", false)
        )
            return
        if (!invDel(inv, objs.coins, count = fare).success) {
            mes("You need $fare coins for this journey.")
            return
        }
        vars[CharterBits.picked] = to.portId
        telejump(to.dock)
        if (from == CharterPort.ShipYard) player.completeDiaryTask(DiaryTask.CharterShipyard)
    }
}
