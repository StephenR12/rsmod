package org.rsmod.content.skills.crafting.tanning

import jakarta.inject.Inject
import java.util.WeakHashMap
import kotlin.math.min
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.npcs
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList

public enum class TannerPricing {
    Standard,
    Premium,
    Eodan,
}

public class Tanning
@Inject
constructor(private val recipes: TanningRecipeRegistry, private val objTypes: ObjTypeList) {
    private val activeTanner: MutableMap<Player, Int> = WeakHashMap()
    private val activePricing: MutableMap<Player, TannerPricing> = WeakHashMap()

    public fun open(access: ProtectedAccess, pricing: TannerPricing, tannerId: Int? = null) {
        with(access) {
            if (interfaces.tanner !in player.ui) {
                ifOpenMainModal(interfaces.tanner)
            }
            activePricing[player] = pricing
            if (tannerId == null) activeTanner.remove(player) else activeTanner[player] = tannerId
            for (recipe in recipes.recipes) {
                ifSetObj(recipe.modelComponent, recipe.output, zoom = 1)
                ifSetText(recipe.textComponent, recipe.name)
                val price = price(recipe, pricing)
                ifSetText(recipe.priceComponent, "$price Coin${if (price == 1) "" else "s"}")
                for (button in recipe.buttons.values) {
                    ifSetEvents(button, 0..0, IfEvent.Op1)
                }
            }
        }
    }

    public fun close(player: Player) {
        activePricing.remove(player)
        activeTanner.remove(player)
    }

    public fun tan(access: ProtectedAccess, recipe: TanningRecipe, requested: Int) {
        val pricing = activePricing[access.player] ?: return
        with(access) {
            if (recipe.members && !player.members) {
                mes("You need to be on a members' world to tan that hide.")
                return
            }
            val hideCount = player.inv.count(objTypes[recipe.input])
            if (hideCount == 0) {
                mes("You don't have any suitable hides to tan.")
                return
            }
            val unitPrice = price(recipe, pricing)
            val coinCount = player.inv.count(objTypes[objs.coins])
            val affordable = coinCount / unitPrice
            if (affordable == 0) {
                mes("You don't have enough coins to pay for tanning.")
                return
            }
            val count = min(requested.coerceAtLeast(0), min(hideCount, affordable))
            if (count == 0) {
                return
            }
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.input.id, count)
                    delete(inventory, objs.coins.id, unitPrice * count)
                    add(inventory, recipe.output.id, count)
                }
            check(result.success) { "Tanning transaction failed after validation: $recipe" }
            if (player.members && activeTanner[player] == npcs.werewolf_tanner.id) player.completeDiaryTask(DiaryTask.TanWithSbott)
            mes(if (count == 1) "The tanner tans your hide." else "The tanner tans your hides.")
        }
    }

    public fun price(recipe: TanningRecipe, pricing: TannerPricing, player: Player? = null): Int =
        when (pricing) {
            TannerPricing.Standard -> recipe.standardPrice
            TannerPricing.Premium -> recipe.premiumPrice
            TannerPricing.Eodan -> recipe.eodanPrice * eodanPercent(player) / 100
        }

    private fun eodanPercent(player: Player?): Int =
        when {
            player == null -> 100
            player.vars[varbits.kourend_diary_elite_complete] != 0 -> 20
            player.vars[varbits.kourend_diary_hard_complete] != 0 -> 40
            player.vars[varbits.kourend_diary_medium_complete] != 0 -> 60
            player.vars[varbits.kourend_diary_easy_complete] != 0 -> 80
            else -> 100
        }

    private fun ProtectedAccess.price(recipe: TanningRecipe, pricing: TannerPricing): Int =
        price(recipe, pricing, player)
}
