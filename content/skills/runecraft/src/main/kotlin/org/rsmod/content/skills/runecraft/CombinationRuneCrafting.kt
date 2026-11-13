package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import org.rsmod.content.skills.pets.RiftGuardian
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.front
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.spells.MagicImbueStateRepository
import org.rsmod.game.type.obj.ObjType

public class CombinationRuneCrafting
@Inject
internal constructor(
    private val guardian: RiftGuardian,
    private val rift:RiftRound,private val imbuing:RiftImbuing,
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

            val temple = riftTemples.firstOrNull { it.recipe.altar==recipe.altar && rift.playingAltar(player,it.recipe) }
            val essenceCounts = if(temple==null)availableEssence(recipe) else listOf(RiftItems.essence to invTotal(inv,RiftItems.essence))
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
                    if(temple!=null)imbuing.cellToCharge(this@with,temple)?.let {cell->
                        delete(inventory,cell.id,1);add(inventory,RiftItems.cells[temple.cellTier].id,1)
                    }
                    if (consumeTalisman && talisman != null) {
                        val talismanSlot = if (selectedIsTalisman) selectedSlot else null
                        delete(inventory, talisman.id, 1, slot = talismanSlot)
                    }
                    if (recipe.aether && successes > 0) {
                        delete(inventory, RunecraftObjs.aetherCatalyst.id, successes)
                    }
                    if (successes > 0) {
                        val bonus=if(temple==null)0 else imbuing.lanternRunes(this@with,temple,successes)
                        add(inventory, recipe.output.id, successes+bonus)
                        if(temple!=null)add(inventory,RiftItems.stones[if(recipe.aether)3 else 2].id,successes)
                    }
                }
            if(temple!=null && !transaction.success){mes("You need more inventory space to imbue your essence.");return}
            if(temple!=null)imbuing.rollTalisman(this,temple,attempts)?.let {invAdd(inv,it)}
            check(transaction.success) {
                "Combination-rune transaction failed after validation: $transaction"
            }

            anim(RunecraftSeqs.craft)
            spotanim(RunecraftSpotanims.craft)
            if (successes > 0) {
                if (temple == null) {
                    val altar = StandardRuneRecipes.all.firstOrNull { it.altar == recipe.altar }
                    if (altar != null) guardian.craft(player,altar.name,successes)
                }
                statAdvance(stats.runecrafting, recipe.xp * successes)
                if (recipe.output == objs.lava_rune && recipe.altar == RunecraftLocs.fireAltar) {
                    player.completeDiaryTask(DiaryTask.CraftLavaAtFireAltar)
                }
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
