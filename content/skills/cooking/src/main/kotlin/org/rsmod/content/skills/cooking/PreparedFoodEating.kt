package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.timers
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.events.PlayerConsumableEvent
import org.rsmod.api.player.events.PlayerDeathEvent
import org.rsmod.api.player.isInCombat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.realm.Realm
import org.rsmod.api.realm.WorldFlag
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.stat.StatType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object PreparedFoodObjs : ObjReferences() {
    val partialCake = find("partial_cake")
    val cakeSlice = find("cake_slice")
    val partialChocolateCake = find("partial_chocolate_cake")
    val chocolateSlice = find("chocolate_slice")
    val halfPlainPizza = find("half_plain_pizza")
    val halfMeatPizza = find("half_meat_pizza")
    val halfAnchovyPizza = find("half_anchovie_pizza")
    val halfPineapplePizza = find("half_pineapple_pizza")
    val halfMeatPie = find("half_a_meat_pie")
    val halfRedberryPie = find("half_a_redberry_pie")
    val halfApplePie = find("half_an_apple_pie")
    val halfGardenPie = find("half_garden_pie")
    val halfFishPie = find("half_fish_pie")
    val halfBotanicalPie = find("half_botanical_pie")
    val halfMushroomPie = find("half_mushroom_pie")
    val halfAdmiralPie = find("half_admiral_pie")
    val halfDragonfruitPie = find("half_dragonfruit_pie")
    val halfWildPie = find("half_wild_pie")
    val halfSummerPie = find("half_summer_pie")
    val angler = find("anglerfish")
}

public data class FoodBite(
    val input: ObjType,
    val replacement: ObjType?,
    val healing: Int,
    val delay: Int = 3,
    val boosts: List<Pair<StatType, Int>> = emptyList(),
    val runEnergy: Int = 0,
)

/** Dated Food/Fast foods and individual pie pages: each bite retains its own eat delay. */
public object PreparedFoods {
    val pizzas: List<Pair<ObjType, ObjType>> by lazy {
        listOf(
            objs.plain_pizza to PreparedFoodObjs.halfPlainPizza,
            objs.meat_pizza to PreparedFoodObjs.halfMeatPizza,
            objs.anchovy_pizza to PreparedFoodObjs.halfAnchovyPizza,
            objs.pineapple_pizza to PreparedFoodObjs.halfPineapplePizza,
        )
    }
    val all: List<FoodBite> by lazy {
        buildList {
            add(FoodBite(objs.cake, PreparedFoodObjs.partialCake, 4, 2))
            add(FoodBite(PreparedFoodObjs.partialCake, PreparedFoodObjs.cakeSlice, 4, 2))
            add(FoodBite(PreparedFoodObjs.cakeSlice, null, 4))
            add(FoodBite(objs.chocolate_cake, PreparedFoodObjs.partialChocolateCake, 5, 2))
            add(
                FoodBite(
                    PreparedFoodObjs.partialChocolateCake,
                    PreparedFoodObjs.chocolateSlice,
                    5,
                    2,
                )
            )
            add(FoodBite(PreparedFoodObjs.chocolateSlice, null, 5))
            for ((index, pair) in pizzas.withIndex()) {
                add(FoodBite(pair.first, pair.second, listOf(7, 8, 9, 11)[index], 1))
                add(FoodBite(pair.second, null, listOf(7, 8, 9, 11)[index], 2))
            }
            fun pie(
                whole: ObjType,
                half: ObjType,
                heal: Int,
                secondDelay: Int = 1,
                boosts: List<Pair<StatType, Int>> = emptyList(),
                energy: Int = 0,
            ) {
                add(FoodBite(whole, half, heal, 1, boosts, energy))
                add(FoodBite(half, objs.pie_dish, heal, secondDelay, boosts, energy))
            }
            pie(objs.redberry_pie, PreparedFoodObjs.halfRedberryPie, 5, 2)
            pie(objs.meat_pie, PreparedFoodObjs.halfMeatPie, 6, 2)
            pie(objs.apple_pie, PreparedFoodObjs.halfApplePie, 7, 2)
            pie(
                objs.garden_pie,
                PreparedFoodObjs.halfGardenPie,
                6,
                boosts = listOf(stats.farming to 3),
            )
            pie(objs.fish_pie, PreparedFoodObjs.halfFishPie, 6, boosts = listOf(stats.fishing to 3))
            pie(
                objs.botanical_pie,
                PreparedFoodObjs.halfBotanicalPie,
                7,
                boosts = listOf(stats.herblore to 4),
            )
            pie(
                objs.mushroom_pie,
                PreparedFoodObjs.halfMushroomPie,
                8,
                boosts = listOf(stats.crafting to 4),
            )
            pie(
                objs.admiral_pie,
                PreparedFoodObjs.halfAdmiralPie,
                8,
                boosts = listOf(stats.fishing to 5),
            )
            pie(
                objs.dragonfruit_pie,
                PreparedFoodObjs.halfDragonfruitPie,
                10,
                boosts = listOf(stats.fletching to 4),
            )
            pie(
                objs.wild_pie,
                PreparedFoodObjs.halfWildPie,
                11,
                boosts = listOf(stats.ranged to 4, stats.slayer to 5),
            )
            pie(
                objs.summer_pie,
                PreparedFoodObjs.halfSummerPie,
                11,
                boosts = listOf(stats.agility to 5),
                energy = 1000,
            )
            add(FoodBite(objs.stew, objs.bowl_empty, 11))
            add(FoodBite(objs.curry, objs.bowl_empty, 19))
            add(FoodBite(objs.cooked_chompy, null, 10))
            add(FoodBite(OrdinaryFoodObjs.ugthanki, null, 3))
            add(FoodBite(objs.lava_eel, null, 11))
        }
    }
}

