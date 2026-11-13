package org.rsmod.content.skills.slayer

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.npc.slayerSuperiorOwner
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.stat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.map.collision.get
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal object SuperiorTimer : TimerReferences() {
    val check = find("slayer_superior_watch")
}

internal object SuperiorVars : VarBitReferences() {
    val enabled = find("slayer_unlock_superiormobs")
    val disabled = find("slayer_toggleoff_superiormobs")
    val eliteCombatRewards = find("ca_tier_status_elite")
}

@Singleton
class SuperiorSlayer
@Inject
internal constructor(
    private val types: NpcTypeList,
    private val objTypes: ObjTypeList,
    private val npcs: NpcRepository,
    private val drops: ObjRepository,
    private val regionalDrops: RegionalSlayerDrops,
    private val players: PlayerList,
    private val clock: MapClock,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
) : PluginScript() {
    private val issuingMasters = mutableMapOf<Npc, Int>()
    private val active = mutableMapOf<PlayerUid, Npc>()

    override fun ScriptContext.startup() {
        for (id in superiorMonsters.values.distinct()) {
            onNpcHit(types.getValue(id)) {
                if (
                    npc.slayerSuperiorOwner != null &&
                        hit.isFromPlayer &&
                        hit.resolvePlayerSource(players)?.uid == npc.slayerSuperiorOwner
                ) {
                    npc.lifecycleDelCycle = clock + 200
                }
            }
        }
        onNpcTimer(SuperiorTimer.check) {
            val player = npc.slayerSuperiorOwner?.resolve(players)
            if (
                player == null ||
                    !npc.isWithinDistance(player, 15) ||
                    player.stat(stats.hitpoints) == 0
            ) {
                npcs.del(npc, Int.MAX_VALUE)
            }
        }
        onEvent<NpcStateEvents.Delete> {
            npc.slayerSuperiorOwner?.let { active.remove(it, npc) }
            issuingMasters.remove(npc)
        }
    }

    internal fun trySpawn(player: Player, defeated: Npc) {
        val superior = superiorMonsters[defeated.visType.id] ?: return
        if (
            player.vars[SuperiorVars.enabled] == 0 ||
                player.vars[SuperiorVars.disabled] != 0 ||
                player.uid in active
        )
            return
        val chance = if (player.vars[SuperiorVars.eliteCombatRewards] >= 2) 150 else 200
        if (random.of(chance) != 0) return
        val type = types.getValue(superior)
        val coords = findSpace(defeated.coords, type.size) ?: return
        val npc = Npc(type, coords)
        npc.slayerSuperiorOwner = player.uid
        npcs.add(npc, 200)
        active[player.uid] = npc
        issuingMasters[npc] = player.slayerMaster
        npc.timer(SuperiorTimer.check, 50)
        player.mes("A superior foe has appeared...")
    }

    private fun findSpace(origin: CoordGrid, size: Int): CoordGrid? {
        for (radius in 0..4) {
            for (dx in -radius..radius) for (dz in -radius..radius) {
                if (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz)) != radius) continue
                val tile = origin.translate(dx, dz)
                if (
                    (0 until size).all { x ->
                        (0 until size).all { z ->
                            val flags = collision[tile.translate(x, z)]
                            flags and
                                (CollisionFlag.BLOCK_WALK or
                                    CollisionFlag.LOC or
                                    CollisionFlag.GROUND_DECOR) == 0 &&
                                (x == 0 || flags and CollisionFlag.WALL_WEST == 0) &&
                                (z == 0 || flags and CollisionFlag.WALL_SOUTH == 0)
                        }
                    }
                )
                    return tile
            }
        }
        return null
    }

    internal fun reward(player: Player, npc: Npc) {
        val master = issuingMasters.remove(npc) ?: return
        regionalDrops.roll(player, npc, master, superior = true)
        active.remove(player.uid, npc)
        val level = npc.visType.param(params.slayer_levelrequire)
        val rate = 25_000 - (level + 55) * (level + 55)
        // Two independent rolls. Both may succeed on the same kill.
        if (random.of(rate) < 125) {
            val id =
                when (random.of(16)) {
                    0,
                    1 -> 20724
                    in 2..8 -> 20730
                    else -> 20736
                }
            drops.add(objTypes.getValue(id), npc.coords, constants.lootdrop_duration, player)
        }
        if (random.of(rate) < 125 && random.of(8) == 0) {
            drops.add(SlayerObjs.eternalGem, npc.coords, constants.lootdrop_duration, player)
        }
    }
}
