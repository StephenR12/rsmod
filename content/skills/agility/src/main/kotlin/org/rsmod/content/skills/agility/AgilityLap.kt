package org.rsmod.content.skills.agility

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varp.VarpReferences

internal enum class AgilityLap(val key: String, val label: String, val variable: String) {
    Gnome("gnome", "Gnome Stronghold", "agility_laps_gnome"),
    Draynor("draynor", "Draynor Village", "agility_laps_draynor"),
    ShayzienBasic("shayzien_basic", "Shayzien Basic", "shayzien_agility_basic_laps"),
    Kharid("kharid", "Al Kharid", "agility_laps_kharid"),
    Varrock("varrock", "Varrock", "agility_laps_varrock"),
    Pyramid("pyramid", "Agility Pyramid", "agility_laps_pyramid"),
    Penguin("penguin", "Penguin", "agility_laps_penguin"),
    Barbarian("barbarian", "Barbarian Outpost", "agility_laps_barbarian"),
    Canifis("canifis", "Canifis", "agility_laps_canifis"),
    ShayzienAdvanced("shayzien_advanced", "Shayzien Advanced", "shayzien_agility_advanced_laps"),
    Ape("ape", "Ape Atoll", "agility_laps_ape"),
    Falador("falador", "Falador", "agility_laps_falador"),
    Wild("wild", "Wilderness", "agility_laps_wild"),
    Seers("seers", "Seers' Village", "agility_laps_seers"),
    Werewolf("werewolf", "Werewolf", "agility_laps_werewolf"),
    Pollnivneach("pollnivneach", "Pollnivneach", "agility_laps_pollnivneach"),
    Dorgesh("dorgesh", "Dorgesh-Kaan", "agility_laps_dorgesh"),
    Prifddinas("prifddinas", "Prifddinas", "agility_laps_prifddinas"),
    Rellekka("rellekka", "Rellekka", "agility_laps_rellekka"),
    Ardy("ardy", "Ardougne", "agility_laps_ardy"),
    Skullball("skullball", "Werewolf Skullball", "agility_laps_skullball"),
    WyrmBasic("wyrm_basic", "Colossal Wyrm Basic", "agility_laps_wyrm_basic"),
    WyrmAdvanced("wyrm_advanced", "Colossal Wyrm Advanced", "agility_laps_wyrm_advanced"),
}

internal object LapVars : VarpReferences() {
    val prifBest = find("prif_agility_best_lap")
    val noticesOff = find("agility_lap_notifications_off")
    val capeDay = find("agility_cape_last_day")
    val counts = AgilityLap.entries.associateWith { find(it.variable) }
}

internal object LapVarBuilder : VarpBuilder() {
    init {
        val names = AgilityLap.entries.map { it.variable }.filter { it.startsWith("agility_laps_") }
        for (name in
            names + listOf("prif_agility_best_lap", "agility_lap_notifications_off", "agility_cape_last_day")) build(name) {
            permanent = true
            transmitNever = true
        }
    }
}

internal fun ProtectedAccess.recordLap(course: AgilityLap) {
    publish(AgilityCourseFinished(player,course))
    val variable = LapVars.counts.getValue(course)
    val total = if (vars[variable] == Int.MAX_VALUE) Int.MAX_VALUE else vars[variable] + 1
    vars[variable] = total
    if (vars[LapVars.noticesOff] == 0)
        mes(
            "You have completed $total lap${if(total==1) "" else "s"} of the ${course.label} course."
        )
}
