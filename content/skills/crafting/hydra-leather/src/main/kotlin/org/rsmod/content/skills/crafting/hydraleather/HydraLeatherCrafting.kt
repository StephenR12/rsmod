package org.rsmod.content.skills.crafting.hydraleather

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.seq.SeqType

public class HydraLeatherCrafting @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun inspect(access: ProtectedAccess) {
        access.startDialogue {
            objbox(
                objs.hydra_leather,
                "This leather looks pretty tough to work with... Maybe the dragonkin had a way.",
            )
        }
    }

    public suspend fun investigate(access: ProtectedAccess, machinery: BoundLocInfo) {
        val input =
            when {
                access.player.inv.contains(objs.hydra_leather) -> objs.hydra_leather
                access.player.inv.contains(objs.ferocious_gloves) -> objs.ferocious_gloves
                else -> {
                    access.mes("You have nothing suitable to feed through the machine.")
                    return
                }
            }
        transform(access, machinery, input)
    }

    public suspend fun transform(access: ProtectedAccess, machinery: BoundLocInfo, input: ObjType) {
        val recipe = recipe(input) ?: return
        with(access) {
            if (!canTransform(recipe, sendFailure = true)) {
                return
            }
            startDialogue {
                val confirmed = choice2("Yes", true, "No", false, title = recipe.confirmation)
                if (!confirmed || !canTransform(recipe, sendFailure = false)) {
                    return@startDialogue
                }
                clearCraftingQueues()
                anim(hammerAnimation())
                delay(ACTION_TICKS)
                if (
                    !isWithinDistance(machinery, distance = 1) ||
                        !canTransform(recipe, sendFailure = false)
                ) {
                    resetAnim()
                    return@startDialogue
                }
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, recipe.input.id, 1)
                        add(inventory, recipe.output.id, 1)
                    }
                check(result.success) {
                    "Hydra-leather transaction failed after validation: $recipe"
                }
                resetAnim()
                objbox(recipe.output, recipe.successMessage)
            }
        }
    }

    private fun ProtectedAccess.canTransform(
        recipe: HydraLeatherRecipe,
        sendFailure: Boolean,
    ): Boolean {
        fun reject(message: String): Boolean {
            if (sendFailure) {
                mes(message)
            }
            return false
        }
        if (!player.members) {
            return reject("You need to be on a members' world to use the machine.")
        }
        if (player.vars[varbits.dragon_slayer_ii_progress] < QUEST_COMPLETE_STATE) {
            return reject("You need to complete Dragon Slayer II to use the machine.")
        }
        if (!hasHammer()) {
            return reject("You need a hammer to use the machine.")
        }
        if (player.inv.count(objTypes[recipe.input]) == 0) {
            return reject("You no longer have the item you wanted to feed through the machine.")
        }
        return true
    }

    private fun ProtectedAccess.hasHammer(): Boolean =
        player.inv.any { it?.id in HAMMERS } || player.worn.any { it?.id in HAMMERS }

    private fun ProtectedAccess.hammerAnimation(): SeqType =
        if (
            player.inv.contains(objs.imcando_hammer) ||
                player.worn.contains(objs.imcando_hammer) ||
                player.inv.contains(objs.imcando_hammer_offhand) ||
                player.worn.contains(objs.imcando_hammer_offhand)
        ) {
            seqs.human_hammer_hit_imcando
        } else {
            seqs.human_hammer_hit
        }

    private fun recipe(input: ObjType): HydraLeatherRecipe? =
        when (input) {
            objs.hydra_leather ->
                HydraLeatherRecipe(
                    input = objs.hydra_leather,
                    output = objs.ferocious_gloves,
                    confirmation =
                        "Are sure you want to make the Ferocious Gloves? " +
                            "<col=ff0000>(This will make them untradeable)</col>",
                    successMessage =
                        "By feeding the tough to work leather through the machine, you manage to " +
                            "form a pair of gloves.",
                )
            objs.ferocious_gloves ->
                HydraLeatherRecipe(
                    input = objs.ferocious_gloves,
                    output = objs.hydra_leather,
                    confirmation =
                        "Would you like to revert your Ferocious Gloves into Hydra Leather?",
                    successMessage =
                        "By feeding the gloves through the machine, you manage to revert them " +
                            "into leather.",
                )
            else -> null
        }

    private companion object {
        const val ACTION_TICKS = 7
        const val QUEST_COMPLETE_STATE = 215

        val HAMMERS = setOf(objs.hammer.id, objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
    }
}

private data class HydraLeatherRecipe(
    val input: ObjType,
    val output: ObjType,
    val confirmation: String,
    val successMessage: String,
)
