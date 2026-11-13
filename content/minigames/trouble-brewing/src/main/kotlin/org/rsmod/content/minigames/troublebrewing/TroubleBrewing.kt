package org.rsmod.content.minigames.troublebrewing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.*
import org.rsmod.api.player.events.*
import org.rsmod.api.player.hat
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.*
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

private data class Brewer(
    val crew: Int,
    val halfway: Boolean = false,
    var earned: Int = 0,
    var xpThirds: Int = 0,
    var allowance: Int = 0,
)

private class Still {
    var logs = 75
    var flower = 0
    val fuel = mutableMapOf<CoordGrid, Int>()
    val lit = mutableSetOf<CoordGrid>()
    val ingredients = IntArray(5)
    var due = 0
    var bottle = false
    var packed = 0
}

private val doors = listOf(CoordGrid(3824, 3015), CoordGrid(3803, 3015))
private val bases = listOf(CoordGrid(3817, 2996), CoordGrid(3815, 2955))
private val belt = listOf(CoordGrid(3821, 3003), CoordGrid(3812, 2948))
private val costs = intArrayOf(5, 3, 1, 1, 1)
private val table =
    listOf(
        objs.bucket_empty,
        objs.bowl_empty,
        objs.raw_beef,
        objs.knife,
        objs.bronze_axe,
        objs.tinderbox,
    )

private fun crewAt(c: CoordGrid) = if (c.z >= 2976) 0 else 1

