package org.rsmod.content.skills.magic.spell.utility.standard

import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.interact.ObjTInteractionPolicyRegistry
import org.rsmod.api.script.onApObjT
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.obj.Obj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TelekineticGrabSpellScript
@Inject
constructor(
    private val telegrab: TelekineticGrabSpell,
    private val policies: ObjTInteractionPolicyRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        policies.register(components.magic_spellbook_telegrab, range = 10, checkLineOfSight = false)
        onApObjT(components.magic_spellbook_telegrab) { telegrab.cast(this, it.obj) }
        onPlayerQueueWithArgs<Obj>(telekinetic_grab_queues.cast) {
            telegrab.cast(this, it.args, allowQueue = false)
        }
        onPlayerQueueWithArgs<TelekineticGrabImpact>(telekinetic_grab_queues.impact) {
            telegrab.impact(this, it.args)
        }
    }
}
