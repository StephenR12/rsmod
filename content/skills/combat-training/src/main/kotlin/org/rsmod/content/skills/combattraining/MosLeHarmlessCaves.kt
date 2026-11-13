package org.rsmod.content.skills.combattraining

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HorrorCaveLocs : LocReferences() {
    val entrance = find("harmless_cave_entrance_left")
    val exits = listOf(find("harmless_dungeon_exit_left"), find("harmless_dungeon_exit_right"))
    val stairs = find("harmless_black_spiral_stairs")
    val stairsTop = find("harmless_spiral_steps_hole")
}

internal object HorrorCaveQuest : VarpReferences() {
    val cabinFever = find("fever_quest")
}

internal object HorrorCaveLight : VarBitReferences() {
    val permanent = find("my2arm_fire_horrors")
}

class MosLeHarmlessCaves : PluginScript() {
    override fun ScriptContext.startup() {
        with(HorrorCaveLocs) {
            onOpLoc1(entrance) { enter(CoordGrid(3748, 9374)) }
            for (exit in exits) onOpLoc1(exit) { telejump(CoordGrid(3749, 2973)) }
            onOpLoc1(stairs) { telejump(CoordGrid(it.loc.x - 1, 3062)) }
            onOpLoc1(stairsTop) { enter(CoordGrid(it.loc.x - 1, 9462)) }
        }
    }

    private fun ProtectedAccess.enter(tile: CoordGrid) {
        if (!player.members || player.vars[HorrorCaveQuest.cabinFever] < 140) return
        if (player.vars[HorrorCaveLight.permanent] == 0 && !hasLight()) {
            mes("You'll need a light source to find your way through the cave.")
            return
        }
        telejump(tile)
    }
}
