package org.rsmod.content.travel.diaryroutes

import jakarta.inject.Inject
import java.util.WeakHashMap
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// Ordinals are the seven morph indices in the pilots' native last-destination varbit.
internal enum class Flight(
    val label: String,
    val button: String,
    val arrival: CoordGrid,
    val pilot: String,
) {
    GrandTree("Ta Quir Priw", "grandtree", CoordGrid(2465, 3500, 3), "Captain Errdo"),
    Karamja("Gandius", "karamja", CoordGrid(2970, 2973), "Captain Klemfoodle"),
    AlKharid("Kar-Hewo", "alkharid", CoordGrid(3279, 3211), "Captain Dalbur"),
    Digsite("Lemanto Andra", "varrock", CoordGrid(3321, 3430), "Captain Errdo"),
    WhiteWolf("Sindarpos", "whitewolfmountain", CoordGrid(2847, 3499), "Captain Bleemadge"),
    Feldip("Lemantolly Undri", "ogrearea", CoordGrid(2544, 2971), "Gnormadium Avlafrim"),
    ApeAtoll("Ookookolly Undri", "apeatoll", CoordGrid(2711, 2803), "Captain Shoracks"),
}

internal object GliderMap : InterfaceReferences() {
    val main = find("glidermap")
}

internal object GliderButtons : ComponentReferences() {
    val destinations = Flight.entries.associateWith { find("glidermap:${it.button}_button") }
    val close = find("glidermap:close")
}

public class GliderTravel @Inject constructor(private val npcTypes: NpcTypeList) : PluginScript() {
    private val at = WeakHashMap<Player, Flight>()

    override fun ScriptContext.startup() {
        for (pilot in npcTypes.values) {
            val station = Flight.entries.firstOrNull { it.pilot == pilot.name } ?: continue
            if (pilot.op[0] != "Glider") continue
            onOpNpc1(pilot) { map(station) }
            if (pilot.op[2] == "Talk-to") onOpNpc3(pilot) { map(station) }
            val quick = pilot.op[3]?.removePrefix("Glider-to ")
            val destination = Flight.entries.firstOrNull { it.label.equals(quick, true) }
            if (destination != null) onOpNpc4(pilot) { fly(station, destination) }
        }
        for ((flight, button) in GliderButtons.destinations) onIfModalButton(button) {
            val from = at[player] ?: return@onIfModalButton
            fly(from, flight)
        }
        onIfModalButton(GliderButtons.close) { ifClose() }
        onIfClose(GliderMap.main) { at.remove(player) }
        onPlayerLogout { at.remove(player) }
    }

    private fun ProtectedAccess.allowed(stop: Flight): Boolean =
        player.members &&
            vars[GnomeVars.grandTree] >= 160 &&
            (stop != Flight.Feldip || vars[GnomeVars.favour] > 275) &&
            (stop != Flight.ApeAtoll || vars[GnomeBits.monkeys] >= 200)

    private fun ProtectedAccess.map(from: Flight) {
        if (!allowed(from)) {
            mes("The pilot is unable to fly you anywhere.")
            return
        }
        ifOpenMainModal(GliderMap.main)
        at[player] = from
        for ((destination, button) in GliderButtons.destinations) {
            if (destination != from && allowed(destination))
                ifSetEvents(button, -1..-1, IfEvent.Op1)
        }
        ifSetEvents(GliderButtons.close, -1..-1, IfEvent.Op1)
    }

    private suspend fun ProtectedAccess.fly(from: Flight, to: Flight) {
        if (!allowed(from) || !allowed(to) || from == to) return
        ifClose()
        delay(2)
        teleport(to.arrival)
        publish(PlayerTeleportEvent.Start(player, to.arrival))
        vars[GnomeBits.lastFlight] = to.ordinal
        when (to) {
            Flight.Karamja -> player.completeDiaryTask(DiaryTask.FlyKaramjaGlider)
            Flight.Feldip -> player.completeDiaryTask(DiaryTask.FlyFeldipGlider)
            else -> Unit
        }
    }
}
