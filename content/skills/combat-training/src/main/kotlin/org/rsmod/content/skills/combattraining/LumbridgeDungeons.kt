package org.rsmod.content.skills.combattraining

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.skills.firemaking.LightSourceRecipes
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DungeonLocs : LocReferences() {
    val cave = find("goblin_cave_entrance")
    val rope = find("swamp_cave_climbing_rope")
    val trapClosed = find("vampire_trap1")
    val trapOpen = find("vampire_trap2")
    val ladder = find("vampire_ladder")
}

internal object DungeonLadders : LocEditor() {
    init {
        edit(DungeonLocs.trapOpen) {
            param[params.climb_anim] = seqs.human_pickupfloor
            contentGroup = content.dungeonladder_down
        }
        edit(DungeonLocs.ladder) { contentGroup = content.dungeonladder_up }
    }
}

internal object DungeonVars : VarBitReferences() {
    val ropeAttached = find("swamp_caves_roped_entrance")
    val fireOfEternalLight = find("my2arm_fire_lumb")
}

internal var Player.swampRope by boolVarBit(DungeonVars.ropeAttached)

class LumbridgeDungeons @Inject constructor(private val locs: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(DungeonLocs.trapClosed) { locs.change(it.loc, DungeonLocs.trapOpen, 300) }
        onOpLocU(DungeonLocs.cave, objs.rope) {
            if (!player.members) return@onOpLocU
            if (player.swampRope) {
                mes("There is already a rope leading down into the caves.")
            } else if (invDel(inv, objs.rope, slot = it.invSlot).success) {
                player.swampRope = true
                mes("You tie the rope to a root at the edge of the hole.")
            }
        }
        onOpLoc1(DungeonLocs.cave) {
            if (!player.members) {
                mes("You need to be on a members' world to enter these caves.")
            } else if (!player.swampRope) {
                mes("You'll need to use a rope on the hole before you can climb down.")
            } else if (player.vars[DungeonVars.fireOfEternalLight] == 0 && !hasLight()) {
                mes("It's too dark down here. You climb back up the rope.")
            } else {
                telejump(CoordGrid(3169, 9571))
            }
        }
        onOpLoc1(DungeonLocs.rope) { telejump(CoordGrid(3169, 3171)) }
    }
}

internal fun ProtectedAccess.hasLight(): Boolean = LightSourceRecipes.carriesLight(player)
