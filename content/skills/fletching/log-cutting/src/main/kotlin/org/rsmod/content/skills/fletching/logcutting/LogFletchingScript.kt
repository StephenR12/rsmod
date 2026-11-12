package org.rsmod.content.skills.fletching.logcutting

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class LogFletchingScript
@Inject
constructor(private val fletching: LogFletching, private val recipes: LogFletchingRecipeRegistry) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (log in recipes.logs) {
            registerKnife(log, objs.knife)
            registerKnife(log, objs.fletching_knife)
        }
        onPlayerQueueWithArgs<LogFletchingRequest>(fletching_queues.logCutting) {
            fletching.process(this, it.args)
        }
    }

    private fun ScriptContext.registerKnife(log: ObjType, knife: ObjType) {
        onOpHeldU(knife, log) { fletching.select(this, log) }
    }
}
