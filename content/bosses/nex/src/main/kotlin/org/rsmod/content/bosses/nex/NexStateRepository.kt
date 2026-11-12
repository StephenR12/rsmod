package org.rsmod.content.bosses.nex

import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.game.entity.Npc

@Singleton
public class NexStateRepository {
    private val states: MutableMap<Npc, NexState> = WeakHashMap()
    private val mageOwners: MutableMap<Npc, Npc> = WeakHashMap()
    private val reaverOwners: MutableMap<Npc, Npc> = WeakHashMap()

    public fun reset(nex: Npc): NexState = NexState().also { states[nex] = it }

    public fun state(nex: Npc): NexState? = states[nex]

    public fun bindMage(mage: Npc, nex: Npc) {
        mageOwners[mage] = nex
    }

    public fun nexForMage(mage: Npc): Npc? = mageOwners[mage]

    public fun bindReaver(reaver: Npc, nex: Npc) {
        reaverOwners[reaver] = nex
    }

    public fun nexForReaver(reaver: Npc): Npc? = reaverOwners[reaver]
}
