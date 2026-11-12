package org.rsmod.content.skills.firemaking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeld5
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpObj4
import org.rsmod.api.script.onOpObjT
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FiremakingScript
@Inject
constructor(
    private val firemaking: Firemaking,
    private val gnomishFirelighter: GnomishFirelighter,
    private val pyreLogPreparation: PyreLogPreparation,
    private val funeralPyre: FuneralPyre,
    private val barbarianPyreShip: BarbarianPyreShip,
    private val handheldLightSources: HandheldLightSources,
    private val lampOilStill: LampOilStill,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in ColouredLogRecipes.all) {
            onOpHeldU(recipe.firelighter, objs.logs) {
                firemaking.colourLogs(this, it.firstSlot, it.secondSlot, recipe)
            }
            onOpHeldU(recipe.firelighter, objs.gnomish_firelighter) {
                gnomishFirelighter.charge(this, it.firstSlot, it.secondSlot, recipe)
            }
            onOpHeldU(recipe.firelighter, objs.gnomish_firelighter_charged) {
                gnomishFirelighter.charge(this, it.firstSlot, it.secondSlot, recipe)
            }
        }
        onOpHeld4(objs.gnomish_firelighter) { gnomishFirelighter.check(this, it.slot) }
        onOpHeld4(objs.gnomish_firelighter_charged) { gnomishFirelighter.check(this, it.slot) }
        onOpHeld5(objs.gnomish_firelighter_charged) { gnomishFirelighter.uncharge(this, it.slot) }
        onOpHeld1(objs.dry_sticks) { handheldLightSources.rubDrySticks(this, it.slot) }
        onOpHeldU(objs.gnomish_firelighter_charged, objs.logs) {
            firemaking.lightFromGnomishFirelighter(this, it.secondSlot, it.firstSlot)
        }
        for (recipe in FiremakingRecipes.all) {
            onOpHeldU(objs.tinderbox, recipe.log) {
                firemaking.lightFromInventory(this, it.secondSlot, recipe)
            }
            onOpObj4(recipe.log) { firemaking.lightFromGround(this, it.obj, recipe) }
            onOpObjT(recipe.log, components.inv_items) {
                if (it.objType?.id == objs.tinderbox.id) {
                    firemaking.lightFromGround(this, it.obj, recipe)
                } else if (
                    recipe.log == objs.logs && it.objType?.id == objs.gnomish_firelighter_charged.id
                ) {
                    firemaking.lightGroundLogFromGnomishFirelighter(
                        this,
                        groundLog = it.obj,
                        firelighterSlot = it.comsub,
                    )
                }
            }
        }
        for (recipe in FiremakingRecipes.barbarian) {
            for (tool in BowFiremakingTools.all) {
                onOpHeldU(tool.bow, recipe.log) {
                    firemaking.lightFromBow(this, it.secondSlot, it.firstSlot, recipe, tool)
                }
            }
        }
        for (recipe in PyreLogRecipes.all) {
            for (oil in SacredOils.all) {
                onOpHeldU(oil.obj, recipe.input) {
                    pyreLogPreparation.select(this, it.firstSlot, it.secondSlot, recipe)
                }
            }
        }
        onOpLoc1(firemaking_locs.funeralPyre) { funeralPyre.buildBest(this, it.loc) }
        for (log in FuneralPyreLogs.all) {
            onOpLocU(firemaking_locs.funeralPyre, log.obj) {
                funeralPyre.build(this, it.loc, it.invSlot, log)
            }
            onOpLoc1(log.logLoc) { funeralPyre.addBestRemains(this, it.loc, log) }
            for (remains in ShadeRemains.all) {
                onOpLocU(log.logLoc, remains.obj) {
                    funeralPyre.addRemains(this, it.loc, it.invSlot, log, remains)
                }
            }
            onOpLoc1(log.remainsLoc) { funeralPyre.light(this, it.loc, log) }
            onOpLocU(log.remainsLoc, objs.tinderbox) { funeralPyre.light(this, it.loc, log) }
        }
        onOpLoc1(firemaking_locs.barbarianPyreSite) { barbarianPyreShip.construct(this, it.loc) }
        for (recipe in LightSourceRecipes.all) {
            onOpHeldU(objs.tinderbox, recipe.unlit) {
                handheldLightSources.light(
                    this,
                    sourceSlot = it.secondSlot,
                    recipe = recipe,
                    ignitionSlot = it.firstSlot,
                    ignitionId = objs.tinderbox.id,
                )
            }
            when (recipe.extinguishOp) {
                1 ->
                    onOpHeld1(recipe.lit) { handheldLightSources.extinguish(this, it.slot, recipe) }
                2 ->
                    onOpHeld2(recipe.lit) { handheldLightSources.extinguish(this, it.slot, recipe) }
                3 ->
                    onOpHeld3(recipe.lit) { handheldLightSources.extinguish(this, it.slot, recipe) }
                4 ->
                    onOpHeld4(recipe.lit) { handheldLightSources.extinguish(this, it.slot, recipe) }
                else -> error("Unsupported light-source extinguish op: ${recipe.extinguishOp}")
            }
            if (recipe.wearable) {
                onOpWorn2(recipe.lit) { handheldLightSources.extinguishWorn(this, it.slot, recipe) }
            }
        }
        for (recipe in CandleLanternRecipes.all) {
            onOpHeldU(objs.empty_candle_lantern, recipe.candle) {
                handheldLightSources.assembleCandleLantern(
                    this,
                    lanternSlot = it.firstSlot,
                    candleSlot = it.secondSlot,
                    recipe = recipe,
                )
            }
        }
        for (still in listOf(firemaking_locs.lampOilStillEmpty, firemaking_locs.dorgeshLampStill)) {
            onOpLocU(still, objs.swamp_tar) { lampOilStill.addTar(this, it.loc, it.invSlot) }
        }
        for (container in LampOilContainers.all) {
            onOpLocU(firemaking_locs.lampOilStillFull, container.empty) {
                lampOilStill.fill(this, it.loc, it.invSlot, container)
            }
            onOpLocU(firemaking_locs.lampOilStillEmpty, container.empty) {
                lampOilStill.fill(this, it.loc, it.invSlot, container)
            }
            onOpLocU(firemaking_locs.dorgeshLampStill, container.empty) {
                lampOilStill.fill(this, it.loc, it.invSlot, container)
            }
        }
        onPlayerQueueWithArgs<FiremakingRequest>(firemaking_queues.attempt) {
            firemaking.process(this, it.args)
        }
        onPlayerQueueWithArgs<PyreLogPreparationRequest>(firemaking_queues.pyreLogPreparation) {
            pyreLogPreparation.process(this, it.args)
        }
    }
}
