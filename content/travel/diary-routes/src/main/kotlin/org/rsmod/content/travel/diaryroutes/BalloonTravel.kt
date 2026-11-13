package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.util.WeakHashMap
import net.rsprot.protocol.game.outgoing.interfaces.IfSetModel
import net.rsprot.protocol.game.outgoing.interfaces.IfSetPosition
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.EntranaEquipment
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal enum class BalloonPort(
    val shortName: String,
    val landing: CoordGrid,
    val level: Int,
    val fuel: Int,
    val firstCost: Int,
    val mapNumber: Int = 0,
    val altitude: Int = 0,
) {
    Entrana("ent", CoordGrid(2810, 3355), 20, 0, 1),
    Taverley("tav", CoordGrid(2937, 3421), 20, 0, 1),
    CraftingGuild("craft", CoordGrid(2923, 3301), 30, 1, 10, 10, 8),
    Varrock("varr", CoordGrid(3298, 3481), 40, 2, 10, 13, 6),
    CastleWars("cast", CoordGrid(2459, 3107), 50, 3, 10, 4, 5),
    GrandTree("gno", CoordGrid(2479, 3458), 60, 4, 3, 7, 8);

    companion object {
        fun near(tile: CoordGrid) =
            entries.firstOrNull { tile.level == 0 && tile.chebyshevDistance(it.landing) <= 8 }
    }
}

internal object BalloonLocs : LocReferences() {
    val baskets =
        listOf("entrana", "tav", "craft", "varr", "cast", "gno").map {
            find("zep_multi_basket_$it")
        }
    val storage =
        listOf("entrana", "tav", "craft", "varr", "cast", "gno").map {
            find("zep_multi_storage_$it")
        }
}

internal object BalloonNpcs : NpcReferences() {
    val pilots =
        listOf(
                "multi_piccard",
                "assist_tav",
                "multi_assistant_craft",
                "multi_assistant_varr",
                "multi_assistant_cast",
                "multi_assistant_gno",
                "assist_gno_replacement",
            )
            .map { find("zep_$it") }
}

internal object BalloonBits : VarBitReferences() {
    val quest = find("zep_quest")
    val routes =
        BalloonPort.entries
            .filter { it.mapNumber != 0 }
            .associateWith { find("zep_multi_${it.shortName}") }
    val gnomePilot = find("mm2_zep_gnome_pilot")
    val sandbags = find("zep_if_sandbags")
    val logs = find("zep_if_logs")
    val column = find("zep_if_screen_dist")
    val height = find("zep_if_balloon_height")
    val map = find("zep_if_current_map")
}

internal object BalloonSaves : VarpReferences() {
    val bins = listOf(find("balloon_stored_logs"), find("balloon_stored_magic"))
}

internal object BalloonSaveBuilder : VarpBuilder() {
    init {
        build("balloon_stored_logs") { transmitNever = true }
        build("balloon_stored_magic") { transmitNever = true }
    }
}

internal object BalloonInterfaces : InterfaceReferences() {
    val map = find("zep_balloon_map")
    val flight = find("zep_interface")
    val controls = find("zep_interface_side")
}

internal object BalloonButtons : ComponentReferences() {
    val ports = BalloonPort.entries.associateWith { find("zep_balloon_map:btn_${it.shortName}") }
    val moves =
        mapOf(
                "zep_btn_sandbags" to 2,
                "zep_btn_logs" to 1,
                "zep_btn_relax" to 0,
                "zep_btn_tug" to -1,
                "zep_btn_tug_emerg" to -2,
            )
            .map { (name, delta) -> find("zep_interface_side:$name") to delta }
    val bail = find("zep_interface_side:zep_btn_close")
    val bagsLeft = find("zep_interface_side:zep_if_sandbags")
    val logsLeft = find("zep_interface_side:zep_if_logs")
    val envelope = find("zep_interface:zep_if_balloon")
    val basket = find("zep_interface:zep_if_balloon2")
    val cells = List(13) { y -> List(20) { x -> find("zep_interface:zep_grid_${y}_$x") } }
}

private data class BalloonFlight(
    val port: BalloonPort,
    var screen: Int = 0,
    var x: Int = 0,
    var y: Int = port.altitude,
    var logs: Int = 10,
    var bags: Int = 8,
)

public class BalloonTravel @Inject constructor(private val types: ObjTypeList) : PluginScript() {
    private val maps = mutableMapOf<Pair<BalloonPort, Int>, Array<IntArray>>()
    private val planning = WeakHashMap<Player, BalloonPort>()
    private val flights = WeakHashMap<Player, BalloonFlight>()
    private val logs =
        listOf(objs.logs, objs.oak_logs, objs.willow_logs, objs.yew_logs, objs.magic_logs)

