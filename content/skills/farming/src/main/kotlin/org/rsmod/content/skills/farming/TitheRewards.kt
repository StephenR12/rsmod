package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsAlignment
import org.rsmod.content.skills.prayer.remains.PrayerGodWarsGod
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TitheFarmers : NpcReferences() {
    val farmer = find("hosidius_tithe_farmer")
    val bologa = find("hosidius_evilgrapegirl")
}

private enum class TitheReward(val label: String, val cost: Int) {
    Hat("Farmer's strawhat", 75),
    Shirt("Farmer's shirt", 150),
    Trousers("Farmer's trousers", 125),
    Boots("Farmer's boots", 50),
    Compost("Compost", 1),
    Supercompost("Supercompost", 5),
    Grape("Grape seed", 2),
    Blessing("20 Bologa's blessings", 1),
    Herbs("Herb box", 30),
    Seeds("Seed pack", 30),
    SeedBox("Seed box", 250),
    HerbSack("Herb sack", 250),
    WaterCan("Gricoller's can", 200),
    Refill("Refill Gricoller's can", 0),
    AutoWeed("Unlock auto-weed", 50),
    Toggle("Toggle auto-weed", 0),
}

internal object TitheGoods : ObjReferences() {
    val produce =
        listOf(
                "cert_bucket_compost",
                "cert_bucket_supercompost",
                "grape_seed",
                "grape_blessing",
                "nzone_herbbox",
                "seedbox",
                "seed_box",
                "slayer_herb_sack",
                "zeah_wateringcan",
            )
            .map { find(it) }
    val openSack = find("slayer_herb_sack_open")
}

public class TitheRewards
@Inject
constructor(private val alignment: PrayerGodWarsAlignment, private val objects: ObjTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TitheFarmers.farmer) {
            if (!player.members) return@onOpNpc1
            mes(
                "Take seeds from my table, then plant them in the farm. Water each stage and deposit the harvested fruit into a sack."
            )
            if (choice2("See Farmer Gricoller's rewards.", true, "Goodbye.", false)) shop()
        }
        onOpNpc3(TitheFarmers.farmer) { if (player.members) shop() }
        onOpNpc1(TitheFarmers.bologa) {
            if (!player.members) return@onOpNpc1
            if (vars[TitheRewardVars.blessing] != 0) {
                mes("You can buy my blessings in Farmer Gricoller's Rewards.")
                return@onOpNpc1
            }
            val others =
                PrayerGodWarsGod.entries
                    .filter { it != PrayerGodWarsGod.Zamorak }
                    .any { alignment.count(player, it) > 0 } ||
                    worn.any {
                        it != null && objects[it].paramMap?.getOrNull(params.guthixitem) == 1
                    }
            if (
                statBase(stats.prayer) < 50 ||
                    alignment.count(player, PrayerGodWarsGod.Zamorak) == 0 ||
                    others
            ) {
                mes(
                    "Show your devotion to Zamorak with 50 Prayer and only Zamorak-affiliated god equipment."
                )
                return@onOpNpc1
            }
            if (
                choice2("Pay 75,000 coins to unlock Bologa's blessings?", true, "No thanks.", false)
            ) {
                if (invDel(inv, objs.coins, 75000).failure) mes("You need 75,000 coins.")
                else {
                    vars[TitheRewardVars.blessing] = 1
                    mes("You may now buy Bologa's blessings with Tithe Farm points.")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.shop() {
        val stock = TitheReward.entries
        val i =
            menu(
                "Farmer Gricoller's Rewards (${vars[TitheBits.points]} points)",
                true,
                stock.map { "${it.label} (${it.cost})" },
            )
        val reward = stock.getOrNull(i) ?: return
        if (vars[TitheBits.points] < reward.cost) {
            mes("You need ${reward.cost} Tithe Farm points.")
            return
        }
        when (reward) {
            TitheReward.AutoWeed -> {
                if (vars[TitheRewardVars.autoWeed] != 0) {
                    mes("You have already unlocked auto-weed.")
                    return
                }
                vars[TitheRewardVars.autoWeed] = 1
                mes("Auto-weed is now active.")
            }
            TitheReward.Toggle -> {
                if (vars[TitheRewardVars.autoWeed] == 0) {
                    mes("Unlock auto-weed first.")
                    return
                }
                val active = vars[TitheRewardVars.autoWeed] != 1
                vars[TitheRewardVars.autoWeed] = if (active) 1 else 2
                mes(if (active) "Auto-weed is now active." else "Auto-weed is now inactive.")
            }
            TitheReward.Refill -> {
                if (objs.gricollers_can !in inv) {
                    mes("Bring your Gricoller's can to refill it.")
                    return
                }
                vars[TitheRewardVars.water] = 1000
                mes("You fill Gricoller's can.")
            }
            else -> {
                val product: ObjType =
                    if (i < 4) TitheItems.outfit[i][player.appearance.bodyType.coerceIn(0, 1)]
                    else TitheGoods.produce[i - 4]
                if (reward == TitheReward.Blessing && vars[TitheRewardVars.blessing] == 0) {
                    mes("Speak to Bologa upstairs to unlock this reward.")
                    return
                }
                if (reward == TitheReward.HerbSack && statBase(stats.herblore) < 58) {
                    mes("You need 58 Herblore to buy a herb sack.")
                    return
                }
                val unique =
                    reward in
                        listOf(TitheReward.HerbSack, TitheReward.SeedBox, TitheReward.WaterCan)
                if (
                    unique &&
                        (product in inv ||
                            product in bank ||
                            (reward == TitheReward.HerbSack &&
                                (TitheGoods.openSack in inv || TitheGoods.openSack in bank)))
                ) {
                    mes("You already have that reward.")
                    return
                }
                val pack = if (reward == TitheReward.Seeds) seedPack(3, true) else 0
                if (
                    invAdd(inv, product, if (reward == TitheReward.Blessing) 20 else 1, vars = pack)
                        .failure
                ) {
                    mes("You need more inventory space.")
                    return
                }
            }
        }
        vars[TitheBits.points] -= reward.cost
    }
}
