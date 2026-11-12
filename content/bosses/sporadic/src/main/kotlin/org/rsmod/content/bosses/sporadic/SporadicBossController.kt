package org.rsmod.content.bosses.sporadic

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class SporadicBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, SporadicBossState>()
    private val owners = IdentityHashMap<Npc, Npc>()

    public fun reset(npc: Npc): SporadicBossState = SporadicBossState().also { states[npc] = it }

    public fun state(npc: Npc): SporadicBossState = states.getOrPut(npc, ::SporadicBossState)

    public fun modifyHit(npc: Npc, hit: HitBuilder, definition: SporadicBossDefinition) {
        val active = state(npc).activeMinions.size
        when (definition.family) {
            SporadicBossFamily.Bryophyta,
            SporadicBossFamily.Hespori -> if (active > 0) hit.damage = 0
            SporadicBossFamily.Skotizo -> repeat(active.coerceAtMost(4)) { hit.damage /= 2 }
            else -> Unit
        }
    }

    public fun onBossDamaged(npc: Npc, definition: SporadicBossDefinition) {
        val state = state(npc)
        when (definition.family) {
            SporadicBossFamily.Bryophyta ->
                if (!state.firstWave && npc.hitpoints <= 58) {
                    state.firstWave = true
                    spawnWave(npc, List(3) { SporadicBossNpcs.growthling }, 10)
                }
            SporadicBossFamily.Mimic ->
                if (!state.firstWave && npc.hitpoints <= 115) {
                    state.firstWave = true
                    spawnWave(
                        npc,
                        listOf(
                            SporadicBossNpcs.thirdAgeWarrior,
                            SporadicBossNpcs.thirdAgeRanger,
                            SporadicBossNpcs.thirdAgeMage,
                        ),
                        50,
                    )
                }
            SporadicBossFamily.Hespori -> {
                if (!state.firstWave && npc.hitpoints <= 200) {
                    state.firstWave = true
                    spawnWave(npc, List(4) { SporadicBossNpcs.hesporiFlower }, 10)
                } else if (!state.secondWave && npc.hitpoints <= 100) {
                    state.secondWave = true
                    spawnWave(npc, List(4) { SporadicBossNpcs.hesporiFlower }, 10)
                }
            }
            SporadicBossFamily.Skotizo -> {
                if (!state.firstWave && npc.hitpoints <= 300) {
                    state.firstWave = true
                    spawnWave(
                        npc,
                        listOf(SporadicBossNpcs.northAltar, SporadicBossNpcs.southAltar),
                        100,
                    )
                } else if (!state.secondWave && npc.hitpoints <= 150) {
                    state.secondWave = true
                    spawnWave(
                        npc,
                        listOf(
                            SporadicBossNpcs.eastAltar,
                            SporadicBossNpcs.westAltar,
                            SporadicBossNpcs.skotizoDemon,
                        ),
                        100,
                    )
                }
            }
            SporadicBossFamily.Obor -> Unit
        }
    }

    public fun onMinionKilled(npc: Npc) {
        val owner = owners.remove(npc) ?: return
        states[owner]?.activeMinions?.remove(npc)
    }

    public fun owner(npc: Npc): Npc? = owners[npc]

    private fun spawnWave(boss: Npc, types: List<NpcType>, hitpoints: Int) {
        types.forEachIndexed { index, type ->
            val offset = WAVE_OFFSETS[index % WAVE_OFFSETS.size]
            val coords =
                CoordGrid(
                    boss.coords.x + offset.first,
                    boss.coords.z + offset.second,
                    boss.coords.level,
                )
            val minion = Npc(npcTypes[type], coords)
            minion.apply {
                this.hitpoints = hitpoints
                baseHitpointsLvl = hitpoints
                attackLvl = 100
                baseAttackLvl = 100
                strengthLvl = 100
                baseStrengthLvl = 100
                defenceLvl = 80
                baseDefenceLvl = 80
                magicLvl = 100
                baseMagicLvl = 100
                rangedLvl = 100
                baseRangedLvl = 100
                mode = NpcMode.Wander
                setHunt(12)
                setHuntMode(huntmodes.constant_melee)
            }
            owners[minion] = boss
            state(boss).activeMinions.add(minion)
            npcs.add(minion, MINION_LIFETIME)
        }
    }

    public companion object {
        private const val MINION_LIFETIME: Int = 200
        private val WAVE_OFFSETS = listOf(-2 to 0, 2 to 0, 0 to -2, 0 to 2)
    }
}

public data class SporadicBossState(
    var attacks: Int = 0,
    var firstWave: Boolean = false,
    var secondWave: Boolean = false,
    val activeMinions: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap<Npc, Boolean>()),
) {
    public fun consumeAttack(): Int = ++attacks
}
