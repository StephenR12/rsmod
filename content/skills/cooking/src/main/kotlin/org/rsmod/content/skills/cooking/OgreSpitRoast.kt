package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType

public class OgreSpitRoast @Inject constructor() {
    public fun start(access: ProtectedAccess, source: BoundLocInfo, inputSlot: Int) {
        start(access, source, inputSlot, CHOMPY_RECIPE)
    }

    public fun startJubbly(access: ProtectedAccess, source: BoundLocInfo, inputSlot: Int) {
        start(access, source, inputSlot, JUBBLY_RECIPE)
    }

    public fun startChicken(access: ProtectedAccess, source: BoundLocInfo, inputSlot: Int) {
        start(access, source, inputSlot, CHICKEN_RECIPE)
    }

    private fun start(
        access: ProtectedAccess,
        source: BoundLocInfo,
        inputSlot: Int,
        recipe: OgreSpitRoastRecipe,
    ) {
        with(access) {
            clearCookingQueues()
            if (!canCook(recipe, inputSlot, sendFailure = true)) {
                return
            }
            player.ogreSpitRoastState = recipe.rawState
            player.softTimer(cooking_timers.ogreSpitRoastState, STATE_WATCHDOG_TICKS)
            anim(seqs.spit_anim_player)
            weakQueue(
                cooking_queues.ogreSpitRoast,
                recipe.resultTicks,
                OgreSpitRoastRequest(source, inputSlot, recipe.raw),
            )
            mes(recipe.startMessage)
        }
    }

