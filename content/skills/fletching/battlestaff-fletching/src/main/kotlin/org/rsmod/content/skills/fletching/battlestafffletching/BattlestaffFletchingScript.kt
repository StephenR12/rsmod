package org.rsmod.content.skills.fletching.battlestafffletching

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BattlestaffFletchingScript
@Inject
constructor(private val fletching: BattlestaffFletching) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(objs.celastrus_bark) { fletching.start(this) }
        onOpHeldU(objs.knife, objs.celastrus_bark) { fletching.start(this) }
        onOpHeldU(objs.fletching_knife, objs.celastrus_bark) { fletching.start(this) }
        onPlayerQueueWithArgs<BattlestaffFletchingRequest>(fletching_queues.battlestaffFletching) {
            fletching.process(this, it.args)
        }
    }
}
