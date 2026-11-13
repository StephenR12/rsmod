package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DiaryDungeonPeople : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DiaryDungeonPopulations>("diary-dungeon-populations.toml")
    }
}

internal object DiaryDungeonLocs : LocReferences() {
    val shadowLadder = find("fd_shadow_ladder_there")
    val shadowExit = find("fd_shadowladder")
    val heroesDoors = listOf(find("herodoor_l"), find("herodoor_r"))
    val down = find("ladder_cellar")
    val up = find("ladder_from_cellar_directional")
    val cage = listOf(find("metalgateclosedl"), find("metalgateclosedr"))
    val pothole = find("pothole_cave_entrance")
    val handholds = find("jp_caverocksout")
}

internal object DiaryDungeonBits : VarBitReferences() {
    val rasolo = find("fd_shadowwarrior_quest")
    val visibility = find("shadow_realm_visibility")
}

internal object DiaryDungeonNpcs : NpcReferences() {
    val rasolo = find("shadow_warrior_rasool")
}

internal object DiaryDungeonObjs : ObjReferences() {
    val visibility = find("fd_ring_visibility")
    val shadows = listOf(find("ring_of_shadows"), find("ring_of_shadows_uncharged"))
}

public class DiaryDungeonPopulations : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { refreshVisibility(player) }
        onEvent<HeldEquipEvents.WearposChange> {
            if (wearpos == Wearpos.Ring) refreshVisibility(player)
        }
        onOpNpc1(DiaryDungeonNpcs.rasolo) {
            val ring = DiaryDungeonObjs.visibility
            when {
                !player.members || vars[DiaryDungeonBits.rasolo] < 3 ->
                    mes("Rasolo is looking for a gilded cross.")
                ring in inv || ring in worn || ring in bank ->
                    mes("You already have the ring of visibility.")
                invAdd(inv, ring).success -> mes("Rasolo gives you another ring of visibility.")
                else -> mes("You need a free inventory space for the ring.")
            }
        }
        with(DiaryDungeonLocs) {
            onOpLoc1(shadowLadder) {
                if (player.members && hasRing(player)) telejump(CoordGrid(2630, 5072))
            }
            onOpLoc1(shadowExit) { telejump(CoordGrid(2548, 3421)) }
            for (door in heroesDoors) onOpLoc1(door) {
                val entering = coords.x >= 2902
                if (entering && (!player.members || vars[varps.heroes_quest] < 15))
                    mes("Only those who have completed the Heroes' Quest may enter.")
                else telejump(CoordGrid(if (entering) 2901 else 2902, it.loc.z))
            }
            onOpLoc1(down, CoordGrid(2892, 3507)) { telejump(CoordGrid(2893, 9907)) }
            onOpLoc1(up, CoordGrid(2892, 9907)) { telejump(CoordGrid(2893, 3507)) }
            for ((index, gate) in cage.withIndex()) onOpLoc1(gate, CoordGrid(2910 - index, 9910)) {
                telejump(CoordGrid(it.loc.x, if (coords.z < 9910) 9910 else 9909))
            }
            onOpLoc2(pothole) {
                if (
                    player.members &&
                        choice2("Climb down into the dungeon.", true, "Stay here.", false)
                )
                    telejump(CoordGrid(2831, 9522))
            }
            onOpLoc1(handholds) { telejump(CoordGrid(2825, 3117)) }
        }
    }

    private fun hasRing(player: Player): Boolean =
        DiaryDungeonObjs.visibility in player.worn ||
            DiaryDungeonObjs.shadows.any { it in player.worn }

    private fun refreshVisibility(player: Player) {
        // The cache multi selects the visible ladder for all three nonzero visibility values.
        val value =
            when {
                DiaryDungeonObjs.shadows.any { it in player.worn } -> 2
                DiaryDungeonObjs.visibility in player.worn -> 1
                else -> 0
            }
        if (player.vars[DiaryDungeonBits.visibility] != value)
            VarPlayerIntMapSetter.set(player, DiaryDungeonBits.visibility, value)
    }
}
