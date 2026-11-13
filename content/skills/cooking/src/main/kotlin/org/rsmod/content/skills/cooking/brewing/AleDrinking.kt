package org.rsmod.content.skills.cooking.brewing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.cooking.FoodConsumptionRepository
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object AleSeqs : SeqReferences() {
    val glass = find("human_drink_rum")
    val keg = find("farming_keg_drink")
}

/** One dose per drink, including native partially filled calquat kegs. */
public class AleDrinking
@Inject
constructor(private val food: FoodConsumptionRepository, private val types: ObjTypeList) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in AleRecipes.all) for (mature in listOf(false, true)) {
            val glass = if (mature) recipe.mature else recipe.normal
            onOpHeld1(glass) { drink(this, recipe, mature, 0, it.slot) }
            for ((index, keg) in
                (if (mature) BreweryObjs.matureKegs[recipe.index]
                    else BreweryObjs.kegs[recipe.index])
                    .withIndex()) {
                onOpHeld1(keg) { drink(this, recipe, mature, index + 1, it.slot) }
                onOpHeldU(BreweryObjs.glass, keg) {
                    pour(this, recipe, mature, index + 1, it.firstSlot, it.secondSlot)
                }
            }
        }
    }

    private fun input(recipe: AleRecipe, mature: Boolean, doses: Int): ObjType =
        if (doses == 0) {
            if (mature) recipe.mature else recipe.normal
        } else
            (if (mature) BreweryObjs.matureKegs[recipe.index] else BreweryObjs.kegs[recipe.index])[
                doses - 1]

    private fun remainder(recipe: AleRecipe, mature: Boolean, doses: Int): ObjType =
        when (doses) {
            0 -> BreweryObjs.glass
            1 -> BreweryObjs.keg
            else -> input(recipe, mature, doses - 1)
        }

    public fun drink(
        access: ProtectedAccess,
        recipe: AleRecipe,
        mature: Boolean,
        doses: Int,
        slot: Int,
    ): Boolean =
        with(access) {
            if (doses !in 0..4 || slot !in inv.indices) return false
            val product = input(recipe, mature, doses)
            if (
                inv[slot]?.id != product.id ||
                    !food.isReady(player) ||
                    types[product].members && !player.members
            )
                return false
            val result =
                player.invTransaction(inv) {
                    val i = select(inv)
                    delete(i, product.id, 1, slot = slot)
                    add(i, remainder(recipe, mature, doses).id, 1, slot = slot)
                }
            if (!result.success) return false
            stopAction()
            anim(if (doses == 0) AleSeqs.glass else AleSeqs.keg)
            val strength = if (mature) 2 else 1
            fun boost(stat: StatType, constant: Int = strength, percent: Int = 0) {
                statBoost(stat, constant, percent)
            }
            fun drain(stat: StatType, constant: Int, percent: Int) {
                statSub(stat, constant + player.stat(stat) * percent / 100, 0)
            }
            fun drainCombat(constant: Int, percent: Int) {
                for (stat in listOf(stats.attack, stats.strength, stats.defence)) drain(
                    stat,
                    constant,
                    percent,
                )
            }
            when (recipe.index) {
                0 -> {
                    boost(stats.mining)
                    boost(stats.smithing)
                    drainCombat(strength + 1, 4)
                }
                1 -> {
                    boost(stats.strength, strength + 1)
                    drain(stats.attack, strength + 1, 5)
                }
                2 -> {
                    boost(stats.herblore)
                    drainCombat(strength + 1, 4)
                }
                3 -> {
                    boost(stats.magic, strength + 1, 2)
                    drainCombat(strength, 5)
                }
                4 -> {
                    boost(stats.strength, strength + 1)
                    drain(stats.attack, 2, 5)
                }
                5 -> Unit
                6 -> {
                    boost(stats.woodcutting)
                    drain(stats.attack, strength + 1, 2)
                    drain(stats.strength, strength + 1, 2)
                }
                7 -> {
                    boost(stats.cooking, strength, 5)
                    drain(stats.attack, strength + 1, 5)
                    drain(stats.strength, strength + 1, 5)
                }
                8 -> {
                    boost(stats.slayer, strength * 2)
                    drain(stats.attack, strength + 1, 2)
                    drain(stats.strength, strength + 1, 2)
                }
                9 -> {
                    boost(stats.farming)
                    drain(stats.attack, strength + 1, 2)
                    drain(stats.strength, strength + 1, 2)
                }
            }
            val heal =
                if (recipe.index == 5) {
                    if (mature) 6 else 4
                } else strength
            statHeal(stats.hitpoints, heal, 0)
            food.consume(player, 3, 3)
            publish(PlayerConsumableEvent.Consumed(player, product, foodHealing = heal))
            mes("You drink some ${types[product].name}.")
            true
        }

    public fun pour(
        access: ProtectedAccess,
        recipe: AleRecipe,
        mature: Boolean,
        doses: Int,
        glassSlot: Int,
        kegSlot: Int,
    ): Boolean =
        with(access) {
            if (
                !player.members ||
                    doses !in 1..4 ||
                    glassSlot == kegSlot ||
                    glassSlot !in inv.indices ||
                    kegSlot !in inv.indices
            )
                return false
            val keg = input(recipe, mature, doses)
            if (inv[glassSlot]?.id != BreweryObjs.glass.id || inv[kegSlot]?.id != keg.id)
                return false
            player
                .invTransaction(inv) {
                    val i = select(inv)
                    delete(i, BreweryObjs.glass.id, 1, slot = glassSlot)
                    delete(i, keg.id, 1, slot = kegSlot)
                    add(i, input(recipe, mature, 0).id, 1, slot = glassSlot)
                    add(i, remainder(recipe, mature, doses).id, 1, slot = kegSlot)
                }
                .success
        }
}
