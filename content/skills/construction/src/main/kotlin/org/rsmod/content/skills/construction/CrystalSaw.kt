package org.rsmod.content.skills.construction

import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object SawItems : ObjReferences() {
    val saw = find("eyeglo_crystal_saw")
    val seed = find("crystal_seed_old_small")
}

internal object SawLocs : LocReferences() {
    val cave = find("eyeglo_singing_bowl")
    val prif = find("prif_singing_bowl")
}

internal object SawBits : VarBitReferences() {
    val quest = find("eyeglo_quest")
    val charges = find("eyeglo_crystal_saw_charges")
    val extra = find("eyeglo_crystal_saw_charges_extended")
}

internal object SawSongs : SeqReferences() {
    val cave = find("eyeglo_singing_bowl_human")
    val prif = find("prif_crystal_singing")
}

public class CrystalSaw : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc3(SawLocs.cave) { sing(false) }
        onOpLocU(SawLocs.cave, SawItems.seed) { sing(false) }
        onOpLocU(SawLocs.prif, SawItems.seed) { sing(true) }
        onOpHeld4(SawItems.saw) { mes("Your saw has ${charges()} charges remaining.") }
    }

    private suspend fun ProtectedAccess.sing(prif: Boolean) {
        if (!player.members || vars[SawBits.quest] < 60 || SawItems.seed !in inv) {
            mes("You need a crystal saw seed from The Eyes of Glouphrie.")
            return
        }
        anim(if (prif) SawSongs.prif else SawSongs.cave)
        delay(3)
        if (invReplace(inv, SawItems.seed, 1, SawItems.saw).failure) return
        val medium = DiaryRegion.Western.tasks(DiaryDifficulty.Medium).all { it.isComplete(player) }
        vars[SawBits.charges] = 28
        vars[SawBits.extra] = if (medium) 1 else 0
        player.completeDiaryTask(DiaryTask.SingCrystalSaw)
        mes("The seed takes the shape of a crystal saw.")
    }

    internal companion object {
        fun ProtectedAccess.charges(): Int = vars[SawBits.charges] + vars[SawBits.extra] * 28

        fun ProtectedAccess.useCharge() {
            val next = charges() - 1
            vars[SawBits.extra] = if (next > 28) 1 else 0
            vars[SawBits.charges] = if (next > 28) next - 28 else next.coerceAtLeast(0)
            if (next == 0) invReplace(inv, SawItems.saw, 1, SawItems.seed)
        }
    }
}
