package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.api.script.advanced.onDropHeld
import org.rsmod.api.script.advanced.onOpBankside
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpWorn1
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class RunecraftScript
@Inject
constructor(
    private val combinationCrafting: CombinationRuneCrafting,
    private val standardCrafting: StandardRuneCrafting,
    private val bloodEssence: BloodEssenceLifecycle,
    private val essencePouches: EssencePouchStorage,
    private val kourendCrafting: KourendRuneCrafting,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in CombinationRuneRecipes.all) {
            onOpLocU(recipe.altar, recipe.inputRune) {
                combinationCrafting.craft(this, recipe, it.invSlot)
            }
            if (recipe.talisman != null) {
                onOpLocU(recipe.altar, recipe.talisman) {
                    combinationCrafting.craft(this, recipe, it.invSlot)
                }
            }
        }
        for (recipe in StandardRuneRecipes.all) {
            onOpLoc1(recipe.altar) { standardCrafting.craft(this, recipe) }
        }
        for (recipe in KourendRuneRecipes.all) {
            onOpLoc1(recipe.altar) { kourendCrafting.craft(this, recipe) }
        }
        onOpLoc1(RunecraftLocs.darkAltar) { kourendCrafting.startVenerate(this) }
        onOpHeldU(objs.chisel, RunecraftObjs.darkEssenceBlock) {
            kourendCrafting.chisel(this, it.secondSlot)
        }
        onPlayerQueue(RunecraftQueues.darkAltarVenerate) { kourendCrafting.finishVenerate(this) }
        onOpHeld1(RunecraftObjs.bloodEssenceInactive) { bloodEssence.activate(this, it.slot) }
        onOpHeld1(RunecraftObjs.bloodEssenceActive) {
            bloodEssence.check(this, it.inventory[it.slot])
        }
        onOpHeld3(objs.binding_necklace) { combinationCrafting.checkNecklace(this) }
        for (pouch in EssencePouchDefinitions.all) {
            for (variant in pouch.variants) {
                onOpHeld1(variant) { essencePouches.fill(this, pouch, it.slot) }
                onOpHeld2(variant) { essencePouches.empty(this, pouch) }
                onOpHeld3(variant) { essencePouches.check(this, pouch) }
                onOpBankside(variant) { essencePouches.bankside(player, pouch, slot) }
            }
        }
        onDropHeld {
            val pouch = EssencePouchDefinitions.find(type.id) ?: return@onDropHeld
            essencePouches.clear(player, pouch)
        }
        onOpWorn1(objs.binding_necklace) { combinationCrafting.checkNecklace(this) }
        onDestroyHeld {
            if (type.id == objs.binding_necklace.id) {
                combinationCrafting.resetNecklaceCharges(player)
            }
        }
    }
}
