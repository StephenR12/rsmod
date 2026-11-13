package org.rsmod.content.minigames.pestcontrol

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object PestRewardInterface : InterfaceReferences() {
    val shop = find("pest_rewardshop")
}

internal object PestRewardButtons : ComponentReferences() {
    val confirm = find("pest_rewardshop:confirm")
}

/** The client constructs the confirmation child using the index from its native reward enum. */
public class PestRewards @Inject constructor(private val random: GameRandom) : PluginScript() {
    private val training =
        listOf(
            stats.attack,
            stats.strength,
            stats.defence,
            stats.ranged,
            stats.magic,
            stats.hitpoints,
            stats.prayer,
        )
    private val gearIndices = listOf(3, 0, 1, 2, 6, 5, 4, 7)
    private val prices = listOf(250, 250, 250, 150, 200, 200, 200, 10)

    override fun ScriptContext.startup() {
        for (knight in PestNpcs.knights) {
            onOpNpc1(knight) { open() }
            onOpNpc3(knight) { open() }
        }
        onIfModalButton(PestRewardButtons.confirm) {
            if (it.op.slot != 1 || !player.members) return@onIfModalButton
            when (val index = it.comsub) {
                in 0..20 -> train(index / 3, listOf(1, 10, 100)[index % 3])
                in 21..23 -> purchasePack(index)
                in 24..31 -> purchaseArmour(index - 24)
            }
            vars[varps.if1] = player.commendations
        }
    }

    private fun ProtectedAccess.open() {
        if (!player.members) return
        vars[varps.if1] = player.commendations
        ifOpenMainModal(PestRewardInterface.shop)
        ifSetEvents(PestRewardButtons.confirm, 0..31, IfEvent.Op1)
    }

    private fun ProtectedAccess.train(index: Int, points: Int) {
        val stat = training[index]
        val level = player.statBase(stat)
        if (level < 25 || !canPay(points)) return
        val multiplier =
            when (stat) {
                stats.prayer -> 18
                stats.magic,
                stats.ranged -> 32
                else -> 35
            }
        val bonus =
            when (points) {
                100 -> 110
                10 -> 101
                else -> 100
            }
        val xp = (level * level / 600) * multiplier * points * bonus / 100
        player.commendations -= points
        statAdvance(stat, xp.toDouble())
    }

    private fun ProtectedAccess.canPay(points: Int): Boolean {
        if (player.commendations >= points) return true
        mes("You do not have enough commendation points.")
        return false
    }

    private fun ProtectedAccess.purchaseArmour(index: Int) {
        val price = prices[index]
        if (!canPay(price)) return
        if (
            index != 7 && training.any { player.statBase(it) < if (it == stats.prayer) 22 else 42 }
        ) {
            mes(
                "You need level 42 in each combat skill and 22 Prayer to claim Void Knight equipment."
            )
            return
        }
        if (invAdd(inv, PestStock.gear[gearIndices[index]]).success) player.commendations -= price
        else mes("You need more inventory space.")
    }

    private fun ProtectedAccess.purchasePack(index: Int) {
        val stat = listOf(stats.herblore, stats.mining, stats.farming)[index - 21]
        val cost = if (index == 21) 30 else 15
        if (player.statBase(stat) < 25 || !canPay(cost)) return
        val contents =
            when (index) {
                21 -> herbs.map { (id, range) -> id to random.of(range) }
                22 -> listOf(454 to 25, 441 to 18)
                else -> List(3) { rollSeed() }
            }
        val result =
            player.invTransaction(inv) {
                val bag = select(inv)
                for ((id, count) in contents) add(bag, id, count)
            }
        if (result.success) player.commendations -= cost
        else mes("You need more inventory space for this pack.")
    }

    private val herbs =
        listOf(206 to 2..4, 208 to 2..3, 3050 to 1..4, 210 to 2..4, 212 to 1..4, 214 to 1..5)

    private data class Seed(val id: Int, val count: Int, val weight: Int)

    private val seeds =
        listOf(
            Seed(5317, 1, 2),
            Seed(5316, 1, 2),
            Seed(5315, 1, 16),
            Seed(5314, 1, 20),
            Seed(5313, 1, 40),
            Seed(5312, 1, 60),
            Seed(5295, 1, 40),
            Seed(5320, 3, 240),
            Seed(5323, 2, 180),
            Seed(5100, 2, 200),
            Seed(5321, 2, 180),
            Seed(5322, 6, 220),
            Seed(5324, 9, 200),
            Seed(5319, 11, 200),
            Seed(5318, 14, 400),
            Seed(22879, 2, 120),
            Seed(22869, 1, 1),
            Seed(22871, 1, 1),
        )

    private fun rollSeed(): Pair<Int, Int> {
        var roll = random.of(2122)
        for (seed in seeds) {
            roll -= seed.weight
            if (roll < 0) return seed.id to seed.count
        }
        error("Pest Control seed table is incomplete.")
    }
}
