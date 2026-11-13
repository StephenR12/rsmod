package org.rsmod.content.skills.farming

import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object WhiteTreeBits : VarBitReferences() {
    val quest = find("garden_quest")
    val stage = find("garden_white_tree_varbit")
}

internal object WhiteTreeLocs : LocReferences() {
    val base = find("garden_white_tree_patch")
    val fruit = (1..4).map { find("garden_white_tree_fruit_$it") }
}

internal object WhiteTreeItems : ObjReferences() {
    val fruit = find("garden_white_tree_fruit")
}

internal object WhiteTreeVars : VarpReferences() {
    val next = find("white_tree_next_fruit_minute")
}

internal object WhiteTreeVarBuilder : VarpBuilder() {
    init {
        build("white_tree_next_fruit_minute") { transmitNever = true }
    }
}

public class WhiteTree : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in WhiteTreeLocs.fruit + WhiteTreeLocs.base) onOpLoc1(type) {
            if (
                !player.members ||
                    vars[WhiteTreeBits.quest] < 60 ||
                    it.loc.x != 3229 ||
                    it.loc.z != 3474
            )
                return@onOpLoc1
            refresh(player)
            val stage = vars[WhiteTreeBits.stage]
            if (stage <= GROWN) return@onOpLoc1
            if (invAdd(inv, WhiteTreeItems.fruit).failure) {
                mes("You need room in your inventory to pick a fruit.")
                return@onOpLoc1
            }
            if (stage == FULL) vars[WhiteTreeVars.next] = now() + 5
            vars[WhiteTreeBits.stage] = stage - 1
            farmingXp(12.0)
            player.completeDiaryTask(DiaryTask.PickVarrockWhiteTreeFruit)
        }
    }

    internal companion object {
        private const val GROWN = 8
        private const val FULL = 12

        private fun now(): Int = (System.currentTimeMillis() / 60_000).toInt()

        fun refresh(player: Player) {
            if (
                !player.members ||
                    player.coords.x !in 3210..3250 ||
                    player.coords.z !in 3455..3495 ||
                    player.vars[WhiteTreeBits.quest] < 60
            )
                return
            val stage = player.vars[WhiteTreeBits.stage]
            if (stage == FULL) return
            val minute = now()
            val next = player.vars[WhiteTreeVars.next]
            if (stage < GROWN) {
                // Old saves with the completed quest can lack the separate tree state.
                VarPlayerIntMapSetter.set(player, WhiteTreeBits.stage, FULL)
            } else if (next == 0) VarPlayerIntMapSetter.set(player, WhiteTreeVars.next, minute + 5)
            else if (minute >= next) {
                val fruit = 1 + (minute - next) / 5
                val grown = (stage + fruit).coerceAtMost(FULL)
                VarPlayerIntMapSetter.set(player, WhiteTreeBits.stage, grown)
                VarPlayerIntMapSetter.set(
                    player,
                    WhiteTreeVars.next,
                    if (grown == FULL) 0 else next + fruit * 5,
                )
            }
        }
    }
}
