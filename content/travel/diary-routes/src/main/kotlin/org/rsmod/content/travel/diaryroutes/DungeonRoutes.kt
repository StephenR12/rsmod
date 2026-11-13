package org.rsmod.content.travel.diaryroutes

import org.rsmod.api.player.isTeleportBlocked
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.events.PlayerTeleportEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DungeonRoutesLocs : LocReferences() {
    val kbdIn = find("dragonkinginlever")
    val kbdOut = find("dragonkingoutlever")
    val down = listOf(find("wildymirrorladdertop1"),find("wildymirrorladdertop2"))
    val up = listOf(find("wildymirrorladder1"),find("wildymirrorladder2"))
    val burrow = find("kalphite_burrow_entrance_norope")
    val burrowRope = find("kalphite_burrow_entrance_withrope")
    val queen = find("kalphite_chamber_entrance_norope")
    val queenRopes = listOf(find("kalphite_chamber_entrance_withrope_normal"),find("kalphite_chamber_entrance_withrope_private"))
    val burrowExit = find("kalphite_burrow_exit")
    val queenExit = find("kalphite_chamber_exit")
}
internal object HiveBits : VarBitReferences() {
    val first = find("kalphite_rope_1")
    val second = find("kalphite_rope_2")
    val entrance = find("kalphite_chamber_entrance")
}
public class DungeonRoutes @Inject constructor(private val players: PlayerRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (ladder in DungeonRoutesLocs.down) onOpLoc1(ladder) { teleport(it.loc.coords.translate(0,6400)) }
        for (ladder in DungeonRoutesLocs.up) onOpLoc1(ladder) { teleport(it.loc.coords.translate(0,-6400)) }
        onOpLoc1(DungeonRoutesLocs.kbdIn) {
            if (lever(kbd)) player.completeDiaryTask(DiaryTask.EnterKingBlackDragonLair)
        }
        onOpLoc2(DungeonRoutesLocs.kbdIn) {
            val count = players.findAll(ZoneKey.from(kbd),4).count { it.coords.mx == 35 && it.coords.mz == 73 }
            mes("There are $count adventurers in the King Black Dragon's lair.")
        }
        onOpLoc1(DungeonRoutesLocs.kbdOut) { lever(CoordGrid(3067,10253)) }
        onOpLocU(DungeonRoutesLocs.burrow,objs.rope) {
            if (player.members && vars[HiveBits.first] == 0 && invDel(inv,objs.rope,slot=it.invSlot).success) {
                vars[HiveBits.first] = 1
                mes("You tie the rope securely to the entrance.")
            }
        }
        onOpLoc1(DungeonRoutesLocs.burrowRope) {
            if (player.members && vars[HiveBits.first] != 0) {
                teleport(CoordGrid(3484,9510,2))
                player.completeDiaryTask(DiaryTask.EnterKalphiteHive)
            }
        }
        onOpLocU(DungeonRoutesLocs.queen,objs.rope) {
            if (player.members && vars[HiveBits.second] == 0 && invDel(inv,objs.rope,slot=it.invSlot).success) {
                vars[HiveBits.second] = 1
                vars[HiveBits.entrance] = 1
                mes("You secure the rope to the tunnel.")
            }
        }
        for (rope in DungeonRoutesLocs.queenRopes) onOpLoc1(rope) {
            if (player.members && vars[HiveBits.second] != 0) teleport(CoordGrid(3507,9494))
        }
        for (hole in DungeonRoutesLocs.queenRopes + DungeonRoutesLocs.queen) onOpLoc3(hole) {
            val count = players.findAll(ZoneKey.from(CoordGrid(3488,9504)),5).count { it.coords.mx == 54 && it.coords.mz == 148 && it.level == 0 }
            mes("There are $count adventurers in the Queen's chamber.")
        }
        onOpLoc1(DungeonRoutesLocs.queenExit) { teleport(CoordGrid(3507,9497,2)) }
        onOpLoc1(DungeonRoutesLocs.burrowExit) {
            teleport(CoordGrid(3226,3109))
            resetTemporaryRopes(player)
        }
        onPlayerLogin { if (player.coords.mx != 54 || player.coords.mz != 148) resetTemporaryRopes(player) }
        onEvent<PlayerTeleportEvent.Start> { if (destination.mx != 54 || destination.mz != 148) resetTemporaryRopes(player) }
    }

    private fun resetTemporaryRopes(player: Player) {
        if (player.vars[DiaryRegion.Desert.tier(DiaryDifficulty.Hard).reward] != 0) return
        for (bit in listOf(HiveBits.first,HiveBits.second,HiveBits.entrance)) {
            if (player.vars[bit] != 0) VarPlayerIntMapSetter.set(player,bit,0)
        }
    }
    private suspend fun ProtectedAccess.lever(destination: CoordGrid): Boolean {
        if (!player.members || player.isTeleportBlocked()) {
            mes("A magical force stops you from teleporting.")
            return false
        }
        delay(2)
        teleport(destination)
        publish(PlayerTeleportEvent.Start(player,destination))
        return true
    }
    private val kbd = CoordGrid(2271,4680)
}
