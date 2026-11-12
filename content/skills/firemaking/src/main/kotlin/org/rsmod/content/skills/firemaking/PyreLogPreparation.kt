package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.type.obj.ObjTypeList

public class PyreLogPreparation
@Inject
constructor(private val objTypes: ObjTypeList, private val firemakingXp: FiremakingXp) {
    public suspend fun select(
        access: ProtectedAccess,
        oilSlot: Int,
        logSlot: Int,
        recipe: PyreLogRecipe,
    ) {
        with(access) {
            val selectedOil = SacredOils.byObj[player.inv[oilSlot]?.id] ?: return
            if (player.inv[logSlot]?.id != recipe.input.id) {
                return
            }
            if (!canPrepare(recipe, oilSlot, logSlot, sendFailure = true)) {
                return
            }
            val maximum = availableConversions(recipe)
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = MAKE_ACTION_MODE,
                )
            if (!components.skillmulti_a.isType(input.component)) {
                return
            }
            start(
                this,
                recipe,
                quantity = input.subcomponent,
                firstOilSlot = oilSlot,
                firstOilId = selectedOil.obj.id,
                firstLogSlot = logSlot,
            )
        }
    }

    public fun start(
        access: ProtectedAccess,
        recipe: PyreLogRecipe,
        quantity: Int,
        firstOilSlot: Int? = null,
        firstOilId: Int? = null,
        firstLogSlot: Int? = null,
    ) {
        require(quantity >= 1) { "Pyre-log preparation quantity must be positive." }
        with(access) {
            if (!canPrepare(recipe, firstOilSlot, firstLogSlot, sendFailure = true)) {
                return
            }
            if (
                firstOilId != null &&
                    (firstOilSlot == null || player.inv[firstOilSlot]?.id != firstOilId)
            ) {
                return
            }
            clearWeakQueue(firemaking_queues.attempt)
            clearWeakQueue(firemaking_queues.pyreLogPreparation)
            prepareOne(recipe, firstOilSlot, firstLogSlot)
            val remaining = quantity - 1
            if (remaining <= 0 || !canPrepare(recipe, sendFailure = false)) {
                return
            }
            weakQueue(
                firemaking_queues.pyreLogPreparation,
                ACTION_TICKS,
                PyreLogPreparationRequest(input = recipe.input.id, remaining = remaining),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: PyreLogPreparationRequest) {
        val recipe = PyreLogRecipes.byInput[request.input] ?: return
        with(access) {
            if (!canPrepare(recipe, sendFailure = false)) {
                return
            }
            prepareOne(recipe, preferredOilSlot = null, preferredLogSlot = null)
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canPrepare(recipe, sendFailure = false)) {
                return
            }
            weakQueue(
                firemaking_queues.pyreLogPreparation,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.prepareOne(
        recipe: PyreLogRecipe,
        preferredOilSlot: Int?,
        preferredLogSlot: Int?,
    ) {
        val oilSlot = preferredOilSlot ?: findOilSlot(recipe.oilDoses)
        val logSlot = preferredLogSlot ?: player.inv.indexOfFirst { it?.id == recipe.input.id }
        check(oilSlot >= 0 && logSlot >= 0) {
            "Pyre-log preparation reached mutation without materials: $recipe"
        }
        val oil = SacredOils.byObj.getValue(requireNotNull(player.inv[oilSlot]).id)
        val remainingDoses = oil.doses - recipe.oilDoses
        val oilRemainder = SacredOils.withDoses(remainingDoses)?.obj ?: objs.empty_vial
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, oil.obj.id, 1, slot = oilSlot)
                delete(inventory, recipe.input.id, 1, slot = logSlot)
                add(inventory, oilRemainder.id, 1, slot = oilSlot)
                add(inventory, recipe.output.id, 1, slot = logSlot)
            }
        check(result.success) { "Pyre-log preparation transaction failed: $recipe" }
        firemakingXp.advance(this, recipe.xp)
    }

    private fun ProtectedAccess.canPrepare(
        recipe: PyreLogRecipe,
        requiredOilSlot: Int? = null,
        requiredLogSlot: Int? = null,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to prepare pyre logs.")
        }
        if (player.vars[varps.morttonquest] < MIN_QUEST_STATE) {
            return reject(
                "You need to progress further in Shades of Mort'ton before preparing pyre logs."
            )
        }
        if (player.vars[varps.morttonquest] < QUEST_COMPLETE_STATE && recipe.input != objs.logs) {
            return reject(
                "You need to complete Shades of Mort'ton before preparing these pyre logs."
            )
        }
        val hasLog =
            if (requiredLogSlot == null) {
                player.inv.any { it?.id == recipe.input.id }
            } else {
                player.inv[requiredLogSlot]?.id == recipe.input.id
            }
        if (!hasLog) {
            return reject("You do not have any ${objTypes[recipe.input].name.lowercase()} left.")
        }
        val hasOil =
            if (requiredOilSlot == null) {
                findOilSlot(recipe.oilDoses) >= 0
            } else {
                val oil = SacredOils.byObj[player.inv[requiredOilSlot]?.id]
                oil != null && oil.doses >= recipe.oilDoses
            }
        if (!hasOil) {
            return reject("You do not have enough sacred oil to prepare these logs.")
        }
        return true
    }

    private fun ProtectedAccess.findOilSlot(requiredDoses: Int): Int =
        player.inv.indexOfFirst { invObj ->
            val oil = invObj?.let { SacredOils.byObj[it.id] }
            oil != null && oil.doses >= requiredDoses
        }

    private fun ProtectedAccess.availableConversions(recipe: PyreLogRecipe): Int {
        val logs = player.inv.count(objTypes[recipe.input])
        val oilCapacity =
            player.inv.sumOf { invObj ->
                val doses = invObj?.let { SacredOils.byObj[it.id]?.doses } ?: 0
                doses / recipe.oilDoses
            }
        return minOf(logs, oilCapacity)
    }

    public companion object {
        public const val MIN_QUEST_STATE: Int = 65
        public const val QUEST_COMPLETE_STATE: Int = 85
        public const val ACTION_TICKS: Int = 4
        public const val RESCHEDULE_TICK_COMPENSATION: Int = 1
        public const val MAKE_ACTION_MODE: Int = 0
        public const val MENU_TITLE: String = "How many would you like to make?"
    }
}

public data class PyreLogPreparationRequest(val input: Int, val remaining: Int)
