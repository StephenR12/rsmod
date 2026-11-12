package org.rsmod.content.generic.npcs.ram

import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpNpcU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Direct Ram interactions shared by all five final pre-cutoff identities. */
public class Ram : PluginScript() {
    override fun ScriptContext.startup() {
        RAM_TYPES.forEach { type ->
            onOpNpcU(type, objs.shears) { mes("That one looks a little too violent to shear...") }
        }
    }
}
