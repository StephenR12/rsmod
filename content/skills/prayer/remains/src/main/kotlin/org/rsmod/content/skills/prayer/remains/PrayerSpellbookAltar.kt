package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.loc.BoundLocInfo

/** Cache-233 spellbook switching behavior for the Astral and Ancient Pyramid altars. */
public class PrayerSpellbookAltar {
    public fun astral(access: ProtectedAccess, altar: BoundLocInfo) {
        with(access) {
            if (vars[varbits.lunar_quest_main] < LUNAR_DIPLOMACY_COMPLETE) {
                return
            }
            faceLoc(altar)
            anim(seqs.human_pray)

            val current = Spellbook[vars[varbits.spellbook]]
            val next = if (current == Spellbook.Lunars) Spellbook.Standard else Spellbook.Lunars
            vars[varbits.spellbook] = next.varValue
            mes(if (next == Spellbook.Lunars) LUNAR_ACTIVATED else LUNAR_DEACTIVATED)
        }
    }

    public fun ancientPyramid(access: ProtectedAccess, altar: BoundLocInfo) {
        with(access) {
            if (vars[varbits.desert_treasure_main] < DESERT_TREASURE_COMPLETE) {
                return
            }
            faceLoc(altar)
            anim(seqs.human_pray)

            vars[varbits.prayer_drain_counter] = 0
            val prayer = stat(stats.prayer)
            if (prayer > 0) {
                statSub(stats.prayer, constant = prayer, percent = 0)
            }

            val current = Spellbook[vars[varbits.spellbook]]
            val next = if (current == Spellbook.Ancients) Spellbook.Standard else Spellbook.Ancients
            vars[varbits.spellbook] = next.varValue
            if (next == Spellbook.Ancients) player.completeDiaryTask(DiaryTask.ActivateAncients)
            mes(if (next == Spellbook.Ancients) ANCIENT_ACTIVATED else ANCIENT_DEACTIVATED)
        }
    }

    public companion object {
        public const val LUNAR_DIPLOMACY_COMPLETE: Int = 190
        public const val DESERT_TREASURE_COMPLETE: Int = 15

        public const val LUNAR_ACTIVATED: String = "Lunar spells activated!"
        public const val LUNAR_DEACTIVATED: String = "Lunar spells deactivated!"
        public const val ANCIENT_ACTIVATED: String = "You feel a strange wisdom fill your mind..."
        public const val ANCIENT_DEACTIVATED: String =
            "You feel a strange drain upon your memory..."
    }
}
