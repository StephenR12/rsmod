package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

public class FoodPreparation @Inject constructor() {
    public suspend fun selectDough(access: ProtectedAccess, flourSlot: Int, waterSlot: Int) {
        with(access) {
            val water =
                FoodPreparationRecipes.waterContainers.firstOrNull {
                    player.inv[waterSlot]?.id == it.input.id
                } ?: return
            if (player.inv[flourSlot]?.id != objs.pot_of_flour.id) {
                return
            }
            clearCookingQueues()
            val input =
                skillMulti(
                    title = DOUGH_MENU_TITLE,
                    choices = FoodPreparationRecipes.doughOutputs,
                    maxQuantity = 1,
                    initialQuantity = 1,
                    actionMode = MAKE_ACTION_MODE,
                )
            val index = SKILL_MULTI_BUTTONS.indexOfFirst { it.isType(input.component) }
            val output = FoodPreparationRecipes.doughOutputs.getOrNull(index) ?: return
            val recipe = FoodPreparationRecipes.doughRecipe(water.input, output) ?: return
            start(this, flourSlot, waterSlot, recipe)
        }
    }

    public suspend fun selectBatch(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        recipe: FoodPreparationRecipe,
    ) {
        require(recipe.batch) { "Only batch food-preparation recipes open a quantity menu." }
        with(access) {
            if (!canPrepare(firstSlot, secondSlot, recipe, sendFailure = true)) {
                return
            }
            val maximum = maximumActions(recipe)
            val input =
                skillMulti(
                    title = BATCH_MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, firstSlot, secondSlot, recipe, input.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        firstSlot: Int,
        secondSlot: Int,
        recipe: FoodPreparationRecipe,
        quantity: Int = 1,
    ) {
        require(quantity >= 1) { "Food-preparation quantity must be positive." }
        with(access) {
            clearCookingQueues()
            if (!canPrepare(firstSlot, secondSlot, recipe, sendFailure = true)) {
                return
            }
            val request = FoodPreparationRequest(recipe.key, firstSlot, secondSlot, quantity)
            if (recipe.ticks == 0) {
                process(this, request, compensateQueueHandler = false)
            } else {
                weakQueue(cooking_queues.foodPreparation, recipe.ticks, request)
            }
        }
    }

    public fun process(access: ProtectedAccess, request: FoodPreparationRequest) {
        process(access, request, compensateQueueHandler = true)
    }

    private fun process(
        access: ProtectedAccess,
        request: FoodPreparationRequest,
        compensateQueueHandler: Boolean,
    ) {
        val recipe = FoodPreparationRecipes.byKey[request.key] ?: return
        with(access) {
            val ingredientSlots =
                resolveIngredientSlots(request.firstSlot, request.secondSlot, recipe) ?: return
            if (!canPrepare(request.firstSlot, request.secondSlot, recipe, sendFailure = false)) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for ((ingredient, slot) in ingredientSlots) {
                        delete(inventory, ingredient.obj.id, 1, slot = slot)
                    }
                    for ((ingredient, slot) in ingredientSlots) {
                        ingredient.returned?.let { add(inventory, it.id, 1, slot = slot) }
                    }
                    val outputSlot = if (recipe.first.returned == null) request.firstSlot else null
                    add(inventory, recipe.output.id, 1, slot = outputSlot)
                }
            check(result.success) {
                "Food-preparation transaction failed after validation: $recipe"
            }
            if (recipe.xp != 0.0) {
                statAdvance(stats.cooking, recipe.xp)
            }
            recipe.message?.let(::mes)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !recipe.batch) {
                return
            }
            val nextSlots = resolveIngredientSlots(recipe) ?: return
            val firstSlot = nextSlots[0].second
            val secondSlot = nextSlots[1].second
            if (!canPrepare(firstSlot, secondSlot, recipe, sendFailure = false)) {
                return
            }
            weakQueue(
                cooking_queues.foodPreparation,
                recipe.repeatTicks +
                    if (compensateQueueHandler) RESCHEDULE_TICK_COMPENSATION else 0,
                request.copy(firstSlot = firstSlot, secondSlot = secondSlot, remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.canPrepare(
        firstSlot: Int,
        secondSlot: Int,
        recipe: FoodPreparationRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (firstSlot == secondSlot || player.inv[firstSlot]?.id != recipe.first.obj.id) {
            return false
        }
        if (player.inv[secondSlot]?.id != recipe.second.obj.id) {
            return false
        }
        if (recipe.members && !player.members) {
            return reject("You need to be on a members' world to use this item.")
        }
        if (player.stat(stats.cooking) < recipe.level) {
            return reject("You need a Cooking level of ${recipe.level} to make this.")
        }
        if (recipe.tool != null && !player.inv.contains(recipe.tool)) {
            return false
        }
        if (resolveIngredientSlots(firstSlot, secondSlot, recipe) == null) {
            return false
        }
        if (player.inv.freeSpace() < recipe.requiredFreeSpace) {
            return reject("You don't have enough inventory space.")
        }
        return true
    }

    private fun ProtectedAccess.resolveIngredientSlots(
        firstSlot: Int,
        secondSlot: Int,
        recipe: FoodPreparationRecipe,
    ): List<Pair<FoodPreparationIngredient, Int>>? {
        val result = mutableListOf(recipe.first to firstSlot, recipe.second to secondSlot)
        val used = mutableSetOf(firstSlot, secondSlot)
        for (ingredient in recipe.additional) {
            val slot =
                player.inv.indices.firstOrNull {
                    it !in used && player.inv[it]?.id == ingredient.obj.id
                } ?: return null
            used += slot
            result += ingredient to slot
        }
        return result
    }

    private fun ProtectedAccess.resolveIngredientSlots(
        recipe: FoodPreparationRecipe
    ): List<Pair<FoodPreparationIngredient, Int>>? {
        val result = mutableListOf<Pair<FoodPreparationIngredient, Int>>()
        val used = mutableSetOf<Int>()
        for (ingredient in listOf(recipe.first, recipe.second) + recipe.additional) {
            val slot =
                player.inv.indices.firstOrNull {
                    it !in used && player.inv[it]?.id == ingredient.obj.id
                } ?: return null
            used += slot
            result += ingredient to slot
        }
        return result
    }

    private fun ProtectedAccess.maximumActions(recipe: FoodPreparationRecipe): Int =
        (listOf(recipe.first, recipe.second) + recipe.additional)
            .groupingBy { it.obj.id }
            .eachCount()
            .minOf { (obj, count) ->
                player.inv.filterNotNull { it.id == obj }.sumOf { it.count } / count
            }

    public companion object {
        public const val DOUGH_MENU_TITLE: String = "What would you like to make?"
        public const val BATCH_MENU_TITLE: String = "How many would you like to make?"
        public const val MAKE_ACTION_MODE: Int = 0
        private const val RESCHEDULE_TICK_COMPENSATION: Int = 1

        public val SKILL_MULTI_BUTTONS: List<ComponentType> =
            listOf(
                components.skillmulti_a,
                components.skillmulti_b,
                components.skillmulti_c,
                components.skillmulti_d,
            )
    }
}

public data class FoodPreparationIngredient(val obj: ObjType, val returned: ObjType? = null)

public data class FoodPreparationRecipe(
    val first: FoodPreparationIngredient,
    val second: FoodPreparationIngredient,
    val additional: List<FoodPreparationIngredient> = emptyList(),
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val members: Boolean,
    val ticks: Int,
    val repeatTicks: Int = ticks,
    val batch: Boolean = false,
    val tool: ObjType? = null,
    val message: String? = null,
) {
    public val key: FoodPreparationRecipeKey =
        FoodPreparationRecipeKey(first.obj.id, second.obj.id, output.id)

    public val requiredFreeSpace: Int =
        (1 + listOf(first, second).plus(additional).count { it.returned != null } -
                (2 + additional.size))
            .coerceAtLeast(0)
}

public data class FoodPreparationRecipeKey(val first: Int, val second: Int, val output: Int)

public data class FoodPreparationRequest(
    val key: FoodPreparationRecipeKey,
    val firstSlot: Int,
    val secondSlot: Int,
    val remaining: Int = 1,
)

public data class WaterContainer(val input: ObjType, val empty: ObjType)

public object FoodPreparationRecipes {
    public val waterContainers: List<WaterContainer> =
        listOf(
            WaterContainer(objs.bucket_water, objs.bucket_empty),
            WaterContainer(objs.bowl_water, objs.bowl_empty),
            WaterContainer(objs.jug_water, objs.jug_empty),
        )

    public val doughOutputs: List<ObjType> =
        listOf(objs.bread_dough, objs.pastry_dough, objs.pizza_base, objs.pitta_dough)

    private val doughDefinitions =
        listOf(
            DoughDefinition(objs.bread_dough, level = 1, members = false),
            DoughDefinition(objs.pastry_dough, level = 1, members = false),
            DoughDefinition(objs.pizza_base, level = 35, members = false),
            DoughDefinition(objs.pitta_dough, level = 58, members = true),
        )

    public val doughRecipes: List<FoodPreparationRecipe> =
        waterContainers.flatMap { water ->
            doughDefinitions.map { dough ->
                FoodPreparationRecipe(
                    first = FoodPreparationIngredient(objs.pot_of_flour, objs.pot_empty),
                    second = FoodPreparationIngredient(water.input, water.empty),
                    output = dough.output,
                    level = dough.level,
                    xp = 0.0,
                    members = dough.members,
                    ticks = 1,
                )
            }
        }

    private val cakeIngredients =
        listOf(
            FoodPreparationIngredient(objs.egg),
            FoodPreparationIngredient(objs.pot_of_flour, objs.pot_empty),
            FoodPreparationIngredient(objs.bucket_of_milk, objs.bucket_empty),
        )

    public val cakeRecipes: List<FoodPreparationRecipe> =
        cakeIngredients.map { selected ->
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.cake_tin),
                second = selected,
                additional = cakeIngredients.filterNot { it == selected },
                output = objs.uncooked_cake,
                level = 40,
                xp = 0.0,
                members = false,
                ticks = 2,
                message = "You mix the milk, flour and egg together to make a raw cake mix.",
            )
        }

    public val directRecipes: List<FoodPreparationRecipe> =
        listOf(
            recipe(objs.pizza_base, objs.tomato, objs.incomplete_pizza, 35, 0.0, false),
            recipe(objs.incomplete_pizza, objs.cheese, objs.uncooked_pizza, 35, 0.0, false),
            recipe(objs.cake, objs.chocolate_bar, objs.chocolate_cake, 50, 30.0, false),
            recipe(objs.cake, objs.chocolate_dust, objs.chocolate_cake, 50, 30.0, false),
            recipe(objs.plain_pizza, objs.cooked_meat, objs.meat_pizza, 45, 26.0, false),
            recipe(objs.plain_pizza, objs.cooked_chicken, objs.meat_pizza, 45, 26.0, false),
            recipe(objs.plain_pizza, objs.anchovies, objs.anchovy_pizza, 55, 39.0, false),
            recipe(objs.plain_pizza, objs.pineapple_chunks, objs.pineapple_pizza, 65, 45.0, true),
            recipe(objs.plain_pizza, objs.pineapple_ring, objs.pineapple_pizza, 65, 45.0, true),
        )

    public val stewAndCurryRecipes: List<FoodPreparationRecipe> =
        listOf(
            recipe(
                objs.bowl_water,
                objs.potato,
                objs.incomplete_stew_potato,
                level = 25,
                xp = 0.0,
                members = false,
                message = "You cut up the potato and put it into the bowl.",
            ),
            recipe(
                objs.bowl_water,
                objs.cooked_meat,
                objs.incomplete_stew_meat,
                level = 25,
                xp = 0.0,
                members = false,
                message = "You cut up the meat and put it into the bowl.",
            ),
            recipe(
                objs.bowl_water,
                objs.cooked_chicken,
                objs.incomplete_stew_meat,
                level = 25,
                xp = 0.0,
                members = false,
                message = "You cut up the chicken and put it into the bowl.",
            ),
            recipe(
                objs.incomplete_stew_potato,
                objs.cooked_meat,
                objs.uncooked_stew,
                level = 25,
                xp = 0.0,
                members = false,
                message = "You cut up the meat and put it into the stew.",
            ),
            recipe(
                objs.incomplete_stew_potato,
                objs.cooked_chicken,
                objs.uncooked_stew,
                level = 25,
                xp = 0.0,
                members = false,
                message = "You cut up the chicken and put it into the stew.",
            ),
            recipe(
                objs.incomplete_stew_meat,
                objs.potato,
                objs.uncooked_stew,
                level = 25,
                xp = 0.0,
                members = false,
                message = "You cut up the potato and put it into the bowl.",
            ),
            recipe(
                objs.uncooked_stew,
                objs.spice,
                objs.uncooked_curry,
                level = 60,
                xp = 0.0,
                members = true,
            ),
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.uncooked_stew),
                second = FoodPreparationIngredient(objs.curry_leaf),
                additional =
                    listOf(
                        FoodPreparationIngredient(objs.curry_leaf),
                        FoodPreparationIngredient(objs.curry_leaf),
                    ),
                output = objs.uncooked_curry,
                level = 60,
                xp = 0.0,
                members = true,
                ticks = 2,
            ),
        )

