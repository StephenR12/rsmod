package org.rsmod.content.areas.city.lumbridge

import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object HideoutLocs : LocReferences() {
    val trapdoor = find("ham_multi_trapdoor")
    val closed = find("osf_trapdoor_closed")
    val open = find("osf_trapdoor_open")
    val ladder = find("osf_ham_ladder")
}
internal object HideoutBits : VarBitReferences() { val lock = find("ham_thief") }
internal object HideoutObjs : ObjReferences() {
    val picks = listOf(find("lockpick"), find("strange_old_lockpick"), find("strange_old_lockpick_full"))
}
internal object HideoutSeqs : SeqReferences() { val pick = find("human_picklock_chest") }

public class HamHideout : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(HideoutLocs.closed) { mes("The trapdoor is locked.") }
        onOpLoc5(HideoutLocs.closed) { pick() }
        for (tool in HideoutObjs.picks) {
            onOpLocU(HideoutLocs.closed, tool) {
                if (inv[it.invSlot]?.id == tool.id) pick()
            }
        }
        onOpLoc2(HideoutLocs.open) { vars[HideoutBits.lock] = 0 }
        onOpLoc1(HideoutLocs.open) {
            if (player.members && vars[HideoutBits.lock] == 1) {
                teleport(CoordGrid(3149,9652))
                vars[HideoutBits.lock] = 0
                player.completeDiaryTask(DiaryTask.EnterHamHideout)
            }
        }
        onOpLoc1(HideoutLocs.ladder) { teleport(CoordGrid(3165,3252)) }
    }

    private suspend fun ProtectedAccess.pick() {
        if (!player.members || vars[HideoutBits.lock] == 1) return
        anim(HideoutSeqs.pick)
        delay(2)
        val tool = HideoutObjs.picks.any { it in inv }
        if (statRandom(stats.thieving, if (tool) 200 else 150, 255, 0)) {
            vars[HideoutBits.lock] = 1
            statAdvance(stats.thieving, 4.0)
            mes("You pick the lock on the trapdoor.")
        } else {
            statSub(stats.thieving, 1, 0)
            if (random.of(4) == 0) statAdvance(stats.thieving, 1.0)
            mes("You fail to pick the lock and numb your fingers.")
        }
    }
}
