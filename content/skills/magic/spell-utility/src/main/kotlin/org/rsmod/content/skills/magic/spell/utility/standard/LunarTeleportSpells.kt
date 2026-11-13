package org.rsmod.content.skills.magic.spell.utility.standard

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

/** Cache-233 behavior for the fixed-destination Lunar self teleports. */
public class LunarTeleportSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
) {
    public suspend fun cast(access: ProtectedAccess, spellType: ObjType) {
        require(spellType.id in supportedSpellIds) { "Unsupported Lunar teleport: $spellType" }
        with(access) {
            val spell = requireNotNull(spells.getObjSpell(spellType))
            check(spell.type == MagicSpellType.Teleport) { "Expected teleport spell: $spell" }

            if (!canAccess(spellType) || !runes.canCastSpell(player, spell)) {
                return
            }
            stopAction()
            if (spellType.id == objs.spell_lunarghorrock_teleport.id && !confirmDeepWilderness()) {
                return
            }
            if (!canAccess(spellType) || runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.lunar_teleport)
            spotanim(spotanims.lunar_teleport_spotanim)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(destination(spellType))
            when (spellType.id) {
                objs.spell_waterbirth_teleport.id ->
                    player.completeDiaryTask(DiaryTask.TeleportToWaterbirth)
                objs.spell_catherby_teleport.id ->
                    player.completeDiaryTask(DiaryTask.TeleportToCatherby)
            }
        }
    }

    private suspend fun ProtectedAccess.confirmDeepWilderness(): Boolean =
        menu(
            title = "Deep Wilderness",
            hotkeys = false,
            choices = listOf("Teleport to the Ice Plateau.", "Cancel."),
        ) == 0

    private fun ProtectedAccess.canAccess(spellType: ObjType): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
            mes("You need to complete Lunar Diplomacy to cast this spell.")
            return false
        }
        if (spellType.id == objs.spell_zmi_teleport.id && vars[varbits.rc_zmi_lunar_spell] == 0) {
            return false
        }
        if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
            mes("A mysterious force prevents you from teleporting!")
            return false
        }
        if (player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return false
        }
        return true
    }

    private fun destination(spellType: ObjType): CoordGrid =
        objTypes[spellType].param(params.spell_telecoord)

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    private companion object {
        const val CAST_TICKS = 4
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val MAX_WILDERNESS_LEVEL = 20

        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920

        val supportedSpellIds =
            setOf(
                objs.spell_moonclan_teleport.id,
                objs.spell_zmi_teleport.id,
                objs.spell_waterbirth_teleport.id,
                objs.spell_barboutpost_teleport.id,
                objs.spell_portkhazard_teleport.id,
                objs.spell_fishguild_teleport.id,
                objs.spell_catherby_teleport.id,
                objs.spell_lunarghorrock_teleport.id,
            )
    }
}