    public val pieRecipes: List<FoodPreparationRecipe> =
        listOf(
            batchRecipe(objs.pastry_dough, objs.pie_dish, objs.pie_shell, 1, false),
            batchRecipe(objs.pie_shell, objs.redberries, objs.uncooked_redberry_pie, 10, false),
            batchRecipe(objs.pie_shell, objs.cooked_meat, objs.uncooked_meat_pie, 20, false),
            batchRecipe(objs.pie_shell, objs.cooked_chicken, objs.uncooked_meat_pie, 20, false),
            batchRecipe(objs.pie_shell, objs.cooking_apple, objs.uncooked_apple_pie, 30, false),
            batchRecipe(
                objs.pie_shell,
                objs.bucket_of_compost,
                objs.part_mud_pie_compost,
                29,
                true,
                returnedSecond = objs.bucket_empty,
            ),
            batchRecipe(
                objs.part_mud_pie_compost,
                objs.bucket_water,
                objs.part_mud_pie_water,
                29,
                true,
                returnedSecond = objs.bucket_empty,
            ),
            batchRecipe(objs.part_mud_pie_water, objs.clay, objs.uncooked_mud_pie, 29, true),
            batchRecipe(objs.pie_shell, objs.tomato, objs.part_garden_pie_tomato, 34, true),
            batchRecipe(
                objs.part_garden_pie_tomato,
                objs.onion,
                objs.part_garden_pie_onion,
                34,
                true,
            ),
            batchRecipe(
                objs.part_garden_pie_onion,
                objs.cabbage,
                objs.uncooked_garden_pie,
                34,
                true,
            ),
            batchRecipe(objs.pie_shell, objs.trout, objs.part_fish_pie_trout, 47, true),
            batchRecipe(objs.part_fish_pie_trout, objs.cod, objs.part_fish_pie_cod, 47, true),
            batchRecipe(objs.part_fish_pie_cod, objs.potato, objs.uncooked_fish_pie, 47, true),
            batchRecipe(
                objs.pie_shell,
                objs.golovanova_fruit_top,
                objs.uncooked_botanical_pie,
                52,
                true,
            ),
            batchRecipe(objs.pie_shell, objs.sulliuscep_cap, objs.uncooked_mushroom_pie, 60, true),
            batchRecipe(
                objs.pie_shell,
                objs.salmon,
                objs.part_admiral_pie_salmon,
                70,
                true,
                ticks = 1,
            ),
            batchRecipe(
                objs.part_admiral_pie_salmon,
                objs.tuna,
                objs.part_admiral_pie_tuna,
                70,
                true,
            ),
            batchRecipe(
                objs.part_admiral_pie_tuna,
                objs.potato,
                objs.uncooked_admiral_pie,
                70,
                true,
            ),
            batchRecipe(objs.pie_shell, objs.dragonfruit, objs.uncooked_dragonfruit_pie, 73, true),
            batchRecipe(objs.pie_shell, objs.raw_bear_meat, objs.part_wild_pie_bear, 85, true),
            batchRecipe(
                objs.part_wild_pie_bear,
                objs.raw_chompy,
                objs.part_wild_pie_chompy,
                85,
                true,
            ),
            batchRecipe(
                objs.part_wild_pie_chompy,
                objs.raw_rabbit,
                objs.uncooked_wild_pie,
                85,
                true,
            ),
            batchRecipe(objs.pie_shell, objs.strawberry, objs.part_summer_pie_strawberry, 95, true),
            batchRecipe(
                objs.part_summer_pie_strawberry,
                objs.watermelon,
                objs.part_summer_pie_watermelon,
                95,
                true,
            ),
            batchRecipe(
                objs.part_summer_pie_watermelon,
                objs.cooking_apple,
                objs.uncooked_summer_pie,
                95,
                true,
            ),
        )

