package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object PrayerScrollItems : ObjReferences() {
    val items =
        listOf(
                "raids_prayerscroll_preserve",
                "raids_prayerscroll",
                "raids_prayerscroll_augury",
                "deadeye_prayer_scroll",
                "mystic_vigour_prayer_scroll",
            )
            .map { find(it) }
}

internal object PrayerScrollUnlocks : VarBitReferences() {
    val bits =
        listOf("preserve", "rigour", "augury", "deadeye", "mystic_vigour").map {
            find("prayer_${it}_unlocked")
        }
}

internal object PrayerScrollAnims : SeqReferences() {
    val raid = find("qip_watchtower_read_scroll_and_stop")
    val titan =
        listOf("deadeye", "mystic_vigour").map {
            listOf(
                find("human_male_prayer_scroll_read_01_$it"),
                find("human_female_prayer_scroll_read_02_$it"),
            )
        }
}

internal object PrayerScrollSpots : SpotanimReferences() {
    val titan =
        listOf(
            find("vfx_human_prayer_scroll_read_01_deadeye"),
            find("vfx_human_prayer_scroll_read_02_mystic_vigour"),
        )
}

public class PrayerScrolls : PluginScript() {
    override fun ScriptContext.startup() {
        val names = listOf("Preserve", "Rigour", "Augury", "Deadeye", "Mystic Vigour")
        val levels = listOf(1, 1, 1, 62, 63)
        for ((index, scroll) in PrayerScrollItems.items.withIndex()) onOpHeld1(scroll) {
            val bit = PrayerScrollUnlocks.bits[index]
            val name = names[index]
            if (!player.members || vars[bit] != 0) {
                mes("You cannot learn anything more from this scroll.")
                return@onOpHeld1
            }
            if (player.statBase(stats.prayer) < levels[index]) {
                mes("You need level ${levels[index]} Prayer to read this scroll.")
                return@onOpHeld1
            }
            if (!choice2("Read the scroll and unlock $name.", true, "Keep the scroll.", false))
                return@onOpHeld1
            if (vars[bit] != 0 || !invDel(inv, scroll, slot = it.slot).success) return@onOpHeld1
            vars[bit] = 1
            if (index < 3) anim(PrayerScrollAnims.raid)
            else {
                anim(
                    PrayerScrollAnims.titan[index - 3][
                        if (player.appearance.bodyType == constants.bodytype_b) 1 else 0]
                )
                spotanim(PrayerScrollSpots.titan[index - 3])
            }
            mes("You have unlocked $name.")
        }
    }
}
