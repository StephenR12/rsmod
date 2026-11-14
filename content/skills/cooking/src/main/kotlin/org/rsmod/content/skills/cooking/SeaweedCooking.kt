package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public object SeaweedInputs : ObjReferences() {
    val giant = find("giant_seaweed")
    val edible = find("edible_seaweed")
    val all: List<ObjType> = listOf(objs.seaweed, giant, edible, objs.swamp_weed)
}

/** Cutoff Soda ash revision 14916754: zero XP, with a six-ash success roll for giant seaweed. */
public class SeaweedCooking @Inject constructor(private val objTypes: ObjTypeList) {
    public suspend fun select(access: ProtectedAccess, source: BoundLocInfo, raw: ObjType) {
        with(access) {
            if (!canCook(source, raw)) return
            val maximum = player.inv.count(objTypes[raw])
            if (maximum == 1) { cook(this, source, raw, 1); return }
            val input = skillMulti(title = "What would you like to cook?", choices = listOf(objs.soda_ash), maxQuantity = maximum, initialQuantity = maximum, actionMode = 1)
            if (input.component.packed == components.skillmulti_a.packed) cook(this, source, raw, input.subcomponent)
        }
    }

    public suspend fun cook(access: ProtectedAccess, source: BoundLocInfo, raw: ObjType, quantity: Int) {
        with(access) {
            if (quantity <= 0 || !canCook(source, raw)) return
            clearCookingQueues()
            val actions = minOf(quantity, player.inv.count(objTypes[raw]))
            repeat(actions) { index ->
                if (!canCook(source, raw)) { resetAnim(); return }
                val fire = source.id == cooking_locs.fire.id || cooking_locs.permanentCookFires.any { it.id == source.id } || (cooking_locs.houseFireStoves + cooking_locs.campfires).any { it.id == source.id }
                anim(if (fire) seqs.human_firecooking else seqs.human_cooking)
                delay(if (index == 0) 1 else if (index == 1) 3 else 4)
                if (!canCook(source, raw)) { resetAnim(); return }
                val count = if (raw.id != SeaweedInputs.giant.id) 1 else if (giantSucceeds(source)) 6 else 1
                val result = player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, raw.id, 1)
                    add(inventory, objs.soda_ash.id, count)
                }
                if (!result.success) { mes("You don't have enough inventory space for the soda ash."); resetAnim(); return }
                mes("You burn the seaweed into soda ash.")
            }
            resetAnim()
        }
    }

    private fun ProtectedAccess.giantSucceeds(source: BoundLocInfo): Boolean {
        if (player.stat(stats.cooking) >= 51) return true
        val hosidius = source.id == cooking_locs.hosidiusRange.id || source.id == cooking_locs.hosidiusRange2.id
        val bonus = if (!hosidius) 0 else if (vars[varbits.kourend_diary_elite_complete] != 0) 25 else 12
        return hasCookingBurnImmunity() || statRandom(stats.cooking, bonus, 500 + bonus, invisibleBoost = 0)
    }

    private fun ProtectedAccess.canCook(source: BoundLocInfo, raw: ObjType): Boolean {
        if (cooking_locs.allFoodUseSources.none { it.id == source.id }) return false
        if (!player.members || SeaweedInputs.all.none { it.id == raw.id } || raw !in player.inv || !isWithinDistance(source, 1)) return false
        if (source.id == cooking_locs.lumbridgeRange.id && vars[varps.cookquest] < 2) { mes("You need to complete Cook's Assistant to use this range."); return false }
        if ((source.id == cooking_locs.hosidiusRange.id || source.id == cooking_locs.hosidiusRange2.id) && vars[varbits.kourend_diary_easy_complete] == 0) { mes("You need to complete the Easy Kourend & Kebos Diary to use this range."); return false }
        return true
    }
}

public class SeaweedCookingScript @Inject constructor(private val cooking: SeaweedCooking) : PluginScript() {
    override fun ScriptContext.startup() {
        for (source in cooking_locs.allFoodUseSources) {
            for (raw in SeaweedInputs.all) onOpLocU(source, raw) { cooking.select(this, it.loc, raw) }
        }
    }
}