public object HunterMeatTimer : TimerReferences() {
    val heal = find("cooking_hunter_meat_heal")
}

public data class HunterMeat(
    val input: ObjType,
    val initial: Int,
    val secondary: Int,
    val energy: Int = 0,
    val curePoison: Boolean = false,
)

public object HunterMeats {
    val all: List<HunterMeat> by lazy {
        listOf(
            HunterMeat(objs.cooked_wild_kebbit, 4, 4),
            HunterMeat(objs.cooked_larupia, 6, 5),
            HunterMeat(objs.cooked_barb_tailed_kebbit, 7, 5),
            HunterMeat(objs.cooked_graahk, 8, 6),
            HunterMeat(objs.cooked_kyatt, 9, 8),
            HunterMeat(objs.cooked_pyre_fox, 11, 8),
            HunterMeat(objs.cooked_sunlight_antelope, 12, 9),
            HunterMeat(objs.cooked_dashing_kebbit, 13, 10, energy = 1000),
            HunterMeat(objs.cooked_moonlight_antelope, 14, 12, curePoison = true),
        )
    }
}

@Singleton
public class PreparedFoodEating
@Inject
constructor(
    private val food: FoodConsumptionRepository,
    private val types: ObjTypeList,
    private val realm: Realm,
) {
    private val pending = WeakHashMap<Player, HunterMeat>()

    public fun eat(access: ProtectedAccess, slot: Int, bite: FoodBite) =
        with(access) {
            if (!consume(this, slot, bite.input, bite.replacement)) return
            statHeal(stats.hitpoints, constant = bite.healing, percent = 0)
            for ((stat, boost) in bite.boosts) statBoost(stat, boost, 0)
            player.runEnergy = (player.runEnergy + bite.runEnergy).coerceAtMost(10000)
            food.consume(player, bite.delay, 3)
            publish(PlayerConsumableEvent.Consumed(player, bite.input, foodHealing = bite.healing))
        }

    public fun eatAngler(access: ProtectedAccess, slot: Int) =
        with(access) {
            if (!consume(this, slot, PreparedFoodObjs.angler, null)) return
            val base = player.statBase(stats.hitpoints)
            val heal = base / 10 + 2 * (base / 25) + 5 * (base / 93) + 2
            val pvp =
                player.vars[varbits.inside_wilderness] == 1 ||
                    player.vars[varbits.pvp_area_client] == 1 ||
                    realm.config.hasWorldFlag(WorldFlag.Pvp)
            if (pvp && player.isInCombat()) statHeal(stats.hitpoints, heal, 0)
            else statBoost(stats.hitpoints, heal, 0)
            food.consume(player, 3, 3)
            publish(
                PlayerConsumableEvent.Consumed(player, PreparedFoodObjs.angler, foodHealing = heal)
            )
        }

    public fun eatMeat(access: ProtectedAccess, slot: Int, meat: HunterMeat) =
        with(access) {
            if (!consume(this, slot, meat.input, null)) return
            statHeal(stats.hitpoints, meat.initial, 0)
            pending[player] = meat
            softTimer(HunterMeatTimer.heal, 7)
            food.consume(player, 3, 3)
            publish(
                PlayerConsumableEvent.Consumed(
                    player,
                    meat.input,
                    foodHealing = meat.initial + meat.secondary,
                )
            )
        }

    public fun secondaryHeal(player: Player) {
        val meat = pending.remove(player) ?: return
        if (player.stat(stats.hitpoints) <= 0) return
        player.statHeal(stats.hitpoints, meat.secondary, 0)
        player.runEnergy = (player.runEnergy + meat.energy).coerceAtMost(10000)
        if (meat.curePoison && player.vars[varps.poison] > 0) {
            val poison = player.vars[varps.poison]
            // Shared varp102: venom becomes poison at the same next damage; poison is removed.
            val cured = if (poison >= 1000000) ((poison - 999997) * 2).coerceAtMost(20) * 5 else 0
            VarPlayerIntMapSetter.set(player, varps.poison, cured)
            if (cured == 0) player.clearSoftTimer(timers.toxins)
        }
    }

    public fun clear(player: Player) {
        pending.remove(player)
        player.clearSoftTimer(HunterMeatTimer.heal)
    }

    public fun combinePizza(
        access: ProtectedAccess,
        first: Int,
        second: Int,
        half: ObjType,
        whole: ObjType,
    ): Unit =
        with(access) {
            if (first == second || first !in player.inv.indices || second !in player.inv.indices)
                return
            if (player.inv[first]?.id != half.id || player.inv[second]?.id != half.id) return
            if (types[half].members && !player.members) return
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete(inv, half.id, 1, slot = first)
                delete(inv, half.id, 1, slot = second)
                add(inv, whole.id, 1, slot = first)
            }
        }

    private fun consume(
        access: ProtectedAccess,
        slot: Int,
        input: ObjType,
        replacement: ObjType?,
    ): Boolean =
        with(access) {
            if (
                slot !in player.inv.indices ||
                    player.inv[slot]?.id != input.id ||
                    !food.isReady(player)
            )
                return false
            if (types[input].members && !player.members) {
                mes("You need to be on a members' world to eat this food.")
                return false
            }
            val result =
                player.invTransaction(player.inv) {
                    val inv = select(player.inv)
                    delete(inv, input.id, 1, slot = slot)
                    if (replacement != null) add(inv, replacement.id, 1, slot = slot)
                }
            if (!result.success) return false
            stopAction()
            anim(seqs.human_eat)
            mes("You eat the ${types[input].name.lowercase()}.")
            true
        }
}

public class PreparedFoodScript @Inject constructor(private val eating: PreparedFoodEating) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (bite in PreparedFoods.all) onOpHeld1(bite.input) { eating.eat(this, it.slot, bite) }
        onOpHeld1(PreparedFoodObjs.angler) { eating.eatAngler(this, it.slot) }
        for (meat in HunterMeats.all) onOpHeld1(meat.input) { eating.eatMeat(this, it.slot, meat) }
        for ((whole, half) in PreparedFoods.pizzas) onOpHeldU(half, half) {
            eating.combinePizza(this, it.firstSlot, it.secondSlot, half, whole)
        }
        onPlayerSoftTimer(HunterMeatTimer.heal) { eating.secondaryHeal(player) }
        onPlayerLogout { eating.clear(player) }
        onEvent<PlayerDeathEvent.Completed> { eating.clear(player) }
    }
}
