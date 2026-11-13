package org.rsmod.content.skills.farming

import kotlin.random.Random
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// The tier, number of each remaining reward and Tithe origin travel with the individual pack.
private data class Pack(
    val tier: Int,
    val low: Int,
    val medium: Int,
    val high: Int,
    val tithe: Boolean,
    val seed: Int,
) {
    val remaining: Int
        get() = low + medium + high

    fun encode(): Int =
        tier or
            (low shl 3) or
            (medium shl 7) or
            (high shl 11) or
            (if (tithe) 1 shl 15 else 0) or
            (seed shl 16)

    companion object {
        fun read(bits: Int) =
            Pack(
                bits and 7,
                (bits ushr 3) and 15,
                (bits ushr 7) and 15,
                (bits ushr 11) and 15,
                bits and (1 shl 15) != 0,
                bits ushr 16,
            )
    }
}

internal fun ProtectedAccess.seedPack(tier: Int, tithe: Boolean = false): Int {
    val medium =
        when (tier) {
            1 -> 1 + random.of(3)
            2 -> 2 + random.of(2)
            3 -> 2 + random.of(3)
            4 -> 3 + random.of(3)
            else -> 4 + random.of(3)
        }
    val high =
        when (tier) {
            1 -> 0
            2 -> if (random.of(11) == 0) 1 else 0
            3 -> random.of(2)
            4 -> 1 + random.of(2)
            else -> 1 + random.of(3)
        }
    return Pack(tier, tier + 5 - medium - high, medium, high, tithe, random.of(65536)).encode()
}

public class SeedPacks : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ContractItems.pack) { take(it.slot, false) }
        onOpHeld3(ContractItems.pack) { take(it.slot, true) }
        onOpHeld4(ContractItems.pack) {
            val contents = contents(it.slot)
            mes("This tier ${contents.tier} seed pack has ${contents.remaining} rewards left.")
        }
    }

    private fun ProtectedAccess.contents(slot: Int): Pack {
        val item = inv.getValue(slot)
        if (item.vars != 0) return Pack.read(item.vars)
        // Older packs from Gricoller's reward shop were issued without contents.
        val bits = seedPack(3, true)
        inv[slot] = InvObj(ContractItems.pack, vars = bits)
        return Pack.read(bits)
    }

    private fun ProtectedAccess.take(slot: Int, all: Boolean) {
        var pack = contents(slot)
        do {
            // Keep the roll stable when a full inventory prevents delivering the next seed.
            val rollSource =
                Random((pack.seed.toLong() shl 32) or (pack.encode().toLong() and 0xffff))
            val table =
                when {
                    pack.low > 0 -> SeedPackRewards.low
                    pack.medium > 0 -> SeedPackRewards.medium
                    else -> SeedPackRewards.high
                }
            val whiteLily = SeedPackRewards.medium.last()
            val regular = if (table === SeedPackRewards.medium) table.dropLast(1) else table
            val reward =
                if (table === SeedPackRewards.medium && pack.tithe && rollSource.nextInt(90) == 0)
                    whiteLily
                else {
                    var roll = rollSource.nextInt(regular.sumOf { it.weight })
                    regular.first {
                        roll -= it.weight
                        roll < 0
                    }
                }
            val count = reward.minimum + rollSource.nextInt(reward.maximum - reward.minimum + 1)
            val next =
                when {
                    pack.low > 0 -> pack.copy(low = pack.low - 1)
                    pack.medium > 0 -> pack.copy(medium = pack.medium - 1)
                    else -> pack.copy(high = pack.high - 1)
                }
            val tx =
                player.invTransaction(inv) {
                    val bag = select(inv)
                    delete(bag, ContractItems.pack.id, 1, slot)
                    if (next.remaining > 0)
                        add(bag, ContractItems.pack.id, 1, vars = next.encode(), slot = slot)
                    add(bag, reward.seed.id, count)
                }
            if (tx.failure) {
                mes("Make room for the seeds.")
                return
            }
            pack = next
        } while (all && pack.remaining > 0)
    }
}
