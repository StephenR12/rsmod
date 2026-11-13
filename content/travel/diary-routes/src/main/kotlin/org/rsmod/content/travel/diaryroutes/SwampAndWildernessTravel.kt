package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.isTeleportBlocked
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object RouteLocs : LocReferences() {
    val swampGates = listOf(find("mortmyre_metalgateclosed_l"), find("mortmyre_metalgateclosed_r"))
    val ardougne = find("wildinlever")
    val edgeville = find("edgeville_wildy_lever")
    val normal = find("wildoutlever_default")
    val diary = find("wildoutlever_diary")
    val swapped = find("wildoutlever_diary_swapped")
}

internal object RouteBits : VarBitReferences() {
    val warningDisabled = find("wilderness_lever_blockwarning_standard")
    val leverMode = find("wildoutlever_vis")
    val swap = find("wilderness_lever_first_op")
}

internal object RouteVars : VarpReferences() {
    val natureSpirit = find("druidspirit")
}

public class SwampAndWildernessTravel : PluginScript() {
    override fun ScriptContext.startup() {
        for (gate in RouteLocs.swampGates) onOpLoc1(gate) {
            if (!player.members) return@onOpLoc1
            val entering = coords.z >= 3458
            // TODO(quests): Drezel advances the existing Nature Spirit save stage.
            if (entering && vars[RouteVars.natureSpirit] == 0) {
                mes(
                    "Mort Myre is a dangerous Ghast infested swamp. Speak to Drezel before entering."
                )
                return@onOpLoc1
            }
            teleport(CoordGrid(it.loc.x, if (entering) 3457 else 3458))
            if (entering) player.completeDiaryTask(DiaryTask.EnterMortMyre)
        }
        onOpLoc1(RouteLocs.ardougne) { enterKeep(ardougne = true) }
        onOpLoc1(RouteLocs.edgeville) { enterKeep(ardougne = false) }
        onOpLoc1(RouteLocs.normal) { pull(ardougne) }
        onOpLoc1(RouteLocs.diary) { pull(ardougne) }
        onOpLoc2(RouteLocs.diary) { if (canChoose()) pull(edgeville) }
        onOpLoc1(RouteLocs.swapped) { if (canChoose()) pull(edgeville) else pull(ardougne) }
        onOpLoc2(RouteLocs.swapped) { pull(ardougne) }
        for (lever in listOf(RouteLocs.diary, RouteLocs.swapped)) onOpLoc3(lever) {
            if (canChoose()) {
                vars[RouteBits.swap] = 1 - vars[RouteBits.swap]
                syncLever(player)
            }
        }
        onPlayerLogin { syncLever(player) }
    }

    private fun ProtectedAccess.canChoose() =
        player.members && vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Easy).reward] != 0

    private fun syncLever(player: Player) {
        val claimed =
            player.members &&
                player.vars[DiaryRegion.Wilderness.tier(DiaryDifficulty.Easy).reward] != 0
        val mode = if (claimed) 1 + player.vars[RouteBits.swap] else 0
        VarPlayerIntMapSetter.set(player, RouteBits.leverMode, mode)
    }

    private suspend fun ProtectedAccess.enterKeep(ardougne: Boolean) {
        if (vars[RouteBits.warningDisabled] == 0) {
            when (
                menu(
                    "Teleport to the deep Wilderness?",
                    true,
                    listOf("Yes.", "No.", "Yes, and don't ask me again."),
                )
            ) {
                0 -> Unit
                2 -> vars[RouteBits.warningDisabled] = 1
                else -> return
            }
        }
        if (!pull(keep)) return
        if (ardougne) player.completeDiaryTask(DiaryTask.ArdougneWildernessLever)
        player.completeDiaryTask(DiaryTask.EnterByWildernessLever)
        syncLever(player)
    }

    private suspend fun ProtectedAccess.pull(tile: CoordGrid): Boolean {
        if (player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return false
        }
        mes("You pull the lever...")
        anim(seqs.teleport_casting)
        spotanim(spotanims.teleport_casting)
        delay(3)
        teleport(tile)
        publish(PlayerTeleportEvent.Start(player, tile))
        return true
    }

    private companion object {
        val ardougne = CoordGrid(2562, 3311)
        val edgeville = CoordGrid(3090, 3476)
        val keep = CoordGrid(3154, 3923)
    }
}
