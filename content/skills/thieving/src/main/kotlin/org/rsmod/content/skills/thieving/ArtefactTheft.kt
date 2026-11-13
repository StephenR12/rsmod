package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import kotlin.math.abs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

public object ArtefactNpcs : NpcReferences() {
    val captain = find("piscarilius_captain_khaled_multi")
    val guards =
        listOf("nw_1", "nw_2", "n_1", "n_2", "n_3", "s_1", "sw_1", "idle").map {
            find("piscarilius_patrolman_" + it)
        }
}

public object ArtefactLocs : LocReferences() {
    val drawers =
        listOf("n", "se", "s", "sw", "w", "nw").map { find("piscarilius_burglary_drawers_" + it) }
    val ladder = find("piscarilius_ladder_bottom")
    val upstairs = listOf(find("piscarilius_ladder_top_01"), find("piscarilius_ladder_top_02"))
}

public object ArtefactObjs : ObjReferences() {
    val loot =
        listOf("pendant", "ring", "circlet", "heirloom", "box").map {
            find("piscarilius_stolen_" + it)
        }
    val lockpick = find("lockpick")
    val hairclip = find("kr_hairclip")
}

public object ArtefactBits : VarBitReferences() {
    val target = find("piscarilius_burglary_target")
    val completed = find("piscarilius_burglary_completed")
}

public object ArtefactTimers : TimerReferences() {
    val watch = find("piscarilius_artefact_watch")
}

internal object ArtefactGuardEditor : NpcEditor() {
    init {
        for (guard in ArtefactNpcs.guards) edit(guard) {
            defaultMode = NpcMode.None
            timer = 2
        }
    }
}

internal object ArtefactPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<ArtefactTheft>("artefact-theft.toml")
    }
}

/**
 * Stealing artefacts14989711, Drawers14939120, stolen items14839558..62/14932047. Assignment
 * weights, lock animation duration and guard routes/cones/reaction are playable models; the
 * published requirements, XP, coin range, logout/teleport loss and diary action are reproduced.
 */
