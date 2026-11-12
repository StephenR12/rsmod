package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType

public class DairyChurning
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val objRepo: ObjRepository,
    private val worldRepo: WorldRepository,
) {
    public suspend fun select(
        access: ProtectedAccess,
        churn: BoundLocInfo,
        requestedInput: ObjType? = null,
        requestedSlot: Int? = null,
    ) {
        with(access) {
            if (requestedInput != null && requestedInput !in DairyChurningRecipes.inputs) {
                return
            }
            if (requestedSlot != null && player.inv[requestedSlot]?.id != requestedInput?.id) {
                return
            }
            val input =
                requestedInput
                    ?: DairyChurningRecipes.inputs.firstOrNull { player.inv.contains(it) }
                    ?: run {
                        mes(NO_INGREDIENTS_MESSAGE)
                        return
                    }
            if (!player.inv.contains(input)) {
                return
            }
            clearCookingQueues()
            val recipes = DairyChurningRecipes.forInput.getValue(input.id)
            val maximum = player.inv.count(objTypes[input]).coerceAtLeast(1)
            val selection =
                skillMulti(
                    title = MENU_TITLE,
                    choices = recipes.map(DairyChurningRecipe::output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            val index = SKILL_MULTI_BUTTONS.indexOfFirst { it.isType(selection.component) }
            val recipe = recipes.getOrNull(index) ?: return
            val slot =
                requestedSlot?.takeIf { player.inv[it]?.id == input.id }
                    ?: player.inv.indexOfFirst { it?.id == input.id }
            if (slot < 0) {
                return
            }
            start(this, churn, slot, recipe, selection.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        churn: BoundLocInfo,
        inputSlot: Int,
        recipe: DairyChurningRecipe,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Dairy-churning quantity must be positive." }
        with(access) {
            clearCookingQueues()
            if (!canChurn(churn, inputSlot, recipe, sendFailure = true)) {
                return
            }
            beginAction(churn, recipe)
            weakQueue(
                cooking_queues.dairyChurning,
                recipe.ticks,
                DairyChurningRequest(churn, recipe.input, recipe.output, inputSlot, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: DairyChurningRequest) {
        val recipe = DairyChurningRecipes.byKey[request.input.id to request.output.id] ?: return
        with(access) {
            if (!canChurn(request.churn, request.inputSlot, recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            churn(recipe, request.inputSlot)
            val remaining = request.remaining - 1
            if (remaining <= 0) {
                return
            }
            val nextSlot = player.inv.indexOfFirst { it?.id == recipe.input.id }
            if (nextSlot < 0 || !canChurn(request.churn, nextSlot, recipe, sendFailure = false)) {
                return
            }
            beginAction(request.churn, recipe)
            weakQueue(
                cooking_queues.dairyChurning,
                recipe.ticks + QUEUE_HANDLER_TICK_COMPENSATION,
                request.copy(inputSlot = nextSlot, remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.churn(recipe: DairyChurningRecipe, inputSlot: Int) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.input.id, 1, slot = inputSlot)
                add(inventory, recipe.output.id, 1, slot = inputSlot)
            }
        check(result.success) { "Dairy-churning transaction failed after validation: $recipe" }
        if (recipe.returnsBucket) {
            invAddOrDrop(objRepo, objs.bucket_empty)
        }
        statAdvance(stats.cooking, recipe.xp)
    }

    private fun ProtectedAccess.canChurn(
        churn: BoundLocInfo,
        inputSlot: Int,
        recipe: DairyChurningRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!isWithinDistance(churn, distance = 1)) {
            return false
        }
        if (!player.members) {
            return reject(MEMBERS_MESSAGE)
        }
        if (player.stat(stats.cooking) < recipe.level) {
            return reject("You need a Cooking level of ${recipe.level} to churn this.")
        }
        if (player.inv[inputSlot]?.id != recipe.input.id) {
            return false
        }
        return true
    }

    private fun ProtectedAccess.beginAction(churn: BoundLocInfo, recipe: DairyChurningRecipe) {
        anim(recipe.playerAnimation)
        val metal = churn.id == cooking_locs.dairyChurnMetal.id
        spotanimMap(worldRepo, recipe.churnAnimation(metal), churn.coords)
    }

    public companion object {
        public const val MENU_TITLE: String = "What would you like to make in the churn?"
        public const val NO_INGREDIENTS_MESSAGE: String =
            "You need a bucket of milk, pot of cream or pat of butter to use the churn."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use the churn."
        public const val MAKE_ACTION_MODE: Int = 0
        private const val QUEUE_HANDLER_TICK_COMPENSATION: Int = 1

        public val SKILL_MULTI_BUTTONS: List<ComponentType> =
            listOf(components.skillmulti_a, components.skillmulti_b, components.skillmulti_c)
    }
}

public data class DairyChurningRecipe(
    val input: ObjType,
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val ticks: Int,
    val playerAnimation: SeqType,
    val regularChurnAnimation: SpotanimType,
    val metalChurnAnimation: SpotanimType,
    val returnsBucket: Boolean = input == objs.bucket_of_milk,
) {
    public fun churnAnimation(metal: Boolean): SpotanimType =
        if (metal) metalChurnAnimation else regularChurnAnimation
}

public object DairyChurningRecipes {
    private val shortPlayerAnimation = seqs.player_churns_milk_short
    private val mediumPlayerAnimation = seqs.player_churns_milk_medium
    private val longPlayerAnimation = seqs.player_churns_milk_long

    public val all: List<DairyChurningRecipe> =
        listOf(
            short(objs.bucket_of_milk, objs.pot_of_cream, 21, 18.0),
            medium(objs.bucket_of_milk, objs.pat_of_butter, 38, 40.5),
            long(objs.bucket_of_milk, objs.cheese, 48, 64.0),
            short(objs.pot_of_cream, objs.pat_of_butter, 38, 22.5),
            medium(objs.pot_of_cream, objs.cheese, 48, 46.0),
            short(objs.pat_of_butter, objs.cheese, 48, 23.5),
            short(objs.pat_of_not_garlic_butter, objs.cheese, 48, 23.5),
        )

    public val inputs: List<ObjType> =
        listOf(
            objs.bucket_of_milk,
            objs.pot_of_cream,
            objs.pat_of_butter,
            objs.pat_of_not_garlic_butter,
        )

    public val forInput: Map<Int, List<DairyChurningRecipe>> = all.groupBy { it.input.id }
    public val byKey: Map<Pair<Int, Int>, DairyChurningRecipe> =
        all.associateBy { it.input.id to it.output.id }

    private fun short(
        input: ObjType,
        output: ObjType,
        level: Int,
        xp: Double,
    ): DairyChurningRecipe =
        DairyChurningRecipe(
            input,
            output,
            level,
            xp,
            ticks = 10,
            playerAnimation = shortPlayerAnimation,
            regularChurnAnimation = spotanims.dairy_churn_short,
            metalChurnAnimation = spotanims.dairy_churn_metal_short,
        )

    private fun medium(
        input: ObjType,
        output: ObjType,
        level: Int,
        xp: Double,
    ): DairyChurningRecipe =
        DairyChurningRecipe(
            input,
            output,
            level,
            xp,
            ticks = 19,
            playerAnimation = mediumPlayerAnimation,
            regularChurnAnimation = spotanims.dairy_churn_medium,
            metalChurnAnimation = spotanims.dairy_churn_metal_medium,
        )

    private fun long(input: ObjType, output: ObjType, level: Int, xp: Double): DairyChurningRecipe =
        DairyChurningRecipe(
            input,
            output,
            level,
            xp,
            ticks = 26,
            playerAnimation = longPlayerAnimation,
            regularChurnAnimation = spotanims.dairy_churn_long,
            metalChurnAnimation = spotanims.dairy_churn_metal_long,
        )
}

public data class DairyChurningRequest(
    val churn: BoundLocInfo,
    val input: ObjType,
    val output: ObjType,
    val inputSlot: Int,
    val remaining: Int,
)
