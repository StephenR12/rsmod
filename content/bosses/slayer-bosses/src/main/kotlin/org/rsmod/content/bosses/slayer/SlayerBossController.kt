package org.rsmod.content.bosses.slayer

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class SlayerBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, SlayerBossState>()
    private val owners = IdentityHashMap<Npc, Npc>()

    public fun reset(npc: Npc): SlayerBossState = SlayerBossState().also { states[npc] = it }

    public fun state(npc: Npc): SlayerBossState = states.getOrPut(npc, ::SlayerBossState)

    public fun onBossDamaged(npc: Npc, definition: SlayerBossDefinition) {
        val state = state(npc)
        when (definition.family) {
            SlayerBossFamily.Cerberus ->
                if (!state.firstWave && npc.hitpoints <= 400) {
                    state.firstWave = true
                    spawnWave(
                        npc,
                        listOf(
                            SlayerBossNpcs.magicSoul,
                            SlayerBossNpcs.rangedSoul,
                            SlayerBossNpcs.meleeSoul,
                        ),
                        30,
                    )
                }
            SlayerBossFamily.Kraken ->
                if (!state.firstWave && npc.hitpoints <= 128) {
                    state.firstWave = true
                    spawnWave(npc, List(4) { SlayerBossNpcs.krakenTentacle }, 120)
                }
            SlayerBossFamily.Dusk ->
                if (!state.firstWave && npc.hitpoints <= 225) {
                    state.firstWave = true
                }
            SlayerBossFamily.Dawn -> Unit
            SlayerBossFamily.Sire -> {
                if (!state.firstWave && npc.hitpoints <= 300) {
                    state.firstWave = true
                    spawnWave(npc, List(4) { SlayerBossNpcs.sireLung }, 50)
                } else if (!state.secondWave && npc.hitpoints <= 100) {
                    state.secondWave = true
                    spawnWave(
                        npc,
                        listOf(
                            SlayerBossNpcs.sireSpawn,
                            SlayerBossNpcs.sireSpawn,
                            SlayerBossNpcs.sireScion,
                        ),
                        60,
                    )
                }
            }
            SlayerBossFamily.Araxxor -> {
                if (!state.firstWave && npc.hitpoints <= 700) {
                    state.firstWave = true
                    spawnWave(
                        npc,
                        listOf(
                            SlayerBossNpcs.mirrorback,
                            SlayerBossNpcs.ruptura,
                            SlayerBossNpcs.acidicAraxyte,
                        ),
                        80,
                    )
                } else if (!state.secondWave && npc.hitpoints <= 300) {
                    state.secondWave = true
                    spawnWave(npc, listOf(SlayerBossNpcs.ruptura, SlayerBossNpcs.acidicAraxyte), 80)
                }
            }
            SlayerBossFamily.Hydra -> {
                if (!state.firstWave && npc.hitpoints <= 825) state.firstWave = true
                if (!state.secondWave && npc.hitpoints <= 550) state.secondWave = true
            }
            SlayerBossFamily.Thermy -> Unit
        }
    }

    public fun onMinionKilled(npc: Npc) {
        val owner = owners.remove(npc) ?: return
        states[owner]?.activeMinions?.remove(npc)
    }

    private fun spawnWave(boss: Npc, types: List<NpcType>, hitpoints: Int) {
        types.forEachIndexed { index, type ->
            val offset = OFFSETS[index % OFFSETS.size]
            val minion =
                Npc(
                    npcTypes[type],
                    CoordGrid(
                        boss.coords.x + offset.first,
                        boss.coords.z + offset.second,
                        boss.coords.level,
                    ),
                )
            minion.apply {
                this.hitpoints = hitpoints
                baseHitpointsLvl = hitpoints
                attackLvl = 150
                baseAttackLvl = 150
                strengthLvl = 150
                baseStrengthLvl = 150
                defenceLvl = 100
                baseDefenceLvl = 100
                magicLvl = 150
                baseMagicLvl = 150
                rangedLvl = 150
                baseRangedLvl = 150
                mode = NpcMode.Wander
                setHunt(14)
                setHuntMode(huntmodes.constant_melee)
            }
            owners[minion] = boss
            state(boss).activeMinions.add(minion)
            npcs.add(minion, 200)
        }
    }

    private companion object {
        private val OFFSETS = listOf(-3 to 0, 3 to 0, 0 to 3, 0 to -3)
    }
}

public data class SlayerBossState(
    var attacks: Int = 0,
    var firstWave: Boolean = false,
    var secondWave: Boolean = false,
    val activeMinions: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap<Npc, Boolean>()),
) {
    public fun consumeAttack(): Int = ++attacks
}
