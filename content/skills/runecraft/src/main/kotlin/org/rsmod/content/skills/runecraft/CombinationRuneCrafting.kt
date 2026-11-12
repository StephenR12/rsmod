package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.front
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.content.skills.magic.spell.utility.standard.MagicImbueStateRepository
import org.rsmod.game.type.obj.ObjType

public class CombinationRuneCrafting
@Inject
constructor(
    private val magicImbue: MagicImbueStateRepository,
    private val bindingNecklace: BindingNecklaceRepository,
) {
    public fun craft(access: ProtectedAccess, recipe: CombinationRuneRecipe, selectedSlot: Int) {
        with(access) {
            val selected = player.inv[selectedSlot] ?: return
            val selectedIsRune = selected.id == recipe.inputRune.id
            val selectedIsTalisman = recipe.talisman?.id == selected.id
            if (!selectedIsRune && !selectedIsTalisman) {
                return
            }
            if (!canCraft(recipe)) {
                return
            }

            val imbued = magicImbue.isActive(player)
            if (recipe.aether && !imbued) {
                mes("You need Magic Imbue to be active to craft aether runes.")
                return
            }
            val necklaceActive = player.front?.id == objs.binding_necklace.id
            if (recipe.aether && !necklaceActive) {
                mes("You need to wear a Binding necklace to craft aether runes.")
                return
            }
            val consumeTalisman = !recipe.aether && (selectedIsTalisman || !imbued)
            val talisman = recipe.talisman
            if (consumeTalisman && talisman != null && !player.inv.contains(talisman)) {
                mes("You need a ${recipe.talismanName} to bind ${recipe.name} runes.")
                return
            }
            val inputCount = invTotal(player.inv, recipe.inputRune)
            if (inputCount <= 0) {
                mes("You need ${recipe.inputRuneName}s to bind ${recipe.name} runes.")
                return
            }

            val essenceCounts = availableEssence(recipe)
            val essenceTotal = essenceCounts.sumOf { it.second }
            if (essenceTotal <= 0) {
                mes("You need pure essence to bind ${recipe.name} runes.")
                return
            }
            val catalystCount =
                if (recipe.aether) invTotal(player.inv, RunecraftObjs.aetherCatalyst)
                else Int.MAX_VALUE
            if (recipe.aether && catalystCount <= 0) {
                mes("You need aether catalysts to bind aether runes.")
                return
            }

            val attempts = min(min(inputCount, essenceTotal), catalystCount)
            val successes =
                if (necklaceActive) attempts else (0 until attempts).count { random.of(2) == 0 }
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, recipe.inputRune.id, attempts)
                    var essenceRemaining = attempts
                    for ((essence, count) in essenceCounts) {
                        val consumed = min(essenceRemaining, count)
                        if (consumed > 0) {
                            delete(inventory, essence.id, consumed)
                            essenceRemaining -= consumed
                        }
                    }
                    if (consumeTalisman && talisman != null) {
                        val talismanSlot = if (selectedIsTalisman) selectedSlot else null
                        delete(inventory, talisman.id, 1, slot = talismanSlot)
                    }
                    if (recipe.aether && successes > 0) {
                        delete(inventory, RunecraftObjs.aetherCatalyst.id, successes)
                    }
                    if (successes > 0) {
                        add(inventory, recipe.output.id, successes)
                    }
                }
            check(transaction.success) {
                "Combination-rune transaction failed after validation: $transaction"
            }

            anim(RunecraftSeqs.craft)
            spotanim(RunecraftSpotanims.craft)
            if (successes > 0) {
                statAdvance(stats.runecrafting, recipe.xp * successes)
            }
            sendOutcome(recipe, attempts, successes)
            if (necklaceActive && bindingNecklace.consume(player)) {
                player.front = null
                mes("Your Binding necklace has disintegrated.")
            }
        }
    }

    public fun checkNecklace(access: ProtectedAccess) {
        val charges = bindingNecklace.remaining(access.player)
        val amount = if (charges == 1) "one" else charges.toString()
        val noun = if (charges == 1) "charge" else "charges"
        access.mes("You have $amount $noun left before your Binding necklace disintegrates.")
    }

    public fun resetNecklaceCharges(player: org.rsmod.game.entity.Player) {
        bindingNecklace.reset(player)
    }

    private fun ProtectedAccess.canCraft(recipe: CombinationRuneRecipe): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to craft combination runes.")
            return false
        }
        if (player.stat(stats.runecrafting) < recipe.level) {
            mes(
                "You need a Runecrafting level of at least ${recipe.level} to craft " +
                    "${recipe.name} runes."
            )
            return false
        }
        return true
    }

    private fun ProtectedAccess.availableEssence(
        recipe: CombinationRuneRecipe
    ): List<Pair<ObjType, Int>> {
        val types =
            if (recipe.aether) {
                listOf(RunecraftObjs.pureEssence, RunecraftObjs.daeyaltEssence)
            } else {
                listOf(RunecraftObjs.pureEssence)
            }
        return types.map { it to invTotal(player.inv, it) }.filter { it.second > 0 }
    }

    private fun ProtectedAccess.sendOutcome(
        recipe: CombinationRuneRecipe,
        attempts: Int,
        successes: Int,
    ) {
        val message =
            when {
                successes == attempts -> "You bind the temple's power into ${recipe.name} runes."
                successes > 0 ->
                    "You partially succeed to bind the temple's power into ${recipe.name} runes."
                else -> "You fail to bind the temple's power into ${recipe.name} runes."
            }
        mes(message)
    }
}