    public fun process(access: ProtectedAccess, request: OgreSpitRoastRequest) {
        val recipe = recipeFor(request.raw) ?: return
        with(access) {
            if (
                !isWithinDistance(request.source, distance = 1) ||
                    player.ogreSpitRoastState != recipe.rawState ||
                    !canCook(recipe, request.inputSlot, sendFailure = false)
            ) {
                clearState(player)
                resetAnim()
                return
            }
            val success = hasCookingBurnImmunity() || rollSuccess(recipe)
            val output = if (success) recipe.cooked else recipe.burnt
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.raw.id, 1, slot = request.inputSlot)
                    add(inventory, output.id, 1, slot = request.inputSlot)
                }
            check(result.success) { "Ogre-spit transaction failed after validation: $recipe" }
            player.ogreSpitRoastState = if (success) recipe.cookedState else recipe.burntState
            player.softTimer(cooking_timers.ogreSpitRoastState, RESULT_DISPLAY_TICKS)
            resetAnim()
            anim(seqs.spit_end_anim_player)
            if (success) {
                statAdvance(stats.cooking, recipe.xp)
                mes(recipe.successMessage)
            } else {
                mes(recipe.burnMessage)
            }
        }
    }

    public fun tickState(player: Player) {
        when (player.ogreSpitRoastState) {
            EMPTY_STATE -> player.clearSoftTimer(cooking_timers.ogreSpitRoastState)
            RAW_CHOMPY_STATE,
            RAW_CHICKEN_STATE,
            RAW_JUBBLY_STATE -> {
                if (cooking_queues.ogreSpitRoast !in player.weakQueueList) {
                    clearState(player)
                    player.resetAnim()
                }
            }
            else -> clearState(player)
        }
    }

    public fun clearState(player: Player) {
        player.clearWeakQueue(cooking_queues.ogreSpitRoast)
        player.clearOgreSpitRoastState()
    }

    private fun ProtectedAccess.canCook(
        recipe: OgreSpitRoastRecipe,
        inputSlot: Int,
        sendFailure: Boolean,
    ): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to cook this food.")
            }
            return false
        }
        if (
            recipe.questState != null &&
                player.vars[varbits.rfd_skrach_uglogwee] < recipe.questState
        ) {
            if (sendFailure) {
                mes("You need to progress further through Recipe for Disaster to cook this food.")
            }
            return false
        }
        if (player.stat(stats.cooking) < recipe.level) {
            if (sendFailure) {
                mes("You need a Cooking level of ${recipe.level} to cook this food.")
            }
            return false
        }
        return player.inv[inputSlot]?.id == recipe.raw.id
    }

    private fun ProtectedAccess.rollSuccess(recipe: OgreSpitRoastRecipe): Boolean =
        statRandom(stats.cooking, recipe.chance.low, recipe.chance.high, invisibleBoost = 0)

    private fun recipeFor(raw: ObjType): OgreSpitRoastRecipe? =
        when (raw.id) {
            objs.raw_chompy.id -> CHOMPY_RECIPE
            objs.raw_chicken.id -> CHICKEN_RECIPE
            objs.raw_jubbly.id -> JUBBLY_RECIPE
            else -> null
        }

    public companion object {
        public const val EMPTY_STATE: Int = 0
        public const val RAW_CHOMPY_STATE: Int = 1
        public const val COOKED_CHOMPY_STATE: Int = 2
        public const val RUINED_CHOMPY_STATE: Int = 3
        public const val RAW_CHICKEN_STATE: Int = 4
        public const val COOKED_CHICKEN_STATE: Int = 5
        public const val RAW_JUBBLY_STATE: Int = 6
        public const val COOKED_JUBBLY_STATE: Int = 7
        public const val BURNT_JUBBLY_STATE: Int = 8
        public const val CHOMPY_COOKING_LEVEL: Int = 30
        public const val CHOMPY_COOKING_XP: Double = 100.0
        public const val CHOMPY_RESULT_TICKS: Int = 5
        public const val CHICKEN_COOKING_LEVEL: Int = 30
        public const val CHICKEN_COOKING_XP: Double = 30.0
        public const val CHICKEN_RESULT_TICKS: Int = 6
        public const val JUBBLY_COOKING_LEVEL: Int = 41
        public const val JUBBLY_COOKING_XP: Double = 160.0
        public const val JUBBLY_RESULT_TICKS: Int = 6
        public const val JUBBLY_QUEST_STATE: Int = 110
        public const val STATE_WATCHDOG_TICKS: Int = 1
        public const val RESULT_DISPLAY_TICKS: Int = 1
        public val CHOMPY_CHANCE: CookingChance = CookingChance(200, 255)
        public val CHICKEN_CHANCE: CookingChance = CookingChance(128, 512)
        public val JUBBLY_CHANCE: CookingChance = CookingChance(191, 245)

        private val CHOMPY_RECIPE =
            OgreSpitRoastRecipe(
                raw = objs.raw_chompy,
                cooked = objs.cooked_chompy,
                burnt = objs.ruined_chompy,
                rawState = RAW_CHOMPY_STATE,
                cookedState = COOKED_CHOMPY_STATE,
                burntState = RUINED_CHOMPY_STATE,
                level = CHOMPY_COOKING_LEVEL,
                xp = CHOMPY_COOKING_XP,
                chance = CHOMPY_CHANCE,
                resultTicks = CHOMPY_RESULT_TICKS,
                questState = null,
                startMessage = "You carefully place the chompy bird on the spit-roast.",
                successMessage = "You spit-roast the chompy bird.",
                burnMessage = "You accidentally burn the chompy.",
            )

        private val CHICKEN_RECIPE =
            OgreSpitRoastRecipe(
                raw = objs.raw_chicken,
                cooked = objs.cooked_chicken,
                burnt = objs.burnt_chicken,
                rawState = RAW_CHICKEN_STATE,
                cookedState = COOKED_CHICKEN_STATE,
                burntState = RUINED_CHOMPY_STATE,
                level = CHICKEN_COOKING_LEVEL,
                xp = CHICKEN_COOKING_XP,
                chance = CHICKEN_CHANCE,
                resultTicks = CHICKEN_RESULT_TICKS,
                questState = null,
                startMessage = "You carefully place the chicken on the spit-roast.",
                successMessage = "You roast a chicken.",
                burnMessage = "You accidentally burn the chicken.",
            )

        private val JUBBLY_RECIPE =
            OgreSpitRoastRecipe(
                raw = objs.raw_jubbly,
                cooked = objs.cooked_jubbly,
                burnt = objs.burnt_jubbly,
                rawState = RAW_JUBBLY_STATE,
                cookedState = COOKED_JUBBLY_STATE,
                burntState = BURNT_JUBBLY_STATE,
                level = JUBBLY_COOKING_LEVEL,
                xp = JUBBLY_COOKING_XP,
                chance = JUBBLY_CHANCE,
                resultTicks = JUBBLY_RESULT_TICKS,
                questState = JUBBLY_QUEST_STATE,
                startMessage = "You carefully place the jubbly bird on the spit-roast.",
                successMessage = "Eventually the Jubbly is cooked.",
                burnMessage = "Unfortunately the Jubbly is burnt.",
            )
    }
}

public data class OgreSpitRoastRequest(
    val source: BoundLocInfo,
    val inputSlot: Int,
    val raw: ObjType,
)

private data class OgreSpitRoastRecipe(
    val raw: ObjType,
    val cooked: ObjType,
    val burnt: ObjType,
    val rawState: Int,
    val cookedState: Int,
    val burntState: Int,
    val level: Int,
    val xp: Double,
    val chance: CookingChance,
    val resultTicks: Int,
    val questState: Int?,
    val startMessage: String,
    val successMessage: String,
    val burnMessage: String,
)