    init {
        checkNotNull(javaClass.getResourceAsStream("balloon-maps.tsv")).bufferedReader().useLines {
            lines ->
            for (line in lines.filterNot { it.startsWith("#") || it.isBlank() }) {
                val (course, stage, height, models) = line.split('\t')
                val grid =
                    maps.getOrPut(BalloonPort.valueOf(course) to stage.toInt() - 1) {
                        Array(13) { IntArray(20) }
                    }
                grid[height.toInt()] = models.split(',').map(String::toInt).toIntArray()
            }
        }
    }

    override fun ScriptContext.startup() {
        for (pilot in BalloonNpcs.pilots) {
            onOpNpc1(pilot) { choose(it.npc.coords) }
            onOpNpc4(pilot) { choose(it.npc.coords) }
        }
        for (basket in BalloonLocs.baskets) onOpLoc1(basket) { choose(it.loc.coords) }
        for ((port, button) in BalloonButtons.ports) onIfModalButton(button) {
            val source = planning[player] ?: return@onIfModalButton
            depart(source, port)
        }
        for ((button, movement) in BalloonButtons.moves) onIfModalButton(button) { move(movement) }
        onIfModalButton(BalloonButtons.bail) {
            val flight = flights[player] ?: return@onIfModalButton
            crash(flight)
        }
        onIfClose(BalloonInterfaces.map) { planning.remove(player) }
        onIfClose(BalloonInterfaces.flight) { flights.remove(player) }
        onPlayerLogout {
            flights.remove(player)
            planning.remove(player)
        }
        for (shed in BalloonLocs.storage) {
            onOpLoc1(shed) {
                if (!unlocked() || BalloonPort.near(it.loc.coords) == null) return@onOpLoc1
                var total = 0
                for ((i, log) in logs.withIndex()) {
                    val cert = types[log].certlink
                    for (item in listOfNotNull(types[log], types[cert])) {
                        val qty = minOf(100 - stock(i), invTotal(inv, item))
                        if (qty > 0 && invDel(inv, item, count = qty).success) {
                            setStock(i, stock(i) + qty)
                            total += qty
                        }
                    }
                }
                mes(
                    if (total == 0) "You have no more suitable logs to store."
                    else "You store $total logs for future flights."
                )
            }
            onOpLoc2(shed) {
                if (!unlocked()) return@onOpLoc2
                val bagFirst =
                    choice2(
                        "Use logs from my inventory first.",
                        true,
                        "Use stored logs first.",
                        false,
                    )
                val saved = vars[BalloonSaves.bins[1]] and 127
                vars[BalloonSaves.bins[1]] = if (bagFirst) saved or 128 else saved
            }
            onOpLoc3(shed) {
                if (unlocked())
                    mes(
                        logs
                            .mapIndexed { i, log -> "${types[log].name}: ${stock(i)}" }
                            .joinToString(", ")
                    )
            }
        }
    }

    private fun ProtectedAccess.unlocked() = player.members && vars[BalloonBits.quest] >= 200

    private fun ProtectedAccess.discovered(port: BalloonPort) =
        BalloonBits.routes[port]?.let { vars[it] != 0 } ?: true

    private fun ProtectedAccess.choose(at: CoordGrid) {
        val from = BalloonPort.near(at) ?: return
        if (!unlocked() || !discovered(from)) {
            mes("You must unlock this balloon route from Entrana first.")
            return
        }
        ifOpenMainModal(BalloonInterfaces.map)
        planning[player] = from
        for ((to, button) in BalloonButtons.ports) {
            if (to != from && (from == BalloonPort.Entrana || discovered(to)))
                ifSetEvents(button, -1..-1, IfEvent.Op1)
        }
    }

    private fun ProtectedAccess.depart(from: BalloonPort, to: BalloonPort) {
        if (!unlocked() || BalloonPort.near(coords) != from || to == from || !discovered(from))
            return
        val first = !discovered(to)
        if (first && from != BalloonPort.Entrana) {
            mes("New routes can be unlocked only from Entrana.")
            return
        }
        if (stat(stats.firemaking) < to.level) {
            mes("You need a Firemaking level of ${to.level} for that flight.")
            return
        }
        if (
            to == BalloonPort.Entrana &&
                (inv.objs + worn.objs).filterNotNull().any {
                    EntranaEquipment.prohibited(types[it])
                }
        ) {
            mes("You cannot take that equipment to Entrana.")
            return
        }
        val cost = if (first) to.firstCost else 1
        val stored = stock(to.fuel)
        val fuel = logs[to.fuel]
        val carried = invTotal(inv, fuel)
        if (stored + carried < cost) {
            mes("You need $cost ${types[fuel].name.lowercase()} for this flight.")
            return
        }
        val bagFirst = vars[BalloonSaves.bins[1]] and 128 != 0
        val fromBag = if (bagFirst) minOf(cost, carried) else maxOf(0, cost - stored)
        if (player.runWeight - types[fuel].weight * fromBag > 40000) {
            mes("The balloon cannot carry more than 40 kg.")
            return
        }
        if (fromBag > 0 && !invDel(inv, fuel, count = fromBag).success) return
        setStock(to.fuel, stored - (cost - fromBag))
        ifClose()
        if (!first) {
            arrive(from, to)
            return
        }
        val flight = BalloonFlight(to)
        ifOpenMainSidePair(BalloonInterfaces.flight, BalloonInterfaces.controls)
        flights[player] = flight
        for (button in BalloonButtons.moves.map { it.first } + BalloonButtons.bail) ifSetEvents(
            button,
            -1..-1,
            IfEvent.Op1,
        )
        render(flight, true)
    }

