package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class BlackDyeMaking @Inject constructor() {
    public fun start(access: ProtectedAccess, mushroomSlot: Int) {
        with(access) {
            clearHerbloreQueues()
            if (!player.members) {
                mes("You need to be on a members' world to use this item.")
                return
            }
            if (player.vars[varps.golem_quest] < QUEST_STARTED) {
                return
            }
            if (!hasPestleAndMortar() || player.inv[mushroomSlot]?.id != objs.black_mushroom.id) {
                return
            }
            val vialSlot = player.inv.indexOfFirst { it?.id == objs.empty_vial.id }
            if (vialSlot < 0) {
                return
            }
            anim(seqs.human_herbing_grind)
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, objs.black_mushroom.id, 1, slot = mushroomSlot)
                    delete(inventory, objs.empty_vial.id, 1, slot = vialSlot)
                    add(inventory, objs.black_dye.id, 1, slot = vialSlot)
                }
            check(result.success) { "Black-dye transaction failed after validation." }
        }
    }

    private fun ProtectedAccess.hasPestleAndMortar(): Boolean =
        player.inv.any { it?.id == objs.pestle_and_mortar.id }

    private companion object {
        const val QUEST_STARTED = 1
    }
}
