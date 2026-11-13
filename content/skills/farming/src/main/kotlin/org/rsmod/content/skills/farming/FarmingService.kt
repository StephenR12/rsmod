package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.diary.recordDiaryCelastrusHarvest
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.content.skills.woodcutting.configs.WoodcuttingParams
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.map.square.MapSquareKey
import org.rsmod.map.zone.ZoneKey

@Singleton
public class FarmingService
@Inject
constructor(
    private val petRates:org.rsmod.content.skills.pets.SkillPetRates,
    private val repository: FarmingPatchRepository,
    private val catalogue: FarmingCropCatalogue,
    private val codec: FarmingPatchCodec,
    private val locTypes: LocTypeList,
    private val locs: LocZoneStorage,
    private val random: GameRandom,
    private val objTypes: ObjTypeList,
    private val enumTypes: EnumTypeList,
) {
    internal fun spiritTrees(player: org.rsmod.game.entity.Player): Set<CoordGrid> {
        val destinations = mutableSetOf<CoordGrid>()
        for ((index, tile) in SPIRIT_TREES.withIndex()) {
            val base = locTypes[SpiritPatchLocs.grownPlots[index]]
            val state = repository.find(player, codec.patchKey(tile, base)) ?: continue
            repository.advance(player, state)
            if (
                state.cropKey == "spirit" &&
                    state.status == FarmingCropStatus.Harvestable &&
                    state.healthChecked
            )
                destinations += tile
        }
        return destinations
    }

    internal fun syncHespori(player: org.rsmod.game.entity.Player, state: FarmingPatchState) {
        codec.sync(player, locTypes[HesporiLocs.patch], state, crop(state))
    }

    public fun syncNearby(player: org.rsmod.game.entity.Player) {
        val centre = ZoneKey.from(player.coords)
        for (zoneX in nearbyZones(centre.x)) {
            for (zoneZ in nearbyZones(centre.z)) {
                val zone = ZoneKey(zoneX, zoneZ, centre.level)
                for (loc in locs.mapLocs.findAll(zone)) {
                    val base = locTypes[loc.id] ?: continue
                    if (base.internalName == "kebos_farming_guild_hespori_indicator") {
                        val plot = locTypes[HesporiLocs.patch]
                        val key = codec.patchKey(loc, plot)
                        val state = repository.getOrCreate(player, key, FarmingPatchType.Hespori)
                        repository.advance(player, state)
                        syncHespori(player, state)
                    }
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
        if (state.patchType == FarmingPatchType.Grapes) {
            access.mes("Vine patches must be treated with saltpetre.")
            return false
        }
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
        if (state.patchType in setOf(FarmingPatchType.Anima, FarmingPatchType.Hespori)) return false
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
        access.farmingXp(4.0)
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
        if (
            crop.patchType == FarmingPatchType.Grapes && state.compost != FarmingCompost.Saltpetre
        ) {
            access.mes("Treat the soil with saltpetre before planting a grape seed.")
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
        val bareHanded =
            !treeCrop && tool !in access.inv && access.vars[BarbarianFarmingBits.planting] > 0
        if (tool !in access.inv && !bareHanded) {
            access.mes(
                if (treeCrop) "You need a spade to plant that sapling."
                else "You need a seed dibber to plant that seed."
            )
            return null
        }
        if (crop.patchType == FarmingPatchType.Anima) repository.advanceExisting(access.player)
        val removed = access.invDel(access.inv, crop.seed, crop.seedCount, slot = slot)
        if (removed.failure) return null
        if (
            bareHanded &&
                crop.patchType != FarmingPatchType.Grapes &&
                access.vars[BarbarianFarmingBits.planting] == 1
        ) {
            val failures = access.vars[BarbarianFarmingBits.failures]
            if (failures < 7 && random.of(8) > failures) {
                access.vars[BarbarianFarmingBits.failures] = failures + 1
                access.mes("You crush the seed while trying to plant it without a dibber.")
                return null
            }
            access.vars[BarbarianFarmingBits.planting] = 2
            access.mes("You plant the seed bare-handed. You can report your progress to Otto.")
        }
        if (treeCrop) {
            if (
                access.vars[BarbarianFarmingBits.smashing] > 0 &&
                    access.vars[BarbarianFarmingBits.automatic] != 0
            ) {
                access.mes("You smash the empty plant pot to dust.")
                if (
                    crop.patchType != FarmingPatchType.SpiritTree &&
                        access.vars[BarbarianFarmingBits.smashing] == 1
                )
                    access.vars[BarbarianFarmingBits.smashing] = 2
            } else access.invAdd(access.inv, FarmingObjs.plantPot)
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
        if (crop.key == "jute" && MapSquareKey.from(loc.coords).id == 10551) {
            access.player.completeDiaryTask(DiaryTask.PlantMcGruborJute)
        }
        access.farmingXp(crop.plantXp)
        sync(access, loc, state)
        return crop
    }

    public fun harvest(access: ProtectedAccess, loc: BoundLocInfo): FarmingCrop? {
        val state = state(access, loc) ?: return null
        val crop = crop(state) ?: return null
        if (
            state.status != FarmingCropStatus.Harvestable ||
                state.harvestLives <= 0 ||
                crop.produce == null
        )
            return null
        val blessed =
            crop.patchType == FarmingPatchType.Grapes && FarmingObjs.bologaBlessing in access.inv
        val received =
            access.player.invTransaction(access.inv) {
                val inventory = select(access.inv)
                if (blessed) delete(inventory, FarmingObjs.bologaBlessing.id, 1)
                add(inventory, if (blessed) FarmingObjs.zamorakGrapes.id else crop.produce.id, 1)
            }
        if (!received.success) {
            access.mes("You don't have enough inventory space.")
            return null
        }
        if (crop.key == "celastrus") access.player.recordDiaryCelastrusHarvest(loc.coords)
        val task =
            when (crop.key to MapSquareKey.from(loc.coords).id) {
                "strawberry" to 10548 -> DiaryTask.HarvestArdougneStrawberries
                "poisonivy" to 10290 -> DiaryTask.PickArdougnePoisonIvy
                "torstol" to 10548 -> DiaryTask.PickArdougneTorstol
                "limpwurt" to 11062 -> DiaryTask.PickCatherbyLimpwurt
                "dwarf_weed" to 11062 -> DiaryTask.PickCatherbyDwarfWeed
                "belladonna" to 12340 -> DiaryTask.PickDraynorBelladonna
                "watermelon" to 15148 -> DiaryTask.HarvestHarmonyWatermelon
                "bittercap" to 13622 -> DiaryTask.HarvestCanifisMushroom
                else -> null
            }
        task?.let(access.player::completeDiaryTask)
        access.farmingXp(crop.harvestXp)
        val low = if (blessed) -70 else crop.harvestSaveLow
        val high = if (blessed) 160 else crop.harvestSaveHigh
        val hasMagicSecateurs =
            crop.key != "seaweed" &&
                (access.inv.contains(FarmingObjs.magicSecateurs) ||
                    access.player.worn.contains(FarmingObjs.magicSecateurs))
        val hasFarmingCape =
            state.patchType == FarmingPatchType.Herb &&
                (access.player.back?.id == objs.farming_skillcape.id ||
                    access.player.back?.id == objs.farming_skillcape_t.id ||
                    access.player.back?.id == objs.max_cape_worn.id)
        val itemBonus =
            (if (hasMagicSecateurs) 10 else 0) +
                (if (hasFarmingCape) 5 else 0) +
                (if (repository.activeAnima(access.player) == "attas") 5 else 0)
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
        if (!saved && crop.regrowMinutes > 0 && state.nextGrowthAt == 0L) {
            state.nextGrowthAt =
                repository.firstGrowthTick(
                    access.player,
                    crop.regrowMinutes,
                    System.currentTimeMillis(),
                )
        } else if (crop.regrowMinutes == 0 && state.harvestLives <= 0) {
            if (crop.healthXp == 0.0) {
                access.finishContract(crop, loc.coords)
                rollHesporiSeed(access, crop)
            }
            if (crop.patchType == FarmingPatchType.Grapes) state.status = FarmingCropStatus.Dead
            else clear(state)
        }
        sync(access, loc, state)
        return crop
    }

    public fun checkHealth(
        access: ProtectedAccess,
        loc: BoundLocInfo,
        state: FarmingPatchState,
    ): FarmingCrop? {
        val crop = crop(state) ?: return null
        if (state.status != FarmingCropStatus.Harvestable || state.healthChecked) return null
        if (crop.healthXp <= 0.0) return null
        access.farmingXp(crop.healthXp)
        state.healthChecked = true
        access.finishContract(crop, loc.coords)
        rollHesporiSeed(access, crop)
        val square = MapSquareKey.from(loc.coords).id
        if (square == 11058 && crop.patchType == FarmingPatchType.FruitTree) {
            access.player.completeDiaryTask(DiaryTask.GrowBrimhavenFruitTree)
        }
        val task =
            when (crop.key to square) {
                "palm" to 9777 -> DiaryTask.CheckGnomeVillagePalm
                "palm" to 11058 -> DiaryTask.CheckBrimhavenPalm
                "calquat" to 11056 -> DiaryTask.CheckTaiBwoWannaiCalquat
                "palm" to 9265 -> DiaryTask.CheckLletyaPalm
                else -> null
            }
        task?.let(access.player::completeDiaryTask)
        return crop
    }

    private fun rollHesporiSeed(access: ProtectedAccess, crop: FarmingCrop) {
        petRates.rollTangleroot(access.player,crop)
        if (access.inv.freeSpace() == 0) return
        val denominator =
            when (crop.patchType) {
                FarmingPatchType.Herb -> 196
                FarmingPatchType.FruitTree -> 18
                else ->
                    when (crop.key) {
                        "potato",
                        "onion",
                        "cabbage",
                        "tomato",
                        "marigold",
                        "rosemary",
                        "nasturtium",
                        "woad",
                        "white_lily" -> 562
                        "sweetcorn",
                        "limpwurt" -> 449
                        "strawberry" -> 374
                        "snape_grass" -> 347
                        "watermelon",
                        "potato_cactus" -> 321
                        "barley",
                        "hammerstone" -> 244
                        "asgarnian",
                        "jute" -> 179
                        "yanillian" -> 149
                        "krandorian" -> 128
                        "wildblood" -> 112
                        "redberry" -> 89
                        "cadavaberry" -> 74
                        "dwellberry" -> 64
                        "jangerberry",
                        "whiteberry",
                        "poisonivy" -> 56
                        "oak" -> 44
                        "willow" -> 32
                        "maple" -> 28
                        "yew" -> 22
                        "magic",
                        "celastrus",
                        "crystal" -> 18
                        "belladonna" -> 16
                        "seaweed",
                        "bittercap" -> 15
                        "cactus" -> 14
                        "calquat" -> 12
                        "spirit",
                        "teak",
                        "mahogany",
                        "redwood" -> 10
                        "grape" -> 770
                        else -> return
                    }
            }
        if (
            random.of(denominator) == 0 &&
                access.invAdd(access.inv, FarmingObjs.hesporiSeed).success
        ) {
            access.mes("You find a Hespori seed.")
        }
    }

    internal fun clearAnima(access: ProtectedAccess, loc: BoundLocInfo) {
        val state = state(access, loc) ?: return
        if (state.patchType != FarmingPatchType.Anima || state.isEmpty) return
        repository.advanceExisting(access.player)
        clear(state)
        sync(access, loc, state)
    }

    public fun clearDead(access: ProtectedAccess, loc: BoundLocInfo): Boolean {
        val state = state(access, loc) ?: return false
        if (state.status != FarmingCropStatus.Dead) return false
        if (state.patchType == FarmingPatchType.Anima) repository.advanceExisting(access.player)
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
        state.compost = FarmingCompost.None
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

    private data class TendedPatch(
        val loc: LocInfo,
        val type: UnpackedLocType,
        val state: FarmingPatchState,
    )

    public suspend fun payGardener(
        access: ProtectedAccess,
        gardener: Npc,
        option: String = "",
        used: Int? = null,
    ) {
        val usedType = used?.let { objTypes[it] }?.let(objTypes::uncert)
        val nearby = gardenerPatches(access, gardener)
        val directional =
            when {
                "north" in option -> nearby.maxByOrNull { it.loc.coords.z }
                "south" in option -> nearby.minByOrNull { it.loc.coords.z }
                "east" in option -> nearby.maxByOrNull { it.loc.coords.x }
                "west" in option -> nearby.minByOrNull { it.loc.coords.x }
                else -> null
            }
        val candidates =
            (directional?.let(::listOf) ?: nearby).filter { candidate ->
                val plant = crop(candidate.state)
                plant != null &&
                    plant.protection != null &&
                    !candidate.state.protected &&
                    candidate.state.status in
                        setOf(FarmingCropStatus.Growing, FarmingCropStatus.Diseased) &&
                    (usedType == null || payment(plant).any { it.item.id == usedType.id })
            }
        if (candidates.isEmpty()) {
            access.mes("There is no nearby crop requiring that payment.")
            return
        }
        val chosen =
            if (candidates.size == 1) candidates.single()
            else {
                val names =
                    candidates.map {
                        "${crop(it.state)!!.displayName} at ${it.loc.coords.x}, ${it.loc.coords.z}"
                    }
                candidates.getOrNull(access.menu("Which patch should I look after?", true, names))
                    ?: return
            }
        val crop = crop(chosen.state) ?: return
        val price = payment(crop)
        val quote =
            price.joinToString(", ") { "${it.quantity} × ${objTypes[it.item].name.lowercase()}" }
        if (
            !access.choice2(
                "Pay $quote to protect the ${crop.displayName.lowercase()}.",
                true,
                "Cancel.",
                false,
            )
        )
            return
        repository.advance(access.player, chosen.state)
        if (
            chosen.state.protected ||
                chosen.state.cropKey != crop.key ||
                chosen.state.status !in setOf(FarmingCropStatus.Growing, FarmingCropStatus.Diseased)
        )
            return
        if (!takePayment(access, price)) {
            access.mes("You do not have the gardener's full payment with you.")
            return
        }
        chosen.state.protected = true
        if (crop.key == "magic" && gardener.type.internalName == "farming_gardener_tree_gnome") {
            access.player.completeDiaryTask(DiaryTask.ProtectGnomeMagicTree)
        }
        if (chosen.state.status == FarmingCropStatus.Diseased)
            chosen.state.status = FarmingCropStatus.Growing
        codec.sync(access.player, chosen.type, chosen.state, crop)
        access.mes("The gardener agrees to watch over your ${crop.displayName.lowercase()}.")
    }

    private fun gardenerPatches(access: ProtectedAccess, gardener: Npc): List<TendedPatch> {
        val centre = ZoneKey.from(gardener.coords)
        val nearby = mutableMapOf<String, TendedPatch>()
        val allowed = tendedTypes(gardener)
        for (x in nearbyZones(centre.x)) for (z in nearbyZones(centre.z)) {
            for (loc in locs.mapLocs.findAll(ZoneKey(x, z, centre.level))) {
                if (loc.coords.chebyshevDistance(gardener.coords) > 20) continue
                val base = locTypes[loc.id] ?: continue
                val type = codec.patchType(base) ?: continue
                if (type !in allowed) continue
                val key = codec.patchKey(loc, base)
                if (key in nearby) continue
                val state = repository.getOrCreate(access.player, key, type)
                repository.advance(access.player, state)
                nearby[key] = TendedPatch(loc, base, state)
            }
        }
        return nearby.values.sortedBy { it.loc.coords.chebyshevDistance(gardener.coords) }
    }

    private fun tendedTypes(gardener: Npc): Set<FarmingPatchType> {
        val name = gardener.type.internalName.orEmpty()
        return when {
            "spirit" in name -> setOf(FarmingPatchType.SpiritTree)
            "redwood" in name -> setOf(FarmingPatchType.Redwood)
            "celastrus" in name -> setOf(FarmingPatchType.Celastrus)
            "farmguild_t3" in name || "fruit" in name || name == "garth" ->
                setOf(FarmingPatchType.FruitTree)
            "farmguild_t2" in name || "_tree_" in name -> setOf(FarmingPatchType.Tree)
            "farmguild_t1" in name -> setOf(FarmingPatchType.Allotment)
            "underwater" in name -> setOf(FarmingPatchType.Seaweed)
            "squirrel" in name -> setOf(FarmingPatchType.HardwoodTree)
            "calquat" in name -> setOf(FarmingPatchType.Calquat)
            "cactus" in name -> setOf(FarmingPatchType.Cactus)
            "hops" in name || name == "francis" -> setOf(FarmingPatchType.Hops)
            "bush" in name -> setOf(FarmingPatchType.Bush)
            else -> setOf(FarmingPatchType.Allotment)
        }
    }

    private fun payment(crop: FarmingCrop): List<FarmingProtection> =
        if (crop.key == "spirit")
            listOf(
                FarmingProtection(FarmingObjs.monkeyNuts, 5),
                FarmingProtection(FarmingObjs.monkeyBar, 1),
                FarmingProtection(FarmingObjs.groundTooth, 1),
            )
        else listOf(requireNotNull(crop.protection))

    private fun takePayment(access: ProtectedAccess, price: List<FarmingProtection>): Boolean {
        val deletions = mutableListOf<Pair<Int, Int>>()
        for ((item, amount) in price) {
            val unnoted = objTypes[item]
            val normalCount = minOf(amount, access.invTotal(access.inv, unnoted))
            if (normalCount > 0) deletions += unnoted.id to normalCount
            val remainder = amount - normalCount
            if (remainder > 0) {
                val note = objTypes.cert(unnoted)
                if (note.id == unnoted.id || access.invTotal(access.inv, note) < remainder)
                    return false
                deletions += note.id to remainder
            }
        }
        return access.player
            .invTransaction(access.inv) {
                val inventory = select(access.inv)
                for ((id, count) in deletions) delete(inventory, id, count)
            }
            .success
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
        when (MapSquareKey.from(loc.coords).id) {
            12083 -> access.player.completeDiaryTask(DiaryTask.PlaceFaladorScarecrow)
            14391 -> access.player.completeDiaryTask(DiaryTask.PlaceMorytaniaScarecrow)
        }
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
        if (crop.roots != null) {
            val count =
                (1 + (access.stat(stats.farming) - crop.level).coerceAtLeast(0) / 8).coerceAtMost(4)
            if (access.invAdd(access.inv, crop.roots, count).failure) {
                access.mes("You need inventory space for the roots.")
                return false
            }
            val square = MapSquareKey.from(loc.coords).id
            if (crop.key == "magic" && square == 11828 && count >= 3)
                access.player.completeDiaryTask(DiaryTask.DigFaladorMagicRoots)
            if (crop.key == "yew" && square == 12854 && count >= 2)
                access.player.completeDiaryTask(DiaryTask.DigVarrockYewRoots)
        }
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
                access.farmingXp(4.5)
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
        private val SPIRIT_TREES =
            listOf(
                CoordGrid(3059, 3257),
                CoordGrid(2612, 3857),
                CoordGrid(2801, 3202),
                CoordGrid(1692, 3541),
                CoordGrid(1252, 3749),
            )
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