    public val potatoRecipes: List<FoodPreparationRecipe> =
        listOf(
            recipe(objs.baked_potato, objs.pat_of_butter, objs.potato_with_butter, 39, 40.0, true),
            recipe(
                objs.baked_potato,
                objs.pat_of_not_garlic_butter,
                objs.potato_with_butter,
                39,
                40.0,
                true,
            ),
            recipe(objs.potato_with_butter, objs.cheese, objs.potato_with_cheese, 47, 40.0, true),
            recipe(
                objs.potato_with_butter,
                objs.chilli_con_carne,
                objs.chilli_potato,
                41,
                15.0,
                true,
                returnedSecond = objs.bowl_empty,
            ),
            recipe(objs.potato_with_butter, objs.egg_and_tomato, objs.egg_potato, 51, 45.0, true),
            recipe(
                objs.potato_with_butter,
                objs.mushroom_and_onion,
                objs.mushroom_potato,
                64,
                55.0,
                true,
            ),
            recipe(
                objs.potato_with_butter,
                objs.tuna_and_corn,
                objs.tuna_potato,
                68,
                10.0,
                true,
                returnedSecond = objs.bowl_empty,
            ),
            recipe(
                objs.bowl_empty,
                objs.garlic,
                objs.chopped_garlic,
                1,
                0.0,
                true,
                tool = objs.knife,
            ),
            recipe(objs.chopped_garlic, objs.gnome_spice, objs.spicy_sauce, 9, 25.0, true),
            recipe(
                objs.bowl_empty,
                objs.cooked_meat,
                objs.minced_meat,
                1,
                0.0,
                true,
                tool = objs.knife,
            ),
            recipe(
                objs.spicy_sauce,
                objs.cooked_meat,
                objs.chilli_con_carne,
                11,
                0.0,
                true,
                ticks = 1,
                tool = objs.knife,
            ),
            recipe(
                objs.spicy_sauce,
                objs.minced_meat,
                objs.chilli_con_carne,
                11,
                0.0,
                true,
                ticks = 1,
                returnedSecond = objs.bowl_empty,
            ),
            recipe(objs.bowl_empty, objs.egg, objs.uncooked_egg, 13, 0.0, true),
            recipe(objs.scrambled_egg, objs.tomato, objs.egg_and_tomato, 23, 0.0, true),
            recipe(
                objs.bowl_empty,
                objs.bittercap_mushroom,
                objs.sliced_mushrooms,
                1,
                0.0,
                true,
                tool = objs.knife,
            ),
            recipe(
                objs.bowl_empty,
                objs.onion,
                objs.chopped_onion,
                1,
                0.0,
                true,
                tool = objs.knife,
            ),
            recipe(objs.fried_mushrooms, objs.fried_onions, objs.mushroom_and_onion, 57, 0.0, true),
            recipe(objs.bowl_empty, objs.tuna, objs.chopped_tuna, 1, 0.0, true, tool = objs.knife),
            recipe(objs.bowl_empty, objs.cooked_sweetcorn, objs.bowl_of_sweetcorn, 1, 0.0, true),
            recipe(
                objs.bowl_of_sweetcorn,
                objs.tuna,
                objs.tuna_and_corn,
                67,
                0.0,
                true,
                tool = objs.knife,
            ),
            recipe(
                objs.bowl_of_sweetcorn,
                objs.chopped_tuna,
                objs.tuna_and_corn,
                67,
                0.0,
                true,
                returnedSecond = objs.bowl_empty,
            ),
            recipe(objs.chopped_tuna, objs.cooked_sweetcorn, objs.tuna_and_corn, 67, 0.0, true),
        )

