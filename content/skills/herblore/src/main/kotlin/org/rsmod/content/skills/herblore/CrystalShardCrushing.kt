package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class CrystalShardCrushing @Inject constructor() {
    public suspend fun start(access: ProtectedAccess, slot: Int) {
        with(access) {
            clearHerbloreQueues()
            if (!player.members) {
                mes("You need to be on a members' world to use this item.")
                return
            }
            if (player.vars[varbits.song_of_the_elves] < SONG_OF_THE_ELVES_COMPLETE) {
                mes("You need to have completed Song of the Elves to do this.")
                return
            }
            if (!hasPestleAndMortar()) {
                return
            }
            val initial = player.inv[slot] ?: return
            if (initial.id != objs.crystal_shard.id) {
                return
            }
            val maximum = min(initial.count, Int.MAX_VALUE / DUST_PER_SHARD)
            val requested = min(countDialog(), maximum)
            if (requested <= 0) {
                return
            }
            val current = player.inv[slot] ?: return
            if (
                !player.members ||
                    player.vars[varbits.song_of_the_elves] < SONG_OF_THE_ELVES_COMPLETE ||
                    !hasPestleAndMortar() ||
                    current.id != objs.crystal_shard.id ||
                    current.count != initial.count
            ) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.crystal_shard.id, requested, slot = slot)
                    if (requested == current.count) {
                        add(
                            inventory,
                            objs.crystal_dust.id,
                            requested * DUST_PER_SHARD,
                            slot = slot,
                        )
                    } else {
                        add(inventory, objs.crystal_dust.id, requested * DUST_PER_SHARD)
                    }
                }
            if (result.success) {
                anim(seqs.human_herbing_grind)
            }
        }
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    public companion object {
        public const val DUST_PER_SHARD: Int = 10
        public const val SONG_OF_THE_ELVES_COMPLETE: Int = 194
    }
}
