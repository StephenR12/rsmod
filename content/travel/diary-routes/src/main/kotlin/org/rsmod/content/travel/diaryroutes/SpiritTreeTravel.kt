package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.CultivatedSpiritTrees
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object TreeLocs : LocReferences() {
    val public =
        listOf(
                "spirittree_big_2ops",
                "spirittree_big_2ops_orbs",
                "spirittree_small_2ops",
                "spirittree_prif_2ops",
                "pog_spirit_tree_alive_static",
            )
            .map { find(it) }
    val locked =
        listOf("spirittree_big_1op", "spirittree_small_1op", "spirittree_prif_1op").map { find(it) }
    val grown = find("spirit_tree_fullygrown")
    val house = find("poh_spirit_tree")
}

internal object GnomeVars : VarpReferences() {
    val village = find("treequest")
    val grandTree = find("grandtree")
    val favour = find("onesmallfavour")
}

internal object GnomeBits : VarBitReferences() {
    val path = find("pog")
    val elves = find("sote")
    val monkeys = find("mm2_progress")
    val lastFlight = find("pilot_previous_destination")
}

public class SpiritTreeTravel : PluginScript() {
    private data class Tree(val name: String, val anchor: CoordGrid, val arrival: CoordGrid)

    private val village = Tree("Tree Gnome Village", CoordGrid(2543, 3168), CoordGrid(2541, 3170))
    private val stronghold = Tree("Gnome Stronghold", CoordGrid(2460, 3445), CoordGrid(2461, 3444))
    private val exchange = Tree("Grand Exchange", CoordGrid(3184, 3509), CoordGrid(3185, 3508))
    private val waste = Tree("Poison Waste", CoordGrid(2338, 3110), CoordGrid(2339, 3109))
    private val prif = Tree("Prifddinas", CoordGrid(3274, 6124), CoordGrid(3275, 6123))
    private val permanent =
        listOf(
            village,
            stronghold,
            Tree("Battlefield of Khazard", CoordGrid(2555, 3260), CoordGrid(2556, 3259)),
            exchange,
            Tree("Feldip Hills", CoordGrid(2486, 2850), CoordGrid(2487, 2849)),
            waste,
            prif,
        )
    private val patches =
        mapOf(
            CoordGrid(3059, 3257) to "Port Sarim",
            CoordGrid(2612, 3857) to "Etceteria",
            CoordGrid(2801, 3202) to "Brimhaven",
            CoordGrid(1692, 3541) to "Hosidius",
            CoordGrid(1252, 3749) to "Farming Guild",
        )

    override fun ScriptContext.startup() {
        for (tree in TreeLocs.public) {
            onOpLoc1(tree) { destinations(it.loc.coords) }
            onOpLoc2(tree) { destinations(it.loc.coords) }
        }
        for (tree in TreeLocs.locked) onOpLoc2(tree) { destinations(it.loc.coords) }
        onOpLoc1(TreeLocs.grown) { destinations(it.loc.coords, cultivated = true) }
        onOpLoc1(TreeLocs.house) { destinations(it.loc.coords, house = true) }
        for (tree in FairyRingLocs.combined) onOpLoc1(tree) {
            destinations(it.loc.coords, house = true)
        }
    }

    private suspend fun ProtectedAccess.destinations(
        source: CoordGrid,
        cultivated: Boolean = false,
        house: Boolean = false,
    ) {
        if (!player.members) return
        val villageQuest = vars[GnomeVars.village] >= 9
        val grandQuest = vars[GnomeVars.grandTree] >= 160
        val trees = CultivatedSpiritTrees(player).also(::publish)
        if (
            !villageQuest && !grandQuest ||
                !villageQuest && source != stronghold.anchor ||
                !grandQuest && source != village.anchor ||
                cultivated && source !in trees.plots ||
                house && !trees.hasHouseTree
        ) {
            mes("The spirit tree doesn't seem interested in talking.")
            return
        }
        val options =
            permanent
                .filter {
                    it.anchor != source &&
                        (it != waste || vars[GnomeBits.path] >= 38) &&
                        (it != prif || vars[GnomeBits.elves] >= 200)
                }
                .toMutableList()
        for ((anchor, name) in patches) if (anchor != source && anchor in trees.plots) {
            options += Tree(name, anchor, anchor.translate(1, -1))
        }
        val names = options.map { it.name }.toMutableList()
        if (!house && trees.hasHouseTree) names += "Your house"
        val choice = menu("Where would you like to travel?", true, names)
        val tree = options.getOrNull(choice)
        if (player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return
        }
        val arrival =
            tree?.arrival
                ?: if (!house && trees.hasHouseTree && choice == options.size)
                    CultivatedSpiritTrees(player, reserveHouse = true)
                        .also(::publish)
                        .houseDestination
                else null
        if (arrival == null) return
        ifClose()
        teleport(arrival)
        publish(PlayerTeleportEvent.Start(player, arrival))
        if (source == exchange.anchor) player.completeDiaryTask(DiaryTask.UseExchangeSpiritTree)
        if (tree == stronghold) player.completeDiaryTask(DiaryTask.TravelStrongholdSpiritTree)
    }
}
