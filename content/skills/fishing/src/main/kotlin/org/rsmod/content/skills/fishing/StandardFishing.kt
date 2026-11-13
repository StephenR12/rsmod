package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.clue.ClueVessels
import org.rsmod.content.skills.pets.SkillPetRates
import kotlin.math.floor
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.LumbridgeDiarySkillArea
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.diary.recordDiaryFish
import org.rsmod.api.player.events.KingdomContribution
import org.rsmod.api.player.events.KingdomWork
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class StandardFishing
@Inject
constructor(private val vessels: ClueVessels, private val petRates: SkillPetRates, private val objTypes: ObjTypeList, private val fishBarrel: FishBarrelStorage) {
    public suspend fun lure(access: ProtectedAccess, npc: Npc) {
        val method =
            if (access.player.inv.contains(StandardFishingMethods.fly.bait!!)) {
                StandardFishingMethods.fly
            } else if (access.player.inv.contains(StandardFishingMethods.rainbow.bait!!)) {
                StandardFishingMethods.rainbow
            } else {
                StandardFishingMethods.fly
            }
        fish(access, npc, method)
    }

    public suspend fun fish(access: ProtectedAccess, npc: Npc, initialMethod: FishingMethod) {
        with(access) {
            val kingdom = KingdomWork.fish(npc.coords)
            var method = initialMethod
            var previousDiabolicCatch = false
            if (!canFish(method, sendFailure = true, kingdom = kingdom)) {
                return
            }
            faceEntitySquare(npc)
            while (true) {
                if (initialMethod.catches.singleOrNull()?.output == objs.raw_anglerfish) {
                    method = HighLevelFishingMethods.selectAngler(player)
                    if (!canFish(method, sendFailure = false, kingdom = kingdom)) return
                }
                val tool = findTool(method)
                val barehanded = tool == null && canBarehand(method)
                if (tool == null && !barehanded) {
                    return
                }
                anim(tool?.animation ?: seqs.brut_player_hand_fishing_end_blank)
                val extraDelay = method.extraRollDelayChance?.let(random::randomBoolean) == true
                val fasterRoll = previousDiabolicCatch && random.randomBoolean(2)
                previousDiabolicCatch = false
                gatheringDelay(
                    ROLL_INTERVAL + (if (extraDelay) 1 else 0) - (if (fasterRoll) 1 else 0)
                )
                if (
                    !isWithinDistance(npc, distance = 1) ||
                        !canFish(method, sendFailure = false, kingdom = kingdom) ||
                        (tool?.let { !hasTool(it) } ?: !canBarehand(method))
                ) {
                    resetAnim()
                    return
                }
                val level = player.stat(stats.fishing)
                val invisibleBoost = fishingGuildBoost(npc)
                val catch =
                    method.catches.firstOrNull { candidate ->
                        level >= candidate.level &&
                            meetsCatchRequirements(candidate) &&
                            (!barehanded || canBarehand(candidate)) &&
                            rollCatch(candidate, tool?.catchRateMultiplier ?: 1.0, invisibleBoost)
                    } ?: continue
                val replacement =
                    catch.rareReplacement?.takeIf { rare ->
                        (!rare.membersOnly || player.members) && random.randomBoolean(rare.chance)
                    }
                val output = replacement?.output ?: catch.output
                val message = replacement?.message ?: catch.message
                if (kingdom != null) {
                    FishingXp.advance(this, catch.xp)
                    if (barehanded && catch.barehandedStrengthXp > 0.0)
                        statAdvance(stats.strength, catch.barehandedStrengthXp)
                    publish(KingdomContribution(player, kingdom))
                    continue
                }
                val multiplier = catchMultiplier(method, output)
                val infernalCookingXp =
                    catch.infernalCookingXp?.takeIf {
                        replacement == null && tool?.infernalDepletedType != null
                    }
                val cookedCount =
                    if (infernalCookingXp == null) {
                        0
                    } else {
                        (1..multiplier.outputCount).count {
                            random.randomBoolean(INFERNAL_AUTO_COOK_CHANCE)
                        }
                    }
                val rawCount = multiplier.outputCount - cookedCount
                val bait = findBait(method)
                val barrelCount =
                    if (fishBarrel.canStoreCaught(player, output)) {
                        minOf(rawCount, fishBarrel.remainingCapacity(player))
                    } else {
                        0
                    }
                val inventoryRawCount = rawCount - barrelCount
                val occupied = petRates.occupiedBy(player,output.id)
                val inventorySuccess =
                    if (bait == null && !multiplier.consumeSpiritFlake && inventoryRawCount == 0) {
                        true
                    } else {
                        player
                            .invTransaction(player.inv) {
                                val inventory = select(player.inv)
                                bait?.let { delete(inventory, it.id, 1) }
                                if (multiplier.consumeSpiritFlake) {
                                    delete(inventory, objs.spirit_flakes.id, 1)
                                }
                                if (inventoryRawCount > 0) {
                                    add(inventory, output.id, inventoryRawCount)
                                }
                            }
                            .success
                    }
                if (!inventorySuccess) {
                    resetAnim()
                    return
                }
                previousDiabolicCatch = bait == HighLevelFishingObjs.diabolic
                check(fishBarrel.storeCaught(player, output, barrelCount) == barrelCount)
                if (cookedCount > 0 && !consumeInfernalCharges(tool!!, cookedCount)) {
                    resetAnim()
                    return
                }
                if (
                    tool?.crystalDepletedType != null &&
                        !consumeCrystalCharges(tool, multiplier.outputCount)
                ) {
                    resetAnim()
                    return
                }
                val petRate = when (catch.output.id) {
                    objs.raw_shrimps.id, objs.raw_anchovies.id -> 870_330
                    objs.raw_sardine.id, objs.raw_herring.id -> 1_056_000
                    objs.raw_trout.id, objs.raw_salmon.id -> 923_616
                    objs.raw_pike.id -> 305_792
                    objs.raw_rainbow_fish.id -> 137_739
                    objs.raw_tuna.id, objs.raw_swordfish.id, objs.raw_cave_eel.id, 25664 -> 257_770
                    objs.raw_lobster.id -> 116_129
                    objs.raw_monkfish.id -> 138_583
                    objs.leaping_trout.id, objs.leaping_salmon.id, objs.leaping_sturgeon.id -> 1_280_862
                    objs.raw_shark.id -> 82_243
                    objs.infernal_eel.id -> 165_000
                    objs.raw_anglerfish.id -> 78_649
                    objs.raw_dark_crab.id -> 149_434
                    objs.sacred_eel.id -> 99_000
                    21356 -> 977_778
                    25652 -> 820_330
                    25658 -> 300_792
                    25670 -> 152_120
                    else -> null
                }
                if (petRate != null) petRates.roll(player,stats.fishing,13320,petRate,
                    petRates.newResourceSlot(player,output.id,occupied))
                val bottleRate = when (catch.output.id) {
                    objs.raw_shrimps.id, objs.raw_anchovies.id -> 435_165
                    objs.raw_sardine.id, objs.raw_herring.id -> 528_000
                    objs.raw_trout.id, objs.raw_salmon.id -> 461_808
                    objs.leaping_trout.id, objs.leaping_salmon.id, objs.leaping_sturgeon.id -> 426_954
                    objs.infernal_eel.id -> 160_000
                    25652, 25658, 25664, 25670 -> 257_770
                    else -> petRate
                }
                if (bottleRate != null) vessels.bottles(player, level, bottleRate)
                FishingXp.advance(this, catch.xp)
                player.recordDiaryFish(catch.output.id, npc.coords, barehanded)
                if (
                    catch.output.id == objs.raw_anchovies.id &&
                        LumbridgeDiarySkillArea.alKharidFishing(npc.coords)
                ) {
                    player.completeDiaryTask(DiaryTask.CatchAlKharidAnchovies)
                }
                if (barehanded) {
                    statAdvance(stats.strength, catch.barehandedStrengthXp)
                }
                if (catch.strengthXp > 0.0) {
                    statAdvance(stats.strength, catch.strengthXp)
                }
                if (catch.agilityXp > 0.0) {
                    statAdvance(stats.agility, catch.agilityXp)
                }
                if (infernalCookingXp != null && cookedCount > 0) {
                    statAdvance(stats.cooking, infernalCookingXp * cookedCount)
                }
                mes(message)
                if (
                    player.inv.contains(objs.fish_barrel_open) &&
                        output in FishBarrelStorage.accepted &&
                        fishBarrel.remainingCapacity(player) == 0
                ) {
                    mes(FishBarrelStorage.BARREL_FULL_MESSAGE)
                }
                if (barehanded && vars[varbits.brut_fishing_s] == TRAINING_TASKED) {
                    vars[varbits.brut_fishing_s] = TRAINING_FISH_CAUGHT
                    resetAnim()
                    mesboxNp(TRAINING_MESSAGE)
                    return
                }
                if (
                    method.requiresBarbarianRodTraining &&
                        vars[varbits.brut_fishing_r] == TRAINING_TASKED
                ) {
                    vars[varbits.brut_fishing_r] = TRAINING_FISH_CAUGHT
                    resetAnim()
                    mesboxNp(TRAINING_MESSAGE)
                    return
                }
            }
        }
    }

    public suspend fun bigNet(access: ProtectedAccess, npc: Npc) {
        with(access) {
            if (!canBigNet(sendFailure = true)) {
                return
            }
            faceEntitySquare(npc)
            while (true) {
                anim(seqs.human_bignet)
                gatheringDelay(ROLL_INTERVAL)
                if (!isWithinDistance(npc, distance = 1) || !canBigNet(sendFailure = false)) {
                    resetAnim()
                    return
                }
                val visibleLevel = player.stat(stats.fishing)
                val baseLevel = player.statBase(stats.fishing)
                val invisibleBoost = fishingGuildBoost(npc)
                val chargedNecklace =
                    StandardFishingMethods.chargedSkillsNecklaces.any(player.worn::contains)
                val catches =
                    StandardFishingMethods.bigNetRolls.mapNotNull { roll ->
                        val requirementLevel =
                            if (roll.baseLevelRequired) baseLevel else visibleLevel
                        if (requirementLevel < roll.level) {
                            return@mapNotNull null
                        }
                        val chanceBonus = if (chargedNecklace) roll.casketChanceBonus else 0
                        val success =
                            statRandom(
                                stats.fishing,
                                roll.low + chanceBonus,
                                roll.high + chanceBonus,
                                invisibleBoost = invisibleBoost,
                            )
                        if (!success) {
                            return@mapNotNull null
                        }
                        val replacement =
                            roll.rareReplacement?.takeIf { rare ->
                                (!rare.membersOnly || player.members) &&
                                    random.randomBoolean(rare.chance)
                            }
                        BigNetResult(
                            output = replacement?.output ?: roll.output,
                            xp = roll.xp,
                            message = replacement?.message ?: roll.message,
                        )
                    }
                if (catches.isEmpty()) {
                    continue
                }
                val accepted = multiplyBigNetCatches(catches)
                if (accepted.catches.isEmpty()) {
                    resetAnim()
                    return
                }
                val lastResource = accepted.catches.lastOrNull { it.inventoryCount > 0 }?.output?.id ?: -1
                val occupied = petRates.occupiedBy(player,lastResource)
                val inventorySuccess =
                    if (
                        accepted.spiritFlakesConsumed == 0 &&
                            accepted.catches.none { it.inventoryCount > 0 }
                    ) {
                        true
                    } else {
                        player
                            .invTransaction(player.inv) {
                                val inventory = select(player.inv)
                                if (accepted.spiritFlakesConsumed > 0) {
                                    delete(
                                        inventory,
                                        objs.spirit_flakes.id,
                                        accepted.spiritFlakesConsumed,
                                    )
                                }
                                accepted.catches.forEach {
                                    if (it.inventoryCount > 0) {
                                        add(inventory, it.output.id, it.inventoryCount)
                                    }
                                }
                            }
                            .success
                    }
                if (!inventorySuccess) {
                    resetAnim()
                    return
                }
                accepted.catches.forEach {
                    check(
                        fishBarrel.storeCaught(player, it.output, it.barrelCount) == it.barrelCount
                    )
                }
                petRates.roll(player,stats.fishing,13320,1_147_827,petRates.newResourceSlot(player,lastResource,occupied))
                vessels.bottles(player, player.stat(stats.fishing), 382_609)
                accepted.catches.forEach {
                    FishingXp.advance(this, it.xp)
                    player.recordDiaryFish(it.output.id, npc.coords)
                    mes(it.message)
                }
                if (
                    accepted.catches.any { it.barrelCount > 0 } &&
                        fishBarrel.remainingCapacity(player) == 0
                ) {
                    mes(FishBarrelStorage.BARREL_FULL_MESSAGE)
                }
                if (player.inv.isFull() && !fishBarrel.hasOpenCapacity(player)) {
                    resetAnim()
                    return
                }
            }
        }
    }

    private fun ProtectedAccess.canFish(
        method: FishingMethod,
        sendFailure: Boolean,
        kingdom: KingdomWork?,
    ): Boolean {
        if (kingdom != null && !kingdom.permitted(player, sendFailure)) return false
        if (method.membersOnly && !player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to fish here.")
            }
            return false
        }
        val varbitRequirement = method.varbitRequirement
        if (
            varbitRequirement != null &&
                vars[varbitRequirement.type] < varbitRequirement.minimumValue
        ) {
            if (sendFailure) {
                mes(varbitRequirement.failureMessage)
            }
            return false
        }
        if (method.requiresBarbarianRodTraining && vars[varbits.brut_fishing_r] < TRAINING_TASKED) {
            if (sendFailure) {
                mes("You must begin Barbarian Fishing training before you can fish here.")
            }
            return false
        }
        val level = player.stat(stats.fishing)
        val minimumLevel = method.catches.minOf(FishingCatch::level)
        if (level < minimumLevel) {
            if (sendFailure) {
                mes("You need a Fishing level of $minimumLevel to fish here.")
            }
            return false
        }
        val minimumStrengthLevel = method.catches.mapNotNull { it.baseStrengthLevel }.minOrNull()
        if (
            minimumStrengthLevel != null && player.statBase(stats.strength) < minimumStrengthLevel
        ) {
            if (sendFailure) {
                mes("You need a Strength level of $minimumStrengthLevel to fish here.")
            }
            return false
        }
        val minimumAgilityLevel = method.catches.mapNotNull { it.baseAgilityLevel }.minOrNull()
        if (minimumAgilityLevel != null && player.statBase(stats.agility) < minimumAgilityLevel) {
            if (sendFailure) {
                mes("You need an Agility level of $minimumAgilityLevel to fish here.")
            }
            return false
        }
        if (findTool(method) == null && !canBarehand(method)) {
            if (sendFailure) {
                mes("You do not have the required equipment to fish here.")
            }
            return false
        }
        if (method.requiredWorn.isNotEmpty() && method.requiredWorn.none(player.worn::contains)) {
            if (sendFailure) {
                mes(method.missingRequiredWornMessage)
            }
            return false
        }
        val bait = findBait(method)
        if (requiredBaits(method).isNotEmpty() && bait == null) {
            if (sendFailure) {
                mes(method.missingBaitMessage)
            }
            return false
        }
        val barrelCanAcceptCatch =
            method.catches.any { fishBarrel.canStoreCaught(player, it.output) }
        if (
            kingdom == null &&
                player.inv.isFull() &&
                (bait == null || player.inv.count(objTypes[bait]) > 1) &&
                !barrelCanAcceptCatch
        ) {
            if (sendFailure) {
                mes("You do not have enough inventory space.")
            }
            return false
        }
        return true
    }

    private fun ProtectedAccess.findTool(method: FishingMethod): FishingTool? =
        method.tools.firstOrNull { hasTool(it) }

    private fun requiredBaits(method: FishingMethod): List<ObjType> =
        if (method.baits.isNotEmpty()) method.baits else listOfNotNull(method.bait)

    private fun ProtectedAccess.findBait(method: FishingMethod): ObjType? =
        requiredBaits(method).firstOrNull(player.inv::contains)

    private fun ProtectedAccess.meetsCatchRequirements(catch: FishingCatch): Boolean =
        (catch.baseStrengthLevel == null ||
            player.statBase(stats.strength) >= catch.baseStrengthLevel) &&
            (catch.baseAgilityLevel == null ||
                player.statBase(stats.agility) >= catch.baseAgilityLevel)

    private fun ProtectedAccess.canBarehand(method: FishingMethod): Boolean =
        method.allowsBarehanded &&
            player.members &&
            vars[varbits.brut_fishing_s] >= TRAINING_TASKED &&
            method.catches.any { canBarehand(it) }

    private fun ProtectedAccess.canBarehand(catch: FishingCatch): Boolean {
        val strengthLevel = catch.barehandedStrengthLevel ?: return false
        return player.stat(stats.fishing) >= catch.level + BAREHANDED_FISHING_LEVEL_OFFSET &&
            player.statBase(stats.strength) >= strengthLevel
    }

    private fun ProtectedAccess.hasTool(tool: FishingTool): Boolean =
        (!tool.membersOnly || player.members) &&
            player.stat(stats.fishing) >= tool.level &&
            (player.inv.contains(tool.type) || (tool.wearable && player.worn.contains(tool.type)))

    private fun ProtectedAccess.catchMultiplier(
        method: FishingMethod,
        output: ObjType,
    ): FishingCatchMultiplier {
        if (!player.members || availableOutputSlots(method, output) < 2) {
            return FishingCatchMultiplier.Single
        }
        val blessing =
            StandardFishingMethods.radasBlessings.firstOrNull { player.worn.contains(it.type) }
        val consumeSpiritFlake = player.inv.contains(objs.spirit_flakes)
        val extraCatchPercent =
            (if (consumeSpiritFlake) SPIRIT_FLAKE_EXTRA_CATCH_PERCENT else 0) +
                (blessing?.extraCatchPercent ?: 0)
        if (extraCatchPercent == 0) {
            return FishingCatchMultiplier.Single
        }
        val outputCount = if (random.of(100) < extraCatchPercent) 2 else 1
        return FishingCatchMultiplier(outputCount, consumeSpiritFlake)
    }

    private fun ProtectedAccess.availableOutputSlots(method: FishingMethod, output: ObjType): Int {
        val baitFreesSlot = findBait(method)?.let { player.inv.count(objTypes[it]) == 1 } == true
        val barrelSpace =
            if (fishBarrel.canStoreCaught(player, output)) {
                fishBarrel.remainingCapacity(player)
            } else {
                0
            }
        return player.inv.freeSpace() + (if (baitFreesSlot) 1 else 0) + barrelSpace
    }

    private fun ProtectedAccess.multiplyBigNetCatches(
        catches: List<BigNetResult>
    ): BigNetAcceptedCatches {
        var remainingInventorySlots = player.inv.freeSpace()
        var remainingBarrelSlots = fishBarrel.remainingCapacity(player)
        var remainingSpiritFlakes = player.inv.count(objTypes[objs.spirit_flakes])
        var spiritFlakesConsumed = 0
        val blessing =
            StandardFishingMethods.radasBlessings.firstOrNull { player.worn.contains(it.type) }
        val accepted = mutableListOf<BigNetResult>()
        for (catch in catches) {
            val barrelEligible = fishBarrel.canStoreCaught(player, catch.output)
            val availableSlots =
                remainingInventorySlots + if (barrelEligible) remainingBarrelSlots else 0
            if (availableSlots == 0) {
                continue
            }
            val canMultiply = availableSlots >= 2
            val consumeSpiritFlake = canMultiply && remainingSpiritFlakes > 0
            val extraCatchPercent =
                (if (consumeSpiritFlake) SPIRIT_FLAKE_EXTRA_CATCH_PERCENT else 0) +
                    (if (canMultiply) blessing?.extraCatchPercent ?: 0 else 0)
            val count = if (extraCatchPercent > 0 && random.of(100) < extraCatchPercent) 2 else 1
            val barrelCount = if (barrelEligible) minOf(count, remainingBarrelSlots) else 0
            val inventoryCount = count - barrelCount
            accepted += catch.copy(count = count, barrelCount = barrelCount)
            remainingBarrelSlots -= barrelCount
            remainingInventorySlots -= inventoryCount
            if (consumeSpiritFlake) {
                remainingSpiritFlakes--
                spiritFlakesConsumed++
            }
        }
        return BigNetAcceptedCatches(accepted, spiritFlakesConsumed)
    }

    private fun ProtectedAccess.consumeInfernalCharges(tool: FishingTool, count: Int): Boolean {
        val depletedType = tool.infernalDepletedType ?: return false
        val chargesToConsume = chargeConsumptionCount(count, allowCrystalSignet = false)
        if (chargesToConsume == 0) {
            return true
        }
        val inventorySlot = player.inv.indexOfFirst { it?.id == tool.type.id }
        if (inventorySlot != -1) {
            val obj = player.inv.getValue(inventorySlot)
            player.inv[inventorySlot] =
                InfernalHarpoonState.fromCharged(obj).consume(obj, depletedType, chargesToConsume)
            return true
        }
        val wornSlot = player.worn.indexOfFirst { it?.id == tool.type.id }
        if (wornSlot == -1) {
            return false
        }
        val obj = player.worn.getValue(wornSlot)
        player.worn[wornSlot] =
            InfernalHarpoonState.fromCharged(obj).consume(obj, depletedType, chargesToConsume)
        return true
    }

    private fun ProtectedAccess.consumeCrystalCharges(tool: FishingTool, count: Int): Boolean {
        val depletedType = tool.crystalDepletedType ?: return false
        val chargesToConsume = chargeConsumptionCount(count, allowCrystalSignet = true)
        if (chargesToConsume == 0) {
            return true
        }
        val inventorySlot = player.inv.indexOfFirst { it?.id == tool.type.id }
        if (inventorySlot != -1) {
            val obj = player.inv.getValue(inventorySlot)
            player.inv[inventorySlot] =
                CrystalHarpoonState.fromCharged(obj).consume(obj, depletedType, chargesToConsume)
            return true
        }
        val wornSlot = player.worn.indexOfFirst { it?.id == tool.type.id }
        if (wornSlot == -1) {
            return false
        }
        val obj = player.worn.getValue(wornSlot)
        player.worn[wornSlot] =
            CrystalHarpoonState.fromCharged(obj).consume(obj, depletedType, chargesToConsume)
        return true
    }

    private fun ProtectedAccess.chargeConsumptionCount(
        count: Int,
        allowCrystalSignet: Boolean,
    ): Int {
        val hasCrystalSignet =
            allowCrystalSignet && CRYSTAL_CHARGE_SAVING_SIGNETS.any { player.worn.contains(it) }
        val hasLuckyPenny = player.worn.contains(objs.ghommals_lucky_penny)
        return (1..count).count {
            val signetSaved = hasCrystalSignet && random.randomBoolean(SIGNET_SAVE_CHANCE)
            val pennySaved = hasLuckyPenny && random.randomBoolean(LUCKY_PENNY_SAVE_CHANCE)
            !signetSaved && !pennySaved
        }
    }

    private fun ProtectedAccess.rollCatch(
        catch: FishingCatch,
        catchRateMultiplier: Double,
        invisibleBoost: Int,
    ): Boolean {
        // Elite Wilderness adds an independent bonus roll; a failed bonus still gets normal3/40.
        if (
            catch.output == objs.raw_dark_crab &&
                vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Elite).complete] != 0
        ) {
            val bonus = catch.copy(low = 13, high = 55)
            val rate =
                adjustFishingSuccessRate(
                    fishingSuccessRate(bonus, player.stat(stats.fishing), invisibleBoost),
                    catchRateMultiplier,
                )
            if (rate > random.randomDouble()) return true
        }
        val baseRate = fishingSuccessRate(catch, player.stat(stats.fishing), invisibleBoost)
        val adjustedRate = adjustFishingSuccessRate(baseRate, catchRateMultiplier)
        return adjustedRate > random.randomDouble()
    }

    private fun fishingGuildBoost(npc: Npc): Int =
        if (FishingGuildFishingNpcs.all.any { it.id == npc.id }) FISHING_GUILD_BOOST else 0

    private fun ProtectedAccess.canBigNet(sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to fish here.")
            }
            return false
        }
        if (player.stat(stats.fishing) < BIG_NET_LEVEL) {
            if (sendFailure) {
                mes("You need a Fishing level of $BIG_NET_LEVEL to fish here.")
            }
            return false
        }
        if (!player.inv.contains(objs.big_fishing_net)) {
            if (sendFailure) {
                mes("You do not have the required equipment to fish here.")
            }
            return false
        }
        if (player.inv.isFull() && !fishBarrel.hasOpenCapacity(player)) {
            if (sendFailure) {
                mes("You do not have enough inventory space.")
            }
            return false
        }
        return true
    }

    private data class BigNetResult(
        val output: ObjType,
        val xp: Double,
        val message: String,
        val count: Int = 1,
        val barrelCount: Int = 0,
    ) {
        val inventoryCount: Int
            get() = count - barrelCount
    }

    private data class BigNetAcceptedCatches(
        val catches: List<BigNetResult>,
        val spiritFlakesConsumed: Int,
    )

    private data class FishingCatchMultiplier(
        val outputCount: Int,
        val consumeSpiritFlake: Boolean,
    ) {
        companion object {
            val Single: FishingCatchMultiplier = FishingCatchMultiplier(1, false)
        }
    }

    private companion object {
        const val BIG_NET_LEVEL = 16
        const val BAREHANDED_FISHING_LEVEL_OFFSET = 20
        const val TRAINING_TASKED = 1
        const val TRAINING_FISH_CAUGHT = 2
        const val FISHING_GUILD_BOOST = 7
        const val INFERNAL_AUTO_COOK_CHANCE = 3
        const val SIGNET_SAVE_CHANCE = 10
        const val LUCKY_PENNY_SAVE_CHANCE = 20
        const val SPIRIT_FLAKE_EXTRA_CATCH_PERCENT = 50
        const val ROLL_INTERVAL = 5

        const val TRAINING_MESSAGE =
            "You feel you have learned more of barbarian ways. Otto might wish to talk to you more."

        val CRYSTAL_CHARGE_SAVING_SIGNETS =
            listOf(objs.elven_signet, objs.celestial_signet, objs.celestial_signet_charged)
    }
}

public fun fishingSuccessRate(catch: FishingCatch, visibleLevel: Int, invisibleBoost: Int): Double =
    SkillingSuccessRate.successRate(
        catch.low,
        catch.high,
        visibleLevel.coerceIn(1, stats.fishing.maxLevel) + invisibleBoost,
        stats.fishing.maxLevel,
    )

public fun adjustFishingSuccessRate(baseRate: Double, multiplier: Double): Double =
    (floor(baseRate * multiplier * 256.0) / 256.0).coerceAtMost(1.0)
