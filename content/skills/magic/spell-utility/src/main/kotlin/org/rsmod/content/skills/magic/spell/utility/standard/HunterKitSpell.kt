package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.constants
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
import org.rsmod.game.inv.isType
import org.rsmod.game.type.obj.ObjType

public class HunterKitSpell
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val cooldowns: UtilitySpellCooldownRepository,
) {
    public fun cast(access: ProtectedAccess) {
        with(access) {
            if (!cooldowns.isReady(player, mapClock) || !canAccess()) {
                return
            }
            if (player.inv.contains(objs.hunter_kit) || hasCompleteKitContents()) {
                mes(ALREADY_HAS_EQUIPMENT_MESSAGE)
                return
            }

            val spell = requireNotNull(spells.getObjSpell(objs.spell_hunter_kit))
            val preflight = invAdd(player.inv, objs.hunter_kit, autoCommit = false)
            if (!preflight.success) {
                mes(constants.dm_invspace)
                return
            }
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            val committed = invAdd(player.inv, objs.hunter_kit)
            check(committed.success) { "Hunter Kit transaction failed after validation." }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.dream_player_hunter_box_spell)
            spotanim(spotanims.dream_hunter_spell)
            soundSynth(synths.lunar_hunter_kit)
            cooldowns.markCast(player, mapClock + CAST_RATE)
        }
    }

    public fun open(access: ProtectedAccess, slot: Int) {
        with(access) {
            if (!player.inv[slot].isType(objs.hunter_kit)) {
                return
            }

            val preflight = unpack(slot, autoCommit = false)
            if (!preflight.success) {
                mes(constants.dm_invspace)
                return
            }
            val committed = unpack(slot, autoCommit = true)
            check(committed.success) { "Hunter kit unpack transaction failed after validation." }
            soundSynth(synths.lunar_hunter_kit_inv)
        }
    }

    private fun ProtectedAccess.hasCompleteKitContents(): Boolean =
        HunterKitContents.all.all(player.inv::contains)

    private fun ProtectedAccess.unpack(slot: Int, autoCommit: Boolean) =
        player.invTransaction(player.inv, autoCommit = autoCommit) {
            val inventory = select(player.inv)
            delete(inventory, objs.hunter_kit.id, count = 1, slot = slot)
            for (content in HunterKitContents.all) {
                add(inventory, content.id, count = 1)
            }
        }

    private companion object {
        const val CAST_RATE = 3
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val DREAM_MENTOR_COMPLETE = 28
        const val ALREADY_HAS_EQUIPMENT_MESSAGE =
            "Why do you want to create a kit? You already have all the Hunter equipment it contains."
    }

    private fun ProtectedAccess.canAccess(): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        if (vars[varbits.dream_mentor] < DREAM_MENTOR_COMPLETE) {
            mes("You need to complete Dream Mentor to cast this spell.")
            return false
        }
        return true
    }
}

public object HunterKitContents {
    public val all: List<ObjType> =
        listOf(
            objs.noose_wand,
            objs.butterfly_net,
            objs.bird_snare,
            objs.rabbit_snare,
            objs.teasing_stick,
            objs.unlit_torch,
            objs.impling_jar,
            objs.box_trap,
        )
}
