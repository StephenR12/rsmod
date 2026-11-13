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

    internal fun configure(npc: Npc, definition: SporadicBossDefinition) {
        npc.apply {
            hitpoints = definition.hitpoints
            baseHitpointsLvl = definition.hitpoints
            attackLvl = definition.attack
            baseAttackLvl = definition.attack
            strengthLvl = definition.strength
            baseStrengthLvl = definition.strength
            defenceLvl = definition.defence
            baseDefenceLvl = definition.defence
            magicLvl = definition.magic
            baseMagicLvl = definition.magic
            rangedLvl = definition.ranged
            baseRangedLvl = definition.ranged
            mode = NpcMode.Wander
            setHunt(18)
            setHuntMode(huntmodes.constant_ranged)
        }
    }

    public fun spawnHespori(coords: CoordGrid): Npc {
        val definition = SporadicBosses.byId.getValue(SporadicBossNpcs.hespori.id)
        val boss = Npc(npcTypes[definition.npc], coords)
        configure(boss, definition)
        boss.mode = NpcMode.None
        npcs.add(boss, 6000)
        reset(boss)
        spawnWave(boss, List(4) { SporadicBossNpcs.hesporiFlower }, 10)
        return boss
    }

    public fun removeHespori(boss: Npc) {
        val flowers = states.remove(boss)?.activeMinions.orEmpty()
        for (flower in flowers) {
            owners.remove(flower)
            if (flower.slotId != -1) npcs.del(flower, Int.MAX_VALUE)
        }
        if (boss.slotId != -1) npcs.del(boss, Int.MAX_VALUE)
    }

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
            val isFlower = type == SporadicBossNpcs.hesporiFlower
            val offsets = if (isFlower) FLOWER_OFFSETS else WAVE_OFFSETS
            val offset = offsets[index % offsets.size]
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
            if (isFlower) {
                minion.mode = NpcMode.None
                minion.setHunt(0)
            }
            owners[minion] = boss
            state(boss).activeMinions.add(minion)
            npcs.add(minion, if (isFlower) 6000 else MINION_LIFETIME)
        }
    }

    public companion object {
        private const val MINION_LIFETIME: Int = 200
        private val FLOWER_OFFSETS = listOf(-4 to -3, -4 to 5, 5 to -3, 5 to 5)
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
