package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.*
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object DecantObjs : ObjReferences() {
    val olive = (1..4).map { find("oliveoil$it") }
    val jar = find("butterfly_jar")
    val serums =
        listOf("mort_serum", "mort_serum_perm").map { base -> (1..4).map { find(base + it) } }
    val hunter =
        listOf("glacialis", "snowy", "ruby", "warlock", "sunmoth", "moonmoth").map { name ->
            (1..2).map { find("hunter_mix_${name}_${it}dose") }
        }
    val relicym = (1..4).map { find("relicyms_balm$it") }
}

public data class DecantFluid(val doses: List<ObjType>, val empty: ObjType = objs.empty_vial) {
    public val maximum: Int
        get() = doses.size

    public fun amount(id: Int): Int = doses.indexOfFirst { it.id == id } + 1
}

public object DecantFluids {
    public val all: List<DecantFluid> by lazy {
        data class Dose(val obj: ObjType, val replacement: ObjType?, val number: Int)
        val standard =
            AntipoisonPotionDoses.all +
                HigherPoisonRemedyPotionDoses.all +
                SanfewSerumPotionDoses.all +
                AntifirePotionDoses.all +
                StatRestorePotionDoses.all +
                PrayerRegenerationPotionDoses.all +
                MenaphiteRemedyPotionDoses.all +
                StatBoostPotionDoses.all +
                DirectOnlyStatBoostPotionDoses.all +
                DirectBrewPotionDoses.all
        val raw =
            (standard.map { Dose(it.potion, it.replacement, it.dosesRemaining + 1) } +
                    GoadingPotionDoses.all.map {
                        Dose(it.potion, it.replacement, it.dosesRemaining + 1)
                    } +
                    SurgePotionDoses.all.map {
                        Dose(it.potion, it.replacement, it.dosesRemaining + 1)
                    })
                .distinctBy { it.obj.id }
        val byId = raw.associateBy { it.obj.id }
        fun root(dose: Dose): Int {
            var current = dose
            while (current.number > 1) current =
                byId.getValue(requireNotNull(current.replacement).id)
            return current.obj.id
        }
        raw.groupBy(::root).values.map { group ->
            DecantFluid(group.sortedBy { it.number }.map { it.obj })
        } +
            BarbarianMixRecipes.all.map { DecantFluid(listOf(it.oneDose, it.output)) } +
            listOf(
                DecantFluid(DecantObjs.olive),
                DecantFluid(
                    listOf(
                        objs.sacred_oil_1,
                        objs.sacred_oil_2,
                        objs.sacred_oil_3,
                        objs.sacred_oil_4,
                    )
                ),
                DecantFluid(DecantObjs.relicym),
            ) +
            DecantObjs.serums.map { DecantFluid(it) } +
            DecantObjs.hunter.map { DecantFluid(it, DecantObjs.jar) }
    }
    public val byId: Map<Int, DecantFluid> by lazy {
        all.flatMap { fluid -> fluid.doses.map { it.id to fluid } }.toMap()
    }
}

