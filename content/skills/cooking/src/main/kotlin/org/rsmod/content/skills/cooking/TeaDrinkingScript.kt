package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class TeaDrinkingScript @Inject constructor(private val tea: TeaDrinking) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(objs.cup_of_tea) { tea.drink(this, it.slot) }
    }
}
