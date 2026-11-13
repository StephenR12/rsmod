package org.rsmod.content.skills.prayer.remains

import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.DiaryTask
import org.rsmod.api.player.diary.completeDiaryTask
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal object DarkAltarNpcs : NpcReferences() {
    val tyss = find("arceuus_darkguardian")
}

internal object DarkAltarGoods : ObjReferences() {
    val signet = find("poh_arceuus_signet")
    val sanctifier = find("ash_sanctifier")
}

public class DarkAltarGuardian : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc3(DarkAltarNpcs.tyss) { switchBook() }
        onOpNpc1(DarkAltarNpcs.tyss) {
            if (!player.members) return@onOpNpc1
            when (
                menu(
                    "Talk to Tyss",
                    true,
                    listOf(
                        "Switch spellbook.",
                        "Ask for an Arceuus signet.",
                        "Ask for an ash sanctifier.",
                        "Leave.",
                    ),
                )
            ) {
                0 -> switchBook()
                1 ->
                    if (invAdd(inv, DarkAltarGoods.signet).success)
                        mes("Tyss gives you an Arceuus signet.")
                2 -> {
                    val item = DarkAltarGoods.sanctifier
                    when {
                        vars[DiaryRegion.Kourend.tier(DiaryDifficulty.Hard).reward] == 0 ->
                            mes("Complete the hard Kourend & Kebos Diary first.")
                        item in inv || player.invMap[invs.bank]?.contains(item) == true ->
                            mes("You already have an ash sanctifier.")
                        invAdd(inv, item).success -> mes("Tyss gives you an ash sanctifier.")
                    }
                }
            }
        }
    }

    private fun ProtectedAccess.switchBook() {
        if (!player.members) return
        val active = vars[varbits.spellbook] == Spellbook.Arceuus.varValue
        vars[varbits.arceuus_spellbook_unlocked] = 1
        vars[varbits.spellbook] =
            if (active) Spellbook.Standard.varValue else Spellbook.Arceuus.varValue
        if (!active) player.completeDiaryTask(DiaryTask.SwitchAtTyss)
        mes(
            if (active) "You switch to the standard spellbook."
            else "You switch to the Arceuus spellbook."
        )
    }
}
