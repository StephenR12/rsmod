package org.rsmod.content.bosses.nex

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

public class NexController
@Inject
constructor(
    private val states: NexStateRepository,
    private val npcs: NpcRepository,
    private val players: PlayerRepository,
    private val npcTypes: NpcTypeList,
    private val random: GameRandom,
) {
    public fun reset(nex: Npc): NexState = states.reset(nex)

    public fun state(nex: Npc): NexState = states.state(nex) ?: states.reset(nex)

    public fun phase(nex: Npc): NexPhase = state(nex).phase

    public fun roomPlayers(nex: Npc, primary: Player): List<Player> =
        (sequenceOf(primary) + players.findAll(ZoneKey.from(nex.coords), 2))
            .filter { NexArena.contains(it.coords) }
            .distinct()
            .toList()

    public fun modifyNexHit(nex: Npc, hit: HitBuilder) {
        val state = state(nex)
        if (state.waitingForMage != null) {
            hit.damage = 0
            return
        }
        if (nex.currentMapClock < state.siphonUntil) {
            nex.hitpoints = min(nex.baseHitpointsLvl, nex.hitpoints + hit.damage)
            hit.damage = 0
            return
        }
        if (state.curse == NexCurse.DeflectMelee && hit.type == HitType.Melee) {
            hit.damage = 0
            return
        }
        if (state.phase != NexPhase.Zaros) {
            val damageToThreshold = (nex.hitpoints - state.phase.threshold).coerceAtLeast(0)
            hit.damage = min(hit.damage, damageToThreshold)
        }
    }

    public fun onNexHit(nex: Npc, hit: Hit) {
        if (!hit.isFromPlayer) {
            return
        }
        val state = state(nex)
        if (
            state.phase != NexPhase.Zaros &&
                state.waitingForMage == null &&
                nex.hitpoints <= state.phase.threshold
        ) {
            val mage = checkNotNull(state.phase.mage)
            state.waitingForMage = mage
            nex.say("${mageName(mage)}, don't fail me!")
        }
    }

    public fun modifyMageHit(mage: Npc, hit: HitBuilder) {
        if (!isMageActive(mage)) {
            hit.damage = 0
        }
    }

    public fun isMageActive(mage: Npc): Boolean {
        val nex = states.nexForMage(mage) ?: return false
        return state(nex).waitingForMage?.id == mage.id
    }

    public fun onMageKilled(mage: Npc) {
        val nex = states.nexForMage(mage) ?: return
        val state = state(nex)
        if (state.waitingForMage?.id != mage.id) {
            return
        }
        val next = NexPhase.entries.getOrNull(state.phase.ordinal + 1) ?: return
        state.phase = next
        state.waitingForMage = null
        state.attacks = 0
        state.specialToggle = false
        state.siphonUntil = Int.MIN_VALUE
        if (next == NexPhase.Zaros) {
            nex.hitpoints = min(nex.baseHitpointsLvl, nex.hitpoints + ZAROS_HEAL)
            nex.spotanim(NexSpotanims.turmoil)
            nex.say("NOW, THE POWER OF ZAROS!")
        } else {
            nex.say(phaseCall(next))
        }
    }

    public fun consumeAttack(nex: Npc): Boolean {
        val state = state(nex)
        state.attacks++
        return state.phase != NexPhase.Zaros && state.attacks % SPECIAL_INTERVAL == 0
    }

    public fun toggleSpecial(nex: Npc): Boolean {
        val state = state(nex)
        state.specialToggle = !state.specialToggle
        return state.specialToggle
    }

    public fun beginSiphon(nex: Npc) {
        val state = state(nex)
        state.siphonUntil = nex.currentMapClock + SIPHON_DURATION
        nex.spotanim(NexSpotanims.bloodSiphon)
        repeat(REAVER_COUNT) { index ->
            val offset = REAVER_OFFSETS[index]
            val coords =
                CoordGrid(
                    nex.coords.x + offset.first,
                    nex.coords.z + offset.second,
                    nex.coords.level,
                )
            val reaver = Npc(npcTypes[NexNpcs.bloodReaver], coords)
            reaver.apply {
                hitpoints = REAVER_HITPOINTS
                baseHitpointsLvl = REAVER_HITPOINTS
                attackLvl = 120
                baseAttackLvl = 120
                strengthLvl = 120
                baseStrengthLvl = 120
                defenceLvl = 80
                baseDefenceLvl = 80
                mode = NpcMode.Wander
                setHunt(18)
                setHuntMode(huntmodes.constant_melee)
            }
            states.bindReaver(reaver, nex)
            npcs.add(reaver, REAVER_LIFETIME)
        }
    }

    public fun bindMage(mage: Npc, nex: Npc) {
        states.bindMage(mage, nex)
    }

    public fun wrath(nex: Npc) {
        players
            .findAll(ZoneKey.from(nex.coords), 1)
            .filter { NexArena.contains(it.coords) }
            .filter {
                kotlin.math.max(
                    kotlin.math.abs(it.coords.x - nex.coords.x),
                    kotlin.math.abs(it.coords.z - nex.coords.z),
                ) <= 2
            }
            .forEach { player ->
                player.queueHit(nex, WRATH_DELAY, HitType.Typeless, random.of(1..WRATH_MAX_HIT))
            }
    }

    private fun mageName(type: NpcType): String =
        when (type.id) {
            NexNpcs.fumus.id -> "Fumus"
            NexNpcs.umbra.id -> "Umbra"
            NexNpcs.cruor.id -> "Cruor"
            else -> "Glacies"
        }

    private fun phaseCall(phase: NexPhase): String =
        when (phase) {
            NexPhase.Shadow -> "Darken my shadow!"
            NexPhase.Blood -> "Flood my lungs with blood!"
            NexPhase.Ice -> "Infuse me with the power of ice!"
            else -> ""
        }

    public companion object {
        public const val ZAROS_HEAL: Int = 500
        public const val SPECIAL_INTERVAL: Int = 5
        public const val SIPHON_DURATION: Int = 8
        public const val REAVER_HITPOINTS: Int = 100
        public const val WRATH_MAX_HIT: Int = 50
        private const val WRATH_DELAY: Int = 3
        private const val REAVER_COUNT: Int = 3
        private const val REAVER_LIFETIME: Int = 40
        private val REAVER_OFFSETS = listOf(-2 to -2, 2 to -2, 0 to 2)
    }
}
