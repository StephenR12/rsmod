package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueVessels
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.*
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.queue.QueueReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeFastIntervalChance
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeInterval
import org.rsmod.content.skills.pets.SkillPetRates
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object StarClock : ControllerBuilder() {
    val falls = build("shooting_star_falls")
}

internal object StarMining : QueueReferences() {
    val swing = find("shooting_star_mine")
}

internal object StarMineBits : VarBitReferences() {
    val bonus = find("star_bonus_dust")
}

internal object StarSaves : VarpReferences() {
    val discoveries = find("shooting_star_discoveries")
}

internal object StarCount : VarpBuilder() {
    init {
        build("shooting_star_discoveries") { permanent = true }
    }
}

internal object StarUi : InterfaceReferences() {
    val board = find("star_noticeboard")
}

internal object StarBoard : ComponentReferences() {
    val names = (1..5).map { find("star_noticeboard:star_finder_$it") }
    val times = (1..5).map { find("star_noticeboard:star_time_$it") }
    val count = find("star_noticeboard:star_personal_count")
}

internal data class Landing(val region: String, val tile: CoordGrid)

private data class MiningStar(val tile: CoordGrid, val size: Int)

public class ShootingStars
@Inject
constructor(
    private val clock: MapClock,
    private val rng: GameRandom,
    private val controllers: ControllerRepository,
    private val locs: LocRepository,
    private val types: LocTypeList,
    private val items: ObjTypeList,
    private val vessels: ClueVessels,
    private val xp: XpModifiers,
    private val pets: SkillPetRates,
    private val crystal: CrystalPickaxeLifecycle,
    private val celestial: CelestialMiningRing,
    private val events: org.rsmod.events.EventBus,
) : PluginScript() {
    private val landings =
        ShootingStars::class
            .java
            .getResourceAsStream("star-sites.tsv")!!
            .bufferedReader()
            .useLines { lines ->
                lines
                    .drop(1)
                    .map { line ->
                        val (region, x, z) = line.split('\t')
                        Landing(region, CoordGrid(x.toInt(), z.toInt()))
                    }
                    .toList()
            }
    private val darkmeyer = landings.single { it.tile == CoordGrid(3635, 3340) }
    private val ordinarySites = landings - darkmeyer
    private val stars = intArrayOf(41229, 41228, 41227, 41226, 41225, 41224, 41223, 41021, 41020)
    private val discoveries = ArrayDeque<Pair<String, Int>>()
    private lateinit var next: Landing
    private var nextAt = 0
    private var size = 0
    private var active: LocInfo? = null
    private var site: Landing? = null
    private var worked = 0
    private var mineUntil = 0
    private var found = false

    override fun ScriptContext.startup() {
        onGameStartup {
            schedule(rng.of(2000..5000))
            val con = Controller(StarClock.falls, CoordGrid(3022, 3342))
            controllers.add(con, Int.MAX_VALUE)
            con.aiTimer(nextAt - clock.cycle)
        }
        onAiConTimer(StarClock.falls) {
            if (clock.cycle >= nextAt) {
                active?.let { locs.del(it, Int.MAX_VALUE) }
                land(next)
                schedule(rng.of(7500..10500))
            } else if (active != null && clock.cycle <= mineUntil) {
                worked++
                if (worked == 700) peel()
            }
            controller.aiTimer(if (active == null) nextAt - clock.cycle else 1)
        }
        for (id in stars) {
            onOpLoc1(types.getValue(id)) {
                if (active?.coords != it.loc.coords || active?.id != id) return@onOpLoc1
                if (!found) {
                    found = true
                    vars[StarMineBits.bonus] = 300
                    vars[StarSaves.discoveries] += 1
                    discoveries.addFirst(player.displayName to clock.cycle)
                    if (discoveries.size > 5) discoveries.removeLast()
                    mes(
                        "You are the first to discover this star. You will receive double stardust for the next 300 pieces you mine."
                    )
                }
                val request = MiningStar(it.loc.coords, size)
                clearWeakQueue(StarMining.swing)
                swing(request, false)
            }
            onOpLoc2(types.getValue(id)) {
                if (active?.coords == it.loc.coords)
                    mes(
                        "This is a size $size star. ${worked*100/700}% of this layer has been mined."
                    )
            }
        }
        onPlayerQueueWithArgs<MiningStar>(StarMining.swing) { swing(it.args, true) }
        onOpLoc1(types.getValue(41230)) {
            ifOpenMainModal(StarUi.board)
            for (i in 0..4) {
                val row = discoveries.getOrNull(i)
                ifSetText(StarBoard.names[i], row?.first ?: "-")
                ifSetText(
                    StarBoard.times[i],
                    if (row == null) "-" else "${(clock.cycle-row.second)/100} minutes ago",
                )
            }
            ifSetText(StarBoard.count, "Stars discovered: ${vars[StarSaves.discoveries]}")
        }
        for ((i, width) in listOf(24, 9, 2).withIndex()) onOpLoc1(types.getValue(13656 + i)) {
            if (!player.members || !::next.isInitialized) return@onOpLoc1
            val view = org.rsmod.api.player.events.TelescopeObservation(player, it.loc)
            events.publish(view)
            if (view.blocked) {
                mes("The telescope doesn't have a clear view of the sky.")
                return@onOpLoc1
            }
            val left = (nextAt - clock.cycle) / 100
            if (left > 60) {
                mes("You look through the telescope but you don't see anything interesting.")
                return@onOpLoc1
            }
            val lower = (left / width) * width
            mes(
                "You see a shooting star! It looks like it will land in ${next.region} in $lower to ${lower+width} minutes."
            )
        }
    }

    private fun schedule(inTicks: Int) {
        val candidate = ordinarySites[rng.of(ordinarySites.size)]
        next = if (candidate == site) darkmeyer else candidate
        nextAt = clock + inTicks
    }

    private fun land(where: Landing) {
        size =
            when (rng.of(100)) {
                in 0..19 -> 9
                in 20..44 -> 8
                in 45..74 -> 7
                else -> 6
            }
        worked = 0
        mineUntil = 0
        found = false
        site = where
        active =
            locs.add(
                where.tile,
                types.getValue(stars[size - 1]),
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
    }

    private fun peel() {
        val old = active ?: return
        worked = 0
        mineUntil = 0
        size--
        if (size == 0) {
            locs.del(old, Int.MAX_VALUE)
            active = null
            return
        }
        active =
            locs.add(
                old.coords,
                types.getValue(stars[size - 1]),
                Int.MAX_VALUE,
                old.angle,
                old.shape,
            )
    }

    private fun ProtectedAccess.swing(request: MiningStar, roll: Boolean) {
        val loc = active ?: return
        if (
            loc.coords != request.tile ||
                size != request.size ||
                player.coords.level != loc.coords.level ||
                player.coords.x !in loc.coords.x - 1..loc.coords.x + 2 ||
                player.coords.z !in loc.coords.z - 1..loc.coords.z + 2
        )
            return
        if (stat(stats.mining) < size * 10) {
            mes("You need level ${size*10} Mining for this layer.")
            return
        }
        val tool = Mining.findPickaxe(player, items)
        if (tool == null) {
            mes("You need a pickaxe that you can use.")
            return
        }
        if (inv.isFull() && StarItems.dust !in inv) {
            mes("Your inventory is too full to hold the stardust.")
            return
        }
        val pickaxe = items[tool]
        if (roll && statRandom(stats.mining, 73, 117, celestial.invisibleBoost(player))) {
            val bonus = vars[StarMineBits.bonus] > 0
            val newSlot = if (StarItems.dust in inv) -1 else inv.indexOfFirst { it == null }
            if (!invAdd(inv, StarItems.dust, if (bonus) 2 else 1).success) return
            if (bonus) vars[StarMineBits.bonus] -= 1
            crystal.consumeCharge(this, tool)
            statAdvance(
                stats.mining,
                (if (player.members) 32.0 else 16.0) * xp.get(player, stats.mining),
            )
            pets.roll(player, stats.mining, 13321, 521550, newSlot)
            vessels.geodes(player, stat(stats.mining), 521550)
            if (player.members && site?.region == "Varlamore" && random.of(3400) == 0)
                invAdd(inv, items.getValue(30107))
            if (random.of(Mining.gemPreRollDenominator(player)) < MiningGemPreTable.TOTAL_WEIGHT)
                MiningGemPreTable.at(random.of(MiningGemPreTable.TOTAL_WEIGHT)).obj?.let {
                    invAdd(inv, it)
                }
        }
        anim(pickaxe.pickaxeAnim)
        val fast = pickaxe.pickaxeFastIntervalChance
        val interval =
            pickaxe.pickaxeInterval - if (fast > 0 && random.randomBoolean(fast)) 1 else 0
        mineUntil = maxOf(mineUntil, clock + interval)
        weakQueue(StarMining.swing, interval, request)
    }
}
