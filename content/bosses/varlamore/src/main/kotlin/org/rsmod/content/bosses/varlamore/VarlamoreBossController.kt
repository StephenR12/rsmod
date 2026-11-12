package org.rsmod.content.bosses.varlamore

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
public class VarlamoreBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, VarlamoreBossState>()

    public fun reset(npc: Npc): VarlamoreBossState = VarlamoreBossState().also { states[npc] = it }

    public fun state(npc: Npc): VarlamoreBossState = states.getOrPut(npc, ::VarlamoreBossState)

    public fun onBossDamaged(npc: Npc, definition: VarlamoreBossDefinition) {
        val state = state(npc)
        if (!state.firstSpecial && npc.hitpoints <= definition.hitpoints * 2 / 3) {
            state.firstSpecial = true
            when (definition.family) {
                VarlamoreBossFamily.Huey -> spawn(npc, VarlamoreBossNpcs.hueyTail, 300, -4, 0)
                VarlamoreBossFamily.BloodMoon ->
                    spawn(npc, VarlamoreBossNpcs.bloodJaguar, 100, -3, 0)
                VarlamoreBossFamily.BlueMoon ->
                    spawn(npc, VarlamoreBossNpcs.frozenWeapons, 100, 3, 0)
                VarlamoreBossFamily.EclipseMoon ->
                    repeat(3) { spawn(npc, VarlamoreBossNpcs.eclipseClone, 80, it - 1, 3) }
                VarlamoreBossFamily.GemstoneCrab -> Unit
            }
        }
        if (!state.secondSpecial && npc.hitpoints <= definition.hitpoints / 3) {
            state.secondSpecial = true
            if (definition.family == VarlamoreBossFamily.Huey) {
                repeat(3) { spawn(npc, VarlamoreBossNpcs.hueyBody, 150, it - 1, -3) }
            }
        }
    }

    private fun spawn(owner: Npc, type: NpcType, hitpoints: Int, x: Int, z: Int) {
        val npc =
            Npc(
                npcTypes[type],
                CoordGrid(owner.coords.x + x, owner.coords.z + z, owner.coords.level),
            )
        npc.apply {
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
        state(owner).adds++
        npcs.add(npc, 200)
    }
}

public data class VarlamoreBossState(
    var attacks: Int = 0,
    var firstSpecial: Boolean = false,
    var secondSpecial: Boolean = false,
    var adds: Int = 0,
) {
    public fun consumeAttack(): Int = ++attacks
}
