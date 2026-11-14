package org.rsmod.content.other.commands

import jakarta.inject.Inject
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.interfaces.bank.BankDestinations
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.varp.VarpType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object LeagueTeleportObjects : ObjReferences() {
    val briefcase = find("league_bank_heist_teleport")
    val eye = find("league_evil_eye_necklace")
    val map = find("league_agility_map")
}

internal object LeagueTeleportVars : VarpReferences() {
    val briefcase = find("league_briefcase_last_destination")
    val eye = find("league_eye_last_destination")
    val map = find("league_map_last_destination")
}

internal object LeagueTeleportVarBuilds : VarpBuilder() {
    init {
        build("league_briefcase_last_destination") { transmitNever = true }
        build("league_eye_last_destination") { transmitNever = true }
        build("league_map_last_destination") { transmitNever = true }
    }
}

internal object LeagueTeleportQuests : VarBitReferences() {
    val requirements =
        mapOf(
            "Song of the Elves" to (find("sote") to 200),
            "Sins of the Father" to (find("myq5") to 138),
            "The Heart of Darkness" to (find("vmq3") to 76),
            "The Final Dawn" to (find("vmq4") to 68),
            "Shadows of Custodia" to (find("soc") to 24),
        )
}

internal data class LeagueDestination(
    val name: String,
    val coords: CoordGrid,
    val region: String,
    val quest: String? = null,
)

/** Relic teleports remain available on this unrestricted development world. */
class LeagueTeleportItems @Inject constructor() : PluginScript() {
    private data class Item(val type: ObjType, val last: VarpType, val title: String)

    override fun ScriptContext.startup() {
        val briefcase = Item(LeagueTeleportObjects.briefcase, LeagueTeleportVars.briefcase, "Bank Heist")
        val eye = Item(LeagueTeleportObjects.eye, LeagueTeleportVars.eye, "Evil eye")
        val map = Item(LeagueTeleportObjects.map, LeagueTeleportVars.map, "Map of alacrity")
        for (item in listOf(briefcase, eye, map)) {
            onOpHeld1(item.type) { choose(item, it.inventory, it.slot) }
        }
        onOpHeld3(briefcase.type) { repeat(briefcase, it.inventory, it.slot) }
        onOpHeld3(eye.type) { repeat(eye, it.inventory, it.slot) }
        onOpHeld3(map.type) { repeat(map, it.inventory, it.slot) }
        for (item in listOf(briefcase, eye)) {
            onOpWorn2(item.type) { choose(item, worn, it.slot) }
            onOpWorn3(item.type) { repeat(item, worn, it.slot) }
        }
    }

    private fun destinations(item: Item): List<LeagueDestination> =
        when (item.type) {
            LeagueTeleportObjects.briefcase ->
                BankDestinations.all.map { LeagueDestination(it.name, it.coords, it.region) }
            LeagueTeleportObjects.eye -> load("league-eye.tsv")
            else -> load("league-agility.tsv")
        }

    private fun load(resource: String): List<LeagueDestination> =
        requireNotNull(javaClass.getResourceAsStream(resource)).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() && !it.startsWith('#') }.map { line ->
                val fields = line.split('\t')
                LeagueDestination(
                    fields[0],
                    CoordGrid(fields[1].toInt(), fields[2].toInt(), fields[3].toInt()),
                    fields[4],
                    fields.getOrNull(6)?.takeIf { it.isNotBlank() },
                )
            }.toList()
        }

    private suspend fun ProtectedAccess.choose(item: Item, source: Inventory, slot: Int) {
        val groups = destinations(item).groupBy { it.region }.toSortedMap()
        val regions = groups.keys.toList()
        val region = menu(item.title, false, regions)
        if (region !in regions.indices) return
        val choices = groups.getValue(regions[region])
        val selection = menu(regions[region], false, choices.map { it.name })
        if (selection !in choices.indices) return
        travel(item, source, slot, choices[selection])
    }

    private suspend fun ProtectedAccess.repeat(item: Item, source: Inventory, slot: Int) {
        val packed = vars[item.last]
        if (packed == 0) {
            mes("Choose a destination first.")
            return
        }
        val destination = destinations(item).firstOrNull { it.coords.packed == packed }
        if (destination == null) {
            mes("Choose a destination first.")
            return
        }
        travel(item, source, slot, destination)
    }

    private suspend fun ProtectedAccess.travel(item: Item, source: Inventory, slot: Int, destination: LeagueDestination) {
        if (source[slot]?.id != item.type.id) return
        val quest = destination.quest
        val unlocked =
            when (quest) {
                null -> true
                "Legends' Quest" -> vars[varps.legends_quest] >= 75
                else -> {
                    val (progress, complete) = LeagueTeleportQuests.requirements.getValue(quest)
                    vars[progress] >= complete
                }
            }
        if (!unlocked) {
            mes("You must complete $quest to reach this destination.")
            return
        }
        if (player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return
        }
        stopAction()
        anim(seqs.teleport_casting)
        spotanim(spotanims.teleport_casting)
        delay(3)
        if (source[slot]?.id != item.type.id) return
        teleport(destination.coords)
        VarPlayerIntMapSetter.set(player, item.last, destination.coords.packed)
        publish(PlayerTeleportEvent.Start(player, destination.coords))
    }
}
