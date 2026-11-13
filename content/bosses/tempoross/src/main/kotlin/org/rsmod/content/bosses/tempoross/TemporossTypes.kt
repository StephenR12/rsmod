package org.rsmod.content.bosses.tempoross

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid

internal object SeaSpirit : ControllerBuilder() {
    val lobby = build("tempoross_lobby")
}

internal object SeaVars : VarpReferences() {
    val kills = find("total_tempoross_kills")
    val trip = find("tempoross_expedition")
    val upgrades = find("spirit_angler_upgrades")
    val searched = find("tempoross_permits_searched")
    val fastest = find("tempoross_fastest_ticks")
    val best = find("tempoross_best_points")
}

internal object SeaSaves : VarpBuilder() {
    init {
        for (name in
            listOf(
                "tempoross_expedition",
                "spirit_angler_upgrades",
                "tempoross_permits_searched",
                "tempoross_best_points",
                "tempoross_fastest_ticks",
            )) build(name) { permanent = true }
    }
}

internal object SeaBits : VarBitReferences() {
    val permits = find("tempoross_rewardpermits")
    val tethered = find("tempoross_tethered")
    val points = find("tempoross_current_points")
}

internal object SeaUi : InterfaceReferences() {
    val hud = find("tempoross_hud")
    val stats = find("tempoross_stats")
}

internal object SeaSeqs : SeqReferences() {
    val tether = find("tempoross_tether")
    val untether = find("tempoross_untether")
}

internal object UnkahSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<UnkahSpawns>("unkah.toml")
    }
}

internal val pier = CoordGrid(3137, 2840)
internal val lobbyDeck = CoordGrid(3134, 2840)
internal const val RAW = 25564
internal const val COOKED = 25565
internal const val CRYSTAL = 25566
internal val harpoonfish = listOf(RAW, COOKED, CRYSTAL)
