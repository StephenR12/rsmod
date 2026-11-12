package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.back
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.zone.ZoneKey

@Singleton
public class FarmingService
@Inject
constructor(
    private val repository: FarmingPatchRepository,
    private val catalogue: FarmingCropCatalogue,
    private val codec: FarmingPatchCodec,
    private val locTypes: LocTypeList,
    private val locs: LocZoneStorage,
    private val random: GameRandom,
    private val objTypes: ObjTypeList,
    private val enumTypes: EnumTypeList,
) {
    public fun syncNearby(player: org.rsmod.game.entity.Player) {
        val centre = ZoneKey.from(player.coords)
        for (zoneX in nearbyZones(centre.x)) {
            for (zoneZ in nearbyZones(centre.z)) {
                val zone = ZoneKey(zoneX, zoneZ, centre.level)
                for (loc in locs.mapLocs.findAll(zone)) {
                    val base = locTypes[loc.id] ?: continue
                    val patchType = codec.patchType(base) ?: continue
                    val patch = repository.getOrCreate(player, codec.patchKey(loc, base), patchType)
                    repository.advance(player, patch)
                    codec.sync(player, base, patch, crop(patch))
                }
            }
        }
    }

    public fun state(access: ProtectedAccess, loc: BoundLocInfo): FarmingPatchState? {
        val base = locTypes[loc.id] ?: return null
        val patchType = codec.patchType(base) ?: return null
        val state = repository.getOrCreate(access.player, codec.patchKey(loc, base), patchType)
        repository.advance(access.player, state)
        codec.sync(access.player, base, state, state.cropKey?.let(catalogue::get))
        return state
    }

    public fun crop(state: FarmingPatchState): FarmingCrop? = state.cropKey?.let(catalogue::get)

    public fun fertilise(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        compost: FarmingCompost,
    ): Boolean {
        val state = state(access, loc) ?: return false
        if (state.compost != FarmingCompost.None) {
            access.mes("This patch has already been treated with compost.")
            return false
        }
        state.compost = compost
        sync(access, loc, state)
        return true
    }

    public fun cure(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (state.status != FarmingCropStatus.Diseased) return false
        state.status = FarmingCropStatus.Growing
        val crop = requireNotNull(crop(state))
        state.nextGrowthAt =
            repository.firstGrowthTick(
                access.player,
                crop.growthMinutes,
                System.currentTimeMillis(),
            )
        sync(access, loc, state)
        return true
    }

    /** Trees and bushes have the cache-authentic 75% secateurs cure chance. */
    public fun prune(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (state.status != FarmingCropStatus.Diseased || !requiresSecateurs(state.patchType)) {
            return false
        }
        if (random.of(4) == 0) return false
        return cure(access, loc)
    }

    public fun water(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (
            state.isEmpty ||
                state.status != FarmingCropStatus.Growing ||
                state.patchType !in WATERABLE_PATCHES ||
                state.watered
        ) {
            return false
        }
        state.watered = true
        sync(access, loc, state)
        return true
    }

    public fun resurrect(access: ProtectedAccess, loc: BoundLocInfo, success: Boolean): Boolean {
        val state = state(access, loc) ?: return false
        if (state.status != FarmingCropStatus.Dead || state.resurrectionAttempted) return false
        state.resurrectionAttempted = true
        if (success) {
            state.status = FarmingCropStatus.Growing
            val crop = requireNotNull(crop(state))
            state.nextGrowthAt =
                repository.firstGrowthTick(
                    access.player,
                    crop.growthMinutes,
                    System.currentTimeMillis(),
                )
        } else {
            clear(state)
        }
        sync(access, loc, state)
        return true
    }

    public fun rake(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (!state.isEmpty || state.weeds <= 0) return false
        state.weeds--
        access.invAdd(access.inv, FarmingObjs.weeds)
        access.statAdvance(stats.farming, 4.0)
        sync(access, loc, state)
        return true
    }

    public fun plant(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        seedId: Int,
        slot: Int,
    ): FarmingCrop? {
        val state = state(access, loc) ?: return null
        val crop = catalogue.forSeed(seedId) ?: return null
        if (crop.patchType != state.patchType || !state.isEmpty || state.weeds != 0) return null
        if (access.stat(stats.farming) < crop.level) {
            access.mes(
                "You need a Farming level of ${crop.level} to grow ${crop.displayName.lowercase()}."
            )
            return null
        }
        if (crop.key == "spirit") {
            val level = access.stat(stats.farming)
            val limit =
                when {
                    level >= 99 -> Int.MAX_VALUE
                    level >= 96 -> 4
                    level >= 93 -> 3
                    level >= 88 -> 2
                    else -> 1
                }
            val planted = repository.snapshot(access.player).count { it.cropKey == "spirit" }
            if (planted >= limit) {
                access.mes("You cannot grow any more spirit trees at your current Farming level.")
                return null
            }
        }
        val treeCrop = crop.patchType in TREE_PATCHES
        val tool = if (treeCrop) FarmingObjs.spade else FarmingObjs.dibber
        if (tool !in access.inv) {
            access.mes(
                if (treeCrop) "You need a spade to plant that sapling."
                else "You need a seed dibber to plant that seed."
            )
            return null
        }
        val removed = access.invDel(access.inv, crop.seed, crop.seedCount, slot = slot)
        if (removed.failure) return null
        if (treeCrop && crop.key !in setOf("redwood", "crystal")) {
            access.invAdd(access.inv, FarmingObjs.plantPot)
        }
        state.cropKey = crop.key
        state.stage = 0
        state.status = FarmingCropStatus.Growing
        state.harvestLives = 0
        state.watered = false
        state.resurrectionAttempted = false
        state.healthChecked = false
        state.protected = false
        state.nextGrowthAt =
            repository.firstGrowthTick(
                access.player,
                crop.growthMinutes,
                System.currentTimeMillis(),
            )
        access.statAdvance(stats.farming, crop.plantXp)
        sync(access, loc, state)
        return crop
    }

    public fun harvest(access: ProtectedAccess, loc: BoundLocInfo): FarmingCrop? {
        val state = state(access, loc) ?: return null
        val crop = crop(state) ?: return null
        if (state.status != FarmingCropStatus.Harvestable || crop.produce == null) return null
        if (access.inv.isFull()) {
            access.mes("You don't have enough inventory space.")
            return null
        }
        access.invAdd(access.inv, crop.produce)
        access.statAdvance(stats.farming, crop.harvestXp)
        val low = crop.harvestSaveLow
        val high = crop.harvestSaveHigh
        val hasMagicSecateurs =
            crop.key != "seaweed" &&
                (access.inv.contains(FarmingObjs.magicSecateurs) ||
                    access.player.worn.contains(FarmingObjs.magicSecateurs))
        val hasFarmingCape =
            state.patchType == FarmingPatchType.Herb &&
                (access.player.back?.id == objs.farming_skillcape.id ||
                    access.player.back?.id == objs.farming_skillcape_t.id ||
                    access.player.back?.id == objs.max_cape_worn.id)
        val itemBonus = (if (hasMagicSecateurs) 10 else 0) + (if (hasFarmingCape) 5 else 0)
        val saved =
            if (low != null && high != null) {
                val level = access.stat(stats.farming).coerceIn(1, 99)
                random.of(256) < FarmingRules.harvestSaveChance(level, low, high, itemBonus)
            } else {
                false
            }
        if (!saved) {
            state.harvestLives--
        }
        if (state.harvestLives <= 0) {
            if (crop.regrowMinutes > 0) {
                state.nextGrowthAt =
                    repository.firstGrowthTick(
                        access.player,
                        crop.regrowMinutes,
                        System.currentTimeMillis(),
                    )
            } else {
                clear(state)
            }
        }
        sync(access, loc, state)
        return crop
    }

    public fun checkHealth(access: ProtectedAccess, state: FarmingPatchState): FarmingCrop? {
        val crop = crop(state) ?: return null
        if (state.status != FarmingCropStatus.Harvestable || state.healthChecked) return null
        if (crop.healthXp <= 0.0) return null
        access.statAdvance(stats.farming, crop.healthXp)
        state.healthChecked = true
        return crop
    }

    public fun clearDead(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (state.status != FarmingCropStatus.Dead) return false
        clear(state)
        sync(access, loc, state)
        return true
    }

    public fun inspect(access: ProtectedAccess, loc: BoundLocInfo) {
        val state = state(access, loc) ?: return
        val crop = crop(state)
        when {
            state.isEmpty && state.weeds > 0 -> access.mes("This patch needs weeding.")
            state.isEmpty -> access.mes("This patch is empty and ready for planting.")
            state.status == FarmingCropStatus.Diseased ->
                access.mes("The ${crop?.displayName?.lowercase()} is diseased.")
            state.status == FarmingCropStatus.Dead ->
                access.mes("The ${crop?.displayName?.lowercase()} has died.")
            state.status == FarmingCropStatus.Harvestable ->
                access.mes("The ${crop?.displayName?.lowercase()} is ready to harvest.")
            state.status == FarmingCropStatus.Stump -> access.mes("The tree has been cut down.")
            else -> access.mes("The ${crop?.displayName?.lowercase()} is growing well.")
        }
    }

    public fun sync(access: ProtectedAccess, loc: BoundLocInfo, state: FarmingPatchState) {
        val base = locTypes[loc.id] ?: return
        codec.sync(access.player, base, state, crop(state))
    }

    private fun clear(state: FarmingPatchState) {
        state.weeds = 0
        state.cropKey = null
        state.stage = 0
        state.status = FarmingCropStatus.Growing
        state.harvestLives = 0
        state.nextGrowthAt = 0L
        state.watered = false
        state.resurrectionAttempted = false
        state.healthChecked = false
        state.protected = false
        state.scarecrow = false
    }

    public fun protect(access: ProtectedAccess, loc: BoundLocInfo, objId: Int, slot: Int): Boolean {
        val state = state(access, loc) ?: return false
        val crop = crop(state) ?: return false
        val payment = crop.protection ?: return false
        if (state.protected || payment.item.id != objId) return false
        val removed =
            if (crop.key == "spirit") {
                access.invDel(
                    access.inv,
                    FarmingObjs.monkeyNuts,
                    5,
                    FarmingObjs.monkeyBar,
                    1,
                    FarmingObjs.groundTooth,
                    1,
                )
            } else {
                access.invDel(access.inv, payment.item, payment.quantity, slot = slot)
            }
        if (removed.failure) {
            access.mes("You do not have the gardener's full payment with you.")
            return false
        }
        state.protected = true
        sync(access, loc, state)
        access.mes("The nearby gardener agrees to watch over your ${crop.displayName.lowercase()}.")
        return true
    }

    public fun protectNearest(
        access: ProtectedAccess,
        gardener: Npc,
        objId: Int,
        slot: Int,
    ): Boolean {
        val usedType = objTypes[objId] ?: return false
        val paymentId = objTypes.uncert(usedType).id
        val centre = ZoneKey.from(gardener.coords)
        val candidates = buildList {
            for (zoneX in nearbyZones(centre.x)) {
                for (zoneZ in nearbyZones(centre.z)) {
                    for (loc in locs.mapLocs.findAll(ZoneKey(zoneX, zoneZ, centre.level))) {
                        if (loc.coords.chebyshevDistance(gardener.coords) > 20) continue
                        val base = locTypes[loc.id] ?: continue
                        val type = codec.patchType(base) ?: continue
                        if (type == FarmingPatchType.Compost) continue
                        val patch =
                            repository.getOrCreate(access.player, codec.patchKey(loc, base), type)
                        repository.advance(access.player, patch)
                        val crop = crop(patch) ?: continue
                        val payment = crop.protection ?: continue
                        if (
                            !patch.protected &&
                                payment.item.id == paymentId &&
                                patch.status in
                                    setOf(FarmingCropStatus.Growing, FarmingCropStatus.Diseased)
                        ) {
                            add(Triple(loc, base, patch))
                        }
                    }
                }
            }
        }
        val chosen =
            candidates.minByOrNull { it.first.coords.chebyshevDistance(gardener.coords) }
                ?: return false
        val crop = requireNotNull(crop(chosen.third))
        val payment = requireNotNull(crop.protection)
        val removed =
            if (crop.key == "spirit" && objId == FarmingObjs.monkeyNuts.id) {
                access.invDel(
                    access.inv,
                    FarmingObjs.monkeyNuts,
                    5,
                    FarmingObjs.monkeyBar,
                    1,
                    FarmingObjs.groundTooth,
                    1,
                )
            } else {
                access.player.invDel(access.inv, objId, payment.quantity, slot = slot)
            }
        if (removed.failure) {
            access.mes("You do not have the gardener's full payment with you.")
            return true
        }
        chosen.third.protected = true
        codec.sync(access.player, chosen.second, chosen.third, crop)
        access.mes("The gardener agrees to watch over your ${crop.displayName.lowercase()}.")
        return true
    }

    private fun nearbyZones(centre: Int): IntRange =
        (centre - NEARBY_ZONE_RADIUS).coerceAtLeast(0)..(centre + NEARBY_ZONE_RADIUS).coerceAtMost(
                ZoneKey.X_BIT_MASK
            )

    public fun placeScarecrow(access: ProtectedAccess, loc: BoundLocInfo, slot: Int): Boolean {
        val state = state(access, loc) ?: return false
        if (state.patchType != FarmingPatchType.Flower || !state.isEmpty || state.scarecrow)
            return false
        if (access.stat(stats.farming) < 23) {
            access.mes("You need a Farming level of 23 to place a scarecrow.")
            return false
        }
        if (access.invDel(access.inv, FarmingObjs.scarecrow, slot = slot).failure) return false
        state.scarecrow = true
        state.nextGrowthAt =
            repository.firstGrowthTick(access.player, 5, System.currentTimeMillis())
        sync(access, loc, state)
        return true
    }

    public fun removeScarecrow(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (!state.scarecrow || access.inv.isFull()) return false
        state.scarecrow = false
        state.nextGrowthAt = 0L
        access.invAdd(access.inv, FarmingObjs.scarecrow)
        sync(access, loc, state)
        return true
    }

    public fun chopTree(access: ProtectedAccess, loc: BoundLocInfo): FarmingCrop? {
        val state = state(access, loc) ?: return null
        val crop = crop(state) ?: return null
        if (
            state.status != FarmingCropStatus.Harvestable ||
                !state.healthChecked ||
                crop.logs == null ||
                crop.baseHarvestLives > 0
        )
            return null
        if (access.stat(stats.woodcutting) < crop.woodcuttingLevel) {
            access.mes(
                "You need a Woodcutting level of ${crop.woodcuttingLevel} to chop this tree."
            )
            return null
        }
        if (access.inv.isFull()) {
            access.mes("Your inventory is too full to hold any more logs.")
            return null
        }
        val axe = Woodcutting.findAxe(access.player, objTypes)
        if (axe == null) {
            access.mes("You need an axe that you have the Woodcutting level to use.")
            return null
        }
        val base = locTypes[loc.id] ?: return null
        val visual = codec.visualType(base, state, crop) ?: return null
        if (!visual.hasParam(WoodcuttingParams.success_rates)) {
            access.mes("This tree has no cache-defined Woodcutting rate.")
            return null
        }
        val animation = with(Woodcutting) { objTypes[axe].axeWoodcuttingAnim }
        access.anim(animation)
        access.spam("You swing your axe at the tree.")
        val (low, high) = Woodcutting.cutSuccessRates(visual, axe, enumTypes)
        if (!access.statRandom(stats.woodcutting, low, high, invisibleBoost = 0)) return crop
        access.invAdd(access.inv, crop.logs)
        access.statAdvance(stats.woodcutting, crop.woodcuttingXp)
        access.spam("You get some logs.")
        val depleteChance = with(Woodcutting) { visual.treeDepleteChance }
        if (random.of(1, 255) > depleteChance) {
            state.status = FarmingCropStatus.Stump
            val fixed = with(Woodcutting) { visual.treeRespawnTime }
            val respawnTicks =
                if (fixed > 0) fixed
                else {
                    val lowTicks = with(Woodcutting) { visual.treeRespawnTimeLow }
                    val highTicks = with(Woodcutting) { visual.treeRespawnTimeHigh }
                    random.of(lowTicks, highTicks)
                }
            state.nextGrowthAt = System.currentTimeMillis() + respawnTicks.coerceAtLeast(1) * 600L
        }
        sync(access, loc, state)
        return crop
    }

    public fun clearStump(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        val crop = crop(state) ?: return false
        if (state.status != FarmingCropStatus.Stump) return false
        if (crop.roots != null && access.inv.isFull()) {
            access.mes("You need inventory space for the roots.")
            return false
        }
        crop.roots?.let { access.invAdd(access.inv, it) }
        clear(state)
        sync(access, loc, state)
        return true
    }

    public fun clearMatureSpecialTree(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        val crop = crop(state) ?: return false
        if (
            state.status != FarmingCropStatus.Harvestable ||
                crop.patchType !in
                    setOf(
                        FarmingPatchType.Calquat,
                        FarmingPatchType.SpiritTree,
                        FarmingPatchType.CrystalTree,
                    )
        ) {
            return false
        }
        clear(state)
        sync(access, loc, state)
        return true
    }

    public fun compostCapacity(loc: BoundLocInfo): Int {
        val base = locTypes[loc.id] ?: return 15
        return if (base.internalName.orEmpty().contains("bigbin")) 30 else 15
    }

    public fun addToCompostBin(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        objId: Int,
        slot: Int,
    ): Boolean {
        val state = state(access, loc) ?: return false
        if (
            state.patchType == FarmingPatchType.Compost &&
                state.compostBinStatus == CompostBinStatus.Ready
        ) {
            if (
                state.compostBinKind == CompostBinKind.Supercompost &&
                    objId == FarmingObjs.volcanicAsh.id
            ) {
                val amount = if (compostCapacity(loc) == 30) 50 else 25
                if (
                    access.invDel(access.inv, FarmingObjs.volcanicAsh, amount, slot = slot).failure
                ) {
                    access.mes("You need $amount volcanic ash to upgrade this bin.")
                    return true
                }
                state.compostBinKind = CompostBinKind.Ultracompost
                sync(access, loc, state)
                access.mes("You mix the volcanic ash into the bin, making ultracompost.")
                return true
            }
            val dose = FarmingObjs.compostPotions.indexOfFirst { it.id == objId }
            if (state.compostBinKind == CompostBinKind.Compost && dose >= 0) {
                if (
                    access.invDel(access.inv, FarmingObjs.compostPotions[dose], slot = slot).failure
                )
                    return false
                access.invAdd(
                    access.inv,
                    if (dose == 0) FarmingObjs.vial else FarmingObjs.compostPotions[dose - 1],
                )
                state.compostBinKind = CompostBinKind.Supercompost
                sync(access, loc, state)
                access.mes("You treat the entire bin with a dose of compost potion.")
                return true
            }
        }
        if (
            state.patchType != FarmingPatchType.Compost ||
                state.compostBinStatus != CompostBinStatus.Open
        )
            return false
        val material = compostMaterial(objId) ?: return false
        val capacity = compostCapacity(loc)
        if (state.compostBinCount >= capacity) {
            access.mes("The compost bin is full. Close the lid to begin rotting it down.")
            return false
        }
        if (state.compostBinCount > 0 && material != state.compostBinKind) {
            access.mes("You should not mix different grades of compostable material.")
            return false
        }
        val source = COMPOST_SOURCES.firstOrNull { it.id == objId } ?: return false
        if (access.invDel(access.inv, source, slot = slot).failure) return false
        state.compostBinKind = material
        state.compostBinCount++
        sync(access, loc, state)
        return true
    }

    public fun operateCompostBin(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (state.patchType != FarmingPatchType.Compost) return false
        repository.advance(access.player, state)
        val capacity = compostCapacity(loc)
        when (state.compostBinStatus) {
            CompostBinStatus.Open -> {
                if (state.compostBinCount < capacity) {
                    access.mes(
                        "The compost bin needs ${capacity - state.compostBinCount} more items."
                    )
                    return true
                }
                state.compostBinStatus = CompostBinStatus.Rotting
                state.compostReadyAt = System.currentTimeMillis() + 60L * 60_000L
                access.mes("You close the lid and leave the contents to rot.")
            }
            CompostBinStatus.Rotting -> access.mes("The contents are still rotting.")
            CompostBinStatus.Ready -> {
                if (FarmingObjs.bucket !in access.inv) {
                    access.mes("You need an empty bucket to collect the compost.")
                    return true
                }
                val output =
                    when (state.compostBinKind) {
                        CompostBinKind.Compost -> FarmingObjs.compost
                        CompostBinKind.Supercompost -> FarmingObjs.supercompost
                        CompostBinKind.Ultracompost -> FarmingObjs.ultracompost
                        CompostBinKind.RottenTomatoes -> FarmingObjs.rottenTomato
                        CompostBinKind.Empty -> return true
                    }
                access.invDel(access.inv, FarmingObjs.bucket)
                access.invAdd(access.inv, output)
                access.statAdvance(stats.farming, 4.5)
                state.compostBinCount--
                if (state.compostBinCount <= 0) {
                    state.compostBinCount = 0
                    state.compostBinKind = CompostBinKind.Empty
                    state.compostBinStatus = CompostBinStatus.Open
                }
            }
        }
        sync(access, loc, state)
        return true
    }

    private fun compostMaterial(objId: Int): CompostBinKind? {
        if (objId == FarmingObjs.tomato.id) return CompostBinKind.RottenTomatoes
        if (objId in SUPERCOMPOSTABLE) return CompostBinKind.Supercompost
        if (objId in COMPOSTABLE) return CompostBinKind.Compost
        return null
    }

    public companion object {
        private const val NEARBY_ZONE_RADIUS = 2
        public val WATERABLE_PATCHES: Set<FarmingPatchType> =
            setOf(FarmingPatchType.Allotment, FarmingPatchType.Flower, FarmingPatchType.Hops)

        public val TREE_PATCHES: Set<FarmingPatchType> =
            setOf(
                FarmingPatchType.Tree,
                FarmingPatchType.FruitTree,
                FarmingPatchType.HardwoodTree,
                FarmingPatchType.Calquat,
                FarmingPatchType.SpiritTree,
                FarmingPatchType.Celastrus,
                FarmingPatchType.Redwood,
                FarmingPatchType.CrystalTree,
            )

        public fun requiresSecateurs(type: FarmingPatchType): Boolean =
            type in
                setOf(
                    FarmingPatchType.Bush,
                    FarmingPatchType.Tree,
                    FarmingPatchType.FruitTree,
                    FarmingPatchType.HardwoodTree,
                    FarmingPatchType.Calquat,
                    FarmingPatchType.SpiritTree,
                    FarmingPatchType.Celastrus,
                    FarmingPatchType.Redwood,
                )

        private val SUPERCOMPOSTABLE: Set<Int> =
            setOf(
                FarmingObjs.watermelon.id,
                FarmingObjs.snapeGrass.id,
                FarmingObjs.whiteLily.id,
                FarmingObjs.toadflax.id,
                FarmingObjs.avantoe.id,
                FarmingObjs.kwuarm.id,
                FarmingObjs.snapdragon.id,
                FarmingObjs.huasca.id,
                FarmingObjs.cadantine.id,
                FarmingObjs.lantadyme.id,
                FarmingObjs.dwarfWeed.id,
                FarmingObjs.torstol.id,
                FarmingObjs.jangerberries.id,
                FarmingObjs.whiteberries.id,
                FarmingObjs.poisonIvyBerries.id,
                FarmingObjs.pineapple.id,
                FarmingObjs.papaya.id,
                FarmingObjs.coconut.id,
                FarmingObjs.dragonfruit.id,
                FarmingObjs.bittercapMushroom.id,
                FarmingObjs.giantSeaweed.id,
            )
        private val COMPOSTABLE: Set<Int> =
            setOf(
                FarmingObjs.weeds.id,
                FarmingObjs.potato.id,
                FarmingObjs.onion.id,
                FarmingObjs.cabbage.id,
                FarmingObjs.tomato.id,
                FarmingObjs.sweetcorn.id,
                FarmingObjs.strawberry.id,
                FarmingObjs.marigold.id,
                FarmingObjs.rosemary.id,
                FarmingObjs.nasturtium.id,
                FarmingObjs.woad.id,
                FarmingObjs.limpwurt.id,
                FarmingObjs.guam.id,
                FarmingObjs.marrentill.id,
                FarmingObjs.tarromin.id,
                FarmingObjs.harralander.id,
                FarmingObjs.ranarr.id,
                FarmingObjs.irit.id,
                FarmingObjs.barley.id,
                FarmingObjs.hammerstone.id,
                FarmingObjs.asgarnian.id,
                FarmingObjs.jute.id,
                FarmingObjs.yanillian.id,
                FarmingObjs.krandorian.id,
                FarmingObjs.wildblood.id,
                FarmingObjs.redberries.id,
                FarmingObjs.cadavaberries.id,
                FarmingObjs.dwellberries.id,
                FarmingObjs.apple.id,
                FarmingObjs.banana.id,
                FarmingObjs.orange.id,
                FarmingObjs.curryLeaf.id,
                FarmingObjs.potatoCactus.id,
            )
        private val COMPOST_SOURCES =
            listOf(
                FarmingObjs.weeds,
                FarmingObjs.potato,
                FarmingObjs.onion,
                FarmingObjs.cabbage,
                FarmingObjs.tomato,
                FarmingObjs.sweetcorn,
                FarmingObjs.strawberry,
                FarmingObjs.watermelon,
                FarmingObjs.snapeGrass,
                FarmingObjs.marigold,
                FarmingObjs.rosemary,
                FarmingObjs.nasturtium,
                FarmingObjs.woad,
                FarmingObjs.limpwurt,
                FarmingObjs.whiteLily,
                FarmingObjs.guam,
                FarmingObjs.marrentill,
                FarmingObjs.tarromin,
                FarmingObjs.harralander,
                FarmingObjs.ranarr,
                FarmingObjs.toadflax,
                FarmingObjs.irit,
                FarmingObjs.avantoe,
                FarmingObjs.kwuarm,
                FarmingObjs.snapdragon,
                FarmingObjs.huasca,
                FarmingObjs.cadantine,
                FarmingObjs.lantadyme,
                FarmingObjs.dwarfWeed,
                FarmingObjs.torstol,
                FarmingObjs.barley,
                FarmingObjs.hammerstone,
                FarmingObjs.asgarnian,
                FarmingObjs.jute,
                FarmingObjs.yanillian,
                FarmingObjs.krandorian,
                FarmingObjs.wildblood,
                FarmingObjs.redberries,
                FarmingObjs.cadavaberries,
                FarmingObjs.dwellberries,
                FarmingObjs.jangerberries,
                FarmingObjs.whiteberries,
                FarmingObjs.poisonIvyBerries,
                FarmingObjs.apple,
                FarmingObjs.banana,
                FarmingObjs.orange,
                FarmingObjs.curryLeaf,
                FarmingObjs.pineapple,
                FarmingObjs.papaya,
                FarmingObjs.coconut,
                FarmingObjs.dragonfruit,
                FarmingObjs.bittercapMushroom,
                FarmingObjs.potatoCactus,
                FarmingObjs.giantSeaweed,
            )
    }
}
