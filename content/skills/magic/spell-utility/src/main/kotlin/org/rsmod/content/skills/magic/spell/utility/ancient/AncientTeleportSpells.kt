package org.rsmod.content.skills.magic.spell.utility.ancient

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpellType
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.content.skills.magic.spell.utility.standard.spellTeleport
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

/** Fixed-destination Ancient Magicks teleports. */
public class AncientTeleportSpells
@Inject
constructor(
    private val spells: MagicSpellRegistry,
    private val runes: MagicRuneManager,
    private val objTypes: ObjTypeList,
) {
    public suspend fun cast(access: ProtectedAccess, spellType: ObjType) {
        require(spellType.id in supportedSpellIds) { "Unsupported Ancient teleport: $spellType" }
        with(access) {
            if (!player.members) {
                mes("You need to be on a members' world to cast this spell.")
                return
            }
            if (vars[varbits.desert_treasure_main] < DESERT_TREASURE_COMPLETE) {
                mes("You must complete Desert Treasure to use Ancient Magicks.")
                return
            }
            if (vars[varbits.inside_wilderness] == 1 && wildernessLevel() > MAX_WILDERNESS_LEVEL) {
                mes("A mysterious force prevents you from teleporting!")
                return
            }
            if (player.isTeleportBlocked()) {
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
            anim(AncientTeleportSeqs.cast)
            spotanim(AncientTeleportSpotanims.cast)
            soundSynth(synths.teleport_all)
            delay(CAST_TICKS)
            spellTeleport(objTypes[spellType].param(params.spell_telecoord))
            when (spellType.id) {
                objs.spell_paddewwa_teleport.id ->
                    player.completeDiaryTask(DiaryTask.TeleportToPaddewwa)
                objs.spell_ghorrock_teleport.id ->
                    player.completeDiaryTask(DiaryTask.TeleportToGhorrock)
            }
        }
    }

    private fun ProtectedAccess.wildernessLevel(): Int {
        val baseZ =
            if (coords.z >= WILDERNESS_UNDERGROUND_Z) WILDERNESS_UNDERGROUND_Z else WILDERNESS_Z
        return ((coords.z - baseZ) / WILDERNESS_LEVEL_HEIGHT) + 1
    }

    private companion object {
        const val CAST_TICKS = 4
        const val DESERT_TREASURE_COMPLETE = 15
        const val MAX_WILDERNESS_LEVEL = 20

        const val WILDERNESS_LEVEL_HEIGHT = 8
        const val WILDERNESS_Z = 3520
        const val WILDERNESS_UNDERGROUND_Z = 9920

        val supportedSpellIds =
            setOf(
                objs.spell_paddewwa_teleport.id,
                objs.spell_senntisten_teleport.id,
                objs.spell_kharyllyl_teleport.id,
                objs.spell_lassar_teleport.id,
                objs.spell_dareeyak_teleport.id,
                objs.spell_carrallagar_teleport.id,
                objs.spell_annakarl_teleport.id,
                objs.spell_ghorrock_teleport.id,
            )
    }
}

internal object AncientTeleportSeqs : SeqReferences() {
    val cast = find("zaros_teleport")
}

internal object AncientTeleportSpotanims : SpotanimReferences() {
    val cast = find("zaros_teleport")
}
