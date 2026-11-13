package org.rsmod.content.skills.smithing.smelting.blast

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.hands
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ring
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Published costs/recipes/storage. Paid service is locally selected by funding the coffer. World
 * activity flags, exact machine break/heat and conveyor cadence remain approximate.
 */
@Singleton
public class BlastFurnace
@Inject
constructor(
    private val locs: LocRepository,
    private val xp: XpModifiers,
    private val events: EventBus,
    private val shops: Shops,
    private val clock: MapClock,
) : PluginScript() {
    private val damaged = mutableSetOf<Int>()
    private var lastWorldTick = -1
    private var fuelWorkTicks = 0
    private var fuel = 0
    private var heat = 0
    private var pedalled = -1000
    private var pumped = -1000
    private val active = WeakHashMap<Player, Boolean>()
    private val contributors = WeakHashMap<Player, Int>()
    private val bonusAt = WeakHashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpLoc1(BlastLocs.down) {
            if (player.members) {
                telejump(CoordGrid(1940, 4957))
                activate(player)
            }
        }
        onOpLoc1(BlastLocs.up) { telejump(CoordGrid(2931, 10197)) }
        onOpNpc1(BlastNpcs.foreman) { foreman(this) }
        onOpNpc3(BlastNpcs.foreman) { pay(this) }
        onOpNpc3(BlastNpcs.ordan) {
            if (player.members) shops.open(player, "Ore seller.", BlastInvs.shop, 40.0, 150.0, 0.0)
        }
        onOpNpc1(BlastNpcs.ordan) {
            if (player.members && choice2("Trade ores.", true, "Leave.", false))
                shops.open(player, "Ore seller.", BlastInvs.shop, 40.0, 150.0, 0.0)
        }
        onOpLoc1(BlastLocs.belt) { deposit(this, it.loc) }
        for (ore in ORES + objs.smithing_catalyst) onOpLocU(BlastLocs.belt, ore) {
            deposit(this, it.loc)
        }
        for (loc in BlastLocs.dispensers) {
            onOpLoc1(loc) { if (vars[BlastBits.hot] == 0) check(this) else collect(this) }
            onOpLoc2(loc) { check(this) }
            onOpLocU(loc, objs.bucket_water) { cool(this) }
        }
        for (loc in BlastLocs.coffers) {
            onOpLoc1(loc) { coffer(this) }
            onOpLocU(loc, objs.coins) { coffer(this) }
        }
        onOpLoc1(BlastLocs.sink) { fill(this) }
        onOpLocU(BlastLocs.sink, objs.bucket_empty) { fill(this) }
        onOpLoc1(BlastLocs.coke) { coke(this) }
        onOpLocU(BlastLocs.coke, objs.spade) { coke(this) }
        for (stove in BlastLocs.stoves) {
            onOpLoc1(stove) { refuel(this) }
            onOpLocU(stove, BlastObjs.coke) { refuel(this) }
        }
        onOpLoc1(BlastLocs.pedals) { operate(this, it.loc, false) }
        onOpLoc1(BlastLocs.pump) { operate(this, it.loc, true) }
        for (part in BlastLocs.brokenParts) onOpLoc1(part) { repair(this, it.loc) }
        onOpLoc1(BlastLocs.gauge) {
            contribute(player)
            vars[BlastBits.temp] = heat
            ifOpenOverlay(BlastInterfaces.gauge)
        }
        onPlayerSoftTimer(BlastTimers.tick) { tick(player) }
        onPlayerLogin { if (BlastArea.contains(player.coords)) activate(player) }
        onPlayerLogout {
            active.remove(player)
            player.clearSoftTimer(BlastTimers.tick)
        }
    }

    public suspend fun foreman(access: ProtectedAccess): Unit =
        with(access) {
            when (
                choice3(
                    "Can I use the furnace for free?",
                    0,
                    "Pay for ten minutes.",
                    1,
                    "How does it work?",
                    2,
                )
            ) {
                0 -> permission(this)
                1 -> pay(this)
                else ->
                    mes(
                        "Load ore and coal onto the conveyor, operate the machinery or fund the coffer, then cool and collect your bars."
                    )
            }
        }

    public fun permission(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || player.statBase(stats.smithing) < 60) {
                mes("You need an unboosted Smithing level of 60 to use the furnace for free.")
                return false
            }
            vars[BlastVars.permission] = 1
            player.completeDiaryTask(DiaryTask.ObtainFreeBlastFurnacePermission)
            mes("You may use the Blast Furnace without paying my fee.")
            true
        }

    public fun pay(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members) return false
            val cost = if (player.ring?.id == BlastObjs.charos.id) 1250 else 2500
            if (!invDel(inv, objs.coins, cost).success) {
                mes("You need $cost coins.")
                return false
            }
            vars[BlastVars.paid] = 1000
            activate(player)
            mes("You may use the furnace for ten minutes.")
            true
        }

    public fun allowed(p: Player): Boolean =
        p.members &&
            (p.vars[BlastVars.permission] != 0 && p.statBase(stats.smithing) >= 60 ||
                p.vars[BlastVars.paid] > 0)

    public fun activate(p: Player) {
        if (!p.members || !BlastArea.contains(p.coords)) return
        VarPlayerIntMapSetter.set(p, BlastBits.visited, 1)
        if (active.put(p, true) == null) p.softTimer(BlastTimers.tick, 1)
        if (BlastInterfaces.hud !in p.ui) p.ifOpenOverlay(BlastInterfaces.hud, events)
    }

    public suspend fun coffer(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) return
            val mode = choice3("Deposit coins.", 0, "Withdraw coins.", 1, "Check balance.", 2)
            if (mode == 2) {
                mes("The coffer contains " + vars[BlastBits.coins] + " coins.")
                return
            }
            val requested =
                countDialog(
                    if (mode == 0) "How many coins would you like to deposit?"
                    else "How many coins would you like to withdraw?"
                )
            transferCoins(this, requested, mode == 0)
        }

    public fun transferCoins(access: ProtectedAccess, requested: Int, deposit: Boolean): Int =
        with(access) {
            if (!player.members || requested <= 0) return 0
            val old = vars[BlastBits.coins]
            val count =
                minOf(
                    requested,
                    if (deposit) minOf(invTotal(inv, objs.coins), Int.MAX_VALUE - old) else old,
                )
            if (count <= 0) return 0
            val ok =
                if (deposit) invDel(inv, objs.coins, count).success
                else invAdd(inv, objs.coins, count).success
            if (!ok) return 0
            set(player, BlastBits.coins, old + if (deposit) count else -count)
            set(player, BlastBits.hasCoins, if (vars[BlastBits.coins] > 0) 1 else 0)
            activate(player)
            count
        }

    public fun deposit(access: ProtectedAccess, loc: BoundLocInfo): Int =
        with(access) {
            if (
                !allowed(player) ||
                    loc.id != BlastLocs.belt.id ||
                    !BlastArea.contains(coords) ||
                    !isWithinDistance(loc, 1)
            ) {
                mes("Ask the foreman for permission to use the furnace first.")
                return 0
            }
            var deposited = 0
            for ((i, ore) in ORES.withIndex()) {
                if (player.stat(stats.smithing) < LEVELS[i]) continue
                val limit = if (i == 1 || i == 8) 254 else 28
                val count =
                    minOf(invTotal(inv, ore), (limit - vars[BlastBits.ores[i]]).coerceAtLeast(0))
                if (count > 0 && invDel(inv, ore, count).success) {
                    vars[BlastBits.ores[i]] += count
                    deposited += count
                }
            }
            val catalysts =
                minOf(
                    invTotal(inv, objs.smithing_catalyst),
                    (254 - vars[BlastBits.catalysts]).coerceAtLeast(0),
                )
            if (catalysts > 0 && invDel(inv, objs.smithing_catalyst, catalysts).success) {
                vars[BlastBits.catalysts] += catalysts
                deposited += catalysts
            }
            if (deposited > 0) {
                vars[BlastVars.delay] = 2
                activate(player)
            } else
                mes("You have no suitable ore, or you should collect your bars before adding more.")
            deposited
        }

    public fun tick(p: Player) {
        if (!p.members || !BlastArea.contains(p.coords)) {
            p.clearSoftTimer(BlastTimers.tick)
            active.remove(p)
            if (BlastInterfaces.hud in p.ui) p.ifCloseOverlay(BlastInterfaces.hud, events)
            return
        }
        if (p.vars[BlastVars.paid] > 0)
            VarPlayerIntMapSetter.set(p, BlastVars.paid, p.vars[BlastVars.paid] - 1)
        if (lastWorldTick != clock.cycle) {
            lastWorldTick = clock.cycle
            if (clock.cycle % 2 == 0) heat = (heat - 1).coerceAtLeast(0)
        }
        val paid = p.vars[BlastBits.coins] >= 12
        if (paid) {
            repairAll()
            set(p, BlastBits.coins, p.vars[BlastBits.coins] - 12)
            set(p, BlastBits.hasCoins, if (p.vars[BlastBits.coins] > 0) 1 else 0)
        }
        val delay = p.vars[BlastVars.delay]
        if (delay < 0) {
            VarPlayerIntMapSetter.set(p, BlastVars.delay, delay + 1)
            if (delay == -1 && p.vars[BlastBits.hot] == 2) set(p, BlastBits.hot, 3)
        } else if (delay > 0) VarPlayerIntMapSetter.set(p, BlastVars.delay, delay - 1)
        val manual =
            clock.cycle - pedalled <= 3 &&
                clock.cycle - pumped <= 3 &&
                fuel > 0 &&
                heat in 1..200 &&
                damaged.isEmpty()
        if (allowed(p) && (paid || manual) && p.vars[BlastVars.delay] == 0) process(p, !paid)
    }

    public fun process(p: Player, manual: Boolean = false): Int {
        var made = 0
        var experience = 0.0
        fun output(
            ore: Int,
            bar: Int,
            level: Int,
            base: Double,
            coal: Int = 0,
            second: Int? = null,
        ) {
            if (p.stat(stats.smithing) < level) return
            val count =
                minOf(
                    p.vars[BlastBits.ores[ore]],
                    28 - p.vars[BlastBits.bars[bar]],
                    if (coal > 0) p.vars[BlastBits.ores[8]] / coal else 28,
                    if (second != null) p.vars[BlastBits.ores[second]] else 28,
                )
            if (count <= 0) return
            val catalysts = if (coal > 0) minOf(count, p.vars[BlastBits.catalysts]) else 0
            set(p, BlastBits.ores[ore], p.vars[BlastBits.ores[ore]] - count)
            if (second != null)
                set(p, BlastBits.ores[second], p.vars[BlastBits.ores[second]] - count)
            if (coal > 0) set(p, BlastBits.ores[8], p.vars[BlastBits.ores[8]] - count * coal)
            if (catalysts > 0) set(p, BlastBits.catalysts, p.vars[BlastBits.catalysts] - catalysts)
            set(p, BlastBits.bars[bar], p.vars[BlastBits.bars[bar]] + count)
            experience += base * (count + catalysts)
            made += count
        }
        output(0, 0, 1, 6.2, second = 1)
        output(2, 2, 30, 17.5, coal = 1)
        output(2, 1, 15, 12.5)
        output(3, 3, 20, 13.7)
        val gold =
            p.hands?.id == objs.goldsmith_gauntlets.id ||
                p.back?.id in
                    listOf(
                        objs.smithing_skillcape.id,
                        objs.smithing_skillcape_t.id,
                        objs.max_cape_worn.id,
                    )
        output(4, 4, 40, if (gold) 56.2 else 22.5)
        output(5, 5, 50, 30.0, coal = 2)
        output(6, 6, 70, 37.5, coal = 3)
        output(7, 7, 85, 50.0, coal = 4)
        if (made > 0) {
            p.statAdvance(stats.smithing, experience * xp.get(p, stats.smithing))
            set(p, BlastBits.hot, 2)
            VarPlayerIntMapSetter.set(p, BlastVars.delay, -10)
            if (manual)
                for ((other, last) in contributors.toMap()) if (
                    clock.cycle - last <= 10 &&
                        BlastArea.contains(other.coords) &&
                        clock.cycle - (bonusAt[other] ?: -1000) >= 10
                ) {
                    other.statAdvance(stats.smithing, 1.5 * xp.get(other, stats.smithing))
                    bonusAt[other] = clock.cycle
                }
        }
        return made
    }

    public fun collect(access: ProtectedAccess): Int =
        with(access) {
            if (!player.members || !BlastArea.contains(coords)) return 0
            if (
                vars[BlastBits.hot] == 2 &&
                    player.hands?.id !in listOf(objs.ice_gloves.id, BlastObjs.smithIce.id)
            ) {
                mes("The bars are too hot. Cool them with water or wear ice gloves.")
                return 0
            }
            var moved = 0
            for ((i, bar) in BARS.withIndex()) {
                val count = minOf(inv.freeSpace(), vars[BlastBits.bars[i]])
                if (count > 0 && invAdd(inv, bar, count).success) {
                    vars[BlastBits.bars[i]] -= count
                    moved += count
                }
            }
            if (BlastBits.bars.all { vars[it] == 0 }) {
                vars[BlastBits.hot] = 0
                vars[BlastVars.delay] = 0
            }
            if (moved == 0) mes("There are no bars ready, or your inventory is full.")
            moved
        }

    public fun cool(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || vars[BlastBits.hot] != 2 || objs.bucket_water !in inv)
                return false
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.bucket_water.id, 1)
                    add(bag, objs.bucket_empty.id, 1)
                }
            if (tx.success) {
                vars[BlastBits.hot] = 3
                vars[BlastVars.delay] = 0
            }
            tx.success
        }

    public fun fill(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members) return false
            player
                .invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.bucket_empty.id, 1)
                    add(bag, objs.bucket_water.id, 1)
                }
                .success
        }

    public fun check(access: ProtectedAccess): Unit =
        with(access) {
            mes(
                BARS.mapIndexed { i, bar ->
                        listOf(
                            "Bronze",
                            "Iron",
                            "Steel",
                            "Silver",
                            "Gold",
                            "Mithril",
                            "Adamantite",
                            "Runite",
                        )[i] + ": " + vars[BlastBits.bars[i]]
                    }
                    .joinToString(", ")
            )
        }

    public fun coke(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members || player.stat(stats.firemaking) < 30 || objs.spade !in inv)
                return false
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, objs.spade.id, 1)
                    add(bag, BlastObjs.coke.id, 1)
                }
            if (tx.success) anim(BlastSeqs.coke)
            tx.success
        }

    public fun refuel(access: ProtectedAccess): Boolean =
        with(access) {
            if (
                !player.members ||
                    player.stat(stats.firemaking) < 30 ||
                    fuel >= 30 ||
                    BlastObjs.coke !in inv
            )
                return false
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, BlastObjs.coke.id, 1)
                    add(bag, objs.spade.id, 1)
                }
            if (tx.success) {
                fuel++
                heat = (heat + 5).coerceAtMost(200)
                anim(BlastSeqs.refuel)
                statAdvance(stats.firemaking, 5.0)
                contribute(player)
            }
            tx.success
        }

    public suspend fun operate(access: ProtectedAccess, loc: BoundLocInfo, pump: Boolean): Unit =
        with(access) {
            val stat = if (pump) stats.strength else stats.agility
            if (!player.members || player.stat(stat) < 30 || vars[BlastBits.coins] >= 12) {
                mes(
                    "The station requires level 30 and is operated by you when the paid service is not running."
                )
                return
            }
            repeat(10000) {
                if (
                    !BlastArea.contains(coords) ||
                        !isWithinDistance(loc, 1) ||
                        player.stat(stat) < 30 ||
                        (!pump && player.runEnergy < 5)
                )
                    return
                anim(if (pump) BlastSeqs.pump else BlastSeqs.pedal)
                gatheringDelay(1)
                if (!isWithinDistance(loc, 1)) return
                if (pump) {
                    val first = pumped != mapClock
                    pumped = mapClock
                    if (fuel > 0) {
                        if (first) {
                            heat = (heat + 2).coerceAtMost(250)
                            fuelWorkTicks++
                            if (fuelWorkTicks >= 30) {
                                fuel--
                                fuelWorkTicks = 0
                            }
                        }
                        statAdvance(stats.strength, 2.0)
                    }
                } else {
                    pedalled = mapClock
                    player.runEnergy = (player.runEnergy - 5).coerceAtLeast(0)
                    statAdvance(stats.agility, 1.0)
                }
                if (random.of(500) == 0) breakPart(if (pump) random.of(2, 3) else random.of(2))
                contribute(player)
                activate(player)
            }
        }

    /**
     * Break cadence is a 1/500 per working tick model; published repair requirements/XP are exact.
     */
    public fun breakPart(index: Int): Boolean {
        if (index !in PART_COORDS.indices || index in damaged) return false
        val loc = locs.findExact(PART_COORDS[index], BlastLocs.normalParts[index]) ?: return false
        locs.change(loc, BlastLocs.brokenParts[index], Int.MAX_VALUE)
        damaged += index
        return true
    }

    public suspend fun repair(access: ProtectedAccess, loc: BoundLocInfo): Boolean =
        with(access) {
            val index = PART_COORDS.indexOf(loc.coords)
            fun valid(): Boolean =
                player.members &&
                    player.stat(stats.crafting) >= 30 &&
                    objs.hammer in inv &&
                    index >= 0 &&
                    loc.id == BlastLocs.brokenParts[index].id &&
                    isWithinDistance(loc, 1) &&
                    locs.findExact(loc.coords, BlastLocs.brokenParts[index]) != null
            if (!valid()) {
                mes("You need 30 Crafting and a hammer to repair this machinery.")
                return false
            }
            anim(seqs.human_hammer_hit)
            gatheringDelay(3)
            if (!valid()) return false
            locs.change(loc, BlastLocs.normalParts[index], Int.MAX_VALUE)
            damaged.remove(index)
            statAdvance(stats.crafting, 50.0)
            contribute(player)
            true
        }

    private fun repairAll() {
        for (index in damaged.toList()) {
            val loc = locs.findExact(PART_COORDS[index], BlastLocs.brokenParts[index])
            if (loc != null) locs.change(loc, BlastLocs.normalParts[index], Int.MAX_VALUE)
            damaged.remove(index)
        }
    }

    private fun contribute(p: Player) {
        contributors[p] = clock.cycle
    }

    private fun set(p: Player, bit: VarBitType, value: Int) {
        VarPlayerIntMapSetter.set(p, bit, value)
    }

    public companion object {
        public val PART_COORDS: List<CoordGrid> =
            listOf(
                CoordGrid(1944, 4967),
                CoordGrid(1945, 4967),
                CoordGrid(1943, 4961),
                CoordGrid(1947, 4961),
            )
        public val ORES: List<ObjType> =
            listOf(
                objs.copper_ore,
                objs.tin_ore,
                objs.iron_ore,
                objs.silver_ore,
                objs.gold_ore,
                objs.mithril_ore,
                objs.adamantite_ore,
                objs.runite_ore,
                objs.coal,
            )
        public val LEVELS: List<Int> = listOf(1, 1, 15, 20, 40, 50, 70, 85, 1)
        public val BARS: List<ObjType> =
            listOf(
                objs.bronze_bar,
                objs.iron_bar,
                objs.steel_bar,
                objs.silver_bar,
                objs.gold_bar,
                objs.mithril_bar,
                objs.adamantite_bar,
                objs.runite_bar,
            )
    }
}
