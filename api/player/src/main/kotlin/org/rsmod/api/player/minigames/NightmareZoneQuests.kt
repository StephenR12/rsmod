package org.rsmod.api.player.minigames

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player

/** Quest table row IDs, terminal values and saved variables from 233.4 quest_progress_get. */
internal object DreamQuestBits : VarBitReferences() {
    val requirements = listOf(
        Triple(3, find("arcquest"), 14),
        Triple(16, find("contact"), 130),
        Triple(18, find("corscurs_progress"), 60),
        Triple(26, find("hosidiusquest"), 11),
        Triple(27, find("deserttreasure"), 15),
        Triple(33, find("dream_prog"), 28),
        Triple(46, find("fairy_farmers_quest"), 90),
        Triple(56, find("fris_quest"), 340),
        Triple(61, find("ga"), 34),
        Triple(68, find("grim_quest"), 60),
        Triple(74, find("horrorquest"), 10),
        Triple(88, find("lunar_quest_main"), 190),
        Triple(98, find("mdaughter_quest_var"), 70),
        Triple(102, find("myarm"), 320),
        Triple(117, find("hundred_main_quest_var"), 5),
        Triple(129, find("agrith_quest"), 125),
        Triple(137, find("sote"), 200),
        Triple(143, find("shayzienquest"), 17),
        Triple(159, find("surok_quest"), 150),
    )
}

internal object DreamQuestVarps : VarpReferences() {
    val requirements = listOf(
        Triple(31, find("dragonquest"), 10),
        Triple(48, find("crestquest"), 11),
        Triple(51, find("arenaquest"), 14),
        Triple(66, find("grandtree"), 160),
        Triple(67, find("brain_quest_var"), 130),
        Triple(70, find("hauntedmine"), 11),
        Triple(73, find("grail"), 10),
        Triple(79, find("routequest"), 105),
        Triple(85, find("legendsquest"), 75),
        Triple(86, find("zanaris"), 6),
        Triple(95, find("mm_main"), 9),
        Triple(107, find("onesmallfavour"), 285),
        Triple(122, find("roving_elves_quest"), 6),
        Triple(133, find("zombiequeen"), 15),
        Triple(150, find("treequest"), 9),
        Triple(152, find("troll_love"), 45),
        Triple(153, find("troll_quest"), 50),
        Triple(155, find("vampire"), 3),
        Triple(160, find("ballquest"), 7),
    )
}

public fun Player.completedNightmareZoneQuests(): Set<Int> = buildSet {
    for ((quest, bit, end) in DreamQuestBits.requirements) if (vars[bit] >= end) add(quest)
    for ((quest, varp, end) in DreamQuestVarps.requirements) if (vars[varp] >= end) add(quest)
}

public fun Player.canEnterNightmareZone(): Boolean = members && completedNightmareZoneQuests().size >= 5
