package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.minigames.skillcapeTeleport
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.*
import org.rsmod.content.skills.magic.spell.utility.standard.HouseLocation
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class ConstructionCape @Inject constructor(private val houses: StarterHouse) :
    PluginScript() {
    private val sites =
        listOf(
            HouseLocation.Rimmington,
            HouseLocation.Taverley,
            HouseLocation.Pollnivneach,
            HouseLocation.Hosidius,
            HouseLocation.Aldarin,
            HouseLocation.Rellekka,
            HouseLocation.Brimhaven,
            HouseLocation.Yanille,
            HouseLocation.Prifddinas,
        )

    override fun ScriptContext.startup() {
        for (cape in listOf(objs.construction_skillcape, objs.construction_skillcape_t)) {
            onOpHeld3(cape) { choose() }
            onOpHeld4(cape) { go(null) }
            onOpWorn3(cape) { choose() }
            onOpWorn4(cape) { go(null) }
        }
    }

    public suspend fun teleport(access: ProtectedAccess, portal: Int): Unit =
        with(access) {
            if (portal == 0) go(null) else sites.getOrNull(portal - 1)?.let { go(it) }
            Unit
        }

    public suspend fun selectPortal(access: ProtectedAccess): Unit = with(access) { choose() }

    private suspend fun ProtectedAccess.choose() {
        val choice =
            menu("Teleport to a house portal", true, listOf("Home") + sites.map { it.name })
        if (choice == 0) go(null) else sites.getOrNull(choice - 1)?.let { go(it) }
    }

    private suspend fun ProtectedAccess.go(site: HouseLocation?) {
        val owned = HouseLocation.fromCacheValue(vars[varbits.poh_house_location])
        if (site == null && owned == null) {
            mes("You do not own a house.")
            return
        }
        if (site == HouseLocation.Prifddinas && vars[varbits.song_of_the_elves] < 200) {
            mes("You must complete Song of the Elves to teleport to Prifddinas.")
            return
        }
        if (
            (site == HouseLocation.Hosidius && vars[HouseTravelBits.kourendVisited] == 0) ||
                (site == HouseLocation.Aldarin && vars[HouseTravelBits.varlamoreVisited] == 0)
        ) {
            mes("You must visit that region first.")
            return
        }
        skillcapeTeleport {
            when {
                site != null -> site.outside
                vars[varbits.poh_tele_toggle] != 0 -> owned?.outside
                else -> houses.prepare(player, vars[varbits.poh_teleport_building_mode] != 0)
            }
        }
    }
}
