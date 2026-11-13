package org.rsmod.content.skills.hunter.herbiboar

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.HunterCapture
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.skills.pets.SkillPets
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object BoarBits : VarBitReferences() {
    val tracks = (0..23).map { find("hunting_trail_state${9+it/10}_${it%10}") }
    val end = find("hunting_trail_ends_fossil")
    val used = find("hunting_trails_used_fossil")
    val visible = find("fossil_herbiboar_visible")
    val warning = find("fossil_herbiboar_already_caught_ignore_warning")
    val quest = find("fossilquest_progress")
}

internal object BoarCounts : VarpReferences() {
    val count = find("herbiboar_catches")
}

internal object BoarCountBuilder : VarpBuilder() {
    init {
        build("herbiboar_catches") { transmitNever = true }
    }
}

internal object BoarNpcs : NpcReferences() {
    val harvest = find("fossil_herbiboar_visible")
}

internal object BoarSeqs : SeqReferences() {
    val kick = find("human_unarmedkick")
}

internal object HerbiboarSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<HerbiboarHunting>("herbiboars.toml")
    }
}

public class HerbiboarHunting
@Inject
constructor(
    private val locs: LocTypeList,
    private val items: ObjTypeList,
    private val ground: ObjRepository,
    private val pets: SkillPets,
    rows: DbRowTypeList,
) : PluginScript() {
    private val voyage = rows.getValue(11).data.getValue(19).single() as Int

    override fun ScriptContext.startup() {
        for (index in HerbiboarMap.startTiles.indices) {
            val loc = locs.getValue(30519 + index)
            onOpLoc1(loc) { if (it.loc.coords == HerbiboarMap.startTiles[index]) start(index) }
            onOpLoc2(loc) { mes("You have caught ${vars[BoarCounts.count]} herbiboars.") }
            onOpLoc3(loc) {
                vars[BoarBits.warning] = 1 - vars[BoarBits.warning]
                mes(
                    if (vars[BoarBits.warning] == 0) "You will be warned before abandoning a trail."
                    else "The trail restart warning is now disabled."
                )
            }
        }
        for ((id, tile) in HerbiboarMap.clues) onOpLoc1(locs.getValue(id)) {
            if (it.loc.coords == tile) inspect(id)
        }
        val tunnel = locs.getValue(30532)
        onOpLoc1(tunnel) { kick(it.loc.coords) }
        onOpLoc2(tunnel) {
            mes(
                if (
                    vars[BoarBits.end] > 0 &&
                        HerbiboarMap.tunnels.getOrNull(vars[BoarBits.end] - 1) == it.loc.coords
                )
                    "You can hear something in the tunnel."
                else "There doesn't appear to be anything here."
            )
        }
        onOpNpc1(BoarNpcs.harvest) { harvest(it.npc.coords) }
    }

    private suspend fun ProtectedAccess.start(index: Int) {
        if (!player.members || stat(stats.hunter) < 80) {
            mes("You need a Hunter level of 80 to follow these tracks.")
            return
        }
        if (vars[BoarBits.quest] < voyage) {
            mes("You need to complete Bone Voyage to hunt herbiboars.")
            return
        }
        if (BoarBits.tracks.any { vars[it] != 0 } || vars[BoarBits.visible] != 0) {
            if (
                vars[BoarBits.warning] == 0 &&
                    !choice2(
                        "Abandon the old trail and start here?",
                        true,
                        "Keep the current trail.",
                        false,
                    )
            )
                return
        }
        reset()
        val leg = random.pick(HerbiboarMap.starts[index])
        follow(kotlin.math.abs(leg) - 1, leg > 0)
        mes("You find a trail of tracks leading away.")
    }

    private fun ProtectedAccess.reset() {
        for (bit in BoarBits.tracks) vars[bit] = 0
        vars[BoarBits.used] = 0
        vars[BoarBits.end] = 0
        vars[BoarBits.visible] = 0
    }

    /**
     * A state of 3/4 exposes footprints in the travelled direction. A state of 1/2 is the pending
     * leg whose inspectable clue will reveal those prints. Both belong to this player.
     */
    private fun ProtectedAccess.follow(index: Int, forward: Boolean) {
        val travelled = HerbiboarMap.tracks[index]
        vars[BoarBits.tracks[index]] = if (forward) 3 else 4
        val node = if (forward) travelled.b else travelled.a
        if (node < 0) {
            vars[BoarBits.end] = -node
            return
        }
        val visited = mutableSetOf<Int>()
        for ((i, edge) in HerbiboarMap.tracks.withIndex()) {
            when (vars[BoarBits.tracks[i]]) {
                3 -> visited += edge.a
                4 -> visited += edge.b
            }
        }
        val options =
            HerbiboarMap.tracks.indices.filter { i ->
                val edge = HerbiboarMap.tracks[i]
                edge.leaves(node) &&
                    vars[BoarBits.tracks[i]] == 0 &&
                    (if (edge.a == node) edge.b else edge.a) !in visited
            }
        val near = HerbiboarMap.nearbyTunnel[node]
        if (
            near != null &&
                (options.isEmpty() ||
                    BoarBits.tracks.count { vars[it] >= 3 } >= 3 && random.randomBoolean(3))
        ) {
            vars[BoarBits.end] = near
        } else {
            val next = random.pickOrNull(options)
            if (next == null) {
                reset()
                mes("The creature has led you round in circles. You'll need to start again.")
                return
            }
            vars[BoarBits.tracks[next]] = if (HerbiboarMap.tracks[next].a == node) 1 else 2
        }
    }

    private fun ProtectedAccess.inspect(id: Int) {
        val active = BoarBits.tracks.indexOfFirst { vars[it] in 1..2 }
        if (active < 0) {
            mes("You find no further tracks.")
            return
        }
        val edge = HerbiboarMap.tracks[active]
        val forward = vars[BoarBits.tracks[active]] == 1
        if ((if (forward) edge.clueA else edge.clueB) != id) {
            mes("You find nothing of interest.")
            return
        }
        vars[BoarBits.used] = 1
        follow(active, forward)
        if (vars[BoarBits.end] != 0) {
            mes("The tracks lead into a nearby tunnel.")
            return
        }
        if (BoarBits.tracks.none { vars[it] != 0 }) return
        statAdvance(stats.hunter, 50.0)
        when (random.of(500)) {
            in 0..179 -> invAddOrDrop(ground, items.getValue(21555), random.of(5..24))
            in 180..189 -> invAddOrDrop(ground, items.getValue(21562))
            in 190..194 -> invAddOrDrop(ground, items.getValue(21564))
            in 195..198 -> invAddOrDrop(ground, items.getValue(21566))
            199 -> invAddOrDrop(ground, items.getValue(21568))
        }
        mes("You find more tracks.")
    }

    private suspend fun ProtectedAccess.kick(tile: CoordGrid) {
        val end = vars[BoarBits.end]
        if (end == 0 || HerbiboarMap.tunnels.getOrNull(end - 1) != tile) {
            mes("Nothing stirs inside the tunnel.")
            return
        }
        anim(BoarSeqs.kick)
        delay(1)
        if (vars[BoarBits.end] != end) return
        reset()
        vars[BoarBits.visible] = end
        val level = statBase(stats.hunter)
        val xp = if (level < 95) 1950 + 30 * (level - 80) else 2385 + 19 * (level - 95)
        statAdvance(stats.hunter, xp.toDouble())
        mes("A herbiboar emerges from the tunnel, stunned.")
    }

    private fun ProtectedAccess.harvest(tile: CoordGrid) {
        val end = vars[BoarBits.visible]
        if (end == 0 || HerbiboarMap.boars.getOrNull(end - 1) != tile) return
        val level = stat(stats.herblore)
        if (level < 31) {
            mes("You need a Herblore level of 31 to harvest the herbs.")
            return
        }
        val secateurs = items.getValue(7409)
        val count = random.of(1..3) + if (secateurs in inv || secateurs in worn) 1 else 0
        for (i in 1..count) {
            val herb =
                herbs
                    .firstOrNull { (_, low, high) ->
                        random.randomDouble() <
                            SkillingSuccessRate.successRate(low, high, level, 99)
                    }
                    ?.first ?: 199
            invAddOrDrop(ground, items.getValue(herb))
        }
        vars[BoarBits.visible] = 0
        vars[BoarCounts.count] = vars[BoarCounts.count] + 1
        if (count > 1) statAdvance(stats.herblore, 25.0 * (count - 1))
        publish(HunterCapture(player, "fossil_herbiboar_visible"))
        if (random.randomBoolean(6500)) pets.award(player, 21509)
        mes("You harvest the herbs and the herbiboar escapes.")
    }

    private companion object {
        val herbs =
            listOf(
                Triple(207, -10, 20),
                Triple(219, -70, 20),
                Triple(3051, -60, 20),
                Triple(217, -50, 30),
                Triple(2485, -30, 40),
                Triple(215, -10, 50),
                Triple(213, 10, 60),
                Triple(211, 20, 60),
                Triple(209, 30, 70),
                Triple(203, 70, -20),
                Triple(205, 100, -30),
                Triple(201, 170, -40),
            )
    }
}
