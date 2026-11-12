package org.rsmod.content.bosses.instanced

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class InstancedBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, InstancedBossState>()

    public fun reset(npc: Npc): InstancedBossState = InstancedBossState().also { states[npc] = it }

    public fun state(npc: Npc): InstancedBossState = states.getOrPut(npc, ::InstancedBossState)

    public fun onBossDamaged(npc: Npc, definition: InstancedBossDefinition) {
        val state = state(npc)
        if (!state.firstSpecial && npc.hitpoints <= definition.hitpoints * 2 / 3) {
            state.firstSpecial = true
            when (definition.family) {
                InstancedBossFamily.Vorkath ->
                    spawn(npc, InstancedBossNpcs.zombifiedSpawn, 38, 0, 3)
                InstancedBossFamily.Nightmare ->
                    repeat(4) {
                        spawn(
                            npc,
                            InstancedBossNpcs.nightmareTotem,
                            100,
                            OFFSETS[it].first,
                            OFFSETS[it].second,
                        )
                    }
                InstancedBossFamily.Phosani ->
                    repeat(4) {
                        spawn(
                            npc,
                            InstancedBossNpcs.sleepwalker,
                            40,
                            OFFSETS[it].first,
                            OFFSETS[it].second,
                        )
                    }
                InstancedBossFamily.Muspah -> Unit
            }
        }
        if (!state.secondSpecial && npc.hitpoints <= definition.hitpoints / 3) {
            state.secondSpecial = true
            when (definition.family) {
                InstancedBossFamily.Nightmare ->
                    listOf(
                            InstancedBossNpcs.parasite,
                            InstancedBossNpcs.magicHusk,
                            InstancedBossNpcs.rangedHusk,
                        )
                        .forEachIndexed { index, type -> spawn(npc, type, 80, index - 1, 3) }
                InstancedBossFamily.Phosani ->
                    spawn(npc, InstancedBossNpcs.phosaniParasite, 100, 0, 3)
                else -> Unit
            }
        }
    }

    private fun spawn(owner: Npc, type: NpcType, hitpoints: Int, x: Int, z: Int) {
        val add =
            Npc(
                npcTypes[type],
                CoordGrid(owner.coords.x + x, owner.coords.z + z, owner.coords.level),
            )
        add.apply {
            this.hitpoints = hitpoints
            baseHitpointsLvl = hitpoints
            attackLvl = 180
            baseAttackLvl = 180
            strengthLvl = 180
            baseStrengthLvl = 180
            defenceLvl = 100
            baseDefenceLvl = 100
            magicLvl = 180
            baseMagicLvl = 180
            rangedLvl = 180
            baseRangedLvl = 180
            mode = NpcMode.Wander
            setHunt(14)
            setHuntMode(huntmodes.constant_melee)
        }
        state(owner).adds++
        npcs.add(add, 200)
    }

    private companion object {
        private val OFFSETS = listOf(-3 to 0, 3 to 0, 0 to -3, 0 to 3)
    }
}

public data class InstancedBossState(
    var attacks: Int = 0,
    var firstSpecial: Boolean = false,
    var secondSpecial: Boolean = false,
    var adds: Int = 0,
) {
    public fun consumeAttack(): Int = ++attacks
}
