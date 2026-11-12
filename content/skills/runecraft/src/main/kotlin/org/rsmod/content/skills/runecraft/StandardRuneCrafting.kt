package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.game.type.obj.ObjType

public class StandardRuneCrafting
@Inject
constructor(
    private val bloodEssence: BloodEssenceLifecycle,
    private val modifiers: RunecraftOutputModifiers,
) {
    public fun craft(access: ProtectedAccess, recipe: StandardRuneRecipe) {
        with(access) {
            val visibleLevel = player.stat(stats.runecrafting)
            if (!checkRequirements(recipe, visibleLevel)) {
                return
            }

            val essence = availableEssence(recipe)
            val essenceCount = essence.sumOf { it.count }
            if (essenceCount == 0) {
                mes("You do not have any essence to bind.")
                return
            }

            val multiplier = recipe.outputMultiplier(visibleLevel)
            val baseRunes = essenceCount * multiplier
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
                    (extractCount * recipe.extractBonus) +
                    outfitRunes +
                    (bloodEssencePlan?.bonusRunes ?: 0)
            val xp = essence.sumOf { it.count * recipe.xp * it.xpMultiplier }

            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for (input in essence) {
                        delete(inventory, input.type.id, input.count)
                    }
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
