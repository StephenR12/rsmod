package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.game.type.loc.LocType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class CookingScript
@Inject
constructor(
    private val cooking: StandardCooking,
    private val karambwan: KarambwanCooking,
    private val sinew: SinewCooking,
    private val ironSpit: IronSpitCooking,
    private val chompy: ChompyFireCooking,
    private val ogreSpitRoast: OgreSpitRoast,
    private val foodPreparation: FoodPreparation,
    private val dairyChurning: DairyChurning,
    private val wineMaking: WineMaking,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (range in cooking_locs.standardRangesOp1) {
            registerRange(range, CookingSourceKind.Range, operation = 1)
        }
        for (range in cooking_locs.standardRangesOp2) {
            registerRange(range, CookingSourceKind.Range, operation = 2)
        }
        registerRange(cooking_locs.lumbridgeRange, CookingSourceKind.Lumbridge)
        registerRange(cooking_locs.hosidiusRange, CookingSourceKind.Hosidius)
        registerRange(cooking_locs.hosidiusRange2, CookingSourceKind.Hosidius)
        for (stove in cooking_locs.houseFireStoves + cooking_locs.campfires) registerFoodUse(stove, CookingSourceKind.Fire)
        for (stove in cooking_locs.houseRangeStoves) registerFoodUse(stove, CookingSourceKind.Range)
        registerFoodUse(cooking_locs.fire, CookingSourceKind.Fire)
        registerRange(cooking_locs.permanentCookFire, CookingSourceKind.Fire)
        onOpHeldU(objs.raw_chompy, objs.iron_spit) {
            chompy.skewer(this, it.firstSlot, it.secondSlot)
        }
        for (recipe in IronSpitRecipes.all) {
            onOpHeldU(recipe.raw, objs.iron_spit) {
                ironSpit.skewer(this, recipe, it.firstSlot, it.secondSlot)
            }
        }
        for (water in FoodPreparationRecipes.waterContainers) {
            onOpHeldU(objs.pot_of_flour, water.input) {
                foodPreparation.selectDough(this, it.firstSlot, it.secondSlot)
            }
        }
        for (recipe in FoodPreparationRecipes.heldRecipes) {
            onOpHeldU(recipe.first.obj, recipe.second.obj) {
                if (recipe.batch) {
                    foodPreparation.selectBatch(this, it.firstSlot, it.secondSlot, recipe)
                } else {
                    foodPreparation.start(this, it.firstSlot, it.secondSlot, recipe)
                }
            }
        }
        for (recipe in WineRecipes.all) {
            onOpHeldU(recipe.grapes, objs.jug_water) {
                wineMaking.select(this, it.firstSlot, it.secondSlot, recipe)
            }
        }
        for (churn in cooking_locs.dairyChurns) {
            onOpLoc1(churn) { dairyChurning.select(this, it.loc) }
            for (input in DairyChurningRecipes.inputs) {
                onOpLocU(churn, input) { dairyChurning.select(this, it.loc, input, it.invSlot) }
            }
        }
        for (stove in cooking_locs.houseFireStoves + cooking_locs.campfires) {
            onOpLocU(stove, objs.skewered_chompy) { chompy.start(this, it.loc, it.invSlot) }
            for (recipe in IronSpitRecipes.all) onOpLocU(stove, recipe.skewered) { ironSpit.start(this, recipe, it.loc, it.invSlot) }
        }
        onOpLocU(cooking_locs.fire, objs.skewered_chompy) { chompy.start(this, it.loc, it.invSlot) }
        onOpLocU(cooking_locs.permanentCookFire, objs.skewered_chompy) {
            chompy.start(this, it.loc, it.invSlot)
        }
        for (recipe in IronSpitRecipes.all) {
            onOpLocU(cooking_locs.fire, recipe.skewered) {
                ironSpit.start(this, recipe, it.loc, it.invSlot)
            }
            onOpLocU(cooking_locs.permanentCookFire, recipe.skewered) {
                ironSpit.start(this, recipe, it.loc, it.invSlot)
            }
        }
        onOpLocU(cooking_locs.ogreSpitRoastEmpty, objs.raw_chompy) {
            ogreSpitRoast.start(this, it.loc, it.invSlot)
        }
        onOpLocU(cooking_locs.ogreSpitRoastEmpty, objs.raw_jubbly) {
            ogreSpitRoast.startJubbly(this, it.loc, it.invSlot)
        }
        onOpLocU(cooking_locs.ogreSpitRoastEmpty, objs.raw_chicken) {
            ogreSpitRoast.startChicken(this, it.loc, it.invSlot)
        }
        onPlayerQueueWithArgs<KarambwanCookingRequest>(cooking_queues.karambwan) {
            karambwan.process(this, it.args)
        }
        onPlayerQueueWithArgs<SinewCookingRequest>(cooking_queues.sinew) {
            sinew.process(this, it.args)
        }
        onPlayerQueueWithArgs<IronSpitCookingRequest>(cooking_queues.ironSpit) {
            ironSpit.process(this, it.args)
        }
        onPlayerQueueWithArgs<StandardCookingRequest>(cooking_queues.standardFood) {
            cooking.process(this, it.args)
        }
        onPlayerQueueWithArgs<ChompyFireCookingRequest>(cooking_queues.chompyFire) {
            chompy.process(this, it.args)
        }
        onPlayerQueueWithArgs<OgreSpitRoastRequest>(cooking_queues.ogreSpitRoast) {
            ogreSpitRoast.process(this, it.args)
        }
        onPlayerQueueWithArgs<FoodPreparationRequest>(cooking_queues.foodPreparation) {
            foodPreparation.process(this, it.args)
        }
        onPlayerQueueWithArgs<DairyChurningRequest>(cooking_queues.dairyChurning) {
            dairyChurning.process(this, it.args)
        }
        onPlayerQueueWithArgs<WineMakingRequest>(cooking_queues.wineMaking) {
            wineMaking.process(this, it.args)
        }
        onPlayerTimer(cooking_timers.wineFermentation) { wineMaking.ferment(this) }
        onPlayerSoftTimer(cooking_timers.ogreSpitRoastState) { ogreSpitRoast.tickState(player) }
        onPlayerLogin { ogreSpitRoast.clearState(player) }
        onPlayerLogout { ogreSpitRoast.clearState(player) }
    }

    private fun ScriptContext.registerRange(
        range: LocType,
        kind: CookingSourceKind,
        operation: Int = 1,
    ) {
        when (operation) {
            1 -> onOpLoc1(range) { cooking.select(this, it.loc, kind) }
            2 -> onOpLoc2(range) { cooking.select(this, it.loc, kind) }
            else -> error("Unsupported Cooking source operation: $operation")
        }
        registerFoodUse(range, kind)
    }

    private fun ScriptContext.registerFoodUse(source: LocType, kind: CookingSourceKind) {
        for (recipe in StandardCookingRecipes.all.filter { kind in it.allowedSources }) {
            onOpLocU(source, recipe.raw) { cooking.select(this, it.loc, kind, recipe.raw) }
        }
        onOpLocU(source, objs.raw_karambwan) { karambwan.select(this, it.loc, kind) }
        onOpLocU(source, objs.burnt_karambwan) { karambwan.reheatBurnt(this, kind) }
    }
}
