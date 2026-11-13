package org.rsmod.content.skills.construction

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HouseSupplyNpcs : NpcReferences() {
    val brimstail =
        listOf("gnome_brimstail", "gnome_brimstail_1op", "gnome_brimstail_2op").map { find(it) }
    val askeladden =
        listOf("viking_askelapen", "viking_askelladen_norockop", "viking_askelladen_rockop").map {
            find(it)
        }
}

internal object HouseSupplyCave : LocReferences() {
    val entry = find("eyeglo_brimstails_cave_entrance")
    val exits = listOf(find("eyeglo_dungeon_exit_left"), find("eyeglo_dungeon_exit_right"))
}

internal object HouseSupplyBits : VarBitReferences() {
    val rock = find("askelladen_hasop")
}

internal object HouseSupplyVars : VarpReferences() {
    val trials = find("viking")
}

internal object HouseSupplyItems : ObjReferences() {
    val rock = find("vt_useless_rock")
}

public class HouseQuestSupplies : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(HouseSupplyCave.entry) { if (player.members) telejump(CoordGrid(2408, 9812)) }
        for (door in HouseSupplyCave.exits) onOpLoc1(door) { telejump(CoordGrid(2403, 3419)) }
        for (gnome in HouseSupplyNpcs.brimstail) onOpNpc1(gnome) {
            if (!player.members || vars[SawBits.quest] < 60) {
                mes("Brimstail is studying the singing bowl in his cave.")
                return@onOpNpc1
            }
            if (!choice2("I've lost my crystal saw.", true, "Goodbye.", false)) return@onOpNpc1
            if (listOf(SawItems.seed, SawItems.saw).any { it in inv || it in bank }) {
                mes("You already have a crystal saw or its seed.")
                return@onOpNpc1
            }
            if (invAdd(inv, SawItems.seed).success)
                mes("Brimstail gives you another crystal saw seed.")
            else mes("You need room for the seed.")
        }
        for (npc in HouseSupplyNpcs.askeladden) {
            onOpNpc1(npc) { if (choice2("May I have a pet rock?", true, "Goodbye.", false)) rock() }
            if (npc != HouseSupplyNpcs.askeladden[1]) onOpNpc3(npc) { rock() }
        }
    }

    private fun ProtectedAccess.rock() {
        if (
            !player.members ||
                (vars[HouseSupplyBits.rock] == 0 && vars[HouseSupplyVars.trials] == 0)
        ) {
            mes("Askeladden is busy preparing for his Fremennik trials.")
            return
        }
        if (invAdd(inv, HouseSupplyItems.rock).failure) {
            mes("You need more room to carry a rock.")
            return
        }
        vars[HouseSupplyBits.rock] = 1
        mes("Askeladden hands you a pet rock.")
    }
}
