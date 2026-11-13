package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.content.skills.pets.RiftGuardian
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType

public class StandardRuneCrafting
@Inject
internal constructor(
    private val guardian: RiftGuardian,
    private val rift:RiftRound,private val imbuing:RiftImbuing,
    private val bloodEssence: BloodEssenceLifecycle,
    private val modifiers: RunecraftOutputModifiers,
) {
    public fun craft(access: ProtectedAccess, recipe: StandardRuneRecipe) {
        with(access) {
            val visibleLevel = player.stat(stats.runecrafting)
            if (!checkRequirements(recipe, visibleLevel)) {
                return
            }

            if (rift.playingAltar(player,recipe)) {
                val temple=riftTemples.first {it.recipe==recipe}
                imbuing.standard(this,temple)
                return
            }
            val essence = availableEssence(recipe)
            val essenceCount = essence.sumOf { it.count }
            val core =
                when (recipe) {
                    StandardRuneRecipes.mind -> RunecraftObjs.mindCore
                    StandardRuneRecipes.body -> RunecraftObjs.bodyCore
                    StandardRuneRecipes.chaos -> RunecraftObjs.chaosCore
                    else -> null
                }
            val cores = core?.let { invTotal(inv, it) } ?: 0
            if (essenceCount == 0 && cores == 0) {
                mes("You do not have any essence to bind.")
                return
            }

            val multiplier = recipe.outputMultiplier(visibleLevel)
            val baseRunes = essenceCount * multiplier
            val coreRunes = cores * 11 * multiplier
            val extractCount = modifiers.applicableExtractCount(this, essenceCount, recipe.extract)
            val outfitRunes = modifiers.raimentsBonus(this, baseRunes)
            val bloodEssencePlan =
                if (recipe == StandardRuneRecipes.blood) {
                    bloodEssence.planBonus(player.inv, essenceCount, random)
                } else {
                    null
                }
            val outputCount =
                baseRunes +
                    coreRunes +
                    modifiers.raimentsBonus(this, coreRunes) +
                    (extractCount * recipe.extractBonus) +
                    outfitRunes +
                    (bloodEssencePlan?.bonusRunes ?: 0)
            val xp =
                essence.sumOf { it.count * recipe.xp * it.xpMultiplier } + cores * 10 * recipe.xp

            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for (input in essence) {
                        delete(inventory, input.type.id, input.count)
                    }
                    if (core != null && cores > 0) delete(inventory, core.id, cores)
                    if (extractCount > 0) {
                        delete(inventory, requireNotNull(recipe.extract).id, extractCount)
                    }
                    if (bloodEssencePlan != null && bloodEssencePlan.bonusRunes > 0) {
                        delete(
                            inventory,
                            RunecraftObjs.bloodEssenceActive.id,
                            1,
                            slot = bloodEssencePlan.slot,
                        )
                        if (bloodEssencePlan.remainingCharges > 0) {
                            add(
                                inventory,
                                RunecraftObjs.bloodEssenceActive.id,
                                1,
                                vars = BloodEssenceState(bloodEssencePlan.remainingCharges).pack(),
                                slot = bloodEssencePlan.slot,
                            )
                        }
                    }
                    add(inventory, recipe.output.id, outputCount)
                }
            check(transaction.success) {
                "Rune altar transaction failed after validation: $transaction"
            }

            val simpleTask =
                when (recipe) {
                    StandardRuneRecipes.water -> DiaryTask.CraftWaterRunes
                    StandardRuneRecipes.earth -> DiaryTask.CraftEarthRunes
                    StandardRuneRecipes.nature -> DiaryTask.CraftNatureRunes
                    StandardRuneRecipes.death -> DiaryTask.CraftDeathRunes
                    else -> null
                }
            simpleTask?.let(player::completeDiaryTask)
            // Additional runes from extracts do not count toward a batch requirement.
            val eligibleRunes = baseRunes + outfitRunes
            val quantityTask =
                when {
                    recipe == StandardRuneRecipes.mind && eligibleRunes >= 140 ->
                        DiaryTask.Craft140MindRunes
                    recipe == StandardRuneRecipes.air && eligibleRunes >= 252 ->
                        DiaryTask.Craft252AirRunes
                    recipe == StandardRuneRecipes.astral && eligibleRunes >= 56 ->
                        DiaryTask.Craft56AstralRunes
                    recipe == StandardRuneRecipes.nature && eligibleRunes >= 56 ->
                        DiaryTask.Craft56NatureRunes
                    recipe == StandardRuneRecipes.cosmic && eligibleRunes >= 56 ->
                        DiaryTask.Craft56CosmicRunes
                    recipe == StandardRuneRecipes.water && eligibleRunes >= 140 ->
                        DiaryTask.Craft140WaterRunes
                    recipe == StandardRuneRecipes.earth && eligibleRunes >= 100 ->
                        DiaryTask.Craft100EarthRunes
                    else -> null
                }
            if (
                quantityTask != null &&
                    (extractCount == 0 ||
                        quantityTask.region == org.rsmod.api.player.diary.DiaryRegion.Falador)
            ) {
                player.completeDiaryTask(quantityTask)
            }

            guardian.craft(player,recipe.name,essenceCount)
            anim(RunecraftSeqs.craft)
            spotanim(RunecraftSpotanims.craft)
            statAdvance(stats.runecrafting, xp)
            mes("You bind the temple's power into ${recipe.name} runes.")
        }
    }

    private fun ProtectedAccess.checkRequirements(
        recipe: StandardRuneRecipe,
        visibleLevel: Int,
    ): Boolean {
        if (recipe.members && !player.members) {
            mes("You need to be on a members' world to craft ${recipe.name} runes.")
            return false
        }
        if (visibleLevel < recipe.level) {
            mes(
                "You need a Runecrafting level of at least ${recipe.level} to craft ${recipe.name} runes."
            )
            return false
        }
        val quest = recipe.quest
        if (quest != null && vars[quest.varbit] < quest.completionValue) {
            mes("You need to complete ${quest.questName} to craft ${recipe.name} runes.")
            return false
        }
        return true
    }

    private fun ProtectedAccess.availableEssence(recipe: StandardRuneRecipe): List<EssenceInput> =
        buildList {
            if (recipe.runeEssence) {
                val count = invTotal(player.inv, RunecraftObjs.runeEssence)
                if (count > 0) add(EssenceInput(RunecraftObjs.runeEssence, count, 1.0))
            }
            val pureCount = invTotal(player.inv, RunecraftObjs.pureEssence)
            if (pureCount > 0) add(EssenceInput(RunecraftObjs.pureEssence, pureCount, 1.0))
            if (player.members) {
                val daeyaltCount = invTotal(player.inv, RunecraftObjs.daeyaltEssence)
                if (daeyaltCount > 0) {
                    add(EssenceInput(RunecraftObjs.daeyaltEssence, daeyaltCount, 1.5))
                }
            }
        }

    private data class EssenceInput(val type: ObjType, val count: Int, val xpMultiplier: Double)
}
