package org.rsmod.content.bosses.corporealbeast

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.min
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.hit.Hit
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

@Singleton
public class CorporealBeastController
@Inject
constructor(private val npcs: NpcRepository, private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, CorporealBeastState>()

    public fun reset(npc: Npc): CorporealBeastState =
        CorporealBeastState().also { states[npc] = it }

    public fun state(npc: Npc): CorporealBeastState = states.getOrPut(npc, ::CorporealBeastState)

    public fun onHit(npc: Npc, hit: Hit) {
        if (!hit.isFromPlayer) {
            return
        }
        val state = state(npc)
        if (!state.coreSpawned && npc.hitpoints <= CorporealBeastStats.HITPOINTS / 2) {
            state.coreSpawned = true
            summonDarkCore(npc)
            npc.say("A dark energy core breaks away from the beast!")
        }
    }

    public fun summonDarkCore(boss: Npc): Npc {
        val spawn = CoordGrid(boss.coords.x + 3, boss.coords.z, boss.coords.level)
        val core = Npc(npcTypes[CorporealBeastNpcs.darkCore], spawn)
        core.apply {
            hitpoints = DARK_CORE_HITPOINTS
            baseHitpointsLvl = DARK_CORE_HITPOINTS
            attackLvl = 1
            baseAttackLvl = 1
            strengthLvl = 1
            baseStrengthLvl = 1
            defenceLvl = 10
            baseDefenceLvl = 10
            mode = NpcMode.Wander
            setHunt(12)
            setHuntMode(huntmodes.constant_melee)
        }
        npcs.add(core, DARK_CORE_LIFETIME)
        return core
    }

    public fun healFromCore(core: Npc, amount: Int) {
        val boss =
            npcs.findAll(ZoneKey.from(core.coords), 1).firstOrNull {
                it.id == CorporealBeastNpcs.boss.id && distance(it.coords, core.coords) <= 12
            } ?: return
        boss.hitpoints = min(boss.baseHitpointsLvl, boss.hitpoints + amount)
    }

    public fun activeCores(npc: Npc): List<Npc> =
        npcs
            .findAll(ZoneKey.from(npc.coords), 1)
            .filter { it.id == CorporealBeastNpcs.darkCore.id && it.hitpoints > 0 }
            .toList()

    private fun distance(a: CoordGrid, b: CoordGrid): Int =
        maxOf(kotlin.math.abs(a.x - b.x), kotlin.math.abs(a.z - b.z))

    public companion object {
        public const val DARK_CORE_HITPOINTS: Int = 25
        public const val DARK_CORE_DRAIN: Int = 5
        private const val DARK_CORE_LIFETIME: Int = 500
    }
}

public data class CorporealBeastState(var attacks: Int = 0, var coreSpawned: Boolean = false)