/** Conserves every dose and container; no production XP or chemistry/diary creation procs. */
@Singleton
public class PotionDecanting
@Inject
constructor(private val types: ObjTypeList, private val prices: MarketPrices) {
    public fun combine(access: ProtectedAccess, first: Int, second: Int): Boolean =
        with(access) {
            if (first == second || first !in inv.indices || second !in inv.indices) return false
            val a = inv[first] ?: return false
            val b = inv[second] ?: return false
            val family = DecantFluids.byId[a.id] ?: DecantFluids.byId[b.id] ?: return false
            val left = family.amount(a.id)
            val right = family.amount(b.id)
            if ((left == 0 && a.id != family.empty.id) || (right == 0 && b.id != family.empty.id))
                return false
            if (
                !player.members &&
                    ((left > 0 && types[a].members) || (right > 0 && types[b].members))
            )
                return false
            val newLeft: Int
            val newRight: Int
            if (left == 0 || right == 0) {
                val amount = left + right
                if (amount < 2) return false
                val larger = (amount + 1) / 2
                val smaller = amount / 2
                newLeft = if (left > 0) larger else smaller
                newRight = if (right > 0) larger else smaller
            } else {
                val transfer = minOf(left, family.maximum - right)
                if (transfer <= 0) return false
                newLeft = left - transfer
                newRight = right + transfer
            }
            fun obj(amount: Int): ObjType =
                if (amount == 0) family.empty else family.doses[amount - 1]
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, a.id, 1, slot = first)
                    delete(i, b.id, 1, slot = second)
                    add(i, obj(newLeft).id, 1, slot = first)
                    add(i, obj(newRight).id, 1, slot = second)
                }
            if (result.success) {
                stopAction()
                mes("You redistribute the doses between the containers.")
            }
            result.success
        }

    public fun empty(access: ProtectedAccess, slot: Int, fluid: DecantFluid): Boolean =
        with(access) {
            if (slot !in inv.indices) return false
            val item = inv[slot] ?: return false
            if (fluid.amount(item.id) == 0 || (!player.members && types[item].members)) return false
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, item.id, 1, slot = slot)
                    add(i, fluid.empty.id, 1, slot = slot)
                }
            if (result.success) {
                stopAction()
                mes("You empty the container.")
            }
            result.success
        }

    public suspend fun select(access: ProtectedAccess): Unit =
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to use this service.")
                return
            }
            val amount =
                choice4(
                    "One-dose containers.",
                    1,
                    "Two-dose containers.",
                    2,
                    "Three-dose containers.",
                    3,
                    "Four-dose containers.",
                    4,
                )
            decant(this, amount)
        }

    private data class Input(
        val slot: Int,
        val item: InvObj,
        val family: DecantFluid,
        val doses: Int,
        val noted: Boolean,
    )

    private data class Output(val type: ObjType, val count: Int)

    private data class EmptyUse(
        val slot: Int,
        val item: InvObj,
        val count: Int,
        val noted: Boolean,
    )

    public fun decant(access: ProtectedAccess, requested: Int): Boolean =
        with(access) {
            if (!player.members || requested !in 1..4) return false
            val inputs =
                inv.indices.mapNotNull { slot ->
                    val item = inv[slot] ?: return@mapNotNull null
                    val type = types[item]
                    val unnoted = if (type.certtemplate > 0) type.certlink else item.id
                    val fluid = DecantFluids.byId[unnoted] ?: return@mapNotNull null
                    Input(slot, item, fluid, fluid.amount(unnoted), unnoted != item.id)
                }
            if (inputs.isEmpty()) {
                mes("You have no suitable potions or oils to decant.")
                return false
            }
            val outputs = mutableListOf<Output>()
            val containerIn = mutableMapOf<ObjType, Long>()
            val containerOut = mutableMapOf<ObjType, Long>()
            for ((fluid, batch) in inputs.groupBy { it.family }) {
                val total = batch.sumOf { it.item.count.toLong() * it.doses }
                val dose = minOf(requested, fluid.maximum)
                val full = total / dose
                val remainder = (total % dose).toInt()
                if (full > Int.MAX_VALUE) return false
                if (full > 0) outputs += Output(fluid.doses[dose - 1], full.toInt())
                if (remainder > 0) outputs += Output(fluid.doses[remainder - 1], 1)
                containerIn[fluid.empty] =
                    (containerIn[fluid.empty] ?: 0) + batch.sumOf { it.item.count.toLong() }
                containerOut[fluid.empty] =
                    (containerOut[fluid.empty] ?: 0) + full + (if (remainder > 0) 1 else 0)
            }
            val emptyUse = mutableListOf<EmptyUse>()
            var cost = 0L
            for ((container, filled) in containerOut) {
                val original = containerIn.getValue(container)
                var needed = (filled - original).coerceAtLeast(0)
                val note = types[container].certlink
                for (slot in inv.indices) {
                    val item = inv[slot] ?: continue
                    if (item.id != container.id && item.id != note) continue
                    val used = minOf(item.count.toLong(), needed).toInt()
                    if (used > 0) {
                        emptyUse += EmptyUse(slot, item, used, item.id != container.id)
                        needed -= used
                    }
                }
                if (needed > 0) {
                    val unit =
                        (prices[types[container]] ?: types[container].cost)
                            .coerceAtLeast(0)
                            .toLong() + 2
                    if (needed > Int.MAX_VALUE / unit || cost + needed * unit > Int.MAX_VALUE)
                        return false
                    cost += needed * unit
                }
                val surplus = original - filled
                if (surplus > Int.MAX_VALUE) return false
                if (surplus > 0) outputs += Output(container, surplus.toInt())
            }
            if (cost > Int.MAX_VALUE) {
                mes("That decanting request costs too much.")
                return false
            }
            fun attempt(noted: Boolean): Boolean =
                player
                    .invTransaction(inv) {
                        val i = select(inv)
                        for (input in inputs) delete(
                            i,
                            input.item.id,
                            input.item.count,
                            slot = input.slot,
                        )
                        for (empty in emptyUse) delete(
                            i,
                            empty.item.id,
                            empty.count,
                            slot = empty.slot,
                        )
                        if (cost > 0) delete(i, objs.coins.id, cost.toInt())
                        for (output in outputs) {
                            val type =
                                if (noted && types[output.type].certlink > 0)
                                    types.getValue(types[output.type].certlink)
                                else output.type
                            add(i, type.id, output.count)
                        }
                    }
                    .success
            val noted = inputs.any { it.noted } || emptyUse.any { it.noted }
            val success = attempt(noted) || (!noted && attempt(true))
            if (success) mes("Your potions and oils have been decanted.")
            else
                mes(
                    if (cost > 0)
                        "You need $cost coins for the missing containers and room for the results."
                    else "You need more inventory space to decant those items."
                )
            success
        }
}

public class PotionDecantingScript
@Inject
constructor(private val decanting: PotionDecanting, private val types: ObjTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (fluid in DecantFluids.all) {
            for ((index, first) in fluid.doses.withIndex()) {
                for (second in fluid.doses.drop(index)) onOpHeldU(first, second) {
                    decanting.combine(this, it.firstSlot, it.secondSlot)
                }
                onOpHeldU(first, fluid.empty) {
                    decanting.combine(this, it.firstSlot, it.secondSlot)
                }
                when (types[first].iop.indexOf("Empty")) {
                    0 -> onOpHeld1(first) { decanting.empty(this, it.slot, fluid) }
                    1 -> onOpHeld2(first) { decanting.empty(this, it.slot, fluid) }
                    2 -> onOpHeld3(first) { decanting.empty(this, it.slot, fluid) }
                    3 -> onOpHeld4(first) { decanting.empty(this, it.slot, fluid) }
                }
            }
        }
    }
}
