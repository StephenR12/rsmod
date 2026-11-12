package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

public class BoltEnchantSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public suspend fun select(access: ProtectedAccess) {
        with(access) {
            val input =
                skillMulti(
                    title = MENU_TITLE,
                    choices = menuProductIcons(),
                    maxQuantity = maximumSelectableSets(),
                    actionMode = ENCHANT_ACTION_MODE,
                )
            val recipe = BoltEnchantRecipe.forButton(input.component) ?: return
            startBatch(this, recipe, input.subcomponent)
        }
    }

    public fun startBatch(access: ProtectedAccess, recipe: BoltEnchantRecipe, sets: Int) {
        require(sets >= 1) { "Bolt-enchantment set count must be positive." }
        with(access) {
            clearQueue(bolt_enchant_queues.batch)
            if (cooldowns.isReady(player, mapClock)) {
                castAndContinue(this, recipe, sets)
            } else {
                queue(
                    bolt_enchant_queues.batch,
                    cooldowns.cyclesUntilReady(player, mapClock),
                    BoltEnchantRequest(recipe.spell, sets),
                )
            }
        }
    }

    internal fun castQueued(access: ProtectedAccess, request: BoltEnchantRequest) {
        val recipe = BoltEnchantRecipe.forSpell(request.spell) ?: return
        castAndContinue(access, recipe, request.remainingSets)
    }

    private fun castAndContinue(
        access: ProtectedAccess,
        recipe: BoltEnchantRecipe,
        remainingSets: Int,
    ) {
        if (!cast(access, recipe)) {
            return
        }
        if (remainingSets > 1 && access.hasSource(recipe)) {
            access.queue(
                bolt_enchant_queues.batch,
                AUTO_CAST_RATE,
                BoltEnchantRequest(recipe.spell, remainingSets - 1),
            )
        }
    }

    private fun cast(access: ProtectedAccess, recipe: BoltEnchantRecipe): Boolean {
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return false
            }

            val transformation = findTransformation(recipe)
            if (transformation == null) {
                mes("You do not have any bolts of that type to enchant.")
                return false
            }
            val spell = requireNotNull(spells.getObjSpell(recipe.spell))
            if (!runes.canCastSpell(player, spell)) {
                return false
            }

            val preflight = transform(transformation, autoCommit = false)
            if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
                return false
            }
            val committed = transform(transformation, autoCommit = true)
            check(committed.success) { "Bolt-enchantment transaction failed: $transformation" }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_xbow_enchant_arrowtip)
            spotanim(spotanims.xbows_enchanting_tips, height = GRAPHIC_HEIGHT)
            soundSynth(synths.enchanted_tipping)
            mes(SUCCESS_MESSAGE)
            cooldowns.markCast(player, mapClock + MANUAL_CAST_RATE)
            return true
        }
    }

    private fun ProtectedAccess.findTransformation(recipe: BoltEnchantRecipe): Transformation? {
        val regularCount = invCount(recipe.regularSource)
        if (regularCount > 0) {
            return Transformation(
                recipe.regularSource,
                recipe.regularOutput,
                min(BOLTS_PER_SET, regularCount),
            )
        }
        val dragonCount = invCount(recipe.dragonSource)
        if (dragonCount > 0) {
            return Transformation(
                recipe.dragonSource,
                recipe.dragonOutput,
                min(BOLTS_PER_SET, dragonCount),
            )
        }
        return null
    }

    private fun ProtectedAccess.transform(transformation: Transformation, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            delete(inventory, transformation.source.id, count = transformation.count)
            add(inventory, transformation.output.id, count = transformation.count)
        }

    private fun ProtectedAccess.hasSource(recipe: BoltEnchantRecipe): Boolean =
        invCount(recipe.regularSource) > 0 || invCount(recipe.dragonSource) > 0

    private fun ProtectedAccess.maximumSelectableSets(): Int =
        max(
            1,
            BoltEnchantRecipe.entries.maxOf { recipe ->
                invCount(recipe.regularSource).sets() + invCount(recipe.dragonSource).sets()
            },
        )

    private fun ProtectedAccess.menuProductIcons(): List<ObjType> {
        val hasRegularBolts =
            BoltEnchantRecipe.entries.any { recipe -> invCount(recipe.regularSource) > 0 }
        val hasDragonBolts =
            BoltEnchantRecipe.entries.any { recipe -> invCount(recipe.dragonSource) > 0 }
        return BoltEnchantRecipe.entries.map { recipe ->
            if (!hasRegularBolts && hasDragonBolts) recipe.dragonOutput else recipe.regularOutput
        }
    }

    private fun ProtectedAccess.invCount(obj: ObjType): Int = inv.count(objTypes[obj])

    private fun Int.sets(): Int = (this + BOLTS_PER_SET - 1) / BOLTS_PER_SET

    private data class Transformation(val source: ObjType, val output: ObjType, val count: Int)

    private companion object {
        const val ENCHANT_ACTION_MODE = 19
        const val BOLTS_PER_SET = 10
        const val MANUAL_CAST_RATE = 1
        const val AUTO_CAST_RATE = 3
        const val GRAPHIC_HEIGHT = 100
        const val MENU_TITLE = "How many sets of bolts to enchant?"
        const val SUCCESS_MESSAGE =
            "The magic of the runes coaxes out the true nature of the gem tips."
    }
}
