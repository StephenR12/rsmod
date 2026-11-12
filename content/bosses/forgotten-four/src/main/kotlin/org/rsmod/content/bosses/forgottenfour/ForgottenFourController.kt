package org.rsmod.content.bosses.forgottenfour

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

@Singleton
public class ForgottenFourController
@Inject
constructor(
    private val npcs: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val random: GameRandom,
) {
    private val states = IdentityHashMap<Npc, ForgottenBossState>()
    private val minionOwners = IdentityHashMap<Npc, Npc>()
    private val encounterOwners = IdentityHashMap<Npc, Player>()
    private val sanity = IdentityHashMap<Player, Int>()
    private val pendingShadowCasts = Collections.newSetFromMap(IdentityHashMap<Player, Boolean>())

    public fun reset(npc: Npc): ForgottenBossState {
        clearAdds(npc)
        encounterOwners.remove(npc)
        return ForgottenBossState(whispererRotation = random.of(2)).also { states[npc] = it }
    }

    public fun state(npc: Npc): ForgottenBossState = states.getOrPut(npc, ::ForgottenBossState)

    public fun claim(npc: Npc, player: Player): Boolean {
        val owner = encounterOwners[npc]
        if (owner == null) {
            encounterOwners[npc] = player
            sanity[player] = MAX_SANITY
            return true
        }
        return owner === player
    }

    public fun ownerFor(npc: Npc): Player? = encounterOwners[npc]

    public fun rememberShadowCast(player: Player) {
        pendingShadowCasts += player
    }

    public fun consumeShadowCast(player: Player): Boolean = pendingShadowCasts.remove(player)

    public fun sanity(player: Player): Int = sanity.getOrPut(player) { MAX_SANITY }

    public fun adjustSanity(player: Player, amount: Int): Int {
        val adjusted = (sanity(player) + amount).coerceIn(0, MAX_SANITY)
        sanity[player] = adjusted
        return adjusted
    }

    public fun markMistake(npc: Npc) {
        state(npc).perfect = false
    }

    public fun modifyHit(npc: Npc, hit: HitBuilder, definition: ForgottenBossDefinition) {
        val state = state(npc)
        if (definition.family == ForgottenBossFamily.Leviathan && state.leviathanStunned) {
            hit.damage = min(hit.damage, LEVIATHAN_STUN_DAMAGE_CAP)
            if (hit.isFromPlayer && hit.damage > 0) {
                state.leviathanStunned = false
                state.leviathanVolley = min(6, state.leviathanStuns + 1)
                state.leviathanVolleyAttack = 0
            }
        }
        if (
            definition.family == ForgottenBossFamily.Whisperer &&
                !state.enraged &&
                hit.damage >= npc.hitpoints
        ) {
            state.enraged = true
            npc.hitpoints = WHISPERER_ENRAGE_HEAL
            hit.damage = 0
        }
        if (
            definition.family == ForgottenBossFamily.Whisperer && state.activeMinions.isNotEmpty()
        ) {
            hit.damage = 0
        }
    }

    public fun onBossDamaged(npc: Npc, player: Player, definition: ForgottenBossDefinition) {
        if (!claim(npc, player)) return
        val state = state(npc)
        when (definition.family) {
            ForgottenBossFamily.Leviathan -> {
                if (consumeShadowCast(player)) {
                    state.leviathanStunned = true
                    state.leviathanStuns++
                    npc.actionDelay += LEVIATHAN_STUN_TICKS
                }
                if (npc.hitpoints <= LEVIATHAN_ENRAGE_HP) state.enraged = true
            }
            ForgottenBossFamily.Whisperer -> queueWhispererSpecial(npc, state)
            ForgottenBossFamily.Vardorvis -> scaleVardorvis(npc, definition)
            ForgottenBossFamily.Duke ->
                if (npc.hitpoints <= definition.hitpoints / 4) state.enraged = true
        }
    }

    public fun spawnAdds(
        boss: Npc,
        types: List<NpcType>,
        hitpoints: Int,
        duration: Int = 50,
    ): List<Npc> =
        types.mapIndexed { index, type ->
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
            minionOwners[minion] = boss
            state(boss).activeMinions.add(minion)
            npcs.add(minion, duration)
            minion
        }

    public fun spawnLeviathanPathfinder(boss: Npc): Npc {
        val state = state(boss)
        state.pathfinder?.let {
            return it
        }
        val pathfinder =
            Npc(
                npcTypes[ForgottenFourNpcs.leviathanPathfinder],
                CoordGrid(boss.coords.x - 5, boss.coords.z + 5, boss.coords.level),
            )
        pathfinder.mode = NpcMode.Wander
        npcs.add(pathfinder, 200)
        state.pathfinder = pathfinder
        return pathfinder
    }

    public fun onMinionKilled(npc: Npc) {
        val owner = minionOwners.remove(npc) ?: return
        state(owner).activeMinions.remove(npc)
    }

    public fun finishSoulSiphon(boss: Npc): Boolean {
        val state = state(boss)
        val failed = state.activeMinions.any { it.hitpoints > 0 }
        if (failed) {
            boss.hitpoints = min(boss.baseHitpointsLvl, boss.hitpoints + 100)
            state.perfect = false
        }
        clearAdds(boss)
        return !failed
    }

    public fun clearAdds(boss: Npc) {
        val state = states[boss] ?: return
        state.activeMinions.toList().forEach { minion ->
            minionOwners.remove(minion)
            if (minion.slotId != INVALID_SLOT) {
                npcs.del(minion, Int.MAX_VALUE)
            }
        }
        state.activeMinions.clear()
        state.pathfinder?.let { pathfinder ->
            if (pathfinder.slotId != INVALID_SLOT) {
                npcs.del(pathfinder, Int.MAX_VALUE)
            }
        }
        state.pathfinder = null
    }

    private fun queueWhispererSpecial(npc: Npc, state: ForgottenBossState) {
        val thresholds = intArrayOf(720, 495, 270)
        if (state.whispererSpecials >= thresholds.size) return
        if (npc.hitpoints > thresholds[state.whispererSpecials]) return
        val rotation =
            if (state.whispererRotation == 0) {
                WHISPERER_ROTATION_A
            } else {
                WHISPERER_ROTATION_B
            }
        state.pendingSpecial = rotation[state.whispererSpecials]
        state.whispererSpecials++
    }

    private fun scaleVardorvis(npc: Npc, definition: ForgottenBossDefinition) {
        val remaining = npc.hitpoints.coerceIn(0, definition.hitpoints)
        val missing = definition.hitpoints - remaining
        val defence = max(1, definition.defence * remaining / definition.hitpoints)
        val strength = definition.strength + 190 * missing / definition.hitpoints
        npc.defenceLvl = defence
        npc.baseDefenceLvl = defence
        npc.strengthLvl = strength
        npc.baseStrengthLvl = strength
        state(npc).enraged = remaining <= definition.hitpoints / 3
    }

    public companion object {
        public const val MAX_SANITY: Int = 100
        public const val LEVIATHAN_ENRAGE_HP: Int = 180
        public const val LEVIATHAN_STUN_TICKS: Int = 15
        public const val LEVIATHAN_STUN_DAMAGE_CAP: Int = 10
        public const val WHISPERER_ENRAGE_HEAL: Int = 140

        private val WHISPERER_ROTATION_A =
            listOf(
                ForgottenSpecial.CorruptedSeeds,
                ForgottenSpecial.Screech,
                ForgottenSpecial.SoulSiphon,
            )
        private val WHISPERER_ROTATION_B =
            listOf(
                ForgottenSpecial.Screech,
                ForgottenSpecial.SoulSiphon,
                ForgottenSpecial.CorruptedSeeds,
            )
        private val OFFSETS =
            listOf(
                -4 to -4,
                0 to -4,
                4 to -4,
                4 to 0,
                4 to 4,
                0 to 4,
                -4 to 4,
                -4 to 0,
                -2 to -4,
                2 to -4,
                2 to 4,
                -2 to 4,
            )
    }
}

public enum class ForgottenSpecial {
    CorruptedSeeds,
    Screech,
    SoulSiphon,
}

public data class ForgottenBossState(
    var attacks: Int = 0,
    var enrageAttacks: Int = 0,
    var enraged: Boolean = false,
    var perfect: Boolean = true,
    var pendingSpecial: ForgottenSpecial? = null,
    var whispererSpecials: Int = 0,
    var whispererRotation: Int = 0,
    var leviathanVolley: Int = 0,
    var leviathanVolleyAttack: Int = 0,
    var leviathanSpecials: Int = 0,
    var leviathanStuns: Int = 0,
    var leviathanStunned: Boolean = false,
    var pathfinder: Npc? = null,
    val activeMinions: MutableSet<Npc> = Collections.newSetFromMap(IdentityHashMap<Npc, Boolean>()),
) {
    public fun consumeAttack(): Int = ++attacks
}