    public val hotDrinkRecipes: List<FoodPreparationRecipe> =
        listOf(
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.bowl_water),
                second = FoodPreparationIngredient(objs.nettles),
                output = objs.bowl_nettlewater,
                level = 20,
                xp = 0.0,
                members = true,
                ticks = 2,
                repeatTicks = 2,
                batch = true,
            ),
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.empty_cup),
                second = FoodPreparationIngredient(objs.bowl_nettletea, objs.bowl_empty),
                output = objs.cup_of_nettle_tea,
                level = 1,
                xp = 0.0,
                members = true,
                ticks = 0,
            ),
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.empty_cup),
                second = FoodPreparationIngredient(objs.bowl_nettletea_milky, objs.bowl_empty),
                output = objs.cup_of_milky_nettle_tea,
                level = 1,
                xp = 0.0,
                members = true,
                ticks = 0,
            ),
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.bowl_nettletea),
                second = FoodPreparationIngredient(objs.bucket_of_milk, objs.bucket_empty),
                output = objs.bowl_nettletea_milky,
                level = 1,
                xp = 0.0,
                members = true,
                ticks = 0,
            ),
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.cup_of_nettle_tea),
                second = FoodPreparationIngredient(objs.bucket_of_milk, objs.bucket_empty),
                output = objs.cup_of_milky_nettle_tea,
                level = 1,
                xp = 0.0,
                members = true,
                ticks = 0,
            ),
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.empty_cup),
                second = FoodPreparationIngredient(objs.bowl_hot_water, objs.bowl_empty),
                output = objs.cup_of_hot_water,
                level = 1,
                xp = 0.0,
                members = true,
                ticks = 0,
                repeatTicks = 1,
                batch = true,
            ),
        )

    public val oomlieRecipes: List<FoodPreparationRecipe> =
        listOf(
            FoodPreparationRecipe(
                first = FoodPreparationIngredient(objs.raw_oomlie),
                second = FoodPreparationIngredient(objs.palm_leaf),
                output = objs.wrapped_oomlie,
                level = 50,
                xp = 10.0,
                members = true,
                ticks = 0,
            )
        )

    public val heldRecipes: List<FoodPreparationRecipe> =
        cakeRecipes +
            directRecipes +
            stewAndCurryRecipes +
            pieRecipes +
            potatoRecipes +
            hotDrinkRecipes +
            oomlieRecipes
    public val all: List<FoodPreparationRecipe> = doughRecipes + heldRecipes
    public val byKey: Map<FoodPreparationRecipeKey, FoodPreparationRecipe> =
        all.associateBy { it.key }

    public fun doughRecipe(water: ObjType, output: ObjType): FoodPreparationRecipe? =
        doughRecipes.firstOrNull { it.second.obj == water && it.output == output }

    private fun recipe(
        first: ObjType,
        second: ObjType,
        output: ObjType,
        level: Int,
        xp: Double,
        members: Boolean,
        ticks: Int = 2,
        returnedSecond: ObjType? = null,
        tool: ObjType? = null,
        message: String? = null,
    ): FoodPreparationRecipe =
        FoodPreparationRecipe(
            first = FoodPreparationIngredient(first),
            second = FoodPreparationIngredient(second, returnedSecond),
            output = output,
            level = level,
            xp = xp,
            members = members,
            ticks = ticks,
            tool = tool,
            message = message,
        )

    private fun batchRecipe(
        first: ObjType,
        second: ObjType,
        output: ObjType,
        level: Int,
        members: Boolean,
        returnedSecond: ObjType? = null,
        ticks: Int = 2,
    ): FoodPreparationRecipe =
        FoodPreparationRecipe(
            first = FoodPreparationIngredient(first),
            second = FoodPreparationIngredient(second, returnedSecond),
            output = output,
            level = level,
            xp = 0.0,
            members = members,
            ticks = ticks,
            repeatTicks = 2,
            batch = true,
        )

    private data class DoughDefinition(val output: ObjType, val level: Int, val members: Boolean)
}
