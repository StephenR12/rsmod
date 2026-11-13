package org.rsmod.content.skills.fishing.driftnet

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.LocalDate
import java.time.ZoneOffset
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.VarPlayerIntMapSetter.set
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.type.builders.inv.InvBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.region.Region
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

internal object DriftBits : VarBitReferences() {
    val quest = find("fossilquest_progress")
    val life = find("fossil_driftnet_unlimitedaccess")
    val stored = find("fossil_driftnet_store")
    val nets = listOf(find("fossil_drift_net1"), find("fossil_drift_net2"))
    val fish = listOf(find("fossil_drift_net1_catch"), find("fossil_drift_net2_catch"))
    val oxygen = find("fossil_underwater_oxygen")
}

internal object DriftVars : VarpReferences() {
    val day = find("drift_net_access_day")
    val cave = find("drift_net_instance")
    val route = find("fossil_island_rowboat_unlocked")
}

internal object DriftSaves : VarpBuilder() {
    init {
        for (name in
            listOf(
                "drift_net_access_day",
                "drift_net_instance",
                "fossil_island_rowboat_unlocked",
            )) build(name) { permanent = true }
    }
}

internal object DriftInv : InvReferences() {
    val harvest = find("drift_net_harvest")
}

internal object DriftStorage : InvBuilder() {
    init {
        build("drift_net_harvest") {
            size = 10
            scope = InvScope.Perm
            protect = false
            stack = InvStackType.Always
        }
    }
}

internal object DriftTimer : TimerReferences() {
    val life = find("drift_net_lifecycle")
}

internal object DriftUi : InterfaceReferences() {
    val oxygen = find("fossil_underwater")
    val harvest = find("fossil_driftnet")
    val store = find("fossil_driftnet_store")
    val side = find("fossil_driftnet_sidestore")
}

internal object DriftSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DriftSpawns>("ceto.toml")
    }
}

internal class DriftRoom(val region: Region) {
    val shoals = mutableMapOf<Npc, CoordGrid>()

    data class Chase(val net: Int, val until: Int)

    val chased = mutableMapOf<Npc, Chase>()

    fun tile(x: Int, z: Int) = region.normal[1, 58, 160, x - 3712, z - 10240]

    fun anchor(i: Int) = if (i == 0) tile(3746, 10295) else tile(3743, 10288)
}

