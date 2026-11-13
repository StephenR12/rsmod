package org.rsmod.content.interfaces.journal.tab

import jakarta.inject.Inject
import org.rsmod.api.config.refs.fontmetrics
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.diary.DiaryDifficulty
import org.rsmod.api.player.diary.DiaryRegion
import org.rsmod.api.player.diary.syncDiaryTier
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.type.font.FontMetricsTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

object DiaryInterfaces : InterfaceReferences() {
    val scroll = find("journalscroll")
    val xpReward = find("xpreward")
}

object DiaryComponents : ComponentReferences() {
    val taskbox = find("area_task:taskbox")
    val title = find("journalscroll:title")
    val lines = (1..200).map { find("journalscroll:qj$it") }
    val skillChoices = find("xpreward:universe")
}

class DiaryJournal
@Inject
constructor(
    private val launcher: ProtectedAccessLauncher,
    private val alignment: TextAlignment,
    private val fonts: FontMetricsTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen(interfaces.area_task) {
            for (region in DiaryRegion.entries) {
                for (difficulty in DiaryDifficulty.entries) player.syncDiaryTier(region, difficulty)
            }
            player.ifSetEvents(DiaryComponents.taskbox, 0..11, IfEvent.Op1)
        }
        onIfOverlayButton(DiaryComponents.taskbox) {
            val region = DiaryRegion.entries.firstOrNull { it.journalIndex == comsub }
            if (op == IfButtonOp.Op1 && region != null) {
                if (!launcher.launch(player) { openJournal(region) }) {
                    player.mes("Please finish what you're doing first.")
                }
            }
        }
    }

    private fun ProtectedAccess.openJournal(region: DiaryRegion) {
        ifOpenMainModal(DiaryInterfaces.scroll)
        runClientScript(6844) // Clear lines from the previously viewed journal.
        ifSetText(DiaryComponents.title, region.title)
        val font = fonts[fontmetrics.p12_full]
        val lines = buildList {
            for (difficulty in DiaryDifficulty.entries) {
                val tasks = region.tasks(difficulty)
                val done = tasks.count { it.isComplete(player) }
                add("<col=800000>${difficulty.name} ($done/${tasks.size})</col>")
                for (entry in tasks) {
                    val completed = entry.isComplete(player)
                    val pages = alignment.generatePageList(entry.text, 404, font)
                    for (page in pages) {
                        for (line in page.text.split("<br>")) {
                            add(if (completed) "<str>$line</str>" else line)
                        }
                    }
                }
                add("")
            }
        }
        lines.forEachIndexed { index, line -> ifSetText(DiaryComponents.lines[index], line) }
        runClientScript(6845, 1, lines.size)
    }
}
