package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.HashedNpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class FarmingScript
@Inject
constructor(
    private val service: FarmingService,
    private val crops: FarmingCropCatalogue,
    private val codec: FarmingPatchCodec,
    private val locTypes: LocTypeList,
    private val npcTypes: NpcTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val patches =
            locTypes.values.filter {
                val type = codec.patchType(it)
                type != null
            }
        for (base in patches) {
            onOpLoc1(base) { operate(it.loc) }
            onOpLoc2(base) { service.inspect(this, it.loc) }
            onOpLoc3(base) { service.inspect(this, it.loc) }
            onOpLoc4(base) { service.inspect(this, it.loc) }
            onOpLoc5(base) { service.inspect(this, it.loc) }
            onOpLocU(base) { useItem(it.loc, it.objType.id, it.invSlot) }
        }
        onOpHeldU(FarmingObjs.haySack, FarmingObjs.bronzeSpear) {
            invDel(inv, FarmingObjs.haySack)
            invDel(inv, FarmingObjs.bronzeSpear)
            invAdd(inv, FarmingObjs.scarecrowTorso)
            mes("You push the bronze spear through the hay sack.")
        }
        onOpHeldU(FarmingObjs.scarecrowTorso, FarmingObjs.watermelon) {
            if (stat(org.rsmod.api.config.refs.stats.farming) < 23) {
                mes("You need a Farming level of 23 to make a scarecrow.")
                return@onOpHeldU
            }
            invDel(inv, FarmingObjs.scarecrowTorso)
            invDel(inv, FarmingObjs.watermelon)
            invAdd(inv, FarmingObjs.scarecrow)
            statAdvance(org.rsmod.api.config.refs.stats.farming, 25.0)
            mes("You add a watermelon head and finish the scarecrow.")
        }
        val gardeners = npcTypes.values.filter { isPatchGardener(it.internalName.orEmpty()) }
        for (gardener in gardeners) {
            val type = HashedNpcType(null, gardener.internalName, gardener.id)
            onOpNpcU(type) {
                if (!service.protectNearest(this, it.npc, it.objType.id, it.invSlot)) {
                    mes("The gardener cannot use that as payment for any nearby crop.")
                }
            }
        }
        onPlayerLogin {
            service.syncNearby(player)
            player.softTimer(FarmingTimers.sync, SYNC_INTERVAL)
        }
        onPlayerSoftTimer(FarmingTimers.sync) {
            service.syncNearby(player)
            player.softTimer(FarmingTimers.sync, SYNC_INTERVAL)
        }
    }

    private suspend fun org.rsmod.api.player.protect.ProtectedAccess.operate(loc: BoundLocInfo) {
        arriveDelay()
        faceLoc(loc)
        val state = service.state(this, loc) ?: return
        when {
            state.patchType == FarmingPatchType.Compost -> service.operateCompostBin(this, loc)
            state.isEmpty && state.weeds > 0 -> {
                if (FarmingObjs.rake !in inv) {
                    mes("You need a rake to weed this patch.")
                    return
                }
                anim(FarmingSeqs.rake)
                service.rake(this, loc)
            }
            state.scarecrow -> {
                if (service.removeScarecrow(this, loc)) mes("You remove the scarecrow.")
            }
            state.status == FarmingCropStatus.Diseased -> {
                if (FarmingService.requiresSecateurs(state.patchType)) {
                    if (FarmingObjs.secateurs !in inv && FarmingObjs.magicSecateurs !in inv) {
                        mes("You need secateurs to prune this diseased plant.")
                        return
                    }
                    anim(FarmingSeqs.harvest)
                    if (service.prune(this, loc)) {
                        mes("You successfully prune away the diseased leaves.")
                    } else {
                        mes("You fail to cut away all of the diseased leaves.")
                    }
                    return
                }
                if (FarmingObjs.plantCure !in inv) {
                    mes("You need plant cure to treat this crop.")
                    return
                }
                invDel(inv, FarmingObjs.plantCure)
                anim(FarmingSeqs.cure)
                service.cure(this, loc)
                mes("You cure the diseased plant.")
            }
            state.status == FarmingCropStatus.Dead -> {
                if (FarmingObjs.spade !in inv) {
                    mes("You need a spade to clear this patch.")
                    return
                }
                anim(FarmingSeqs.harvest)
                service.clearDead(this, loc)
                mes("You clear the dead crop from the patch.")
            }
            state.status == FarmingCropStatus.Stump -> {
                if (FarmingObjs.spade !in inv) {
                    mes("You need a spade to dig up the tree roots.")
                    return
                }
                anim(FarmingSeqs.harvest)
                if (service.clearStump(this, loc)) mes("You dig up the tree roots.")
            }
            state.status == FarmingCropStatus.Harvestable -> {
                val checked = service.checkHealth(this, state)
                if (checked != null) {
                    mes("You check the health of the ${checked.displayName.lowercase()}.")
                    service.sync(this, loc, state)
                    return
                }
                val tree = service.crop(state)
                if (tree?.logs != null && tree.baseHarvestLives == 0) {
                    val chopped = service.chopTree(this, loc) ?: return
                    if (service.state(this, loc)?.status != FarmingCropStatus.Stump) {
                        opLoc3(loc)
                    }
                    return
                }
                anim(FarmingSeqs.harvest)
                val crop = service.harvest(this, loc) ?: return
                spam("You harvest some ${crop.displayName.lowercase()}.")
            }
            else -> service.inspect(this, loc)
        }
    }

    private suspend fun org.rsmod.api.player.protect.ProtectedAccess.useItem(
        loc: BoundLocInfo,
        objId: Int,
        slot: Int,
    ) {
        arriveDelay()
        faceLoc(loc)
        val state = service.state(this, loc) ?: return
        if (state.patchType == FarmingPatchType.Compost) {
            if (!service.addToCompostBin(this, loc, objId, slot)) {
                mes("That cannot be used to make compost.")
            }
            return
        }
        val crop = crops.forSeed(objId)
        when {
            crop != null -> {
                val planted =
                    service.plant(this, loc, objId, slot)
                        ?: run {
                            if (!state.isEmpty) mes("Something is already growing in this patch.")
                            else if (state.weeds > 0) mes("You need to weed the patch first.")
                            return
                        }
                anim(FarmingSeqs.plant)
                mes(
                    "You plant the ${planted.displayName.lowercase()} seed${if (planted.seedCount == 1) "" else "s"}."
                )
            }
            objId == FarmingObjs.compost.id -> applyCompost(loc, FarmingCompost.Compost, slot)
            objId == FarmingObjs.supercompost.id ->
                applyCompost(loc, FarmingCompost.Supercompost, slot)
            objId == FarmingObjs.ultracompost.id ->
                applyCompost(loc, FarmingCompost.Ultracompost, slot)
            objId in FarmingObjs.wateringCans.drop(1).map { it.id } -> water(loc, objId, slot)
            (objId == FarmingObjs.secateurs.id || objId == FarmingObjs.magicSecateurs.id) &&
                state.status == FarmingCropStatus.Diseased &&
                FarmingService.requiresSecateurs(state.patchType) -> {
                anim(FarmingSeqs.harvest)
                if (service.prune(this, loc)) {
                    mes("You successfully prune away the diseased leaves.")
                } else {
                    mes("You fail to cut away all of the diseased leaves.")
                }
            }
            objId == FarmingObjs.plantCure.id &&
                state.status == FarmingCropStatus.Diseased &&
                !FarmingService.requiresSecateurs(state.patchType) -> {
                invDel(inv, FarmingObjs.plantCure, slot = slot)
                anim(FarmingSeqs.cure)
                service.cure(this, loc)
                mes("You cure the diseased plant.")
            }
            objId == FarmingObjs.rake.id && state.isEmpty && state.weeds > 0 -> {
                anim(FarmingSeqs.rake)
                service.rake(this, loc)
            }
            objId == FarmingObjs.spade.id && state.status == FarmingCropStatus.Dead -> {
                anim(FarmingSeqs.harvest)
                service.clearDead(this, loc)
            }
            objId == FarmingObjs.spade.id && state.status == FarmingCropStatus.Stump -> {
                anim(FarmingSeqs.harvest)
                if (service.clearStump(this, loc)) mes("You dig up the tree roots.")
            }
            objId == FarmingObjs.spade.id &&
                state.status == FarmingCropStatus.Harvestable &&
                state.patchType in
                    setOf(
                        FarmingPatchType.Calquat,
                        FarmingPatchType.SpiritTree,
                        FarmingPatchType.CrystalTree,
                    ) -> {
                anim(FarmingSeqs.harvest)
                if (service.clearMatureSpecialTree(this, loc)) {
                    mes("You clear the tree from the patch.")
                }
            }
            objId == FarmingObjs.scarecrow.id && state.patchType == FarmingPatchType.Flower -> {
                if (service.placeScarecrow(this, loc, slot)) {
                    anim(FarmingSeqs.plant)
                    mes("You place the scarecrow in the flower patch.")
                }
            }
            objId == FarmingObjs.spade.id && state.scarecrow -> {
                if (service.removeScarecrow(this, loc)) mes("You remove the scarecrow.")
            }
            service.protect(this, loc, objId, slot) -> Unit
            else -> mes("Nothing interesting happens.")
        }
    }

    private fun org.rsmod.api.player.protect.ProtectedAccess.water(
        loc: BoundLocInfo,
        objId: Int,
        slot: Int,
    ) {
        val charge = FarmingObjs.wateringCans.indexOfFirst { it.id == objId }
        if (charge <= 0 || !service.water(this, loc)) {
            mes("This patch does not need watering.")
            return
        }
        invDel(inv, FarmingObjs.wateringCans[charge], slot = slot)
        invAdd(inv, FarmingObjs.wateringCans[charge - 1])
        anim(FarmingSeqs.water)
        mes("You water the growing plants.")
    }

    private fun org.rsmod.api.player.protect.ProtectedAccess.applyCompost(
        loc: BoundLocInfo,
        compost: FarmingCompost,
        slot: Int,
    ) {
        if (!service.fertilise(this, loc, compost)) return
        val source =
            when (compost) {
                FarmingCompost.Compost -> FarmingObjs.compost
                FarmingCompost.Supercompost -> FarmingObjs.supercompost
                FarmingCompost.Ultracompost -> FarmingObjs.ultracompost
                FarmingCompost.None -> return
            }
        invDel(inv, source, slot = slot)
        invAdd(inv, FarmingObjs.bucket)
        statAdvance(org.rsmod.api.config.refs.stats.farming, 18.0)
        mes("You treat the patch with ${compost.name.lowercase()}.")
    }

    private companion object {
        const val SYNC_INTERVAL = 5

        fun isPatchGardener(name: String): Boolean =
            name.startsWith("farming_gardener") ||
                name.startsWith("fossil_squirrel_gardener") ||
                name in
                    setOf(
                        "hosidius_allotment_gardener",
                        "prif_gardener",
                        "fortis_gardener",
                        "farmer_farming",
                        "farmer_farming_on_49_51",
                        "farmer_farming_on_48_51",
                        "elstan",
                        "dantaera",
                        "kragen",
                        "lyra",
                        "francis",
                        "garth",
                    )
    }
}
