package org.rsmod.content.skills.fletching.antlers

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
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
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object AntlerObjs : ObjReferences() {
    val spike = find("huntingbeast_spike")
    val longSpike = find("huntingbeast_bigspike")
    val bolts = find("huntingbow_bolts")
    val longBolts = find("huntingbow_bigbolts")
    val sunlight = find("sunlight_antler")
    val moonlight = find("moonlight_antler")
    val sunlightBolts = find("sunlight_antelope_bolt")
    val moonlightBolts = find("moonlight_antelope_bolt")
    val crossbow = find("hunting_crossbow")
    val sunlightCrossbow = find("hunting_crossbow_sunlight")
}

public data class AntlerBoltRecipe(
    val antler: ObjType,
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val ticks: Int = 3,
)

public object HuntingBoltSeqs : SeqReferences() {
    val cut = find("human_fletching_huntingbolts")
}

/** Dated Kebbit bolts14613870 and Long kebbit bolts14613869, both post-Varlamore2024. */
public object KebbitBoltRecipes {
    val all =
        listOf(
            AntlerBoltRecipe(AntlerObjs.spike, AntlerObjs.bolts, 32, 5.8, 3),
            AntlerBoltRecipe(AntlerObjs.longSpike, AntlerObjs.longBolts, 42, 7.9, 2),
        )
}

public object AntlerBoltRecipes {
    val all =
        listOf(
            AntlerBoltRecipe(AntlerObjs.sunlight, AntlerObjs.sunlightBolts, 62, 10.0),
            AntlerBoltRecipe(AntlerObjs.moonlight, AntlerObjs.moonlightBolts, 72, 12.1),
        )
}

public class AntlerFletching @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, recipe: AntlerBoltRecipe) {
        with(access) {
            if (!canFletch(recipe.level) || recipe.antler !in player.inv) return
            val maximum = player.inv.count(objTypes[recipe.antler])
            val input =
                skillMulti(
                    title = "How many would you like to make?",
                    choices = listOf(recipe.output),
                    maxQuantity = maximum,
                    initialQuantity = maximum,
                    actionMode = 0,
                )
            if (input.component.packed != components.skillmulti_a.packed) return
            cut(this, recipe, input.subcomponent)
        }
    }

    public suspend fun cut(access: ProtectedAccess, recipe: AntlerBoltRecipe, quantity: Int) {
        with(access) {
            if (quantity <= 0 || !canFletch(recipe.level)) return
            val actions = minOf(quantity, player.inv.count(objTypes[recipe.antler]))
            clearFletchingQueues()
            repeat(actions) {
                if (!canFletch(recipe.level) || recipe.antler !in player.inv) return
                anim(HuntingBoltSeqs.cut)
                gatheringDelay(recipe.ticks)
                if (!canFletch(recipe.level) || recipe.antler !in player.inv) return
                val result =
                    player.invTransaction(player.inv) {
                        val inventory = select(player.inv)
                        delete(inventory, recipe.antler.id, 1)
                        add(inventory, recipe.output.id, BOLTS_PER_ANTLER)
                    }
                if (!result.success) return
                statAdvance(stats.fletching, recipe.xp)
            }
        }
    }

    public fun upgradeCrossbow(access: ProtectedAccess) {
        with(access) {
            if (!canFletch(CROSSBOW_LEVEL)) return
            if (AntlerObjs.crossbow !in player.inv || AntlerObjs.sunlight !in player.inv) return
            val result =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, AntlerObjs.crossbow.id, 1)
                    delete(inventory, AntlerObjs.sunlight.id, 1)
                    add(inventory, AntlerObjs.sunlightCrossbow.id, 1)
                }
            if (result.success) clearFletchingQueues()
        }
    }

    private fun ProtectedAccess.canFletch(level: Int): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to fletch hunting bolts.")
            return false
        }
        if (player.stat(stats.fletching) < level) {
            mes("You need a Fletching level of $level to make this item.")
            return false
        }
        if (objs.chisel !in player.inv) {
            mes("You need a chisel to fletch hunting bolts.")
            return false
        }
        return true
    }

    public companion object {
        public const val CROSSBOW_LEVEL: Int = 74
        public const val BOLTS_PER_ANTLER: Int = 12
        public const val BOLT_TICKS: Int = 3
    }
}

public class AntlerFletchingScript @Inject constructor(private val fletching: AntlerFletching) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in AntlerBoltRecipes.all + KebbitBoltRecipes.all) {
            onOpHeldU(objs.chisel, recipe.antler) { fletching.select(this, recipe) }
        }
        onOpHeldU(AntlerObjs.crossbow, AntlerObjs.sunlight) { fletching.upgradeCrossbow(this) }
    }
}
