package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
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
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.varbit.VarBitType

public data class PlankMakeRecipe(
    val input: ObjType,
    val output: ObjType,
    val fee: Int,
    val confirmation: VarBitType,
)

public object PlankMakeRecipes {
    public val all: List<PlankMakeRecipe> =
        listOf(
            PlankMakeRecipe(objs.logs, objs.plank, fee = 70, varbits.plank_make_plain_confirmation),
            PlankMakeRecipe(
                objs.oak_logs,
                objs.oak_plank,
                fee = 175,
                varbits.plank_make_oak_confirmation,
            ),
            PlankMakeRecipe(
                objs.teak_logs,
                objs.teak_plank,
                fee = 350,
                varbits.plank_make_teak_confirmation,
            ),
            PlankMakeRecipe(
                objs.mahogany_logs,
                objs.mahogany_plank,
                fee = 1_050,
                varbits.plank_make_mahogany_confirmation,
            ),
        )

    public val byInput: Map<Int, PlankMakeRecipe> = all.associateBy { it.input.id }
}

public class PlankMakeSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess, targetSlot: Int, allowQueue: Boolean = true) {
        with(access) {
            val target = player.inv[targetSlot] ?: return
            val recipe = PlankMakeRecipes.byInput[target.id]
            if (recipe == null) {
                mes("You can only cast this spell on logs.")
                return
            }
            clearWeakQueue(plank_make_queues.batch)
            if (!cooldowns.isReady(player, mapClock)) {
                if (allowQueue && plank_make_queues.cast !in player.queueList) {
                    queue(
                        plank_make_queues.cast,
                        cooldowns.cyclesUntilReady(player, mapClock),
                        PlankMakeCastRequest(targetSlot, target.id),
                    )
                }
                return
            }
            if (!canAccess(sendFailure = true)) {
                return
            }
            val next = nextLog() ?: return
            if (!convert(next.slot, next.recipe)) {
                return
            }
            if (nextLog() != null) {
                weakQueue(plank_make_queues.batch, FIRST_AUTO_DELAY)
            }
        }
    }

    public fun process(access: ProtectedAccess) {
        with(access) {
            if (!canAccess(sendFailure = false)) {
                return
            }
            val next = nextLog() ?: return
            if (!convert(next.slot, next.recipe)) {
                return
            }
            if (nextLog() != null) {
                weakQueue(plank_make_queues.batch, REPEAT_AUTO_DELAY)
            }
        }
    }

    private fun ProtectedAccess.convert(targetSlot: Int, recipe: PlankMakeRecipe): Boolean {
        val current = player.inv[targetSlot] ?: return false
        if (current.id != recipe.input.id) {
            return false
        }
        if (player.inv.count(objTypes[objs.coins]) < recipe.fee) {
            mes("You need ${recipe.fee} coins to convert that log into a plank.")
            return false
        }
        val spell = requireNotNull(spells.getObjSpell(objs.spell_plank_make))
        if (!runes.canCastSpell(player, spell)) {
            return false
        }

        val useVoucher = objs.sawmill_voucher in player.inv && player.inv.freeSpace() > 0
        val preflight = transform(targetSlot, recipe, useVoucher, autoCommit = false)
        if (!preflight.success || runes.attemptCast(player, spell).isFailure()) {
            return false
        }
        val committed = transform(targetSlot, recipe, useVoucher, autoCommit = true)
        check(committed.success) { "Plank Make transaction failed after validation: $recipe" }

        if (vars[varbits.fountain_of_rune_active] == 0) {
            statAdvance(stats.magic, spell.castXp)
        }
        anim(seqs.dream_player_make_plank_spell)
        spotanim(spotanims.dream_plank_make_spell_spotanim)
        soundSynth(synths.lunar_make_plank)
        cooldowns.markCast(player, mapClock + MANUAL_CAST_RATE)
        return true
    }

    private fun ProtectedAccess.transform(
        targetSlot: Int,
        recipe: PlankMakeRecipe,
        useVoucher: Boolean,
        autoCommit: Boolean,
    ) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            delete(inventory, recipe.input.id, count = 1, slot = targetSlot)
            delete(inventory, objs.coins.id, count = recipe.fee)
            if (useVoucher) {
                delete(inventory, objs.sawmill_voucher.id, count = 1)
            }
            add(inventory, recipe.output.id, count = if (useVoucher) 2 else 1)
        }

    private fun ProtectedAccess.nextLog(): PendingLog? =
        player.inv.indices.firstNotNullOfOrNull { slot ->
            val obj = player.inv[slot] ?: return@firstNotNullOfOrNull null
            val recipe = PlankMakeRecipes.byInput[obj.id] ?: return@firstNotNullOfOrNull null
            PendingLog(slot, recipe)
        }

    private fun ProtectedAccess.canAccess(sendFailure: Boolean): Boolean {
        if (!player.members) {
            if (sendFailure) {
                mes("You need to be on a members' world to cast this spell.")
            }
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            if (sendFailure) {
                mes("You need to complete Lunar Diplomacy to cast this spell.")
            }
            return false
        }
        if (vars[varbits.dream_mentor] < DREAM_MENTOR_COMPLETE) {
            if (sendFailure) {
                mes("You need to complete Dream Mentor to cast this spell.")
            }
            return false
        }
        return true
    }

    private data class PendingLog(val slot: Int, val recipe: PlankMakeRecipe)

    private companion object {
        const val MANUAL_CAST_RATE = 3
        const val FIRST_AUTO_DELAY = 5
        const val REPEAT_AUTO_DELAY = 7
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val DREAM_MENTOR_COMPLETE = 28
    }
}
