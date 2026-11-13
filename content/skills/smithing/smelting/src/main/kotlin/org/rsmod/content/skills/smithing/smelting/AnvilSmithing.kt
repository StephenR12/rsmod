package org.rsmod.content.skills.smithing.smelting

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.recordDiaryAnvil
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class AnvilSmithing
@Inject
constructor(
    private val recipes: AnvilRecipes,
    private val objTypes: ObjTypeList,
    private val random: GameRandom,
) {
    private data class Selection(val anvil: BoundLocInfo, val metal: Int, var quantity: Int = 1)

    private val selections = WeakHashMap<Player, Selection>()

    public fun open(access: ProtectedAccess, anvil: BoundLocInfo, requestedBar: ObjType? = null) {
        with(access) {
            if (!canUseAnvil(anvil) || !hasHammer()) return
            val previous = player.vars[AnvilVars.lastBarType] - 1
            val metal =
                requestedBar?.let { bar -> recipes.bars.indexOfFirst { it.id == bar.id } }
                    ?: 0.takeIf { anvil.id == AnvilLocs.rusted.id }
                    ?: previous.takeIf {
                        it in recipes.bars.indices && recipes.bars[it] in player.inv
                    }
                    ?: recipes.bars.indexOfFirst { it in player.inv }
            if (metal !in recipes.bars.indices || recipes.bars[metal] !in player.inv) {
                mes("You need a metal bar to smith at an anvil.")
                return
            }
            if (anvil.id == AnvilLocs.rusted.id && metal != 0) {
                mes("You can only smith bronze items on this rusted anvil.")
                return
            }
            vars[AnvilVars.barType] = metal + 1
            ifOpenMainModal(AnvilInterfaces.smithing)
            selections[player] = Selection(anvil, metal + 1)
            for (component in AnvilComponents.products) {
                ifSetEvents(
                    component,
                    -1..10,
                    IfEvent.Op1,
                    IfEvent.Op2,
                    IfEvent.Op3,
                    IfEvent.Op4,
                    IfEvent.Op5,
                )
            }
            for (component in AnvilComponents.quantities) {
                ifSetEvents(component, -1..10, IfEvent.Op1)
            }
        }
    }

    public suspend fun quantity(access: ProtectedAccess, index: Int) {
        val selection = selections[access.player] ?: return
        selection.quantity =
            when (index) {
                0 -> 1
                1 -> 5
                2 -> 10
                4 -> MAX_QUANTITY
                else ->
                    access
                        .countDialog("How many sets would you like to smith?")
                        .coerceIn(1, MAX_QUANTITY)
            }
    }

    public suspend fun choose(access: ProtectedAccess, button: IfModalButton) {
        val selection = selections[access.player] ?: return
        val recipe = recipes.find(selection.metal, button.component) ?: return
        val quantity =
            when (button.op) {
                IfButtonOp.Op1 -> selection.quantity
                IfButtonOp.Op2 -> 5
                IfButtonOp.Op3 -> 10
                IfButtonOp.Op4 -> access.countDialog("How many sets would you like to smith?")
                IfButtonOp.Op5 -> MAX_QUANTITY
                else -> return
            }
        access.ifClose()
        smith(access, selection.anvil, recipe, quantity)
    }

    public suspend fun smith(
        access: ProtectedAccess,
        anvil: BoundLocInfo,
        recipe: AnvilRecipe,
        quantity: Int,
    ) {
        with(access) {
            if (quantity <= 0) return
            if (!canSmith(anvil, recipe)) return
            val actions =
                minOf(quantity, MAX_QUANTITY, player.inv.count(objTypes[recipe.bar]) / recipe.bars)
            clearFurnaceQueues()
            repeat(actions) {
                if (!canSmith(anvil, recipe)) {
                    resetAnim()
                    return
                }
                faceLoc(anvil)
                val imcando =
                    player.worn.any { it?.id in imcandoIds } ||
                        player.inv.any { it?.id in imcandoIds }
                anim(if (imcando) AnvilSeqs.imcando else AnvilSeqs.smith)
                delay(actionTicks(player))
                if (!canSmith(anvil, recipe)) {
                    resetAnim()
                    return
                }
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, recipe.bar.id, recipe.bars)
                        add(inventory, recipe.output.id, recipe.count)
                    }
                if (!result.success) {
                    mes("You don't have enough inventory space.")
                    resetAnim()
                    return
                }
                vars[AnvilVars.lastBarType] = recipe.metal
                statAdvance(stats.smithing, recipe.xp)
                player.recordDiaryAnvil(recipe.output.id, recipe.count, anvil.coords)
            }
            resetAnim()
        }
    }

    public fun close(player: Player) {
        selections.remove(player)
    }

    private fun ProtectedAccess.canSmith(anvil: BoundLocInfo, recipe: AnvilRecipe): Boolean {
        if (anvil.id == AnvilLocs.rusted.id && recipe.metal != 1) {
            mes("You can only smith bronze items on this rusted anvil.")
            return false
        }
        if (!canUseAnvil(anvil) || !hasHammer() || !isWithinDistance(anvil, distance = 1))
            return false
        if (objTypes[recipe.output].members && !player.members) {
            mes("You need to be on a members' world to smith this item.")
            return false
        }
        if (player.stat(stats.smithing) < recipe.level) {
            mes("You need a Smithing level of ${recipe.level} to smith this item.")
            return false
        }
        if (recipe.output.id in dartTips && player.vars[AnvilQuestVars.touristTrap] < 16) {
            mes("You need to complete The Tourist Trap to smith dart tips.")
            return false
        }
        if (recipe.output.id in claws && player.vars[AnvilQuestVars.deathPlateau] < 80) {
            mes("You need to complete Death Plateau to smith claws.")
            return false
        }
        if (player.inv.count(objTypes[recipe.bar]) < recipe.bars) {
            mes("You don't have enough bars to smith this item.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.canUseAnvil(anvil: BoundLocInfo): Boolean {
        if (
            anvil.coords.level == 0 &&
                anvil.coords.x in 1932..1961 &&
                anvil.coords.z in 4953..4978 &&
                player.statBase(stats.smithing) < 60
        ) {
            mes("You need an unboosted Smithing level of 60 to use the Blast Furnace anvil.")
            return false
        }
        if (anvil.id == AnvilLocs.doric.id && player.vars[AnvilQuestVars.doric] < 100) {
            mes("You need to complete Doric's Quest to use this anvil.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.hasHammer(): Boolean {
        if (
            objs.hammer in player.inv ||
                player.inv.any { it?.id in imcandoIds } ||
                player.worn.any { it?.id in imcandoIds }
        )
            return true
        mes("You need a hammer to smith items.")
        return false
    }

    public fun actionTicks(player: Player): Int {
        val pieces = player.worn.count { it?.id in uniformIds }
        val chance = if (pieces == 4) 100 else pieces * 20
        return if (chance == 100 || (chance > 0 && random.of(100) < chance)) 4 else 5
    }

    public companion object {
        public const val MAX_QUANTITY: Int = 28
        private val imcandoIds = setOf(objs.imcando_hammer.id, objs.imcando_hammer_offhand.id)
        private val uniformIds = setOf(27023, 27025, 27027, 27029, 27031)
        private val dartTips = setOf(819, 820, 821, 822, 823, 824)
        private val claws = setOf(3095, 3096, 3097, 3099, 3100, 3101)
    }
}
