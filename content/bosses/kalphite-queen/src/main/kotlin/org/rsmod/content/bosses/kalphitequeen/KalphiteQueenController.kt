package org.rsmod.content.bosses.kalphitequeen

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.min
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.type.npc.NpcTypeList

@Singleton
public class KalphiteQueenController @Inject constructor(private val npcTypes: NpcTypeList) {
    private val states = IdentityHashMap<Npc, KalphiteQueenState>()

    public fun reset(npc: Npc): KalphiteQueenState = KalphiteQueenState().also { states[npc] = it }

    public fun state(npc: Npc): KalphiteQueenState = states.getOrPut(npc, ::KalphiteQueenState)

    public fun modifyHit(npc: Npc, hit: HitBuilder) {
        if (!state(npc).airborne) {
            hit.damage = min(hit.damage, (npc.hitpoints - 1).coerceAtLeast(0))
        }
    }

    public fun onHit(npc: Npc, hit: Hit) {
        if (hit.isFromPlayer && npc.hitpoints <= 1 && !state(npc).airborne) {
            transformToAirborne(npc)
        }
    }

    public fun transformToAirborne(npc: Npc) {
        state(npc).airborne = true
        npc.transmog(npcTypes[KalphiteQueenNpcs.airborne], Int.MAX_VALUE)
        npc.hitpoints = KalphiteQueenDefinition.HITPOINTS
        npc.baseHitpointsLvl = KalphiteQueenDefinition.HITPOINTS
        npc.say("The Kalphite Queen sheds her damaged carapace!")
    }
}

public data class KalphiteQueenState(var airborne: Boolean = false, var attacks: Int = 0)
