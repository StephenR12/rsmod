package org.rsmod.content.skills.herblore

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object WesleyNpcs : NpcReferences() { val wesley = find("pestlemortar_crusher") }

public data class WesleyRecipe(val input: ObjType, val output: ObjType, val outputCount: Int = 1)

public object WesleyRecipes {
    val all: List<WesleyRecipe> = listOf(
        WesleyRecipe(objs.guam_leaf, objs.ground_guam),
        WesleyRecipe(objs.unicorn_horn, objs.unicorn_horn_dust),
        WesleyRecipe(objs.chocolate_bar, objs.chocolate_dust),
        WesleyRecipe(objs.blue_dragon_scale, objs.dragon_scale_dust),
        WesleyRecipe(objs.kebbit_teeth, objs.kebbit_teeth_dust),
        WesleyRecipe(objs.gorak_claws, objs.gorak_claw_powder),
        WesleyRecipe(objs.empty_bird_nest, objs.crushed_nest),
        WesleyRecipe(objs.desert_goat_horn, objs.goat_horn_dust),
        WesleyRecipe(objs.charcoal, objs.ground_charcoal),
        WesleyRecipe(objs.rune_shards, objs.rune_dust),
        WesleyRecipe(objs.ashes, objs.ground_ashes),
        WesleyRecipe(objs.raw_karambwan, objs.raw_karambwan_paste),
        WesleyRecipe(objs.poison_karambwan, objs.poison_karambwan_paste),
        WesleyRecipe(objs.cooked_karambwan, objs.cooked_karambwan_paste),
        WesleyRecipe(objs.lava_scale, objs.lava_scale_shard, 3),
        WesleyRecipe(objs.superior_dragon_bones, objs.crushed_superior_dragon_bones),
        WesleyRecipe(objs.nihil_shard, objs.nihil_dust),
    )
}

/** The cutoff Wesley table charges 50 coins per input, including noted bulk ingredients. */
public class WesleyCrushing @Inject constructor(private val objTypes: ObjTypeList) {
    private data class Plan(val input: Int, val output: Int, val count: Int, val outputCount: Int)

    public suspend fun offer(access: ProtectedAccess) {
        with(access) {
            if (!player.members) { mes("Wesley's crushing service is only available on members' worlds."); return }
            val quote = plan(this)
            if (quote.isEmpty()) { mes("You don't have any ingredients that Wesley can crush."); return }
            val cost = quote.sumOf { it.count.toLong() } * COINS_PER_ITEM
            val choice = menu("Crush all suitable ingredients for $cost coins?", "Yes, crush my ingredients.", "No.")
            if (choice != 1) return
            if (plan(this) != quote) { mes("Your ingredients have changed. Please ask Wesley for another quote."); return }
            crush(this)
        }
    }

    public fun crush(access: ProtectedAccess): Int {
        with(access) {
            if (!player.members) return 0
            val plan = plan(this)
            val count = plan.sumOf { it.count.toLong() }
            if (count == 0L) return 0
            val cost = count * COINS_PER_ITEM
            if (cost > player.inv.count(objTypes[objs.coins]).toLong()) {
                mes("You don't have enough coins to pay Wesley.")
                return 0
            }
            if (plan.any { it.outputCount.toLong() * it.count > Int.MAX_VALUE }) return 0
            val result = player.invTransaction(player.inv) {
                val inventory = select(player.inv)
                delete(inventory, objs.coins.id, cost.toInt())
                for (entry in plan) {
                    delete(inventory, entry.input, entry.count)
                    add(inventory, entry.output, entry.count * entry.outputCount)
                }
            }
            if (!result.success) {
                mes("You don't have enough inventory space for the crushed ingredients.")
                return 0
            }
            mes("Wesley crushes your ingredients.")
            return count.toInt()
        }
    }

    private fun plan(access: ProtectedAccess): List<Plan> =
        access.player.inv.filterNotNull().groupBy { it.id }.mapNotNull { (inputId, stacks) ->
            val input = objTypes.getValue(inputId)
            val rawId = if (input.certtemplate > 0) input.certlink else inputId
            val recipe = WesleyRecipes.all.firstOrNull { it.input.id == rawId } ?: return@mapNotNull null
            val output = objTypes[recipe.output]
            val outputId = if (input.certtemplate > 0 && output.certlink > 0 && output.certtemplate == 0) output.certlink else output.id
            val count = stacks.sumOf { it.count.toLong() }
            if (count > Int.MAX_VALUE) return@mapNotNull null
            Plan(inputId, outputId, count.toInt(), recipe.outputCount)
        }

    public companion object { public const val COINS_PER_ITEM: Int = 50 }
}

public class WesleyScript @Inject constructor(private val crushing: WesleyCrushing) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(WesleyNpcs.wesley) { crushing.offer(this) }
    }
}

internal object WesleySpawn : MapNpcSpawnBuilder() {
    override fun onPackMapTask() { resourceFile<WesleyScript>("wesley.toml") }
}
