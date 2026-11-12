package org.rsmod.content.skills.magic.spell.utility.arceuus

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.timers
import org.rsmod.api.script.onApNpcT
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class DarkLureSpellScript @Inject constructor(private val spell: DarkLureSpell) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onApNpcT(components.magic_spellbook_dark_lure) { spell.cast(this, it.npc) }
        onPlayerQueueWithArgs<DarkLureImpact>(dark_lure_queues.impact) {
            spell.impact(this, it.args)
        }
        onPlayerSoftTimer(timers.magic_arceuus_dark_lure_cooldown) { player.darkLureCooldown = 0 }
        onNpcTimer(timers.magic_arceuus_dark_lure_follow) {
            npc.clearTimer(timers.magic_arceuus_dark_lure_follow)
            if (npc.mode == org.rsmod.game.entity.npc.NpcMode.PlayerFollow) {
                defaultMode()
            }
        }
    }
}
