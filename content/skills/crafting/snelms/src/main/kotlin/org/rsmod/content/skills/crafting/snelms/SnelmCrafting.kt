package org.rsmod.content.skills.crafting.snelms

import org.rsmod.api.player.diary.recordDiaryCraft
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.skills.crafting.commons.clearCraftingQueues
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public data class SnelmRecipe(val shell: ObjType, val output: ObjType)

public object SnelmRecipes : ObjReferences() {
    val all: List<SnelmRecipe> = listOf(
        SnelmRecipe(find("shellround_swamp"), find("snelm_round_swamp")),
        SnelmRecipe(find("shellround_red+black"), find("snelm_round_red+black")),
        SnelmRecipe(find("shellround_yellow"), find("snelm_round_yellow")),
        SnelmRecipe(find("shellround_blue"), find("snelm_round_blue")),
        SnelmRecipe(find("shellround_orange"), find("snelm_round_orange")),
        SnelmRecipe(find("shellpoint_swamp"), find("snelm_point_swamp")),
        SnelmRecipe(find("shellpoint_red+black"), find("snelm_point_red+black")),
        SnelmRecipe(find("shellpoint_yellow"), find("snelm_point_yellow")),
        SnelmRecipe(find("shellpoint_blue"), find("snelm_point_blue")),
    )
}

public object SnelmSequences : SeqReferences() { val cut = find("human_snailshellcutting") }

public class SnelmCrafting @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, recipe: SnelmRecipe) {
        with(access) {
            if (!canCraft(recipe)) return
            val maximum = player.inv.count(objTypes[recipe.shell])
            val input = skillMulti(title = "What would you like to make?", choices = listOf(recipe.output), maxQuantity = maximum, initialQuantity = maximum, actionMode = 0)
            if (input.component.packed != components.skillmulti_a.packed) return
            craft(this, recipe, input.subcomponent)
        }
    }

    public suspend fun craft(access: ProtectedAccess, recipe: SnelmRecipe, quantity: Int) {
        with(access) {
            if (quantity <= 0 || !canCraft(recipe)) return
            val actions = minOf(quantity, player.inv.count(objTypes[recipe.shell]))
            clearCraftingQueues()
            repeat(actions) {
                if (!canCraft(recipe)) { resetAnim(); return }
                anim(SnelmSequences.cut)
                delay(ACTION_TICKS)
                if (!canCraft(recipe)) { resetAnim(); return }
                val result = player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.shell.id, 1)
                    add(inventory, recipe.output.id, 1)
                }
                if (!result.success) { resetAnim(); return }
                statAdvance(stats.crafting, CRAFTING_XP)
                player.recordDiaryCraft(recipe.output.id, player.coords)
            }
            resetAnim()
        }
    }

    private fun ProtectedAccess.canCraft(recipe: SnelmRecipe): Boolean {
        if (!player.members) { mes("You need to be on a members' world to make snelms."); return false }
        if (player.stat(stats.crafting) < CRAFTING_LEVEL) { mes("You need a Crafting level of $CRAFTING_LEVEL to make snelms."); return false }
        if (objs.chisel !in player.inv) { mes("You need a chisel to make a snelm."); return false }
        return recipe.shell in player.inv
    }

    public companion object {
        public const val CRAFTING_LEVEL: Int = 15
        public const val CRAFTING_XP: Double = 32.5
        public const val ACTION_TICKS: Int = 2
    }
}

public class SnelmCraftingScript @Inject constructor(private val crafting: SnelmCrafting) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in SnelmRecipes.all) {
            onOpHeldU(objs.chisel, recipe.shell) { crafting.select(this, recipe) }
        }
    }
}
