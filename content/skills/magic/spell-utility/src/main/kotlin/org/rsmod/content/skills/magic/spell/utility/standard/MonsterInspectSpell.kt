package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.MagicSpellRegistry
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjType

public class MonsterInspectSpell
@Inject
constructor(private val spells: MagicSpellRegistry, private val runes: MagicRuneManager) {
    public fun cast(
        access: ProtectedAccess,
        npc: Npc,
        type: UnpackedNpcType = npc.visType,
        spellType: ObjType = objs.spell_monster_inspect,
    ) {
        with(access) {
            if (spellType.id == objs.spell_monster_examine.id && !canCastMonsterExamine()) {
                return
            }
            val spell = requireNotNull(spells.getObjSpell(spellType))
            if (runes.attemptCast(player, spell).isFailure()) {
                return
            }

            if (vars[varbits.fountain_of_rune_active] == 0) {
                statAdvance(stats.magic, spell.castXp)
            }
            if (
                spellType.id == objs.spell_monster_examine.id &&
                    type.id in 936..942 &&
                    npc.coords.level == 0 &&
                    npc.coords.x in 1209..1255 &&
                    npc.coords.z in 3480..3529
            ) {
                player.completeDiaryTask(DiaryTask.ExamineQuidamortemTroll)
            }
            anim(seqs.dream_player_monsterexam_statspy)
            spotanim(spotanims.dream_spell_monsterexam_statspy_spotanim)
            soundSynth(synths.lunar_stat_spy)
            ifOpenMainModal(interfaces.dream_monster_stat)
            runClientScript(DREAM_MONSTER_POPULATE, MonsterInspectFormatter.format(npc, type))
        }
    }

    private fun ProtectedAccess.canCastMonsterExamine(): Boolean {
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

    private companion object {
        /** Cache 233 clientscript `[clientscript,dream_monster_populate]`. */
        const val DREAM_MONSTER_POPULATE: Int = 1179
        const val LUNAR_DIPLOMACY_COMPLETE = 190
        const val DREAM_MENTOR_COMPLETE = 28
    }
}
