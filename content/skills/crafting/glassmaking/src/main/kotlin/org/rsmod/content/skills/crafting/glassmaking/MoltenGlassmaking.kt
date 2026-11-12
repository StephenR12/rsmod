package org.rsmod.content.skills.crafting.glassmaking

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.content.skills.smithing.smelting.clearFurnaceQueues
import org.rsmod.content.skills.smithing.smelting.furnace_queues
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjTypeList

public class MoltenGlassmaking @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, furnace: BoundLocInfo) {
        with(access) {
            if (!canMake(sendFailure = true)) {
                return
            }
            val maximum =
                min(
                    player.inv.count(objTypes[objs.bucket_of_sand]),
                    player.inv.count(objTypes[objs.soda_ash]),
                )
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = listOf(objs.molten_glass),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = SMELT_ACTION_MODE,
                )
            if (!input.component.isType(components.skillmulti_a)) {
                return
            }
            start(this, furnace, input.subcomponent)
        }
    }

    public fun start(access: ProtectedAccess, furnace: BoundLocInfo, quantity: Int) {
        require(quantity >= 1) { "Molten-glass quantity must be positive." }
        with(access) {
            if (!canMake(sendFailure = true)) {
                return
            }
            clearCraftingQueues()
            clearFurnaceQueues()
            anim(seqs.human_furnace)
            weakQueue(
                furnace_queues.moltenGlassmaking,
                ACTION_TICKS,
                MoltenGlassmakingRequest(furnace, quantity),
            )
        }
    }

    public fun process(access: ProtectedAccess, request: MoltenGlassmakingRequest) {
        with(access) {
            if (!isWithinDistance(request.furnace, distance = 1) || !canMake(sendFailure = false)) {
                resetAnim()
                return
            }
            make()
            val remaining = request.remaining - 1
            if (remaining <= 0 || !canMake(sendFailure = false)) {
                resetAnim()
                return
            }
            anim(seqs.human_furnace)
            weakQueue(
                furnace_queues.moltenGlassmaking,
                ACTION_TICKS + RESCHEDULE_TICK_COMPENSATION,
                request.copy(remaining = remaining),
            )
        }
    }

    private fun ProtectedAccess.make() {
        val result =
            player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.bucket_of_sand.id, 1)
                delete(inventory, objs.soda_ash.id, 1)
                add(inventory, objs.bucket_empty.id, 1)
                add(inventory, objs.molten_glass.id, 1)
            }
        check(result.success) { "Molten-glass transaction failed after validation." }
        statAdvance(stats.crafting, CRAFTING_XP)
    }

    private fun ProtectedAccess.canMake(sendFailure: Boolean): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }

        if (!player.members) {
            return reject("You need to be on a members' world to make molten glass.")
        }
        if (player.stat(stats.crafting) < CRAFTING_LEVEL) {
            return reject("You need a Crafting level of 1 to make molten glass.")
        }
        if (
            player.inv.count(objTypes[objs.bucket_of_sand]) == 0 ||
                player.inv.count(objTypes[objs.soda_ash]) == 0
        ) {
            return reject("You need soda ash and a bucket of sand to make molten glass.")
        }
        return true
    }

    private companion object {
        const val CRAFTING_LEVEL = 1
        const val CRAFTING_XP = 20.0
        const val ACTION_TICKS = 2
        const val RESCHEDULE_TICK_COMPENSATION = 1
        const val SMELT_ACTION_MODE = 13
        const val MENU_TITLE = "What would you like to smelt?"
    }
}

public data class MoltenGlassmakingRequest(val furnace: BoundLocInfo, val remaining: Int)
