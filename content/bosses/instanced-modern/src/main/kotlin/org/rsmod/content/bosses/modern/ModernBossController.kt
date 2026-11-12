package org.rsmod.content.bosses.modern

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
public class ModernBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, ModernBossState>()

    public fun reset(npc: Npc): ModernBossState = ModernBossState().also { states[npc] = it }

    public fun state(npc: Npc): ModernBossState = states.getOrPut(npc, ::ModernBossState)

    public fun onBossDamaged(npc: Npc, definition: ModernBossDefinition) {
        val state = state(npc)
        val maximum = npc.baseHitpointsLvl.coerceAtLeast(definition.hitpoints)
        if (!state.firstSpecial && npc.hitpoints <= maximum * 2 / 3) {
            state.firstSpecial = true
            when (definition.family) {
                ModernBossFamily.Amoxliatl -> spawnRing(npc, ModernBossNpcs.amoxliatlIce, 25, 4)
                ModernBossFamily.Branda -> spawnRing(npc, ModernBossNpcs.fireElemental, 45, 3)
                ModernBossFamily.Eldric -> spawnRing(npc, ModernBossNpcs.iceElemental, 45, 3)
                ModernBossFamily.Yama -> spawnRing(npc, ModernBossNpcs.yamaVoidflare, 80, 2)
                ModernBossFamily.Doom -> {
                    spawn(npc, ModernBossNpcs.doomEnergyRanged, 60, -3, 2)
                    spawn(npc, ModernBossNpcs.doomEnergyMagic, 60, 0, 3)
                    spawn(npc, ModernBossNpcs.doomEnergyMelee, 60, 3, 2)
                }
            }
        }
        if (!state.secondSpecial && npc.hitpoints <= maximum / 3) {
            state.secondSpecial = true
            when (definition.family) {
                ModernBossFamily.Amoxliatl -> spawnRing(npc, ModernBossNpcs.amoxliatlIce, 35, 6)
                ModernBossFamily.Branda -> spawnRing(npc, ModernBossNpcs.fireElemental, 60, 4)
                ModernBossFamily.Eldric -> spawnRing(npc, ModernBossNpcs.iceElemental, 60, 4)
                ModernBossFamily.Yama -> {
                    spawn(npc, ModernBossNpcs.yamaJudge, 120, -3, 3)
                    spawn(npc, ModernBossNpcs.yamaMeteor, 1, 3, 3)
                }
                ModernBossFamily.Doom -> {
                    npc.transmog(npcTypes[ModernBossNpcs.doomShielded], 100)
                    spawn(npc, ModernBossNpcs.doomEnergyGiantRanged, 120, -4, 3)
                    spawn(npc, ModernBossNpcs.doomEnergyGiantMagic, 120, 4, 3)
                }
            }
        }
    }

    public fun setDoomDelve(npc: Npc, level: Int) {
        val normalized = level.coerceAtLeast(1)
        val hitpoints = doomHitpoints(normalized)
        state(npc).delve = normalized
        npc.hitpoints = hitpoints
        npc.baseHitpointsLvl = hitpoints
        npc.transmog(npcTypes[ModernBossNpcs.doomBurrowed], 1)
    }

    public fun doomHitpoints(level: Int): Int =
        when (level.coerceAtLeast(1)) {
            1 -> 525
            2 -> 550
            3 -> 575
            4 -> 600
            5 -> 625
            6,
            7 -> 650
            8 -> 700
            else -> 625
        }

    private fun spawnRing(owner: Npc, type: NpcType, hitpoints: Int, count: Int) {
        repeat(count) { index ->
            val offset = OFFSETS[index % OFFSETS.size]
            spawn(owner, type, hitpoints, offset.first, offset.second)
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
        private val OFFSETS = listOf(-3 to 0, 3 to 0, 0 to -3, 0 to 3, -3 to -3, 3 to 3)
    }
}

public data class ModernBossState(
    var attacks: Int = 0,
    var firstSpecial: Boolean = false,
    var secondSpecial: Boolean = false,
    var adds: Int = 0,
    var delve: Int = 1,
) {
    public fun consumeAttack(): Int = ++attacks
}
