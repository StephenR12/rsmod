package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType

public class LeapingFishGutting @Inject constructor() {
    public fun start(access: ProtectedAccess, firstSlot: Int, secondSlot: Int) {
        with(access) {
            val knifeSlot = selectedSlot(firstSlot, secondSlot, objs.knife) ?: return
            val fishSlot =
                LeapingFishGuttingRecipes.all.firstNotNullOfOrNull { recipe ->
                    selectedSlot(firstSlot, secondSlot, recipe.input)?.let { recipe to it }
                } ?: return
            if (!canGut(fishSlot.first, fishSlot.second, knifeSlot, exactKnifeSlot = true)) {
                return
            }
            clearLeapingFishGuttingQueue()
            processOne(fishSlot.first, fishSlot.second)
            scheduleNext(fishSlot.first)
        }
    }

    public fun process(access: ProtectedAccess, request: LeapingFishGuttingRequest) {
        with(access) {
            val fishSlot = player.inv.indexOfFirst { it?.id == request.input.id }
            if (fishSlot == -1 || !canGut(request.recipe, fishSlot, knifeSlot = -1)) {
                resetAnim()
                return
            }
            processOne(request.recipe, fishSlot)
            scheduleNext(request.recipe, compensateQueueHandler = true)
        }
    }

    private fun ProtectedAccess.processOne(recipe: LeapingFishGuttingRecipe, fishSlot: Int) {
        anim(seqs.brut_human_knifeuse)
        val success =
            guttingRollSucceeds(recipe, player.stat(stats.cooking), random.of(recipe.rate))
        val rolledOffcuts = success && random.of(recipe.offcutRate) < recipe.offcutNumerator
        val addOffcuts = rolledOffcuts && hasCapacityForBothOutputs(recipe)
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, recipe.input.id, 1, slot = fishSlot)
                if (success) {
                    add(inventory, recipe.primaryOutput.id, 1)
                    if (addOffcuts) {
                        add(inventory, objs.fish_offcuts.id, 1)
                    }
                }
            }
        check(result.success) {
            "Leaping-fish gutting transaction failed after validation: $recipe"
        }
        if (success) {
            statAdvance(stats.cooking, recipe.xp)
        }
    }

    private fun ProtectedAccess.scheduleNext(
        recipe: LeapingFishGuttingRecipe,
        compensateQueueHandler: Boolean = false,
    ) {
        if (!canContinue(recipe)) {
            return
        }
        weakQueue(
            fishing_queues.leapingFishGutting,
            AUTO_ACTION_TICKS + if (compensateQueueHandler) 1 else 0,
            LeapingFishGuttingRequest(recipe.input),
        )
    }

    private fun ProtectedAccess.canGut(
        recipe: LeapingFishGuttingRecipe,
        fishSlot: Int,
        knifeSlot: Int,
        exactKnifeSlot: Boolean = false,
    ): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to gut this fish.")
            return false
        }
        if (player.inv[fishSlot]?.id != recipe.input.id) {
            return false
        }
        return if (exactKnifeSlot) {
            player.inv[knifeSlot]?.id == objs.knife.id
        } else {
            player.inv.contains(objs.knife)
        }
    }

    private fun ProtectedAccess.canContinue(recipe: LeapingFishGuttingRecipe): Boolean =
        player.members && player.inv.contains(objs.knife) && player.inv.contains(recipe.input)

    private fun ProtectedAccess.hasCapacityForBothOutputs(
        recipe: LeapingFishGuttingRecipe
    ): Boolean {
        val freeAfterFishRemoval = player.inv.count { it == null } + 1
        val newStacks =
            listOf(recipe.primaryOutput, objs.fish_offcuts).count { !player.inv.contains(it) }
        return newStacks <= freeAfterFishRemoval
    }

    private fun ProtectedAccess.selectedSlot(firstSlot: Int, secondSlot: Int, type: ObjType): Int? =
        when {
            player.inv[firstSlot]?.id == type.id -> firstSlot
            player.inv[secondSlot]?.id == type.id -> secondSlot
            else -> null
        }

    private companion object {
        const val AUTO_ACTION_TICKS = 3
    }
}

public data class LeapingFishGuttingRequest(val input: ObjType) {
    public val recipe: LeapingFishGuttingRecipe
        get() = checkNotNull(LeapingFishGuttingRecipes.byInput[input])
}

public data class LeapingFishGuttingRecipe(
    val input: ObjType,
    val primaryOutput: ObjType,
    val xp: Double,
    val rate: Int,
    val maximumNumerator: Int,
    val offcutNumerator: Int,
    val offcutRate: Int,
)

public object LeapingFishGuttingRecipes {
    public val trout =
        LeapingFishGuttingRecipe(
            input = objs.leaping_trout,
            primaryOutput = objs.roe,
            xp = 10.0,
            rate = 150,
            maximumNumerator = 99,
            offcutNumerator = 1,
            offcutRate = 2,
        )
    public val salmon =
        LeapingFishGuttingRecipe(
            input = objs.leaping_salmon,
            primaryOutput = objs.roe,
            xp = 10.0,
            rate = 80,
            maximumNumerator = 80,
            offcutNumerator = 3,
            offcutRate = 4,
        )
    public val sturgeon =
        LeapingFishGuttingRecipe(
            input = objs.leaping_sturgeon,
            primaryOutput = objs.caviar,
            xp = 15.0,
            rate = 80,
            maximumNumerator = 80,
            offcutNumerator = 5,
            offcutRate = 6,
        )

    public val all: List<LeapingFishGuttingRecipe> = listOf(trout, salmon, sturgeon)
    public val byInput: Map<ObjType, LeapingFishGuttingRecipe> = all.associateBy { it.input }
}

public fun guttingRollSucceeds(
    recipe: LeapingFishGuttingRecipe,
    cookingLevel: Int,
    roll: Int,
): Boolean = roll < cookingLevel.coerceIn(1, recipe.maximumNumerator)
