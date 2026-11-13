package org.rsmod.content.skills.herblore.mixology

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.*
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.*
import org.rsmod.api.shops.onOmniShopButton
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object MixShopUi : InterfaceReferences() {
    val main = find("omnishop_main")
    val side = find("omnishop_side")
}

internal object MixShopComps : ComponentReferences() {
    val list = find("omnishop_main:list")
    val items = find("omnishop_side:items")
    val buy = (1..4).map { find("omnishop_main:button_$it") }
    val sell = (1..4).map { find("omnishop_side:button_$it") }
}

internal object MixShopVars : VarpReferences() {
    val last = find("omnishop_lastshop")
    val selected = find("omnishop_selected_id")
    val side = find("omnishop_selected_side_id")
    val redraw = find("omnishop_redraw")
}

internal object MixPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<MixologyRewards>("mixology.toml")
    }
}

public data class MixReward(
    val item: Int,
    val cost: List<Int>,
    val level: Int = 60,
    val refund: Boolean = false,
)

public class MixologyRewards
@Inject
constructor(
    private val rows: DbRowTypeList,
    private val types: ObjTypeList,
    private val game: MasteringMixology,
) : PluginScript() {
    public val goods: List<MixReward> by lazy {
        rows.getValue(3746).data.getValue(5).map { entry ->
            val data = rows.getValue(entry as Int).data
            val item = data.getValue(0).single() as Int
            val cost = MutableList(3) { 0 }
            for (pair in data.getValue(6).chunked(2)) {
                val i =
                    when (pair[0] as Int) {
                        3748 -> 0
                        3747 -> 1
                        3749 -> 2
                        else -> error("Unknown Mixology currency")
                    }
                cost[i] = pair[1] as Int
            }
            MixReward(
                item,
                cost,
                when (item) {
                    29972 -> 70
                    29973 -> 85
                    else -> 60
                },
                item in setOf(29974, 29976, 29978, 29980, 29982, 29984, 29986),
            )
        }
    }

    override fun ScriptContext.startup() {
        for (npc in MixNpcs.supervisors) {
            onOpNpc1(npc) {
                if (!game.gate(player)) {
                    mes("You need level 60 Herblore and completion of Children of the Sun.")
                    return@onOpNpc1
                }
                vars[MixBits.met] = 1
                when (
                    menu(
                        "Alchemical Society",
                        "How do I help in the laboratory?",
                        "Show me the rewards.",
                        "Goodbye.",
                    )
                ) {
                    1 -> {
                        vars[MixBits.introduction] = 1
                        game.ensureOrders(player)
                        mes(
                            "Refine clean herbs into paste, fill the hopper, pull the levers in threes, process your mixtures and deliver the requested potions."
                        )
                    }
                    2 -> open(this)
                }
            }
            onOpNpc3(npc) { open(this) }
        }
        onOmniShopButton(3746, MixShopComps.list) { e ->
            val index = e.comsub / 12
            if (e.comsub < 0 || index !in goods.indices) return@onOmniShopButton
            vars[MixShopVars.selected] = index
            if (e.op.slot == 1) runClientScript(7146, 3746, -1, index, 0, 0, 0, -1, "", 1)
            if (e.op.slot in 2..5) buy(this, index, listOf(1, 5, 10, 50)[e.op.slot - 2])
        }
        onOmniShopButton(3746, MixShopComps.items) { e ->
            if (inv[e.comsub]?.id != e.obj?.id) return@onOmniShopButton
            vars[MixShopVars.side] = e.comsub
            if (e.op.slot in 2..5) refund(this, e.comsub, listOf(1, 5, 10, 50)[e.op.slot - 2])
        }
        for (i in 0..3) {
            onOmniShopButton(3746, MixShopComps.buy[i]) {
                buy(this, vars[MixShopVars.selected], listOf(1, 5, 10, 50)[i])
            }
            onOmniShopButton(3746, MixShopComps.sell[i]) {
                refund(this, vars[MixShopVars.side], listOf(1, 5, 10, 50)[i])
            }
        }
    }

    public fun open(access: ProtectedAccess): Boolean =
        with(access) {
            if (!game.allowed(player)) return false
            vars[MixBits.met] = 1
            ifOpenMainSidePair(MixShopUi.main, MixShopUi.side)
            vars[MixShopVars.last] = 3746
            vars[MixShopVars.selected] = -1
            vars[MixShopVars.side] = -1
            invTransmit(inv)
            runClientScript(7140, 3746, -1)
            runClientScript(7246, 3746)
            ifSetEvents(
                MixShopComps.list,
                0 until goods.size * 12,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
            )
            ifSetEvents(
                MixShopComps.items,
                inv.indices,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
            )
            for (button in MixShopComps.buy + MixShopComps.sell) ifSetEvents(
                button,
                -1..0,
                IfEvent.Op1,
            )
            true
        }

    public fun buy(access: ProtectedAccess, index: Int, request: Int): Int =
        with(access) {
            if (!game.allowed(player) || request !in 1..50) return 0
            val good = goods.getOrNull(index) ?: return 0
            if (player.statBase(stats.herblore) < good.level) {
                mes("You need a Herblore level of ${good.level} to buy this.")
                return 0
            }
            var bought = 0
            repeat(request) {
                if ((0..2).any { vars[MixVars.points[it]] < good.cost[it] }) {
                    mes("You need more resin to buy this reward.")
                    return bought
                }
                if (good.item == 29999) {
                    if (vars[MixBits.potionStorage] != 0) {
                        mes("You have already unlocked potion storage.")
                        return bought
                    }
                    vars[MixBits.potionStorage] = 1
                } else if (!player.invAdd(inv, types.getValue(good.item)).success) {
                    mes("Make room for the reward.")
                    return bought
                }
                for (i in 0..2) vars[MixVars.points[i]] -= good.cost[i]
                if (good.item == objs.alchemists_amulet_charged.id)
                    vars[varbits.alchemists_amulet_charges] =
                        (vars[varbits.alchemists_amulet_charges] + 50).coerceAtMost(5000)
                vars[MixShopVars.redraw] += 1
                bought++
            }
            bought
        }

    public fun refund(access: ProtectedAccess, slot: Int, request: Int): Int =
        with(access) {
            if (!game.allowed(player) || request !in 1..50) return 0
            val item = inv[slot] ?: return 0
            val canonical =
                when (item.id) {
                    29976 -> 29974
                    29980 -> 29978
                    29984 -> 29982
                    else -> item.id
                }
            val good = goods.firstOrNull { it.item == canonical && it.refund } ?: return 0
            var count = 0
            repeat(request) {
                val current = inv.indexOfFirst { it?.id == item.id }
                if (current < 0) return count
                if (!player.invDel(inv, item.id, 1, slot = current).success) return count
                for (i in 0..2) vars[MixVars.points[i]] =
                    (vars[MixVars.points[i]].toLong() + good.cost[i] * 4 / 10)
                        .coerceAtMost(Int.MAX_VALUE.toLong())
                        .toInt()
                vars[MixShopVars.redraw] += 1
                count++
            }
            count
        }
}
