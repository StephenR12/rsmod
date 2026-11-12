package org.rsmod.content.bosses.wilderness

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.min
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Singleton
public class WildernessBossController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, WildernessBossState>()

    public fun reset(npc: Npc): WildernessBossState =
        WildernessBossState().also { states[npc] = it }

    public fun state(npc: Npc): WildernessBossState = states.getOrPut(npc, ::WildernessBossState)

    public fun modifyHit(npc: Npc, hit: HitBuilder, definition: WildernessBossDefinition) {
        val state = state(npc)
        if (definition.family == WildernessBossFamily.Vetion && activeHounds(npc)) {
            hit.damage = 0
            return
        }
        if (definition.family == WildernessBossFamily.Vetion && !state.enraged) {
            hit.damage = min(hit.damage, (npc.hitpoints - 1).coerceAtLeast(0))
        }
    }

    public fun onHit(npc: Npc, hit: Hit, definition: WildernessBossDefinition) {
        if (!hit.isFromPlayer) {
            return
        }
        val state = state(npc)
        when (definition.family) {
            WildernessBossFamily.Vetion -> {
                if (!state.houndsSpawned && npc.hitpoints <= npc.baseHitpointsLvl / 2) {
                    state.houndsSpawned = true
                    spawnHounds(npc, definition, senior = state.enraged)
                    npc.say("Kill, my pets!")
                }
                if (!state.enraged && npc.hitpoints <= 1 && !activeHounds(npc)) {
                    state.enraged = true
                    state.houndsSpawned = false
                    npc.hitpoints = npc.baseHitpointsLvl
                    npc.say("Do it again!")
                }
            }
            WildernessBossFamily.Scorpia ->
                if (!state.guardiansSpawned && npc.hitpoints <= 99) {
                    state.guardiansSpawned = true
                    spawnGuardians(npc)
                }
            else -> Unit
        }
    }

    public fun healScorpia(guardian: Npc) {
        val scorpia =
            npcs.findAll(ZoneKey.from(guardian.coords), 1).firstOrNull {
                it.id == WildernessBossNpcs.scorpia.id && distance(it.coords, guardian.coords) <= 3
            } ?: return
        scorpia.hitpoints = min(scorpia.baseHitpointsLvl, scorpia.hitpoints + SCORPIA_HEAL)
    }

    public fun activeHounds(npc: Npc): Boolean {
        val ids =
            setOf(
                WildernessBossNpcs.juniorHound.id,
                WildernessBossNpcs.seniorHound.id,
                WildernessBossNpcs.juniorHoundSingle.id,
                WildernessBossNpcs.seniorHoundSingle.id,
            )
        return npcs.findAll(ZoneKey.from(npc.coords), 1).any { it.id in ids && it.hitpoints > 0 }
    }

    private fun spawnHounds(npc: Npc, definition: WildernessBossDefinition, senior: Boolean) {
        val type =
            when {
                definition.lesser && senior -> WildernessBossNpcs.seniorHoundSingle
                definition.lesser -> WildernessBossNpcs.juniorHoundSingle
                senior -> WildernessBossNpcs.seniorHound
                else -> WildernessBossNpcs.juniorHound
            }
        spawnMinion(type, offset(npc.coords, -2, 0), HOUND_HITPOINTS)
        spawnMinion(type, offset(npc.coords, 2, 0), HOUND_HITPOINTS)
    }

    private fun spawnGuardians(npc: Npc) {
        spawnMinion(WildernessBossNpcs.scorpiaGuardian, offset(npc.coords, -3, 0), 100)
        spawnMinion(WildernessBossNpcs.scorpiaGuardian, offset(npc.coords, 3, 0), 100)
    }

    public fun spawnSpiderlings(npc: Npc, lesser: Boolean) {
        val type =
            if (lesser) WildernessBossNpcs.spindelSpiderling
            else WildernessBossNpcs.venenatisSpiderling
        listOf(-2 to -2, 2 to -2, 0 to 2).forEach { (x, z) ->
            spawnMinion(type, offset(npc.coords, x, z), SPIDERLING_HITPOINTS)
        }
    }

    private fun spawnMinion(type: NpcType, coords: CoordGrid, hitpoints: Int) {
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
            rangedLvl = 100
            baseRangedLvl = 100
            mode = NpcMode.Wander
            setHunt(12)
            setHuntMode(huntmodes.constant_melee)
        }
        npcs.add(minion, MINION_LIFETIME)
    }

    private fun offset(coords: CoordGrid, x: Int, z: Int): CoordGrid =
        CoordGrid(coords.x + x, coords.z + z, coords.level)

    private fun distance(a: CoordGrid, b: CoordGrid): Int =
        maxOf(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.z - b.z))

    public companion object {
        public const val HOUND_HITPOINTS: Int = 33
        public const val SPIDERLING_HITPOINTS: Int = 5
        public const val SCORPIA_HEAL: Int = 2
        private const val MINION_LIFETIME: Int = 100
    }
}

public data class WildernessBossState(
    var attacks: Int = 0,
    var enraged: Boolean = false,
    var houndsSpawned: Boolean = false,
    var guardiansSpawned: Boolean = false,
)
