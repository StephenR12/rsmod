package org.rsmod.content.interfaces.journal.tab

import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object DiaryTaskmasterSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DiaryTaskmasters>("taskmasters.toml")
    }
}

object DiaryNpcs : NpcReferences() {
    val masters =
        listOf(
            find("ardy_twopints_diary") to DiaryRegion.Ardougne,
            find("jarr_desert_diary") to DiaryRegion.Desert,
            find("white_knight_diary") to DiaryRegion.Falador,
            find("viking_frem_diary") to DiaryRegion.Fremennik,
            find("seers_diary_wedge") to DiaryRegion.Kandarin,
            find("agilityarena_tickettrader") to DiaryRegion.Karamja,
            find("elise_kourend_kebos_diary") to DiaryRegion.Kourend,
            find("lesabre_mort_diary") to DiaryRegion.Morytania,
            find("toby_varrock_diary") to DiaryRegion.Varrock,
            find("western_gnome_child_diary") to DiaryRegion.Western,
            find("lesser_fanatic_diary") to DiaryRegion.Wilderness,
            find("agilityarena_tickettrader_2ops") to DiaryRegion.Karamja,
            find("agilityarena_tickettrader_3ops") to DiaryRegion.Karamja,
        )
}

internal object WildernessBoneOptions : VarBitReferences() {
    val noteLavaBones = find("cert_lava_d_bones")
}

class DiaryTaskmasters : PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, region) in DiaryNpcs.masters) {
            onOpNpc1(npc) {
                if (
                    region == DiaryRegion.Wilderness &&
                        player.members &&
                        vars[region.tier(DiaryDifficulty.Elite).reward] != 0
                ) {
                    val enabled = vars[WildernessBoneOptions.noteLavaBones] != 0
                    val option =
                        if (enabled) "Drop ordinary lava dragon bones."
                        else "Drop noted lava dragon bones."
                    if (choice2("Ask about diary equipment.", false, option, true)) {
                        vars[WildernessBoneOptions.noteLavaBones] = if (enabled) 0 else 1
                        mes(
                            if (enabled) "Lava dragons will drop ordinary bones."
                            else "Lava dragons will drop noted bones."
                        )
                        return@onOpNpc1
                    }
                }
                DiaryRewards.claim(this, region)
            }
        }
    }
}
