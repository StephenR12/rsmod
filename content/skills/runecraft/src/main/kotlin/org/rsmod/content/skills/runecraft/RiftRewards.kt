package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import kotlin.math.pow
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.operation.StandardShopOperations
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RiftCurrency : CurrencyReferences() {
    val pearls = find("rift_pearls")
}

internal object RiftShopInvs : InvReferences() {
    val shop =
        listOf("none", "law", "death", "law_death", "blood", "law_blood", "death_blood", "all")
            .map { find("gotr_reward_shop_$it") }
}

internal object RiftLoot : ObjReferences() {
    val ring = find("ring_of_elements")
    val eye = find("guardians_eye")
    val catalytic = find("catalytic_talisman")
    val dyes = listOf("red", "green", "blue").map { find("abyssal_${it}_dye") }
    val ashes = find("abyssal_ashes")
    val amulet = find("gotr_amulet_of_the_eye")
    val locket = find("gotr_tarnished_locket")
    val bag = find("gotr_lost_bag")
}

internal object RiftRewardBits : VarBitReferences() {
    val bag = find("gotr_bag_obtained")
    val locket = find("gotr_locket_obtained")
}

internal object RiftRewardVars : VarpReferences() {
    val pulls = find("gotr_search_count")
}

internal val pearlPrices
    get() =
        buildMap<ObjType, Int> {
            put(RunecraftObjs.raimentsHat, 400)
            put(RunecraftObjs.raimentsTorso, 350)
            put(RunecraftObjs.raimentsLegsBase, 350)
            put(RunecraftObjs.raimentsBoots, 250)
            put(RiftLoot.ring, 400)
            put(RiftLoot.eye, 3000)
            for (t in
                listOf(
                    RuneTemple.Air,
                    RuneTemple.Water,
                    RuneTemple.Earth,
                    RuneTemple.Fire,
                    RuneTemple.Mind,
                    RuneTemple.Body,
                    RuneTemple.Chaos,
                    RuneTemple.Cosmic,
                    RuneTemple.Nature,
                    RuneTemple.Law,
                    RuneTemple.Death,
                    RuneTemple.Blood,
                )) {
                put(
                    TempleObjects.talismans.getValue(t),
                    when (t) {
                        RuneTemple.Chaos,
                        RuneTemple.Cosmic -> 30
                        RuneTemple.Nature,
                        RuneTemple.Law -> 50
                        RuneTemple.Death -> 80
                        RuneTemple.Blood -> 100
                        else -> 10
                    },
                )
            }
        }

internal object RiftShopStock : InvEditor() {
    init {
        for ((mask, shop) in RiftShopInvs.shop.withIndex()) edit(shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            for (item in pearlPrices.keys) {
                val bit =
                    when (item) {
                        TempleObjects.talismans.getValue(RuneTemple.Law) -> 1
                        TempleObjects.talismans.getValue(RuneTemple.Death) -> 2
                        TempleObjects.talismans.getValue(RuneTemple.Blood) -> 4
                        else -> 0
                    }
                if (mask and bit == bit) stock += stock(item, count = 100, restockCycles = 1)
            }
        }
    }
}

