package org.rsmod.content.skills.magic.spell.utility.standard

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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

/** Shared cache-233 behavior for the fixed-destination Standard teleports. */
public class StandardTeleportSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
) {
    public suspend fun cast(access: ProtectedAccess, spellType: ObjType) {
        require(spellType.id in supportedSpellIds) { "Unsupported Standard teleport: $spellType" }
        with(access) {
            if (!player.members && spellType.id !in freeToPlaySpellIds) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            val unlockFailure = unlockFailure(spellType)
            if (unlockFailure != null) {
                mes(unlockFailure)
                return
            }
            if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
                mes("A mysterious force prevents you from teleporting!")
                return
            }
            if (vars[varbits.teleblock_cycles] > TELEBLOCK_IMMUNITY_CYCLES) {
                mes("A magical force stops you from teleporting.")
                return
            }

            val spell = requireNotNull(spells.getObjSpell(spellType))
            check(spell.type == MagicSpellType.Teleport) { "Expected teleport spell: $spell" }
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            stopAction()
            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.teleport_casting)
            spotanim(spotanims.teleport_casting)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(destination(spellType))
        }
    }

    private fun ProtectedAccess.destination(spellType: ObjType): CoordGrid =
        when (spellType.id) {
            objs.spell_varrock_teleport.id ->
                if (
                    vars[varbits.varrock_diary_medium_complete] != 0 &&
                        vars[varbits.varrock_ge_teleport] != 0
                ) {
                    GRAND_EXCHANGE
                } else {
                    objTypes[spellType].param(params.spell_telecoord)
                }
            objs.spell_camelot_teleport.id ->
                if (
                    vars[varbits.kandarin_diary_hard_complete] != 0 &&
                        vars[varbits.seers_camelot_teleport] != 0
                ) {
                    SEERS_VILLAGE_BANK
                } else {
                    objTypes[spellType].param(params.spell_telecoord)
                }
            objs.spell_watchtower_teleport.id ->
                if (
                    vars[varbits.ardougne_diary_hard_complete] != 0 &&
                        vars[varbits.yanille_teleport_location] != 0
                ) {
                    YANILLE
                } else {
                    objTypes[spellType].param(params.spell_telecoord)
                }
            else -> objTypes[spellType].param(params.spell_telecoord)
        }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    private fun ProtectedAccess.unlockFailure(spellType: ObjType): String? =
        when (spellType.id) {
            objs.spell_kourendcastle_teleport.id ->
                if (vars[varbits.veos_progress] < CLIENT_OF_KOUREND_COMPLETE) {
                    "You must complete Client of Kourend to cast this spell."
                } else {
                    null
                }
            objs.spell_ardougne_teleport.id ->
                if (vars[varps.elenaquest] < ARDOUGNE_TELEPORT_LEARNED) {
                    "You must complete Plague City and read the Ardougne teleport scroll to cast this spell."
                } else {
                    null
                }
            objs.spell_civitas_fortis_teleport.id ->
                if (vars[varbits.vmq2] < TWILIGHTS_PROMISE_COMPLETE) {
                    "You must complete Twilight's Promise to cast this spell."
                } else {
                    null
                }
            objs.spell_watchtower_teleport.id ->
                if (vars[varps.itwatchtower] < WATCHTOWER_TELEPORT_LEARNED) {
                    "You must complete Watchtower and read the spell scroll to cast this spell."
                } else {
                    null
                }
            objs.spell_trollheim_teleport.id ->
                if (vars[varps.eadgar_quest] < EADGARS_RUSE_COMPLETE) {
                    "You must complete Eadgar's Ruse to cast this spell."
                } else {
                    null
                }
            objs.spell_apeatoll_teleport.id ->
                if (vars[varbits.hundred_ilm_quest] < FREEING_KING_AWOWOGEI_COMPLETE) {
                    "You must complete the Freeing King Awowogei subquest to cast this spell."
                } else {
                    null
                }
            else -> null
        }

    private companion object {
        const val CAST_TICKS = 4
        const val MAX_WILDERNESS_LEVEL = 20
        const val TELEBLOCK_IMMUNITY_CYCLES = 100
        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920
        const val CLIENT_OF_KOUREND_COMPLETE = 7
        const val ARDOUGNE_TELEPORT_LEARNED = 30
        const val TWILIGHTS_PROMISE_COMPLETE = 50
        const val WATCHTOWER_TELEPORT_LEARNED = 14
        const val EADGARS_RUSE_COMPLETE = 110
        const val FREEING_KING_AWOWOGEI_COMPLETE = 50

        val GRAND_EXCHANGE = CoordGrid(0, 49, 54, 29, 23)
        val SEERS_VILLAGE_BANK = CoordGrid(0, 42, 54, 36, 28)
        val YANILLE = CoordGrid(0, 40, 48, 24, 25)

        val freeToPlaySpellIds =
            setOf(
                objs.spell_varrock_teleport.id,
                objs.spell_lumbridge_teleport.id,
                objs.spell_falador_teleport.id,
            )
        val supportedSpellIds =
            freeToPlaySpellIds +
                setOf(
                    objs.spell_camelot_teleport.id,
                    objs.spell_kourendcastle_teleport.id,
                    objs.spell_ardougne_teleport.id,
                    objs.spell_civitas_fortis_teleport.id,
                    objs.spell_watchtower_teleport.id,
                    objs.spell_trollheim_teleport.id,
                    objs.spell_apeatoll_teleport.id,
                )
    }
}
