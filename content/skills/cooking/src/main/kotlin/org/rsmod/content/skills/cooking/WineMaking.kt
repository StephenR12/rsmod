package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import kotlin.math.min
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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class WineMaking @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(
        access: ProtectedAccess,
        grapeSlot: Int,
        waterSlot: Int,
        recipe: WineRecipe,
    ) {
        with(access) {
            if (!canMix(grapeSlot, waterSlot, recipe, sendFailure = true)) {
                return
            }
            val maximum = maximumActions(this, recipe)
            val selection =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.unfermented),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            start(this, grapeSlot, waterSlot, recipe, selection.subcomponent)
        }
    }

    public fun start(
        access: ProtectedAccess,
        grapeSlot: Int,
        waterSlot: Int,
        recipe: WineRecipe,
        quantity: Int,
    ) {
        require(quantity >= 1) { "Wine-making quantity must be positive." }
        with(access) {
            clearCookingQueues()
            if (!canMix(grapeSlot, waterSlot, recipe, sendFailure = true)) {
                return
            }
            beginAction()
            weakQueue(
                cooking_queues.wineMaking,
                FIRST_RESULT_TICKS,
                WineMakingRequest(recipe.grapes, grapeSlot, waterSlot, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: WineMakingRequest) {
        val recipe = WineRecipes.byGrapes[request.grapes.id] ?: return
        with(access) {
            if (!canMix(request.grapeSlot, request.waterSlot, recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            mix(recipe, request.grapeSlot, request.waterSlot)
            val remaining = request.remaining - 1
            if (remaining <= 0) {
                return
            }
            val grapeSlot = player.inv.indexOfFirst { it?.id == recipe.grapes.id }
            val waterSlot = player.inv.indexOfFirst { it?.id == objs.jug_water.id }
            if (grapeSlot < 0 || waterSlot < 0) {
                return
            }
            if (!canMix(grapeSlot, waterSlot, recipe, sendFailure = false)) {
                return
            }
            beginAction()
            weakQueue(
                cooking_queues.wineMaking,
                REPEAT_RESULT_TICKS + QUEUE_HANDLER_TICK_COMPENSATION,
                request.copy(grapeSlot = grapeSlot, waterSlot = waterSlot, remaining = remaining),
            )
        }
    }

    public fun ferment(access: ProtectedAccess) {
        with(access) {
            val storedWines =
                WineRecipes.all.mapNotNull { recipe ->
                    val inventorySlots =
                        player.inv.indices.filter { player.inv[it]?.id == recipe.unfermented.id }
                    val bankSlots = bank.indices.filter { bank[it]?.id == recipe.unfermented.id }
                    val inventoryCount = inventorySlots.sumOf { player.inv.getValue(it).count }
                    val bankCount = bankSlots.sumOf { bank.getValue(it).count }
                    val total = inventoryCount + bankCount
                    if (total == 0) {
                        null
                    } else {
                        WineFermentationOutcome(
                            recipe = recipe,
                            inventorySlots = inventorySlots,
                            bankSlots = bankSlots,
                            inventorySuccesses = 0,
                            bankSuccesses = 0,
                            bankCount = bankCount,
                        )
                    }
                }
            if (storedWines.isEmpty()) {
                clearTimer(cooking_timers.wineFermentation)
                return
            }
            if (!bankCanHoldEveryOutcome(storedWines)) {
                timer(cooking_timers.wineFermentation, BANK_CAPACITY_RETRY_TICKS)
                return
            }
            val outcomes =
                storedWines.map { stored ->
                    val inventoryCount =
                        stored.inventorySlots.sumOf { player.inv.getValue(it).count }
                    val total = inventoryCount + stored.bankCount
                    val successes = rollSuccessCount(stored.recipe, total)
                    val inventorySuccesses = min(successes, inventoryCount)
                    stored.copy(
                        inventorySuccesses = inventorySuccesses,
                        bankSuccesses = successes - inventorySuccesses,
                    )
                }
            val result =
                player.invTransaction(player.inv, bank) {
                    val inventory = select(player.inv)
                    val bankInventory = select(bank)
                    for (outcome in outcomes) {
                        var inventorySuccesses = outcome.inventorySuccesses
                        for (slot in outcome.inventorySlots) {
                            val count = player.inv.getValue(slot).count
                            delete(inventory, outcome.recipe.unfermented.id, count, slot = slot)
                            val successes = min(inventorySuccesses, count)
                            val failures = count - successes
                            if (successes > 0) {
                                add(inventory, outcome.recipe.fermented.id, successes, slot = slot)
                            }
                            if (failures > 0) {
                                add(
                                    inventory,
                                    objs.jug_of_bad_wine.id,
                                    failures,
                                    slot = if (successes == 0) slot else null,
                                )
                            }
                            inventorySuccesses -= successes
                        }
                        if (outcome.bankCount > 0) {
                            val firstSlot = outcome.bankSlots.first()
                            delete(
                                bankInventory,
                                outcome.recipe.unfermented.id,
                                outcome.bankCount,
                                slot = firstSlot,
                            )
                            if (outcome.bankSuccesses > 0) {
                                add(
                                    bankInventory,
                                    outcome.recipe.fermented.id,
                                    outcome.bankSuccesses,
                                    slot = firstSlot,
                                )
                            }
                            val failures = outcome.bankCount - outcome.bankSuccesses
                            if (failures > 0) {
                                add(
                                    bankInventory,
                                    objs.jug_of_bad_wine.id,
                                    failures,
                                    slot = if (outcome.bankSuccesses == 0) firstSlot else null,
                                )
                            }
                        }
                    }
                }
            check(result.success) { "Wine fermentation transaction failed after validation." }
            clearTimer(cooking_timers.wineFermentation)
            val successfulWines = outcomes.sumOf { it.inventorySuccesses + it.bankSuccesses }
            if (successfulWines > 0) {
                statAdvance(stats.cooking, successfulWines * XP_PER_SUCCESS)
            }
        }
    }

    public fun maximumActions(access: ProtectedAccess, recipe: WineRecipe): Int =
        with(access) {
            minOf(
                player.inv.count(objTypes[recipe.grapes]),
                player.inv.count(objTypes[objs.jug_water]),
                MAX_UNFERMENTED - unfermentedCount(),
            )
        }

    private fun ProtectedAccess.mix(recipe: WineRecipe, grapeSlot: Int, waterSlot: Int) {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.grapes.id, 1, slot = grapeSlot)
                delete(inventory, objs.jug_water.id, 1, slot = waterSlot)
                add(inventory, recipe.unfermented.id, 1, slot = grapeSlot)
            }
        check(result.success) { "Wine-making transaction failed after validation: $recipe" }
        spam(SQUEEZE_MESSAGE)
        timer(cooking_timers.wineFermentation, FERMENTATION_TICKS)
    }

    private fun ProtectedAccess.canMix(
        grapeSlot: Int,
        waterSlot: Int,
        recipe: WineRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (grapeSlot == waterSlot || player.inv[grapeSlot]?.id != recipe.grapes.id) {
            return false
        }
        if (player.inv[waterSlot]?.id != objs.jug_water.id) {
            return false
        }
        if (recipe.members && !player.members) {
            return reject(MEMBERS_MESSAGE)
        }
        if (player.stat(stats.cooking) < recipe.level) {
            return reject("You need a Cooking level of ${recipe.level} to make this.")
        }
        if (unfermentedCount() >= MAX_UNFERMENTED) {
            return false
        }
        return true
    }

    private fun ProtectedAccess.unfermentedCount(): Int =
        WineRecipes.all.sumOf { recipe ->
            player.inv.count(objTypes[recipe.unfermented]) +
                bank.count(objTypes[recipe.unfermented])
        }

    private fun ProtectedAccess.rollSuccessCount(recipe: WineRecipe, count: Int): Int {
        val batchSize = count / FERMENTATION_BATCHES
        val remainder = count % FERMENTATION_BATCHES
        var successes = 0
        if (batchSize > 0) {
            repeat(FERMENTATION_BATCHES) {
                if (statRandom(stats.cooking, recipe.chance.low, recipe.chance.high, 0)) {
                    successes += batchSize
                }
            }
        }
        repeat(remainder) {
            if (statRandom(stats.cooking, recipe.chance.low, recipe.chance.high, 0)) {
                successes++
            }
        }
        return successes
    }

    private fun ProtectedAccess.bankCanHoldEveryOutcome(
        outcomes: List<WineFermentationOutcome>
    ): Boolean {
        val releasedSlots = outcomes.sumOf { it.bankSlots.size }
        val possibleOutputs =
            outcomes
                .filter { outcome -> outcome.bankCount > 0 }
                .flatMap { outcome ->
                    buildList {
                        add(outcome.recipe.fermented)
                        if (player.stat(stats.cooking) < outcome.recipe.noFailureLevel) {
                            add(objs.jug_of_bad_wine)
                        }
                    }
                }
                .distinctBy(ObjType::id)
        val newOutputSlots = possibleOutputs.count { output -> !bank.contains(output) }
        return newOutputSlots <= bank.freeSpace() + releasedSlots
    }

    private fun ProtectedAccess.beginAction() {
        anim(seqs.human_make_wine)
        spotanim(spotanims.cooking_make_wine)
    }

    public companion object {
        public const val MENU_TITLE: String = "How many would you like to make?"
        public const val MAKE_ACTION_MODE: Int = 0
        public const val FIRST_RESULT_TICKS: Int = 3
        public const val REPEAT_RESULT_TICKS: Int = 2
        public const val FERMENTATION_TICKS: Int = 20
        public const val FERMENTATION_BATCHES: Int = 15
        public const val MAX_UNFERMENTED: Int = 60_000
        public const val XP_PER_SUCCESS: Double = 200.0
        public const val SQUEEZE_MESSAGE: String = "You squeeze the grapes into the jug."
        public const val MEMBERS_MESSAGE: String =
            "You need to be on a members' world to use this item."
        private const val QUEUE_HANDLER_TICK_COMPENSATION: Int = 1
        private const val BANK_CAPACITY_RETRY_TICKS: Int = 1

        public val SKILL_MULTI_BUTTON = components.skillmulti_a
    }
}

public data class WineRecipe(
    val grapes: ObjType,
    val unfermented: ObjType,
    val fermented: ObjType,
    val level: Int,
    val members: Boolean,
    val chance: CookingChance,
    val noFailureLevel: Int,
)

public object WineRecipes {
    public val regular =
        WineRecipe(
            grapes = objs.grapes,
            unfermented = objs.unfermented_wine,
            fermented = objs.jug_of_wine,
            level = 35,
            members = false,
            chance = CookingChance(48, 352),
            noFailureLevel = 68,
        )

    public val zamorak =
        WineRecipe(
            grapes = objs.zamoraks_grapes,
            unfermented = objs.zamoraks_unfermented_wine,
            fermented = objs.wine_of_zamorak,
            level = 65,
            members = true,
            chance = CookingChance(-125, 260),
            noFailureLevel = 98,
        )

    public val all: List<WineRecipe> = listOf(regular, zamorak)
    public val byGrapes: Map<Int, WineRecipe> = all.associateBy { it.grapes.id }
}

public data class WineMakingRequest(
    val grapes: ObjType,
    val grapeSlot: Int,
    val waterSlot: Int,
    val remaining: Int,
)

private data class WineFermentationOutcome(
    val recipe: WineRecipe,
    val inventorySlots: List<Int>,
    val bankSlots: List<Int>,
    val inventorySuccesses: Int,
    val bankSuccesses: Int,
    val bankCount: Int,
)