public class TroubleBrewing
@Inject
constructor(
    private val clock: MapClock,
    private val events: EventBus,
    private val collision: CollisionFlagMap,
    private val locs: LocRepository,
    private val ground: ObjRepository,
    private val types: ObjTypeList,
    private val random: GameRandom,
) : PluginScript() {
    private val waiting = linkedMapOf<Player, Int>()
    private val players = linkedMapOf<Player, Brewer>()
    private var stills = List(2) { Still() }
    private val personalAxes = mutableSetOf<Player>()
    private var starts = 0
    private var ends = 0
    private var ticked = -1

    override fun ScriptContext.startup() {
        for ((i, crew) in BrewNpcs.crew.withIndex()) {
            onOpNpc1(crew) { join(i) }
            onOpNpc3(crew) { join(i) }
        }
        onOpNpc1(BrewNpcs.jimmy) {
            when (
                choice3(
                    "Join the quieter crew.",
                    1,
                    "Show me your rewards.",
                    2,
                    "How many are waiting?",
                    3,
                )
            ) {
                1 -> join(quieter())
                2 -> shop()
                else ->
                    mes(
                        "Blue crew: ${waiting.values.count{it==0}}. Red crew: ${waiting.values.count{it==1}}."
                    )
            }
        }
        onOpNpc3(BrewNpcs.jimmy) { shop() }
        onOpNpc4(BrewNpcs.jimmy) { join(quieter()) }
        onOpLoc1(BrewLocs.door) { if (player in waiting) leave(player) }
        onOpLoc1(BrewLocs.portal) { if (player in players) leave(player) }
        for ((i, ladder) in BrewLocs.stairs.withIndex()) onOpLoc1(ladder) {
            if (player !in players) return@onOpLoc1
            val c = it.loc.coords
            telejump(CoordGrid(c.x + 1, c.z, if (i < 2) 2 else 0))
        }
        for ((i, hat) in BrewObjs.hats.withIndex()) onOpHeld1(hat) {
            if (players[player]?.crew == i)
                mes("You are part of the ${if(i==0)"blue" else "red"} crew.")
        }
        onPlayerSoftTimer(BrewTimer.tick) {
            if (ticked == clock.cycle) return@onPlayerSoftTimer
            ticked = clock.cycle
            tick()
        }
        onPlayerLogout { if (player in waiting || player in players) leave(player) }
        onPlayerLogin { if (player.vars[BrewVars.crew] > 0) leave(player) }
        onEvent<PlayerTeleportEvent.Start> {
            if (player in waiting || player in players) leave(player, false)
        }
        onEvent<PlayerDeathEvent.ResolveRespawn> {
            if (player in players) {
                leave(player, false)
                safeDestination = doors[0]
            }
        }
        onEvent<PlayerCombatPermission> {
            if (attacker in players || target in players) allowed = false
        }
        production()
    }

    private fun quieter() =
        if (waiting.values.count { it == 0 } <= waiting.values.count { it == 1 }) 0 else 1

    private fun ProtectedAccess.join(i: Int) {
        if (!player.members || vars[BrewVars.quest] < 140 || player.statBase(stats.cooking) < 40) {
            mes("You need Cabin Fever and level 40 Cooking to join a crew.")
            return
        }
        if (player in players || player in waiting) return
        if (inv.isNotEmpty() || player.hat != null || vars[BrewBits.follower] != 0) {
            mes("Bank your inventory and headgear and dismiss your follower first.")
            return
        }
        if (player.righthand?.id == objs.bronze_axe.id) personalAxes.add(player)
        waiting[player] = i
        vars[BrewVars.crew] = i + 1
        player.hat = InvObj(BrewObjs.hats[i])
        telejump(doors[i].translate(if (i == 0) 1 else -1, 2))
        player.ifOpenOverlay(BrewUI.wait, events)
        player.softTimer(BrewTimer.tick, 1)
        if (ends == 0 && starts == 0 && waiting.values.containsAll(listOf(0, 1)))
            starts = clock.cycle + 300
    }

    private fun admit(p: Player, half: Boolean) {
        val crew = waiting.remove(p) ?: return
        p.ifCloseOverlay(BrewUI.wait, events)
        players[p] = Brewer(crew, half)
        move(p, bases[crew])
        p.ifOpenOverlay(BrewUI.game, events)
    }

    private fun tick() {
        if (ends == 0) {
            if (starts > 0 && clock.cycle >= starts) {
                starts = 0
                if (waiting.values.containsAll(listOf(0, 1))) {
                    ends = clock.cycle + 2000
                    stills = List(2) { Still() }
                    for (p in waiting.keys.toList()) admit(p, false)
                }
            }
            for (p in waiting.keys) if (clock.cycle % 10 == 0)
                p.ifSetText(
                    BrewHud.wait,
                    if (starts == 0) "Waiting for both crews" else "${(starts-clock.cycle)*6/10}s",
                )
            return
        }
        val remaining = ends - clock.cycle
        if (remaining == 1000) for (p in waiting.keys.toList()) admit(p, true)
        if (remaining <= 0) {
            for ((p, b) in players.toMap()) {
                val packed = stills[b.crew].packed
                val eight = (b.earned.coerceAtMost(100) + 10 * packed) / (if (b.halfway) 2 else 1)
                val sets = minOf(b.xpThirds / 3, packed)
                leave(p)
                if (eight > 0)
                    VarPlayerIntMapSetter.set(
                        p,
                        BrewVars.pieces,
                        (p.vars[BrewVars.pieces].toLong() + eight)
                            .coerceAtMost(Int.MAX_VALUE.toLong())
                            .toInt(),
                    )
                if (sets > 0) {
                    p.statAdvance(stats.crafting, 100.0 * sets)
                    p.statAdvance(stats.cooking, 250.0 * sets)
                    p.statAdvance(stats.firemaking, 150.0 * sets)
                }
                p.completeDiaryTask(DiaryTask.CompleteTroubleBrewing)
                p.mes("Your crew packed $packed rum. You earned $eight pieces of eight.")
            }
            ends = 0
            starts = if (waiting.values.containsAll(listOf(0, 1))) clock.cycle + 300 else 0
            return
        }
        for ((crew, still) in stills.withIndex()) {
            if (still.due != 0 && clock.cycle >= still.due) {
                still.due = 0
                still.bottle = true
                locs.add(
                    belt[crew],
                    BrewLocs.bottle,
                    67,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
            }
            if (
                still.due == 0 &&
                    still.fuel.size == 3 &&
                    still.lit.size == 3 &&
                    still.fuel.values.all { it >= 3 } &&
                    costs.indices.all { still.ingredients[it] >= costs[it] }
            ) {
                costs.indices.forEach { still.ingredients[it] -= costs[it] }
                still.due = clock.cycle + 67
            }
        }
        if (remaining % 10 == 0)
            for ((p, _) in players) {
                p.ifSetText(
                    BrewHud.time,
                    "${remaining/100}:${(remaining%100*6/10).toString().padStart(2,'0')}",
                )
                for (i in 0..1) p.ifSetText(BrewHud.scores[i], stills[i].packed.toString())
            }
    }

    private fun leave(p: Player, returnToDoor: Boolean = true) {
        val crew =
            players.remove(p)?.crew
                ?: waiting.remove(p)
                ?: (p.vars[BrewVars.crew] - 1).coerceIn(0, 1)
        if (returnToDoor) move(p, doors[crew])
        if (p.hat?.id in listOf(8949, 8950)) p.hat = null
        // Entry requires an empty inventory. Clear only objects provided by this activity.
        val cleanup =
            table.map { it.id }.toSet() +
                setOf(1511, 1929, 8934, 8936, 8938, 8942, 8943, 8946, 8972, 8974, 8976, 8977, 8981)
        var protectAxe = personalAxes.remove(p) && p.righthand?.id != objs.bronze_axe.id
        for (slot in p.inv.indices) {
            val id = p.inv[slot]?.id ?: continue
            if (id == objs.bronze_axe.id && protectAxe) protectAxe = false
            else if (id in cleanup) p.inv[slot] = null
        }
        p.ifCloseOverlay(BrewUI.game, events)
        p.ifCloseOverlay(BrewUI.wait, events)
        p.clearSoftTimer(BrewTimer.tick)
        VarPlayerIntMapSetter.set(p, BrewVars.crew, 0)
        VarPlayerIntMapSetter.set(p, BrewBits.score, 0)
    }

    private fun move(p: Player, c: CoordGrid) = PathingEntityCommon.telejump(p, collision, c)

    private fun ScriptContext.production() {
        onOpLoc1(BrewLocs.tools) {
            if (player !in players) return@onOpLoc1
            val i = menu("Take a tool", true, table.map { types[it].name })
            table.getOrNull(i)?.let { invAdd(inv, it) }
        }
        onOpLoc1(BrewLocs.logstore) {
            val b = players[player] ?: return@onOpLoc1
            if (b.crew != crewAt(it.loc.coords)) return@onOpLoc1
            val still = stills[b.crew]
            if (still.logs > 0 && invAdd(inv, objs.logs).success) still.logs--
        }
        onOpLocU(BrewLocs.logstore, objs.logs) {
            val b = players[player] ?: return@onOpLocU
            if (b.crew == crewAt(it.loc.coords) && invDel(inv, objs.logs).success)
                stills[b.crew].logs++
        }
        onOpLocU(BrewLocs.pump, objs.bucket_empty) {
            if (player in players) invReplace(inv, objs.bucket_empty, 1, objs.bucket_water)
        }
        for ((i, flower) in BrewLocs.flower.withIndex()) onOpLoc1(flower) {
            if (players[player]?.crew == i && invTotal(inv, BrewObjs.flowers[i]) < 5)
                invAdd(inv, BrewObjs.flowers[i])
        }
        for ((i, flower) in BrewObjs.flowers.withIndex()) onOpLocU(BrewLocs.kettle, flower) {
            if (
                players[player]?.crew == i &&
                    crewAt(it.loc.coords) == i &&
                    invDel(inv, flower).success
            )
                stills[i].flower++
        }
        onOpLocU(BrewLocs.kettle, objs.bowl_empty) {
            val crew = players[player]?.crew ?: return@onOpLocU
            if (
                crew == crewAt(it.loc.coords) &&
                    stills[crew].flower > 0 &&
                    invReplace(inv, objs.bowl_empty, 1, BrewObjs.colours[crew]).success
            )
                stills[crew].flower--
        }
        onOpNpc1(BrewNpcs.monkey) {
            if (
                player in players && BrewObjs.monkey !in inv && invAdd(inv, BrewObjs.monkey).success
            ) {
                statAdvance(stats.agility, 15.0)
                statAdvance(stats.hunter, 15.0)
            }
        }
        for ((i, colour) in BrewObjs.colours.withIndex()) onOpHeldU(BrewObjs.monkey, colour) {
            if (players[player]?.crew == i)
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, colour.id, 1)
                    delete(bag, BrewObjs.monkey.id, 1)
                    add(bag, BrewObjs.monkeys[i].id, 1)
                    add(bag, objs.bowl_empty.id, 1)
                }
        }
        for ((i, monkey) in BrewObjs.monkeys.withIndex()) onOpLocU(BrewLocs.nut, monkey) {
            if (players[player]?.crew == i && invReplace(inv, monkey, 1, BrewObjs.nuts).success)
                statAdvance(stats.hunter, 50.0)
        }
        onOpLocU(BrewLocs.mound, objs.raw_beef) {
            if (player in players && invReplace(inv, objs.raw_beef, 1, BrewObjs.grubs).success)
                statAdvance(stats.hunter, 40.0)
        }
        for (tree in BrewLocs.jungle + BrewLocs.scrapey) onOpLoc1(tree) {
            if (player !in players) return@onOpLoc1
            val axe =
                (inv + worn).filterNotNull().any { o ->
                    types.getValue(o.id).name.endsWith(" axe", true)
                }
            if (!axe) {
                mes("You need an axe.")
                return@onOpLoc1
            }
            if (invAdd(inv, if (tree == BrewLocs.scrapey) BrewObjs.wood else objs.logs).success)
                statAdvance(stats.woodcutting, 25.0)
        }
        onOpHeldU(objs.knife, BrewObjs.wood) {
            if (player in players && invReplace(inv, BrewObjs.wood, 1, BrewObjs.bark).success)
                statAdvance(stats.fletching, 50.0)
        }
        for ((i, boiler) in (BrewLocs.boilers + BrewLocs.logboilers + BrewLocs.fires).withIndex()) {
            onOpLocU(boiler, objs.logs) {
                val c = it.loc.coords
                val crew = players[player]?.crew ?: return@onOpLocU
                val still = stills[crew]
                val fuel = still.fuel.getOrDefault(c, 0)
                if (crew != crewAt(c) || fuel >= 10 || invDel(inv, objs.logs).failure)
                    return@onOpLocU
                still.fuel[c] = fuel + 1
                if (c in still.lit) {
                    statAdvance(stats.firemaking, 100.0)
                    points(player, 1)
                } else
                    locs.change(
                        it.loc,
                        BrewLocs.logboilers[i % 4],
                        (ends - clock.cycle).coerceAtLeast(1),
                    )
            }
            onOpLocU(boiler, objs.tinderbox) {
                val c = it.loc.coords
                val crew = players[player]?.crew ?: return@onOpLocU
                val still = stills[crew]
                if (crew != crewAt(c) || still.fuel.getOrDefault(c, 0) == 0 || !still.lit.add(c))
                    return@onOpLocU
                val fuel = still.fuel.getValue(c)
                points(player, fuel)
                statAdvance(stats.firemaking, fuel * 100.0)
                locs.change(it.loc, BrewLocs.fires[i % 4], (ends - clock.cycle).coerceAtLeast(1))
            }
        }
        for ((crew, hopper) in BrewLocs.hopper.withIndex()) {
            val supplies =
                listOf(
                    objs.bucket_water,
                    BrewObjs.colours[crew],
                    BrewObjs.grubs,
                    BrewObjs.bark,
                    BrewObjs.nuts,
                )
            for ((i, item) in supplies.withIndex()) onOpLocU(hopper, item) {
                val b = players[player] ?: return@onOpLocU
                if (b.crew != crew) return@onOpLocU
                val expected = hopperIndex(crew, it.loc.coords)
                if (expected != i) {
                    mes("That ingredient belongs in a different hopper.")
                    return@onOpLocU
                }
                val result =
                    when (i) {
                        0 -> invReplace(inv, item, 1, objs.bucket_empty)
                        1 -> invReplace(inv, item, 1, objs.bowl_empty)
                        else -> invDel(inv, item)
                    }
                if (result.failure) return@onOpLocU
                stills[crew].ingredients[i]++
                points(player, 1)
                b.allowance++
                if (i != 0) {
                    b.xpThirds += if (i == 1) 1 else 3
                    statAdvance(stats.cooking, if (i == 4) 200.0 else 100.0)
                }
            }
        }
        onOpLoc1(BrewLocs.bottle) {
            val b = players[player] ?: return@onOpLoc1
            val still = stills[b.crew]
            if (crewAt(it.loc.coords) != b.crew || !still.bottle || b.allowance <= 0)
                return@onOpLoc1
            b.allowance--
            still.bottle = false
            still.packed++
            locs.del(it.loc, Int.MAX_VALUE)
            for ((p, member) in players) if (member.crew == b.crew)
                p.mes("Your crew packed a bottle of rum.")
        }
        for (rum in BrewObjs.rum) onOpHeld1(rum) {
            if (player.members && invDel(inv, rum).success) telejump(CoordGrid(3813, 3021))
        }
    }

    private fun hopperIndex(crew: Int, c: CoordGrid): Int {
        val positions =
            if (crew == 0)
                listOf(3811 to 3000, 3811 to 3003, 3815 to 3003, 3813 to 3003, 3817 to 3003)
            else listOf(3822 to 2951, 3822 to 2948, 3818 to 2948, 3820 to 2948, 3816 to 2948)
        return positions.indexOf(c.x to c.z)
    }

    private fun points(p: Player, n: Int) {
        val b = players[p] ?: return
        b.earned = (b.earned + n).coerceAtMost(100)
        VarPlayerIntMapSetter.set(p, BrewBits.score, b.earned)
    }

    private suspend fun ProtectedAccess.shop() {
        if (!player.members) return
        val goods =
            (BrewObjs.rum + BrewObjs.yeast + BrewObjs.clothing + BrewObjs.flags).map {
                it to types[it].cost * 10
            }
        val i =
            menu(
                "Honest Jimmy's House of Stuff",
                true,
                goods.map { (obj, cost) -> "${types[obj].name} ($cost pieces of eight)" },
            )
        val (obj, price) = goods.getOrNull(i) ?: return
        if (vars[BrewVars.pieces] < price) {
            mes("You need more pieces of eight.")
            return
        }
        if (invAdd(inv, obj).success) vars[BrewVars.pieces] -= price
        else mes("Make room in your inventory.")
    }
}
