package org.rsmod.content.generic.npcs.services

import jakarta.inject.Inject
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onGameStartup
import org.rsmod.api.script.onMissingNpcService
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private data class Reward(
    val shop: String,
    val membersShop: Boolean,
    val payment: String,
    val owners: Map<String, Int>,
    val label: String,
    val goods: Map<String, Int>,
    val price: Int,
    val refund: Int?,
    val minimumScore: Int,
    val membersItem: Boolean,
) {
    val quantity: Int get() = goods.values.singleOrNull() ?: 1
    val display: String get() = if (quantity == 1) label else "$label (x$quantity)"
}

private val rewards = requireNotNull(Reward::class.java.getResourceAsStream("point-rewards.tsv"))
    .bufferedReader().useLines { lines ->
        fun amounts(value: String): Map<String, Int> = value.split(',').associate {
            val (name, amount) = it.split(':')
            name to amount.toInt()
        }
        lines.filterNot { it.startsWith('#') }.map { line ->
            val f = line.split('\t')
            Reward(f[0], f[1].toBooleanStrict(), f[2], amounts(f[3]), f[4], amounts(f[5]),
                f[6].toInt(), f[7].toIntOrNull(), f[8].toInt(), f[9].toBooleanStrict())
        }.toList()
    }

internal object RewardPeople : NpcReferences() {
    val names = rewards.flatMap { it.owners.keys }.distinct().associateWith { find(it) }
}

internal object RewardItems : ObjReferences() {
    val names = (rewards.flatMap { it.goods.keys } +
        rewards.map { it.payment }.filter { it.startsWith("i:") }.map { it.substring(2) })
        .distinct().associateWith { find(it) }
}

internal object RewardVarps : VarpReferences() {
    val targetKills = find("bh_2023_target_kill")
    val competitiveWins = find("br_competitive_wins")
    val currency = rewards.map { it.payment }.filter { it.startsWith("p:") }.distinct()
        .associateWith { find(it.substring(2)) }
}

// The native LMS interface receives these server-side account totals in shared interface varps 261 and 262.
internal object CompetitiveRewardSaves : VarpBuilder() {
    init {
        build("br_competitive_shop_points") { transmitNever = true }
        build("br_competitive_wins") { transmitNever = true }
    }
}

internal object RewardVarbits : VarBitReferences() {
    val currency = rewards.map { it.payment }.filter { it.startsWith("b:") }.distinct()
        .associateWith { find(it.substring(2)) }
}

public class PointRewardShops @Inject constructor(private val npcs: NpcTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup {
            for ((_, offers) in rewards.groupBy { it.shop }) {
                for ((owner, slot) in offers.first().owners) {
                    onMissingNpcService(npcs[RewardPeople.names.getValue(owner)], slot) {
                        open(offers)
                    }
                }
            }
        }
    }

    private suspend fun ProtectedAccess.open(offers: List<Reward>) {
        val shop = offers.first()
        if (shop.membersShop && !player.members) {
            mes("You need to be on a members' world to use this shop.")
            return
        }
        val refund =
            offers.any { it.refund != null } &&
                choice2("Browse rewards.", false, "Return a reward.", true)
        val stock = offers.filter { (!refund || it.refund != null) && (player.members || !it.membersItem) }
        val options = stock.map { "${it.display} - ${if (refund) it.refund else it.price}" }
        val item = stock.getOrNull(menu("${shop.shop} (${balance(shop.payment)} available)", true, options)) ?: return
        val lms = item.payment == "p:br_competitive_shop_points"
        val score = if (lms) vars[RewardVarps.competitiveWins] else vars[RewardVarps.targetKills]
        if (score < item.minimumScore) {
            val activity = if (lms) "competitive wins" else "target kills"
            mes("You need ${item.minimumScore} $activity to claim that reward.")
            return
        }
        val cost = if (refund) -requireNotNull(item.refund) else item.price
        val before = balance(shop.payment)
        if (before.toLong() - cost !in 0L..Int.MAX_VALUE.toLong()) {
            mes("You cannot afford that transaction.")
            return
        }
        val currencyItem = if (shop.payment.startsWith("i:")) RewardItems.names.getValue(shop.payment.substring(2)) else null
        val result = player.invTransaction(inv) {
            val bag = select(inv)
            if (currencyItem != null && cost > 0) delete(bag, currencyItem.id, cost)
            for ((symbol, quantity) in item.goods) {
                val id = RewardItems.names.getValue(symbol).id
                if (refund) delete(bag, id, quantity) else add(bag, id, quantity)
            }
            if (currencyItem != null && cost < 0) add(bag, currencyItem.id, -cost)
        }
        if (!result.success) {
            mes(if (refund) "Bring the reward you want to return." else "You need more inventory space.")
            return
        }
        // This protected action has no suspension between inventory commit and updating the balance.
        if (shop.payment.startsWith("p:")) {
            VarPlayerIntMapSetter.set(player, RewardVarps.currency.getValue(shop.payment), before - cost)
        } else if (shop.payment.startsWith("b:")) {
            VarPlayerIntMapSetter.set(player, RewardVarbits.currency.getValue(shop.payment), before - cost)
        }
    }

    private fun ProtectedAccess.balance(payment: String): Int = when {
        payment.startsWith("p:") -> vars[RewardVarps.currency.getValue(payment)]
        payment.startsWith("b:") -> vars[RewardVarbits.currency.getValue(payment)]
        else -> invTotal(inv, RewardItems.names.getValue(payment.substring(2)))
    }
}
