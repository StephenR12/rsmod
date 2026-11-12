package org.rsmod.content.skills.crafting.birdhouses

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.crafting.commons.crafting_queues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class BirdhouseCrafting @Inject constructor(private val objTypes: ObjTypeList) {
    public fun start(access: ProtectedAccess, recipe: BirdhouseRecipe) {
        with(access) {
            if (!canMakeInternal(recipe, sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            anim(craftingSequence())
            weakQueue(
                crafting_queues.birdhouseCrafting,
                FIRST_RESULT_TICKS,
                BirdhouseCraftingRequest(recipe.output),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: BirdhouseCraftingRequest) {
        val recipe = BirdhouseRecipes.forOutput(request.output) ?: return
        with(access) {
            if (!makeOne(this, recipe)) {
                resetAnim()
                return
            }

            if (!canMakeInternal(recipe, sendFailure = false)) {
                resetAnim()
                return
            }
            anim(craftingSequence())
            weakQueue(
                crafting_queues.birdhouseCrafting,
                REPEAT_RESULT_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request,
            )
        }
    }

    /**
     * Makes one inventory bird house with the same material transaction and Crafting XP used by the
     * repeated item-on-item action. This deliberately does not start an animation or queue so
     * callers such as Fossil Island's one-click build can own their distinct action timing.
     */
    public fun makeOne(access: ProtectedAccess, recipe: BirdhouseRecipe): Boolean {
        with(access) {
            if (!canMakeInternal(recipe, sendFailure = false)) {
                return false
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.logs.id, 1)
                    delete(inventory, birdhouse_objs.clockwork.id, 1)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) { "Birdhouse transaction failed after validation: $recipe" }
            statAdvance(stats.crafting, recipe.xp)
            return true
        }
    }

    /**
     * Crafts around the clockwork recovered from the bird house currently on the patch. Only the
     * replacement log is consumed: the existing mechanism never needs a temporary inventory slot.
     */
    public fun makeFromReturnedClockwork(
        access: ProtectedAccess,
        recipe: BirdhouseRecipe,
    ): Boolean {
        with(access) {
            if (!canMakeInternal(recipe, sendFailure = false, requireClockwork = false)) {
                return false
            }
            val result =
                player.invTransaction(player.inv) { delete(select(player.inv), recipe.logs.id, 1) }
            check(result.success) { "Birdhouse reset transaction failed after validation: $recipe" }
            statAdvance(stats.crafting, recipe.xp)
            return true
        }
    }

    public fun canMake(
        access: ProtectedAccess,
        recipe: BirdhouseRecipe,
        sendFailure: Boolean,
        requireClockwork: Boolean = true,
    ): Boolean = access.canMakeInternal(recipe, sendFailure, requireClockwork)

    public fun beginSingle(access: ProtectedAccess) {
        access.anim(access.craftingSequence())
    }

    private fun ProtectedAccess.canMakeInternal(
        recipe: BirdhouseRecipe,
        sendFailure: Boolean,
        requireClockwork: Boolean = true,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to make a bird house.")
        }
        if (player.stat(stats.crafting) < recipe.level) {
            return reject("You need a Crafting level of ${recipe.level} to make this bird house.")
        }
        if (!player.inv.contains(objs.chisel)) {
            return reject("You need a chisel to make a bird house.")
        }
        if (!hasHammer()) {
            return reject("You need a hammer to make a bird house.")
        }
        if (player.inv.count(objTypes[recipe.logs]) == 0) {
            return false
        }
        if (requireClockwork && player.inv.count(objTypes[birdhouse_objs.clockwork]) == 0) {
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasHammer(): Boolean =
        player.inv.any { it?.id in HAMMERS } || player.worn.any { it?.id in HAMMERS }

    private fun ProtectedAccess.craftingSequence() =
        if (
            player.inv.contains(objs.imcando_hammer) ||
                player.worn.contains(objs.imcando_hammer) ||
                player.inv.contains(objs.imcando_hammer_offhand) ||
                player.worn.contains(objs.imcando_hammer_offhand)
        ) {
            birdhouse_seqs.makeWithImcandoHammer
        } else {
            birdhouse_seqs.make
        }

    private companion object {
        const val FIRST_RESULT_TICKS = 2
        const val REPEAT_RESULT_TICKS = 3
        const val RESCHEDULE_TICK_COMPENSATION = 1
        val HAMMERS = setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
    }
}

public data class BirdhouseCraftingRequest(val output: ObjType)
