package org.rsmod.content.skills.runecraft

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

public data class KourendRuneRecipe(
    val altar: LocType,
    val output: ObjType,
    val level: Int,
    val xp: Double,
    val name: String,
    val bloodEssence: Boolean,
)

public object KourendRuneRecipes {
    public val blood: KourendRuneRecipe =
        KourendRuneRecipe(
            altar = RunecraftLocs.kourendBloodAltar,
            output = objs.blood_rune,
            level = 77,
            xp = 23.8,
            name = "blood",
            bloodEssence = true,
        )
    public val soul: KourendRuneRecipe =
        KourendRuneRecipe(
            altar = RunecraftLocs.kourendSoulAltar,
            output = objs.soul_rune,
            level = 90,
            xp = 29.7,
            name = "soul",
            bloodEssence = false,
        )
    public val all: List<KourendRuneRecipe> = listOf(blood, soul)
}

/** The dense-to-dark-to-rune production path at the cache-233.4 cutoff. */
public class KourendRuneCrafting
@Inject
constructor(
    private val modifiers: RunecraftOutputModifiers,
    private val bloodEssence: BloodEssenceLifecycle,
) {
    public fun startVenerate(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to venerate dense essence blocks.")
                return
            }
            if (invTotal(player.inv, RunecraftObjs.denseEssenceBlock) == 0) {
                mes("You do not have any dense essence blocks to venerate.")
                return
            }
            clearWeakQueue(RunecraftQueues.darkAltarVenerate)
            anim(RunecraftSeqs.craft)
            weakQueue(RunecraftQueues.darkAltarVenerate, VENERATE_TICKS)
        }
    }

    public fun finishVenerate(access: ProtectedAccess) {
        with(access) {
            if (!player.members) {
                return
            }
            val blocks =
                player.inv.indices.mapNotNull { slot ->
                    val obj = player.inv[slot]
                    if (obj?.id == RunecraftObjs.denseEssenceBlock.id) slot to obj.count else null
                }
            if (blocks.isEmpty()) {
                return
            }
            val count = blocks.sumOf { it.second }
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    for ((slot, quantity) in blocks) {
                        delete(inventory, RunecraftObjs.denseEssenceBlock.id, quantity, slot = slot)
                        add(inventory, RunecraftObjs.darkEssenceBlock.id, quantity, slot = slot)
                    }
                }
            check(transaction.success) {
                "Dark Altar transaction failed after validation: $transaction"
            }
            val prayerDrain = min(player.stat(stats.prayer), count)
            if (prayerDrain > 0) {
                player.statSub(stats.prayer, constant = prayerDrain, percent = 0)
            }
            statAdvance(stats.runecrafting, count * VENERATE_XP)
        }
    }

    public fun chisel(access: ProtectedAccess, blockSlot: Int) {
        with(access) {
            if (player.inv[blockSlot]?.id != RunecraftObjs.darkEssenceBlock.id) {
                return
            }
            if (!player.members) {
                mes("You need to be on a members' world to chip dark essence blocks.")
                return
            }
            if (player.stat(stats.crafting) < CHISEL_LEVEL) {
                mes("You need a Crafting level of at least 38 to chip dark essence blocks.")
                return
            }
            if (invTotal(player.inv, objs.chisel) == 0) {
                mes("You need a chisel to chip dark essence blocks.")
                return
            }
            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(inventory, RunecraftObjs.darkEssenceBlock.id, 1, slot = blockSlot)
                    add(
                        inventory,
                        RunecraftObjs.darkEssenceFragments.id,
                        FRAGMENTS_PER_BLOCK,
                        slot = blockSlot,
                        counted = true,
                    )
                }
            check(transaction.success) {
                "Dark essence chiseling transaction failed after validation: $transaction"
            }
            anim(RunecraftSeqs.chiselDarkEssence)
            statAdvance(stats.crafting, CHISEL_XP)
        }
    }

    public fun craft(access: ProtectedAccess, recipe: KourendRuneRecipe) {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to craft ${recipe.name} runes.")
                return
            }
            if (player.stat(stats.runecrafting) < recipe.level) {
                mes(
                    "You need a Runecrafting level of at least ${recipe.level} to craft ${recipe.name} runes."
                )
                return
            }
            val fragments = invTotal(player.inv, RunecraftObjs.darkEssenceFragments)
            if (fragments == 0) {
                mes("You do not have any dark essence fragments to bind.")
                return
            }

            val extractCount =
                modifiers.applicableExtractCount(this, fragments, RunecraftObjs.scarredExtract)
            val outfitRunes = modifiers.raimentsBonus(this, fragments)
            val diaryRunes =
                if (
                    recipe == KourendRuneRecipes.blood &&
                        vars[varbits.kourend_diary_elite_complete] > 0
                ) {
                    fragments / 10
                } else {
                    0
                }
            val bloodEssencePlan =
                if (recipe.bloodEssence) {
                    bloodEssence.planBonus(player.inv, fragments, random)
                } else {
                    null
                }
            val outputCount =
                fragments +
                    (extractCount * EXTRACT_BONUS) +
                    outfitRunes +
                    diaryRunes +
                    (bloodEssencePlan?.bonusRunes ?: 0)

            val transaction =
                player.invTransaction(player.inv) {
                    val inventory = select(player.inv)
                    delete(
                        inventory,
                        RunecraftObjs.darkEssenceFragments.id,
                        fragments,
                        counted = true,
                    )
                    if (extractCount > 0) {
                        delete(inventory, RunecraftObjs.scarredExtract.id, extractCount)
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
                "Kourend rune altar transaction failed after validation: $transaction"
            }

            anim(RunecraftSeqs.craft)
            spotanim(RunecraftSpotanims.craft)
            statAdvance(stats.runecrafting, fragments * recipe.xp)
            mes("You bind the temple's power into ${recipe.name} runes.")
        }
    }

    public companion object {
        public const val VENERATE_TICKS: Int = 3
        public const val VENERATE_XP: Double = 2.5
        public const val CHISEL_LEVEL: Int = 38
        public const val CHISEL_XP: Double = 8.0
        public const val FRAGMENTS_PER_BLOCK: Int = 4
        public const val EXTRACT_BONUS: Int = 60
    }
}
