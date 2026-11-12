package org.rsmod.content.skills.fletching.ogrearrowshafts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class OgreArrowShaftFletchingScript
@Inject
constructor(private val fletching: OgreArrowShaftFletching) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(objs.knife, objs.achey_tree_logs) { fletching.select(this) }
        onOpHeldU(objs.fletching_knife, objs.achey_tree_logs) { fletching.select(this) }
        onPlayerQueueWithArgs<OgreLogFletchingRequest>(fletching_queues.ogreArrowShafts) {
            fletching.process(this, it.args)
        }
    }
}
