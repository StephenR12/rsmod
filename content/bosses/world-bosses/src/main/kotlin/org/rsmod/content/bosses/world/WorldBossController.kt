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
                if (!state.firstMinions && npc.hitpoints <= 200) {
                    state.firstMinions = true
                    spawnPair(
                        npc,
                        WorldBossNpcs.sarachnisMeleeSpawn,
                        WorldBossNpcs.sarachnisMagicSpawn,
                        30,
                    )
                }
                if (!state.secondMinions && npc.hitpoints <= 100) {
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

    private fun spawnPair(boss: Npc, first: NpcType, second: NpcType, hitpoints: Int) {
        spawn(first, boss.coords, -2, 0, hitpoints)
        spawn(second, boss.coords, 2, 0, hitpoints)
    }

    private fun spawn(type: NpcType, origin: CoordGrid, x: Int, z: Int, hitpoints: Int) {
        val npc = Npc(npcTypes[type], CoordGrid(origin.x + x, origin.z + z, origin.level))
        npc.apply {
            this.hitpoints = hitpoints
            baseHitpointsLvl = hitpoints
            mode = NpcMode.Wander
            setHunt(10)
            setHuntMode(huntmodes.constant_melee)
        }
        npcs.add(npc, 200)
    }
}

public data class WorldBossState(
    var attacks: Int = 0,
    var firstMinions: Boolean = false,
    var secondMinions: Boolean = false,
)