@Singleton
public class ArtefactTheft
@Inject
constructor(
    private val npcs: NpcRepository,
    private val rays: RayCastValidator,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    private data class Heist(
        val house: Int,
        var item: Int? = null,
        var last: CoordGrid,
        var spotted: Int = -1,
        var guard: Npc? = null,
    )

    private data class Patrol(var waypoint: Int = 1, var dx: Int = 1, var dz: Int = 0)

    private val heists = WeakHashMap<Player, Heist>()
    private val patrols = WeakHashMap<Npc, Patrol>()

    override fun ScriptContext.startup() {
        onOpNpc1(ArtefactNpcs.captain) { converse(this) }
        onOpNpc3(ArtefactNpcs.captain) { converse(this) }
        for ((index, loc) in ArtefactLocs.drawers.withIndex()) onOpLoc1(loc) {
            steal(this, it.loc, index)
        }
        onOpLoc1(ArtefactLocs.ladder) { climb(this, it.loc, true) }
        for (loc in ArtefactLocs.upstairs) onOpLoc1(loc) { climb(this, it.loc, false) }
        for ((index, type) in ArtefactNpcs.guards.withIndex()) onAiTimer(type) {
            patrol(npc, index)
        }
        for (obj in ArtefactObjs.loot) onOpHeld5(obj) {
            if (
                confirmDestroy(
                    obj,
                    1,
                    "Destroy this stolen artefact?",
                    "You will fail Captain Khaled's task.",
                )
            ) {
                if (invDel(inv, obj, count = 1, slot = it.slot).completed() == 1)
                    invalidate(player, false)
            }
        }
        onPlayerSoftTimer(ArtefactTimers.watch) { watch(player) }
        onEvent<PlayerTeleportEvent.Start> {
            if (heists[player]?.item != null) invalidate(player, false)
        }
        onPlayerLogout { invalidate(player, false) }
        onPlayerLogin { invalidate(player, false) }
    }

    private suspend fun converse(access: ProtectedAccess): Unit =
        with(access) {
            if (heists[player]?.item != null) {
                deliver(this)
                return
            }
            val heist = heists[player]
            if (heist != null) {
                mes(
                    "Recover an artefact from the ${HOUSES[heist.house]} house in the western residential district."
                )
                return
            }
            if (choice2("Give me an artefact job.", true, "Leave.", false)) assign(this)
        }

    public fun assign(access: ProtectedAccess): Boolean =
        with(access) {
            if (!eligible(this) || heists.containsKey(player)) return false
            val house = random.of(6)
            heists[player] = Heist(house, last = player.coords)
            vars[ArtefactBits.target] = house + 1
            vars[ArtefactBits.completed] = 0
            mes(
                "Recover an artefact from the ${HOUSES[house]} house in the western residential district."
            )
            true
        }

    public suspend fun steal(access: ProtectedAccess, loc: BoundLocInfo, house: Int): Boolean =
        with(access) {
            if (!eligible(this) || !isWithinDistance(loc, 1)) return false
            val heist = heists[player]
            if (heist == null || heist.house != house) {
                mes("This isn't the house Khaled assigned you.")
                return false
            }
            if (heist.item != null) {
                mes("You already have the artefact. Return it to Captain Khaled.")
                return false
            }
            if (ArtefactObjs.lockpick !in inv && ArtefactObjs.hairclip !in inv) {
                mes("You need a lockpick or a hair clip to pick this lock.")
                return false
            }
            if (inv.isFull()) {
                mes("You don't have enough inventory space.")
                return false
            }
            anim(seqs.human_pickpocket)
            delay(4)
            if (
                heists[player] !== heist ||
                    !eligible(this) ||
                    !isWithinDistance(loc, 1) ||
                    (ArtefactObjs.lockpick !in inv && ArtefactObjs.hairclip !in inv)
            )
                return false
            val obj = ArtefactObjs.loot[random.of(5)]
            if (!invAdd(inv, obj).success) return false
            heist.item = obj.id
            heist.last = player.coords
            vars[ArtefactBits.completed] = 1
            statAdvance(stats.thieving, 750.0)
            player.softTimer(ArtefactTimers.watch, 1)
            mes("You take a valuable artefact. Smuggle it back to Captain Khaled on foot.")
            true
        }

    public fun deliver(access: ProtectedAccess): Boolean =
        with(access) {
            if (!eligible(this)) return false
            val heist = heists[player] ?: return false
            val item = heist.item ?: return false
            if (inv.none { it?.id == item }) {
                invalidate(player, false)
                mes("You have lost the artefact. Ask Khaled for another job.")
                return false
            }
            val coins = random.of(500..1000)
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, item, 1)
                    add(bag, objs.coins.id, coins)
                }
            if (!tx.success) {
                mes("You don't have enough inventory space for your payment.")
                return false
            }
            statAdvance(stats.thieving, player.thievingLvl * 40.0)
            player.completeDiaryTask(DiaryTask.DeliverPiscariliusArtefact)
            invalidate(player, false)
            mes("Khaled takes the artefact and pays you $coins coins.")
            true
        }

    private fun eligible(access: ProtectedAccess): Boolean =
        with(access) {
            if (!player.members) return false
            if (player.thievingLvl < 49) {
                mes("You need a Thieving level of 49 to steal artefacts.")
                return false
            }
            true
        }

    public fun invalidate(player: Player, caught: Boolean) {
        heists.remove(player)
        player.clearSoftTimer(ArtefactTimers.watch)
        val ids = ArtefactObjs.loot.mapTo(hashSetOf()) { it.id }
        for (inv in player.invMap.values) for (slot in inv.indices) if (inv[slot]?.id in ids)
            inv[slot] = null
        VarPlayerIntMapSetter.set(player, ArtefactBits.target, 0)
        VarPlayerIntMapSetter.set(player, ArtefactBits.completed, 0)
        if (caught) {
            player.cancelActiveCoroutine()
            PathingEntityCommon.telejump(player, collision, CAUGHT)
            player.mes("A patrol catches you and confiscates the stolen artefact!")
        }
    }

    public fun watch(player: Player) {
        val heist =
            heists[player]
                ?: run {
                    player.clearSoftTimer(ArtefactTimers.watch)
                    return
                }
        val item = heist.item ?: return
        val previous = heist.last
        heist.last = player.coords
        // Detect nonstandard travel as well as accepted standard teleports; house ladders may
        // change
        // one plane in place. Normal running covers at most two tiles per axis each tick.
        if (
            !player.members ||
                player.inv.none { it?.id == item } ||
                abs(previous.x - player.coords.x) > 2 ||
                abs(previous.z - player.coords.z) > 2 ||
                abs(previous.level - player.coords.level) > 1
        ) {
            invalidate(player, false)
            return
        }
        val seen = npcs.findAll(ZoneKey.from(player.coords), 1).firstOrNull { sees(it, player) }
        if (seen == null) {
            heist.spotted = -1
            heist.guard = null
            return
        }
        seen.faceSquare(player.coords)
        if (heist.guard !== seen) {
            heist.guard = seen
            heist.spotted = player.currentMapClock
        } else if (player.currentMapClock - heist.spotted >= 2) invalidate(player, true)
    }

    public fun sees(npc: Npc, player: Player): Boolean {
        if (
            ArtefactNpcs.guards.none { it.id == npc.id } ||
                npc.coords.level != player.coords.level ||
                !npc.isWithinDistance(player, 4)
        )
            return false
        val dx = player.coords.x - npc.coords.x
        val dz = player.coords.z - npc.coords.z
        val patrol = patrols[npc] ?: Patrol()
        val forward = dx * patrol.dx + dz * patrol.dz
        val side = abs(dx * patrol.dz - dz * patrol.dx)
        return (abs(dx) <= 1 && abs(dz) <= 1 || forward > 0 && side <= forward) &&
            rays.hasLineOfSight(npc.coords, player.coords)
    }

    public fun patrol(npc: Npc, index: Int) {
        val route = ROUTES[index]
        val state = patrols.getOrPut(npc) { Patrol() }
        var dest = route[state.waypoint % route.size]
        if (npc.coords == dest) {
            state.waypoint = (state.waypoint + 1) % route.size
            dest = route[state.waypoint]
        }
        state.dx = (dest.x - npc.coords.x).coerceIn(-1, 1)
        state.dz = (dest.z - npc.coords.z).coerceIn(-1, 1)
        npc.walk(dest)
    }

    public fun climb(access: ProtectedAccess, loc: BoundLocInfo, up: Boolean): Boolean =
        with(access) {
            if (!player.members) return false
            val delta = if (up) 1 else -1
            val level = player.coords.level + delta
            if (level !in 0..3) return false
            // Native Piscarilius buildings use linked bridge tiles; server planes are one below the
            // encoded drawers/ladders in the six target houses.
            val dest = CoordGrid(player.coords.x, player.coords.z, level)
            if (heists[player]?.item != null) heists[player]?.last = dest
            telejump(dest)
            true
        }

    public companion object {
        public val HOUSES: List<String> =
            listOf(
                "northern",
                "south-eastern",
                "southern",
                "south-western",
                "western",
                "north-western",
            )
        public val CAUGHT: CoordGrid = CoordGrid(1805, 3730)
        // Bounded patrol paths on native bridge footprints; precise historical paths are
        // unverified.
        public val ROUTES: List<List<CoordGrid>> =
            listOf(
                listOf(CoordGrid(1757, 3755), CoordGrid(1764, 3755)),
                listOf(CoordGrid(1766, 3761), CoordGrid(1766, 3756)),
                listOf(
                    CoordGrid(1764, 3756),
                    CoordGrid(1774, 3756),
                    CoordGrid(1774, 3746),
                    CoordGrid(1764, 3746),
                ),
                listOf(CoordGrid(1768, 3747), CoordGrid(1774, 3747)),
                listOf(CoordGrid(1778, 3748), CoordGrid(1784, 3748)),
                listOf(CoordGrid(1779, 3730), CoordGrid(1786, 3730)),
                listOf(CoordGrid(1748, 3742), CoordGrid(1748, 3738)),
                listOf(CoordGrid(1761, 3737), CoordGrid(1770, 3737)),
            )
    }
}