public class RiftRewards
@Inject
internal constructor(
    private val pets: org.rsmod.content.skills.pets.SkillPets,
    private val clues: org.rsmod.api.clue.ClueInventory,
    private val clueSteps: org.rsmod.api.clue.Cache233ClueSteps,
    private val shops: Shops,
    private val operations: ShopOperationMap,
    private val types: ObjTypeList,
    private val drops: org.rsmod.api.repo.obj.ObjRepository,
    private val pouches: EssencePouchStorage,
) : PluginScript(), StandardShopOperations {
    private val prices by lazy { pearlPrices.mapKeys { it.key.id } }

    override fun ScriptContext.startup() {
        operations.register(RiftCurrency.pearls, this@RiftRewards)
        onOpLoc1(RiftLocs.reward) { search(1, false) }
        onOpLoc2(RiftLocs.reward) { report() }
        onOpLoc3(RiftLocs.reward) { search(5, false) }
        onOpLocU(RiftLocs.reward, RiftItems.pearls) {
            if (choice2("Sacrifice 25 abyssal pearls for a search.", true, "Keep them.", false))
                search(1, true)
        }
        onOpLocU(RiftLocs.reward, objs.needle) {
            if (
                vars[RiftBits.needle] != 0 &&
                    invTotal(inv, RiftItems.needle) == 0 &&
                    invTotal(bank, RiftItems.needle) == 0 &&
                    invTotal(inv, objs.needle) > 0
            ) {
                val tx =
                    player.invTransaction(inv) {
                        val b = select(inv)
                        delete(b, objs.needle.id, 1)
                        add(b, RiftItems.needle.id, 1)
                    }
                if (tx.success) mes("Your needle has become an abyssal needle.")
            }
        }
        onOpNpc3(RiftNpcs.felix) { shop() }
        for (dye in RiftLoot.dyes) onOpNpcU(RiftNpcs.felix, dye) { exchangeDye(dye) }
        onOpNpc1(RiftNpcs.felix) {
            if (!player.members) return@onOpNpc1
            val missing = mutableListOf<ObjType>()
            if (
                vars[RiftRewardBits.locket] != 0 &&
                    vars[RiftHeirlooms.amulet] == 0 &&
                    RiftLoot.locket !in inv &&
                    RiftLoot.locket !in bank
            )
                missing += RiftLoot.locket
            if (
                vars[RiftRewardBits.bag] != 0 &&
                    ColouredRiftItems.bags.none { it in inv || it in bank || it in worn }
            )
                missing += ColouredRiftItems.bags[vars[RiftHeirlooms.bagColour].coerceIn(0, 3)]
            val choices =
                mutableListOf(
                    "Trade",
                    "Buy an abyssal lantern (1,500 pearls)",
                    "Sell a spare lantern or dye",
                    "Exchange a dye colour",
                )
            choices += missing.map { "Reclaim ${ocName(it)}" }
            choices += "Leave"
            when (
                val choice = menu(hotkeys = false, title = "Temple Supplies", choices = choices)
            ) {
                0 -> shop()
                1 -> trade(RiftItems.pearls, 1500, RiftItems.lantern, 1)
                2 -> {
                    val spares = (listOf(RiftItems.lantern) + RiftLoot.dyes).filter { it in inv }
                    val item = chooseItem(spares) ?: return@onOpNpc1
                    trade(item, 1, RiftItems.pearls, if (item == RiftItems.lantern) 100 else 50)
                }
                3 -> chooseItem(RiftLoot.dyes.filter { it in inv })?.let { exchangeDye(it) }
                else ->
                    missing.getOrNull(choice - 4)?.let { item ->
                        if (
                            item !in inv &&
                                item !in bank &&
                                item !in worn &&
                                !invAdd(inv, item).success
                        )
                            mes("Make room in your inventory for ${ocName(item)}.")
                    }
            }
        }
        onOpHeld1(RiftItems.pouch) { openPouch() }
    }

    private suspend fun ProtectedAccess.chooseItem(items: List<ObjType>): ObjType? =
        when (items.size) {
            0 -> {
                mes("You don't have an item to exchange.")
                null
            }
            1 -> items.single()
            else ->
                items.getOrNull(
                    menu(
                        hotkeys = false,
                        title = "Choose an item",
                        choices = items.map { ocName(it) },
                    )
                )
        }

    private suspend fun ProtectedAccess.exchangeDye(old: ObjType) {
        if (!player.members || old !in inv) return
        val alternatives = RiftLoot.dyes.filter { it != old }
        val choice =
            menu(
                "Exchange dye",
                false,
                alternatives.map { ocName(it) } + listOf("50 abyssal pearls", "Cancel"),
            )
        if (choice == alternatives.size) trade(old, 1, RiftItems.pearls, 50)
        else alternatives.getOrNull(choice)?.let { trade(old, 1, it, 1) }
    }

    private fun ProtectedAccess.shop() {
        if (!player.members) return
        val mask =
            (if (vars[AbyssVars.trollStronghold] >= 50) 1 else 0) +
                (if (eligible(RuneTemple.Death)) 2 else 0) +
                (if (eligible(RuneTemple.Blood)) 4 else 0)
        shops.open(
            player,
            "Temple Supplies",
            RiftShopInvs.shop[mask],
            1.0,
            1.0,
            0.0,
            RiftCurrency.pearls,
        )
    }

    private fun ProtectedAccess.eligible(t: RuneTemple): Boolean =
        t.recipe.quest?.let { vars[it.varbit] >= it.completionValue } ?: true

    private fun ProtectedAccess.trade(from: ObjType, n: Int, into: ObjType, m: Int) {
        val result =
            player.invTransaction(inv) {
                val b = select(inv)
                delete(b, from.id, n)
                add(b, into.id, m)
            }
        mes(
            if (result.success) "You receive ${ocName(into)}."
            else "You need the payment and room in your inventory."
        )
    }

    override fun examineShopValue(player: Player, shop: Shop, slot: Int) {
        val item = shop.inv[slot] ?: return
        player.mes("${types[item].name} costs ${prices[item.id]} abyssal pearls.")
    }

    override fun shopBuy(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) {
        val item = shop.inv[slot] ?: return
        val cost = prices[item.id] ?: return
        if (!player.members || request <= 0) return
        val n = minOf(request, item.count, sideInv.count(types[RiftItems.pearls]) / cost)
        if (n <= 0) {
            player.mes("You do not have enough abyssal pearls.")
            return
        }
        val result =
            player.invTransaction(sideInv) {
                val b = select(sideInv)
                delete(b, RiftItems.pearls.id, n * cost)
                add(b, item.id, n)
            }
        if (!result.success) player.mes("You need more room in your inventory.")
    }

    override fun examineInvValue(player: Player, sideInv: Inventory, shop: Shop, slot: Int) {
        player.mes("Felix doesn't buy that through his shop.")
    }

    override fun invSell(player: Player, sideInv: Inventory, shop: Shop, slot: Int, request: Int) =
        examineInvValue(player, sideInv, shop, slot)

    override fun examineDesc(player: Player, inv: Inventory, shop: Shop, slot: Int) {
        inv[slot]?.let { player.mes(types[it].desc) }
    }

    private fun ProtectedAccess.report() {
        mes(
            "You have ${vars[RiftBits.points[0]]} elemental points and ${vars[RiftBits.points[1]]} catalytic points. You have searched the guardian ${vars[RiftRewardVars.pulls]} times."
        )
    }

    private data class Prize(val id: Int, val n: Int = 1)

    private suspend fun ProtectedAccess.search(request: Int, sacrifice: Boolean) {
        if (!player.members) return
        repeat(request) {
            if (
                sacrifice && invTotal(inv, RiftItems.pearls) < 25 ||
                    !sacrifice && RiftBits.points.any { vars[it] < 1 }
            ) {
                report()
                return
            }
            val rewards = mutableListOf<Prize>()
            if (vars[RiftBits.diary] == 0 && random.of(20) == 0)
                rewards += Prize(RiftItems.diary.id)
            if (random.of(200) == 0) rewards += Prize(RiftLoot.catalytic.id)
            if (vars[RiftBits.needle] == 0 && random.of(300) == 0)
                rewards += Prize(RiftItems.needle.id)
            if (random.of(700) == 0) rewards += Prize(RiftItems.lantern.id)
            if (random.of(400) == 0) rewards += Prize(random.pick(RiftLoot.dyes).id)
            if (random.of(4000) == 0) rewards += Prize(26901)
            val pouch = nextPouch()
            val roll = random.of(if (pouch != null) 140 else 125)
            if (roll >= 125) rewards += Prize(pouch!!.normal.id) else rewards += normal(roll)
            if (sacrifice) {
                if (!invDel(inv, RiftItems.pearls, 25).success) return
            } else {
                for (key in RiftBits.points) vars[key] -= 1
            }
            deliver(rewards)
            if (rewards.any { it.id == RiftItems.diary.id }) vars[RiftBits.diary] = 1
            if (rewards.any { it.id == RiftItems.needle.id }) vars[RiftBits.needle] = 1
            if (roll >= 125 && pouch != null)
                pouches.setState(
                    player,
                    pouch,
                    (EssencePouchDefinitions.all.indexOf(pouch) + 1) * 2,
                    EssenceType.Pure,
                )
            vars[RiftRewardVars.pulls] =
                (vars[RiftRewardVars.pulls].toLong() + 1)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            mes(
                "The guardian gives you " +
                    rewards.joinToString(", ") { "${it.n} ${types.getValue(it.id).name}" } +
                    "."
            )
            delay(3)
        }
    }

    private fun ProtectedAccess.deliver(prizes: List<Prize>) {
        var overflow = false
        for (prize in prizes) {
            if (prize.id == 26901) pets.award(player, prize.id)
            else if (!invAddOrDrop(drops, types.getValue(prize.id), prize.n)) overflow = true
        }
        if (overflow) mes("Some of your rewards fell to the ground.")
    }

    private fun ProtectedAccess.nextPouch(): EssencePouchDefinition? {
        fun owned(p: EssencePouchDefinition) = p.variants.any { it in inv || it in bank }
        if (owned(EssencePouchDefinitions.all.last())) return null
        return EssencePouchDefinitions.all.dropLast(1).firstOrNull { !owned(it) }
    }

    private fun ProtectedAccess.normal(roll: Int): Prize {
        if (roll < 84) {
            val table =
                listOf(
                    RuneTemple.Air to 400..500,
                    RuneTemple.Water to 400..500,
                    RuneTemple.Earth to 400..500,
                    RuneTemple.Fire to 400..500,
                    RuneTemple.Mind to 250..400,
                    RuneTemple.Body to 80..150,
                    RuneTemple.Chaos to 61..150,
                    RuneTemple.Cosmic to 20..30,
                    RuneTemple.Nature to 28..150,
                    RuneTemple.Law to 5..120,
                    RuneTemple.Death to 5..120,
                    RuneTemple.Blood to 5..120,
                )
            val (t, qty) = table[if (roll < 24) roll / 4 else 6 + (roll - 24) / 10]
            val count = random.of(qty)
            val scale =
                (stat(stats.runecrafting).toDouble() / t.recipe.level).coerceAtMost(1.0).pow(4)
            return Prize(
                t.recipe.output.id,
                (count * scale).toInt().coerceAtLeast((count / 10).coerceAtLeast(1)),
            )
        }
        if (roll < 100) {
            val t = random.of(35)
            if (t == 34) return Prize(TempleObjects.elementalTalisman.id)
            val temple =
                when (t) {
                    0 ->
                        if (vars[AbyssVars.trollStronghold] >= 50) RuneTemple.Law
                        else RuneTemple.Air
                    1 -> if (eligible(RuneTemple.Death)) RuneTemple.Death else RuneTemple.Air
                    in 2..13 ->
                        listOf(RuneTemple.Air, RuneTemple.Water, RuneTemple.Earth, RuneTemple.Fire)[
                            (t - 2) / 3]
                    else ->
                        listOf(
                            RuneTemple.Mind,
                            RuneTemple.Body,
                            RuneTemple.Chaos,
                            RuneTemple.Cosmic,
                            RuneTemple.Nature,
                        )[(t - 14) / 4]
                }
            val obj = types[TempleObjects.talismans.getValue(temple)]
            return Prize(obj.certlink.takeIf { it > 0 } ?: obj.id)
        }
        return when (roll) {
            in 100..117 -> Prize(RiftItems.pearls.id, random.of(14..16))
            in 118..122 -> Prize(RiftItems.pouch.id)
            123 -> Prize(RiftLoot.ashes.id)
            else -> Prize(objs.needle.id)
        }
    }

    private fun ProtectedAccess.openPouch() {
        if (!player.members) return
        val rewards = mutableListOf<Prize>()
        val roll = random.of(45)
        if (roll < 40) {
            val ids =
                listOf(
                    objs.astral_rune,
                    objs.blood_rune,
                    objs.chaos_rune,
                    objs.cosmic_rune,
                    objs.death_rune,
                    objs.law_rune,
                    objs.nature_rune,
                    objs.soul_rune,
                )
            val counts =
                listOf(
                    150..200,
                    150..200,
                    200..300,
                    200..300,
                    150..200,
                    200..250,
                    200..300,
                    150..200,
                )
            rewards += Prize(ids[roll / 5].id, random.of(counts[roll / 5]))
        } else
            rewards +=
                when (roll) {
                    40 -> Prize(2366)
                    41 -> Prize(1249)
                    42 -> Prize(989)
                    43 -> Prize(1149)
                    else -> Prize(types[RunecraftObjs.pureEssence].certlink, random.of(500..1000))
                }
        if (random.of(200) == 0) rewards += Prize(RunecraftObjs.bloodEssenceInactive.id)
        if (random.of(15) == 0) rewards += Prize(2528)
        if (vars[RiftRewardBits.locket] == 0 && random.of(40) == 0)
            rewards += Prize(RiftLoot.locket.id)
        if (vars[RiftRewardBits.bag] == 0 && random.of(60) == 0) rewards += Prize(RiftLoot.bag.id)
        val tier = org.rsmod.api.clue.ClueTier.Hard
        if (random.of(10) == 0) {
            val state = clues.state(player, tier)
            val clue =
                clues.reward(state, tier) {
                    clueSteps.select(tier, random.of(clueSteps.steps(tier).size))
                }
            clue.obj?.let { rewards += Prize(it.id) }
        }
        if (!invDel(inv, RiftItems.pouch).success) return
        deliver(rewards)
        if (rewards.any { it.id == RiftLoot.locket.id }) vars[RiftRewardBits.locket] = 1
        if (rewards.any { it.id == RiftLoot.bag.id }) vars[RiftRewardBits.bag] = 1
    }
}
