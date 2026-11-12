package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.back
import org.rsmod.api.player.hands
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.smithing.smelting.SmeltingRecipe
import org.rsmod.content.skills.smithing.smelting.SmeltingRecipeRegistry
import org.rsmod.content.skills.smithing.smelting.SmeltingUnlock
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class SuperheatItemSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val recipes: SmeltingRecipeRegistry,
    private val objTypes: ObjTypeList,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, targetSlot: Int, allowQueue: Boolean = true) {
        with(access) {
            val target = player.inv[targetSlot] ?: return
            if (!cooldowns.isReady(player, mapClock)) {
                if (allowQueue && superheat_queues.cast !in player.queueList) {
                    queue(
                        superheat_queues.cast,
                        cooldowns.cyclesUntilReady(player, mapClock),
                        SuperheatCastRequest(targetSlot, target.id),
                    )
                }
                return
            }

            val targetType = objTypes[target]
            if (targetType.id == objs.elemental_ore.id) {
                fail("Even this spell is not hot enough to heat this item.")
                return
            }
            if (targetType.isCert) {
                fail("You need to cast superheat item on ore.")
                return
            }
            if (targetType.id == objs.jogre_bones.id) {
                castOnJogreBones(targetSlot)
                return
            }

            val candidates = recipes.forTarget(targetType)
            if (candidates.isEmpty()) {
                fail("You need to cast superheat item on ore.")
                return
            }

            val catalysed = objs.smithing_catalyst in player.inv
            val recipe = resolveRecipe(candidates, catalysed)
            if (!player.isUnlocked(recipe)) {
                mes(
                    "You need to complete ${recipe.unlock?.questName} before you can smelt this ore."
                )
                return
            }
            if (player.stat(stats.smithing) < recipe.level) {
                mes("You need a Smithing level of ${recipe.level} to smelt this ore.")
                return
            }
            val missingOre =
                recipe.oreCosts.entries.firstOrNull { (ore, count) ->
                    player.inv.count(objTypes[ore]) < count
                }
            if (missingOre != null) {
                mes("You do not have enough ore to make a ${recipe.name.lowercase()}.")
                return
            }

            val coalCount = recipe.coalCount(catalysed)
            val looseCoal = player.inv.count(objTypes[objs.coal])
            val bagCoal = player.availableBagCoal()
            if (looseCoal + bagCoal < coalCount) {
                mes("You do not have enough coal to smelt this ore.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_superheat))
            if (!runes.canCastSpell(player, spell)) {
                return
            }

            val looseCoalUse = min(coalCount, looseCoal)
            val bagCoalUse = coalCount - looseCoalUse
            val preflight =
                createTransaction(
                    recipe,
                    targetSlot,
                    targetType,
                    looseCoalUse,
                    catalysed,
                    autoCommit = false,
                )
            if (!preflight.success) {
                return
            }
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val committed =
                createTransaction(
                    recipe,
                    targetSlot,
                    targetType,
                    looseCoalUse,
                    catalysed,
                    autoCommit = true,
                )
            check(committed.success) { "Superheat transaction failed after validation: $recipe" }
            if (bagCoalUse > 0) {
                vars[varbits.coal_bag_storage_count] = bagCoal - bagCoalUse
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            statAdvance(stats.smithing, smithingXp(recipe, catalysed))
            anim(seqs.human_castsuperheatitem)
            spotanim(spotanims.superheatitem_casting)
            soundSynth(synths.superheat_all)
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    private fun ProtectedAccess.castOnJogreBones(targetSlot: Int) {
        if (player.statBase(stats.cooking) < JOGRE_COOKING_LEVEL) {
            mes("You need a Cooking level of 30 to heat Jogre bones.")
            return
        }
        val spell = requireNotNull(spells.getObjSpell(objs.spell_superheat))
        if (!runes.canCastSpell(player, spell)) {
            return
        }
        val preflight = createJogreTransaction(targetSlot, autoCommit = false)
        if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
            return
        }
        val committed = createJogreTransaction(targetSlot, autoCommit = true)
        check(committed.success) { "Jogre-bone Superheat transaction failed after validation." }

        if (vars[varbits.fountain_of_rune_active] == 0) {
            statAdvance(stats.magic, spell.castXp)
        }
        statAdvance(stats.cooking, JOGRE_COOKING_XP)
        anim(seqs.human_castsuperheatitem)
        spotanim(spotanims.superheatitem_casting)
        soundSynth(synths.superheat_all)
        cooldowns.markCast(player, mapClock + CAST_RATE)
    }

    private fun ProtectedAccess.createTransaction(
        recipe: SmeltingRecipe,
        targetSlot: Int,
        targetType: ObjType,
        looseCoalUse: Int,
        catalysed: Boolean,
        autoCommit: Boolean,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            for ((ore, count) in recipe.oreCosts) {
                val targetCount = if (ore.id == targetType.id) 1 else 0
                if (targetCount > 0) {
                    delete(inventory, ore.id, targetCount, slot = targetSlot)
                }
                if (count > targetCount) {
                    delete(inventory, ore.id, count - targetCount)
                }
            }
            if (looseCoalUse > 0) {
                delete(inventory, objs.coal.id, looseCoalUse)
            }
            if (catalysed && recipe.catalystEligible) {
                delete(inventory, objs.smithing_catalyst.id, 1)
            }
            add(inventory, recipe.output.id, 1)
        }

    private fun ProtectedAccess.createJogreTransaction(targetSlot: Int, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            delete(inventory, objs.jogre_bones.id, 1, slot = targetSlot)
            add(inventory, objs.burnt_jogre_bones.id, 1)
        }

    private fun ProtectedAccess.resolveRecipe(
        candidates: List<SmeltingRecipe>,
        catalysed: Boolean,
    ): SmeltingRecipe {
        val steel = candidates.firstOrNull { it.output.id == objs.steel_bar.id }
        val canMakeSteel =
            steel != null &&
                player.stat(stats.smithing) >= steel.level &&
                player.availableCoal() >= steel.coalCount(catalysed)
        return if (canMakeSteel) steel else candidates.minBy(SmeltingRecipe::level)
    }

    private fun Player.availableCoal(): Int = inv.count(objTypes[objs.coal]) + availableBagCoal()

    private fun Player.isUnlocked(recipe: SmeltingRecipe): Boolean =
        when (val unlock = recipe.unlock) {
            null -> true
            is SmeltingUnlock.Varp -> vars[unlock.type] >= unlock.minimumProgress
            is SmeltingUnlock.VarBit -> vars[unlock.type] >= unlock.minimumProgress
        }

    private fun Player.availableBagCoal(): Int {
        val hasBag = inv.any { it?.id == objs.coal_bag.id || it?.id == objs.coal_bag_open.id }
        return if (hasBag) vars[varbits.coal_bag_storage_count] else 0
    }

    private fun Player.hasGoldXpEquipment(): Boolean {
        val gloves = hands?.id == objs.goldsmith_gauntlets.id
        val cape =
            back?.id in
                setOf(
                    objs.smithing_skillcape.id,
                    objs.smithing_skillcape_t.id,
                    objs.max_cape_worn.id,
                )
        return gloves || cape
    }

    private fun ProtectedAccess.smithingXp(recipe: SmeltingRecipe, catalysed: Boolean): Double {
        val base =
            if (recipe.output.id == objs.gold_bar.id && player.hasGoldXpEquipment()) {
                GOLDSMITH_XP
            } else {
                recipe.xp
            }
        return if (catalysed && recipe.catalystEligible) base * 2 else base
    }

    private fun ProtectedAccess.fail(message: String) {
        mes(message)
        anim(seqs.superheatitem_casting)
        spotanim(spotanims.superheatitem_casting)
        soundSynth(synths.superheat_fail)
        cooldowns.markCast(player, mapClock + CAST_RATE)
    }

    private companion object {
        const val CAST_RATE = 3
        const val GOLDSMITH_XP = 56.2
        const val JOGRE_COOKING_XP = 25.0
        const val JOGRE_COOKING_LEVEL = 30
    }
}