@Singleton
internal class DriftNetCave
@Inject
constructor(
    private val regions: RegionRepository,
    private val creatures: NpcRepository,
    private val rows: org.rsmod.game.type.dbrow.DbRowTypeList,
    private val types: NpcTypeList,
    private val objects: ObjTypeList,
    private val clock: MapClock,
    private val random: GameRandom,
    private val collision: CollisionFlagMap,
    private val events: EventBus,
) {
    private val rooms = mutableMapOf<Player, DriftRoom>()
    private val underwater = mutableSetOf<Player>()

    fun room(p: Player) = rooms[p]

    fun questComplete(p: Player) =
        p.members &&
            p.vars[DriftBits.quest] >= (rows.getValue(11).data.getValue(19).single() as Int)

    fun eligible(p: Player) =
        questComplete(p) && p.statBase(stats.hunter) >= 44 && p.statBase(stats.fishing) >= 47

    fun permitted(p: Player) = p.vars[DriftBits.life] != 0 || p.vars[DriftVars.day] == today()

    fun today() = LocalDate.now(ZoneOffset.UTC).toEpochDay().toInt()

    fun start(p: Player) {
        p.softTimer(DriftTimer.life, 1)
    }

    fun dive(p: Player) {
        if (!questComplete(p)) return
        PathingEntityCommon.telejump(p, collision, SUBMERGED)
        oxygen(p)
        start(p)
    }

    private fun oxygen(p: Player) {
        if (underwater.add(p)) {
            set(p, DriftBits.oxygen, 1000)
            p.ifOpenOverlay(DriftUi.oxygen, events)
        }
    }

    fun leave(p: Player, dest: CoordGrid?) {
        rooms.remove(p)?.let { r ->
            for (n in r.shoals.keys) if (n.isSlotAssigned) creatures.del(n, Int.MAX_VALUE)
        }
        set(p, DriftVars.cave, 0)
        if (dest != null) PathingEntityCommon.telejump(p, collision, dest)
        if (!inWater(p.coords) && underwater.remove(p)) p.ifCloseOverlay(DriftUi.oxygen, events)
    }

    fun logout(p: Player) {
        if (p in rooms) leave(p, ENTRANCE)
        underwater.remove(p)
    }

    fun login(p: Player) {
        if (p.vars[DriftVars.cave] != 0) leave(p, ENTRANCE)
        if (inWater(p.coords)) oxygen(p)
        if (inWater(p.coords) || DriftBits.nets.any { p.vars[it] in 1..2 }) start(p)
    }

    fun enter(p: Player) {
        if (!eligible(p) || !permitted(p) || p in rooms) return
        val region =
            regions.add(
                RegionTemplate.create {
                    copy(466, 1285, 1) {
                        zoneWidth = 5
                        zoneLength = 4
                        regionZoneX = 4
                        regionZoneZ = 4
                    }
                }
            ) ?: return
        region.reclaimFromCycle = clock.cycle + 6
        val room = DriftRoom(region)
        rooms[p] = room
        set(p, DriftVars.cave, 1)
        PathingEntityCommon.telejump(p, collision, room.tile(3737, 10299))
        for ((x, z) in SHOALS) {
            val c = room.tile(x, z)
            val n = Npc(types.getValue(7782), c)
            creatures.add(n, Int.MAX_VALUE)
            n.respawns = false
            room.shoals[n] = c
        }
        oxygen(p)
        start(p)
    }

    fun chase(p: Player, n: Npc) {
        val r = room(p) ?: return
        if (n !in r.shoals || n in r.chased) return
        val net =
            DriftBits.nets.indices
                .filter { p.vars[DriftBits.nets[it]] in 1..2 }
                .minByOrNull { n.coords.chebyshevDistance(r.anchor(it)) }
        if (net == null) {
            p.mes("Set up a drift net with room for more fish.")
            return
        }
        val boosted = p.worn.any { it?.id in HARPOONS }
        if (random.of(100) >= if (boosted) 90 else 65) {
            p.mes("The shoal evades you.")
            return
        }
        val target = if (net == 0) r.tile(3745, 10296) else r.tile(3744, 10293)
        n.walk(target)
        r.chased[n] = DriftRoom.Chase(net, clock.cycle + 14)
    }

    fun tick(p: Player) {
        val r = room(p)
        if (r != null) {
            if (
                p.coords.x !in r.region.southWest.x until r.region.northEast.x ||
                    p.coords.z !in r.region.southWest.z until r.region.northEast.z ||
                    p.coords.level != 1
            ) {
                leave(p, null)
            } else
                for ((n, chase) in r.chased.toMap()) {
                    val near = n.coords.chebyshevDistance(r.anchor(chase.net)) <= 4
                    if (!near && clock.cycle < chase.until) continue
                    if (near) add(p, chase.net, chased = true)
                    r.chased.remove(n)
                    PathingEntityCommon.telejump(n, collision, r.shoals.getValue(n))
                }
        }
        if (room(p) != null || inWater(p.coords)) {
            oxygen(p)
            if (clock.cycle % 3 == 0) {
                val geared = objects.getValue(7534) in p.worn && objects.getValue(7535) in p.worn
                val air = if (geared) 1000 else (p.vars[DriftBits.oxygen] - 10).coerceAtLeast(0)
                set(p, DriftBits.oxygen, air)
                if (air == 0) {
                    val tears = objects.getValue(21656)
                    val loss = minOf(p.inv.count(tears), random.of(10..20))
                    if (loss > 0) p.invDel(p.inv, tears, loss)
                    leave(p, ISLAND)
                    p.mes("You run out of oxygen and surface by the rowboat.")
                }
            }
        } else if (underwater.remove(p)) p.ifCloseOverlay(DriftUi.oxygen, events)
        // Untended nets continue catching while their owner is online, including outside the cave.
        for (i in DriftBits.nets.indices) if (
            p.vars[DriftBits.nets[i]] in 1..2 && random.of(400) == 0
        )
            add(p, i, chased = false)
        if (room(p) != null || inWater(p.coords) || DriftBits.nets.any { p.vars[it] in 1..2 })
            start(p)
    }

    private fun add(p: Player, i: Int, chased: Boolean) {
        if (!p.members || p.vars[DriftBits.nets[i]] !in 1..2) return
        val n = (p.vars[DriftBits.fish[i]] + 1).coerceAtMost(10)
        set(p, DriftBits.fish[i], n)
        set(p, DriftBits.nets[i], if (n == 10) 3 else 2)
        val f = p.statBase(stats.fishing).coerceIn(47, 70)
        val h = p.statBase(stats.hunter).coerceIn(44, 70)
        p.statAdvance(stats.fishing, 50 + (f - 50) * 1.35)
        if (chased) p.statAdvance(stats.hunter, 72 + (h - 50) * 1.475)
        if (n == 10) p.mes("Your drift net is full.")
    }

    companion object {
        val CAMP = CoordGrid(3725, 3804)
        val NORTH = CoordGrid(3735, 3893)
        val ISLAND = CoordGrid(3762, 3899)
        val SUBMERGED = CoordGrid(3732, 10282, 1)
        val ENTRANCE = CoordGrid(3729, 10294, 1)

        fun inWater(c: CoordGrid) = c.x in 3712..3839 && c.z in 10240..10303 && c.level == 1

        private val SHOALS =
            listOf(
                3738 to 10300,
                3744 to 10300,
                3747 to 10300,
                3737 to 10299,
                3741 to 10299,
                3748 to 10298,
                3738 to 10297,
                3739 to 10295,
                3739 to 10292,
                3741 to 10292,
                3742 to 10290,
            )
        private val HARPOONS =
            setOf(
                21028,
                25059,
                11905,
                11907,
                22288,
                22290,
                12899,
                22292,
                22294,
                12900,
                12000,
                21649,
            )
    }
}
