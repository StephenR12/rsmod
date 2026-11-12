package org.rsmod.content.skills.thieving

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

public class CoinPouches @Inject constructor() {
    public fun limit(access: ProtectedAccess): Int =
        with(access) {
            when {
                player.vars[varbits.ardougne_diary_elite_complete] != 0 -> 140
                player.vars[varbits.ardougne_diary_hard_complete] != 0 -> 84
                player.vars[varbits.ardougne_diary_medium_complete] != 0 -> 56
                else -> 28
            }
        }

    public fun count(access: ProtectedAccess): Int =
        ThievingObjs.allCoinPouches.countIn(access.player.inv)

    public fun open(
        access: ProtectedAccess,
        slot: Int,
        definition: CoinPouchDefinition,
        all: Boolean,
    ) {
        with(access) {
            val pouch = player.inv[slot] ?: return
            if (pouch.id != definition.obj.id) {
                return
            }
            val opened = if (all) pouch.count else 1
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, pouch.id, count = opened, slot = slot)
                    add(inventory, objs.coins.id, count = definition.roll(random, opened))
                }
            check(transaction.success) { "Coin-pouch transaction failed after validation." }
            soundSynth(synths.coins_jingle_1)
        }
    }

    public fun openCitizen(access: ProtectedAccess, slot: Int, all: Boolean) =
        open(access, slot, CoinPouchDefinitions.citizen, all)
}
