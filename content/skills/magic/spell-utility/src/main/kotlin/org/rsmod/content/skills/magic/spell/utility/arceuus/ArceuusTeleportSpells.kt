package org.rsmod.content.skills.magic.spell.utility.arceuus

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
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.realm.Realm
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.content.skills.magic.spell.utility.standard.spellTeleport
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

/** Cache-233 behavior for the twelve Arceuus destination teleports. */
public class ArceuusTeleportSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
    private val realm: Realm,
) {
    public suspend fun cast(access: ProtectedAccess, spellType: ObjType) {
        require(spellType.id in supportedSpellIds) { "Unsupported Arceuus teleport: $spellType" }
        with(access) {
            val spell = requireNotNull(spells.getObjSpell(spellType))
            check(spell.type == MagicSpellType.Teleport) { "Expected teleport spell: $spell" }

            if (!canAccess(spellType) || !runes.canCastSpell(player, spell)) {
                return
            }
            stopAction()
            if (!canAccess(spellType) || runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            anim(seqs.human_spellcast_arceuus_teleport)
            spotanim(spotanims.arceuus_teleport)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(destination(spellType))
        }
    }

    private fun ProtectedAccess.canAccess(spellType: ObjType): Boolean {
        if (!player.members) {
            mes("You need to be on a members' world to cast this spell.")
            return false
        }
        val quest = questRequirement(spellType)
        if (quest != null && vars[quest.varp] < quest.completeState) {
            mes("You need to complete ${quest.name} to cast this spell.")
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
        if (spellType.id == objs.spell_respawn_teleport.id) {
            realm.config.respawnCoord
        } else {
            objTypes[spellType].param(params.spell_telecoord)
        }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    private companion object {
        const val CAST_TICKS = 4
        const val MAX_WILDERNESS_LEVEL = 20

        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920

        val priestInPeril = QuestRequirement(varps.priest_in_peril, 60, "Priest in Peril")
        val biohazard = QuestRequirement(varps.biohazard, 16, "Biohazard")
        val greatBrainRobbery =
            QuestRequirement(varps.great_brain_robbery, 130, "The Great Brain Robbery")
        val monkeyMadness = QuestRequirement(varps.monkey_madness, 9, "Monkey Madness I")

        val supportedSpellIds =
            setOf(
                objs.spell_arceuuslibrary_teleport.id,
                objs.spell_draynormanor_teleport.id,
                objs.spell_battlefront_teleport.id,
                objs.spell_mindaltar_teleport.id,
                objs.spell_respawn_teleport.id,
                objs.spell_salvegrave_teleport.id,
                objs.spell_fenkenstrainscastle_teleport.id,
                objs.spell_westardougne_teleport.id,
                objs.spell_harmonyisland_teleport.id,
                objs.spell_cemetery_teleport.id,
                objs.spell_barrows_teleport.id,
                objs.spell_arceuusapeatoll_teleport.id,
            )

        fun questRequirement(spellType: ObjType): QuestRequirement? =
            when (spellType.id) {
                objs.spell_salvegrave_teleport.id,
                objs.spell_fenkenstrainscastle_teleport.id,
                objs.spell_barrows_teleport.id -> priestInPeril
                objs.spell_westardougne_teleport.id -> biohazard
                objs.spell_harmonyisland_teleport.id -> greatBrainRobbery
                objs.spell_arceuusapeatoll_teleport.id -> monkeyMadness
                else -> null
            }
    }

    private data class QuestRequirement(
        val varp: org.rsmod.game.type.varp.VarpType,
        val completeState: Int,
        val name: String,
    )
}
