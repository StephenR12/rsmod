package org.rsmod.content.skills.cooking

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
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType

public class IronSpitCooking @Inject constructor() {
    public fun skewer(
        access: ProtectedAccess,
        recipe: IronSpitRecipe,
        rawSlot: Int,
        spitSlot: Int,
    ) {
        with(access) {
            clearCookingQueues()
            if (!canSkewer(recipe, rawSlot, spitSlot, sendFailure = true)) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.raw.id, 1, slot = rawSlot)
                    delete(inventory, objs.iron_spit.id, 1, slot = spitSlot)
                    add(inventory, recipe.skewered.id, 1, slot = rawSlot)
                }
            check(result.success) { "Iron-spit skewering transaction failed: $recipe" }
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: IronSpitRecipe,
        source: BoundLocInfo,
        inputSlot: Int,
    ) {
        with(access) {
            clearCookingQueues()
            if (!canRoast(recipe, inputSlot, sendFailure = true)) {
                return
            }
            anim(seqs.spit_fire)
            weakQueue(
                cooking_queues.ironSpit,
                RESULT_TICKS,
                IronSpitCookingRequest(source, recipe, inputSlot),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: IronSpitCookingRequest) {
        with(access) {
            if (
                !isWithinDistance(request.source, distance = 1) ||
                    !canRoast(request.recipe, request.inputSlot, sendFailure = false)
            ) {
                resetAnim()
                return
            }
            val success = hasCookingBurnImmunity() || rollSuccess(request.recipe)
            val output = if (success) request.recipe.roasted else request.recipe.burnt
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, request.recipe.skewered.id, 1, slot = request.inputSlot)
                    add(inventory, output.id, 1)
                    add(inventory, objs.iron_spit.id, 1)
                }
            check(result.success) { "Iron-spit roasting transaction failed: ${request.recipe}" }
            if (success) {
                statAdvance(stats.cooking, request.recipe.xp)
                mes(request.recipe.successMessage)
            } else {
                mes(request.recipe.burnMessage)
            }
            resetAnim()
        }
    }

    private fun ProtectedAccess.canSkewer(
        recipe: IronSpitRecipe,
        rawSlot: Int,
        spitSlot: Int,
        sendFailure: Boolean,
    ): Boolean {
        if (!checkMembership(sendFailure)) {
            return false
        }
        if (player.stat(stats.cooking) < recipe.level) {
            if (sendFailure) {
                mes("You need a Cooking level of ${recipe.level} to skewer this meat.")
            }
            return false
        }
        if (!checkFiremaking(sendFailure)) {
            return false
        }
        return player.inv[rawSlot]?.id == recipe.raw.id &&
            player.inv[spitSlot]?.id == objs.iron_spit.id
    }

    private fun ProtectedAccess.canRoast(
        recipe: IronSpitRecipe,
        inputSlot: Int,
        sendFailure: Boolean,
    ): Boolean {
        if (!checkMembership(sendFailure)) {
            return false
        }
        if (player.stat(stats.cooking) < recipe.level) {
            if (sendFailure) {
                mes("You need a Cooking level of ${recipe.level} to roast this meat.")
            }
            return false
        }
        if (recipe.firemakingRequiredToRoast && !checkFiremaking(sendFailure)) {
            return false
        }
        if (player.inv[inputSlot]?.id != recipe.skewered.id) {
            return false
        }
        if (player.inv.freeSpace() < 1) {
            if (sendFailure) {
                mes("You don't have enough inventory space.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.checkMembership(sendFailure: Boolean): Boolean {
        if (player.members) {
            return true
        }
        if (sendFailure) {
            mes("You need to be on a members' world to use an iron spit.")
        }
        return false
    }

    private fun ProtectedAccess.checkFiremaking(sendFailure: Boolean): Boolean {
        if (player.stat(stats.firemaking) >= FIREMAKING_LEVEL) {
            return true
        }
        if (sendFailure) {
            mes("You need a Firemaking level of $FIREMAKING_LEVEL to use an iron spit.")
        }
        return false
    }

    private fun ProtectedAccess.rollSuccess(recipe: IronSpitRecipe): Boolean =
        statRandom(stats.cooking, recipe.chance.low, recipe.chance.high, invisibleBoost = 0)

    public companion object {
        public const val FIREMAKING_LEVEL: Int = 20
        public const val RESULT_TICKS: Int = 3
    }
}

public data class IronSpitRecipe(
    val raw: ObjType,
    val skewered: ObjType,
    val roasted: ObjType,
    val burnt: ObjType,
    val level: Int,
    val xp: Double,
    val chance: CookingChance,
    val firemakingRequiredToRoast: Boolean,
    val successMessage: String,
    val burnMessage: String,
)

public object IronSpitRecipes {
    public val rabbit: IronSpitRecipe =
        IronSpitRecipe(
            raw = objs.raw_rabbit,
            skewered = objs.skewered_rabbit,
            roasted = objs.roast_rabbit,
            burnt = objs.burnt_rabbit_spit,
            level = 16,
            xp = 72.5,
            chance = CookingChance(160, 255),
            firemakingRequiredToRoast = false,
            successMessage = "You roast a rabbit.",
            burnMessage = "You accidentally burn the rabbit.",
        )

    public val bird: IronSpitRecipe =
        IronSpitRecipe(
            raw = objs.raw_bird_meat,
            skewered = objs.skewered_bird_meat,
            roasted = objs.roast_bird_meat,
            burnt = objs.burnt_bird_meat,
            level = 11,
            xp = 62.5,
            chance = CookingChance(155, 255),
            firemakingRequiredToRoast = true,
            successMessage = "You roast a piece of bird meat.",
            burnMessage = "You accidentally burn the bird meat.",
        )

    public val beast: IronSpitRecipe =
        IronSpitRecipe(
            raw = objs.raw_beast_meat,
            skewered = objs.skewered_beast_meat,
            roasted = objs.roast_beast_meat,
            burnt = objs.burnt_beast_meat,
            level = 21,
            xp = 82.5,
            chance = CookingChance(180, 255),
            firemakingRequiredToRoast = false,
            successMessage = "You roast a piece of beast meat.",
            burnMessage = "You accidentally burn the beast meat.",
        )

    public val all: List<IronSpitRecipe> = listOf(rabbit, bird, beast)
}

public data class IronSpitCookingRequest(
    val source: BoundLocInfo,
    val recipe: IronSpitRecipe,
    val inputSlot: Int,
)
