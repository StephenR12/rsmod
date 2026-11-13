package org.rsmod.content.bosses.world

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class WorldBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, WorldBossState>()

    public fun reset(npc: Npc): WorldBossState = WorldBossState().also { states[npc] = it }

    public fun state(npc: Npc): WorldBossState = states.getOrPut(npc, ::WorldBossState)

    public fun onHit(npc: Npc, hit: Hit, definition: WorldBossDefinition) {
        if (!hit.isFromPlayer) {
            return
        }
        val state = state(npc)
        when (definition.family) {
            WorldBossFamily.Sarachnis -> {
                if (!state.firstMinions && npc.hitpoints <= 266) {
                    state.firstMinions = true
                    spawnPair(
                        npc,
                        WorldBossNpcs.sarachnisMeleeSpawn,
                        WorldBossNpcs.sarachnisMagicSpawn,
                        30,
                    )
                }
                if (!state.secondMinions && npc.hitpoints <= 133) {
                    state.secondMinions = true
                    spawnPair(
                        npc,
                        WorldBossNpcs.sarachnisMeleeSpawn,
                        WorldBossNpcs.sarachnisMagicSpawn,
                        30,
                    )
                }
            }
            WorldBossFamily.Scurrius ->
                if (!state.firstMinions && npc.hitpoints <= 250) {
                    state.firstMinions = true
                    repeat(3) { index ->
                        spawn(WorldBossNpcs.giantRat, npc.coords, index - 1, 3, 20)
                    }
                }
            else -> Unit
        }
    }

    public fun removeSpawns(boss: Npc) {
        val state = states.remove(boss) ?: return
        for (spawn in state.spawns) if (spawn.isSlotAssigned && spawn.hitpoints > 0)
            npcs.del(spawn, Int.MAX_VALUE)
    }

    private fun spawnPair(boss: Npc, first: NpcType, second: NpcType, hitpoints: Int) {
        val state = state(boss)
        state.spawns += spawn(first, boss.coords, -2, 0, hitpoints)
        state.spawns += spawn(second, boss.coords, 2, 0, hitpoints)
    }

    private fun spawn(type: NpcType, origin: CoordGrid, x: Int, z: Int, hitpoints: Int): Npc {
        val npc = Npc(npcTypes[type], CoordGrid(origin.x + x, origin.z + z, origin.level))
        npc.apply {
            this.hitpoints = hitpoints
            baseHitpointsLvl = hitpoints
            mode = NpcMode.Wander
            setHunt(10)
            setHuntMode(
                if (type.id == WorldBossNpcs.sarachnisMagicSpawn.id) huntmodes.constant_ranged
                else huntmodes.constant_melee
            )
        }
        npcs.add(npc, 200)
        return npc
    }
}

public data class WorldBossState(
    val spawns: MutableList<Npc> = mutableListOf(),
    var attacks: Int = 0,
    var firstMinions: Boolean = false,
    var secondMinions: Boolean = false,
)
