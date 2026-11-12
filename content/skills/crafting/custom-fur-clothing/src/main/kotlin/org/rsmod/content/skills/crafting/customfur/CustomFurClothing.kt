package org.rsmod.content.skills.crafting.customfur

import jakarta.inject.Inject
import java.util.Collections
import java.util.WeakHashMap
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class CustomFurClothing
@Inject
constructor(private val recipes: CustomFurRecipeRegistry, private val objTypes: ObjTypeList) {
    private val activePlayers: MutableSet<Player> =
        Collections.newSetFromMap(WeakHashMap<Player, Boolean>())

    public fun open(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                return
            }
            val display = inv(invs.hunting_customfurshop)
            for ((slot, recipe) in recipes.recipes.withIndex()) {
                display[slot] = InvObj(recipe.output)
            }
            invTransmit(display)
            ifOpenMainModal(interfaces.hunting_customfurs)
            ifSetEvents(
                components.hunting_customfurs_choices_items,
                display.indices,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
                IfEvent.Op10,
            )
            activePlayers += player
        }
    }

    public fun close(player: Player) {
        activePlayers -= player
        val display = player.invMap[invs.hunting_customfurshop] ?: return
        player.stopInvTransmit(display)
    }

    public fun make(access: ProtectedAccess, slot: Int, clientOutput: ObjType?) {
        with(access) {
            if (player !in activePlayers || interfaces.hunting_customfurs !in player.ui) {
                return
            }
            if (!player.members) {
                return
            }
            val display = player.invMap[invs.hunting_customfurshop] ?: return
            val displayed = display[slot] ?: return
            if (clientOutput == null || displayed.id != clientOutput.id) {
                return
            }
            val recipe = recipes.forOutput(clientOutput) ?: return
            val fur = recipe.furs.firstOrNull { player.inv.count(objTypes[it]) >= recipe.furCount }
            if (fur == null) {
                mes("You don't have the required fur to make that item.")
                return
            }
            if (player.inv.count(objTypes[objs.coins]) < recipe.fee) {
                mes("You don't have enough coins to pay for that item.")
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, fur.id, recipe.furCount)
                    delete(inventory, objs.coins.id, recipe.fee)
                    add(inventory, recipe.output.id, 1)
                }
            check(result.success) { "Custom fur transaction failed after validation: $recipe" }
        }
    }

    public fun display(player: Player): Inventory? = player.invMap[invs.hunting_customfurshop]
}