    private fun ProtectedAccess.move(delta: Int) {
        val f = flights[player] ?: return
        if (delta == 2 && f.bags == 0 || delta == 1 && f.logs == 0) {
            mes("You have run out of ${if(delta==2)"sandbags" else "fuel"}.")
            return
        }
        if (delta == 2) f.bags--
        if (delta == 1) f.logs--
        f.x++
        f.y += delta
        val next = f.x == 20
        if (next) {
            f.screen++
            f.x = 1
        }
        val grid = maps.getValue(f.port to f.screen)
        // The balloon and hanging basket occupy two adjacent cells.
        if (f.y !in 4..10 || grid[f.y][f.x] != 0 || grid[f.y - 1][f.x] != 0) {
            crash(f)
            return
        }
        if (f.screen == 2 && f.x == 19) {
            if (f.y != 5) {
                crash(f)
                return
            }
            flights.remove(player)
            ifClose()
            vars[BalloonBits.routes.getValue(f.port)] = 1
            if (f.port == BalloonPort.GrandTree) vars[BalloonBits.gnomePilot] = 1
            statAdvance(stats.firemaking, 2000.0)
            arrive(BalloonPort.Entrana, f.port)
            return
        }
        // The second and third screens enter over column one; the first
        // movement crossing the seam consumes one action for that new screen.
        render(f, next)
    }

    private fun ProtectedAccess.render(f: BalloonFlight, scene: Boolean) {
        if (scene) {
            val map = maps.getValue(f.port to f.screen)
            for (y in 0..12) for (x in 0..19) {
                val model = map[y][x]
                val button = BalloonButtons.cells[y][x]
                ifSetHide(button, model == 0)
                if (model != 0) player.client.write(IfSetModel(button.packed, model))
            }
        }
        vars[BalloonBits.map] = f.port.mapNumber + f.screen
        vars[BalloonBits.column] = f.x
        vars[BalloonBits.height] = f.y
        vars[BalloonBits.sandbags] = f.bags
        vars[BalloonBits.logs] = f.logs
        ifSetText(BalloonButtons.logsLeft, f.logs.toString())
        ifSetText(BalloonButtons.bagsLeft, f.bags.toString())
        player.client.write(
            IfSetPosition(BalloonButtons.envelope.packed, 13 + 24 * f.x, 8 + 24 * (12 - f.y))
        )
        player.client.write(
            IfSetPosition(BalloonButtons.basket.packed, 13 + 24 * f.x, 8 + 24 * (13 - f.y))
        )
    }

    private fun ProtectedAccess.crash(f: BalloonFlight) {
        flights.remove(player)
        ifClose()
        val rescue =
            when {
                f.screen == 0 && f.x < 4 -> BalloonPort.Entrana.landing
                f.screen == 0 -> BalloonPort.Entrana.landing
                f.port == BalloonPort.CastleWars || f.port == BalloonPort.GrandTree ->
                    CoordGrid(2625, 3243)
                else -> CoordGrid(3068, 3358)
            }
        telejump(rescue)
        mes("Your balloon crashes!")
    }

    private fun ProtectedAccess.arrive(from: BalloonPort, to: BalloonPort) {
        telejump(to.landing)
        if (to == BalloonPort.CastleWars) player.completeDiaryTask(DiaryTask.RideCastleWarsBalloon)
        if (from == BalloonPort.Varrock) player.completeDiaryTask(DiaryTask.FlyFromVarrock)
    }

    private fun ProtectedAccess.stock(slot: Int): Int =
        (vars[BalloonSaves.bins[slot / 4]] ushr ((slot % 4) * 7)) and 127

    private fun ProtectedAccess.setStock(slot: Int, qty: Int) {
        val bin = BalloonSaves.bins[slot / 4]
        val shift = (slot % 4) * 7
        vars[bin] = (vars[bin] and (127 shl shift).inv()) or (qty shl shift)
    }
}
